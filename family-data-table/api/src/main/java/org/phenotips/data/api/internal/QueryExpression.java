/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal;

import org.phenotips.data.api.DocumentSearch;
import org.phenotips.data.api.internal.filter.AbstractFilter;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class QueryExpression implements QueryElement
{

    /** JSON Object key */
    public static final String QUERIES_KEY = "queries";

    /** JSON Object key */
    public static final String FILTERS_KEY = "filters";

    /** JSON Object key */
    public static final String JOIN_MODE_KEY = "join_mode";

    /** JSON Object key */
    public static final String REFERENCE_CLASS_KEY = "reference_class";

    private static final String JOIN_MODE_DEFAULT_VALUE = "and";

    private List<DocumentQuery> documentQueries = new LinkedList<>();
    private List<QueryElement> expressions = new LinkedList<>();

    private DocumentQuery parentQuery;

    private int validFilters;
    private String joinMode;

    /**
     * Constructor.
     * @param parentQuery the parent query of this expression
     */
    public QueryExpression(DocumentQuery parentQuery)
    {
        this.parentQuery = parentQuery;
    }

    /**
     * Getter for parent.
     *
     * @return parent
     */
    public DocumentQuery getParentQuery()
    {
        return parentQuery;
    }

    public boolean isValid()
    {
        return this.validFilters > 0
            || CollectionUtils.isNotEmpty(this.documentQueries)
            || CollectionUtils.isNotEmpty(this.expressions);
    }

    public static StringBuilder appendQueryOperator(StringBuilder buffer, String operator, int valuesIndex)
    {
        if (valuesIndex > 0) {
            buffer.append(" ").append(operator).append(" ");
        }

        return buffer;
    }

    /**
     * Initializes this DocumentQuery based on the input. The hql method should be called after this method is called.
     * @param input input object containing instructions to initialized the query
     * @return this object
     */
    public QueryExpression init(JSONObject input)
    {

        this.joinMode = SearchUtils.getValue(input, QueryExpression.JOIN_MODE_KEY,
            QueryExpression.JOIN_MODE_DEFAULT_VALUE);

        if (input.has(FILTERS_KEY)) {
            JSONArray filterJSONArray = input.getJSONArray(FILTERS_KEY);

            for (int i = 0, len = filterJSONArray.length(); i < len; i++) {
                this.processFilterJSON(filterJSONArray.optJSONObject(i));
            }
        }

        if (input.has(QUERIES_KEY)) {
            JSONArray queriesJSONArray = input.getJSONArray(QUERIES_KEY);

            for (int i = 0, len = queriesJSONArray.length(); i < len; i++) {
                this.handleQuery(queriesJSONArray.optJSONObject(i));
            }
        }

        return this;
    }


    @Override
    public QueryBuffer bindProperty(QueryBuffer where, List<Object> bindingValues)
    {
        for (QueryElement expression : this.expressions) {
            expression.bindProperty(where, bindingValues);
        }
        return where;
    }

    @Override
    public QueryBuffer addValueConditions(QueryBuffer where, List<Object> bindingValues)
    {
        if (CollectionUtils.isEmpty(this.expressions) && (CollectionUtils.isEmpty(this.documentQueries))) {
            return where;
        }

        where.appendOperator().saveAndReset(this.joinMode);

        where.append(" (");

        for (QueryElement expression : this.expressions) {
            expression.addValueConditions(where, bindingValues);
        }

        for (DocumentQuery documentQuery : this.documentQueries) {
            where.append(" exists(");
            documentQuery.hql(where, bindingValues).append(") ");
        }

        return where.append(") ").load();
    }

    public static void addValueConditions(QueryBuffer where, List<Object> bindingValues, List<QueryElement> expressions)
    {
        for (QueryElement expression : expressions) {
            expression.addValueConditions(where, bindingValues);
        }
    }

    private void handleQuery(JSONObject queryJson)
    {
        if (queryJson == null) {
            return;
        }

        if (StringUtils.isBlank(SearchUtils.getValue(queryJson, DocumentSearch.CLASS_KEY))) {
            QueryExpression expr = new QueryExpression(this.parentQuery).init(queryJson);
            if (expr.isValid()) {
                this.expressions.add(expr);
            }
        } else {
            DocumentQuery query = new DocumentQuery(this.getParentQuery()).init(queryJson);

            if (query.isValid()) {
                this.documentQueries.add(query);
            }
        }
    }

    private void processFilterJSON(JSONObject filterJson)
    {
        if (filterJson == null) {
            return;
        }

        AbstractFilter objectFilter = this.getParentQuery().getFilterFactory().getFilter(filterJson);
        if (objectFilter != null && objectFilter.init(filterJson, this.getParentQuery()).isValid()) {
            this.expressions.add(objectFilter.createBindings());

            if (objectFilter.validatesQuery()) {
                this.validFilters++;
            }
        }
    }
}
