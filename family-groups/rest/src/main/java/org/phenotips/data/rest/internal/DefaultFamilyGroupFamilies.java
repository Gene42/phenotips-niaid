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

import org.xwiki.component.annotation.Component;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONObject;

/**
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

    @Override
    public Response addFamilyToFamilyGroup(String familyGroupId, String json)
    {
        JSONObject jsonObj = new JSONObject(json);
        FamilyGroup familyGroup = (FamilyGroup) familyGroupManager.get(familyGroupId);
        Family family = (Family) familyManager.get(jsonObj.optString("familyId"));
        familiesInFamilyGroupManager.addMember(familyGroup, family);
        return Response.ok(familyGroup.toJSON(), MediaType.APPLICATION_JSON).build();
    }

    @Override
    public Response removeFamilyFromFamilyGroup(String familyGroupId, String familyId)
    {
        FamilyGroup familyGroup = (FamilyGroup) familyGroupManager.get(familyGroupId);
        Family family = (Family) familyManager.get(familyId);
        familiesInFamilyGroupManager.removeMember(familyGroup, family);
        return Response.ok(familyGroup.toJSON(), MediaType.APPLICATION_JSON).build();
    }

    /**
     * Generates an error response.
     *
     * @param errorText the plain text of the response.
     * @param status the HTTP status code of the response.
     * @return the Response object.
     */
    private Response generateErrorResponse(String errorText, Response.Status status)
    {
        return Response
            .status(status)
            .entity(errorText)
            .type(MediaType.TEXT_PLAIN)
            .build();
    }
}
