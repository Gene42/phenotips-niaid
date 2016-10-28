package org.phenotips.data.api.internal.filter.property;

import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class PageFilter  extends StringFilter
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

    @Override public String getPropertyValueNameForQuery()
    {
        return "replace(" + this.getPropertyNameForQuery() + ".value, 'xwiki:', '')";
    }
}
