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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private static final Set<String> YES_SET = new HashSet<>();
    private static final Set<String> NO_SET = new HashSet<>();

    static {
        YES_SET.add("yes");
        YES_SET.add("true");
        YES_SET.add("1");

        NO_SET.add("no");
        NO_SET.add("false");
        NO_SET.add("0");
    }

    /**
     * Constructor.
     * @param property PropertyInterface
     * @param baseClass BaseClass
     */
    public BooleanFilter(PropertyInterface property, BaseClass baseClass)
    {
        super(property, baseClass);
        super.setTableName("IntegerProperty");
    }

    @Override public AbstractPropertyFilter populate(JSONObject input, DocumentQuery parent)
    {
        super.populate(input, parent);

        String value = DocumentSearchUtils.getValue(input, VALUES_KEY);

        if (StringUtils.isBlank(value)) {
            return this;
        }

        String lowerCaseValue = StringUtils.lowerCase(value);

        if (YES_SET.contains(lowerCaseValue)) {
            super.addValue(1);
        } else if (NO_SET.contains(lowerCaseValue)) {
            super.addValue(0);
        }

        return this;
    }

    @Override public StringBuilder whereHql(StringBuilder where, List<Object> bindingValues)
    {
        if (CollectionUtils.isEmpty(super.getValues())) {
            return where;
        }

        super.whereHql(where, bindingValues);

        String objPropName;

        if (super.isDocumentProperty()) {
            objPropName = super.getDocumentPropertyName();
        } else {
            objPropName = super.getObjectPropertyName() + ".value";
        }

        where.append(" and ").append(objPropName).append("=? ");
        bindingValues.add(this.getValues().get(0));

        return where;
    }
}
