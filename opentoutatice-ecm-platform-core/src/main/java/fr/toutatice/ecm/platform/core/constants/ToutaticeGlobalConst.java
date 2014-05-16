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
package fr.toutatice.ecm.platform.core.constants;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;

import fr.toutatice.ecm.platform.core.publish.ToutaticeNullPublishedDocument;

/**
 * This class groups together all definitions required within the bundle (except those from the studio)
 * 
 * @author mberhaut1
 */
public class ToutaticeGlobalConst {
    
    public final static String CST_DOC_STATE_PROJECT = "project";
    public final static String CST_DOC_STATE_APPROVED = "approved";
    public final static String CST_DOC_STATE_DELETED = "deleted";
    
    public static final String CST_CONTENT_VIEW_RELATION_SEARCH = "relation_search";
    public static final String CST_ADVANCED_SEARCH_XPATH_PATH = "advanced_search:searchpath";
    public static final String CST_FACETED_SEARCH_XPATH_PATH = "faceted_search_default:ecm_path";
    public static final String CST_RELATION_SEARCH_XPATH_PATH = "relation_search:ecm_path";
    
	// Workflow tasks - mise en ligne
	public final static String CST_WORKFLOW_TASK_CHOOSE_PARTICIPANT = "choose-participant";
	public final static String CST_WORKFLOW_PROCESS_ONLINE = "toutatice_online_approbation";
	public final static String CST_WORKFLOW_TASK_ONLINE_VALIDATE = "validate-online";
	public final static String CST_WORKFLOW_TASK_ONLINE_REJECT = "reject-online";
	public final static String CST_WORKFLOW_TASK_ONLINE_CHOOSE_PARTICIPANT = "choose-participant";
	
	// Workflow tasks - ensembles  
	public final static String[] CST_WORKFLOW_TASK_ONLINE_ALL = new String[] {
		CST_WORKFLOW_TASK_ONLINE_VALIDATE,
		CST_WORKFLOW_TASK_ONLINE_REJECT,
		CST_WORKFLOW_TASK_ONLINE_CHOOSE_PARTICIPANT
	};
	
	public final static String[] CST_WORKFLOW_TASK_ALL = new String[] {
		CST_WORKFLOW_TASK_CHOOSE_PARTICIPANT,
		CST_WORKFLOW_TASK_ONLINE_VALIDATE,
		CST_WORKFLOW_TASK_ONLINE_REJECT,
		CST_WORKFLOW_TASK_ONLINE_CHOOSE_PARTICIPANT
	};
	
	// Events
	public final static String CST_EVENT_SUB_TAB_SELECTION_CHANGED = "subTabSelectionChanged";
	public final static String CST_EVENT_PROPAGATE_SECTIONS = "propagateSections";
	/* Online workflow events */
	public final static String CST_EVENT_ONLINE_TASK_APPROVED_ASSIGNED = "workflowOnlineTaskAssigned";
	public final static String CST_EVENT_ONLINE_TASK_APPROVED = "workflowOnlineTaskApproved";
	public final static String CST_EVENT_ONLINE_TASK_REJECTED = "workflowOnlineTaskRejected";
	public final static String CST_EVENT_ONLINE_WF_CANCELED = "workflowOnlineTaskCanceled";
	
	/* TODO: delete */
	public final static String CST_EVENT_PROPAGATE_ORGANISATION_SOURCE = "propagateOrganisationSource";
	public final static String CST_EVENT_SECTION_MODIFICATION = "sectionModified";

	// Events options
	public final static String CST_EVENT_OPTION_VALUE_ALL = "ALL";
	public final static String CST_EVENT_OPTION_KEY_RUN_UNRESTRICTED = "runUnrestricted";
	public final static String CST_EVENT_OPTION_KEY_APPEND = "append";
	public final static String CST_EVENT_OPTION_KEY_OVERWRITE = "overwrite";
	public final static String CST_EVENT_OPTION_KEY_SECTION_ID = CST_EVENT_OPTION_VALUE_ALL;
	
	// Audit events
	
	// Others
	public final static String CST_PROXY_NAME_SUFFIX = ".proxy";
	
	/**
	 * Cette définition est utilisée pour la gestion du cache (cf: ToutaticeNavigationContext.java) pour distinguer le cas
	 * où le cache retourne aucun résultat parce qu'aucune recherche n'a pas encore été faite du cas où la recherche 
	 * a été faite mais aucun résultat existe.
	 */
	public final static DocumentModel NULL_DOCUMENT_MODEL = new DocumentModelImpl("NULL_DOCUMENT_MODEL");
	public final static PublishedDocument NULL_PUBLISHED_DOCUMENT_MODEL = new ToutaticeNullPublishedDocument();
	
	//Groupe
	public final static String CST_GROUP_ADMINISTRATOR = "Administrators";
}
