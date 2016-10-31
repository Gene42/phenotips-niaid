/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.rest.internal;

import org.phenotips.data.rest.FamilyGroupFamilies;
import org.phenotips.entities.PrimaryEntityGroupManager;
import org.phenotips.entities.PrimaryEntityManager;
import org.phenotips.familyGroups.Family;
import org.phenotips.familyGroups.FamilyGroup;
import org.phenotips.security.authorization.AuthorizationService;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONObject;

/**
 * Default implementation of Families in Family Groups REST endpoint, using the Entities API.
 *
 * @version $Id$
 */
@Component
@Named("org.phenotips.data.rest.internal.DefaultFamilyGroupFamilies")
@Singleton
public class DefaultFamilyGroupFamilies implements FamilyGroupFamilies
{
    @Inject
    @Named("FamilyGroup")
    private PrimaryEntityManager familyGroupManager;

    @Inject
    @Named("Family")
    private PrimaryEntityManager familyManager;

    @Inject
    @Named("FamilyGroup:Family")
    private PrimaryEntityGroupManager<FamilyGroup, Family> familiesInFamilyGroupManager;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private UserManager users;

    @Override
    public Response addFamilyToFamilyGroup(String familyGroupId, String json)
    {
        JSONObject jsonObj = new JSONObject(json);

        FamilyGroup familyGroup = (FamilyGroup) familyGroupManager.get(familyGroupId);
        Family family = (Family) familyManager.get(jsonObj.optString("familyId"));

        Response resp = checkInputs(familyGroup, family);
        if (resp != null) {
            return resp;
        }

        boolean success = familiesInFamilyGroupManager.addMember(familyGroup, family);
        if (success) {
            return Response.ok(familyGroup.toJSON(), MediaType.APPLICATION_JSON).build();
        } else {
            return generateErrorResponse("The Family was not added to the Family Group due to an error.",
                Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Response removeFamilyFromFamilyGroup(String familyGroupId, String familyId)
    {
        FamilyGroup familyGroup = (FamilyGroup) familyGroupManager.get(familyGroupId);
        Family family = (Family) familyManager.get(familyId);

        Response resp = checkInputs(familyGroup, family);
        if (resp != null) {
            return resp;
        }

        boolean success = familiesInFamilyGroupManager.removeMember(familyGroup, family);

        if (success) {
            return Response.ok(familyGroup.toJSON(), MediaType.APPLICATION_JSON).build();
        } else {
            return generateErrorResponse("The Family was not removed from the Family Group due to an error.",
                Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Checks the endpoint inputs for errors, returning an appropriate response in case an error is found. Checks for
     * existence (non-null) on both inputs, as well as user permissions to modify the Family Group.
     *
     * @param familyGroup the Family Group endpoint input.
     * @param family the Family endpoint input.
     * @return null, if no errors are found.
     *         an appropriate response, in case an error is found.
     */
    private Response checkInputs(FamilyGroup familyGroup, Family family)
    {
        if (familyGroup == null) {
            return generateErrorResponse("Family Group not found.", Response.Status.NOT_FOUND);
        }
        if (family == null) {
            return generateErrorResponse("Family not found.", Response.Status.NOT_FOUND);
        }

        if (!curUserHasEditPermission(familyGroup)) {
            return generateErrorResponse("User lacks permission to add or remove Families for this Family Group.",
                Response.Status.FORBIDDEN);
        }
        return null;
    }

    /**
     * Returns whether the current user has edit permission the given Family Group.
     *
     * @param familyGroup the Family Group to check.
     * @return true if the current user has edit permission.
     *         false if the current user doesn't have edit permission.
     */
    private boolean curUserHasEditPermission(FamilyGroup familyGroup)
    {
        User user = this.users.getCurrentUser();
        DocumentReference familyGroupRef = familyGroup.getDocumentReference();
        return this.authorizationService.hasAccess(user, Right.EDIT, familyGroupRef);
    }

    /**
     * Generates an error response.
     *
     * @param errorText the text of the response, which will be returned as the value for an "error" key in a JSON
     *                  object.
     * @param status the HTTP status code of the response.
     * @return the Response object.
     */
    private Response generateErrorResponse(String errorText, Response.Status status)
    {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("error", errorText);

        return Response
            .status(status)
            .entity(jsonObj)
            .type(MediaType.APPLICATION_JSON)
            .build();
    }
}
