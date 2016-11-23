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
import org.xwiki.component.manager.ComponentManager;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Interface dealing with the generation of live table columns.
 *
 * @version $Id$
 */
@Role
public interface LiveTableColumnHandler
{
    /**
     * Adds a new column to the given row, using the TableColumn definition provided.
     * @param row the row to add to
     * @param col the column definition
     * @param doc the document used to get the necessary info for populating the column
     * @param context the wiki context
     * @param componentManager the component manager to use for retrieving any needed components
     * @param queryParameters the request query parameters
     * @throws XWikiException if any error is encountered while attempting to build the column
     */
    void addColumn(JSONObject row, TableColumn col, XWikiDocument doc, XWikiContext context,
        ComponentManager componentManager, Map<String, List<String>> queryParameters) throws XWikiException;
}
