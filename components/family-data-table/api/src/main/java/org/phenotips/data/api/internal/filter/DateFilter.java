/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal.filter;

import org.phenotips.data.api.internal.DocumentQuery;
import org.phenotips.data.api.internal.QueryBuffer;
import org.phenotips.data.api.internal.QueryExpression;
import org.phenotips.data.api.internal.SearchUtils;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
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
 * Filter dealing with dates.
 *
 * @version $Id$
 */
public class DateFilter extends AbstractFilter<DateTime>
{
    /** Param key. */
    public static final String MIN_KEY = "after";

    /** Param key. */
    public static final String MAX_KEY = "before";

    private static final List<String> VALUE_PROPERTY_NAMES = ListUtils.unmodifiableList(
        Arrays.asList(DateFilter.MIN_KEY, DateFilter.MAX_KEY, DateFilter.AGE_KEY)
    );

    /** Param key. */
    private static final String AGE_KEY = "age";

    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("MM/dd/yyyy");

    private static final DateTimeFormatter ENCRYPTED_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd");

    /**
     * Constructor.
     * @param property PropertyInterface
     * @param baseClass BaseClass
     */
    public DateFilter(PropertyInterface property, BaseClass baseClass)
    {
        super(property, baseClass, "DateProperty");
    }

    @Override
    public AbstractFilter init(JSONObject input, DocumentQuery parent, QueryExpression expressionParent)
    {
        super.init(input, parent, expressionParent);

        List<String> stringValues = SearchUtils.getValues(input, VALUES_KEY);

        for (String strValue : stringValues) {
            this.addValue(this.getValue(strValue));
        }

        this.setMin(this.getValue(SearchUtils.getValue(input, MIN_KEY)));
        this.setMax(this.getValue(SearchUtils.getValue(input, MAX_KEY)));

        if (this.getMax() != null) {
            this.setMax(this.getMax().plusDays(1));
        }

        if (input.has(AGE_KEY)) {
            this.handleAge(SearchUtils.getValue(input, AGE_KEY));
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
            this.handleMultipleValues(objPropName, where, bindingValues);

        } else if (this.getMin() != null && this.getMax() != null) {
            this.handleBothMinAndMaxPresent(objPropName, where, bindingValues);

        } else if (this.getMin() != null) {
            where.append(this.handleEncryption(objPropName)).append(">=? ");
            bindingValues.add(this.handleValueEncryption(this.getMin()));

        } else if (this.getMax() != null) {
            where.append(this.handleEncryption(objPropName)).append("<=? ");
            bindingValues.add(this.handleValueEncryption(this.getMax()));
        }

        if (CollectionUtils.isNotEmpty(this.getRefValues())) {
            this.handleRefValues(where);
        }

        return this.endElement(where);
    }

    /**
     * Getter for VALUE_PROPERTY_NAMES.
     *
     * @return VALUE_PROPERTY_NAMES
     */
    public static List<String> getValueParameterNames()
    {
        return VALUE_PROPERTY_NAMES;
    }

    private void handleMultipleValues(String objPropName, QueryBuffer where, List<Object> bindingValues)
    {
        where.saveAndReset("or").append(" (");

        for (int i = 0, len = this.getValues().size(); i < len; i++) {
            where.appendOperator().append("upper(str(").append(objPropName).append(")) like upper(?) ESCAPE '!' ");
            bindingValues.add("%" + FORMATTER.print(this.getValues().get(i)).replaceAll("[\\[_%!]", "!$0") + "%");
        }

        where.append(") ").load();
    }

    private void handleBothMinAndMaxPresent(String objPropName, QueryBuffer where, List<Object> bindingValues)
    {
        where.append(" (");

        where.append(this.handleEncryption(objPropName)).append(">=? ");
        where.append(" and ");
        where.append(this.handleEncryption(objPropName)).append("<=? ");

        bindingValues.add(this.handleValueEncryption(this.getMin()));
        bindingValues.add(this.handleValueEncryption(this.getMax()));

        where.append(") ");
    }

    private void handleRefValues(QueryBuffer where)
    {
        if (CollectionUtils.isNotEmpty(this.getValues()) || this.getMin() != null || this.getMax() != null) {
            where.append(" and ");
        }

        // TODO: implement date reference
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
