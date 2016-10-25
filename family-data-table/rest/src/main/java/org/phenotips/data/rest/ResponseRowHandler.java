package org.phenotips.data.rest;

import org.phenotips.data.rest.internal.TableColumn;

import org.xwiki.component.annotation.Role;

import java.util.List;

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
public interface ResponseRowHandler
{
    JSONObject getRow(XWikiDocument doc, XWikiContext context, List<TableColumn> cols) throws XWikiException;
}
