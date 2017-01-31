/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.rest;

import org.phenotips.rest.RequiredAccess;

import org.xwiki.rest.XWikiRestComponent;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Resource for working with patient records, identified by their internal PhenoTips identifier.
 *
 * @version $Id$
 */
@Path("/entities")
public interface LiveTableSearch extends XWikiRestComponent
{
    /**
     * Retrieve a patient record, identified by its internal PhenoTips identifier, in its JSON representation. If the
     * indicated patient record doesn't exist, or if the user sending the request doesn't have the right to view the
     * target patient record, an error is returned.
     *
     * @return the JSON representation of the requested patient, or a status message in case of error
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RequiredAccess("view")
    Response search();
}

