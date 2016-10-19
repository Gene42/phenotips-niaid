package org.phenotips.data.rest.internal;

import org.phenotips.data.api.internal.filter.EntityFilter;

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
