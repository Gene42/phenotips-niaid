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
 * This class represents a queyr expression which is a block in a query surrounded by round brackets '()'. A
 * QueryExpression can hold filters, other expressions and document queries.
 *
 * @version $Id$
 */
public class QueryExpression implements QueryElement
{

    /** JSON Object key. */
    public static final String QUERIES_KEY = "queries";

    /** JSON Object key. */
    public static final String FILTERS_KEY = "filters";

    /** JSON Object key. */
    public static final String JOIN_MODE_KEY = "join_mode";

    /** JSON Object key. */
    public static final String REFERENCE_CLASS_KEY = "reference_class";

    /** JSON Object key. */
    public static final String NEGATE_KEY = "negate";

    private static final String JOIN_MODE_DEFAULT_VALUE = "and";

    private List<DocumentQuery> documentQueries = new LinkedList<>();
    private List<QueryElement> expressions = new LinkedList<>();

    private DocumentQuery parentQuery;

    private String joinMode;

    private boolean orMode;
    private SpaceAndClass spaceAndClass;
    private PropertyName propertyName;

    private String not;

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
        return this.parentQuery;
    }

    /**
     * Getter for spaceAndClass.
     *
     * @return spaceAndClass
     */
    public SpaceAndClass getSpaceAndClass()
    {
        return this.spaceAndClass;
    }

    /**
     * Getter for propertyName.
     *
     * @return propertyName
     */
    public PropertyName getPropertyName()
    {
        return this.propertyName;
    }

    @Override
    public boolean isValid()
    {
        return CollectionUtils.isNotEmpty(this.documentQueries) || CollectionUtils.isNotEmpty(this.expressions);
    }

    @Override
    public boolean validatesQuery()
    {
        if (CollectionUtils.isNotEmpty(this.documentQueries)) {
            return true;
        }

        for (QueryElement queryElement : this.expressions) {
            if (queryElement.validatesQuery()) {
                return true;
            }
        }

        return false;
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

        if (!StringUtils.equals(this.joinMode, "and") && !StringUtils.equals(this.joinMode, "or")) {
            this.joinMode = QueryExpression.JOIN_MODE_DEFAULT_VALUE;
        }

        if (SearchUtils.BOOLEAN_TRUE_SET.contains(SearchUtils.getValue(input, QueryExpression.NEGATE_KEY))) {
            this.not = " not ";
        } else {
            this.not = "";
        }

        this.orMode = StringUtils.equals(this.joinMode, "or");

        if (input.has(QueryExpression.FILTERS_KEY)) {
            JSONArray filterJSONArray = input.getJSONArray(QueryExpression.FILTERS_KEY);

            for (int i = 0, len = filterJSONArray.length(); i < len; i++) {
                this.processFilterJSON(filterJSONArray.optJSONObject(i));
            }
        }

        if (input.has(QueryExpression.QUERIES_KEY)) {
            JSONArray queriesJSONArray = input.getJSONArray(QueryExpression.QUERIES_KEY);

            for (int i = 0, len = queriesJSONArray.length(); i < len; i++) {
                this.handleQuery(queriesJSONArray.optJSONObject(i));
            }
        }

        return this;
    }

    /**
     * Getter for orMode.
     *
     * @return orMode
     */
    public boolean isOrMode()
    {
        return this.orMode;
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
            where.appendOperator().append(this.not).append(" exists(");
            documentQuery.hql(where, bindingValues).append(") ");
        }

        return where.append(") ").load();
    }

    @Override
    public QueryElement createBindings()
    {
        if (!this.isValid()) {
            return this;
        }

        if (this.orMode) {
            this.spaceAndClass = new SpaceAndClass("group." + this.parentQuery.getNextExpressionIndex());

            this.propertyName = new PropertyName("group_prop", getFirstProp().getObjectType());

            this.parentQuery.addPropertyBinding(this.spaceAndClass, this.propertyName);
        }

        for (QueryElement expression : this.expressions) {
            if (!this.orMode || !(expression instanceof AbstractFilter)) {
                expression.createBindings();
            }
        }
        return this;
    }

    private PropertyName getFirstProp()
    {
        for (QueryElement expression : this.expressions) {
            if (expression instanceof AbstractFilter) {
                return ((AbstractFilter) expression).getPropertyName();
            }
        }

        throw new IllegalArgumentException("This expression is not valid");
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

        if (objectFilter != null && objectFilter.init(filterJson, this.getParentQuery(), this).isValid()) {
            this.expressions.add(objectFilter);
        }
    }
}
