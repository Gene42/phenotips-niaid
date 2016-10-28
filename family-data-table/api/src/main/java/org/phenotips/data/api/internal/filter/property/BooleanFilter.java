/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal.filter.property;

import org.phenotips.data.api.internal.DocumentUtils;
import org.phenotips.data.api.internal.filter.AbstractPropertyFilter;
import org.phenotips.data.api.internal.filter.DocumentQuery;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Filter handling boolean values.
 *
 * @version $Id$
 */
public class BooleanFilter extends AbstractPropertyFilter<Integer>
{


    /**
     * Constructor.
     * @param property PropertyInterface
     * @param baseClass BaseClass
     */
    public BooleanFilter(PropertyInterface property, BaseClass baseClass)
    {
        super(property, baseClass, "IntegerProperty");
    }

    @Override public AbstractPropertyFilter init(JSONObject input, DocumentQuery parent)
    {
        super.init(input, parent);

        String value = AbstractPropertyFilter.getValue(input, VALUES_KEY);

        if (StringUtils.isBlank(value)) {
            return this;
        }

        String lowerCaseValue = StringUtils.lowerCase(value);

        if (DocumentUtils.BOOLEAN_TRUE_SET.contains(lowerCaseValue)) {
            super.addValue(1);
        } else if (DocumentUtils.BOOLEAN_FALSE_SET.contains(lowerCaseValue)) {
            super.addValue(0);
        }

        return this;
    }

    @Override public StringBuilder addValueConditions(StringBuilder where, List<Object> bindingValues)
    {
        if (CollectionUtils.isEmpty(super.getValues())) {
            return where;
        }

        super.addValueConditions(where, bindingValues);

        String objPropName = super.getPropertyValueNameForQuery();

        where.append(objPropName).append("=? ");
        bindingValues.add(this.getValues().get(0));

        return where;
    }
}
