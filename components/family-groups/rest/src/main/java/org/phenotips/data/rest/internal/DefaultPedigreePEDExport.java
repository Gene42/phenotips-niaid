/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.rest.internal;

import org.phenotips.data.rest.PedigreePEDExport;
import org.phenotips.entities.PrimaryEntityManager;
import org.phenotips.familygroups.FamilyGroup;
import org.phenotips.familygroups.FamilyGroupPedigreeExporter;
import org.phenotips.security.authorization.AuthorizationService;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Default implementation of Family Group pedigree export to PED as a REST endpoint, using the
 * FamilyGroupPedigreeExporter service.
 *
 * @version $Id$
 */
@Component
@Named("org.phenotips.data.rest.internal.DefaultPedigreePEDExport")
@Singleton
public class DefaultPedigreePEDExport implements PedigreePEDExport
{
    @Inject
    private FamilyGroupPedigreeExporter familyGroupPedigreeExporter;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private UserManager users;

    @Inject
    @Named("FamilyGroup")
    private PrimaryEntityManager familyGroupManager;

    @Override
    public Response getPEDExport(String familyGroupId)
    {
        FamilyGroup familyGroup = (FamilyGroup) this.familyGroupManager.get(familyGroupId);
        if (familyGroup == null) {
            return generateErrorResponse("Family group not found.", Response.Status.NOT_FOUND);
        }

        // Need view permissions on the Family Group doc (currently no permission checking on the Families themselves)
        User user = this.users.getCurrentUser();
        DocumentReference familyGroupRef = familyGroup.getDocumentReference();
        if (!this.authorizationService.hasAccess(user, Right.VIEW, familyGroupRef)) {
            return generateErrorResponse("User does not have permission to view requested Family Group.",
                Response.Status.FORBIDDEN);
        }

        String pedContent =
            this.familyGroupPedigreeExporter.exportFamilyGroupAsPED(familyGroupId, new ArrayList<String>());
        if (pedContent.isEmpty()) {
            return Response.noContent().build();
        }

        Response.ResponseBuilder resp = Response.ok(pedContent, MediaType.TEXT_PLAIN);
        resp.header("Content-Disposition", "attachment; filename=\"" + familyGroupId + ".ped\"");

        return resp.build();
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
