package org.phenotips.data.rest.internal.adapter;

import org.phenotips.data.api.internal.DocumentQuery;
import org.phenotips.data.api.internal.PropertyName;
import org.phenotips.data.api.internal.SpaceAndClass;
import org.phenotips.data.api.internal.filter.AbstractFilter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class DocumentQueryBuilder
{

    private static final String REFERENCE_VALUE_DELIMITER = "|";

    // Key is the property name to filter on: external_id, status, date_of_birth etc.
    private Map<String, JSONObject> filters = new HashMap<>();

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

    public void add(String key, List<String> values)
    {
        this.add(key, values, this.docClassName);
    }

    private void add(String key, List<String> values, String defaultDocClassName)
    {
        this.add(new ParameterKey(key, values, defaultDocClassName));
    }

    private void add(ParameterKey paramKey)
    {
        this.add(paramKey, 0);
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

        return myself;
    }

    private void add(ParameterKey paramKey, int parentIndex)
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

            childQuery.add(paramKey, nextIndex);

        } else {
            if (StringUtils.equals(this.docClassName, query.getDocClassName())
                && StringUtils.equals(this.tagName, query.getQueryTag()))
            {
                this.addToMyself(paramKey);
            } else {
                throw new IllegalArgumentException(String.format("Invalid query param [%1$s]", paramKey));
            }
        }
    }

    private void addToMyself(ParameterKey paramKey)
    {
        JSONObject filter = this.filters.get(paramKey.getPropertyName());

        if (filter == null) {
            filter = new JSONObject();
            this.filters.put(paramKey.getPropertyName(), filter);
            filter.put(AbstractFilter.DOC_CLASS_KEY, paramKey.getQueryClassAndTag().getDocClassName());
            filter.put(PropertyName.PROPERTY_NAME_KEY, paramKey.getPropertyName());
            filter.put(SpaceAndClass.CLASS_KEY, paramKey.getQueryClassAndTag().getDocClassName());
        }

        if (paramKey.isFilterValue()) {
            for (String val : paramKey.getValues()) {
                filter.append(AbstractFilter.VALUES_KEY, val);
            }
        } else {
            //filter.put(paramKey.getPropertyName(), )
            this.addPropertyValueToFilter(paramKey.getParameterName(), paramKey.getValues(), filter);
        }
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
            // level|class|tag|property_name
            String [] refTokens = StringUtils.splitPreserveAllTokens(refValue, REFERENCE_VALUE_DELIMITER, 4);

            if (refTokens.length != 4) {
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
