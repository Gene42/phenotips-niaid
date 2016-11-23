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
 * Interface for Document search api.
 * @param <T> result type
 * @version $Id$
 */
@Role
public interface DocumentSearch<T>
{
    /** Input key. */
    String CLASS_KEY = "class";

    /** Input key. */
    String LIMIT_KEY = "limit";

    /** Input key. */
    String OFFSET_KEY = "offset";

    /** Input key. */
    String ORDER_KEY = "sort";

    /** Input key. */
    String ORDER_DIR_KEY = "dir";

    /** Input key. */
    String REQUEST_NUMBER_KEY = "reqNo";

    /** Input key. */
    String OUTPUT_SYNTAX_KEY = "outputSyntax";

    /** Input key. */
    String FILTER_WHERE_KEY = "filterWhere";

    /** Input key. */
    String FILTER_FROM_KEY = "filterFrom";

    /** Input key. */
    String QUERY_FILTERS_KEY = "queryFilters";

    /** Input key. */
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
