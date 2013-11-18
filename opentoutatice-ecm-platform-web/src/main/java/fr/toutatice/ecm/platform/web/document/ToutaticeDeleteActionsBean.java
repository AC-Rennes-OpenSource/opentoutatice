package fr.toutatice.ecm.platform.web.document;

import static org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager.CURRENT_DOCUMENT_SECTION_SELECTION;
import static org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager.CURRENT_DOCUMENT_SELECTION;
import static org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager.CURRENT_DOCUMENT_TRASH_SELECTION;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.webapp.action.DeleteActionsBean;

import fr.toutatice.ecm.platform.core.constants.NuxeoStudioConst;
import fr.toutatice.ecm.platform.core.helper.ToutaticeOperationHelper;



@Name("deleteActions")
@Scope(ScopeType.EVENT)
@Install(precedence = Install.DEPLOYMENT)
public class ToutaticeDeleteActionsBean extends DeleteActionsBean {

	private static final long serialVersionUID = 1L;

	private static final Log log = LogFactory.getLog(ToutaticeDeleteActionsBean.class);
	
	/**
	 * Additional controls on delete action.
	 * @see fr.gouv.education.acrennes.ged.document.additionalDeleteFilter()
	 */
	@Override
	public boolean getCanDelete() {
		boolean status = super.getCanDelete();
		
		// check additional custom rules
		if (true == status) {
			status = additionalDeleteFilter(CURRENT_DOCUMENT_SELECTION);
		}
		
		return status;
	}
	
	/**
	 * Additional controls on purge action.
	 * @see fr.gouv.education.acrennes.ged.document.additionalDeleteFilter()
	 */
	@Override
	public boolean getCanPurge() throws ClientException {
		boolean status = super.getCanPurge();
		
		// check additional custom rules
		if (true == status) {
			status = additionalDeleteFilter(CURRENT_DOCUMENT_TRASH_SELECTION);
		}
		
		return status;
	}
	
	/**
	 * Implement additional actions:
	 * Unpublish first the document to delete.
	 * @see org.nuxeo.ecm.webapp.action.DeleteActionsBean#deleteSelection()
	 */
	@Override
	public String deleteSelection() throws ClientException {
		List<DocumentModel> proxiedDocsList = getSelectionProxiesList(CURRENT_DOCUMENT_SELECTION);
		DocumentModel currentFolder = navigationContext.getCurrentDocument();

		if (!proxiedDocsList.isEmpty()) {
			try {
				ToutaticeOperationHelper.runOperationChain(documentManager, NuxeoStudioConst.CST_OPERATION_DOCUMENT_UNPUBLISH_SELECTION, new DocumentModelListImpl(proxiedDocsList));
			} catch (Exception e) {
				log.error("Failed to set offline the selection from the document: '" + currentFolder.getTitle() + "', error: " + e.getMessage());
			}
		}

		return super.deleteSelection();
	}
	
	/**
	 * Implement additional actions:
	 * Unpublish first the document to delete.
	 * @see org.nuxeo.ecm.webapp.action.DeleteActionsBean#purgeSelection()
	 */
	@Override
	public String purgeSelection() throws ClientException {
		List<DocumentModel> proxiedDocsList = getSelectionProxiesList(CURRENT_DOCUMENT_TRASH_SELECTION);
		DocumentModel currentFolder = navigationContext.getCurrentDocument();

		if (!proxiedDocsList.isEmpty()) {
			try {
				ToutaticeOperationHelper.runOperationChain(documentManager, NuxeoStudioConst.CST_OPERATION_DOCUMENT_UNPUBLISH_SELECTION, new DocumentModelListImpl(proxiedDocsList));
			} catch (Exception e) {
				log.error("Failed to set offline the selection from the document: '" + currentFolder.getTitle() + "', error: " + e.getMessage());
			}
		}

		return super.purgeSelection();
	}
	
	/**
	 * Dans une section ou section root, ne pas permettre de supprimer un document publié mais seulement les éléments 'folderish'.
	 * L'action de dé-publication doit être utilisée pour supprimer les documents présents dans les sections. 
	 * <br/>
	 * 
	 * @see org.nuxeo.ecm.webapp.action.DeleteActionsBean#getCanDeleteSections()
	 */
	@Override
	public boolean getCanDeleteSections() {
		List<DocumentModel> docs = documentsListsManager.getWorkingList(CURRENT_DOCUMENT_SECTION_SELECTION);
		for (DocumentModel doc : docs) {
			if (!doc.hasFacet(FacetNames.FOLDERISH)) {
				return false;
			}
		}
		return super.getCanDeleteSections();
	}

	/**
	 * Implement additional filtering:
	 * <ul>
	 *    <li>Do not delete document that is approved if the user has not the "validationWorkflow_validation" permission</li>
	 *    <li>Do not delete document that is published if the user has not the "validationWorkflow_validation" permission</li>
	 * </ul>
	 * </BR>
	 * 
	 * @return: true if the action must be enabled. Otherwise, returns false
	 */
	private boolean additionalDeleteFilter(String workingListName) {
		boolean status = true;
		
		// check additional custom rules
		if (!documentsListsManager.isWorkingListEmpty(workingListName)) {
			try {
				List<DocumentModel> docs = documentsListsManager.getWorkingList(workingListName);
				for (DocumentModel doc : docs) {
					DocumentModelList proxies = documentManager.getProxies(doc.getRef(), doc.getParentRef());

					/*
					 * Suppression (définitive?) du test sur la permission de validation.
					 */
//					boolean isApproved = NuxeoStudioConst.CST_DOC_STATE_APPROVED.equals(doc.getCurrentLifeCycleState());
//					boolean hasProxy = (null != proxies && !proxies.isEmpty());
//					
//					if (isApproved || hasProxy) {
//						if (!documentManager.hasPermission(doc.getRef(), NuxeoStudioConst.CST_PERM_VALIDATE)) {
//							status = false;
//							break;
//						}
//					}
				}
			} catch (ClientException e) {
	            log.error("Cannot check delete permission", e);
	            status = false;
			}
		}
		
		return status;
	}
	
	private List<DocumentModel> getSelectionProxiesList(String selectionList) throws ClientException {
		List<DocumentModel> proxiedDocsList = new ArrayList<DocumentModel>();

		List<DocumentModel> docsList = documentsListsManager.getWorkingList(selectionList);
		if (null != docsList && 0 < docsList.size()) {
			for (DocumentModel document : docsList) {
				DocumentModelList proxies = documentManager.getProxies(document.getRef(), document.getParentRef());
				if (!proxies.isEmpty()) {
					proxiedDocsList.add(document);
				}
			}
		}

		return proxiedDocsList;
	}
	
}
