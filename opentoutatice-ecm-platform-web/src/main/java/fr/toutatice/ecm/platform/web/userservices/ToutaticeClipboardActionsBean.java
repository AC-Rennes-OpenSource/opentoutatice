/*
 * (C) Copyright 2014 Académie de Rennes (http://www.ac-rennes.fr/), OSIVIA (http://www.osivia.com) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *
 * Contributors:
 *   mberhaut1
 *    
 */
package fr.toutatice.ecm.platform.web.userservices;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.bpm.ProcessInstance;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.webapp.clipboard.ClipboardActionsBean;

import fr.toutatice.ecm.platform.core.constants.ExtendedSeamPrecedence;
import fr.toutatice.ecm.platform.core.constants.ToutaticeNuxeoStudioConst;

@Name("clipboardActions")
@Scope(ScopeType.SESSION)
@Install(precedence = ExtendedSeamPrecedence.TOUTATICE)
public class ToutaticeClipboardActionsBean extends ClipboardActionsBean {

	private static final long serialVersionUID = 7075230311269912496L;

	private static final Log log = LogFactory.getLog(ToutaticeClipboardActionsBean.class);
	
    @In(create = true)
    protected transient DocumentRoutingService routingService;
    
	/*
	 * Empêcher la possibilité de déplacer un document qui est impliqué dans un workflow
	 * 
	 * @see org.nuxeo.ecm.webapp.clipboard.ClipboardActionsBean#getCanMoveInside(java.lang.String, org.nuxeo.ecm.core.api.DocumentModel)
	 */
	@Override
    public boolean getCanMoveInside(String listName, DocumentModel document) throws ClientException {
        if (documentsListsManager.isWorkingListEmpty(listName) || document == null) {
            return false;
        }
        DocumentRef destFolderRef = document.getRef();
        DocumentModel destFolder = document;
        if (!documentManager.hasPermission(destFolderRef, SecurityConstants.ADD_CHILDREN)) {
            return false;
        } else {
            // filter on allowed content types
            // see if at least one doc can be removed and pasted
            for (DocumentModel docModel : documentsListsManager.getWorkingList(listName)) {
                DocumentRef sourceFolderRef = docModel.getParentRef();
                String sourceType = docModel.getType();
                boolean canRemoveDoc = documentManager.hasPermission(
                        sourceFolderRef, SecurityConstants.REMOVE_CHILDREN);
                
                boolean canPasteInCurrentFolder = typeManager.isAllowedSubType(
                        sourceType, destFolder.getType(),
                        navigationContext.getCurrentDocument());
                
                boolean sameFolder = sourceFolderRef.equals(destFolderRef);
                
                boolean hasActiveWF = false;
                List<DocumentRoute> documentRoutes = routingService.getDocumentRoutesForAttachedDocument(documentManager, docModel.getId());
                if (documentRoutes != null && !documentRoutes.isEmpty()) {
                	hasActiveWF = true;
                }
                
                if (canRemoveDoc && canPasteInCurrentFolder && !sameFolder && !hasActiveWF) {
                    return true;
                }
            }
            return false;
        }
    }

	@Override
    public List<DocumentModel> moveDocumentsToNewParent(
            DocumentModel destFolder, List<DocumentModel> docs)
            throws ClientException {
        DocumentRef destFolderRef = destFolder.getRef();
        boolean destinationIsDeleted = LifeCycleConstants.DELETED_STATE.equals(destFolder.getCurrentLifeCycleState());
        List<DocumentModel> newDocs = new ArrayList<DocumentModel>();
        StringBuilder sb = new StringBuilder();
        for (DocumentModel docModel : docs) {
            DocumentRef sourceFolderRef = docModel.getParentRef();

            String sourceType = docModel.getType();
            boolean canRemoveDoc = documentManager.hasPermission(
                    sourceFolderRef, SecurityConstants.REMOVE_CHILDREN);
            
            boolean canPasteInCurrentFolder = typeManager.isAllowedSubType(
                    sourceType, destFolder.getType(),
                    navigationContext.getCurrentDocument());
            
            boolean sameFolder = sourceFolderRef.equals(destFolderRef);
            
            boolean hasActiveWF = false;
            List<DocumentRoute> documentRoutes = routingService.getDocumentRoutesForAttachedDocument(documentManager, docModel.getId());
            if (documentRoutes != null && !documentRoutes.isEmpty()) {
            	hasActiveWF = true;
            }

            if (canRemoveDoc && canPasteInCurrentFolder && !sameFolder && !hasActiveWF) {
                if (destinationIsDeleted) {
                    if (checkDeletedState(docModel)) {
                        DocumentModel newDoc = documentManager.move(
                                docModel.getRef(), destFolderRef, null);
                        setDeleteState(newDoc);
                        newDocs.add(newDoc);
                    } else {
                        addWarnMessage(sb, docModel);
                    }
                } else {
                    DocumentModel newDoc = documentManager.move(
                            docModel.getRef(), destFolderRef, null);
                    newDocs.add(newDoc);
                }
            }
        }
        documentManager.save();

        if (sb.length() > 0) {
            facesMessages.add(StatusMessage.Severity.WARN, sb.toString(), null);
        }
        return newDocs;
    }

	/**
	 * Fix mantis #3409: Mise en ligne: erreur sur l'opération de mise en ligne d'un document copié
	 */
	@Override
	protected List<DocumentModel> recreateDocumentsWithNewParent(DocumentModel parent, List<DocumentModel> documents) throws ClientException {
		List<DocumentModel> copiedDocsList = new ArrayList<DocumentModel>();
		
		// copier les documents
		List<DocumentModel> newDocs = super.recreateDocumentsWithNewParent(parent, documents);
		
		// pour chaque document copié...
		for (DocumentModel newDoc : newDocs) {
			// faire passer dans l'état "en projet" les documents validés (pas de conservation de l'historique)
			newDoc.refresh(DocumentModel.REFRESH_STATE, null);
			if (ToutaticeNuxeoStudioConst.CST_DOC_STATE_APPROVED.equals(newDoc.getCurrentLifeCycleState())) {
				if (!newDoc.isCheckedOut()) {
					newDoc.checkOut();
				}
				
				documentManager.followTransition(newDoc.getRef(), "backToProject");
			}
			documentManager.saveDocument(newDoc);

			// supprimer les proxies car ceux-ci pointent sur le document source et non sa copie (en mode unrestricted pour se soustraire aux droits)
			if (newDoc.isFolder()) {
				removeProxy(newDoc);
			}
			
			copiedDocsList.add(newDoc);
		}
		
		return copiedDocsList;
	}
	
	protected void removeProxy(DocumentModel folder) throws ClientException {
		innerRemoveDocumentProxy runner = new innerRemoveDocumentProxy(documentManager, folder);
		runner.runUnrestricted();
	}

	private class innerRemoveDocumentProxy extends UnrestrictedSessionRunner {
		private DocumentModel folder;
		
		public innerRemoveDocumentProxy(CoreSession session, DocumentModel folder) {
			super(session);
			this.folder = folder;
		}
		
		@Override
		public void run() throws ClientException {
			DocumentModelList proxies = this.session.query("SELECT * FROM Document WHERE ecm:path STARTSWITH '" + this.folder.getPathAsString() + "' AND ecm:isProxy = 1");
			for (DocumentModel proxy : proxies) {
				this.session.removeDocument(proxy.getRef());
			}
		}
		
	}

}
