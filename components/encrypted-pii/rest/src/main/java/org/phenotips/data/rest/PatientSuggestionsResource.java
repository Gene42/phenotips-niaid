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
 * Patient suggestions resource, providing suggested patients using the supplied search string, which contains the year
 * of birth and the last name initial.
 *
 * @version $Id$
 */
@Path("/patients/suggest")
public interface PatientSuggestionsResource extends XWikiRestComponent
{
    /**
     * Suggests patients using the supplied search string, which contains the year of birth and the last name initial.
     * The first 4-digit number in the string will be taken as the birth year and the first letter in the string will be
     * taken as the last name initial. If either the year or the initial are not supplied, no results will be returned.
     *
     * @param input search string, which contains the year of birth and the last name initial.
     * @param limit the number of patients to which this result should be limited
     * @return a response containing the set of patients matching the search criteria.
     * @throws Exception in case there is an error generating the response XML or executing the search query
     */
    @GET
    @Produces(MediaType.APPLICATION_XML)
    Response suggestPatients(@QueryParam("input") String input, @QueryParam("nb") String limit) throws Exception;
}
