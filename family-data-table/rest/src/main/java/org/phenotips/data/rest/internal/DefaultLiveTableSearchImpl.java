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
import org.phenotips.data.rest.LiveTableSearch;
import org.phenotips.data.rest.LiveTableInputAdapter;
import org.phenotips.data.rest.LiveTableRowHandler;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.container.Container;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.rest.XWikiRestComponent;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
//import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiRequest;

//import org.xwiki.rendering.parser.ContentParser

/**
 * TODO.
 *
 * @version $Id$
 */
@Component //(roles = { LiveTableSearch.class })
@Named("org.phenotips.data.rest.internal.DefaultLiveTableSearchImpl")
@InstantiationStrategy(ComponentInstantiationStrategy.SINGLETON)
@Singleton
public class DefaultLiveTableSearchImpl implements LiveTableSearch
{

    public static final String COLUMN_LIST_KEY = "collist";


    @Inject
    private Provider<XWikiContext> xContextProvider;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    //@Inject
    //private AuthorizationManager access;

    @Inject
    private LiveTableRowHandler responseRowHandler;

    /** Fills in missing reference fields with those from the current context document to create a full reference. */
    @Inject
    @Named("current")
    private EntityReferenceResolver<EntityReference> currentResolver;

    //@Inject
   // @Named("localization")
   // private ScriptService localizationService;

    //@Inject
    //private Logger logger;

    //@Inject
    //private ContextualAuthorizationManager contextAccess;

    @Inject
    private DocumentSearch documentSearch;

    //@Inject
    //private LocalizationManager localization;

    @Inject
    @Named("url")
    private LiveTableInputAdapter inputAdapter;

    @Inject
    private Container container;

    /**
     * Provides access to the underlying data storage.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    //private DefaultResponseRowHandler rowHandler;

    @Override public Response search(@Context UriInfo uriInfo)
    {

        XWikiRequest xwikiRequest = this.xContextProvider.get().getRequest(); // .getHttpServletRequest().getQueryString()

        HttpServletRequest httpServletRequest = xwikiRequest.getHttpServletRequest();
        
        try {
            Date start = new Date();

            Map<String, List<String>> queryParameters = RequestUtils.getQueryParameters(httpServletRequest
                .getQueryString());
            JSONObject inputObject = this.inputAdapter.convert(queryParameters);
            Date adapterEnd = new Date();

            DocumentSearchResult documentSearchResult = this.documentSearch.search(inputObject);
            Date queryEnd = new Date();

            List<TableColumn> cols = this.getColumns(inputObject);
            //return getWebResponse(documentSearchResult, cols, uriInfo);
            //MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
            JSONObject queryParamsJSON = new JSONObject();

            JSONObject responseObject = new JSONObject();
            responseObject.put(DocumentSearch.REQUEST_NUMBER_KEY, Long.valueOf(RequestUtils.getFirst(queryParameters,
                DocumentSearch.REQUEST_NUMBER_KEY)));
            responseObject.put("query_params", queryParamsJSON);
            responseObject.put(DocumentSearch.OFFSET_KEY, Long.valueOf(RequestUtils.getFirst(queryParameters,
                DocumentSearch.OFFSET_KEY)));

            JSONArray rows = new JSONArray();
            responseObject.put("rows", rows);

            XWikiContext context = this.xContextProvider.get();

            for (XWikiDocument doc : documentSearchResult.getDocuments()) {
                JSONObject row = this.responseRowHandler.getRow(this.getDocument(doc), context, cols, queryParameters);
                if (row != null) {
                    rows.put(row);
                }
            }

            responseObject.put("totalrows", documentSearchResult.getReturnedRows());
            responseObject.put("returnedrows", documentSearchResult.getReturnedRows());
            responseObject.put("offset", documentSearchResult.getOffset() + 1);

            Date tablePrepEnd = new Date();

            JSONObject durationsObj = new JSONObject();

            durationsObj.put("input_adapter", adapterEnd.getTime() - start.getTime());
            durationsObj.put("query", queryEnd.getTime() - adapterEnd.getTime());
            durationsObj.put("table_prep", tablePrepEnd.getTime() - queryEnd.getTime());
            durationsObj.put("total", new Date().getTime() - start.getTime());
            responseObject.put("request_durations", durationsObj);

            Response.ResponseBuilder response = Response.ok(responseObject, MediaType.APPLICATION_JSON_TYPE);

            return response.build();
        } catch (Exception e) {
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
    }



    private XWikiDocument getDocument(XWikiDocument docShell) throws XWikiException
    {
        if (docShell == null) {
            return null;
        }

        try {
            return (XWikiDocument) this.documentAccessBridge.getDocument(docShell.getDocumentReference());

        } catch (Exception e) {
            throw new XWikiException("Error while getting document " + docShell.getDocumentReference().getName(), e);
        }
    }

    private List<TableColumn> getColumns(JSONObject jsonObject)
    {
        if (jsonObject.optJSONArray(COLUMN_LIST_KEY) == null) {
            throw new IllegalArgumentException(String.format("No %1$s key found.", COLUMN_LIST_KEY));
        }

        JSONArray columnArray = jsonObject.getJSONArray(COLUMN_LIST_KEY);

        List<TableColumn> columns = new LinkedList<>();

        for (Object obj : columnArray) {
            if (!(obj instanceof JSONObject)) {
                throw new IllegalArgumentException(String.format("Column %1$s is not a JSONObject", obj));
            }

            columns.add(new TableColumn().populate((JSONObject) obj));
        }

        return columns;
    }

}
