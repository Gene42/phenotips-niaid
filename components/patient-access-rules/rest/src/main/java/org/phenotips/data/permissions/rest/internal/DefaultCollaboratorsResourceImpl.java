/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.data.permissions.rest.internal;

import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.Collaborator;
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.data.permissions.internal.DefaultCollaborator;
import org.phenotips.data.permissions.rest.CollaboratorsResource;
import org.phenotips.data.permissions.rest.DomainObjectFactory;
import org.phenotips.data.permissions.rest.PermissionsResource;
import org.phenotips.data.permissions.rest.Relations;
import org.phenotips.data.permissions.rest.internal.utils.PatientAccessContext;
import org.phenotips.data.permissions.rest.internal.utils.SecureContextFactory;
import org.phenotips.data.rest.PatientResource;
import org.phenotips.data.rest.model.CollaboratorsRepresentation;
import org.phenotips.data.rest.model.Link;

import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.rest.XWikiResource;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Default implementation for {@link CollaboratorsResource} using XWiki's support for REST resources.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Component
@Named("org.phenotips.data.permissions.rest.internal.DefaultCollaboratorsResourceImpl")
@Singleton
public class DefaultCollaboratorsResourceImpl extends XWikiResource implements CollaboratorsResource
{
    private static final String LEVEL = "level";

    private static final String MANAGE_LEVEL = "manage";

    private static final EntityReference XWIKI_SPACE = new EntityReference("XWiki", EntityType.SPACE);

    @Inject
    private Logger logger;

    @Inject
    private SecureContextFactory secureContextFactory;

    /** Fills in missing reference fields with those from the current context document to create a full reference. */
    @Inject
    @Named("current")
    private EntityReferenceResolver<String> currentResolver;

    @Inject
    private DomainObjectFactory factory;

    @Inject
    private PermissionsManager manager;

    @Inject
    private Container container;

    @Override
    public CollaboratorsRepresentation getCollaborators(String patientId)
    {
        this.logger.debug("Retrieving collaborators of patient record [{}] via REST", patientId);
        // besides getting the patient, checks that the user has view access
        PatientAccessContext patientAccessContext = this.secureContextFactory.getContext(patientId, "view");

        CollaboratorsRepresentation result =
            this.factory.createCollaboratorsRepresentation(patientAccessContext.getPatient(), this.uriInfo);

        // factor these out as common
        result.withLinks(new Link().withRel(Relations.SELF).withHref(this.uriInfo.getRequestUri().toString()),
            new Link().withRel(Relations.PATIENT_RECORD)
                .withHref(this.uriInfo.getBaseUriBuilder().path(PatientResource.class).build(patientId).toString()),
            new Link().withRel(Relations.PERMISSIONS)
                .withHref(this.uriInfo.getBaseUriBuilder().path(PermissionsResource.class).build(patientId).toString())
        );

        return result;
    }

    @Override
    public Response postCollaboratorWithJson(String json, String patientId)
    {
        try {
            CollaboratorInfo info = this.collaboratorInfoFromJson(JSONObject.fromObject(json));
            return postCollaborator(info.id, info.level, patientId);
        } catch (Exception ex) {
            this.logger.error("The json was not properly formatted", ex.getMessage());
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public Response postCollaboratorWithForm(String patientId)
    {
        Object idInRequest = container.getRequest().getProperty("collaborator");
        Object levelInRequest = container.getRequest().getProperty(LEVEL);
        if (idInRequest instanceof String && levelInRequest instanceof String) {
            String id = idInRequest.toString().trim();
            String level = levelInRequest.toString().trim();
            if (StringUtils.isNotBlank(id) && StringUtils.isNotBlank(level)) {
                return postCollaborator(id, level, patientId);
            }
        }
        this.logger.error("The id, permissions level, or both were not provided or are invalid");
        throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    @Override
    public Response deleteCollaborators(String patientId)
    {
        return this.updateCollaborators(new LinkedList<Collaborator>(), patientId);
    }

    @Override
    public Response putCollaborators(String json, String patientId)
    {
        List<Collaborator> collaborators = this.jsonToCollaborators(json);
        return this.updateCollaborators(collaborators, patientId);
    }

    private Response postCollaborator(String collaboratorId, String accessLevelName, String patientId)
    {
        this.checkCollaboratorInfo(collaboratorId, accessLevelName);

        this.logger.debug(
            "Adding collaborator [{}] with permission level [{}] to the patient record [{}] via REST",
            collaboratorId, accessLevelName, patientId);
        // besides getting the patient, checks that the user has manage access
        PatientAccessContext patientAccessContext = this.secureContextFactory.getContext(patientId, MANAGE_LEVEL);
        PatientAccess patientAccess = patientAccessContext.getPatientAccess();

        // will throw an error if something goes wrong
        this.addCollaborator(collaboratorId, accessLevelName.trim(), patientAccess);
        return Response.noContent().build();
    }

    private Response updateCollaborators(Collection<Collaborator> collaborators, String patientId)
    {
        // besides getting the patient, checks that the user has manage access
        PatientAccessContext patientAccessContext = this.secureContextFactory.getContext(patientId, MANAGE_LEVEL);
        PatientAccess patientAccess = patientAccessContext.getPatientAccess();

        if (!patientAccess.updateCollaborators(collaborators)) {
            this.logger.error("Could not update collaborators");
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
        return Response.noContent().build();
    }

    private void addCollaborator(String id, String levelName, PatientAccess patientAccess)
        throws WebApplicationException
    {
        // checking that the access level is valid
        AccessLevel level = this.getAccessLevelFromString(levelName);
        EntityReference collaboratorReference = this.currentResolver.resolve(id, EntityType.DOCUMENT, XWIKI_SPACE);

        // todo. function .addCollaborator has to check if the collaborator already exists before adding them
        if (!patientAccess.addCollaborator(collaboratorReference, level)) {
            // todo. should this status be an internal server error, or a bad request?
            this.logger.error("Could not add a collaborator [{}] with access level [{}]", id, levelName);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private AccessLevel getAccessLevelFromString(String accessLevelName)
    {
        for (AccessLevel accessLevelOption : this.manager.listAccessLevels())
        {
            if (StringUtils.equalsIgnoreCase(accessLevelOption.getName(), accessLevelName)) {
                return accessLevelOption;
            }
        }
        this.logger.error("The access level name does not match any available levels");
        throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    private void checkCollaboratorInfo(String collaboratorId, String levelName)
    {
        if (StringUtils.isBlank(collaboratorId)) {
            this.logger.error("The collaborator id was not provided");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        if (StringUtils.isBlank(levelName)) {
            this.logger.error("The permissions level was not provided");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }

    private CollaboratorInfo collaboratorInfoFromJson(final JSONObject json)
    {
        return new CollaboratorInfo(json.optString("id"), json.optString(LEVEL));
    }

    private List<Collaborator> jsonToCollaborators(String json)
    {
        List<Collaborator> collaborators = new LinkedList<>();
        JSONArray collaboratorsArray = JSONArray.fromObject(json);
        for (Object collaboratorObject : collaboratorsArray) {
            CollaboratorInfo collaboratorInfo =
                this.collaboratorInfoFromJson(JSONObject.fromObject(collaboratorObject));
            this.checkCollaboratorInfo(collaboratorInfo.getId(), collaboratorInfo.getLevel());

            EntityReference collaboratorReference =
                this.currentResolver.resolve(collaboratorInfo.id, EntityType.DOCUMENT, XWIKI_SPACE);
            AccessLevel level = this.getAccessLevelFromString(collaboratorInfo.level);
            Collaborator collaborator = new DefaultCollaborator(collaboratorReference, level, null);
            collaborators.add(collaborator);
        }
        return collaborators;
    }

    private class CollaboratorInfo
    {
        private String id;
        private String level;

        CollaboratorInfo(String id, String level)
        {
            this.id = id;
            this.level = level;
        }

        public String getId()
        {
            return id;
        }

        public String getLevel()
        {
            return level;
        }
    }
}
