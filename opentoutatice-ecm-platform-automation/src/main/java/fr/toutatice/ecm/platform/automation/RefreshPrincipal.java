/*
 * (C) Copyright 2014 Académie de Rennes (http://www.ac-rennes.fr/), OSIVIA (http://www.osivia.com) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * Contributors:
 * mberhaut1
 */
package fr.toutatice.ecm.platform.automation;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.plexus.util.StringUtils;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

@Operation(id = RefreshPrincipal.ID, category = Constants.CAT_USERS_GROUPS, label = "Refresh princial", description = "Refresh the principal (to take into consideration LDAP updates)")
public class RefreshPrincipal {
	private static final Log log = LogFactory.getLog(RefreshPrincipal.class);
	public static final String ID = "Document.RefreshPrincipal";

	@Context
	protected OperationContext ctx;

	@OperationMethod
	public void run() throws Exception {
		try {
			final NuxeoPrincipal principal = (NuxeoPrincipal) ctx.getPrincipal();
			final UserManager userManager = Framework.getService(UserManager.class);
			final DirectoryService directoryService = Framework.getLocalService(DirectoryService.class);

			/**
			 * Refer to Jira for implementation: https://jira.nuxeo.com/browse/SUPNXP-10432
			 */

			// flush the user (LDAP) directories cache
			final String userDirectoryName = userManager.getUserDirectoryName();
			final Directory userDirectory = directoryService.getDirectory(userDirectoryName);

			// REDMINE #8590 : issues with non-lower case username (multiple cache entries must be invalidated)
			final String userName = principal.getName();
			final String userNameLowerCase = StringUtils.lowerCase(principal.getName());

			final List<String> cacheKeys = new ArrayList<>();
			cacheKeys.add(userName);
			if (!StringUtils.equals(userName, userNameLowerCase)) {
				cacheKeys.add(userNameLowerCase);
			}
			userDirectory.getCache().invalidate(cacheKeys);

			// refresh the principal (rebuild its data model)
			final DocumentModel um = userManager.getUserModel(principal.getName());
			principal.setModel(um);
		} catch (final Exception e) {
			log.warn("Failed to refresh the principal, error: " + e.getMessage());
			throw new ClientException(e);
		}
	}

	@OperationMethod(collector = DocumentModelCollector.class)
	public DocumentModel run(DocumentModel doc) throws Exception {
		run();
		return ctx.getCoreSession().getDocument(doc.getRef());
	}

	@OperationMethod(collector = DocumentModelCollector.class)
	public DocumentModel run(DocumentRef docRef) throws Exception {
		run();
		return ctx.getCoreSession().getDocument(docRef);
	}

}
