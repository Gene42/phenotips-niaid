/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal.filter.property;

import org.phenotips.data.api.internal.filter.AbstractPropertyFilter;
import org.phenotips.data.api.internal.filter.DocumentQuery;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Filter handling simple string properties.
 *
 * @version $Id$
 */
public class StringFilter extends AbstractPropertyFilter<String>
{
    /** Filter param key. */
    public static final String MATCH_KEY = "match";

    /** Match value. */
    public static final String MATCH_EXACT = "exact";

    /** Match value. */
    public static final String MATCH_SUBSTRING = null;

    /** Match value. */
    public static final String MATCH_CASE_INSENSITIVE = "ci";

    private static final String DOC_CREATOR_PROPERTY = "creator";
    private static final String DOC_AUTHOR_PROPERTY = "author";

    private String match;

    /**
     * Constructor.
     * @param property PropertyInterface
     * @param baseClass BaseClass
     */
    public StringFilter(PropertyInterface property, BaseClass baseClass)
    {
        super(property, baseClass);
        super.setTableName("StringProperty");
    }

    @Override public AbstractPropertyFilter populate(JSONObject input, DocumentQuery parent)
    {
        super.populate(input, parent);

        this.match = input.optString(MATCH_KEY);

        super.setValues(AbstractPropertyFilter.getValues(input, VALUES_KEY));

        return this;
    }

    @Override public StringBuilder whereHql(StringBuilder where, List<Object> bindingValues)
    {
        if (CollectionUtils.isEmpty(super.getValues())) {
            return where;
        }

        super.whereHql(where, bindingValues);

        String objPropName = super.getPropertyNameForQuery(null, ".value", "str(", ")");

        where.append(" (");

        for (int i = 0, len = super.getValues().size(); i < len; i++) {
            String value = super.getValues().get(i);

            super.appendQueryOperator(where, "or", i);

            if (super.isDocumentProperty()) {
                boolean docAuthorOrCreator = StringUtils.equals(super.getPropertyName().get(), DOC_CREATOR_PROPERTY)
                                          || StringUtils.equals(super.getPropertyName().get(), DOC_AUTHOR_PROPERTY);

                this.handleDocumentProperties(where, bindingValues, objPropName, value, docAuthorOrCreator);
            } else {
                this.handleMatch(where, bindingValues, objPropName, value, this.match);
            }
        }

        return where.append(") ");
    }

    /**
     * Getter for match.
     *
     * @return match
     */
    public String getMatch()
    {
        return match;
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

    private void handleDocumentProperties(StringBuilder where, List<Object> bindingValues, String propName, String
        value, boolean docAuthorOrCreator)
    {
        String docPropMatch = MATCH_SUBSTRING;
        String docPropValue = value;

        if (docAuthorOrCreator && StringUtils.startsWith(value, "XWiki.")) {
            docPropMatch = MATCH_EXACT;
        } else if (docAuthorOrCreator && StringUtils.contains(value, ":")) {
            docPropMatch = MATCH_EXACT;
            docPropValue = StringUtils.substringAfter(value, ":");
        }

        this.handleMatch(where, bindingValues, propName, docPropValue, docPropMatch);
    }

    private void handleMatch(StringBuilder where, List<Object> bindingValues, String propName, String value, String
        match) {

        if (StringUtils.equals(match, MATCH_EXACT)) {
            where.append(propName).append("=? ");
            bindingValues.add(value);
        } else if (StringUtils.equals(match, MATCH_CASE_INSENSITIVE)) {
            where.append("upper(").append(propName).append(")=? ");
            bindingValues.add(StringUtils.upperCase(value));
        } else {
            where.append("upper(").append(propName).append(") like upper(?) ESCAPE '!' ");
            bindingValues.add("%" + value.replaceAll("[\\[_%!]", "!$0") + "%");
        }

    }
}
