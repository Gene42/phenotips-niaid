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
import org.phenotips.data.api.internal.filter.PropertyName;

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
        super(property, baseClass, "StringProperty");
    }

    @Override public AbstractPropertyFilter init(JSONObject input, DocumentQuery parent)
    {
        super.init(input, parent);

        this.match = input.optString(MATCH_KEY);

        super.setValues(AbstractPropertyFilter.getValues(input, VALUES_KEY));

        return this;
    }

    @Override public StringBuilder addValueConditions(StringBuilder where, List<Object> bindingValues)
    {
        boolean hasValues = CollectionUtils.isNotEmpty(super.getValues());
        boolean hasRefValues = CollectionUtils.isNotEmpty(super.getRefValues());

        if (!hasValues && !hasRefValues) {
            return where;
        }

        super.addValueConditions(where, bindingValues);

        String objPropName = this.getPropertyValueNameForQuery();

        boolean docAuthorOrCreator = isDocAuthorOrCreator(super.getPropertyName());

        if (hasValues) {
            where.append(" (");

            for (int i = 0, len = super.getValues().size(); i < len; i++) {
                String value = super.getValues().get(i);

                DocumentQuery.appendQueryOperator(where, "or", i);

                if (super.isDocumentProperty()) {
                    this.handleDocumentProperties(where, bindingValues, objPropName, value, docAuthorOrCreator);
                } else {
                    this.handleMatch(where, bindingValues, objPropName, value, this.match);
                }
            }

            where.append(") ");
        }

        if (hasValues && hasRefValues) {
            where.append(" and ");
        }

        if (hasRefValues) {
            where.append(" (");

            for (int i = 0, len = super.getRefValues().size(); i < len; i++) {
                AbstractPropertyFilter ref = super.getRefValues().get(i);

                DocumentQuery.appendQueryOperator(where, "or", i);

                String refPropertyName = ref.getPropertyValueNameForQuery();


                this.handleRefMatch(where, objPropName, refPropertyName, this.match);
            }

            where.append(") ");
        }

        return where;
    }

    @Override public String getPropertyValueNameForQuery()
    {
        if (super.isDocumentProperty()) {
            return "str(" +  super.getPropertyNameForQuery() + ")";
        } else {
            return super.getPropertyNameForQuery() + ".value";
        }
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

    private void handleRefMatch(StringBuilder where, String propName, String value, String match) {
//where.append(" ").append(objPropName).append("=concat('xwiki:',").append(docName).append(".fullName) ");
        if (StringUtils.equals(match, MATCH_EXACT)) {
            where.append(propName).append("=").append(value);
        } else if (StringUtils.equals(match, MATCH_CASE_INSENSITIVE)) {
            where.append("upper(").append(propName).append(")=upper(").append(value).append(")");
        } else {
            //where.append("upper(").append(propName).append(") like upper(?) ESCAPE '!' ");

            where.append("upper(").append(propName);
            where.append(") like concat('%', concat(upper(").append(value).append("), '%')) ESCAPE '!' ");

            //bindingValues.add("%" + value.replaceAll("[\\[_%!]", "!$0") + "%");
        }
        where.append(" ");
    }

    private static boolean isDocAuthorOrCreator(PropertyName propertyName)
    {
        return StringUtils.equals(propertyName.get(), DOC_CREATOR_PROPERTY)
            || StringUtils.equals(propertyName.get(), DOC_AUTHOR_PROPERTY);
    }
}
