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
 * dchevrier
 * lbillon
 */
package fr.toutatice.ecm.platform.web.userservices;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.webapp.contentbrowser.DocumentActions;

import fr.toutatice.ecm.platform.automation.SetWebID;
import fr.toutatice.ecm.platform.core.constants.ExtendedSeamPrecedence;
import fr.toutatice.ecm.platform.web.document.ToutaticeDocumentActionsBean;


/**
 * @author David Chevrier
 */
@Name("toutaticeValidator")
@Scope(ScopeType.SESSION)
@Install(precedence = ExtendedSeamPrecedence.TOUTATICE)
public class ToutaticeValidatorBean implements Serializable {

    private static final long serialVersionUID = -6136329151688755416L;

    private static final Log log = LogFactory.getLog(ToutaticeValidatorBean.class);

    @In(create = true, required = true)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected transient DocumentActions documentActions;


    final Pattern patternId = Pattern.compile("([a-zA-Z_0-9\\-\\_]+)");
    final Pattern patternExplicit = Pattern.compile("([a-zA-Z_0-9\\-\\/]+)");

    /**
     * Validate webId value.
     * 
     * @param context
     * @param component
     * @param value
     * @throws ValidatorException
     */
    public void validateWebId(FacesContext context, UIComponent component, Object value) throws ValidatorException {

        String webId = (String) value;
        if (StringUtils.isNotBlank(webId)) {
            String msg = null;

            // format control
            Matcher m = this.patternId.matcher(webId);
            if (!m.matches()) {
                msg = ComponentUtils.translate(context, "label.toutatice.validator.malformed.webid");
            } else {

                // unicity control
                DocumentModel doc = null;
                try {
                    doc = ((ToutaticeDocumentActionsBean) this.documentActions).getCurrentDocument();

                    if (doc != null) {

                        if (SetWebID.UnrestrictedSilentSetWebIdRunner.isNotUnique(documentManager, doc, webId)) {
                            msg = ComponentUtils.translate(context, "label.toutatice.validator.webid.no.unicity");
                        }

                    } else {
                        msg = ComponentUtils.translate(context, "label.toutatice.validator.no.doc");
                    }
                } catch (ClientException ce) {
                    msg = ce.getMessage();
                }

            }

            if (msg != null) {
                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null);
                throw new ValidatorException(message);
            }
        }
    }

    /**
     * 
     * @param context
     * @param component
     * @param value
     * @throws ValidatorException
     */
    public void validateExplicitUrl(FacesContext context, UIComponent component, Object value) throws ValidatorException {


        String explicitUrl = (String) value;
        if (StringUtils.isNotBlank(explicitUrl)) {
            String msg = null;

            // format control
            Matcher m = patternExplicit.matcher(explicitUrl);
            if (!m.matches()) {
                msg = ComponentUtils.translate(context, "label.toutatice.validator.malformed.explicit");
            }

            if (msg != null) {
                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null);
                throw new ValidatorException(message);
            }
        }
    }

}
