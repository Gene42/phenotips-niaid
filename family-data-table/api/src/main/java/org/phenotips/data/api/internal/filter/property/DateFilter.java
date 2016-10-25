/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal.filter.property;

import org.phenotips.data.api.internal.filter.AbstractPropertyFilter;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

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
    private static final String MAX = "before";
    private static final String MIN = "after";
    private static final String AGE = "age";

    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("MM/dd/yyyy");

    public DateFilter(PropertyInterface property, BaseClass baseClass)
    {
        super(property, baseClass);
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
