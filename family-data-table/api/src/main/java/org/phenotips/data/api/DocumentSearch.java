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
 * @version $Id$
 */
@Role
public interface DocumentSearch
{
    //String CLASSNAME_KEY = "classname";

    /** Class property key */
    String CLASS_KEY = "class";

    String LIMIT_KEY = "limit";
    String OFFSET_KEY = "offset";
    String SORT_KEY = "sort";
    String REQUEST_NUMBER_KEY = "reqNo";
    String OUTPUT_SYNTAX_KEY = "outputSyntax";
    String FILTER_WHERE_KEY = "filterwhere";
    String FILTER_FROM_KEY = "filterfrom";
    String QUERY_FILTERS_KEY = "queryFilters";
    String SORT_DIR_KEY = "dir";
    String COLUMN_LIST_KEY = "collist";
    String TRANS_PREFIX_KEY = "transprefix";

    /**
     *
     * @param queryParameters the parameters used to create the query
     * @return a DocumentSearchResult containing the documents plus extra metadata
     * @throws QueryException on any issues during document querying
     * @throws SecurityException if user is not authorized to search for data
     */
    DocumentSearchResult search(JSONObject queryParameters) throws QueryException;
}
