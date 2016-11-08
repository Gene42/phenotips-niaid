/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.rest.internal.adapter;

import org.phenotips.data.api.DocumentSearch;
import org.phenotips.data.api.internal.DocumentQuery;
import org.phenotips.data.api.internal.PropertyName;
import org.phenotips.data.api.internal.SearchUtils;
import org.phenotips.data.api.internal.SpaceAndClass;
import org.phenotips.data.api.internal.filter.AbstractFilter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.Builder;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class DocumentQueryBuilder implements Builder<DocumentQueryBuilder>
{
    private static final String REFERENCE_VALUE_DELIMITER = "|";

    private static final String DEPENDS_ON_KEY = "dependsOn";

    // Key is the property name to filter on: external_id, status, date_of_birth etc.
    // The value is the filter itself stored in a JSONObject
    private Map<String, JSONObject> filters = new HashMap<>();

    private JSONObject orderFilter;

    // First key is the class name, second key is the query tag (to differentiate queries on the same level)
    private Map<String, Map<String, DocumentQueryBuilder>> queries = new HashMap<>();

    private String docClassName;

    private String tagName;

    public DocumentQueryBuilder(String docClassName)
    {
        this(docClassName, ParameterKey.QUERY_TAG_DEFAULT);
    }

    public DocumentQueryBuilder(String docClassName, String tagName)
    {
        this.docClassName = docClassName;
        this.tagName = tagName;
    }

    public DocumentQueryBuilder addFilter(String key, List<String> values)
    {
        this.addFilter(key, values, this.docClassName);
        return this;
    }

    public DocumentQueryBuilder addToOrderFilter(String key, List<String> values)
    {
        if (this.orderFilter == null) {
            this.orderFilter = this.createFilter(new ParameterKey(key, values, this.docClassName));
        }
        this.addPropertyOrValueToFilter(this.orderFilter, new ParameterKey(key, values, this.docClassName));
        return this;
    }

    @Override public DocumentQueryBuilder build()
    {
        for (Map<String, DocumentQueryBuilder> tagMap : this.queries.values()) {
            for (DocumentQueryBuilder queryBuilder : tagMap.values()) {
                queryBuilder.build();
            }
        }

        if (this.orderFilter != null) {
            String sortPropName = this.orderFilter.getString(PropertyName.PROPERTY_NAME_KEY);
            if (this.filters.containsKey(sortPropName)) {
                JSONObject filter = this.filters.get(sortPropName);
                this.orderFilter.put(SpaceAndClass.CLASS_KEY, SearchUtils.getValue(filter, SpaceAndClass.CLASS_KEY,
                    this.orderFilter.getString(SpaceAndClass.CLASS_KEY)));
            }
        }

        this.handleFilterDependencies();

        return this;
    }

    public JSONObject toJSON()
    {
        JSONObject myself = new JSONObject();
        myself.put(SpaceAndClass.CLASS_KEY, this.docClassName);

        for (JSONObject filter : this.filters.values()) {
            myself.append(DocumentQuery.FILTERS_KEY, filter);
        }

        for (Map<String, DocumentQueryBuilder> tagMap : queries.values()) {
            for (DocumentQueryBuilder queryBuilder : tagMap.values()) {
                myself.append(org.phenotips.data.api.internal.DocumentQuery.QUERIES_KEY, queryBuilder.toJSON());
            }
        }

        if (this.orderFilter != null) {
            myself.put(DocumentSearch.ORDER_KEY, this.orderFilter);
        }

        return myself;
    }


    private void addFilter(String key, List<String> values, String defaultDocClassName)
    {
        this.addFilter(new ParameterKey(key, values, defaultDocClassName));
    }

    private void addFilter(ParameterKey paramKey)
    {
        this.addFilter(paramKey, 0);
    }

    private void handleFilterDependencies()
    {
        // NOTE: Currently depends on can only reference filters of the same document
        List<String> keysToRemove = new LinkedList<>();

        // propertyName + PROPERTY_DELIMITER + documentClassName
        for (Map.Entry<String, JSONObject> entry : this.filters.entrySet()) {
            JSONObject filter = entry.getValue();

            String dependsOn = filter.optString(DEPENDS_ON_KEY);

            if (StringUtils.isBlank(dependsOn)) {
                continue;
            }

            if (!this.filters.containsKey(dependsOn)
                || !this.doesFilterHaveValues(this.filters.get(dependsOn))) {
                keysToRemove.add(entry.getKey());
            }
        }

        for (String keyToRemove : keysToRemove) {
            this.filters.remove(keyToRemove);
        }
    }

    private boolean doesFilterHaveValues(JSONObject filter)
    {
        if (filter == null) {
            return false;
        }

        for (String valueProperty : AbstractFilter.getValuePropertyNames()) {

            Object obj = filter.opt(valueProperty);

            if (obj == null) {
                continue;
            }

            boolean hasValue;

            if (obj instanceof JSONArray) {
                hasValue = ((JSONArray)obj).length() > 0;
            } else if (obj instanceof JSONObject) {
                hasValue = ((JSONObject) obj).length() > 0;
            } else {
                hasValue = obj instanceof String || StringUtils.isNotBlank(String.valueOf(obj));
            }

            if (hasValue) {
                return true;
            }
        }

        return false;
    }

    private void addFilter(ParameterKey paramKey, int parentIndex)
    {
        ParameterKey.QueryClassAndTag query = paramKey.getParents().get(parentIndex);

        if (parentIndex < paramKey.getParents().size() - 1) {
            int nextIndex = parentIndex + 1;
            ParameterKey.QueryClassAndTag childQueryName = paramKey.getParents().get(nextIndex);
            Map<String, DocumentQueryBuilder> childTagMap = this.queries.get(childQueryName.getDocClassName());

            if (childTagMap == null) {
                childTagMap = new HashMap<>();
                this.queries.put(childQueryName.getDocClassName(), childTagMap);
            }

            DocumentQueryBuilder childQuery = childTagMap.get(childQueryName.getQueryTag());

            if (childQuery == null) {
                childQuery = new DocumentQueryBuilder(childQueryName.getDocClassName(), childQueryName.getQueryTag());
                childTagMap.put(childQueryName.getQueryTag(), childQuery);
            }

            childQuery.addFilter(paramKey, nextIndex);

        } else {
            if (StringUtils.equals(this.docClassName, query.getDocClassName())
                && StringUtils.equals(this.tagName, query.getQueryTag()))
            {
                this.addFilterToMyself(paramKey);
            } else {
                throw new IllegalArgumentException(String.format("Invalid query param [%1$s]", paramKey));
            }
        }
    }

    private void addFilterToMyself(ParameterKey paramKey)
    {
        JSONObject filter = this.filters.get(paramKey.getPropertyName());

        if (filter == null) {
            filter = this.createFilter(paramKey);
            this.filters.put(paramKey.getPropertyName(), filter);
        }

        this.addPropertyOrValueToFilter(filter, paramKey);
    }

    private void addPropertyOrValueToFilter(JSONObject filter, ParameterKey paramKey)
    {
        if (paramKey.isFilterValue()) {
            for (String val : paramKey.getValues()) {
                filter.append(AbstractFilter.VALUES_KEY, val);
            }
        } else {
            this.addPropertyValueToFilter(paramKey.getParameterName(), paramKey.getValues(), filter);
        }
    }

    private JSONObject createFilter(ParameterKey paramKey)
    {
        JSONObject filter = new JSONObject();
        filter.put(AbstractFilter.DOC_CLASS_KEY, paramKey.getQueryClassAndTag().getDocClassName());
        filter.put(PropertyName.PROPERTY_NAME_KEY, paramKey.getPropertyName());
        filter.put(SpaceAndClass.CLASS_KEY, paramKey.getQueryClassAndTag().getDocClassName());
        return filter;
    }

    private void addPropertyValueToFilter(String propertyParamName, List<String> values, JSONObject filter)
    {
        if (StringUtils.equals(propertyParamName, AbstractFilter.REF_VALUES_KEY)) {
            // Reference Value
            this.addReferenceValues(propertyParamName, values, filter);
        } else {
            // Regular value
            if (CollectionUtils.isEmpty(values)) {
                filter.put(propertyParamName, (Object) null);
            } else if (values.size() == 1) {
                filter.put(propertyParamName, values.get(0));
            } else {
                filter.put(propertyParamName, values);
            }
        }
    }

    private void addReferenceValues(String propertyParamName, List<String> values, JSONObject filter)
    {
        for (String refValue : values) {
            // level|class|property_name
            String [] refTokens = StringUtils.splitPreserveAllTokens(refValue, REFERENCE_VALUE_DELIMITER, 3);

            if (refTokens.length != 3) {
                throw new IllegalArgumentException(
                    String.format("Ref value is not valid for param [%1$s]", propertyParamName));
            }

            JSONObject refValueFilter = new JSONObject();
            refValueFilter.put(AbstractFilter.PARENT_LEVEL_KEY, refTokens[0]);
            refValueFilter.put(SpaceAndClass.CLASS_KEY, refTokens[1]);
            refValueFilter.put(PropertyName.PROPERTY_NAME_KEY, refTokens[2]);

            filter.append(AbstractFilter.REF_VALUES_KEY, refValueFilter);
        }
    }
}
