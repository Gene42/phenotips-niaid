package org.phenotips.metabolites;

import org.xwiki.component.annotation.Role;

import java.util.Map;

import net.sf.json.JSONArray;

/**
 * Necessary only to make the processing class into a component.
 */
@Role
public interface ProcessorRole
{
    int process(Map<String, String> fieldMap);

    JSONArray getJsonReports(String patientId);
}
