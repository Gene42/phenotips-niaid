package org.phenotips.data.api.internal.filter;

import org.json.JSONObject;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public abstract class AbstractFilterFactory
{
    public abstract AbstractFilter getFilter(JSONObject obj);
}
