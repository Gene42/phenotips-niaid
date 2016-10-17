package org.phenotips.data.api.internal.filter.property;

import org.phenotips.data.api.internal.filter.ObjectFilter;

import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class BooleanFilter extends ObjectFilter
{
    public BooleanFilter(PropertyInterface property, BaseClass baseClass)
    {
        super(property, baseClass);
        super.tableName = "IntegerProperty";
    }
}
