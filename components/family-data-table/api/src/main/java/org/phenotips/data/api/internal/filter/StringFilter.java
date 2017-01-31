/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal.filter;

import org.phenotips.data.api.internal.DocumentQuery;
import org.phenotips.data.api.internal.PropertyName;
import org.phenotips.data.api.internal.QueryBuffer;
import org.phenotips.data.api.internal.QueryExpression;
import org.phenotips.data.api.internal.SearchUtils;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Filter handling simple string properties.
 *
 * @version $Id$
 */
public class StringFilter extends AbstractFilter<String>
{
    /** Filter param key. */
    public static final String MATCH_KEY = "match";

    /** Match value. */
    public static final String MATCH_EXACT = "exact";

    /** Match value. */
    public static final String MATCH_SUBSTRING = null;

    /** Match value. */
    public static final String MATCH_CASE_INSENSITIVE = "ci";

    /** Match value. */
    public static final String NULL_VALUE_KEY = "null_value";

    private static final String CLOSING_BRACKET = ") ";

    private static final String STARTING_BRACKET = " (";

    private static final String DOC_CREATOR_PROPERTY = "creator";
    private static final String DOC_AUTHOR_PROPERTY = "author";

    private String match;
    private boolean nullValue;

    /**
     * Constructor.
     * @param property PropertyInterface
     * @param baseClass BaseClass
     */
    public StringFilter(PropertyInterface property, BaseClass baseClass)
    {
        super(property, baseClass, "StringProperty");
    }

    @Override
    public AbstractFilter init(JSONObject input, DocumentQuery parent, QueryExpression expressionParent)
    {
        super.init(input, parent, expressionParent);

        this.match = input.optString(StringFilter.MATCH_KEY);

        this.nullValue = SearchUtils.BOOLEAN_TRUE_SET.contains(
            SearchUtils.getValue(input, StringFilter.NULL_VALUE_KEY, "false"));

        this.setValues(SearchUtils.getValues(input, VALUES_KEY));

        if (this.nullValue) {
            this.addNullValue();
        }

        return this;
    }

    @Override
    public QueryBuffer addValueConditions(QueryBuffer where, List<Object> bindingValues)
    {
        if (!this.isValid()) {
            return where;
        }

        this.startElement(where, bindingValues);

        String objPropName = this.getPropertyValueNameForQuery();

        if (CollectionUtils.isNotEmpty(this.getValues())) {
            where.saveOperator().setOperator("and");

            if (super.isDocumentProperty()) {
                this.addDocValueConditions(where, bindingValues, objPropName);

            } else if (this.getValues().size() == 1) {
                this.handleMatch(where, bindingValues, objPropName, super.getValues().get(0), this.match);

            } else if (this.nullValue) {
                this.addMultipleValueConditionWithNull(where, bindingValues, objPropName);
            } else {
                this.addMultipleValueCondition(where, bindingValues, objPropName);
            }

            where.loadOperator();
        }

        if (CollectionUtils.isNotEmpty(this.getRefValues())) {
            where.appendOperator();
            this.addRefValueConditions(where, objPropName);
        }

        return this.endElement(where);
    }

    private void addMultipleValueConditionWithNull(QueryBuffer where, List<Object> bindingValues, String objPropName)
    {
        where.saveAndReset("or").append(STARTING_BRACKET);

        for (int i = 0, len = this.getValues().size(); i < len; i++) {
            where.appendOperator();
            this.handleMatch(where, bindingValues, objPropName, this.getValues().get(i), this.match);
        }

        where.append(CLOSING_BRACKET).load();
    }

    private void addMultipleValueCondition(QueryBuffer where, List<Object> bindingValues, String objPropName)
    {
        String str = StringUtils.repeat("?", ", ", this.getValues().size());
        where.append(objPropName).append(SearchUtils.getComparisonOperator("in", this.isNegate()));
        where.append(STARTING_BRACKET).append(str).append(CLOSING_BRACKET);
        bindingValues.addAll(this.getValues());
    }

    private void addDocValueConditions(QueryBuffer where, List<Object> bindingValues, String objPropName)
    {
        boolean docAuthorOrCreator = isDocAuthorOrCreator(this.getPropertyName());

        where.saveAndReset("or").append(STARTING_BRACKET);

        for (int i = 0, len = this.getValues().size(); i < len; i++) {

            where.appendOperator();

            String value = this.getValues().get(i);
            this.handleDocumentProperties(where, bindingValues, objPropName, value, docAuthorOrCreator);
        }

        where.append(CLOSING_BRACKET).load();
    }

    private void addRefValueConditions(QueryBuffer where, String objPropName)
    {
        where.saveAndReset("or").append(STARTING_BRACKET);

        for (int i = 0, len = this.getRefValues().size(); i < len; i++) {
            AbstractFilter ref = this.getRefValues().get(i);

            where.appendOperator();

            String refPropertyName = ref.getPropertyValueNameForQuery();

            this.handleRefMatch(where, objPropName, refPropertyName, this.match);
        }

        where.append(CLOSING_BRACKET).load();
    }

    @Override
    public String getPropertyValueNameForQuery()
    {
        if (super.isDocumentProperty()) {
            return "str(" + this.getPropertyNameForQuery() + ")";
        } else {
            return this.getPropertyNameForQuery() + ".value";
        }
    }

    /**
     * Getter for match.
     *
     * @return match
     */
    public String getMatch()
    {
        return this.match;
    }

    /**
     * Setter for match.
     *
     * @param match match to set
     * @return this object
     */
    public StringFilter setMatch(String match)
    {
        this.match = match;
        return this;
    }

    private void handleDocumentProperties(QueryBuffer where, List<Object> bindingValues, String propName, String
        value, boolean docAuthorOrCreator)
    {
        String docPropMatch = StringFilter.MATCH_SUBSTRING;
        String docPropValue = value;

        if (docAuthorOrCreator && StringUtils.startsWith(value, "XWiki.")) {
            docPropMatch = StringFilter.MATCH_EXACT;
        } else if (docAuthorOrCreator && StringUtils.contains(value, ":")) {
            docPropMatch = StringFilter.MATCH_EXACT;
            docPropValue = StringUtils.substringAfter(value, ":");
        }

        this.handleMatch(where, bindingValues, propName, docPropValue, docPropMatch);
    }

    private void handleMatch(QueryBuffer where, List<Object> bindingValues, String propName, String value, String
        match)
    {

        if (value == null) {
            where.append(propName).append(SearchUtils.getComparisonOperator("is null", this.isNegate()));

        } else if (StringUtils.equals(match, StringFilter.MATCH_EXACT)) {
            where.append(propName).append(SearchUtils.getComparisonOperator("=", this.isNegate())).append("? ");
            bindingValues.add(value);

        } else if (StringUtils.equals(match, StringFilter.MATCH_CASE_INSENSITIVE)) {
            where.append("upper(").append(propName).append(")").append(SearchUtils.getComparisonOperator("=", this
                .isNegate())).append("? ");
            bindingValues.add(StringUtils.upperCase(value));

        } else {
            where.append("upper(").append(propName).append(CLOSING_BRACKET).append(
                SearchUtils.getComparisonOperator("like", this.isNegate())).append(" upper(?) ESCAPE '!' ");
            bindingValues.add("%" + value.replaceAll("[\\[_%!]", "!$0") + "%");
        }
    }

    private void handleRefMatch(QueryBuffer where, String propName, String value, String match)
    {

        if (StringUtils.equals(match, StringFilter.MATCH_EXACT)) {
            where.append(propName).append("=").append(value);
        } else if (StringUtils.equals(match, StringFilter.MATCH_CASE_INSENSITIVE)) {
            where.append("upper(").append(propName).append(")=upper(").append(value).append(")");
        } else {
            where.append("upper(").append(propName);
            where.append(") like concat('%', concat(upper(").append(value).append("), '%')) ESCAPE '!' ");
        }
        where.append(" ");
    }

    private static boolean isDocAuthorOrCreator(PropertyName propertyName)
    {
        return StringUtils.equals(propertyName.get(), DOC_CREATOR_PROPERTY)
            || StringUtils.equals(propertyName.get(), DOC_AUTHOR_PROPERTY);
    }
}
