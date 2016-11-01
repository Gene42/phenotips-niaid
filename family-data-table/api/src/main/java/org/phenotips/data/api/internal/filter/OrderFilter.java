package org.phenotips.data.api.internal.filter;

import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class OrderFilter extends AbstractPropertyFilter<String>
{
    /**
     * Constructor.
     * @param property PropertyInterface
     * @param baseClass BaseClass
     */
    public OrderFilter(PropertyInterface property, BaseClass baseClass)
    {
        super(property, baseClass);
    }
}
