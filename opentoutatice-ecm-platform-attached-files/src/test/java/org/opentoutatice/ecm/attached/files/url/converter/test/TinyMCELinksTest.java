/**
 * 
 */
package org.opentoutatice.ecm.attached.files.url.converter.test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.opentoutatice.ecm.attached.files.url.converter.DocumentContentConverter;


/**
 * @author david
 *
 */
public class TinyMCELinksTest {

    /** Set of absolute external URLs. */
    public static final String[] ABS_EXT_LINK = {"http://www.osivia.org", "https://www.youtube.com", "mailto:dchevrier@osivia.com",
            "nxdrive://edit/http/vm-dch-dev/nuxeo/repo/default/nxdocid/610104e0-fa7c-451a-9a88-14870a049fff/filename/90-portal.ldif"};
    /** Set of absolute internal URLs. */
    // TODO: /portalRef?
    public static final String[] ABS_INT_LINK = {
            "http://ac-rennes-v3/nuxeo/nxpath/default/ckr/eptest/pagetest/filewf@view_documents?tabIds=%3AATAB_EDIT&conversationId=0NXMAIN1",
            "http://vm-dch-dev/portal/auth/pagemarker/5/cms/default-domain/workspaces/espace-pedagogie/test-mail"};

    /** Set of absolute like URLs. */
    public static final String[] ABS_LIKE_LINK = {"//www.osivia.org", "www.osivia.org", "osivia.org"};

    /** Set of relative Nx resources URLs. */
    public static final String[] NX_SRC_LINKS = {"web/bernie-mini3?content=Medium", "nxfile/default/attachedImages/ttc:images/0/file/chat.jpeg",
            "nxfile/default/df253d7f-8811-42d7-b853-512df435c1b0/ttc:images/0/file/rat.jpg?conversationId=0NXMAIN1"};
    
    /** Set of relative Nx resources URLs. */
    public static final String[] NX_DOWNLOAD_SRC_LINKS = {"nxfile/default/1182d4e1-81bc-4e96-bf8e-b4e2bc6bdcd6/blobholder:0/Erreurs%20%C3%A0%20l'import%20de%20fichiers.txt"};

    /**
     * Set of relative bad Nx resources URLs.
     * Tey are bad caus tiniyMCE doesn't allow beginning with /.
     */
    //public static final String[] BAD_NX_SRC_LINKS = {"/web/bernie-mini3?content=Medium", "/nxfile/default/attachedImages/ttc:images/0/file/chat.jpeg"};

    /** Set of relative Nx document URLs. */
    public static final String[] NX_DOC_LINKS = {
            "nxdoc/default/dcb06690-4e47-4af3-8c29-404e48a1f3e1/view_documents",
            "nxdoc/default/dcb06690-4e47-4af3-8c29-404e48a1f3e1/view_documents@view_documents?tabIds=MAIN_TABS%3Adocuments%2C%3A&old_conversationId=0NXMAIN2",
            "nxpath/default/default-domain/workspaces/espace-pedagogie/test-mail/too-move/90-portal-ldif",
            "nxpath/default/default-domain/workspaces/espace-pedagogie/test-mail/too-move/90-portal-ldif@view_documents?tabIds=MAIN_TABS%3Adocuments%2C%3A&old_conversationId=0NXMAIN2",
            "web/bernie-mini", "web/bernie-mini?tabIds=MAIN_TABS%3Adocuments%2C%3A&old_conversationId=0NXMAIN2"};

    /** List of possibilities. */
    public static final String[][] ALL_POSSIBLE_LINKS = {ABS_EXT_LINK, ABS_INT_LINK, ABS_LIKE_LINK, NX_SRC_LINKS, /*BAD_NX_SRC_LINKS,*/ NX_DOC_LINKS};

    /** Content links skeleton. */
    public static final String CONTENT_LINKS_SKELETON = "<p><a href=\"%s\">%s</a></p><br/>";

    /** Content resources skeleton. */
    public static final String CONTENT_SRC_SKELETON = "<p><img src=\"%s\">%s</a></p><br/>";

    @Test
    public void test_links() throws Exception {
        // Content to test
        String content = StringUtils.EMPTY;
        
        String[][] POSSIBLE_LINKS = {NX_DOC_LINKS};

        for (String[] links : POSSIBLE_LINKS) {
            for (String link : links) {
                // Label of html image or link
                String label = StringUtils.substringAfterLast(link, "/");
                if (StringUtils.isEmpty(label)) {
                    label = StringUtils.substringBefore(link, "/");
                }
                // TODO: remove this later:
                label = "OOO";
                
                // a tags
                content = content.concat(String.format(CONTENT_LINKS_SKELETON, link, label));
                // img tags
                //content = content.concat(String.format(CONTENT_SRC_SKELETON, link, label));
            }
        }
        // Do it as unit test .... (no ... sysou) !!
        
        // Display content
        String[] input = StringUtils.splitByWholeSeparator(content, "<br/>");
        for(String tag : input){
            System.out.println(tag);
        }
        
        // Conversion
        String convertedContent = (String) new DocumentContentConverter().getAsObject(null, null, content);

        // Display converted content
        System.out.println("============================ \r\n");
        String[] output = StringUtils.splitByWholeSeparator(convertedContent, "<br/>");
        for(String tag : output){
            System.out.println(tag);
        }
    }

    @Test
    public void test() {
        // final Pattern PATTERN = Pattern.compile(".+^(hierarchy\"\\.\"parentid\" =).+(AND \"_ACLRUSERMAP\"\\.user_id = md5|NX_ACCESS_ALLOWED).+");
        // final Pattern PATTERN_NO_PID_PERM = Pattern.compile(".+(hierarchy\"\\.\"parentid\" =){0}+.+NX_ACCESS_ALLOWED.+");
        final Pattern PATTERN_NO_PID_PERM = Pattern.compile(".+(hierarchy\"\\.\"parentid\" =)+.+");

        final String pidPerm = "SQL: SELECT \"hierarchy\".\"id\" AS \"_C1\" FROM \"hierarchy\" LEFT JOIN \"misc\" \"_F1\" ON \"hierarchy\".\"id\" = \"_F1\".\"id\" JOIN hierarchy_read_acl \"_RACL\" ON \"hierarchy\".\"id\" = \"_RACL\".id JOIN aclr_user_map \"_ACLRUSERMAP\" ON \"_RACL\".acl_id = \"_ACLRUSERMAP\".acl_id WHERE ((\"hierarchy\".\"primarytype\" IN ('ServiceMetier', 'SectionRoot', 'Annonce', 'Workspace', 'RelationSearch', 'user_open_tasks_cv', 'Alerte', 'Room', 'UserWorkspace', 'BlogSite', 'FacetedSavedSearchesFolder', 'Thread', 'WebConfigurationFolder', 'ProceduresContainer', 'TemplateRoot', 'DocumentRouteStep', 'PortalSite', 'Domain', 'Collections', 'ProceduresInstancesContainer', 'WorkspaceRoot', 'Zoom', 'ContactMessage', 'QueryNav', 'ToutaticePad', 'SimpleTask', 'Agenda', 'PortalPage', 'ContactMessageFolder', 'WebPage', 'VEVENT', 'ConditionalTask', 'Video', 'MailMessage', 'Forum', 'PictureBook', 'ContextualLink', 'ProcedureInstance', 'WebConfiguration', 'doc_acaren_advanced_search', 'Question', 'AssetLibrary', 'PortalVirtualPage', 'FollowLifeCycleTransitionTask', 'Staple', 'PublishTask', 'Picture', 'Collection', 'Document', 'FaqFolder', 'Folder', 'FacetedSearch', 'MediaLibrary', 'File', 'DocumentUrl', 'WebSite', 'AdvancedSearch', 'Note', 'Favorites', 'Section', 'ProcedureModel', 'DocumentUrlContainer', 'doc_toutatice_image_search', 'MailFolder', 'ProceduresModelsContainer', 'OrderedFolder', 'Audio', 'BasicAuditSearch', 'AnnonceFolder', 'BlogPost')) AND (\"hierarchy\".\"isversion\" IS NULL) AND (\"_F1\".\"lifecyclestate\" <> 'deleted') AND (\"hierarchy\".\"parentid\" = 3c018468-2936-4f68-9bcd-71cf1c8b3cec) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy'))) AND \"_ACLRUSERMAP\".user_id = md5(array_to_string(['eleve-college','eleve-college-public','members','lea.lafraise','Everyone'], ',')) ORDER BY \"hierarchy\".\"pos\" -- LIMIT 40 OFFSET 0 -- COUNT TOTAL UP TO -1{{RemoteAddr,127.0.0.1}{RequestURL,http://mig44dch/nuxeo/view_documents.faces}{ServletPath,/view_documents.faces}{SessionID,10876BEA15179646D21A33CB11B5EB84.nuxeo}{UserPrincipal,lea.lafraise}}";
        final String pidNoPerm = "SQL: SELECT \"hierarchy\".\"id\" AS \"_C1\" FROM \"hierarchy\" LEFT JOIN \"misc\" \"_F1\" ON \"hierarchy\".\"id\" = \"_F1\".\"id\" WHERE ((\"hierarchy\".\"primarytype\" IN ('ServiceMetier', 'SectionRoot', 'Annonce', 'Workspace', 'RelationSearch', 'user_open_tasks_cv', 'Alerte', 'Room', 'UserWorkspace', 'BlogSite', 'FacetedSavedSearchesFolder', 'Thread', 'WebConfigurationFolder', 'ProceduresContainer', 'TemplateRoot', 'DocumentRouteStep', 'PortalSite', 'Domain', 'Collections', 'ProceduresInstancesContainer', 'WorkspaceRoot', 'Zoom', 'ContactMessage', 'QueryNav', 'ToutaticePad', 'SimpleTask', 'Agenda', 'PortalPage', 'ContactMessageFolder', 'WebPage', 'VEVENT', 'ConditionalTask', 'Video', 'MailMessage', 'Forum', 'PictureBook', 'ContextualLink', 'ProcedureInstance', 'WebConfiguration', 'doc_acaren_advanced_search', 'Question', 'AssetLibrary', 'PortalVirtualPage', 'FollowLifeCycleTransitionTask', 'Staple', 'PublishTask', 'Picture', 'Collection', 'Document', 'FaqFolder', 'Folder', 'FacetedSearch', 'MediaLibrary', 'File', 'DocumentUrl', 'WebSite', 'AdvancedSearch', 'Note', 'Favorites', 'Section', 'ProcedureModel', 'DocumentUrlContainer', 'doc_toutatice_image_search', 'MailFolder', 'ProceduresModelsContainer', 'OrderedFolder', 'Audio', 'BasicAuditSearch', 'AnnonceFolder', 'BlogPost')) AND (\"hierarchy\".\"isversion\" IS NULL) AND (\"_F1\".\"lifecyclestate\" <> 'deleted') AND (\"hierarchy\".\"parentid\" = 3c018468-2936-4f68-9bcd-71cf1c8b3cec)) -- LIMIT 20 OFFSET 0 -- COUNT TOTAL UP TO 200{{RemoteAddr,127.0.0.1}{RequestURL,http://mig44dch/nuxeo/search/search.faces}{ServletPath,/search/search.faces}{SessionID,B9CC1877D611B669F38C720A53F49F0B.nuxeo}{UserPrincipal,Administrator}}";
        // final String test3 =
        // "SQL: SELECT \"hierarchy\".\"id\" AS \"_C1\" FROM \"hierarchy\" LEFT JOIN \"misc\" \"_F1\" ON \"hierarchy\".\"id\" = \"_F1\".\"id\" WHERE ((\"hierarchy\".\"primarytype\" IN ('ServiceMetier', 'SectionRoot', 'Annonce', 'Workspace', 'RelationSearch', 'user_open_tasks_cv', 'Alerte', 'Room', 'UserWorkspace', 'BlogSite', 'FacetedSavedSearchesFolder', 'Thread', 'WebConfigurationFolder', 'ProceduresContainer', 'TemplateRoot', 'DocumentRouteStep', 'PortalSite', 'Domain', 'Collections', 'ProceduresInstancesContainer', 'WorkspaceRoot', 'Zoom', 'ContactMessage', 'QueryNav', 'ToutaticePad', 'SimpleTask', 'Agenda', 'PortalPage', 'ContactMessageFolder', 'WebPage', 'VEVENT', 'ConditionalTask', 'Video', 'MailMessage', 'Forum', 'PictureBook', 'ContextualLink', 'ProcedureInstance', 'WebConfiguration', 'doc_acaren_advanced_search', 'Question', 'AssetLibrary', 'PortalVirtualPage', 'FollowLifeCycleTransitionTask', 'Staple', 'PublishTask', 'Picture', 'Collection', 'Document', 'FaqFolder', 'Folder', 'FacetedSearch', 'MediaLibrary', 'File', 'DocumentUrl', 'WebSite', 'AdvancedSearch', 'Note', 'Favorites', 'Section', 'ProcedureModel', 'DocumentUrlContainer', 'doc_toutatice_image_search', 'MailFolder', 'ProceduresModelsContainer', 'OrderedFolder', 'Audio', 'BasicAuditSearch', 'AnnonceFolder', 'BlogPost')) AND (\"hierarchy\".\"isversion\" IS NULL) AND (\"_F1\".\"lifecyclestate\" <> 'deleted') AND (EXISTS(SELECT 1 FROM ancestors WHERE id = \"hierarchy\".\"id\" AND ARRAY['3c018468-2936-4f68-9bcd-71cf1c8b3cec'] <@ ancestors))) -- LIMIT 20 OFFSET 0 -- COUNT TOTAL UP TO 200{{RemoteAddr,127.0.0.1}{RequestURL,http://mig44dch/nuxeo/search/search.faces}{ServletPath,/search/search.faces}{SessionID,B9CC1877D611B669F38C720A53F49F0B.nuxeo}{UserPrincipal,Administrator}}";
        final String noPidPerm = "SQL: SELECT \"hierarchy\".\"id\" AS \"_C1\" FROM \"hierarchy\" LEFT JOIN \"misc\" \"_F1\" ON \"hierarchy\".\"id\" = \"_F1\".\"id\" JOIN hierarchy_read_acl \"_RACL\" ON \"hierarchy\".\"id\" = \"_RACL\".id JOIN aclr_user_map \"_ACLRUSERMAP\" ON \"_RACL\".acl_id = \"_ACLRUSERMAP\".acl_id WHERE ((\"hierarchy\".\"primarytype\" IN ('ServiceMetier', 'SectionRoot', 'Annonce', 'Workspace', 'RelationSearch', 'user_open_tasks_cv', 'Alerte', 'Room', 'UserWorkspace', 'BlogSite', 'FacetedSavedSearchesFolder', 'Thread', 'WebConfigurationFolder', 'ProceduresContainer', 'TemplateRoot', 'DocumentRouteStep', 'PortalSite', 'Domain', 'Collections', 'ProceduresInstancesContainer', 'WorkspaceRoot', 'Zoom', 'ContactMessage', 'QueryNav', 'ToutaticePad', 'SimpleTask', 'Agenda', 'PortalPage', 'ContactMessageFolder', 'WebPage', 'VEVENT', 'ConditionalTask', 'Video', 'MailMessage', 'Forum', 'PictureBook', 'ContextualLink', 'ProcedureInstance', 'WebConfiguration', 'doc_acaren_advanced_search', 'Question', 'AssetLibrary', 'PortalVirtualPage', 'FollowLifeCycleTransitionTask', 'Staple', 'PublishTask', 'Picture', 'Collection', 'Document', 'FaqFolder', 'Folder', 'FacetedSearch', 'MediaLibrary', 'File', 'DocumentUrl', 'WebSite', 'AdvancedSearch', 'Note', 'Favorites', 'Section', 'ProcedureModel', 'DocumentUrlContainer', 'doc_toutatice_image_search', 'MailFolder', 'ProceduresModelsContainer', 'OrderedFolder', 'Audio', 'BasicAuditSearch', 'AnnonceFolder', 'BlogPost')) AND (\"hierarchy\".\"isversion\" IS NULL) AND (\"_F1\".\"lifecyclestate\" <> 'deleted') AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy')) AND ((\"hierarchy\".\"name\" NOT LIKE '%.proxy') OR (\"hierarchy\".\"name\" LIKE '%.remote.proxy'))) AND \"_ACLRUSERMAP\".user_id = md5(array_to_string(['eleve-college','eleve-college-public','members','lea.lafraise','Everyone'], ',')) ORDER BY \"hierarchy\".\"pos\" -- LIMIT 40 OFFSET 0 -- COUNT TOTAL UP TO -1{{RemoteAddr,127.0.0.1}{RequestURL,http://mig44dch/nuxeo/view_documents.faces}{ServletPath,/view_documents.faces}{SessionID,10876BEA15179646D21A33CB11B5EB84.nuxeo}{UserPrincipal,lea.lafraise}}";

        Matcher matcher = PATTERN_NO_PID_PERM.matcher(pidPerm);
        Matcher matcher2 = PATTERN_NO_PID_PERM.matcher(pidNoPerm);
        // Matcher matcher3 = PATTERN.matcher(test3);
        Matcher matcher4 = PATTERN_NO_PID_PERM.matcher(noPidPerm);

        System.out.println(matcher.matches());
        System.out.println(matcher2.matches());
        // System.out.println(matcher3.matches());
        System.out.println(matcher4.matches());
    }

}
