package org.phenotips.metabolites;

import org.xwiki.component.annotation.Role;

import java.util.Map;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

import net.sf.json.JSONObject;

/**
 * Necessary only to make the processing class into a component.
 */
@Role
public interface ProcessorRole
{
    int process(Map<String, String> fieldMap, XWikiContext xwikiContext, XWiki wiki) throws XWikiException;

    JSONObject getJsonReports(String patientId, Integer offset, Integer limit, String sortColumn, String sortDir,
        Map<String, String> filters);
}
