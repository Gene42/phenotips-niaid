package org.phenotips.data.rest.internal;

import org.phenotips.data.api.internal.filter.EntityFilter;

import java.util.List;

import org.json.JSONObject;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class QueryBlock
{
    private int limit;

    private List<String> columnsToReturn;

    private String documentClass;

    private EntityFilter doc;

    public QueryBlock populate(JSONObject object)
    {
        return this;
    }
}
