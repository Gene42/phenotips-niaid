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
    /**
     *
     * @param queryParameters the parameters used to create the query
     * @return a DocumentSearchResult containing the documents plus extra metadata
     * @throws QueryException on any issues during document querying
     * @throws SecurityException if user is not authorized to search for data
     */
    DocumentSearchResult search(JSONObject queryParameters) throws QueryException;
}
