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
import org.phenotips.data.api.internal.QueryExpression;
import org.phenotips.data.api.internal.SearchUtils;
import org.phenotips.data.api.internal.SpaceAndClass;
import org.phenotips.data.api.internal.filter.AbstractFilter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.Builder;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * This is a builder class for a document query input object.
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

    private ParameterKey.NameAndTag classAndTag;

    private DocumentQueryBuilder parent;

    private DocumentQueryBuilder root;

    private boolean built;

    /**
     * Constructor.
     * @param docClassName the class name of the document which this query represents
     */
    public DocumentQueryBuilder(String docClassName)
    {
        this(null, docClassName);
    }

    /**
     * Constructor.
     * @param parent the parent query
     * @param docClassName the class name of the document which this query represents
     */
    public DocumentQueryBuilder(DocumentQueryBuilder parent, String docClassName)
    {
        this(parent, docClassName, ParameterKey.QUERY_TAG_DEFAULT, ParameterKey.DEFAULT_OPERATION, false);
    }

    /**
     * Constructor.
     * @param parent the parent query
     * @param docClassName the class name of the document which this query represents
     * @param tagName the tag name of this query
     * @param operation the operation used to join expressions in this query ('and', 'or')
     * @param negate flag to determine whether or not to negate this entire query
     */
    public DocumentQueryBuilder(DocumentQueryBuilder parent, String docClassName, String tagName, String operation,
        boolean negate)
    {
        this.classAndTag = new ParameterKey.NameAndTag(docClassName, tagName, operation, negate);
        this.parent = parent;
        if (parent == null) {
            this.root = this;
        } else {
            this.root = parent.root;
        }
    }

    /**
     * Adds the given filter key and/or its values to the query.
     * @param key the key string containing the property name and its parameter definition
     * @param values the values held buy this property
     * @return this
     */
    public DocumentQueryBuilder addFilter(String key, List<String> values)
    {
        this.addFilter(key, values, this.classAndTag.getQueryName());
        return this;
    }

    /**
     * Adds the given filter key and/or its values to the order filter.
     * @param key the key string containing the property name and its parameter definition
     * @param values the values held buy this property
     * @return this
     */
    public DocumentQueryBuilder addToOrderFilter(String key, List<String> values)
    {
        if (this.orderFilter == null) {
            this.orderFilter = this.createFilter(new ParameterKey(key, values, this.classAndTag.getQueryName()));
        }
        this.addPropertyOrValueToFilter(
            this.orderFilter, new ParameterKey(key, values, this.classAndTag.getQueryName()));
        return this;
    }

    @Override
    public DocumentQueryBuilder build()
    {
        if (this.built) {
            return this;
        }

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

        this.built = true;

        return this;
    }

    /**
     * Generates the JSONObject representation of this query builder. If the build() method has not yet been called,
     * it will be called before continuing with the JSONObject generation.
     *
     * @return a JSONObject representation of the final product of this builder
     */
    public JSONObject toJSON()
    {
        if (!this.built) {
            this.build();
        }

        JSONObject myself = new JSONObject();

        myself.put(DocumentQuery.JOIN_MODE_KEY, this.classAndTag.getOperator());

        if (this.isQuery()) {
            myself.put(SpaceAndClass.CLASS_KEY, this.classAndTag.getQueryName());
        }

        if (this.classAndTag.isNegate()) {
            myself.put(QueryExpression.NEGATE_KEY, this.classAndTag.isNegate());
        }

        for (JSONObject filter : this.filters.values()) {
            myself.append(DocumentQuery.FILTERS_KEY, filter);
        }

        for (Map<String, DocumentQueryBuilder> tagMap : this.queries.values()) {
            for (DocumentQueryBuilder queryBuilder : tagMap.values()) {
                myself.append(DocumentQuery.QUERIES_KEY, queryBuilder.toJSON());
            }
        }

        if (this.orderFilter != null) {
            myself.put(DocumentSearch.ORDER_KEY, this.orderFilter);
        }

        return myself;
    }

    /**
     * If this query object represents a document query or is simply an expression within another query.
     * @return true if tis query is supposed to query for a document, false otherwise
     */
    public boolean isQuery()
    {
        return StringUtils.isNotBlank(this.classAndTag.getQueryName());
    }

    private static DocumentQueryBuilder getDocumentQueryBuilder(DocumentQueryBuilder query,
        Queue<ParameterKey.NameAndTag> queries)
    {
        ParameterKey.NameAndTag nextClassAndTag = queries.poll();

        if (query == null) {
            return null;
        } else if (CollectionUtils.isEmpty(queries)) {
            if (ParameterKey.NameAndTag.areEqual(query.classAndTag, nextClassAndTag)) {
                return query;
            } else {
                return null;
            }
        } else {
            return getDocumentQueryBuilder(getDocumentQueryBuilderFromMap(query.queries, queries.peek()), queries);
        }
    }

    private static DocumentQueryBuilder getDocumentQueryBuilderFromMap(Map<String,
        Map<String, DocumentQueryBuilder>> map, ParameterKey.NameAndTag classAndTag)
    {
        if (classAndTag == null) {
            return null;
        }

        Map<String, DocumentQueryBuilder> tagMap = map.get(classAndTag.getQueryName());

        if (tagMap == null) {
            return null;
        }

        return tagMap.get(classAndTag.getQueryTag());
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
        List<String> keysToRemove = new LinkedList<>();

        for (Map.Entry<String, JSONObject> entry : this.filters.entrySet()) {
            JSONObject filter = entry.getValue();

            String dependsOn = filter.optString(DocumentQueryBuilder.DEPENDS_ON_KEY);

            if (StringUtils.isBlank(dependsOn)) {
                continue;
            }

            ParameterKey dependsOnProp = new ParameterKey(
                ParameterKey.FILTER_KEY_PREFIX + dependsOn, null, this.root.classAndTag.getQueryName());

            DocumentQueryBuilder query = getDocumentQueryBuilder(this.root, dependsOnProp.getParentsAsQueue());

            if (!(doesQueryContainFilterWithValues(query, dependsOnProp.getPropertyName()))) {
                keysToRemove.add(entry.getKey());
            }
        }

        for (String keyToRemove : keysToRemove) {
            this.filters.remove(keyToRemove);
        }
    }

    private static boolean doesQueryContainFilterWithValues(DocumentQueryBuilder query, String propertyName)
    {
        if (query == null) {
            throw new IllegalArgumentException(String.format("Invalid dependsOn query for [%1$s]", propertyName));
        }
        JSONObject filter = query.filters.get(propertyName);
        return filter != null && doesFilterHaveValues(filter);
    }

    private static boolean doesFilterHaveValues(JSONObject filter)
    {
        if (filter == null) {
            return false;
        }

        for (String valueProperty : SearchUtils.getValueParameterNames()) {

            Object obj = filter.opt(valueProperty);

            if (obj == null) {
                continue;
            }

            boolean hasValue;

            if (obj instanceof JSONArray) {
                hasValue = ((JSONArray) obj).length() > 0;
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
        ParameterKey.NameAndTag query = paramKey.getParents().get(parentIndex);

        if (parentIndex < paramKey.getParents().size() - 1) {
            int nextIndex = parentIndex + 1;
            ParameterKey.NameAndTag childQueryName = paramKey.getParents().get(nextIndex);
            Map<String, DocumentQueryBuilder> childTagMap = this.queries.get(childQueryName.getQueryName());

            if (childTagMap == null) {
                childTagMap = new HashMap<>();
                this.queries.put(childQueryName.getQueryName(), childTagMap);
            }

            DocumentQueryBuilder childQuery = childTagMap.get(childQueryName.getQueryTag());

            if (childQuery == null) {
                childQuery = new DocumentQueryBuilder(
                        this, childQueryName.getQueryName(), childQueryName.getQueryTag(), childQueryName
                    .getOperator(), childQueryName.isNegate());

                childTagMap.put(childQueryName.getQueryTag(), childQuery);
            }

            childQuery.addFilter(paramKey, nextIndex);

        } else {

            if (ParameterKey.NameAndTag.areEqual(this.classAndTag, query)) {
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
        filter.put(AbstractFilter.DOC_CLASS_KEY, paramKey.getQueryClassAndTag().getQueryName());
        filter.put(PropertyName.PROPERTY_NAME_KEY, paramKey.getPropertyName());
        filter.put(SpaceAndClass.CLASS_KEY, paramKey.getQueryClassAndTag().getQueryName());
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
            String [] refTokens = StringUtils.splitPreserveAllTokens(
                refValue, DocumentQueryBuilder.REFERENCE_VALUE_DELIMITER, 3);

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
