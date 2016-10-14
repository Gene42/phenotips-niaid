package org.phenotips.data.api.internal.filter;

import org.phenotips.Constants;
import org.phenotips.data.api.internal.filter.property.BooleanFilter;
import org.phenotips.data.api.internal.filter.property.ListFilter;
import org.phenotips.data.api.internal.filter.property.NumberFilter;

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
import com.xpn.xwiki.objects.classes.DBTreeListClass;
import com.xpn.xwiki.objects.classes.StaticListClass;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class DefaultFilterFactory extends AbstractFilterFactory
{

    private Provider<XWikiContext> xContextProvider;

    @Override public AbstractFilter getFilter(JSONObject obj)
    {

        if (!obj.has(AbstractFilter.TYPE_KEY)) {
            throw new IllegalArgumentException(
                String.format("Given json does not have the [%s] key", AbstractFilter.TYPE_KEY));
        }

        switch (obj.getString(AbstractFilter.TYPE_KEY)) {
            case "document":
                return new EntityFilter();
            case "object":
                return this.getObjectFilter(obj);
            default:
                throw new IllegalArgumentException(String.format("Filter %s provided [%s] not supported",
                        AbstractFilter.TYPE_KEY, obj.getString(AbstractFilter.TYPE_KEY)));

        }
    }

    private ObjectFilter getObjectFilter(JSONObject obj)
    {

        String className = obj.getString(AbstractFilter.CLASS_KEY);
        String propertyName =  obj.getString(ObjectFilter.PROPERTY_NAME_KEY);

        XWikiContext context = xContextProvider.get();
        BaseClass baseClass = context.getBaseClass(getClassDocumentReference(className));
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

        return null;
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

    private static DocumentReference getClassDocumentReference(String classAndSpace) {

        if (StringUtils.isBlank(classAndSpace)) {
            throw new IllegalArgumentException("class provided is null/empty");
        }

        String [] tokens = StringUtils.split(classAndSpace, ".");

        EntityReference ref;

        if (tokens.length == 2) {
            // Example: PhenoTips.GeneClass
            ref = new EntityReference(tokens[1], EntityType.DOCUMENT, new EntityReference(tokens[0], EntityType.SPACE));
        }
        else {
            ref = new EntityReference(classAndSpace, EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);
        }

        return new DocumentReference(ref);
    }
}
