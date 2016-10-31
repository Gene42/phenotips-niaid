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
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONObject;

import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;

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

    private static final Pattern AGE_INPUT_PATTERN = Pattern.compile("[0-9]*[yYmMwWdD]?");

    private Integer age;

    /**
     * Constructor.
     * @param property PropertyInterface
     * @param baseClass BaseClass
     */
    public DateFilter(PropertyInterface property, BaseClass baseClass)
    {
        super(property, baseClass, "DateProperty");
    }

    @Override public AbstractPropertyFilter init(JSONObject input, DocumentQuery parent)
    {
        super.init(input, parent);

        List<String> stringValues = AbstractPropertyFilter.getValues(input, VALUES_KEY);

        for (String strValue : stringValues) {
            this.addValue(this.getValue(strValue));
        }

        super.setMin(this.getValue(AbstractPropertyFilter.getValue(input, MIN_KEY)));
        super.setMax(this.getValue(AbstractPropertyFilter.getValue(input, MAX_KEY)));

        if (input.has(AGE_KEY)) {
            this.age = Integer.valueOf(AbstractPropertyFilter.getValue(input, AGE_KEY));
            //super.setMin(getAgeMin(AbstractPropertyFilter.getValue(input, AGE_KEY)));
        }

        return this;
    }

    @Override public StringBuilder addValueConditions(StringBuilder where, List<Object> bindingValues)
    {
        if (!this.isValid()) {
            return where;
        }

        super.addValueConditions(where, bindingValues);

        String objPropName = super.getPropertyValueNameForQuery();

        if (CollectionUtils.isNotEmpty(super.getValues())) {

            where.append(" (");

            for (int i = 0, len = super.getValues().size(); i < len; i++) {
                DocumentQuery.appendQueryOperator(where, "or", i);

                where.append("upper(str(").append(objPropName).append(")) like upper(?) ESCAPE '!' ");
                bindingValues.add("%" + FORMATTER.print(super.getValues().get(i)).replaceAll("[\\[_%!]", "!$0") + "%");
            }

            where.append(") ");

        } else if (super.getMin() != null) {
            where.append(objPropName).append(">=? ");
            bindingValues.add(super.getMin().toDate());
        } else if (super.getMax() != null) {
            where.append(objPropName).append("<=? ");
            bindingValues.add(super.getMax().plusDays(1).toDate());
        } else if (this.age != null) {
            where.append(" ?=(? - year(").append(objPropName).append("))");
            bindingValues.add(this.age);
            bindingValues.add(DateTime.now().getYear());
        }

        return where;
    }

    @Override public boolean isValid()
    {
        return super.isValid() || this.age != null;
    }

    private DateTime getValue(String value)
    {
        if (value == null) {
            return null;
        }
        return FORMATTER.parseDateTime(value);
    }

    private DateTime getAgeMin(String ageStr)
    {
        String lowerCase = StringUtils.trim(StringUtils.lowerCase(ageStr));

        if (!AGE_INPUT_PATTERN.matcher(lowerCase).matches()) {
            throw new IllegalArgumentException(String.format("Invalid age format [%1$s]", ageStr));
        }

        DateTime now = DateTime.now();

        if (StringUtils.contains(lowerCase, "m")) {
            return now.minusMonths(getIntValue(lowerCase, "m"));
        } else if (StringUtils.contains(lowerCase, "w")) {
            return now.minusWeeks(getIntValue(lowerCase, "w"));
        } else if (StringUtils.contains(lowerCase, "d")) {
            return now.minusDays(getIntValue(lowerCase, "d"));
        } else {
            return now.minusYears(getIntValue(lowerCase, null));
        }
    }

    private int getIntValue(String value, String symbol)
    {
        Period agePeriod;
        //Period.parse()
        try {
            if (symbol == null) {
                return  Integer.valueOf(value);
            }
            return Integer.valueOf(StringUtils.trim(StringUtils.replace(value, symbol, StringUtils.EMPTY)));
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(String.format("Invalid age format [%1$s]", value));
        }
    }
}
