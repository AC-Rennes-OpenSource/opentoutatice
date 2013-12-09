package fr.toutatice.ecm.platform.service.fragments.types;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

import fr.toutatice.ecm.platform.service.fragments.FragmentServiceException;
import fr.toutatice.ecm.platform.web.fragments.configuration.ConfigurationBeanHelper;

/**
 * 
 * Service dédié aux listes
 * 
 */
public class PortletFragment implements Fragment {

    private static final String PORTLET_SCHEMA = "portlet_fragments";

    private static final String PORTLET_PROPERTIES = "portlet_properties";


    @Override
    public String prepareCreation(DocumentModel doc, String uri, String region, String belowUri, String code2) throws FragmentServiceException {

        try {

            // fragments portlet
            Map<String, Object> schPortlets = doc.getProperties(PORTLET_SCHEMA);

            Collection<Object> values = schPortlets.values();

            Object liste = values.iterator().next();

            if (liste instanceof List) {
                List<Map<String, String>> listeData = (List<Map<String, String>>) liste;

                Map<String, String> newEntry = new HashMap<String, String>();

                newEntry.put("refURI", uri);
                newEntry.put("portletInstance", code2);

                listeData.add(newEntry);

                doc.setProperties(PORTLET_SCHEMA, schPortlets);
            }


            // portlet properties
            Map<String, Object> portletProperties = doc.getProperties(PORTLET_PROPERTIES);

            values = portletProperties.values();

            liste = values.iterator().next();

            if (liste instanceof List) {
                List<Map<String, String>> listeData = (List<Map<String, String>>) liste;

                ConfigurationBeanHelper configBean = ConfigurationBeanHelper.getBean();
                List options = configBean.getFragmentOptionsByCode(doc, code2);

                if (options != null) {
                    for (Object optionObj : options) {
                        Map<String, String> option = (Map) optionObj;
                        Map<String, String> newEntry = new HashMap<String, String>();

                        newEntry.put("refURI", uri);
                        newEntry.put("key", option.get("propertyName"));
                        newEntry.put("value", option.get("propertyDefaultValue"));

                        listeData.add(newEntry);
                    }

                    doc.setProperties(PORTLET_PROPERTIES, portletProperties);
                }
            }


        } catch (ClientException e) {
            throw new FragmentServiceException(e);
        }
        return uri;
    }

}
