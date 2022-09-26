/**
 * 
 */
package fr.toutatice.ecm.plarform.web.filemanager.zip;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * @author david
 */
public class ToutaticeZipExporterUtils {
    
    protected static final Pattern SPACE_LEFT_PATTERN = Pattern.compile("^([0-9]+)([%]{1})$");
    public static final String MAX_SIZE_PROP = "ottc.export.zip.tmp.limit.percent";
    
    public static double getTmpSpaceLeftPercent() {
        return getSpaceLeftPercent(Framework.getProperty("nuxeo.tmp.dir"));
    }
    
    public static double getSpaceLeftPercent(String tmpFolderPath) {
        File tmpFolder = new File(tmpFolderPath);
        return (Double.valueOf(tmpFolder.getFreeSpace()) / Double.valueOf(tmpFolder.getTotalSpace())) * 100;
    }
    
    public static double getTmpPossibleSpaceLeftPercent() throws ToutaticeZipLimitException {
        return getPossibleSpaceLeftPercent(Framework.getProperty(MAX_SIZE_PROP));
    }
    
    public static double getPossibleSpaceLeftPercent(String possibleSpaceLeft) throws ToutaticeZipLimitException {
        double res = -1;
        
        if(StringUtils.isNotBlank(possibleSpaceLeft)) {
            Matcher matcher = SPACE_LEFT_PATTERN.matcher(possibleSpaceLeft);
            if(matcher.matches()) {
                res = Double.valueOf(matcher.group(1));
            } else {
                throw ToutaticeZipLimitException.property(MAX_SIZE_PROP);
            }
        }
        
        return res;
    }
    
    public static boolean stillTmpSpaceLeft() throws ToutaticeZipLimitException {
        return (getTmpSpaceLeftPercent() > getTmpPossibleSpaceLeftPercent());
    }
    
    
    
    
    
    
    
    
    
    
    
  

}
