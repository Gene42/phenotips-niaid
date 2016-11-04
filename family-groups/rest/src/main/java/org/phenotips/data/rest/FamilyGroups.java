/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.rest;

import org.xwiki.rest.XWikiRestComponent;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Provides Family Groups via REST.
 *
 * @version $Id$
 */
@Path("/family-groups")
public interface FamilyGroups extends XWikiRestComponent
{
    /**
     * Retrieves a set of Family Groups using an input string to filter results. Returns a JSON response containing
     * basic data for each Family Group, limited to a specific number of Family Groups returned per query.
     *
     * @param input a string used to filter results. The string will be used for partial word search in the names and
     *              IDs of existing Family Groups.
     * @return a JSON object containing a key called "matchedFamilyGroups", which corresponds to an array of match
     *         results.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Response getFamilyGroups(@QueryParam("input") String input);
}
