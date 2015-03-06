package fr.toutatice.ecm.platform.core.services.fetchinformation;


import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;

@XObject("fetchInfos")
public class FetchInformationsDescriptor implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = -2001913048962270285L;

	protected FetchInformationProvider provider;

    @XNode("@providerService")
    protected Class<?> providerService;

    public void initProvider() throws Exception {
        provider = (FetchInformationProvider) Framework.getService(providerService);
    }

    /**
     * @return the instance
     */
    public FetchInformationProvider getInstance() {
        return provider;
    }
}
