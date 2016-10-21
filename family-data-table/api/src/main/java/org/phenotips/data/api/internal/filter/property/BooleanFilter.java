/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal.filter.property;

import org.phenotips.data.api.internal.filter.AbstractPropertyFilter;

import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class BooleanFilter extends AbstractPropertyFilter<Integer>
{
    public BooleanFilter(PropertyInterface property, BaseClass baseClass)
    {
        super(property, baseClass);
        super.tableName = "IntegerProperty";
    }
}
