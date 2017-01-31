/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.rest;

import org.phenotips.data.rest.internal.TableColumn;

import org.xwiki.component.annotation.Role;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Interface dealing with the generation of live table rows.
 *
 * @version $Id$
 */
@Role
public interface LiveTableRowHandler
{
    /**
     * Returns a new row object, using the list of TableColumn definitions provided.
     * @param doc the document used to get the necessary info for populating the column
     * @param context the wiki context
     * @param cols the column definitions
     * @param queryParameters the request query parameters
     * @return a JSONObject representing a row
     * @throws XWikiException if any error is encountered while attempting to build the column
     */
    JSONObject getRow(XWikiDocument doc, XWikiContext context, List<TableColumn> cols,
        Map<String, List<String>> queryParameters) throws XWikiException;
}
