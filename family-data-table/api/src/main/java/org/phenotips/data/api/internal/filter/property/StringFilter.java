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
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class StringFilter extends AbstractPropertyFilter<String>
{
    public static final String MATCH_KEY = "match";

    private String match;

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
        }
        else {
            objPropName = super.getObjectPropertyName() + ".value";
        }

        where.append(" and ");

        if (this.values.size() > 1) {
            where.append(objPropName).append(" in (").append(StringUtils.repeat("?", ", ", this.values.size()));
            where.append(") ");
            bindingValues.addAll(this.values);
        } else {
            String value = this.values.get(0);

            if (StringUtils.equals(this.match, "exact")) {
                where.append(objPropName).append("=? ");
                bindingValues.add(value);
            } else if (StringUtils.equals(this.match, "ci")) {
                where.append("upper(").append(objPropName).append(")=? ");
                bindingValues.add(StringUtils.upperCase(value));
            } else {
                where.append("upper(").append(objPropName).append(") like upper(?) ESCAPE '!' ");
                bindingValues.add("%" + value.replaceAll("[\\[_%!]", "!$0") + "%");
            }
        }

        return where;
    }
}
