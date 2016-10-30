package org.phenotips.data.rest;

import org.phenotips.data.rest.internal.TableColumn;

import org.xwiki.component.annotation.Role;
import org.xwiki.component.manager.ComponentManager;

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
public interface CustomColumnHandler
{
    void addColumn(JSONObject row, TableColumn col, XWikiDocument doc, XWikiContext context,
        ComponentManager componentManager, MultivaluedMap<String, String> queryParameters) throws XWikiException;
}
