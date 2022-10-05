/**
 * 
 */
package fr.toutatice.ecm.platform.automation.document;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;



/**
 * @author david
 *
 */
public abstract class AbstractDublinCoreDocumentUpdate {
    
    /** DublinCore schema prefix. */
    protected static final String DUBLINCORE_SCHEMA_PREFIX = "dc:";
    /** Title property key. */
    protected static final String PROP_TITLE = "dc:title";

    /**
     * Extract properties of DublinCore schema from given properties.
     * 
     * @param properties
     * @return properties of DublinCore schema
     */
    protected Properties getDublinCoreProperties(Properties properties){
        Properties dcProperties = new Properties();
        
        if(properties != null){
            for(Entry<String, String> property : properties.entrySet()){
                if(StringUtils.contains(property.getKey(), DUBLINCORE_SCHEMA_PREFIX)){

                    // dc properties are updated silently, except the title who is seen as a major modification
                    if(!StringUtils.equalsIgnoreCase(property.getKey(), "dc:title")) {
                        dcProperties.put(property.getKey(), property.getValue());
                    }
                }
            }
        }
        return dcProperties;
    }
    
    /**
     * Treats creation or update taking DublinCore properties into account.
     * 
     * @param session
     * @param document
     * @param properties
     * @param save
     * @return created or updated document
     * @throws ClientException
     * @throws IOException
     */
    protected DocumentModel executeSplittingProperties(CoreSession session, DocumentModel document, Properties properties, boolean save) throws ClientException, IOException{
     // Get Dublincore properties to save them silently
        // (to disable DublinCoreListener)
        Properties dublinCoreProperties = getDublinCoreProperties(properties);
        
        if(dublinCoreProperties.size() > 0){
            // Remove DublinCore entries from original properties
            Set<String> propertiesKeys = properties.keySet();
            propertiesKeys.removeAll(dublinCoreProperties.keySet());
            
            // Treat document with given DublincoreProperties
            document = execute(session, document, properties, dublinCoreProperties, save);
            
        } else {
            // Treats document normally
            document = execute(session, document, properties, save);
        }
        
        return document;
    }
    
    /**
     * Creates or updates document
     * 
     * @param session
     * @param document
     * @param properties
     * @param save
     * @return created or updated document
     * @throws ClientException
     * @throws IOException
     */
    protected abstract DocumentModel execute(CoreSession session, DocumentModel document, Properties properties, boolean save) throws ClientException, IOException;
    
    /**
     * Creates or updates document
     * 
     * @param session
     * @param document
     * @param properties
     * @param dublinCoreProperties
     * @param save
     * @return created or updated document
     * @throws ClientException
     * @throws IOException
     */
    protected abstract DocumentModel execute(CoreSession session, DocumentModel document, Properties properties, Properties dublinCoreProperties, boolean save) throws ClientException, IOException;

}
