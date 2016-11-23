/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.rest;

import org.xwiki.component.annotation.Role;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;

/**
 * Interface for adapting a URL string into a JSONObject to be sent to the Document Search API.
 *
 * @version $Id$
 */
@Role
public interface LiveTableInputAdapter
{
    /**
     * Converts the given query parameter map into a usable/valid JSONObject required by the Document Search API
     * as input.
     * @param queryParameters the query parameter map to convert
     * @return a JSONObject
     */
    JSONObject convert(Map<String, List<String>> queryParameters);
}
