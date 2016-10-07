package org.phenotips.data.rest.internal.filter.property;

import org.phenotips.data.rest.internal.filter.AbstractFilter;

import org.xwiki.query.Query;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class DateFilter extends AbstractFilter
{
    private static final String MAX = "before";
    private static final String MIN = "after";

    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("MM/dd/yyyy");

    private String exact;
    private DateTime min; //max
    private DateTime max; //low

    /**
     *
     * @param name
     * @param values
     * @return
     */
    @Override
    public AbstractFilter parse(String name, Map<String, String> values)
    {
        super.parse(name, values);

        this.exact = values.get(name);

        if (StringUtils.isBlank(this.exact)) {
            if (values.containsKey(MAX)) {
                this.max = FORMATTER.parseDateTime(values.get(MAX));

                // TODO: why do we do this? We should add comment in the code to clarify
                this.max.plusDays(1);
            }

            if (values.containsKey(MIN)) {
                this.min = FORMATTER.parseDateTime(values.get(MIN));
            }
        }
        return this;
    }

    public StringBuilder append(StringBuilder builder, Query query) {
        return builder;
    }
}
