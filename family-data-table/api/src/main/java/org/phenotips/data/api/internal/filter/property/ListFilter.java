package org.phenotips.data.api.internal.filter.property;

import org.phenotips.data.api.internal.filter.AbstractObjectFilterFactory;
import org.phenotips.data.api.internal.filter.ObjectFilter;

import org.json.JSONObject;

import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class ListFilter extends ObjectFilter
{
    private boolean multiSelect;

    private boolean relationalStorage;

    public ListFilter(PropertyInterface property, BaseClass baseClass)
    {
        super(property, baseClass);
    }

    /*public ListFilter(boolean multiSelect, boolean relationalStorage) {
        this.multiSelect = multiSelect;
        this.relationalStorage = relationalStorage;
    }*/

    @Override public ObjectFilter populate(JSONObject input, int level)
    {
        super.populate(input, level);

        return this;
    }
}
