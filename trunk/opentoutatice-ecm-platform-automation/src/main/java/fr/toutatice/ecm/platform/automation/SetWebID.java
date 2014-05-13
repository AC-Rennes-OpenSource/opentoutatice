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
 * lbillon
 */
package fr.toutatice.ecm.platform.automation;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;

import fr.toutatice.ecm.platform.core.constants.ToutaticeNuxeoStudioConst;
import fr.toutatice.ecm.platform.core.helper.ToutaticeDocumentHelper;

/**
 * Generate or apply a webId on a document. Check if the webId is unique in the domain
 * 
 * @author loic
 * 
 */
@Operation(id = SetWebID.ID, category = Constants.CAT_DOCUMENT, label = "Set webid.",
        description = "Check unicity of webid and apply to the document in current domain..")
public class SetWebID {

    /** Op ID */
    public static final String ID = "Document.SetWebId";

    private static final Log log = LogFactory.getLog(SetWebID.class);


    private static final String CREATE_OP = "createOp";

    private static final String WEB_ID_UNICITY_QUERY = "select * from Document Where ttc:domainID = '%s'"
            + " AND ttc:webid = '%s' AND ecm:uuid <> '%s' AND ecm:isProxy = 0 AND ecm:currentLifeCycleState!='deleted' AND ecm:isCheckedInVersion = 0";


    @Context
    protected CoreSession coreSession;

    @In(create = true)
    protected NavigationContext navigationContext;


    @Param(name = "chainSource", required = true)
    protected String chainSource;

    /**
     * Main method
     * 
     * @param doc
     * @return docment modified
     * @throws Exception
     */
    @OperationMethod()
    public DocumentModel run(DocumentModel doc) throws Exception {
        String webid = null;
        String extension = null;
        boolean hasToBeUpdated = false;

        // if document has not toutatice schema
        if (!doc.hasSchema(ToutaticeNuxeoStudioConst.CST_DOC_SCHEMA_TOUTATICE)) {
            return doc;
        }


        // if space does not supports webid or if we can not verify it.
        if (!isSpaceSupportsWebId(doc)) {

            Object currentWebId = doc.getPropertyValue(ToutaticeNuxeoStudioConst.CST_DOC_SCHEMA_TOUTATICE_WEBID);
            if (currentWebId != null) { // in case of import, copy, move or restauration
                if (StringUtils.isNotEmpty(currentWebId.toString())) {

                    // blank the value of the webid document
                    doc.setPropertyValue(ToutaticeNuxeoStudioConst.CST_DOC_SCHEMA_TOUTATICE_WEBID, StringUtils.EMPTY);
                    this.coreSession.saveDocument(doc);

                }
            }

            return doc;
        }


        // webid setted in the document, we use it
        if (doc.getPropertyValue(ToutaticeNuxeoStudioConst.CST_DOC_SCHEMA_TOUTATICE_WEBID) != null) {
            webid = doc.getPropertyValue(ToutaticeNuxeoStudioConst.CST_DOC_SCHEMA_TOUTATICE_WEBID).toString();

            // clean if needed
            webid = IdUtils.generateId(webid, "-", true, 24);
        }
        // else in creation or import, try to generate it.
        else if (CREATE_OP.equals(chainSource)) {
            // else new id is ganarated
            String[] arrayPath = doc.getPathAsString().split("/");
            webid = arrayPath[arrayPath.length - 1];

            // for Files or Pictures : put the extension of the file if exists
            if ("File".equals(doc.getType()) || "Picture".equals(doc.getType())) {
                int lastIndexOf = doc.getTitle().lastIndexOf(".");
                if (lastIndexOf > -1) {
                    extension = doc.getTitle().substring(lastIndexOf + 1, doc.getTitle().length());

                    if (webid.endsWith(extension)) {
                        webid = webid.substring(0, webid.length() - extension.length() - 1);
                    }
                }

            }

            hasToBeUpdated = true;
        }


        Object domainID = doc.getPropertyValue(ToutaticeNuxeoStudioConst.CST_DOC_XPATH_TOUTATICE_DOMAIN_ID);

        // if webid is defined
        if ((webid != null && webid.length() > 0) && domainID != null) {

            if (StringUtils.isNotEmpty(domainID.toString())) {

                // [others ops like move, restore, ...] don't throw an exception, put a suffix after the id
                boolean unicity = true;
                Integer suffix = null;
                String webidconcat = webid;
                do {

                    DocumentModelList query = coreSession.query(String.format(WEB_ID_UNICITY_QUERY, domainID.toString(), webidconcat, doc.getId()));

                    if (query.size() > 0) {
                        unicity = false;
                        if (suffix == null)
                            suffix = 1;
                        else
                            suffix = suffix + 1;
                        webidconcat = webid.concat(suffix.toString());
                    } else {
                        unicity = true;

                        if (!webid.equals(webidconcat)) {
                            webid = webidconcat;
                            hasToBeUpdated = true;
                        }
                    }
                } while (!unicity);

                // save weburl
                if (hasToBeUpdated) {
                    log.warn("Id relocated to " + webid + " for document " + doc.getPathAsString());
                    doc.setPropertyValue(ToutaticeNuxeoStudioConst.CST_DOC_SCHEMA_TOUTATICE_WEBID, webid);
                    if (extension != null) {
                        doc.setPropertyValue(ToutaticeNuxeoStudioConst.CST_DOC_XPATH_TOUTATICE_EXTENSION_URL, extension);
                    }
                    this.coreSession.saveDocument(doc);
                }

                // }
            }
        }

        return doc;
    }

    /**
     * Z
     * Get the parent space and look at the property "ttcs:hasWebIdEnabled"
     * 
     * @param doc
     * @return true if webid are enabled
     * @throws PropertyException
     * @throws ClientException
     */
    private boolean isSpaceSupportsWebId(DocumentModel doc) throws PropertyException, ClientException {
        // check if document belong to a space whose supports webid
        boolean spaceSupportsWebId = true;
        DocumentModelList spaces = ToutaticeDocumentHelper.getParentSpaceList(coreSession, doc, false, true, true);
        if (spaces.size() > 0) {

            DocumentModel space = spaces.get(0);
            Property hasWebIdEnabled = space.getProperty(ToutaticeNuxeoStudioConst.CST_DOC_XPATH_TOUTATICESPACE_WEBID_ENABLED);

            if (hasWebIdEnabled != null) {
                if (hasWebIdEnabled.getValue(Boolean.class) == false) {
                    spaceSupportsWebId = false;
                }
            } else {
                spaceSupportsWebId = false; // param in space is set to false
            }
        } else {
            spaceSupportsWebId = false; // space is not found
        }
        return spaceSupportsWebId;
    }
}
