/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal;

import org.phenotips.data.api.internal.filter.AbstractFilter;

import java.util.Set;

import org.json.JSONObject;

/**
 * The default factory for generating filters.
 *
 * @version $Id$
 */
public abstract class AbstractFilterFactory
{
    /**
     * Returns an instance of a filter defined by the given json object.
     * @param obj the JSONObject containing the configuration needed to instantiate the filter
     * @return an AbstractFilter instance
     */
    public abstract AbstractFilter getFilter(JSONObject obj);

    /**
     * Gets the set of filter parameter names used to store values. Filters which do not have value parameters, are
     * considered empty (some special filters can be empty, like the reference filter).
     *
     * Implementing factories should make sure it includes all supported filters (which have custom value param names)
     *
     * @return a set containing all the parameter values which define a filter as non empty
     */
    public abstract Set<String> getValueParameterNames();
}
