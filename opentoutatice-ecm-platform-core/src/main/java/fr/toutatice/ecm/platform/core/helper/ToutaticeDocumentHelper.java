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
 * mberhaut1
 * dchevrier
 * lbillon
 * sjahier
 */
package fr.toutatice.ecm.platform.core.helper;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationNotFoundException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.impl.InvokableMethod;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.impl.LifeCycleFilter;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.model.NoSuchDocumentException;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;

import fr.toutatice.ecm.platform.core.constants.ToutaticeGlobalConst;
import fr.toutatice.ecm.platform.core.constants.ToutaticeNuxeoStudioConst;

public class ToutaticeDocumentHelper {

	private static final Log log = LogFactory.getLog(ToutaticeDocumentHelper.class);
	private static final String MEDIALIB = "MediaLibrary";

	private ToutaticeDocumentHelper() {
		// static class, cannot be instantiated
	}

	/**
	 * @param session
	 * @param id
	 * @return a document fetched with unrestricted session.
	 */
	public static DocumentModel getUnrestrictedDocument(CoreSession session, String id) throws ClientException {
		final GetUnrestrictedDocument getter = new GetUnrestrictedDocument(session, id);
		getter.runUnrestricted();
		return getter.getDocument();
	}

	/**
	 * Allows to get a document in an unrestricted way.
	 */
	public static class GetUnrestrictedDocument extends UnrestrictedSessionRunner {

		private String id;
		private DocumentModel document;

		public DocumentModel getDocument() {
			return document;
		}

		public GetUnrestrictedDocument(CoreSession session, String id) {
			super(session);
			this.id = id;
		}

		@Override
		public void run() throws ClientException {
			if (StringUtils.isNotBlank(id)) {
				DocumentRef ref = null;
				if (!StringUtils.contains(id, "/")) {
					ref = new IdRef(id);
				} else {
					ref = new PathRef(id);
				}
				document = session.getDocument(ref);
			}

		}
	}
	
	public static DocumentModel getUnrestrictedWorkingCopy(CoreSession session, String proxyId) throws ClientException {
        final GetUnrestrictedWorkingCopy getter = new GetUnrestrictedWorkingCopy(session, proxyId);
        getter.runUnrestricted();
        return getter.getWorkingCopy();
    }
	
	/**
     * Allows to get a working copy of proxy in an unrestricted way.
     */
    public static class GetUnrestrictedWorkingCopy extends UnrestrictedSessionRunner {

        private String proxyId;
        private DocumentModel workingCopy;

        public DocumentModel getWorkingCopy() {
            return workingCopy;
        }

        public GetUnrestrictedWorkingCopy(CoreSession session, String id) {
            super(session);
            this.proxyId = id;
        }

        @Override
        public void run() throws ClientException {
            if (StringUtils.isNotBlank(proxyId)) {
                DocumentRef ref = null;
                if (!StringUtils.contains(proxyId, "/")) {
                    ref = new IdRef(proxyId);
                } else {
                    ref = new PathRef(proxyId);
                }
                workingCopy = session.getWorkingCopy(ref);
            }

        }
    }
	
	/**
	 * Gets parent's document without checking permissions.
	 *
	 * @param document
	 * @return parent's document
	 */
	public static DocumentModel getUnrestrictedParent(CoreSession session, DocumentModel document) {
		// Parent
		DocumentModel parent = null;

		if (document != null) {
			final GetUnresrictedParent runner = new GetUnresrictedParent(session, document);
			runner.runUnrestricted();

			parent = runner.get();
		}

		return parent;
	}

	/**
	 * Gets parent's document without checking permissions.
	 *
	 * @param document
	 * @return parent's document
	 */
	public static DocumentModel getUnrestrictedParent(DocumentModel document) {
		// Parent
		DocumentModel parent = null;

		if (document != null) {
			final GetUnresrictedParent runner = new GetUnresrictedParent(document.getCoreSession(), document);
			runner.runUnrestricted();

			parent = runner.get();
		}

		return parent;
	}

	/**
	 * Allows getting parent's document without checking permissions.
	 */
	public static class GetUnresrictedParent extends UnrestrictedSessionRunner {

		// Document
		private DocumentModel document;
		// Parent
		private DocumentModel parent;

		protected GetUnresrictedParent(CoreSession session, DocumentModel document) {
			super(session);
			this.document = document;
		}

		@Override
		public void run() throws ClientException {
			parent = session.getParentDocument(document.getRef());
		}

		// Getter for parent
		public DocumentModel get() {
			return parent;
		}

	}
	
	public static Serializable getUnrestrictedProperty(CoreSession session, String docId, String xpathProperty, boolean getValue) {
		GetUnrestrictedProperty getter = new GetUnrestrictedProperty(session, docId, xpathProperty, getValue);
		getter.runUnrestricted();
		return getter.getProperty();
	}
	
	public static Serializable getUnrestrictedProperty(CoreSession session, String docId, String xpathProperty) {
		GetUnrestrictedProperty getter = new GetUnrestrictedProperty(session, docId, xpathProperty, true);
		getter.runUnrestricted();
		return getter.getProperty();
	}

	public static class GetUnrestrictedProperty extends UnrestrictedSessionRunner {

		private String docId;
		private String xpathProperty;
		private boolean getValue;

		private Serializable property;

		protected GetUnrestrictedProperty(CoreSession session, String docId, String xpathProperty, boolean getValue) {
			super(session);
			this.docId = docId;
			this.xpathProperty = xpathProperty;
			this.getValue = getValue;
		}

		@Override
		public void run() throws ClientException {
			DocumentModel document = super.session.getDocument(new IdRef(this.docId));
			if(this.getValue) {
				this.property = document.getPropertyValue(this.xpathProperty);
			} else {
				this.property = document.getProperty(this.xpathProperty);
			}
		}

		public Serializable getProperty() {
			return property;
		}

	}

	/**
	 * Save a document in an silent unrestricted or not way: EventService and
	 * VersioningService are bypassed.
	 */
	public static void saveDocumentSilently(CoreSession session, DocumentModel document, boolean unrestricted) {
		final SilentSave save = new SilentSave(session, document);
		save.silentRun(unrestricted, ToutaticeGlobalConst.EVENT_N_VERSIONING_FILTERD_SERVICE);
	}

	/**
	 * Save a document bypassing given services.
	 *
	 * @param session
	 * @param document
	 * @param services
	 * @param unrestricted
	 */
	public static void saveDocumentSilently(CoreSession session, DocumentModel document, List<Class<?>> services,
			boolean unrestricted) {
		final SilentSave save = new SilentSave(session, document);
		save.silentRun(unrestricted, services);
	}

	/**
	 * Save document with no versioning.
	 */
	public static void saveDocumentWithNoVersioning(CoreSession session, DocumentModel document, boolean unrestricted) {
		final SilentSave save = new SilentSave(session, document);
		save.silentRun(unrestricted, ToutaticeGlobalConst.VERSIONING_FILTERD_SERVICE);
	}

	/**
	 * Remiove a document in an silent unrestricted or not way: EventService and
	 * VersioningService are bypassed.
	 */
	public static void removeDocumentSilently(CoreSession session, DocumentModel document, boolean unrestricted) {
		final SilentRemoveRunner deleter = new SilentRemoveRunner(session, document.getRef());
		deleter.silentRun(unrestricted, ToutaticeGlobalConst.EVENT_N_VERSIONING_FILTERD_SERVICE);
	}

	/**
	 * Save a document in an silent way.
	 */
	public static class SilentSave extends ToutaticeSilentProcessRunnerHelper {

		private DocumentModel document;

		protected SilentSave(CoreSession session, DocumentModel document) {
			super(session);
			this.document = document;
		}

		@Override
		public void run() throws ClientException {
			session.saveDocument(document);
		}

	}

	/**
	 * Delete a document silently.
	 */
	public static class SilentRemoveRunner extends ToutaticeSilentProcessRunnerHelper {

		/** Ref of doc to delete. */
		private DocumentRef docRef;

		public SilentRemoveRunner(CoreSession session, DocumentRef docRef) {
			super(session);
			this.docRef = docRef;
		}

		@Override
		public void run() throws ClientException {
			session.removeDocument(docRef);
		}

	}

	/**
	 *
	 * @param document
	 * @return true if document is an empty document model (creation).
	 */
	public static boolean isEmptyDocumentModel(DocumentModel document) {
		return document.getId() == null;
	}

	/**
	 * Retourne la dernière version valide du document passé en paramètre.
	 *
	 * @param document
	 * @param session
	 * @return
	 */
	public static DocumentModel getLatestDocumentVersion(DocumentModel document, CoreSession session) {
		DocumentModel latestDoc = null;

		if ((null != document) && (null != session)) {
			try {
				if (ToutaticeNuxeoStudioConst.CST_DOC_STATE_APPROVED.equals(document.getCurrentLifeCycleState())) {
					latestDoc = document;
				} else {
					List<DocumentModel> versionDocList;
					versionDocList = session.getVersions(document.getRef());
					Collections.sort(versionDocList, new DocumentVersionComparator());

					for (int i = 0; i < versionDocList.size(); i++) {
						final DocumentModel versionDoc = versionDocList.get(i);
						if (ToutaticeNuxeoStudioConst.CST_DOC_STATE_APPROVED
								.equals(versionDoc.getCurrentLifeCycleState())) {
							latestDoc = versionDoc;
							break;
						}
					}
				}
			} catch (final ClientException e) {
				log.debug("Failed to get the latest version of the document '" + document.getName() + "', error: "
						+ e.getMessage());
			}
		}

		return latestDoc;
	}

	/**
	 * Retourne un objet de type VersionModel à partir du document (version) passé
	 * en paramètre.
	 *
	 * @param version
	 * @return
	 * @throws DocumentException
	 */
	public static VersionModel getVersionModel(DocumentModel version) throws DocumentException {
		final VersionModel versionModel = new VersionModelImpl();
		versionModel.setId(version.getId());
		versionModel.setLabel(version.getVersionLabel());
		return versionModel;
	}

	public static class DocumentVersionComparator implements Comparator<DocumentModel> {

		private DocumentVersionComparator() {
		}

		@Override
		public int compare(DocumentModel arg0, DocumentModel arg1) {
			int result = 0;
			if ((null != arg0) && (null != arg1)) {
				result = (isNewer(arg0, arg1) == true) ? 1 : -1;
			}

			return result;
		}

		/**
		 * Vérifie que le document de 'comparaison' à été modifié après le document de
		 * référence.
		 *
		 * @param ref
		 *            le document 'de référence'
		 * @param comp
		 *            le document 'de comparaison'
		 * @return true si c'est le cas. false sinon.
		 */
		public static boolean isNewer(DocumentModel ref, DocumentModel comp) {
			boolean isNewer = false;

			try {
				final Calendar ref_modified = (GregorianCalendar) ref.getPropertyValue("dc:modified");
				final Calendar comp_modified = (GregorianCalendar) comp.getPropertyValue("dc:modified");
				isNewer = comp_modified.after(ref_modified);
			} catch (final ClientException e) {
				log.debug("Failed to determine wich document is the latest modified");
			}

			return isNewer;
		}

		public static boolean isBigger(String v0, String v1) {
			final String[] sV0 = v0.split("\\.");
			final String[] sV1 = v1.split("\\.");
			final int majorV0 = Integer.parseInt(sV0[0]);
			final int minorV0 = Integer.parseInt(sV0[1]);
			final int majorV1 = Integer.parseInt(sV1[0]);
			final int minorV1 = Integer.parseInt(sV1[1]);

			return ((majorV0 > majorV1) || ((majorV0 == majorV1) && (minorV0 > minorV1)));
		}
	}

	/**
	 * Récupérer la liste des parents d'un document.
	 *
	 * @param session
	 *            la session courante de l'utilisateur
	 * @param document
	 *            le document pour lequel il faut rechercher les parents
	 * @param filter
	 *            un filtre pour filter les parents à convenance
	 * @param runInUnrestrictedMode
	 *            Est-ce que cette opération de recherche des parents doit être
	 *            exécutée en mode unrestricted (session System) ou bien avec la
	 *            session de l'utilisateur courant?
	 * @return La liste des parents filtrée
	 */
	public static DocumentModelList getParentList(CoreSession session, DocumentModel document, Filter filter,
			boolean runInUnrestrictedMode) {
		return getParentList(session, document, filter, runInUnrestrictedMode, false);
	}

	/**
	 * Récupérer la liste des parents d'un document.
	 *
	 * @param session
	 *            la session courante de l'utilisateur
	 * @param document
	 *            le document pour lequel il faut rechercher les parents
	 * @param filter
	 *            un filtre pour filter les parents à convenance
	 * @param runInUnrestrictedMode
	 *            Est-ce que cette opération de recherche des parents doit être
	 *            exécutée en mode unrestricted (session System) ou bien avec la
	 *            session de l'utilisateur courant?
	 * @param immediateOnly
	 *            est-ce qu'il faut retourner uniquement le parent immédiat
	 *            satisfaisant le filtre
	 * @return La liste des parents filtrée
	 */
	public static DocumentModelList getParentList(CoreSession session, DocumentModel document, Filter filter,
			boolean runInUnrestrictedMode, boolean immediateOnly) {
		return getParentList(session, document, filter, runInUnrestrictedMode, immediateOnly, false);
	}

	/**
	 * Récupérer la liste des parents d'un document.
	 *
	 * @param session
	 *            la session courante de l'utilisateur
	 * @param document
	 *            le document pour lequel il faut rechercher les parents
	 * @param filter
	 *            un filtre pour filter les parents à convenance
	 * @param runInUnrestrictedMode
	 *            Est-ce que cette opération de recherche des parents doit être
	 *            exécutée en mode unrestricted (session System) ou bien avec la
	 *            session de l'utilisateur courant?
	 * @param immediateOnly
	 *            est-ce qu'il faut retourner uniquement le parent immédiat
	 *            satisfaisant le filtre
	 * @param thisInluded
	 *            est-ce que le document courant est examiné s'il est un folder
	 * @return La liste des parents filtrée
	 */
	public static DocumentModelList getParentList(CoreSession session, DocumentModel document, Filter filter,
			boolean runInUnrestrictedMode, boolean immediateOnly, boolean thisInluded) {
		DocumentModelList parent = null;

		try {
			final UnrestrictedGetParentsListRunner runner = new UnrestrictedGetParentsListRunner(session, document,
					filter, immediateOnly, thisInluded);
			if (runInUnrestrictedMode) {
				runner.runUnrestricted();
			} else {
				runner.run();
			}
			parent = runner.getParentList();
		} catch (final ClientException e) {
			log.warn("Failed to get the parent for the current document, error: " + e.getMessage());
		}

		return parent;
	}

	/**
	 * Récupérer la liste des "spaces" parents d'un document.
	 *
	 * @param session
	 *            la session courante de l'utilisateur
	 * @param document
	 *            le document pour lequel il faut rechercher les parents
	 * @param runInUnrestrictedMode
	 *            Est-ce que cette opération de recherche des parents doit être
	 *            exécutée en mode unrestricted (session System) ou bien avec la
	 *            session de l'utilisateur courant? immediateOnly est-ce qu'il faut
	 *            retourner uniquement l'espace parent immédiat
	 * @return la liste des "spaces" parents d'un document
	 */
	public static DocumentModelList getParentSpaceList(CoreSession session, DocumentModel document,
			boolean runInUnrestrictedMode, boolean immediateOnly) {
		return getParentSpaceList(session, document, runInUnrestrictedMode, immediateOnly, false);
	}

	/**
	 * Récupérer la liste des "spaces" parents d'un document.
	 *
	 * @param session
	 *            la session courante de l'utilisateur
	 * @param document
	 *            le document pour lequel il faut rechercher les parents
	 * @param runInUnrestrictedMode
	 *            Est-ce que cette opération de recherche des parents doit être
	 *            exécutée en mode unrestricted (session System) ou bien avec la
	 *            session de l'utilisateur courant?
	 * @param immediateOnly
	 *            est-ce qu'il faut retourner uniquement l'espace parent immédiat?
	 * @param thisIncluded
	 *            est-ce que le document courant doit être examiné?
	 * @return la liste des "spaces" parents d'un document
	 */
	public static DocumentModelList getParentSpaceList(CoreSession session, DocumentModel document,
			boolean runInUnrestrictedMode, boolean immediateOnly, boolean thisIncluded) {
		final Filter filter = new Filter() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean accept(DocumentModel document) {
				boolean res = false;
				if (isASpaceDocument(document)) {
					res = true;
				}
				return res;
			}
		};

		return ToutaticeDocumentHelper.getParentList(session, document, filter, runInUnrestrictedMode, immediateOnly,
				thisIncluded);
	}

	/**
	 * @param session
	 *            la session courante de l'utilisateur
	 * @param document
	 *            le document pour lequel il faut rechercher le parent
	 * @param lstXpaths
	 *            les xpaths des propriétés qu'il faut retourner
	 * @param filter
	 *            filtre déterminant le parent à consulter
	 * @param runInUnrestrictedMode
	 *            opération doit-être exécuter en mode unrestricted
	 * @param thisIncluded
	 *            le document courant est examiné
	 * @return une map de &lt;xpath, property&gt; du document parent
	 */
	public static Map<String, Property> getPropertiesParentDoc(CoreSession session, DocumentModel document,
			List<String> lstXpaths, Filter filter, boolean runInUnrestrictedMode, boolean thisIncluded) {
		Map<String, Property> mapPpty = null;
		try {
			final GetParentPropertiesRunner runner = new GetParentPropertiesRunner(session, document, lstXpaths, filter,
					runInUnrestrictedMode, thisIncluded);
			if (runInUnrestrictedMode) {
				runner.runUnrestricted();
			} else {
				runner.run();
			}
			mapPpty = runner.getProperties();
		} catch (final ClientException e) {
			log.warn("Failed to get the parent for the current document, error: " + e.getMessage());
		}

		return mapPpty;
	}

	/**
	 * Gets parent of document of Workspace type.
	 *
	 * @param session
	 * @param document
	 * @param unrestricted
	 * @return parent of document of Workspace type
	 */
	public static DocumentModel getWorkspace(CoreSession session, DocumentModel document, boolean unrestricted) {
		@SuppressWarnings("serial")
		final Filter wsFiler = new Filter() {

			@Override
			public boolean accept(DocumentModel docModel) {
				return ToutaticeNuxeoStudioConst.CST_DOC_TYPE_WORKSPACE.equals(docModel.getType());
			}
		};
		final DocumentModelList parentWsList = getParentList(session, document, wsFiler, unrestricted, true, true);
		if ((parentWsList != null) && (parentWsList.size() > 0)) {
			return parentWsList.get(0);
		} else {
			return null;
		}
	}

	/**
	 *
	 * @param session
	 * @param document
	 * @return
	 * @throws ClientException
	 * @throws PropertyException
	 */
	public static String getSpaceID(CoreSession session, DocumentModel document, boolean runInUnrestrictedMode)
			throws ClientException, PropertyException {
		String spaceId = ""; // le document courant n'appartient pas à un space

		// si UserWorspace => spaceId = dc:title (conversion en minuscule afin de
		// pouvoir utiliser l'indexation sur cette méta-donnée)
		if (ToutaticeNuxeoStudioConst.CST_DOC_TYPE_USER_WORKSPACE.equals(document.getType())) {
		    // SpaceId can have yet be filled
		    String spaceId_  = (String) document.getPropertyValue(ToutaticeNuxeoStudioConst.CST_DOC_SCHEMA_TOUTATICE_SPACEID);
			spaceId = StringUtils.isNotBlank(spaceId_) ? spaceId_ : document.getTitle().toLowerCase();
		} else {
			// sinon récupérer la liste des spaceParents
			final DocumentModelList spaceParentList = getParentSpaceList(session, document, runInUnrestrictedMode,
					true);

			if ((spaceParentList != null) && !spaceParentList.isEmpty()) {
				// prendre le 1er parent de type space rencontré
				final DocumentModel space = spaceParentList.get(0);

				if (ToutaticeNuxeoStudioConst.CST_DOC_TYPE_USER_WORKSPACE.equals(space.getType())) {
					// si le type de ce space est UserWorkspace => spaceID = dc:title
				    // SpaceId can have yet be filled
		            String spaceId_  = (String) space.getPropertyValue(ToutaticeNuxeoStudioConst.CST_DOC_SCHEMA_TOUTATICE_SPACEID);
		            spaceId = StringUtils.isNotBlank(spaceId_) ? spaceId_ : space.getTitle().toLowerCase();
				} else {
					// sinon spaceID = space.getId
					spaceId = space.getId();
				}
			}
		}

		return spaceId;
	}

	/**
	 * @param session
	 *            la session courante de l'utilisateur
	 * @param document
	 *            le document pour lequel il faut rechercher les parents
	 * @param runInUnrestrictedMode
	 *            Est-ce que cette opération de recherche des parents doit être
	 *            exécutée en mode unrestricted (session System) ou bien avec la
	 *            session de l'utilisateur courant?
	 * @return le domain parent du document courant
	 * @throws ClientException
	 */
	public static DocumentModel getDomain(CoreSession session, DocumentModel document, boolean runInUnrestrictedMode)
			throws ClientException {
		DocumentModel domain = null;

		// sinon récupérer la liste des spaceParents
		@SuppressWarnings("serial")
		final DocumentModelList DomainList = getParentList(session, document, new Filter() {

			@Override
			public boolean accept(DocumentModel docModel) {
				return ToutaticeNuxeoStudioConst.CST_DOC_TYPE_DOMAIN.equals(docModel.getType());
			}
		}, runInUnrestrictedMode, true, true);

		if ((null != DomainList) && !DomainList.isEmpty()) {
			domain = DomainList.get(0);
		}

		return domain;
	}

	/**
	 * Récupérer la liste des "espaces de publication (locale)" parents d'un
	 * document.
	 *
	 * @param session
	 *            la session courante de l'utilisateur
	 * @param document
	 *            le document pour lequel il faut rechercher les parents
	 * @param runInUnrestrictedMode
	 *            Est-ce que cette opération de recherche des parents doit être
	 *            exécutée en mode unrestricted (session System) ou bien avec la
	 *            session de l'utilisateur courant?
	 * @param immediateOnly
	 *            est-ce qu'il faut retourner uniquement l'espace de publication
	 *            parent immédiat
	 * @return la liste des "spaces" parents d'un document
	 */
	public static DocumentModelList getParentPublishSpaceList(CoreSession session, DocumentModel document,
			boolean runInUnrestrictedMode, boolean immediateOnly) {
		final Filter filter = new Filter() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean accept(DocumentModel document) {
				boolean status = false;

				try {
					status = ToutaticeDocumentHelper.isAPublicationSpaceDocument(document);
				} catch (final Exception e) {
					log.error("Failed to filter the publish space documents, error: " + e.getMessage());
					status = false;
				}

				return status;
			}
		};

		return ToutaticeDocumentHelper.getParentList(session, document, filter, runInUnrestrictedMode, immediateOnly,
				true);
	}

	public static String[] filterLifeCycleStateDocuments(CoreSession session, String[] sectionIdList,
			List<String> acceptedStates, List<String> excludedStates) {
		final List<String> filteredSectionsList = new ArrayList<>();

		try {
			if ((null != sectionIdList) && (0 < sectionIdList.length)) {
				final Filter lcFilter = new LifeCycleFilter(acceptedStates, excludedStates);

				for (final String sectionId : sectionIdList) {
					final DocumentModel section = session.getDocument(new IdRef(sectionId));
					if (lcFilter.accept(section)) {
						filteredSectionsList.add(sectionId);
					}
				}
			}
		} catch (final ClientException e) {
			log.error("Failed to filter the active sections, error: " + e.getMessage());
		}

		return filteredSectionsList.toArray(new String[filteredSectionsList.size()]);
	}

	/**
	 * Retourne le proxy d'un document s'il existe. Recherche réalisée en MODE
	 * UNRESTRICTED. La recherche est effectuée uniquement sur le périmètre du
	 * container direct du document (local). Si une permission est précisée en
	 * paramètre elle sera contrôlée.
	 *
	 * @param session
	 *            la session utilisateur connecté courant
	 * @param document
	 *            le document pour lequel la recherche est faite
	 * @return le proxy associé ou null si aucun proxy existe ou une exception de
	 *         sécurité si les droits sont insuffisants.
	 * @throws ClientException
	 */
	public static DocumentModel getProxy(CoreSession session, DocumentModel document, String permission)
			throws ClientException {
		return getProxy(session, document, permission, true);
	}

	/**
	 * Retourne le proxy d'un document s'il existe. La recherche est effectuée
	 * uniquement sur le périmètre du container direct du document (local). Si une
	 * permission est précisée en paramètre elle sera contrôlée.
	 *
	 * @param session
	 *            la session utilisateur connecté courant
	 * @param document
	 *            le document pour lequel la recherche est faite
	 * @param unrestricted
	 *            true si le proxy doit être récupéré en mode unrestricted. False
	 *            s'il doit être récupéré avec la session utilisateur courante
	 * @return le proxy associé ou null si aucun proxy existe ou une exception de
	 *         sécurité si les droits sont insuffisants.
	 * @throws ClientException
	 */
	public static DocumentModel getProxy(CoreSession session, DocumentModel document, String permission,
			boolean unrestricted) throws ClientException {
		DocumentModel proxy = null;

		final DocumentModelList proxies = getProxies(session, document,
				ToutaticeGlobalConst.CST_TOUTATICE_PROXY_LOOKUP_SCOPE.LOCAL, permission, unrestricted);
		if ((null != proxies) && (0 < proxies.size())) {
			proxy = proxies.get(0);
			if (StringUtils.isNotBlank(permission) && !session.hasPermission(proxy.getRef(), permission)) {
				final Principal principal = session.getPrincipal();
				throw new DocumentSecurityException("The user '" + principal.getName() + "' has not the permission '"
						+ permission + "' on the proxy of document '" + document.getTitle() + "'");
			}
		}

		return proxy;
	}

	/**
	 * Retourne les proxies d'un document s'ils existent.
	 *
	 * @param session
	 *            la session utilisateur connecté courant
	 * @param document
	 *            le document pour lequel la recherche est faite
	 * @param scope
	 *            le périmètre de la recherche: enumeration
	 *            {@link ToutaticeGlobalConst.CST_TOUTATICE_PROXY_LOOKUP_SCOPE}
	 * @param unrestricted
	 *            true si le proxy doit être récupéré en mode unrestricted. False
	 *            s'il doit être récupéré avec la session utilisateur courante
	 * @return le proxy associé ou null si aucun proxy existe ou une exception de
	 *         sécurité si les droits sont insuffisants.
	 * @throws ClientException
	 */
	public static DocumentModelList getProxies(CoreSession session, DocumentModel document,
			ToutaticeGlobalConst.CST_TOUTATICE_PROXY_LOOKUP_SCOPE scope, String permission, boolean unrestricted)
			throws ClientException {
		DocumentModelList proxies = null;

		if (null != document) {
			if (document.isProxy()) {
				proxies = new DocumentModelListImpl();
				proxies.add(document);
			} else {
				final UnrestrictedGetProxyRunner runner = new UnrestrictedGetProxyRunner(session, document, scope);
				if (unrestricted) {
					runner.runUnrestricted();
				} else {
					runner.run();
				}
				proxies = runner.getProxies();
			}
		}

		return proxies;
	}

	/**
	 * Retourne la version d'un proxy de document s'il existe.
	 *
	 * @param session
	 *            la session utilisateur connecté courant
	 * @param document
	 *            le document pour lequel la recherche est faite
	 * @return la version du proxy associé ou null si aucun proxy existe
	 * @throws ClientException
	 */
	public static String getProxyVersion(CoreSession session, DocumentModel document) throws ClientException {
		String proxyVersion = null;

		final DocumentModel proxy = getProxy(session, document, null);
		if (null != proxy) {
			final UnrestrictedGetProxyVersionLabelRunner runner = new UnrestrictedGetProxyVersionLabelRunner(session,
					proxy);
			runner.runUnrestricted();
			proxyVersion = runner.getVersionLabel();
		}

		return proxyVersion;
	}

	public static boolean isVisibleInPortal(DocumentModel doc, CoreSession session) throws ClientException {
		boolean res = false;

		// le document est en ligne ?
		res = (null != getProxy(session, doc, SecurityConstants.READ));

		if (!res) {
			// le document est dans un workspace ou est en attente de publication dans un
			// PortalSite
			final DocumentModelList spaceDocsList = ToutaticeDocumentHelper.getParentSpaceList(session, doc, true, true,
					true);
			if ((spaceDocsList != null) && !spaceDocsList.isEmpty()) {
				final DocumentModel space = spaceDocsList.get(0);
				res = ToutaticeDocumentHelper.isAWorkSpaceLikeDocument(space)
						|| ToutaticeDocumentHelper.isAPublicationSpaceDocument(space);
			}
		}

		return res;
	}

	public static boolean isASpaceDocument(DocumentModel document) {
		return document.hasFacet(ToutaticeNuxeoStudioConst.CST_FACET_SPACE);
	}

	public static boolean isASuperSpaceDocument(DocumentModel document) {
		return document.hasFacet(ToutaticeNuxeoStudioConst.CST_FACET_SUPERSPACE);
	}

	public static boolean isAPublicationSpaceDocument(DocumentModel document) {
		boolean status = false;

		if (document.hasFacet(ToutaticeNuxeoStudioConst.CST_DOC_FACET_TTC_PUBLISH_SPACE)) {
			status = true;
		}

		return status;
	}

	/**
	 *
	 * @param document
	 * @return true if document is Workspace or extends it.
	 */
	// FIXME: UserWorkspace test to move in AcRennes.
	public static boolean isAWorkSpaceLikeDocument(DocumentModel document) {
		return ToutaticeNuxeoStudioConst.CST_DOC_TYPE_WORKSPACE.equals(document.getType())
				|| ToutaticeNuxeoStudioConst.CST_DOC_TYPE_USER_WORKSPACE.equals(document.getType())
				|| isSubTypeOf(document, ToutaticeNuxeoStudioConst.CST_DOC_TYPE_WORKSPACE);
	}

	/**
	 * Checks if document is of Workspace type.
	 *
	 * @param document
	 * @return true if document is of Workspace type
	 */
	public static boolean isWorkspace(DocumentModel document) {
		return ToutaticeNuxeoStudioConst.CST_DOC_TYPE_WORKSPACE.equals(document.getType());
	}

	/**
	 *
	 * @param document
	 * @param type
	 * @return true if document extends a document with givent type.
	 */
	public static boolean isSubTypeOf(DocumentModel document, String type) {
		final DocumentType documentType = document.getDocumentType();
		if (documentType != null) {
			final Type superType = documentType.getSuperType();
			if (superType != null) {
				return StringUtils.equals(type, superType.getName());
			}
		}

		return false;
	}

	/**
	 *
	 * @param session
	 * @param document
	 * @return true if document is in publish space.
	 */
	public static boolean isInPublishSpace(CoreSession session, DocumentModel document) {
		final DocumentModelList parentPublishSpaceList = getParentPublishSpaceList(session, document, true, true);
		return CollectionUtils.isNotEmpty(parentPublishSpaceList);
	}

	/**
	 *
	 * @param session
	 * @param document
	 * @return true if document is in WorkSpace like space
	 */
	public static boolean isInWorkspaceLike(CoreSession session, DocumentModel document) {
		final DocumentModelList spaceDocsList = ToutaticeDocumentHelper.getParentSpaceList(session, document, true,
				true, true);
		if (CollectionUtils.isNotEmpty(spaceDocsList)) {
			final DocumentModel space = spaceDocsList.get(0);
			return ToutaticeDocumentHelper.isAWorkSpaceLikeDocument(space);
		}
		return false;
	}

	@SuppressWarnings("unused")
	private static class UnrestrictedGetProxyRunner extends UnrestrictedSessionRunner {

		private DocumentModel document;
		private DocumentModelList proxies;
		private ToutaticeGlobalConst.CST_TOUTATICE_PROXY_LOOKUP_SCOPE scope;

		public UnrestrictedGetProxyRunner(CoreSession session, DocumentModel document) {
			this(session, document, ToutaticeGlobalConst.CST_TOUTATICE_PROXY_LOOKUP_SCOPE.LOCAL);
		}

		public UnrestrictedGetProxyRunner(CoreSession session, DocumentModel document,
				ToutaticeGlobalConst.CST_TOUTATICE_PROXY_LOOKUP_SCOPE scope) {
			super(session);
			proxies = null;
			this.document = document;
			this.scope = scope;
		}

		public DocumentModel getProxy() throws ClientException {
			return ((null != proxies) && (0 < proxies.size())) ? proxies.get(0) : null;
		}

		public DocumentModelList getProxies() throws ClientException {
			return ((null != proxies) && (0 < proxies.size())) ? proxies : null;
		}

		@Override
		public void run() throws ClientException {
			if ((null == scope) || ToutaticeGlobalConst.CST_TOUTATICE_PROXY_LOOKUP_SCOPE.GLOBAL.equals(scope)) {
				// lookup all proxies of the document (wherever they are placed in the
				// repository)
				proxies = session.getProxies(document.getRef(), null);
			} else {
				// lookup only the proxies of the document placed in the parent folder
				proxies = session.getProxies(document.getRef(), document.getParentRef());
			}
		}

	}

	private static class UnrestrictedGetProxyVersionLabelRunner extends UnrestrictedSessionRunner {

		private DocumentModel proxy;
		private String versionLabel;

		public UnrestrictedGetProxyVersionLabelRunner(CoreSession session, DocumentModel proxy) {
			super(session);
			this.proxy = proxy;
			versionLabel = null;
		}

		@Override
		public void run() throws ClientException {
			String srcDocID = proxy.getSourceId();
			// For compatibility with content views managed by ES
			// (JsonDocumentModelReader#getDocumentModel
			// has sid null when fetchFromEs is true
			if (srcDocID == null) {
				final DocumentModel sourceDocument = session.getSourceDocument(proxy.getRef());
				srcDocID = sourceDocument.getId();
			}
			final DocumentModel srcDoc = session.getDocument(new IdRef(srcDocID));
			versionLabel = srcDoc.getVersionLabel();
		}

		public String getVersionLabel() {
			return versionLabel;
		}

	}

	private static class GetParentPropertiesRunner extends UnrestrictedSessionRunner {

		private DocumentModel doc;
		private Filter filter;
		private boolean included;
		private boolean runInUnrestrictedMode;
		private List<String> lstxpath;
		private Map<String, Property> mapPpties;

		public GetParentPropertiesRunner(CoreSession session, DocumentModel document, List<String> lstXpaths,
				Filter filter, boolean runInUnrestrictedMode, boolean included) {
			super(session);
			doc = document;
			this.filter = filter;
			lstxpath = lstXpaths;
			this.included = included;
			this.runInUnrestrictedMode = runInUnrestrictedMode;
		}

		public Map<String, Property> getProperties() {
			return mapPpties;
		}

		@Override
		public void run() throws ClientException {
			final DocumentModelList lstParent = getParentList(session, doc, filter, runInUnrestrictedMode, true,
					included);
			DocumentModel parent = null;
			if ((lstParent != null) && !lstParent.isEmpty()) {
				parent = lstParent.get(0);
			}
			if (parent != null) {
				mapPpties = new HashMap<>(lstxpath.size());
				for (final String xpath : lstxpath) {
					mapPpties.put(xpath, parent.getProperty(xpath));
				}
			}
		}
	}

	private static class UnrestrictedGetParentsListRunner extends UnrestrictedSessionRunner {

		private DocumentModel baseDoc;
		private DocumentModelList parentDocList;
		private Filter filter = null;
		private boolean immediateOnly;
		private boolean thisIncluded;

		public DocumentModelList getParentList() {
			return parentDocList;
		}

		protected UnrestrictedGetParentsListRunner(CoreSession session, DocumentModel document, Filter filter,
				boolean immediateOnly, boolean thisIncluded) {
			super(session);
			baseDoc = document;
			this.filter = filter;
			parentDocList = new DocumentModelListImpl();
			this.immediateOnly = immediateOnly;
			this.thisIncluded = thisIncluded;
		}

		@Override
		public void run() throws ClientException {

			if (thisIncluded && baseDoc.isFolder()) {
				if (null != filter) {
					if (filter.accept(baseDoc)) {
						parentDocList.add(baseDoc);
					}
				} else {
					parentDocList.add(baseDoc);
				}
			}

			final DocumentRef[] parentsRefList = session.getParentDocumentRefs(baseDoc.getRef());
			if ((null != parentsRefList) && (parentsRefList.length > 0)) {
				for (final DocumentRef parentsRef : parentsRefList) {
					final DocumentModel parent = session.getDocument(parentsRef);
					if (null != filter) {
						if (filter.accept(parent)) {
							parentDocList.add(parent);
						}
					} else {
						parentDocList.add(parent);
					}

					if (immediateOnly && (parentDocList.size() == 1)) {
						break;
					}
				}
			}
		}
	}

	/**
	 * Récupérer la liste des documents similaires dans un dossier.
	 *
	 * @param session
	 *            la session courante de l'utilisateur
	 * @param document
	 *            le document pour lequel il faut rechercher les parents
	 * @param filter
	 *            un filtre pour ajouter des critères de contrôle supplémentaires
	 *
	 * @return le compte des documents similaires dans le dossier.
	 */
	public static int getSameDocsCount(CoreSession session, DocumentModel document, Filter filter) {
		int count = 0;

		try {
			final UnrestrictedGetSameDocsListRunner runner = new UnrestrictedGetSameDocsListRunner(session, document,
					filter);
			runner.runUnrestricted();
			count = runner.getCount();
		} catch (final ClientException e) {
			log.error("Failed to get the same document of one folder, error: " + e.getMessage());
		}

		return count;
	}

	private static class UnrestrictedGetSameDocsListRunner extends UnrestrictedSessionRunner {

		private int count;
		private Filter filter = null;
		private DocumentModel document;

		public int getCount() {
			return count;
		}

		protected UnrestrictedGetSameDocsListRunner(CoreSession session, DocumentModel document, Filter filter) {
			super(session);
			count = 0;
			this.filter = filter;
			this.document = document;
		}

		@Override
		public void run() throws ClientException {
			final String docTitle = document.getTitle();
			final DocumentModel docParent = session.getParentDocument(document.getRef());
			final DocumentModelList rs = session.query("SELECT * FROM " + document.getType() + " WHERE ecm:parentId = '"
					+ docParent.getId() + "' " + "AND ecm:mixinType != 'HiddenInNavigation' "
					+ "AND ecm:isCheckedInVersion = 0 " + "AND dc:title LIKE '" + docTitle.replace("'", "\\'") + "%'",
					filter);

			count = rs.size();
		}
	}

	/**
	 *
	 * @param session
	 * @param doc
	 * @param aclName
	 *            par défaut Local
	 * @param filter
	 * @return
	 * @throws ClientException
	 */
	public static ACL getDocumentACL(CoreSession session, DocumentModel doc, String aclName,
			ToutaticeFilter<ACE> filter) throws ClientException {
		final ACL res = new ACLImpl();

		if (StringUtils.isBlank(aclName)) {
			aclName = ACL.LOCAL_ACL;
		}

		final ACP acp = doc.getACP();
		ACL[] aclList = null;
		if ("*".equals(aclName)) {
			aclList = acp.getACLs();
		} else {
			final ACL acl = acp.getACL(aclName);
			if (null != acl) {
				aclList = new ACLImpl[1];
				aclList[0] = acl;
			}
		}

		if (null != aclList) {
			for (final ACL acl : aclList) {
				for (final ACE ace : acl.getACEs()) {
					if ((filter == null) || filter.accept(ace)) {
						// ajouter les permissions au document doc
						res.add(ace);
					}
				}
			}
		}

		return res;
	}

	/**
	 * ajout une ace sur un document
	 *
	 * @param session
	 * @param ref
	 * @param ace
	 * @throws ClientException
	 */
	public static void setACE(CoreSession session, DocumentRef ref, ACE ace) throws ClientException {
		final ACPImpl acp = new ACPImpl();
		final ACLImpl acl = new ACLImpl(ACL.LOCAL_ACL);
		acp.addACL(acl);
		acl.add(ace);

		session.setACP(ref, acp, false);
	}

	/**
	 * @param document
	 *            le document sur lequel porte le contrôle
	 * @param viewId
	 *            l'identifiant de la vue
	 * @return true si le document porte la vue dont l'identifiant est passé en
	 *         paramètre
	 */
	public static boolean hasView(DocumentModel document, String viewId) {
		boolean status = false;

		if (null != document) {
			final TypeInfo typeInfo = document.getAdapter(TypeInfo.class);
			final String chosenView = typeInfo.getView(viewId);
			status = (chosenView != null);
		}

		return status;
	}

	/**
	 * Méthode permettant d'appeler une opération Nuxeo..
	 * 
	 * @param automation
	 *            Service automation
	 * @param ctx
	 *            Contexte d'exécution
	 * @param operationId
	 *            identifiant de l'opération
	 * @param parameters
	 *            paramètres de l'opération
	 * @return le résultat de l'opération dont le type n'est pas connu à priori
	 * @deprecated use
	 *             {@link ToutaticeOperationHelper#callOperation(OperationContext, String, Map)}
	 *             instead.
	 */
	@Deprecated
	public static Object callOperation(AutomationService automation, OperationContext ctx, String operationId,
			Map<String, Object> parameters) throws Exception {
		final InvokableMethod operationMethod = getRunMethod(automation, operationId);
		final Object operationRes = operationMethod.invoke(ctx, parameters);
		return operationRes;
	}

	/**
	 * Check whether the document is a runtime (technical) document.
	 *
	 * @param document
	 *            the document to check
	 * @return true if the document is runtime type, otherwise false.
	 */
	public static boolean isRuntimeDocument(DocumentModel document) {
		return document.hasFacet(FacetNames.SYSTEM_DOCUMENT) || document.hasFacet(FacetNames.HIDDEN_IN_NAVIGATION);
	}

	/**
	 * Méthode permettant de récupérer la méthode d'exécution (run()) d'une
	 * opération.
	 *
	 * @param automation
	 *            instance du service d'automation
	 * @param operationId
	 *            identifiant de l'opération
	 * @return la méthode run() de l'opération
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws OperationNotFoundException
	 */
	private static InvokableMethod getRunMethod(AutomationService automation, String operationId)
			throws SecurityException, NoSuchMethodException, OperationNotFoundException {
		final OperationType opType = automation.getOperation(operationId);
		Method method;
		try {
			method = opType.getType().getMethod("run", (Class<?>[]) null);
		} catch (final NoSuchMethodException nsme) {
			final Class[] tabArg = new Class[1];
			tabArg[0] = DocumentModel.class;
			method = opType.getType().getMethod("run", tabArg);
		}
		final OperationMethod anno = method.getAnnotation(OperationMethod.class);

		return new InvokableMethod(opType, method, anno);
	}

	public static DocumentModel getMediaSpace(DocumentModel doc, CoreSession session) throws ClientException {
		DocumentModel mediaSpace = null;
		final DocumentModel currentDomain = ToutaticeDocumentHelper.getDomain(session, doc, true);
		if (currentDomain != null) {
			final String searchMediaLibraries = "ecm:primaryType = '" + MEDIALIB + "' and ecm:path startswith '"
					+ currentDomain.getPathAsString()
					+ "' and ecm:isCheckedInVersion = 0 AND ecm:isProxy = 0 AND ecm:currentLifeCycleState!='deleted'";

			final String queryMediaLibraries = String.format("SELECT * FROM Document WHERE %s", searchMediaLibraries);

			final DocumentModelList query = session.query(queryMediaLibraries);

			if ((query.size() < 1) || (query.size() > 1)) {
				mediaSpace = null;
			} else {
				mediaSpace = query.get(0);
			}
		} else {
			log.warn("CurrentDomain not available " + doc.getTitle());
		}

		return mediaSpace;
	}

	/**
	 * @param document
	 * @return list of remote proxies of document (if any).
	 */
	public static DocumentModelList getRemotePublishedDocuments(CoreSession session, DocumentModel document) {
		final DocumentModelList remoteProxies = new DocumentModelListImpl();

		if (!ToutaticeDocumentHelper.isInPublishSpace(session, document)) {
			final DocumentModelList remoteProxiesFound = ToutaticeDocumentHelper.getProxies(session, document,
					ToutaticeGlobalConst.CST_TOUTATICE_PROXY_LOOKUP_SCOPE.GLOBAL, StringUtils.EMPTY, false);
			if (CollectionUtils.isNotEmpty(remoteProxiesFound)) {
				remoteProxies.addAll(remoteProxiesFound);
			}
		}

		return remoteProxies;

	}

	/**
	 * @param document
	 * @return true if document still exists.
	 * @throws ClientException
	 */
	public static boolean isDocStillExists(CoreSession session, DocumentModel document) throws ClientException {
		boolean exists = false;

		if (document != null) {
			try {

				session.getDocument(document.getRef());
				exists = true;

			} catch (final ClientException ce) {

				if (ce.getCause() instanceof NoSuchDocumentException) {
					exists = false;
				} else {
					throw ce;
				}

			}
		}

		return exists;
	}

	/**
	 *
	 * @param document
	 * @return true if document is a remote proxy.
	 */
	public static boolean isRemoteProxy(DocumentModel document) {
		return document.isProxy()
				&& !StringUtils.endsWith(document.getName(), ToutaticeGlobalConst.CST_PROXY_NAME_SUFFIX);
	}

	/**
	 * @param document
	 * @return true if document is local proxy.
	 */
	public static boolean isLocaProxy(DocumentModel document) {
		return document.isProxy()
				&& StringUtils.endsWith(document.getName(), ToutaticeGlobalConst.CST_PROXY_NAME_SUFFIX);
	}

	/**
	 * @param session
	 * @param document
	 * @return true if working copy of document is different from last version.
	 */
	public static boolean isBeingModified(CoreSession session, DocumentModel document) {
		boolean is = false;

		final String versionLabel = document.getVersionLabel();

		final DocumentModel lastDocumentVersion = session.getLastDocumentVersion(document.getRef());
		if (lastDocumentVersion != null) {
			final String lastDocumentVersionLabel = lastDocumentVersion.getVersionLabel();
			is = !StringUtils.equals(versionLabel, lastDocumentVersionLabel);
		}

		return is;
	}

}
