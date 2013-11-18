package fr.toutatice.ecm.platform.core.helper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;

import fr.toutatice.ecm.platform.core.utils.exception.ToutaticeException;

public class ToutaticeOperationHelper {

	private static final Log log = LogFactory.getLog(ToutaticeOperationHelper.class);

    private static AutomationService automationService;

	private ToutaticeOperationHelper() {
		// static class, cannot be instantiated
	}

	public static void runOperationChain(CoreSession session, String chainId, Object document) throws ToutaticeException {
		OperationContext context = new OperationContext(session);
		context.setInput(document);
		runOperationChain(session, chainId, context);
    }
	
	public static void runOperationChain(CoreSession session, String chainId, OperationContext context) throws ToutaticeException {
        try {
        	AutomationService automationService = getAutomationService();
            automationService.run(context, chainId);
        } catch (Exception e) {
        	DocumentModel document = (DocumentModel) context.getInput();
        	log.error("Failed to run the operation '" + chainId + "' on document '" + document.getName() + "', error: " + e.getMessage());
        	throw new ToutaticeException(e);
        }
    }

	private static AutomationService getAutomationService() throws ClientException {
		if (automationService == null) {
			try {
				automationService = Framework.getService(AutomationService.class);
			} catch (Exception e) {
				final String errMsg = "Error connecting to AutomationService. " + e.getMessage();
				log.error(errMsg);
				throw new ClientException(errMsg, e);
			}
			
			if (automationService == null) {
				String errMsg = "AutomationService service not bound";
				log.error(errMsg);
				throw new ClientException(errMsg);
			}
		}
		return automationService;
	}

}
