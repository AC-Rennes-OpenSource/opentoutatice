package fr.toutatice.ecm.platform.web.fragments.configuration;

import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;

/**
 * Bean for getting configuration informations
 */
@Name("config")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = Install.DEPLOYMENT)
public class ConfigurationBeanHelper {


    /** nagivation context for nuxeo queries */
    @In(create = true)
    protected transient NavigationContext navigationContext;

    /**
     * Get the current JSF instance of this class
     * 
     * @return the instance
     */
    public static ConfigurationBeanHelper getBean() {
        FacesContext context = FacesContext.getCurrentInstance();
        return (ConfigurationBeanHelper) context.getApplication().evaluateExpressionGet(context, "#{config}", ConfigurationBeanHelper.class);
    }

    /**
     * List of configurations in nuxeo
     * 
     * @return webvconfigurations describing the allowed types of templates the user can set.
     */
    private DocumentModelList getConfigs(String confType) {

        try {
            CoreSession session = navigationContext.getOrCreateDocumentManager();

            // get current document
            FacesContext context = FacesContext.getCurrentInstance();
            DocumentModel doc = (DocumentModel) context.getApplication().evaluateExpressionGet(context, "#{currentDocument}", DocumentModel.class);
            String type = doc.getType();
            String confPath = null;

            // compute domain path
            DocumentModel child = null;
            while (!(type.equals("Domain")) && doc!=child) {
                child = doc;
                doc = session.getDocument(doc.getParentRef());
                if (doc != null) {
                    type = doc.getType();
                    
                    if (type.equals("Domain")) {
                        confPath = doc.getPath().toString();
                    }
                }

            }

            // select conf objects that are enabled
            String query = "select * from Document " + "where ecm:primaryType = 'WebConfiguration'  " + " AND wconf:type =  '" + confType
                    + "' AND wconf:enabled=1  " + " AND ecm:mixinType != 'HiddenInNavigation'  AND ecm:currentLifeCycleState <> 'deleted'  ";

            // if domain is found, query only conf who is belong to it 
            if (confPath != null) {
                query = query.concat(" AND ecm:path STARTSWITH '" + confPath + "' ");
            }

            // order by the setted conf order
            query = query.concat("ORDER BY wconf:order ");
            DocumentModelList configurations = session.query(query);

            return configurations;


        } catch (ClientException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 
     * @return a list of pages templates allowed in the cms mode
     */
    public DocumentModelList getPageTemplates() {
        return getConfigs("pagetemplate");
    }

    /**
     * 
     * @return a list of fragment types who can be created in cms mode
     */
    public DocumentModelList getFragmentTypes() {
        return getConfigs("fragmenttype");
    }

    /**
     * 
     * @return a list of css window style who can be setted on the CMS windows
     */
    public DocumentModelList getWindowStyles() {
        return getConfigs("windowstyle");
    }

    /**
     * 
     * @return a list of templates who can be setted on the list
     */
    public DocumentModelList getListTemplates() {
        return getConfigs("listtemplate");
    }

    /**
     * List of configurations in nuxeo
     * 
     * @return webconfig of portlet
     */
    public List getFragmentOptionsByCode(DocumentModel doc, String code2) {

        try {
            CoreSession session = navigationContext.getOrCreateDocumentManager();

            // get current document
            FacesContext context = FacesContext.getCurrentInstance();

            String type = doc.getType();
            String confPath = null;

            // compute domain path
            DocumentModel child = null;
            while (!(type.equals("Domain")) && doc != child) {
                child = doc;
                doc = session.getDocument(doc.getParentRef());
                if (doc != null) {
                    type = doc.getType();

                    if (type.equals("Domain")) {
                        confPath = doc.getPath().toString();
                    }
                }

            }

            // select conf objects that are enabled
            String query = "select * from Document " + "where ecm:primaryType = 'WebConfiguration'  " + " AND wconf:type =  'fragmenttype'"
                    + " AND wconf:enabled=1  " + " AND ecm:mixinType != 'HiddenInNavigation'  AND ecm:currentLifeCycleState <> 'deleted'  " +
 " AND wconf:code2 = '" + code2 + "'";

            // if domain is found, query only conf who is belong to it
            if (confPath != null) {
                query = query.concat(" AND ecm:path STARTSWITH '" + confPath + "' ");
            }

            DocumentModelList configurations = session.query(query);

            if (configurations.get(0) != null) {
                DocumentModel config = configurations.get(0);

                Map<String, Object> properties = config.getProperties("webconfiguration");

                if(properties.containsKey("wconf:options") && properties.get("wconf:options") != null)
                    return (List) properties.get("wconf:options");

            }


        } catch (ClientException e) {
            e.printStackTrace();
        }

        return null;
    }
}
