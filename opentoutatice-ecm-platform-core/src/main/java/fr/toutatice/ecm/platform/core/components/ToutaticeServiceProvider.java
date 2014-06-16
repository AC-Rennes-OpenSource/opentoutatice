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
 *   mberhaut1
 *    
 */
package fr.toutatice.ecm.platform.core.components;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.DefaultServiceProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.ServiceProvider;

import fr.toutatice.ecm.platform.core.services.proxyfactory.ProxyFactoryCfgService;

/**
 * Lorsque le framework sera sollicité (session::getLocalService()) pour obtenir une instance 
 * d'un service, il obtiendra un proxy sur ce dernier (implémenté par une classe qui étend
 * la classe abstraite "ToutaticeAbstractServiceHandler". 
 * Le filtrage peut être nomminatif pour un utilisateur connecté.
 * 
 * @author mberhaut1
 */
public class ToutaticeServiceProvider implements ServiceProvider {

	private static final Log log = LogFactory.getLog(ToutaticeServiceProvider.class);
	
	private boolean installed;
	private ServiceProvider nextProvider;
	private static ToutaticeServiceProvider instance = null;
	private static List<String> filteredUsersList = null;
	
	protected final Map<Class<?>, Entry<?>> registry = new HashMap<Class<?>, Entry<?>>();

	@SuppressWarnings("rawtypes")
	private ProxyFactoryCfgService pfsService;

	// singleton
	private ToutaticeServiceProvider() {
		installed = false;
		filteredUsersList = Collections.synchronizedList(new ArrayList<String>());
	}
	
	public static ToutaticeServiceProvider instance() {
		if (null == instance) {
			instance = new ToutaticeServiceProvider();
		}
		
		return instance;
	}
	
	public void install() {
		if (installed) {
			return;
		}
		
		installed = true;
		nextProvider = DefaultServiceProvider.getProvider();
		DefaultServiceProvider.setProvider(instance);
	}

	public void uninstall() {
		DefaultServiceProvider.setProvider(nextProvider);
		installed = false;
	}
	
	public void register(String principalId) {
		synchronized (filteredUsersList) {
			/**
			 * For multi-threading purpose, the expression to test whether the user is already registered is commented.
			 * Hence, multiple asynchronous processing ran for one connected user will keep in silent mode. The first thread
			 * to unregister won't unregister the other threads. 
			 */
//			if (!filteredUsersList.contains(principalId)) {
				filteredUsersList.add(principalId);
//			}
		}
	}

	public void unregister(String principalId) {
		synchronized (filteredUsersList) {
			filteredUsersList.remove(principalId);
		}
	}

	public boolean isRegistered(String principalId) {
		return filteredUsersList.contains(principalId);
	}

	@Override
	public <T> T getService(Class<T> srvClass) {
		if (!registry.containsKey(srvClass)) {
			registry.put(srvClass, new Entry<T>(srvClass));
		}

		return srvClass.cast(registry.get(srvClass).getService());
	}

	@SuppressWarnings("rawtypes")
	private ProxyFactoryCfgService getProxyFactoryService() {
		if (null == this.pfsService) {
			this.pfsService = nextProvider != null ? nextProvider.getService(ProxyFactoryCfgService.class) : Framework.getRuntime().getService(ProxyFactoryCfgService.class);
		}
		return this.pfsService;
	}

	private class Entry<T> {
		final Class<T> srvClass;

		protected Entry(Class<T> srvClass) {
			this.srvClass = srvClass;
		}

		@SuppressWarnings("unchecked")
		public T getService() {
			T srvObject = nextProvider != null ? nextProvider.getService(srvClass) : Framework.getRuntime().getService(srvClass);
			
			try {
				ProxyFactoryCfgService<T> pfs = getProxyFactoryService();
				Class<?> handler = pfs.getServiceHandler(srvClass);
				if (null != handler) {
					Object ho = handler.newInstance();
					Method themethod = handler.getMethod("newProxy", Object.class, srvClass.getClass());
					return (T) themethod.invoke(ho, srvObject, srvClass);
				}
			} catch (Exception e) {
				log.error("Failed to instanciate the service proxy '" + this.srvClass.getName() + " (the native Nuxeo service will be used instead)', error: " + e.getMessage());
			}
			
			return srvObject;			
		}

	}

}
