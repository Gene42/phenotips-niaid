package org.phenotips.data.api.internal.filter;

import org.phenotips.data.api.internal.DocumentSearchUtils;
import org.phenotips.data.api.internal.filter.property.BooleanFilter;
import org.phenotips.data.api.internal.filter.property.ListFilter;
import org.phenotips.data.api.internal.filter.property.NumberFilter;
import org.phenotips.data.api.internal.filter.property.StringFilter;

import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
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


    //private XWikiContext context;

    private Provider<XWikiContext> contextProvider;

    public DefaultObjectFilterFactory(Provider<XWikiContext> contextProvider) {
        this.contextProvider = contextProvider;
    }

    @Override public ObjectFilter getFilter(JSONObject input)
    {
        if (!StringUtils.equalsIgnoreCase(input.optString(AbstractFilter.TYPE_KEY), AbstractFilter.Type.OBJECT.toString())) {
            throw new IllegalArgumentException(
                String.format("Given json does not have the [%s] key", AbstractFilter.TYPE_KEY));
        }

        return this.getObjectFilter(input);
    }

    private ObjectFilter getObjectFilter(JSONObject obj)
    {

        String className = obj.getString(AbstractFilter.CLASS_KEY);
        String propertyName =  obj.getString(ObjectFilter.PROPERTY_NAME_KEY);

        System.out.println("className=" + className);

        XWikiContext context = contextProvider.get();

        /*try {
            XWikiDocument doc = context.getWiki().getDocument(AbstractObjectFilterFactory.getClassDocumentReference(className), context);
            doc.getXClass();

        } catch (XWikiException e) {
            e.printStackTrace();
        }
*/
        //XWikiContext context = xContextProvider.get();
        //context.getC
        BaseClass baseClass = context.getBaseClass(DocumentSearchUtils.getClassDocumentReference(className));

        if (baseClass == null) {
            try {
                XWikiDocument doc = context.getWiki().getDocument(DocumentSearchUtils.getClassDocumentReference(className), context);
                baseClass = doc.getXClass();

            } catch (XWikiException e) {
                e.printStackTrace();
            }
        }

        if (baseClass == null) {
            return null;
        }

        PropertyInterface property = baseClass.get(propertyName);

        //Class clazz = property.getClass();
//  #elseif($propType == 'StaticListClass' || $propType == 'DBListClass' || $propType == 'DBTreeListClass')
        if (property instanceof NumberProperty) {
            //return getNumberFilter((NumberProperty) property);
            return new NumberFilter(property, baseClass);
        }
        else if (property instanceof BooleanClass) {
            return new BooleanFilter(property, baseClass);
        }
        else if (property instanceof StaticListClass || property instanceof DBListClass) {
            // TODO: maybe instanceof ListClass
            return new ListFilter(property, baseClass);
        }
        else {
            return new StringFilter(property, baseClass);
        }
    }

    /*private ObjectFilter getNumberFilter(NumberProperty property) {


    }*/
}
