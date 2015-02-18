package org.phenotips.metabolites;

import org.xwiki.component.annotation.Role;

import java.util.Map;

import net.sf.json.JSONObject;

/**
 * Necessary only to make the processing class into a component.
 */
@Role
public interface ProcessorRole
{
    int process(Map<String, String> fieldMap);

    JSONObject getJsonReports(String patientId, Integer offset, Integer limit, String sortColumn, String sortDir,
        Map<String, String> filters);
}
