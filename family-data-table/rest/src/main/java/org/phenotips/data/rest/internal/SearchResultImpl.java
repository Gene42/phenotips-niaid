/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.rest.internal;

import org.phenotips.data.rest.SearchResult;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class SearchResultImpl implements SearchResult
{
    /**
     * Generate a JSONObject representation of the result.
     * @return a JSONObject
     */
    public JSONObject toJSON(List<XWikiDocument> documents)
    {
        JSONObject result = new JSONObject();

        result.put("reqNo", "");
        result.put("totalrows", "");
        result.put("returnedrows", documents.size());
        result.put("offset", "");

        JSONArray rows = new JSONArray();

        result.put("rows", rows);
        //reqNo: 1,
        //totalrows: 1,
        //returnedrows: 1,
        //offset: 1,

        return result;
    }
}
