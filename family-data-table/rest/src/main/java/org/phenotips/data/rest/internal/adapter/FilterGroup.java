package org.phenotips.data.rest.internal.adapter;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class FilterGroup extends DocumentQueryBuilder
{

    private Map<String, JSONObject> filters = new HashMap<>();

    private Map<String, FilterGroup> groups = new HashMap<>();

    private ParameterKey.NameAndTag nameAndTag;

    private DocumentQueryBuilder parentQuery;

    private FilterGroup parentGroup;

    private FilterGroup rootGroup;

    public FilterGroup(DocumentQueryBuilder parent, String docClassName, String tagName)
    {
        super(parent, docClassName, tagName);
    }
}
