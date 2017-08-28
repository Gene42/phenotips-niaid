/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package com.gene42.phenotips.permissions.rest.internal;

import org.phenotips.data.api.EntitySearch;
import org.phenotips.data.api.internal.builder.DocumentSearchBuilder;
import org.phenotips.data.api.internal.builder.PatientSearchBuilder;
import org.phenotips.data.api.internal.filter.ReferenceClassFilter;
import org.phenotips.data.permissions.rest.PermissionsResource;
import org.phenotips.data.permissions.rest.model.CollaboratorRepresentation;
import org.phenotips.data.permissions.rest.model.CollaboratorsRepresentation;
import org.phenotips.data.permissions.rest.model.OwnerRepresentation;
import org.phenotips.data.permissions.rest.model.PermissionsRepresentation;
import org.phenotips.data.permissions.rest.model.VisibilityRepresentation;
import org.phenotips.data.rest.LiveTableInputAdapter;
import org.phenotips.data.rest.LiveTableSearch;
import org.phenotips.data.rest.internal.DefaultLiveTableSearchImpl;
import org.phenotips.data.rest.internal.RequestUtils;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.XWikiRestComponent;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.gene42.commons.utils.web.WebUtils;
import com.gene42.phenotips.permissions.rest.BatchPermissions;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;

/**
 * Default implementation of the BatchPermissions interface.
 *
 * @version $Id$
 */
@Component
@Named("com.gene42.phenotips.permissions.rest.internal.DefaultBatchPermissionsImpl")
@Singleton
public class DefaultBatchPermissionsImpl implements BatchPermissions
{
    private static final String PERMISSIONS_COMPONENT_NAME =
        "org.phenotips.data.permissions.rest.internal.DefaultPermissionsResourceImpl";
    private static final String DATA_ARRAY_KEY = "data";

    private static final String ID_KEY = "id";
    private static final String OWNER_KEY = "owner";
    private static final String VISIBILITY_KEY = "visibility";
    private static final String COLLABORATORS_KEY = "collaborators";
    private static final String LEVEL_KEY = "level";

    @Inject
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    @Named(DefaultLiveTableSearchImpl.NAME)
    @Inject
    private LiveTableSearch liveTableSearch;

    @Inject
    private EntitySearch<DocumentReference> documentSearch;

    @Inject
    private Provider<XWikiContext> xContextProvider;

    @Inject
    @Named("url")
    private LiveTableInputAdapter inputAdapter;

    @Override
    public Response setPermissions(String jsonString)
    {
        return this.modifyPermissions(jsonString, true);
    }

    @Override
    public Response updatePermissions(String jsonString)
    {
        return this.modifyPermissions(jsonString, false);
    }

    @Override
    public Response search()
    {
        Response firstResponse = this.liveTableSearch.search();
        if (firstResponse.getStatus() != Response.Status.OK.getStatusCode()) {
            return firstResponse;
        }

        JSONObject firstJSON = (JSONObject) firstResponse.getEntity();

        XWikiRequest xwikiRequest = this.xContextProvider.get().getRequest();

        HttpServletRequest httpServletRequest = xwikiRequest.getHttpServletRequest();

        Map<String, List<String>> queryParameters = RequestUtils.getQueryParameters(httpServletRequest
            .getQueryString());

        JSONObject inputObject = this.inputAdapter.convert(queryParameters);

        DocumentSearchBuilder builder = new PatientSearchBuilder()
            .onlyForUser("test", null)
            .newSubQuery(new PatientSearchBuilder())
            .newObjectFilter().setDocSpaceAndClass("PhenoTips.FamilyReferenceClass").back()
            .newStringFilter(ReferenceClassFilter.TYPE).setType(ReferenceClassFilter.TYPE)
                .setSpaceAndClass(PatientSearchBuilder.PATIENT_CLASS).back();

        builder.build();

        return null;
    }

    private Response modifyPermissions(String jsonString, boolean overwrite)
    {
        JSONObject jsonObject = WebUtils.parseToJSONObject(jsonString);
        JSONArray array = WebUtils.getJSONObjectValue(jsonObject, DATA_ARRAY_KEY, JSONArray.class);

        PermissionsResource permissionsResource;

        try {
            permissionsResource =
                this.componentManager.getInstance(XWikiRestComponent.class, PERMISSIONS_COMPONENT_NAME);
        } catch (ComponentLookupException e) {
            this.logger.error(e.getMessage(), e);
            return WebUtils.getErrorResponse(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        }

        for (int i = 0, len = array.length(); i < len; i++) {
            this.handleEntry(array.optJSONObject(i), permissionsResource, overwrite);
        }

        return Response.ok().build();
    }

    private void handleEntry(JSONObject entry, PermissionsResource permissionsResource, boolean overwrite)
    {
        if (entry == null) {
            return;
        }

        PermissionsRepresentation permission = new PermissionsRepresentation()
            .withOwner(getOwnerRepresentation(entry, overwrite))
            .withVisibility(getVisibilityRepresentation(entry, overwrite))
            .withCollaborators(getCollaboratorsRepresentation(entry, overwrite));

        String id = WebUtils.getJSONObjectValue(entry, ID_KEY, String.class);

        if (overwrite) {
            permissionsResource.setPermissions(permission, id);
        } else {
            permissionsResource.updatePermissions(permission, id);
        }
    }

    private static OwnerRepresentation getOwnerRepresentation(JSONObject entry, boolean required)
    {
        if (!required && !entry.has(OWNER_KEY)) {
            return null;
        }
        return new OwnerRepresentation()
            .withId(getID(WebUtils.getJSONObjectValue(entry, OWNER_KEY, JSONObject.class)));
    }

    private static VisibilityRepresentation getVisibilityRepresentation(JSONObject entry, boolean required)
    {
        if (!required && !entry.has(VISIBILITY_KEY)) {
            return null;
        }
        return new VisibilityRepresentation()
            .withLevel(getLevel(WebUtils.getJSONObjectValue(entry, VISIBILITY_KEY, JSONObject.class)));
    }

    private static CollaboratorsRepresentation getCollaboratorsRepresentation(JSONObject entry, boolean required)
    {
        Collection<CollaboratorRepresentation> col = new LinkedList<>();

        if (!required && !entry.has(COLLABORATORS_KEY)) {
            return null;
        }

        JSONObject wrapper = WebUtils.getJSONObjectValue(entry, COLLABORATORS_KEY, JSONObject.class);

        for (Object obj : WebUtils.getJSONObjectValue(wrapper, COLLABORATORS_KEY, JSONArray.class)) {
            col.add(getCollaboratorRepresentation(WebUtils.castJSONObject(obj, JSONObject.class)));
        }

        return new CollaboratorsRepresentation().withCollaborators(col);
    }

    private static CollaboratorRepresentation getCollaboratorRepresentation(JSONObject entry)
    {
        return new CollaboratorRepresentation().withLevel(getLevel(entry)).withId(getID(entry));
    }

    private static String getLevel(JSONObject obj)
    {
        return WebUtils.getJSONObjectValue(obj, LEVEL_KEY, String.class);
    }

    private static String getID(JSONObject obj)
    {
        return WebUtils.getJSONObjectValue(obj, ID_KEY, String.class);
    }
}
