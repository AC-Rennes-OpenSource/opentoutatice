/**
 * 
 */
package fr.toutatice.ecm.plarform.web.filemanager.zip;


/**
 * @author david
 */
public class ToutaticeZipFormatException extends Exception {

    private static final long serialVersionUID = 1L;
    
    public static final String MSG_PROP = "Property %s is not well defined: [%s]. Must be of the form <integer> - Zip build aborted";

    public ToutaticeZipFormatException(){
        super(String.format(MSG_PROP, ToutaticeZipExporterUtils.MAX_SIZE_PROP, ToutaticeZipExporterUtils.MAX_SIZE_PROP));
    }
    
}
