/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.rest.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.rest.Search;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.rest.XWikiResource;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.json.JSONObject;

/**
 * Default implementation for using XWiki's support for REST resources.
 *
 * @version $Id$
 */
@Component
@Named("org.phenotips.data.rest.internal.DefaultSearchImpl")
@Singleton
public class DefaultSearchImpl extends XWikiResource implements Search
{
    @Inject
    private UserManager users;

    @Inject
    private AuthorizationManager access;

    /** Fills in missing reference fields with those from the current context document to create a full reference. */
    @Inject
    @Named("current")
    private EntityReferenceResolver<EntityReference> currentResolver;

    @Override public Response search(@Context UriInfo uriInfo)
    {

        User currentUser = this.users.getCurrentUser();

        if (!this.access.hasAccess(Right.VIEW, currentUser == null ? null : currentUser.getProfileDocument(),
            this.currentResolver.resolve(Patient.DEFAULT_DATA_SPACE, EntityType.SPACE))) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }

        //GenericEntity ge = new GenericEntity(null, );
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("Testing Search", "YES");

        if (currentUser != null) {
            JSONObject currentUserJSON = new JSONObject();
            currentUserJSON.put("name", currentUser.getName());
            currentUserJSON.put("id", currentUser.getId());
            jsonObject.put("current_user", currentUserJSON);
        }

        Response.ResponseBuilder response = Response.ok(jsonObject, MediaType.APPLICATION_JSON_TYPE);

        return response.build();
    }
}
