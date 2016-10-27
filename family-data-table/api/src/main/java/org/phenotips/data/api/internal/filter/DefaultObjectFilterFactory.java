/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal.filter;

import org.phenotips.data.api.internal.DocumentUtils;
import org.phenotips.data.api.internal.SpaceAndClass;
import org.phenotips.data.api.internal.filter.property.BooleanFilter;
import org.phenotips.data.api.internal.filter.property.DateFilter;
import org.phenotips.data.api.internal.filter.property.LargeStringFilter;
import org.phenotips.data.api.internal.filter.property.ListFilter;
import org.phenotips.data.api.internal.filter.property.NumberFilter;
import org.phenotips.data.api.internal.filter.property.StringFilter;

import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.BooleanClass;
import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.DateClass;
import com.xpn.xwiki.objects.classes.GroupsClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.objects.classes.UsersClass;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class DefaultObjectFilterFactory extends AbstractObjectFilterFactory
{

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultObjectFilterFactory.class);

    private Provider<XWikiContext> contextProvider;

    /**
     * Constructor.
     * @param contextProvider context provider
     */
    public DefaultObjectFilterFactory(Provider<XWikiContext> contextProvider) {
        this.contextProvider = contextProvider;
    }

    @Override public AbstractPropertyFilter getFilter(JSONObject input)
    {

        return this.getObjectFilter(input);
    }

    @Override public AbstractPropertyFilter getBindingFilter(JSONObject obj)
    {
        // TODO: get property, baseClass
        return new BindingFilter(null, null);
    }

    private AbstractPropertyFilter getObjectFilter(JSONObject obj)
    {
        if (!obj.has(SpaceAndClass.CLASS_KEY) || !obj.has(PropertyName.PROPERTY_NAME_KEY)) {
            return null;
        }

        String className = obj.getString(SpaceAndClass.CLASS_KEY);
        String propertyName =  obj.getString(PropertyName.PROPERTY_NAME_KEY);

        XWikiContext context = this.contextProvider.get();

        BaseClass baseClass;

        try {
            baseClass = context.getWiki().getXClass(DocumentUtils.getClassDocumentReference(className), context);
        } catch (XWikiException e) {
            LOGGER.warn("Error while getting filter xClass", e);
            return null;
        }

        if (baseClass == null) {
            return null;
        }

        PropertyInterface property = baseClass.get(propertyName);

        AbstractPropertyFilter returnValue;

        if (property instanceof NumberClass) {
            returnValue = new NumberFilter(property, baseClass);
        } else if ((property instanceof DateClass)
             || (PropertyName.isDocProperty(propertyName)
                 && StringUtils.endsWithIgnoreCase(propertyName, "date")
                )
            ) {
            return new DateFilter(property, baseClass);
        } else if (property instanceof BooleanClass) {
             returnValue =  new BooleanFilter(property, baseClass);
        } else if (property instanceof TextAreaClass || property instanceof UsersClass || property instanceof GroupsClass) {
             returnValue = new LargeStringFilter(property, baseClass);
        } else if (property instanceof StaticListClass || property instanceof DBListClass) {
            // NOTE: maybe we can check instanceof ListClass
             returnValue =  new ListFilter(property, baseClass);
        } else {
             returnValue =  new StringFilter(property, baseClass);
        }

        return returnValue;
    }

}
