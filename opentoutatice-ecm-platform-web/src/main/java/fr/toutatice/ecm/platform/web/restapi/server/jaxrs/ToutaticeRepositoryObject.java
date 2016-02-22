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
package fr.toutatice.ecm.platform.web.restapi.server.jaxrs;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.restapi.server.jaxrs.RepositoryObject;
import org.nuxeo.ecm.webengine.model.WebObject;

import fr.toutatice.ecm.platform.service.url.ToutaticeDocumentResolver;
import fr.toutatice.ecm.platform.service.url.WedIdRef;

@WebObject(type = "toutatice")
public class ToutaticeRepositoryObject extends RepositoryObject {

	@Path("web/{web}")
	public Object getDocsByWebId(@PathParam("web") String web) throws ClientException {
		DocumentModelList list = null;
		try {
			CoreSession session = getContext().getCoreSession();
			list = ToutaticeDocumentResolver.resolveReference(session, new WedIdRef(null, web, null));
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		return newObject("Document", list.get(0));
	}

}
