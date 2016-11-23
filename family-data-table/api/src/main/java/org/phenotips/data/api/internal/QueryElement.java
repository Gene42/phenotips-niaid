package org.phenotips.data.api.internal;

import java.util.List;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public interface QueryElement
{
    QueryElement createBindings();

    QueryBuffer addValueConditions(QueryBuffer where, List<Object> bindingValues);

    boolean isValid();

    /** Flag determining whether or not this query element contributes to a valid query; as in: if the query
     * has no other element but this one, does it makes sense to run the query at all with just myself ( think
     * about a ReferenceClassFilter which only makes sense to include if */
    boolean validatesQuery();
}
