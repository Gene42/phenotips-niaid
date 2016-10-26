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

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.joda.time.DateTime;
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

    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("MM/dd/yyyy");

    /**
     * Constructor.
     * @param property PropertyInterface
     * @param baseClass BaseClass
     */
    public DateFilter(PropertyInterface property, BaseClass baseClass)
    {
        super(property, baseClass);
        super.setTableName("DateProperty");
    }

    @Override public AbstractPropertyFilter populate(JSONObject input, DocumentQuery parent)
    {
        super.populate(input, parent);

        List<String> stringValues = DocumentSearchUtils.getValues(input, VALUES_KEY);

        for (String strValue : stringValues) {
            this.addValue(this.getValue(strValue));
        }

        super.setMin(this.getValue(DocumentSearchUtils.getValue(input, MIN_KEY)));
        super.setMax(this.getValue(DocumentSearchUtils.getValue(input, MAX_KEY)));

        return this;
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

        if (super.isDocumentProperty()) {
            objPropName = super.getDocumentPropertyName();
        } else {
            objPropName = super.getObjectPropertyName() + ".value";
        }

        where.append(" and ");

        if (CollectionUtils.isNotEmpty(super.getValues())) {

            where.append(" (");

            for (int i = 0, len = super.getValues().size(); i < len; i++) {
                DateTime value = super.getValues().get(i);
                if (value == null) {
                    continue;
                }

                if (i > 0) {
                    where.append(" or ");
                }

                where.append("upper(str(").append(objPropName).append(")) like upper(?) ESCAPE '!' ");
                bindingValues.add("%" + FORMATTER.print(value).replaceAll("[\\[_%!]", "!$0") + "%");
            }

            where.append(") ");

        } else if (super.getMin() != null) {
            where.append(objPropName).append(" &gt;=? ");
            bindingValues.add(super.getMin());
        } else {
            where.append(objPropName).append(" &lt;=? ");
            bindingValues.add(super.getMax().plusDays(1));
        }

        return where;
    }
}
