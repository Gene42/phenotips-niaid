/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal;

import org.phenotips.data.api.internal.filter.AbstractFilter;
import org.phenotips.data.api.internal.filter.OrderFilter;
import org.phenotips.data.api.internal.filter.ReferenceClassFilter;
import org.phenotips.data.api.internal.filter.BooleanFilter;
import org.phenotips.data.api.internal.filter.DateFilter;
import org.phenotips.data.api.internal.filter.LargeStringFilter;
import org.phenotips.data.api.internal.filter.ListFilter;
import org.phenotips.data.api.internal.filter.NumberFilter;
import org.phenotips.data.api.internal.filter.PageFilter;
import org.phenotips.data.api.internal.filter.StringFilter;
import org.phenotips.security.encryption.internal.EncryptedClass;

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
import com.xpn.xwiki.objects.classes.PageClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.objects.classes.UsersClass;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class DefaultFilterFactory extends AbstractFilterFactory
{

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultFilterFactory.class);

    private Provider<XWikiContext> contextProvider;

    /**
     * Constructor.
     * @param contextProvider context provider
     */
    public DefaultFilterFactory(Provider<XWikiContext> contextProvider) {
        this.contextProvider = contextProvider;
    }

    @Override public AbstractFilter getFilter(JSONObject input)
    {
        return this.getObjectFilter(input);
    }

    private AbstractFilter getObjectFilter(JSONObject obj)
    {
        if (!obj.has(SpaceAndClass.CLASS_KEY) || !obj.has(PropertyName.PROPERTY_NAME_KEY)) {
            return null;
        }

        String className = obj.getString(SpaceAndClass.CLASS_KEY);
        String propertyName =  obj.getString(PropertyName.PROPERTY_NAME_KEY);

        XWikiContext context = this.contextProvider.get();

        BaseClass baseClass;

        try {
            baseClass = context.getWiki().getXClass(SearchUtils.getClassDocumentReference(className), context);
        } catch (XWikiException e) {
            LOGGER.warn("Error while getting filter xClass", e);
            return null;
        }

        if (baseClass == null) {
            return null;
        }

        PropertyInterface property = baseClass.get(propertyName);

        //property.getObject().getXClass(context).get(property.getName())

        AbstractFilter returnValue;

        //EncryptedProperty
        if (StringUtils.equals(OrderFilter.TYPE, obj.optString(AbstractFilter.TYPE_KEY))) {
            returnValue = new OrderFilter(property, baseClass);
        } else if (StringUtils.equals(ReferenceClassFilter.TYPE, obj.optString(AbstractFilter.TYPE_KEY))) {
            returnValue = new ReferenceClassFilter(null, null);
        } else if (property instanceof NumberClass) {
            returnValue = new NumberFilter(property, baseClass);
        } else if ((property instanceof DateClass)
             || (PropertyName.isDocProperty(propertyName)
                 && StringUtils.endsWithIgnoreCase(propertyName, "date")
                )
            ) {
            return new DateFilter(property, baseClass);
        } else if (property instanceof EncryptedClass) {
            returnValue = this.getEncryptedFilter(propertyName, property, baseClass);
        }
        else if (property instanceof BooleanClass) {
            returnValue =  new BooleanFilter(property, baseClass);
        }
        else if (property instanceof PageClass) {
            return new PageFilter(property, baseClass);
        }
        else if (property instanceof TextAreaClass || property instanceof UsersClass || property instanceof GroupsClass) {
            returnValue = new LargeStringFilter(property, baseClass);
        } else if (property instanceof StaticListClass || property instanceof DBListClass) {
            // NOTE: maybe we can check instanceof ListClass
            returnValue =  new ListFilter(property, baseClass);
        }  else {
            returnValue =  new StringFilter(property, baseClass);
        }

        return returnValue;
    }

    private AbstractFilter getEncryptedFilter(String propertyName, PropertyInterface property,
        BaseClass baseClass)
    {
        if (StringUtils.contains(propertyName, "date")) {
            return new DateFilter(property, baseClass);
        }

        return null;
    }

}
