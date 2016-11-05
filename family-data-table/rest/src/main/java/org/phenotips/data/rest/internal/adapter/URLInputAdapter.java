package org.phenotips.data.rest.internal.adapter;

import org.phenotips.data.api.DocumentSearch;
import org.phenotips.data.rest.LiveTableInputAdapter;
import org.phenotips.data.rest.internal.RequestUtils;

import org.xwiki.component.annotation.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MultivaluedMap;

import org.json.JSONObject;

/**
 * This class converts URL parameters given py PhenoTips frontend during table searches, into the JSONObject
 * representation which the DocumentSearch interface knows how to handle.
 *
 * @version $Id$
 */
@Component(roles = { LiveTableInputAdapter.class })
@Named("url")
@Singleton
public class URLInputAdapter implements LiveTableInputAdapter
{
    //property_name/<value|param_name>@<doc_class>(#num)-><doc_class>(#num)
    //f:external_id/doc_class : 1/PhenoTips.PatientClass
    //f:external_id/1@join_mode : or
    //f:external_id/2@join_mode : and
    //f:external_id/1@ : dsa213
    //f:external_id/class


    private static final String CLASSNAME_KEY = "classname";

    private static final Set<String> NON_FILTERS = new HashSet<>();

    static {
        NON_FILTERS.add(CLASSNAME_KEY);
        NON_FILTERS.add(DocumentSearch.LIMIT_KEY);
        NON_FILTERS.add(DocumentSearch.OFFSET_KEY);
        NON_FILTERS.add(DocumentSearch.SORT_KEY);
        NON_FILTERS.add(DocumentSearch.REQUEST_NUMBER_KEY);
        NON_FILTERS.add(DocumentSearch.OUTPUT_SYNTAX_KEY);
        NON_FILTERS.add(DocumentSearch.FILTER_WHERE_KEY);
        NON_FILTERS.add(DocumentSearch.FILTER_FROM_KEY);
        NON_FILTERS.add(DocumentSearch.QUERY_FILTERS_KEY);
        NON_FILTERS.add(DocumentSearch.SORT_DIR_KEY);
        NON_FILTERS.add(DocumentSearch.COLUMN_LIST_KEY);
        NON_FILTERS.add(RequestUtils.TRANS_PREFIX_KEY);
    }

    @Override public JSONObject convert(MultivaluedMap<String, String> queryParameters)
    {
        String documentClassName = queryParameters.getFirst(CLASSNAME_KEY);

        DocumentQueryBuilder builder = new DocumentQueryBuilder(documentClassName);

        // Key is param name, value param value list
        for (Map.Entry<String, List<String>> entry : queryParameters.entrySet()) {
            if (NON_FILTERS.contains(entry.getKey())) {
                continue;
            } else {
                builder.add(ParameterKey.FILTER_KEY_PREFIX + entry.getKey(), entry.getValue());
            }

        }

        return builder.toJSON();
    }
}
