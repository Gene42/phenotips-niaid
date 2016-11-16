package org.phenotips.data.api.internal;

import java.util.List;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public interface QueryElement
{
    QueryBuffer bindProperty(QueryBuffer where, List<Object> bindingValues);
    QueryBuffer addValueConditions(QueryBuffer where, List<Object> bindingValues);
}
