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
import org.joda.time.DateTime;
import org.joda.time.DurationFieldType;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONObject;

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

    private static final String YEAR_KEY = "year";

    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("MM/dd/yyyy");

    private static final DateTimeFormatter ENCRYPTED_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd");

    //private static final Pattern AGE_INPUT_PATTERN = Pattern.compile("[0-9]*[yYmMwWdD]?");

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

        if (super.getMax() != null) {
            super.setMax(super.getMax().plusDays(1));
        }

        if (input.has(AGE_KEY)) {
            this.handleAge(AbstractPropertyFilter.getValue(input, AGE_KEY));
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
        } else {

            if (super.getMin() != null) {
                where.append(this.handleEncryption(objPropName)).append(">=? ");
                bindingValues.add(this.handleValueEncryption(super.getMin()));
            }

            if (super.getMax() != null) {

                if (super.getMin() != null) {
                    where.append(" and ");
                }

                where.append(this.handleEncryption(objPropName)).append("<=? ");
                bindingValues.add(this.handleValueEncryption(super.getMax()));
            }
        }

        return where;
    }

    private String handleEncryption(String objPropName)
    {
        if (this.isEncrypted()) {
            return " str(" + objPropName + ") ";
        } else {
            return objPropName;
        }
    }

    private Object handleValueEncryption(DateTime value)
    {
        if (this.isEncrypted()) {
            return ENCRYPTED_FORMATTER.print(value);
        } else {
            return value.toDate();
        }
    }

    private DateTime getValue(String value)
    {
        if (value == null) {
            return null;
        }
        return FORMATTER.parseDateTime(value);
    }

    private void handleAge(String ageStr)
    {
        //String lowerCase = StringUtils.trim(StringUtils.lowerCase(ageStr));

        /*if (!AGE_INPUT_PATTERN.matcher(lowerCase).matches()) {
            throw new IllegalArgumentException(String.format("Invalid age format [%1$s]", ageStr));
        }*/

        DateTime now = DateTime.now();

        Period agePeriod;
        String pAge = "P" + ageStr;
        agePeriod = Period.parse(StringUtils.isNumeric(ageStr) ? pAge + "Y" : pAge);

        DateTime minDob = now.minus(agePeriod);
        DateTime maxDob = now.minus(agePeriod);

        DurationFieldType minPrecision = this.getMinPrecision(agePeriod);

        if (DurationFieldType.months().equals(minPrecision)) {
            minDob = minDob.minusMonths(1).plusDays(1);
        } else if (DurationFieldType.weeks().equals(minPrecision)) {
            minDob = minDob.minusWeeks(1).plusDays(1);
        } else if (DurationFieldType.days().equals(minPrecision)) {
            minDob = minDob.minusDays(1).plusHours(1);
        } else {
            minDob = minDob.minusYears(1).plusDays(1);
        }

        /*if (StringUtils.contains(lowerCase, "m")) {
            minDob = minDob.minusMonths(1).plusDays(1);
            minPrecision = "m";
        } else if (StringUtils.contains(lowerCase, "w")) {
            minDob = minDob.minusWeeks(1).plusDays(1);
        } else if (StringUtils.contains(lowerCase, "d")) {
            minDob = minDob.minusDays(1).plusHours(1);
        } else {
            minDob = minDob.minusYears(1).plusDays(1);
        }*/

        this.setMin(minDob);
        this.setMax(maxDob);
    }

    private DurationFieldType getMinPrecision(Period period)
    {
        DurationFieldType minPrecision = DurationFieldType.years();

        if (period.getMonths() != 0) {
            minPrecision = DurationFieldType.months();
        }

        if (period.getWeeks() != 0) {
            minPrecision = DurationFieldType.weeks();
        }

        if (period.getDays() != 0) {
            minPrecision = DurationFieldType.days();
        }

        return minPrecision;
    }
}
