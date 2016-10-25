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

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
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

    protected String match;

    /**
     * Constructor.
     * @param property PropertyInterface
     * @param baseClass BaseClass
     */
    public StringFilter(PropertyInterface property, BaseClass baseClass)
    {
        super(property, baseClass);
        super.tableName = "StringProperty";
    }

    @Override public AbstractPropertyFilter populate(JSONObject input, DocumentQuery parent)
    {
        super.populate(input, parent);

        this.match = input.optString(MATCH_KEY);

        Object valueObj = input.opt(VALUES_KEY);

        if (valueObj == null) {
            return this;
        }

        this.values = new LinkedList<>();

        if (valueObj instanceof JSONArray) {
            JSONArray valuesArray = (JSONArray) valueObj;
            for (Object objValue : valuesArray) {
                if (objValue == null) {
                    continue;
                }
                this.values.add(String.valueOf(objValue));
            }
        } else if (valueObj instanceof String) {
            this.values.add((String) valueObj);
        } else {
            throw new IllegalArgumentException(
                String.format("Invalid value for key %1$s: [%2$s]", VALUES_KEY, valueObj));
        }

        return this;
    }

    @Override public StringBuilder whereHql(StringBuilder where, List<Object> bindingValues)
    {
        if (CollectionUtils.isEmpty(this.values)) {
            return where;
        }

        super.whereHql(where, bindingValues);

        String objPropName;

        if (super.isDocumentProperty) {
            objPropName = "str(" +  super.getDocumentPropertyName() + ")";
        } else {
            objPropName = super.getObjectPropertyName() + ".value";
        }

        where.append(" and (");

        for (int i = 0, len = this.values.size(); i < len; i++) {
            String value = this.values.get(i);
            if (value == null) {
                continue;
            }

            if (i > 0) {
                where.append(" or ");
            }

            if (super.isDocumentProperty) {
                boolean docAuthorOrCreator = StringUtils.equals(super.documentPropertyName, DOC_CREATOR_PROPERTY)
                                          || StringUtils.equals(super.documentPropertyName, DOC_AUTHOR_PROPERTY);

                this.handleDocumentProperties(where, bindingValues, objPropName, value, docAuthorOrCreator);
            } else {
                this.handleMatch(where, bindingValues, objPropName, value, this.match);
            }
        }

        return where.append(") ");
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
