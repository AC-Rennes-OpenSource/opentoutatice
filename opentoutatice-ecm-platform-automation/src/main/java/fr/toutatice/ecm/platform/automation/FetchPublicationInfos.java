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
 * mberhaut1
 */
package fr.toutatice.ecm.platform.automation;

import java.net.URLEncoder;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.impl.primitives.BooleanProperty;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.model.NoSuchDocumentException;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.ecm.platform.jbpm.JbpmListFilter;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

import fr.toutatice.ecm.platform.core.constants.ToutaticeGlobalConst;
import fr.toutatice.ecm.platform.core.constants.ToutaticeNuxeoStudioConst;
import fr.toutatice.ecm.platform.core.helper.ToutaticeDocumentHelper;

@Operation(id = FetchPublicationInfos.ID, category = Constants.CAT_FETCH, label = "Fetch publish space informations",
        description = "Fetch informations about the publish space, worksapce, proxy status, ... of a given document.")
public class FetchPublicationInfos {

    private static final String WEB_ID_QUERY = "select * from Document Where ttc:domainID = '%s'"
            + " AND ttc:webid = '%s' AND ecm:isProxy = 0 AND ecm:currentLifeCycleState!='deleted' AND ecm:isCheckedInVersion = 0";


    private static final Log log = LogFactory.getLog(FetchPublicationInfos.class);

    /**
     * Id Nuxeo de l'opération (s'applique à un Document).
     */
    public static final String ID = "Document.FetchPublicationInfos";

    /**
     * Codes d'erreur
     */
    public static final int ERROR_CONTENT_NOT_FOUND = 1;
    public static final int ERROR_CONTENT_FORBIDDEN = 2;
    public static final int ERROR_PUBLISH_SPACE_NOT_FOUND = 3;
    public static final int ERROR_PUBLISH_SPACE_FORBIDDEN = 4;
    public static final int ERROR_WORKSPACE_NOT_FOUND = 5;
    public static final int ERROR_WORKSPACE_FORBIDDEN = 6;
    public static final int SERVER_ERROR = 500;
    public static final String INTERNAL_PROCESSING_ERROR_RESPONSE = "InternalProcessingErrorResponse";
    private static final String TOUTATICE_PUBLI_SUFFIX = ".proxy";

    /**
     * Suufixe du nom des proxies.
     */
    private static final String SUFFIXE_PROXY = ".proxy";
    /**
     * Propriété de contextualisation (schéma toutatice).
     */
    private static final String IN_CONTEXTUALIZATON_PROPERTY = "ttc:contextualizeInternalContents";

    /**
     * Session "avec le coeur" de Nuxeo.
     */
    @Context
    protected CoreSession coreSession;

    /**
     * Service gérant les types.
     */
    @Context
    protected TypeManager typeService;

    /**
     * Service gérant les utilisateurs.
     */
    @Context
    protected UserManager userManager;

    @Context
    protected transient JbpmService jbpmService;

    /**
     * Identifiant ("path" ou uuid) du document en entrée.
     */
    @Param(name = "path", required = false)
    protected DocumentModel document;

    @Param(name = "webid", required = false)
    protected String webid;


    @OperationMethod
    public Object run() throws Exception {

        /* Réponse de l'opération sous forme de flux JSon */
        JSONArray rowInfosPubli = new JSONArray();
        JSONObject infosPubli = new JSONObject();

        List<Integer> errorsCodes = new ArrayList<Integer>();

        // Si on passe non pas un docRef en entrée mais un webId :
        if (webid != null) {
            log.warn("webid : " + webid);

            String[] segments = webid.split("/");
            String domainIdSegment;
            String webIdSegment;
            if (segments.length >= 2) {
                domainIdSegment = segments[0];
                webIdSegment = segments[1];
            } else {
                throw new NoSuchDocumentException(webid);
            }


            UnrestrictedFecthWebIdRunner fecthWebIdRunner = new UnrestrictedFecthWebIdRunner(coreSession, domainIdSegment, webIdSegment);
            fecthWebIdRunner.runUnrestricted();
            document = fecthWebIdRunner.getDoc();
            if (document == null) {
                throw new NoSuchDocumentException(webid);
            }
        }

        /*
         * Récupération du DocumentModel dont le path ou uuid est donné en
         * entrée
         */
        DocumentRef docRef = document.getRef();
        Object fetchDocumentRes = getDocument(docRef);
        /*
         * Chaque méthode "principale utilisée peut retourner un objet de type
         * Boolean ou de type DocumentModel ou d'un autre type qui est alors
         * considéré comme une erreur
         */
        if (isError(fetchDocumentRes)) {
            errorsCodes.add((Integer) fetchDocumentRes);
            infosPubli.element("errorCodes", errorsCodes);
            rowInfosPubli.add(infosPubli);
            return createBlob(rowInfosPubli);
        }
        DocumentModel document = (DocumentModel) fetchDocumentRes;

        /*
         * Test du droit de modification, suppression sur le document.
         */
        Object liveDocRes = getLiveDoc(coreSession, document, infosPubli);
        if (isError(liveDocRes)) {
            infosPubli = (JSONObject) liveDocRes;
            infosPubli.element("documentPath", URLEncoder.encode(document.getPath().toString(), "UTF-8"));
            infosPubli.element("liveId", "");
            infosPubli.element("editableByUser", Boolean.FALSE);
            infosPubli.element("isDeletableByUser", Boolean.FALSE);
        } else {
            DocumentModel liveDoc = (DocumentModel) liveDocRes;
            infosPubli.element("liveId", liveDoc.getId());
            Object isEditable = isEditableByUser(infosPubli, liveDoc);
            infosPubli.element("editableByUser", isEditable);
            Object isDeletable = isDeletableByUser(infosPubli, liveDoc);
            infosPubli.element("isDeletableByUser", isDeletable);
            Object canUserValidate = canUserValidate(infosPubli, liveDoc);
            infosPubli.element("canUserValidate", canUserValidate);
            Object isOnLinePending = isValidateOnLineTaskPending(liveDoc);
            infosPubli.element("isOnLinePending", isOnLinePending);
            Object isUserOnLineInitiator = isUserOnLIneWorkflowInitiator(liveDoc);
            infosPubli.element("isUserOnLineInitiator", isUserOnLineInitiator);

            /*
             * Récupération du path du document - cas où un uuid est donné en
             * entrée
             */
            String livePath = liveDoc.getPathAsString();
            String docPath = document.getPath().toString();
            String path = docPath;
            if (docPath.endsWith(TOUTATICE_PUBLI_SUFFIX) && docPath.equals(livePath + TOUTATICE_PUBLI_SUFFIX))
                path = livePath;
            infosPubli.element("documentPath", URLEncoder.encode(path, "UTF-8"));

            /* Indique une modification du live depuis la dernière publication du proxy */
            liveDoc = (DocumentModel) liveDocRes;
            infosPubli.element("liveVersion", liveDoc.getVersionLabel());
        }

        infosPubli.put("subTypes", new JSONObject());
        if (document.isFolder()) {
            boolean canAddChildren = coreSession.hasPermission(document.getRef(), SecurityConstants.ADD_CHILDREN);
            if (canAddChildren) {
                /*
                 * Récupération des sous-types permis dans le folder.
                 */
                Collection<Type> allowedSubTypes = this.typeService.getAllowedSubTypes(document.getType());
                JSONObject subTypes = new JSONObject();
                for (Type subType : allowedSubTypes) {
                    subTypes.put(subType.getId(), URLEncoder.encode(subType.getLabel(), "UTF-8"));
                }
                infosPubli.put("subTypes", subTypes);
            }
        }

        /*
         * Récupération du "droit" de commenter.
         */
        boolean docCommentable = document.hasFacet("Commentable");
        boolean docMutable = !document.isImmutable();
        Principal user = coreSession.getPrincipal();
        if (user == null) {
            throw new ClientException("Current user not found.");
        }
        boolean userNotAnonymous = !((NuxeoPrincipal) user).isAnonymous();
        infosPubli.put("isCommentableByUser", docCommentable && docMutable && userNotAnonymous);

        UnrestrictedFecthPubliInfosRunner infosPubliRunner = new UnrestrictedFecthPubliInfosRunner(coreSession, document, infosPubli, userManager, errorsCodes);

        infosPubliRunner.runUnrestricted();
        errorsCodes = infosPubliRunner.getErrorsCodes();
        infosPubli = infosPubliRunner.getInfosPubli();
        infosPubli.element("errorCodes", errorsCodes);
        rowInfosPubli.add(infosPubli);
        return createBlob(rowInfosPubli);
    }

    /**
     * Récupère un code d'erreur.
     * 
     * @param operationRes
     *            objet de type Response contenant en en-tête un code d'erreur
     * @param errorCodeNotFound
     *            code d'erreur si un document n'est pas trouvé
     * @param errorCodeForbidden
     *            code d'erreur si l'utilisateur n'a pas le droit de lecture sur
     *            un document
     * @return le code d'erreur correspondant à ceux présents dans l'en-tête de
     *         operationRes
     */
    private static int getErrorCode(Exception inputException, int errorCodeNotFound, int errorCodeForbidden) {
        Exception exception = inputException;
        int errorCode = 0;
        if (exception instanceof NoSuchDocumentException) {
            errorCode = errorCodeNotFound;
        } else if (exception instanceof DocumentSecurityException) {
            errorCode = errorCodeForbidden;
        }
        return errorCode;
    }

    /**
     * Méthode permettant de vérifier si un document (live) est modifiable par
     * l'utilisateur.
     * 
     * @param infos
     *            pour stocker le résultat du test (booléen)
     * @param liveDoc
     *            document testé
     * @return vrai si le document est modifiable par l'utilisateur
     * @throws ServeurException
     */
    private Object isEditableByUser(JSONObject infos, DocumentModel liveDoc) throws ServeurException {
        Boolean canModify = null;
        try {
            canModify = Boolean.valueOf(coreSession.hasPermission(liveDoc.getRef(), SecurityConstants.WRITE));
        } catch (ClientException e) {
            if (e instanceof DocumentSecurityException) {
                return Boolean.FALSE;
            } else {
                log.warn("Failed to fetch permissions for document '" + liveDoc.getPathAsString() + "', error:" + e.getMessage());
                throw new ServeurException(e);
            }
        }
        return canModify;
    }

    /**
     * Get user validate rigth on document.
     */
    private Object canUserValidate(JSONObject infos, DocumentModel liveDoc) throws ServeurException {
        Boolean canValidate = null;
        try {
            canValidate = Boolean.valueOf(coreSession.hasPermission(liveDoc.getRef(), ToutaticeNuxeoStudioConst.CST_PERM_VALIDATE));
        } catch (ClientException e) {
            if (e instanceof DocumentSecurityException) {
                return Boolean.FALSE;
            } else {
                log.warn("Failed to fetch permissions for document '" + liveDoc.getPathAsString() + "', error:" + e.getMessage());
                throw new ServeurException(e);
            }
        }
        return canValidate;
    }

    /**
     * Méthode permettant de vérifier si un document (live) est supprimable par
     * l'utilisateur.
     * 
     * @param infos
     *            pour stocker le résultat du test (booléen)
     * @param liveDoc
     *            document testé
     * @return vrai si le document est supprimable par l'utilisateur
     * @throws Exception
     */
    private Object isDeletableByUser(JSONObject infos, DocumentModel liveDoc) throws Exception {
        Boolean canBeDelete = Boolean.FALSE;
        try {
            TrashService trash = Framework.getService(TrashService.class);
            List<DocumentModel> docs = new ArrayList<DocumentModel>();
            docs.add(liveDoc);
            canBeDelete = trash.canDelete(docs, coreSession.getPrincipal(), false);

            /*
             * Règle de gestion liée au droit de validation et à l'existence de proxy local:
             * Un document dans l'état validé ou bien qui est publié peut être supprimé seulement
             * si l'usager connecté possède le droit de validation.
             */
            if (canBeDelete) {
                DocumentModel proxy = ToutaticeDocumentHelper.getProxy(coreSession, liveDoc, null);
                boolean hasProxy = (null != proxy);
                boolean isApproved = ToutaticeNuxeoStudioConst.CST_DOC_STATE_APPROVED.equals(liveDoc.getCurrentLifeCycleState());
                if (isApproved || hasProxy) {
                    boolean canValidate = coreSession.hasPermission(liveDoc.getRef(), ToutaticeNuxeoStudioConst.CST_PERM_VALIDATE);
                    canBeDelete = Boolean.valueOf(canValidate);
                }
            }
        } catch (ClientException e) {
            if (e instanceof DocumentSecurityException) {
                return Boolean.FALSE;
            } else {
                log.warn("Failed to fetch permissions for document '" + liveDoc.getPathAsString() + "', error:" + e.getMessage());
                throw new ServeurException(e);
            }
        }

        return canBeDelete;
    }

    private boolean isValidateOnLineTaskPending(DocumentModel document) throws ClientException {
        List<TaskInstance> lstTasks = jbpmService.getTaskInstances(document, null, (StringUtils
                .isNotBlank(ToutaticeGlobalConst.CST_WORKFLOW_TASK_ONLINE_VALIDATE)) ? new TaskInstanceFilter(
                ToutaticeGlobalConst.CST_WORKFLOW_TASK_ONLINE_VALIDATE) : null);
        return (lstTasks != null && !lstTasks.isEmpty());
    }

    private Object isUserOnLIneWorkflowInitiator(DocumentModel liveDoc) throws ClientException {
        String currentProcessInitiator = StringUtils.EMPTY;

        NuxeoPrincipal currentUser = (NuxeoPrincipal) coreSession.getPrincipal();
        ProcessInstance onLineProcess = getOnLineProcess(currentUser);
        if (onLineProcess != null) {
            Object initiator = onLineProcess.getContextInstance().getVariable(JbpmService.VariableName.initiator.name());
            if (initiator instanceof String) {
                currentProcessInitiator = (String) initiator;
                if (currentProcessInitiator.startsWith(NuxeoPrincipal.PREFIX)) {
                    currentProcessInitiator = currentProcessInitiator.substring(NuxeoPrincipal.PREFIX.length());
                }
            }
        }
        return currentProcessInitiator.equals(currentUser.getName());
    }

    public ProcessInstance getOnLineProcess(NuxeoPrincipal currentUser) throws ClientException {
        ProcessInstance onLineProcess = null;
        List<ProcessInstance> processes = jbpmService.getProcessInstances(document, currentUser, null);
        if (processes != null) {
            for (ProcessInstance process : processes) {
                if (process.getProcessDefinition().getName().equals(ToutaticeGlobalConst.CST_WORKFLOW_PROCESS_ONLINE)) {
                    onLineProcess = process;
                    break;
                }
            }
        }
        return onLineProcess;
    }

    /**
     * Méthode permettant de "fetcher" un document live et mettant à faux le
     * booléen editableByUser en cas d'erreur.
     * 
     * @param session
     *            session Nuxeo
     * @param doc
     *            document dont on cherche la version live
     * @param infos
     *            permet de stocker le booléen editableByUser
     * @return la version live ou l'objet infos avec la propriété editableByUser
     *         mise à faux en cas d'erreur à la récupération de la version live
     * @throws ServeurException
     */
    private Object getLiveDoc(CoreSession session, DocumentModel doc, JSONObject infos) throws ServeurException {
        DocumentModel liveDoc = null;
        try {
            DocumentModel srcDocument = session.getSourceDocument(doc.getRef());
            if (session.hasPermission(srcDocument.getRef(), SecurityConstants.READ_VERSION)) {
                liveDoc = session.getWorkingCopy(srcDocument.getRef());
            }
        } catch (ClientException ce) {
            if (ce instanceof DocumentSecurityException) {
                infos.element("editableByUser", Boolean.FALSE);
                return infos;
            } else {
                log.warn("Failed to fetch live document of document'" + doc.getPathAsString() + "', error:" + ce.getMessage());
                throw new ServeurException(ce);
            }
        }
        if (liveDoc == null) {
            infos.element("editableByUser", Boolean.FALSE);
            return infos;
        }
        return liveDoc;
    }

    /**
     * Méthode permettant de récupérer un document suivant sa référence; stocke,
     * le cas échéant, les erreurs 401 ou 404.
     * 
     * @param refDoc
     *            référence du document
     * @return un DocumentModel ou l'objet erros en cas d'erreur à la
     *         récupération
     * @throws ServeurException
     */
    private Object getDocument(DocumentRef refDoc) throws ServeurException {
        DocumentModel doc = null;
        try {
            doc = coreSession.getDocument(refDoc);
        } catch (ClientException ce) {
            if (ce instanceof DocumentSecurityException) {
                return ERROR_CONTENT_FORBIDDEN;
            } else {
                if (isNoSuchDocumentException(ce)) {
                    return ERROR_CONTENT_NOT_FOUND;
                } else {
                    log.warn("Failed to fetch document with path or uid: '" + document + "', error:" + ce.getMessage());
                    throw new ServeurException(ce);
                }
            }
        }
        if (doc == null) {
            return ERROR_CONTENT_NOT_FOUND;
        }
        return doc;
    }

    /**
     * Indique si l'exception donnée est engendrée par une exception de type
     * NoSuchDocumentException
     * 
     * @param ce
     *            Exception à tester (de type ClientException)
     * @return vrai si l'exception est a pour cause une NoSuchDocumentException
     */
    private boolean isNoSuchDocumentException(ClientException ce) {
        Throwable causeExc = ce.getCause();
        return causeExc instanceof NoSuchDocumentException;
    }

    private Blob createBlob(JSONArray json) {
        return new StringBlob(json.toString(), "application/json");
    }

    /**
     * Supprime le suffixe du nom d'un proxy.
     * 
     * @param path
     *            Chemin du proxy
     * @return le path avec le nom du proxy sans le suffixe
     */
    public static String computeNavPath(String path) {
        String result = path;
        if (path.endsWith(SUFFIXE_PROXY))
            result = result.substring(0, result.length() - SUFFIXE_PROXY.length());
        return result;
    }

    /**
     * Indique si l'objet en entrée correspond à une erreur.
     * 
     * @param operationRes
     *            résultat d'une opération Nuxeo ou d'une méthode
     * @return vrai si l'objet en entrée correspond à une erreur
     */
    private static boolean isError(Object operationRes) {
        return (!(operationRes instanceof DocumentModel) && !(operationRes instanceof Boolean));
    }

    /**
     * Classe permettant de "tracer" une erreur serveur.
     */
    public static class ServeurException extends Exception {

        private static final long serialVersionUID = -2490817493963408580L;

        ServeurException() {
            super();
        }

        ServeurException(Exception e) {
            super(e);
        }

    }

    /**
	 * 
	 */
    private static class UnrestrictedFecthPubliInfosRunner extends UnrestrictedSessionRunner {

        private DocumentModel document;
        private Object liveDocRes;
        private JSONObject infosPubli;
        private List<Integer> errorsCodes;
        private UserManager userManager;

        /**
         * @return the infosPubli
         */
        public JSONObject getInfosPubli() {
            return infosPubli;
        }

        /**
         * @return the errorsCodes
         */
        public List<Integer> getErrorsCodes() {
            return errorsCodes;
        }

        public UnrestrictedFecthPubliInfosRunner(CoreSession session, DocumentModel document, JSONObject infosPubli, UserManager userManager,
                List<Integer> errorsCodes) {
            super(session);
            this.document = document;
            this.liveDocRes = liveDocRes;
            this.infosPubli = infosPubli;
            this.errorsCodes = errorsCodes;
            this.userManager = userManager;
        }

        @Override
        public void run() throws ClientException {
            try {
                /*
                 * Récupération du spaceID
                 */
                this.infosPubli.put("spaceID", getSpaceID(this.document));

                /*
                 * Récupération du parentSpaceID
                 */
                String parentSpaceID = "";
                DocumentModelList spaceParentList = ToutaticeDocumentHelper.getParentSpaceList(this.session, this.document, true, true);
                if (spaceParentList != null && spaceParentList.size() > 0) {
                    DocumentModel parentSpace = (DocumentModel) spaceParentList.get(0);
                    parentSpaceID = getSpaceID(parentSpace);
                }
                this.infosPubli.put("parentSpaceID", parentSpaceID);

                /*
                 * Récupération du contexte propre à l'appel d'autres opérations
                 * Nuxeo
                 */
                AutomationService automation = null;
                try {
                    automation = Framework.getService(AutomationService.class);
                } catch (Exception e) {
                    log.warn("Error getting automation service, error: " + e.getMessage());
                    throw new ServeurException(e);
                }
                OperationContext ctx = new OperationContext(this.session);

                /* Appel à l'opération FetchPublishSpace */
                Map<String, Object> parameters = new HashMap<String, Object>();
                parameters.put("value", this.document);

                Object fetchPublishSpaceRes = null;
                try {
                    fetchPublishSpaceRes = ToutaticeDocumentHelper.callOperation(automation, ctx, "Document.FetchPublishSpace", parameters);
                    DocumentModel publishSpaceDoc = (DocumentModel) fetchPublishSpaceRes;
                    this.infosPubli.element("publishSpaceType", publishSpaceDoc.getType());
                    this.infosPubli.element("publishSpacePath", URLEncoder.encode(computeNavPath(publishSpaceDoc.getPathAsString()), "UTF-8"));
                    try {
                        this.infosPubli.element("publishSpaceDisplayName", URLEncoder.encode(publishSpaceDoc.getTitle(), "UTF-8"));
                        BooleanProperty property = getInContextualizationProperty(publishSpaceDoc);
                        this.infosPubli.element("publishSpaceInContextualization", property.getValue());
                    } catch (ClientException e) {
                        this.infosPubli.element("publishSpaceInContextualization", Boolean.FALSE);
                        this.errorsCodes = manageException(errorsCodes, publishSpaceDoc, e, ERROR_PUBLISH_SPACE_FORBIDDEN,
                                "fetch publish space name or contextualization property for space ");
                    }
                } catch (Exception e) {
                    this.errorsCodes.add(getErrorCode(e, ERROR_PUBLISH_SPACE_NOT_FOUND, ERROR_PUBLISH_SPACE_FORBIDDEN));
                    this.infosPubli.element("publishSpaceInContextualization", Boolean.FALSE);
                    this.infosPubli.element("publishSpaceType", "");
                    this.infosPubli.element("publishSpacePath", "");
                    this.infosPubli.element("publishSpaceDisplayName", "");
                }

                /* Récupération du workspace contenant le document */
                parameters.clear();
                parameters.put("document", document);
                Object workspaceRes = null;
                try {
                    workspaceRes = ToutaticeDocumentHelper.callOperation(automation, ctx, "Document.FetchWorkspaceOfDocument", parameters);
                    DocumentModel workspace = (DocumentModel) workspaceRes;
                    this.infosPubli.element("workspacePath", URLEncoder.encode(workspace.getPathAsString(), "UTF-8"));
                    try {
                        this.infosPubli.element("workspaceDisplayName", URLEncoder.encode(workspace.getTitle(), "UTF-8"));
                    } catch (ClientException e) {
                        this.errorsCodes = manageException(errorsCodes, workspace, e, ERROR_WORKSPACE_FORBIDDEN,
                                "fetch workspace name or contextualization property for workspace");
                    }

                } catch (Exception e) {
                    /* Cas d'erreur */
                    this.infosPubli.element("workspaceInContextualization", Boolean.FALSE);
                    this.infosPubli.element("workspacePath", "");
                    this.infosPubli.element("workspaceDisplayName", "");
                    this.errorsCodes.add(getErrorCode(e, ERROR_WORKSPACE_NOT_FOUND, ERROR_WORKSPACE_FORBIDDEN));
                }

                /* TODO: valeur toujours mise à true pour l'instant */
                this.infosPubli.element("workspaceInContextualization", Boolean.TRUE);

                /*
                 * Récupération du document publié et attribution du caractère
                 * 'publié'
                 */
                parameters.clear();
                parameters.put("value", document);

                DocumentModel publishedDoc = null;
                try {


                    publishedDoc = (DocumentModel) ToutaticeDocumentHelper.callOperation(automation, ctx, "Document.FetchPublished", parameters);

                    boolean equalVersion = publishedDoc.getVersionLabel().equals(infosPubli.get("liveVersion"));
                    this.infosPubli.element("isLiveModifiedFromProxy", !equalVersion);
                    this.infosPubli.element("proxyVersion", publishedDoc.getVersionLabel());
                    this.infosPubli.element("published", Boolean.TRUE);
                } catch (Exception e) {
                    this.infosPubli.element("isLiveModifiedFromProxy", true);
                    this.infosPubli.element("published", Boolean.FALSE);
                }

                Object isAnonymousRes = isAnonymous(this.session, this.userManager, this.document, this.infosPubli);
                if (isError(isAnonymousRes)) {
                    this.infosPubli = (JSONObject) isAnonymousRes;
                } else {
                    this.infosPubli.element("anonymouslyReadable", isAnonymousRes);
                }

            } catch (Exception e) {
                throw new ClientException(e);
            }

        }


        /**
         * Méthode permettant de vérifier si un document publié est accessible
         * de façon anonyme; met à faux le booléen anonymouslyReadable en cas
         * d'erreur.
         * 
         * @param session
         *            session Nuxeo
         * @param doc
         *            document dont on teste l'accès
         * @param infos
         *            pour stocker le résultat du test (booléen)
         * @return vrai si le document est accessible de façon anonyme
         * @throws ServeurException
         * @throws ClientException
         */
        private Object isAnonymous(CoreSession session, UserManager userManager, DocumentModel doc, JSONObject infos) throws ServeurException {
            boolean isAnonymous = false;

            try {
                ACP acp = this.document.getACP();
                String anonymousId = userManager.getAnonymousUserId();
                Access access = acp.getAccess(anonymousId, SecurityConstants.READ);
                isAnonymous = access.toBoolean();
            } catch (ClientException e) {
                if (e instanceof DocumentSecurityException) {
                    infos.element("anonymouslyReadable", Boolean.FALSE);
                    return infos;
                } else {
                    log.warn("Failed to get ACP of document '" + doc.getPathAsString() + "', error:" + e.getMessage());
                    throw new ServeurException(e);
                }
            }

            return isAnonymous;
        }

        /**
         * Récupère la proriété de contextualisation (schéma toutatice) d'un
         * document.
         * 
         * @param doc
         *            document donné
         * @return la valeur de la propriété sous forme de BooleanProperty
         * @throws PropertyException
         * @throws ClientException
         */
        private BooleanProperty getInContextualizationProperty(DocumentModel doc) throws PropertyException, ClientException {
            BooleanProperty property = (BooleanProperty) doc.getProperty(IN_CONTEXTUALIZATON_PROPERTY);
            return property;
        }

        /**
         * Gère le traitement d'une ClientException
         * 
         * @param errorsCodes
         *            pour stocker une erreur
         * @param doc
         *            pour générer un message dans les logs du serveur
         * @param ce
         *            exception à traiter
         * @param errorCode
         *            code d'erreur dans le cas d'une DocumentSecurityException
         *            (sous-classe de ClientException)
         * @param msg
         *            pour générer un message dans les logs du serveur
         * @throws ServeurException
         */
        private List<Integer> manageException(List<Integer> errorsCodes, DocumentModel doc, ClientException ce, int errorCode, String msg)
                throws ServeurException {
            if (ce instanceof DocumentSecurityException) {
                errorsCodes.add(errorCode);
            } else {
                log.warn("Failed" + msg + "'" + doc.getPathAsString() + "', error:" + ce.getMessage());
                throw new ServeurException(ce);
            }
            return errorsCodes;
        }

        // Règle de gestion de récupération du spaceID d'un document
        private String getSpaceID(DocumentModel document) {
            String spaceID = "";

            try {
                if (ToutaticeDocumentHelper.isASpaceDocument(document)) {
                    spaceID = document.getId();
                } else {
                    spaceID = safeString((String) document.getProperty("toutatice", "spaceID"));
                }
            } catch (ClientException e) {
                log.error("Failed to read the ttc:spaceID meta-data, error:" + e.getMessage());
            }

            return spaceID;
        }

    }

    /**
     * Classe permettant de filtrer une liste de "task instance" en fonction du nom
     */
    private class TaskInstanceFilter implements JbpmListFilter {

        private static final long serialVersionUID = 1L;
        private String taskName;

        public TaskInstanceFilter(String taskName) {
            this.taskName = taskName;
        }

        @SuppressWarnings("unchecked")
        public <T> ArrayList<T> filter(JbpmContext jbpmContext, DocumentModel document, ArrayList<T> list, NuxeoPrincipal principal) {
            ArrayList<TaskInstance> result = new ArrayList<TaskInstance>();

            // pas de filtrage si pas de nom passé en paramètre
            if (StringUtils.isBlank(this.taskName)) {
                return list;
            }

            // filtrage
            for (T t : list) {
                TaskInstance ti = (TaskInstance) t;
                String name = ti.getName();
                if (this.taskName.equals(name)) {
                    result.add(ti);
                }
            }

            return (ArrayList<T>) result;
        }
    }


    /**
     * Get doc by webid in unrestricted mode (admin)
     */
    private class UnrestrictedFecthWebIdRunner extends UnrestrictedSessionRunner {

        String webIdSegment;
        String domainIdSegment;
        DocumentModel docResolved;

        protected UnrestrictedFecthWebIdRunner(CoreSession session, String domainId, String webId) {
            super(session);
            this.webIdSegment = webId;
            this.domainIdSegment = domainId;

        }

        @Override
        public void run() throws ClientException {
            DocumentModelList docs = session.query(String.format(WEB_ID_QUERY, domainIdSegment, webIdSegment));
            if (docs.size() == 1) {
                docResolved = docs.get(0);
            } else if (docs.size() > 1) {
                throw new ClientException("Two or more documents have the webid : " + webid);
            }
        }

        public DocumentModel getDoc() {
            return docResolved;
        }

    }

    private static String safeString(String value) {
        String safeValue = value;
        if (value == null) {
            safeValue = "";
        }
        return safeValue;
    }

}
