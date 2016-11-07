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

import javax.ws.rs.core.MultivaluedMap;

import org.json.JSONObject;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
@Role
public interface LiveTableColumnHandler
{
    void addColumn(JSONObject row, TableColumn col, XWikiDocument doc, XWikiContext context,
        ComponentManager componentManager, Map<String, List<String>> queryParameters) throws XWikiException;
}
