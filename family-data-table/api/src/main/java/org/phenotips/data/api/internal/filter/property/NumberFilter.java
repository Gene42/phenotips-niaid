/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal.filter.property;

import org.phenotips.data.api.internal.filter.AbstractPropertyFilter;
import org.phenotips.data.api.internal.filter.DocumentQuery;

import org.json.JSONObject;

import com.xpn.xwiki.objects.DoubleProperty;
import com.xpn.xwiki.objects.FloatProperty;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class NumberFilter extends AbstractPropertyFilter<Number>
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

    @Override public AbstractPropertyFilter populate(JSONObject input, DocumentQuery parent)
    {
        super.populate(input, parent);

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
}
