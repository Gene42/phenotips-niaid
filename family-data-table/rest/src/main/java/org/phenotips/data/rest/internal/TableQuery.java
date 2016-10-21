/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.rest.internal;

import java.util.List;

import org.json.JSONObject;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class TableQuery
{
    public static final String LIMIT_KEY = "limit";

    private int limit;

    private List<String> columnsToReturn;

    private String documentClass;

    private JSONObject searchFilters;

    public TableQuery populate(JSONObject object)
    {

        return this;
    }
}
