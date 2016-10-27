/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.rest;

import org.xwiki.rest.XWikiRestComponent;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Allows for adding and removing Families in Family Groups, via REST.
 *
 * @version $Id$
 */
@Path("/family-groups/{family-group-id}/families")
public interface FamilyGroupFamilies extends XWikiRestComponent
{
    /**
     * Adds the specified Family to the specified Family Group, returning JSON representing the updated Family Group.
     *
     * @param familyGroupId the Family Group ID.
     * @param json the JSON request body.
     * @return JSON representing the updated Family Group.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response addFamilyToFamilyGroup(@PathParam("family-group-id") String familyGroupId, String json);

    /**
     * Removes the specified Family from the specified Family Group, returning JSON representing the updated Family
     * Group.
     *
     * @param familyGroupId the Family Group ID.
     * @param familyId the Family ID.
     * @return JSON representing the updated Family Group.
     */
    @DELETE
    @Path("/{family-id}")
    @Produces(MediaType.APPLICATION_JSON)
    Response removeFamilyFromFamilyGroup(@PathParam("family-group-id") String familyGroupId,
        @PathParam("family-id") String familyId);
}
