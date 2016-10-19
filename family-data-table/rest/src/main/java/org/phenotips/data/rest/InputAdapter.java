package org.phenotips.data.rest;

import javax.ws.rs.core.UriInfo;

import org.json.JSONObject;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public interface InputAdapter
{

    JSONObject convert(UriInfo uriInfo);
}
