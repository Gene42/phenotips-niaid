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

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Filter handling boolean values.
 *
 * @version $Id$
 */
public class BooleanFilter extends AbstractFilter<Integer>
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

    @Override
    public AbstractFilter init(JSONObject input, DocumentQuery parent, QueryExpression expressionParent)
    {
        super.init(input, parent, expressionParent);

        String value = SearchUtils.getValue(input, VALUES_KEY);

        if (StringUtils.isBlank(value)) {
            return this;
        }

        String lowerCaseValue = StringUtils.lowerCase(value);

        if (SearchUtils.BOOLEAN_TRUE_SET.contains(lowerCaseValue)) {
            super.addValue(1);
        } else if (SearchUtils.BOOLEAN_FALSE_SET.contains(lowerCaseValue)) {
            super.addValue(0);
        }

        return this;
    }

    @Override
    public QueryBuffer addValueConditions(QueryBuffer where, List<Object> bindingValues)
    {
        if (CollectionUtils.isEmpty(super.getValues())) {
            return where;
        }

        this.startElement(where, bindingValues);

        String objPropName = super.getPropertyValueNameForQuery();

        where.append(objPropName).append("=? ");
        bindingValues.add(this.getValues().get(0));

        return this.endElement(where);
    }
}
