/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package com.gene42.phenotips.permissions.rest.internal;

import org.phenotips.data.api.EntitySearch;
import org.phenotips.data.api.EntitySearchResult;
import org.phenotips.data.api.internal.builder.DocumentSearchBuilder;
import org.phenotips.data.api.internal.builder.PatientSearchBuilder;
import org.phenotips.data.api.internal.builder.ReferenceValue;
import org.phenotips.data.permissions.rest.PermissionsResource;
import org.phenotips.data.permissions.rest.model.CollaboratorRepresentation;
import org.phenotips.data.permissions.rest.model.CollaboratorsRepresentation;
import org.phenotips.data.permissions.rest.model.OwnerRepresentation;
import org.phenotips.data.permissions.rest.model.PermissionsRepresentation;
import org.phenotips.data.permissions.rest.model.VisibilityRepresentation;
import org.phenotips.data.rest.LiveTableGenerator;
import org.phenotips.data.rest.LiveTableInputAdapter;
import org.phenotips.data.rest.internal.LiveTableFacade;
import org.phenotips.data.rest.internal.LiveTableStopWatches;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.XWikiRestComponent;
import org.xwiki.users.User;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.gene42.commons.utils.exceptions.ServiceException;
import com.gene42.commons.utils.json.JSONTools;
import com.gene42.commons.utils.web.WebUtils;
import com.gene42.commons.xwiki.XWikiTools;
import com.gene42.phenotips.permissions.rest.BatchPermissions;

/**
 * Default implementation of the BatchPermissions interface.
 *
 * @version $Id$
 */
@Component
@Named("com.gene42.phenotips.permissions.rest.internal.DefaultBatchPermissionsImpl")
@Singleton
@SuppressWarnings({"checkstyle:ClassFanOutComplexity", "checkstyle:ClassDataAbstractionCoupling"})
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

    private static final String DOC_FULL_NAME = "doc.fullName";
    private static final String MANAGE_RIGHT = "manage";

    private static final String FAMILY_REFERENCE_CLASS = "PhenoTips.FamilyReferenceClass";

    @Inject
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    @Inject
    private XWikiTools xWikiTools;

    @Inject
    private LiveTableGenerator<DocumentReference> liveTableGenerator;

    @Inject
    private LiveTableFacade liveTableFacade;

    @Inject
    private EntitySearch<DocumentReference> documentSearch;

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
    public Response getEntities()
    {
        try {
            LiveTableStopWatches stopWatches = new LiveTableStopWatches();

            Map<String, List<String>> queryParameters = this.liveTableFacade.getQueryParameters();

            stopWatches.getAdapterStopWatch().start();
            JSONObject withFamily = this.inputAdapter.convert(queryParameters);
            stopWatches.getAdapterStopWatch().stop();

            this.liveTableFacade.authorizeEntitySearchInput(withFamily);


            stopWatches.getSearchStopWatch().start();
            EntitySearchResult<DocumentReference> firstSearchResult = this.documentSearch.search(withFamily);
            stopWatches.getSearchStopWatch().suspend();

            long offset = Long.parseLong(JSONTools.getValue(withFamily, EntitySearch.Keys.OFFSET_KEY, "0"));
            int limit = Integer.parseInt(JSONTools.getValue(withFamily, EntitySearch.Keys.LIMIT_KEY, "25"));

            int withFamReturned = firstSearchResult.getItems().size();
            int withoutFamNeeded = Math.abs(limit - withFamReturned);

            long withoutFamOffset = offset - firstSearchResult.getTotalRows();
            if (withoutFamOffset < 0) {
                withoutFamOffset = 0;
            }

            JSONObject withoutFamily = this.getPatientsWithoutFamiliesQueryInput(withoutFamOffset, withoutFamNeeded);

            stopWatches.getSearchStopWatch().resume();
            EntitySearchResult<DocumentReference> secondSearchResult = this.documentSearch.search(withoutFamily);
            stopWatches.getSearchStopWatch().stop();

            EntitySearchResult<DocumentReference> result = mergeSearchResults(firstSearchResult, secondSearchResult,
                offset);

            JSONObject output = this.liveTableGenerator.generateTable(result, withFamily, queryParameters);

            return Response.ok(output.toString()).build();
        } catch (ServiceException e) {
            WebUtils.throwWebApplicationException(e, this.logger);
        }

        return Response.serverError().build();
    }

    private JSONObject getPatientsWithoutFamiliesQueryInput(long offset, int limit) throws ServiceException
    {
        User user = this.xWikiTools.getUserManager().getCurrentUser();
        Set<String> groups = this.xWikiTools.getGroupsUserBelongsTo(user);

        DocumentSearchBuilder outerQuery = new PatientSearchBuilder()
            .setOffset(offset).setLimit(limit).setCountOnly(limit <= 0);

        boolean isNotAdmin = !this.xWikiTools.isUserAdmin(user);

        if (isNotAdmin) {
            outerQuery.onlyForUser(user, groups, true, null, MANAGE_RIGHT);
        }

        DocumentSearchBuilder innerQuery = outerQuery.newSubQuery(new PatientSearchBuilder()).setNegate(true)
            .newObjectFilter().setSpaceAndClass(FAMILY_REFERENCE_CLASS).back()
            .newStringFilter(DOC_FULL_NAME).setSpaceAndClass(PatientSearchBuilder.PATIENT_CLASS)
            .addReferenceValue(new ReferenceValue()
                .setLevel(-1)
                .setPropertyName(DOC_FULL_NAME)
                .setSpaceAndClass(PatientSearchBuilder.PATIENT_CLASS)
            ).back();

        if (isNotAdmin) {
            innerQuery.onlyForUser(user, groups, true, null, MANAGE_RIGHT);
        }


        return outerQuery.build();
    }

    private static EntitySearchResult<DocumentReference> mergeSearchResults(
        EntitySearchResult<DocumentReference> firstSearchResult,
        EntitySearchResult<DocumentReference> secondSearchResult, long initialOffset)
    {
        EntitySearchResult<DocumentReference> result = new EntitySearchResult<>();
        result.getItems().addAll(firstSearchResult.getItems());
        result.getItems().addAll(secondSearchResult.getItems());
        result.setTotalRows(firstSearchResult.getTotalRows() + secondSearchResult.getTotalRows());
        result.setOffset(initialOffset);
        return result;
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
