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
import com.xpn.xwiki.objects.classes.NumberClass;

/**
 * Filter handling number properties.
 *
 * @version $Id$
 */
public class NumberFilter extends AbstractPropertyFilter<Number>
{
    /** Param key. */
    public static final String MIN_KEY = "min";

    /** Param key. */
    public static final String MAX_KEY = "max";

    private static final String TYPE_INTEGER = "integer";
    private static final String TYPE_FLOAT = "float";
    private static final String TYPE_LONG = "long";
    private static final String TYPE_DOUBLE = "double";

    private String numberType;

    /**
     * Constructor.
     * @param property PropertyInterface
     * @param baseClass BaseClass
     */
    public NumberFilter(PropertyInterface property, BaseClass baseClass)
    {
        super(property, baseClass);
    }

    @Override public AbstractPropertyFilter populate(JSONObject input, DocumentQuery parent)
    {
        super.populate(input, parent);

        this.numberType = ((NumberClass) super.property).getNumberType();
        super.tableName = StringUtils.capitalize(this.numberType) + "Property";

        Object valueObj = input.opt(VALUES_KEY);

        this.values = new LinkedList<>();

        if (valueObj instanceof JSONArray) {
            JSONArray valuesArray = (JSONArray) valueObj;
            for (Object objValue : valuesArray) {
                this.addValue(objValue, this.values);
            }
        } else {
            this.addValue(valueObj, this.values);
        }

        this.min = this.getValue(input.opt(MIN_KEY));
        this.max = this.getValue(input.opt(MAX_KEY));

        return this;
    }

    @Override public StringBuilder whereHql(StringBuilder where, List<Object> bindingValues)
    {
        if (!this.isValid()) {
            return where;
        }

        String objPropName = super.getObjectPropertyName() + ".value";

        where.append(" and ");

        if (CollectionUtils.isNotEmpty(this.values)) {
            if (this.values.size() > 1) {
                where.append(objPropName).append(" in (").append(StringUtils.repeat("?", ", ", this.values.size()));
                where.append(") ");
                bindingValues.addAll(this.values);
            } else {
                where.append(objPropName).append("=? ");
                bindingValues.add(this.values.get(0));
            }

        } else if (this.min != null) {
            where.append(objPropName).append(" &gt;=? ");
            bindingValues.add(this.min);
        } else {
            where.append(objPropName).append(" &lt;=? ");
            bindingValues.add(this.max);
        }

        return where;
    }

    /**
     * Returns a Number from the given object.
     * @param valueObj the object to convert into a number
     * @return a Number or null if invalid input
     */
    private Number getValue(Object valueObj)
    {
        if (valueObj == null) {
            return null;
        }

        Number toReturn = null;

        if (valueObj instanceof String) {
            toReturn = getNumberFromString((String) valueObj, this.numberType);
        } else if (valueObj instanceof Number && doesNumberMatchPropertyType(valueObj, this.numberType)) {
            toReturn = (Number) valueObj;
        }

        return toReturn;
    }

    private void addValue(Object valueObj, List<Number> valueList)
    {
        Number numberValue = this.getValue(valueObj);
        if (numberValue != null) {
            valueList.add((Number) valueObj);
        }
    }

    private static Number getNumberFromString(String value, String numberType)
    {
        Number toReturn = null;

        try {
            if (StringUtils.equals(TYPE_INTEGER, numberType)) {
                toReturn = Integer.parseInt(value);
            } else if (StringUtils.equals(TYPE_FLOAT, numberType)) {
                toReturn = Float.parseFloat(value);
            } else if (StringUtils.equals(TYPE_DOUBLE, numberType)) {
                toReturn = Double.parseDouble(value);
            } else if (StringUtils.equals(TYPE_LONG, numberType)) {
                toReturn = Long.parseLong(value);
            }
        } catch (NumberFormatException e) {
            // Do nothing
        }

        return toReturn;
    }

    private static boolean doesNumberMatchPropertyType(Object valueObj, String numberType)
    {
        boolean isInt = StringUtils.equals(TYPE_INTEGER, numberType) && valueObj instanceof Integer;
        boolean isFloat = StringUtils.equals(TYPE_FLOAT, numberType) && valueObj instanceof Float;
        boolean isDouble = StringUtils.equals(TYPE_DOUBLE, numberType) && valueObj instanceof Double;
        boolean isLong = StringUtils.equals(TYPE_LONG, numberType) && valueObj instanceof Long;
        return isInt || isFloat || isDouble || isLong;
    }
}
