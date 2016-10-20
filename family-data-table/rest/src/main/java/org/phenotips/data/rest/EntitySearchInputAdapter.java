package org.phenotips.data.rest;

import org.xwiki.component.annotation.Role;

import javax.ws.rs.core.UriInfo;

import org.json.JSONObject;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
@Role
public interface EntitySearchInputAdapter
{

    JSONObject convert(UriInfo uriInfo);
}
