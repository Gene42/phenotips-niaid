/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal.filter;

import org.phenotips.data.api.internal.SpaceAndClass;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class BindingFilter extends AbstractPropertyFilter<String>
{
    public BindingFilter(PropertyInterface property, BaseClass baseClass)
    {
        super(property, baseClass);
        super.tableName = "StringProperty";
    }

    @Override public StringBuilder whereHql(StringBuilder where, List<Object> bindingValues)
    {
        super.whereHql(where, bindingValues);

        String objPropName = super.getObjectPropertyName() + ".value";
        String docName = super.getParent().getParent().getDocName();

        where.append(" and ").append(objPropName).append("=concat('xwiki:',").append(docName).append(".fullName) ");

        return where;
    }

    @Override public boolean isValid()
    {
        return StringUtils.isNotBlank(super.propertyName) && SpaceAndClass.isValid(super.getSpaceAndClass());
    }
}
