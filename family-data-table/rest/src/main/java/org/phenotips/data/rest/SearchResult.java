package org.phenotips.data.rest;

import java.util.List;

import org.json.JSONObject;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public interface SearchResult
{
    JSONObject toJSON(List<XWikiDocument> documents);
}
