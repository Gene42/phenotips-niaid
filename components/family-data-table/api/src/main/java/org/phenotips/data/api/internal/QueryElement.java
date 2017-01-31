/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal;

import java.util.List;

/**
 * This interface defines a a query element such as an expression or a filter.
 *
 * @version $Id$
 */
public interface QueryElement
{
    /**
     * This method binds the query element to its parent DocumentQuery.
     *
     * @return the query element itself (this object)
     */
    QueryElement createBindings();

    /**
     * Adds this query element's value conditions (and those of its child expressions) to the given query buffer.
     *
     * @param where the QueryBuffer to append to
     * @param bindingValues the binding value list to add values to
     * @return the query element itself (this object)
     */
    QueryBuffer addValueConditions(QueryBuffer where, List<Object> bindingValues);

    /**
     * Returns true if this query element is valid and thus eligible to be added to the query, false otherwise.
     * @return boolean value
     */
    boolean isValid();

    /** Flag determining whether or not this query element contributes to a valid query; as in: if the query
     *  has no other element but this one, does it makes sense to run the query at all with just myself ( think
     *  about a ReferenceClassFilter which only makes sense to include if.
     * @return true if this query element validates a query
     */
    boolean validatesQuery();
}
