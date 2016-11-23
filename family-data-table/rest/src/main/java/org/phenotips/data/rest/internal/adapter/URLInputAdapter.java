/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.rest.internal.adapter;

import org.phenotips.data.api.DocumentSearch;
import org.phenotips.data.api.internal.PropertyName;
import org.phenotips.data.api.internal.SpaceAndClass;
import org.phenotips.data.api.internal.filter.AbstractFilter;
import org.phenotips.data.api.internal.filter.OrderFilter;
import org.phenotips.data.rest.LiveTableInputAdapter;
import org.phenotips.data.rest.internal.RequestUtils;
import org.phenotips.data.rest.internal.TableColumn;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
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
    private static final String VALUE_DELIMITER = ",";

    private static final String CLASS_NAME_KEY = "classname";

    private static final Set<String> NON_FILTERS = new HashSet<>();

    static {
        NON_FILTERS.add(CLASS_NAME_KEY);
        NON_FILTERS.add(DocumentSearch.LIMIT_KEY);
        NON_FILTERS.add(DocumentSearch.OFFSET_KEY);
        NON_FILTERS.add(DocumentSearch.ORDER_KEY);
        NON_FILTERS.add(DocumentSearch.REQUEST_NUMBER_KEY);
        NON_FILTERS.add(DocumentSearch.OUTPUT_SYNTAX_KEY);
        NON_FILTERS.add(DocumentSearch.FILTER_WHERE_KEY);
        NON_FILTERS.add(DocumentSearch.FILTER_FROM_KEY);
        NON_FILTERS.add(DocumentSearch.QUERY_FILTERS_KEY);
        NON_FILTERS.add(DocumentSearch.ORDER_DIR_KEY);
        NON_FILTERS.add(DocumentSearch.COLUMN_LIST_KEY);
        NON_FILTERS.add(RequestUtils.TRANS_PREFIX_KEY);
    }

    @Override
    public JSONObject convert(Map<String, List<String>> queryParameters)
    {
        String documentClassName = RequestUtils.getFirst(queryParameters, URLInputAdapter.CLASS_NAME_KEY);


        DocumentQueryBuilder builder = new DocumentQueryBuilder(documentClassName);

        // Key is param name, value param value list
        for (Map.Entry<String, List<String>> entry : queryParameters.entrySet()) {
            if (URLInputAdapter.NON_FILTERS.contains(entry.getKey())) {
                continue;
            } else {
                builder.addFilter(ParameterKey.FILTER_KEY_PREFIX + entry.getKey(), entry.getValue());
            }
        }

        this.addOrderFilter(builder, queryParameters);

        JSONObject queryObj = builder.build().toJSON();

        queryObj.put(DocumentSearch.LIMIT_KEY, RequestUtils.getFirst(queryParameters, DocumentSearch.LIMIT_KEY));
        queryObj.put(DocumentSearch.QUERY_FILTERS_KEY, RequestUtils.getFirst(queryParameters, DocumentSearch
            .QUERY_FILTERS_KEY));
        queryObj.put(DocumentSearch.FILTER_WHERE_KEY, RequestUtils.getFirst(queryParameters, DocumentSearch
            .FILTER_WHERE_KEY));
        queryObj.put(DocumentSearch.FILTER_FROM_KEY, RequestUtils.getFirst(queryParameters, DocumentSearch
            .FILTER_FROM_KEY));

        queryObj.put(DocumentSearch.OFFSET_KEY,
            Integer.valueOf(RequestUtils.getFirst(queryParameters, DocumentSearch.OFFSET_KEY, "1")) - 1);
        queryObj.put(DocumentSearch.COLUMN_LIST_KEY, this.getColumnList(documentClassName, queryParameters));

        return queryObj;
    }

    private void addOrderFilter(DocumentQueryBuilder builder, Map<String, List<String>> queryParameters)
    {
        String sortKey = ParameterKey.FILTER_KEY_PREFIX + RequestUtils.getFirst(queryParameters, DocumentSearch
            .ORDER_KEY);
        String typeKey = sortKey + ParameterKey.PROPERTY_DELIMITER + AbstractFilter.TYPE_KEY;

        builder.addToOrderFilter(sortKey,
            Collections.singletonList(RequestUtils.getFirst(queryParameters, DocumentSearch.ORDER_DIR_KEY)));
        builder.addToOrderFilter(typeKey, Collections.singletonList(OrderFilter.TYPE));
    }

    private JSONArray getColumnList(String className, Map<String, List<String>> queryParameters)
    {
        String [] tokens = StringUtils.split(RequestUtils.getFirst(queryParameters, DocumentSearch.COLUMN_LIST_KEY),
            URLInputAdapter.VALUE_DELIMITER);

        JSONArray array = new JSONArray();

        if (tokens == null) {
            return array;
        }

        for (String token : tokens) {

            JSONObject obj = new JSONObject();
            if (StringUtils.startsWith(token, PropertyName.DOC_PROPERTY_PREFIX)) {
                obj.put(TableColumn.TYPE_KEY, EntityType.DOCUMENT.toString());
            } else {
                obj.put(TableColumn.TYPE_KEY, EntityType.OBJECT.toString());

                String key = token + ParameterKey.PROPERTY_DELIMITER + SpaceAndClass.CLASS_KEY;

                if (queryParameters.containsKey(key)) {
                    obj.put(TableColumn.CLASS_KEY, RequestUtils.getFirst(queryParameters, key));
                } else {
                    obj.put(TableColumn.CLASS_KEY, className);
                }
            }

            obj.put(TableColumn.COLUMN_NAME_KEY, token);

            array.put(obj);
        }

        return array;
    }
}
