/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.rest.internal;

import org.phenotips.data.api.DocumentSearch;
import org.phenotips.data.api.DocumentSearchResult;
import org.phenotips.data.api.internal.filter.AbstractFilter;
import org.phenotips.data.api.internal.filter.EntityFilter;
import org.phenotips.data.api.internal.filter.ObjectFilter;
import org.phenotips.data.api.internal.filter.property.StringFilter;
import org.phenotips.data.rest.EntitySearch;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.rest.XWikiResource;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.UserManager;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.ViewAction;

/**
 * TODO.
 *
 * @version $Id$
 */
@Component
@Named("org.phenotips.data.rest.internal.DefaultEntitySearchImpl")
@Singleton
public class DefaultEntitySearchImpl extends XWikiResource implements EntitySearch
{

    @Inject
    private UserManager users;

    @Inject
    private AuthorizationManager access;

    /** Fills in missing reference fields with those from the current context document to create a full reference. */
    @Inject
    @Named("current")
    private EntityReferenceResolver<EntityReference> currentResolver;


    //@Inject
    //private QueryManager queryManager;

    @Inject
    private Logger logger;

    @Inject
    private ContextualAuthorizationManager contextAccess;

    @Inject
    private DocumentSearch documentSearch;

    @Inject
    private LocalizationManager localization;

    //http://localhost:8080/get/PhenoTips/LiveTableResults
    // ?outputSyntax=plain
    // &transprefix=patient.livetable.
    // &classname=PhenoTips.PatientClass
    // &collist=doc.name,external_id,doc.creator,doc.author,doc.creationDate,doc.date,first_name,last_name,reference
    // &queryFilters=currentlanguage,hidden
    // &&filterFrom=, LongProperty iid
    // &filterWhere=and iid.id.id = obj.id and iid.id.name = 'identifier' and iid.value >= 0
    // &offset=1
    // &limit=25
    // &reqNo=21
    // &external_id=p012
    // &visibility=hidden
    // &visibility=private
    // &visibility=public
    // &visibility=open
    // &visibility/class=PhenoTips.VisibilityClass
    // &owner/class=PhenoTips.OwnerClass
    // &date_of_birth/after=10/11/2000
    // &omim_id=607426
    // &omim_id/join_mode=OR
    // &phenotype=HP:0011903
    // &phenotype=HP:0003460
    // &phenotype/join_mode=OR
    // &phenotype_subterms=yes
    // &gene=TRX-CAT1-2
    // &gene=ATP5A1P10
    // &gene/class=PhenoTips.GeneClass
    // &gene/match=ci
    // &status/class=PhenoTips.GeneClass
    // &status/join_mode=OR
    // &status/dependsOn=gene
    // &status=candidate
    // &status=solved
    // &reference/class=PhenoTips.FamilyReferenceClass
    // &sort=doc.name
    // &dir=asc

    /**
     * Provides access to the underlying data storage.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Provides access to the current execution context.
     */
    @Inject
    private Provider<XWikiContext> xContextProvider;

    @Override public Response search(@Context UriInfo uriInfo)
    {

        if (this.documentSearch == null) {
            //this.documentSearch = new DefaultDocumentSearchImpl(users, currentResolver, access, super.queryManager);

        }

        /*User currentUser = this.users.getCurrentUser();

        if (!this.access.hasAccess(Right.VIEW, currentUser == null ? null : currentUser.getProfileDocument(),
            this.currentResolver.resolve(Patient.DEFAULT_DATA_SPACE, EntityType.SPACE))) {

        }

        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();

        //GenericEntity ge = new GenericEntity(null, );
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("Testing Search", "YES");

        if (currentUser != null) {
            JSONObject currentUserJSON = new JSONObject();
            currentUserJSON.put("name", currentUser.getName());
            currentUserJSON.put("id", currentUser.getId());
            jsonObject.put("current_user", currentUserJSON);
        }

        JSONObject queryParamsJSON = new JSONObject();
        for (Map.Entry<String, List<String>> entry : queryParameters.entrySet()) {
            JSONArray queryParamsValuesJSON = new JSONArray();
            queryParamsJSON.put(entry.getKey(), queryParamsValuesJSON);
            for (String value : entry.getValue()) {
                queryParamsValuesJSON.put(value);
            }
        }
*/
        // JSONArray resultJSONArray = new JSONArray();

        try {
            JSONObject jsonObject = getJSONWrapper(uriInfo);
            DocumentSearchResult documentSearchResult = documentSearch.search(jsonObject);
            return getWebResponse(documentSearchResult, uriInfo);
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(e, Status.BAD_REQUEST);
        }
        /*catch (SecurityException se) {
            throw new WebApplicationException(se, Status.UNAUTHORIZED);
        } catch (QueryException qe) {
            throw new WebApplicationException(qe, Status.INTERNAL_SERVER_ERROR);
        } catch (IllegalArgumentException iae) {
            throw new WebApplicationException(iae, Status.BAD_REQUEST);
        }*/


/*
        try {
            String queryStr = new EntityFilter().hql(new StringBuilder(), 0, "").toString();
            Query query = queryManager.createQuery(queryStr, "hql");

            query.setLimit(3000);

            jsonObject.put("hql", queryStr);


            List<XWikiDocument> results = (List<XWikiDocument>) (List) query.execute();
            int i = 0;
            for (XWikiDocument wikiMacroDocumentData : results) {
                //String space = (String) wikiMacroDocumentData[0];
                resultJSONArray.put(i, wikiMacroDocumentData.getDocumentReference() + ", id=" + wikiMacroDocumentData.getId());
                i++;
            }

            jsonObject.put("result#", results.size());

        } catch (QueryException e) {
            e.printStackTrace();
        }

        jsonObject.put("query_params", queryParamsJSON);
        jsonObject.put("results", resultJSONArray);



        Response.ResponseBuilder response = Response.ok(jsonObject, MediaType.APPLICATION_JSON_TYPE);

        return response.build();*/
    }

    private Response getWebResponse(DocumentSearchResult documentSearchResult, UriInfo uriInfo) throws XWikiException
    {
        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
        JSONObject queryParamsJSON = new JSONObject();
        for (Map.Entry<String, List<String>> entry : queryParameters.entrySet()) {
            JSONArray queryParamsValuesJSON = new JSONArray();
            queryParamsJSON.put(entry.getKey(), queryParamsValuesJSON);
            for (String value : entry.getValue()) {
                queryParamsValuesJSON.put(value);
            }
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("reqNo", queryParameters.getFirst("reqNo"));
        jsonObject.put("query_params", queryParamsJSON);

        JSONArray rows = new JSONArray();
        jsonObject.put("rows", rows);

        for (XWikiDocument doc : documentSearchResult.getDocuments()) {
            this.addRow(rows, doc);
        }

        jsonObject.put("totalrows", documentSearchResult.getReturnedRows());
        jsonObject.put("returnedrows", documentSearchResult.getReturnedRows());
        jsonObject.put("offset", documentSearchResult.getOffset());



        Response.

        ResponseBuilder response = Response.ok(jsonObject, MediaType.APPLICATION_JSON_TYPE);

        return response.build();
    }

    /*
        #set($discard = $row.put('doc_name', $itemDoc.name))
        #set($discard = $row.put('doc_fullName', $fullname))
        #set($discard = $row.put('doc_space', $itemDoc.space))
        #set($discard = $row.put('doc_url', $xwiki.getURL($item)))
        #set($discard = $row.put('doc_space_url', $xwiki.getURL($services.model.createDocumentReference($!itemDoc.wiki, $!itemDoc.space, 'WebHome'))))
        #set($discard = $row.put('doc_wiki', $itemDoc.wiki))
        #set($discard = $row.put('doc_wiki_url', $xwiki.getURL($services.model.resolveDocument('', 'default', $itemDoc.documentReference.extractReference('WIKI')))))
        #set($discard = $row.put('doc_hasadmin', $xwiki.hasAdminRights()))
        #set($discard = $row.put('doc_hasedit', $xwiki.hasAccessLevel('edit', $xcontext.user, $fullname)))
        #set($discard = $row.put('doc_hasdelete', $xwiki.hasAccessLevel('delete', $xcontext.user, $fullname)))
        #set($discard = $row.put('doc_edit_url', $itemDoc.getURL($itemDoc.defaultEditMode)))
        #set($discard = $row.put('doc_copy_url', $itemDoc.getURL('view', 'xpage=copy')))
        #set($discard = $row.put('doc_delete_url', $itemDoc.getURL('delete')))
        #set($discard = $row.put('doc_rename_url', $itemDoc.getURL('view', 'xpage=rename&amp;step=1')))
        #set($discard = $row.put('doc_rights_url', $itemDoc.getURL('edit', 'editor=rights')))
        #set($discard = $row.put('doc_export_url', $itemDoc.getURL('export', "format=xar&amp;name=$!{itemDoc.fullName}&amp;pages=$!{itemDoc.fullName}")))
        #set($discard = $row.put('doc_history_url', $itemDoc.getURL('view', 'viewer=history')))
        #set($discard = $row.put('doc_author_url', $xwiki.getURL($itemDoc.author)))
        #set($discard = $row.put('doc_date', $xwiki.formatDate($itemDoc.date)))
        #set($discard = $row.put('doc_title', $escapetool.xml($itemDoc.plainTitle)))
        #set($discard = $row.put('doc_author', $services.xml.unescape($xwiki.getUserName($itemDoc.author, false))))
        #set($discard = $row.put('doc_creationDate', $xwiki.formatDate($itemDoc.creationDate)))
        #set($discard = $row.put('doc_creator', $services.xml.unescape($xwiki.getUserName($itemDoc.creator, false))))
        #set($discard = $row.put('doc_creator_url', $xwiki.getURL($itemDoc.creator)))
    */
    private void addRow(JSONArray rows, XWikiDocument doc) throws XWikiException
    {
        if (doc == null) {
            return;
        }

        DocumentReference docRef = doc.getDocumentReference();
        String fullName = docRef.toString();

        JSONObject row = new JSONObject();

        XWikiContext context = this.xContextProvider.get();
        XWiki wiki = context.getWiki();

        //row.put()

        //localization.

        row.put("doc_name", docRef.getName());
        row.put("doc_fullName", fullName);
        row.put("doc_space", docRef.getLastSpaceReference().getName());
        //row.put("doc_url",  doc.getURL(ViewAction.VIEW_ACTION, context));
        row.put("doc_url", wiki.getURL(docRef, ViewAction.VIEW_ACTION, context));
        row.put("doc_space_url", "");
        row.put("doc_wiki", wiki.getName());
        row.put("doc_wiki_url", "");

        row.put("doc_hasadmin", this.contextAccess.hasAccess(Right.ADMIN));
        row.put("doc_hasedit", this.contextAccess.hasAccess(Right.EDIT));
        row.put("doc_hasdelete", this.contextAccess.hasAccess(Right.DELETE));

        //row.put("doc_edit_url", doc.getDefaultEditURL(context));
        row.put("doc_copy_url", this.getURL(doc.getURL(ViewAction.VIEW_ACTION, "xpage=copy", context)));
        row.put("doc_delete_url", this.getURL(doc.getURL("delete", context)));
        row.put("doc_rename_url", this.getURL(doc.getURL(ViewAction.VIEW_ACTION, "xpage=rename&amp;step=1", context)));
        row.put("doc_rights_url", this.getURL(doc.getURL("edit", "editor=rights", context)));
        row.put("doc_export_url", this.getURL(doc.getURL("export", "format=xar&amp;name=" + fullName + "&amp;pages=" + fullName, context)));
        row.put("doc_history_url", this.getURL(doc.getURL(ViewAction.VIEW_ACTION, "viewer=history", context)));
        row.put("doc_author_url", this.getURL(wiki.getURL(doc.getAuthorReference(), ViewAction.VIEW_ACTION, context)));


        row.put("doc_date", docRef.getName());
        row.put("doc_title", docRef.getName());
        row.put("doc_author", docRef.getName());
        row.put("doc_creationDate", docRef.getName());
        row.put("doc_creator", docRef.getName());
        row.put("doc_creator_url", this.getURL(wiki.getURL(doc.getCreatorReference(), ViewAction.VIEW_ACTION, context)));

        for (String colName : this.getColumnNames()) {
            this.addColumn(row, colName);
        }

        rows.put(row);
    }

    private String getURL(String urlStr)
    {
        try {
            URL url = new URL(urlStr);
            return url.getPath() + "?" + url.getQuery();
        } catch (MalformedURLException e) {
            this.logger.warn(String.format("Given url string is invalid [%s]", urlStr), e);
        }
        return StringUtils.EMPTY;
    }

    private JSONObject getJSONWrapper(UriInfo uriInfo)
    {
        //JSONObject jsonObject = new JSONObject();

        JSONObject queryObj = new JSONObject();
        queryObj.put(AbstractFilter.TYPE_KEY, AbstractFilter.Type.DOCUMENT.toString());
        queryObj.put(AbstractFilter.CLASS_KEY, "PhenoTips.PatientClass");

        JSONArray filters = new JSONArray();
        queryObj.put(EntityFilter.FILTERS_KEY, filters);

        JSONObject filter1 = new JSONObject();
        filter1.put(AbstractFilter.TYPE_KEY, AbstractFilter.Type.OBJECT.toString());
        filter1.put(AbstractFilter.CLASS_KEY, "PhenoTips.VisibilityClass");
        filter1.put(ObjectFilter.PROPERTY_NAME_KEY, "visibility");
        filter1.put(StringFilter.VALUE_KEY, new JSONArray("[hidden,private,public,open]"));

        filters.put(filter1);
        List<String> bindingValues = new LinkedList<>();

        return queryObj;
    }

    private void addColumn(JSONObject row, String columnName)
    {

    }

    private List<String> getColumnNames()
    {
        return null;
    }
}
