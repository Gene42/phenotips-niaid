package org.phenotips.data.api.internal.filter;

import org.phenotips.Constants;
import org.phenotips.data.api.internal.filter.property.BooleanFilter;
import org.phenotips.data.api.internal.filter.property.ListFilter;
import org.phenotips.data.api.internal.filter.property.NumberFilter;
import org.phenotips.data.api.internal.filter.property.StringFilter;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.DoubleProperty;
import com.xpn.xwiki.objects.FloatProperty;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.objects.LongProperty;
import com.xpn.xwiki.objects.NumberProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.BooleanClass;
import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.StaticListClass;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class DefaultObjectFilterFactory extends AbstractObjectFilterFactory
{

    private XWikiContext context;

    public DefaultObjectFilterFactory(XWikiContext context) {
        this.context = context;
    }

    @Override public ObjectFilter getFilter(JSONObject input)
    {
        if (!StringUtils.equalsIgnoreCase(input.optString(AbstractFilter.TYPE_KEY), FilterType.OBJECT.toString())) {
            throw new IllegalArgumentException(
                String.format("Given json does not have the [%s] key", AbstractFilter.TYPE_KEY));
        }

        return this.getObjectFilter(input);
    }

    private ObjectFilter getObjectFilter(JSONObject obj)
    {

        String className = obj.getString(AbstractFilter.CLASS_KEY);
        String propertyName =  obj.getString(ObjectFilter.PROPERTY_NAME_KEY);

        //XWikiContext context = xContextProvider.get();
        BaseClass baseClass = context.getBaseClass(AbstractObjectFilterFactory.getClassDocumentReference(className));
        PropertyInterface property = baseClass.get(propertyName);

        //Class clazz = property.getClass();
//  #elseif($propType == 'StaticListClass' || $propType == 'DBListClass' || $propType == 'DBTreeListClass')
        if (property instanceof NumberProperty) {
            return getNumberFilter((NumberProperty) property);
        }
        else if (property instanceof BooleanClass) {
            return new BooleanFilter();
        }
        else if (property instanceof StaticListClass || property instanceof DBListClass) {
            return new ListFilter();
        }
        else {
            return new StringFilter();
        }
    }

    private ObjectFilter getNumberFilter(NumberProperty property) {

        if (property instanceof IntegerProperty) {
            return new NumberFilter<Integer>();
        }
        else if (property instanceof LongProperty) {
            return new NumberFilter<Long>();
        }
        else if (property instanceof FloatProperty) {
            return new NumberFilter<Float>();
        }
        else if (property instanceof DoubleProperty) {
            return new NumberFilter<Double>();
        }
        else {
            throw new IllegalArgumentException(String.format("Unknown NumberProperty class [%s]", property.getClass()));
        }
    }
}
