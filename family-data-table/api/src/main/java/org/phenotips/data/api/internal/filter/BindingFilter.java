/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal.filter;

import java.util.List;

import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class BindingFilter extends AbstractPropertyFilter<String>
{

    /**
     * Constructor.
     * @param property PropertyInterface
     * @param baseClass BaseClass
     */
    public BindingFilter(PropertyInterface property, BaseClass baseClass)
    {
        super(property, baseClass, "StringProperty");
    }

    @Override public StringBuilder whereHql(StringBuilder where, List<Object> bindingValues)
    {
        super.whereHql(where, bindingValues);

        String objPropName = super.getPropertyNameForQuery() + ".value";
        String docName = super.getParent().getParent().getDocName();

        where.append(" ").append(objPropName).append("=concat('xwiki:',").append(docName).append(".fullName) ");

        return where;
    }

    @Override public boolean isValid()
    {
        return true;
    }
}
