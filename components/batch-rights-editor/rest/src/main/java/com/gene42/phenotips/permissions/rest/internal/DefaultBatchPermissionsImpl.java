package com.gene42.phenotips.permissions.rest.internal;

import org.phenotips.data.permissions.rest.PermissionsResource;
import org.phenotips.data.permissions.rest.model.CollaboratorRepresentation;
import org.phenotips.data.permissions.rest.model.CollaboratorsRepresentation;
import org.phenotips.data.permissions.rest.model.OwnerRepresentation;
import org.phenotips.data.permissions.rest.model.PermissionsRepresentation;
import org.phenotips.data.permissions.rest.model.VisibilityRepresentation;

import org.xwiki.component.annotation.Component;

import java.util.Collection;
import java.util.LinkedList;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import com.gene42.commons.utils.web.WebUtils;
import com.gene42.phenotips.permissions.rest.BatchPermissions;

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
    private static final String DATA_ARRAY_KEY = "data";

    private static final String ID_KEY = "id";
    private static final String OWNER_KEY = "owner";
    private static final String VISIBILITY_KEY = "visibility";
    private static final String COLLABORATORS_KEY = "collaborators";
    private static final String LEVEL_KEY = "level";

    @Inject
    @Named("org.phenotips.data.permissions.rest.internal.DefaultPermissionsResourceImpl")
    private PermissionsResource permissionsResource;

    @Override
    public Response setPermissions(JSONObject jsonObject)
    {
        if (jsonObject == null) {
            throw new WebApplicationException(
                WebUtils.getErrorResponse("Null JSON.", Response.Status.BAD_REQUEST)
            );
        }

        JSONArray array = WebUtils.getJSONObjectValue(jsonObject, DATA_ARRAY_KEY, JSONArray.class);

        for (int i = 0, len = array.length(); i < len; i++) {
            this.handleEntry(array.optJSONObject(i));
        }

        return Response.ok().build();
    }

    private void handleEntry(JSONObject entry)
    {
        if (entry == null) {
            return;
        }

        this.permissionsResource.setPermissions(
            new PermissionsRepresentation()
            .withOwner(getOwnerRepresentation(entry))
            .withVisibility(getVisibilityRepresentation(entry))
            .withCollaborators(getCollaboratorsRepresentation(entry)),
            WebUtils.getJSONObjectValue(entry, ID_KEY, String.class)
        );
    }

    private static OwnerRepresentation getOwnerRepresentation(JSONObject entry)
    {
        return new OwnerRepresentation()
            .withId(getID(WebUtils.getJSONObjectValue(entry, OWNER_KEY, JSONObject.class)));
    }

    private static VisibilityRepresentation getVisibilityRepresentation(JSONObject entry)
    {
        return new VisibilityRepresentation()
            .withLevel(getLevel(WebUtils.getJSONObjectValue(entry, VISIBILITY_KEY, JSONObject.class)));
    }

    private static CollaboratorsRepresentation getCollaboratorsRepresentation(JSONObject entry)
    {
        Collection<CollaboratorRepresentation> col = new LinkedList<>();

        for (Object obj : WebUtils.getJSONObjectValue(entry, COLLABORATORS_KEY, JSONArray.class)) {
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
