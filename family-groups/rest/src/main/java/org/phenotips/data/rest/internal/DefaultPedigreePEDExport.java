/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.rest.internal;

import org.phenotips.data.rest.PedigreePEDExport;
import org.phenotips.familyGroups.FamilyGroupPedigreeExporter;

import org.xwiki.component.annotation.Component;

import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @version $Id$
 */
@Component
@Named("org.phenotips.data.rest.internal.DefaultPedigreePEDExport")
@Singleton
public class DefaultPedigreePEDExport implements PedigreePEDExport
{
    @Inject
    private FamilyGroupPedigreeExporter familyGroupPedigreeExporter;

    @Override
    public Response getPEDExport(String familyGroupId)
    {
        String pedContent = familyGroupPedigreeExporter.exportFamilyGroupAsPED(familyGroupId, new ArrayList<String>());

        Response.ResponseBuilder resp = Response.ok(pedContent, MediaType.TEXT_PLAIN);
        resp.header("Content-Disposition","attachment; filename=\"" + familyGroupId + ".ped\"");

        return resp.build();
    }
}
