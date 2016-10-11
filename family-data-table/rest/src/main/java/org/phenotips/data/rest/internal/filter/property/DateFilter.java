package org.phenotips.data.rest.internal.filter.property;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

//import org.apache.commons.lang3.StringUtils;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class DateFilter
{
    private static final String MAX = "before";
    private static final String MIN = "after";

    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("MM/dd/yyyy");

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
