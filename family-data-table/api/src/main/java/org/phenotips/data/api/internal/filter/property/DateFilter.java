/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal.filter.property;

import org.phenotips.data.api.internal.DocumentSearchUtils;
import org.phenotips.data.api.internal.filter.AbstractPropertyFilter;
import org.phenotips.data.api.internal.filter.DocumentQuery;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONObject;

import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;

//import org.apache.commons.lang3.StringUtils;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class DateFilter extends AbstractPropertyFilter<DateTime>
{
    /** Param key. */
    public static final String MIN_KEY = "after";

    /** Param key. */
    public static final String MAX_KEY = "before";

    /** Param key. */
    private static final String AGE_KEY = "age";

    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("MM/dd/yyyy");

    /**
     * Constructor.
     * @param property PropertyInterface
     * @param baseClass BaseClass
     */
    public DateFilter(PropertyInterface property, BaseClass baseClass)
    {
        super(property, baseClass);
        super.tableName = "DateProperty";
    }

    @Override public AbstractPropertyFilter populate(JSONObject input, DocumentQuery parent)
    {
        super.populate(input, parent);

        List<String> stringValues = DocumentSearchUtils.getValues(input, VALUES_KEY);

        this.values = new LinkedList<>();

        for (String strValue : stringValues) {
            this.addValue(strValue, this.values);
        }

        this.min = this.getValue(DocumentSearchUtils.getValue(input, MIN_KEY));
        this.max = this.getValue(DocumentSearchUtils.getValue(input, MAX_KEY));

        return this;
    }

    private void addValue(String value, List<DateTime> valueList) {
        DateTime dateTimeValue = this.getValue(value);
        if (dateTimeValue != null) {
            valueList.add(dateTimeValue);
        }
    }

    private DateTime getValue(String value)
    {
        if (value == null) {
            return null;
        }
        return FORMATTER.parseDateTime(value);
    }


    @Override public StringBuilder whereHql(StringBuilder where, List<Object> bindingValues)
    {
        if (!this.isValid()) {
            return where;
        }

        String objPropName;

        if (super.isDocumentProperty) {
            objPropName = super.getDocumentPropertyName();
        } else {
            objPropName = super.getObjectPropertyName() + ".value";
        }

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

    /*public DateFilter(String name, Map<String, String> values)
    {
        //super(name, values);

        //this.exact = values.get(name);

        if (!super.hasSingleNonNullValue()) {
            if (values.containsKey(MAX)) {
                this.max = FORMATTER.parseDateTime(values.get(MAX));

                // TODO: why do we do this? We should add comment in the code to clarify
                this.max.plusDays(1);
            }

            if (values.containsKey(MIN)) {
                this.min = FORMATTER.parseDateTime(values.get(MIN));
            }
        }
    }

    @Override
    public StringBuilder hql(StringBuilder builder, Query query) {

        if (super.hasSingleNonNullValue() && this.min == null && this.max == null) {
            return builder;
        }

        if (super.hasSingleNonNullValue()) {

        }
        else {
            if (this.min != null) {

            }

            if (this.max != null) {

            }
        }

        return builder;
    }*/
}
