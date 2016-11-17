/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api;

import org.xwiki.component.annotation.Role;
import org.xwiki.query.QueryException;

import org.json.JSONObject;

/**
 * DESCRIPTION.
 * @param <T> result type
 * @version $Id$
 */
@Role
public interface DocumentSearch<T>
{
    //String CLASSNAME_KEY = "classname";

    /** Class property key */
    String CLASS_KEY = "class";

    String LIMIT_KEY = "limit";
    String OFFSET_KEY = "offset";
    String ORDER_KEY = "sort";
    String ORDER_DIR_KEY = "dir";
    String REQUEST_NUMBER_KEY = "reqNo";
    String OUTPUT_SYNTAX_KEY = "outputSyntax";
    String FILTER_WHERE_KEY = "filterWhere";
    String FILTER_FROM_KEY = "filterFrom";
    String QUERY_FILTERS_KEY = "queryFilters";
    String COLUMN_LIST_KEY = "collist";

    /**
     *
     * @param queryParameters the parameters used to create the query
     * @return a DocumentSearchResult containing the documents plus extra metadata
     * @throws QueryException on any issues during document querying
     * @throws SecurityException if user is not authorized to search for data
     */
    DocumentSearchResult<T> search(JSONObject queryParameters) throws QueryException;
}
