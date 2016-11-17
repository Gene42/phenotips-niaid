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
import org.phenotips.data.api.internal.SpaceAndClass;
import org.phenotips.data.rest.LiveTableInputAdapter;
import org.phenotips.data.rest.LiveTableRowHandler;
import org.phenotips.data.rest.LiveTableSearch;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.query.QueryException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiRequest;

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

    private static final String COLUMN_LIST_KEY = "collist";

    @Inject
    private Provider<XWikiContext> xContextProvider;

    @Inject
    private LiveTableRowHandler responseRowHandler;

    /** Fills in missing reference fields with those from the current context document to create a full reference. */
    @Inject
    @Named("current")
    private EntityReferenceResolver<EntityReference> currentResolver;

    @Inject
    private Logger logger;

    @Inject
    private UserManager users;

    @Inject
    private AuthorizationManager access;

    @Inject
    private DocumentSearch<DocumentReference> documentSearch;

    @Inject
    @Named("url")
    private LiveTableInputAdapter inputAdapter;

    /**
     * Provides access to the underlying data storage.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Override public Response search()
    {

        XWikiRequest xwikiRequest = this.xContextProvider.get().getRequest();

        HttpServletRequest httpServletRequest = xwikiRequest.getHttpServletRequest();
        
        try {
            Map<String, List<String>> queryParameters = RequestUtils.getQueryParameters(httpServletRequest
                .getQueryString());
            JSONObject inputObject = this.inputAdapter.convert(queryParameters);

            this.authorize(inputObject);

            JSONObject responseObject = this.getResponseObject(inputObject, queryParameters);

            responseObject.put(DocumentSearch.REQUEST_NUMBER_KEY, Long.valueOf(RequestUtils.getFirst(queryParameters,
                DocumentSearch.REQUEST_NUMBER_KEY, "0")));

            responseObject.put(DocumentSearch.OFFSET_KEY, Long.valueOf(RequestUtils.getFirst(queryParameters,
                DocumentSearch.OFFSET_KEY, "0")));


            Response.ResponseBuilder response = Response.ok(responseObject, MediaType.APPLICATION_JSON_TYPE);

            return response.build();
        } catch (SecurityException e) {
            this.handleError(e, Status.UNAUTHORIZED);
        } catch (XWikiException e) {
            this.handleError(e, Status.INTERNAL_SERVER_ERROR);
        } catch (QueryException | IllegalArgumentException e) {
            this.handleError(e, Status.BAD_REQUEST);
        }

        return Response.serverError().build();
    }

    private JSONObject getResponseObject(JSONObject inputObject, Map<String, List<String>> queryParameters)
        throws QueryException, XWikiException
    {
        DocumentSearchResult<DocumentReference> documentSearchResult = this.documentSearch.search(inputObject);

        JSONObject responseObject = new JSONObject();


        JSONArray rows = new JSONArray();
        responseObject.put("rows", rows);

        XWikiContext context = this.xContextProvider.get();

        List<TableColumn> cols = this.getColumns(inputObject);

        for (DocumentReference docRef : documentSearchResult.getItems()) {
            JSONObject row = this.responseRowHandler.getRow(this.getDocument(docRef), context, cols, queryParameters);
            if (row != null) {
                rows.put(row);
            }
        }

        responseObject.put("totalrows", documentSearchResult.getTotalRows());
        responseObject.put("returnedrows", documentSearchResult.getReturnedRows());
        responseObject.put("offset", documentSearchResult.getOffset() + 1);
        return responseObject;
    }

    private XWikiDocument getDocument(DocumentReference docRef) throws XWikiException
    {
        if (docRef == null) {
            return null;
        }

        try {
            return (XWikiDocument) this.documentAccessBridge.getDocument(docRef);

        } catch (Exception e) {
            throw new XWikiException("Error while getting document " + docRef.getName(), e);
        }
    }

    private List<TableColumn> getColumns(JSONObject jsonObject)
    {
        if (jsonObject.optJSONArray(DefaultLiveTableSearchImpl.COLUMN_LIST_KEY) == null) {
            throw new IllegalArgumentException(String.format("No %1$s key found.",
                DefaultLiveTableSearchImpl.COLUMN_LIST_KEY));
        }

        JSONArray columnArray = jsonObject.getJSONArray(DefaultLiveTableSearchImpl.COLUMN_LIST_KEY);

        List<TableColumn> columns = new LinkedList<>();

        for (Object obj : columnArray) {
            if (!(obj instanceof JSONObject)) {
                throw new IllegalArgumentException(String.format("Column %1$s is not a JSONObject", obj));
            }

            columns.add(new TableColumn().populate((JSONObject) obj));
        }

        return columns;
    }

    private void authorize(JSONObject inputObject)
    {
        SpaceAndClass spaceAndClass = new SpaceAndClass(inputObject);

        User currentUser = this.users.getCurrentUser();

        EntityReference spaceRef = new EntityReference(spaceAndClass.getSpaceName(), EntityType.SPACE);

        if (!this.access.hasAccess(Right.VIEW, currentUser == null ? null : currentUser.getProfileDocument(),
            this.currentResolver.resolve(spaceRef, EntityType.SPACE))) {
            throw new SecurityException(String.format("User [%s] is not authorized to access this data", currentUser));
        }
    }

    private void handleError(Exception e, Status status)
    {
        // TODO: remove stack trace
        e.printStackTrace();
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Error encountered", e);
        }
        throw new WebApplicationException(e, status);
    }
}
