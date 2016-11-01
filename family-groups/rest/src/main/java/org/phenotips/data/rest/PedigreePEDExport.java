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
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Presents Family Group pedigree export to PED as a REST endpoint.
 *
 * @version $Id$
 */
@Path("/family-groups/{family-group-id}/pedigree/ped")
public interface PedigreePEDExport extends XWikiRestComponent
{
    /**
     * Returns a response containing a Family Group exported to PED format. The response behaves as a file download.
     *
     * @param familyGroupId the ID of the Family Group to export.
     * @return a response containing a Family Group exported to PED format.
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @RequiredAccess("view")
    Response getPEDExport(@PathParam("family-group-id") String familyGroupId);
}

