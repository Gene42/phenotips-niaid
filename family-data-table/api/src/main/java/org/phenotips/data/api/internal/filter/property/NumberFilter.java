package org.phenotips.data.api.internal.filter.property;

import org.phenotips.data.api.internal.filter.AbstractObjectFilterFactory;
import org.phenotips.data.api.internal.filter.ObjectFilter;

import org.json.JSONObject;

import com.xpn.xwiki.objects.DoubleProperty;
import com.xpn.xwiki.objects.FloatProperty;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.objects.LongProperty;
import com.xpn.xwiki.objects.NumberProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class NumberFilter extends ObjectFilter<Number>
{
    /*public NumberFilter()
    {
        //IntegerProperty
        value.getClass();
    }*/
    //protected Class numberClass;
    //protected NumberProperty numberProperty;

    public NumberFilter(PropertyInterface property, BaseClass baseClass)
    {
        super(property, baseClass);
    }

    /*public NumberFilter(NumberProperty numberProperty)
    {
        this.numberProperty = numberProperty;
    }*/

    private Value numberValue;

    @Override public ObjectFilter populate(JSONObject input, int level)
    {
        super.populate(input, level);

        if (super.property instanceof IntegerProperty) {
            super.tableName = "IntegerProperty";
        }
        else if (super.property instanceof FloatProperty) {
            super.tableName = "FloatProperty";
        }
        else if (super.property instanceof DoubleProperty) {
            super.tableName = "DoubleProperty";
        }
        else {
            super.tableName = "LongProperty";
        }

        return this;
    }

    public static class Value<T extends Number> {

        protected T min;
        protected T max;
        protected T value;
    }
}
