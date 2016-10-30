/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.rest;

import org.xwiki.component.annotation.Role;

import javax.ws.rs.core.MultivaluedMap;
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

    JSONObject convert(MultivaluedMap<String, String> queryParameters);
}
