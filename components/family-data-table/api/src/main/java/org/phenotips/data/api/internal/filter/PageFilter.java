/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal.filter;

import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Filter dealing with PageClass properties.
 *
 * @version $Id$
 */
public class PageFilter extends StringFilter
{
    /**
     * Constructor.
     * @param property PropertyInterface
     * @param baseClass BaseClass
     */
    public PageFilter(PropertyInterface property, BaseClass baseClass)
    {
        super(property, baseClass);
    }

    @Override
    public String getPropertyValueNameForQuery()
    {
        return "replace(str(" + this.getPropertyNameForQuery() + ".value), 'xwiki:', '')";
    }
}
