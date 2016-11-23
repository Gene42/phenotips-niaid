/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal;

import org.phenotips.data.api.internal.filter.AbstractFilter;
import org.phenotips.data.api.internal.filter.BooleanFilter;
import org.phenotips.data.api.internal.filter.DateFilter;
import org.phenotips.data.api.internal.filter.LargeStringFilter;
import org.phenotips.data.api.internal.filter.ListFilter;
import org.phenotips.data.api.internal.filter.NumberFilter;
import org.phenotips.data.api.internal.filter.OrderFilter;
import org.phenotips.data.api.internal.filter.PageFilter;
import org.phenotips.data.api.internal.filter.ReferenceClassFilter;
import org.phenotips.data.api.internal.filter.StringFilter;
import org.phenotips.security.encryption.internal.EncryptedClass;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Provider;

import org.apache.commons.collections4.SetUtils;
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
 * The default filter factory implementation.
 *
 * @version $Id$
 */
@SuppressWarnings({"checkstyle:classdataabstractioncoupling", "checkstyle:classfanoutcomplexity"})
public class DefaultFilterFactory extends AbstractFilterFactory
{

    private static final Set<String> VALUE_PROPERTY_NAMES;

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultFilterFactory.class);

    private static final String DATE_INDICATOR = "date";

    private Provider<XWikiContext> contextProvider;

    static {
        Set<String> valueNames = new HashSet<>();
        valueNames.addAll(AbstractFilter.getValueParameterNames());
        valueNames.addAll(DateFilter.getValueParameterNames());
        valueNames.addAll(NumberFilter.getValueParameterNames());
        VALUE_PROPERTY_NAMES = SetUtils.unmodifiableSet(valueNames);
    }

    /**
     * Constructor.
     * @param contextProvider context provider
     */
    public DefaultFilterFactory(Provider<XWikiContext> contextProvider)
    {
        this.contextProvider = contextProvider;
    }

    @Override
    public AbstractFilter getFilter(JSONObject input)
    {
        if (!input.has(SpaceAndClass.CLASS_KEY) || !input.has(PropertyName.PROPERTY_NAME_KEY)) {
            return null;
        }

        String className = SearchUtils.getValue(input, SpaceAndClass.CLASS_KEY);
        String propertyName = input.getString(PropertyName.PROPERTY_NAME_KEY);

        XWikiContext context = this.contextProvider.get();

        BaseClass baseClass = null;

        try {
            baseClass = context.getWiki().getXClass(SearchUtils.getClassDocumentReference(className), context);
        } catch (XWikiException e) {
            LOGGER.warn("Error while getting filter xClass", e);
        }

        if (baseClass == null) {
            return null;
        }

        if (input.has(AbstractFilter.TYPE_KEY)) {
            return this.getFilterByType(input, propertyName, baseClass);
        } else {
            return this.getPropertyFilter(propertyName, baseClass);
        }
    }

    @Override
    public Set<String> getValueParameterNames()
    {
        return VALUE_PROPERTY_NAMES;
    }

    private AbstractFilter getFilterByType(JSONObject input, String propertyName, BaseClass baseClass)
    {

        AbstractFilter returnValue;

        String type = input.optString(AbstractFilter.TYPE_KEY);

        if (StringUtils.equals(OrderFilter.TYPE, type)) {
            AbstractFilter orderPropertyFilter = this.getPropertyFilter(propertyName, baseClass);
            returnValue = new OrderFilter(baseClass.get(propertyName), baseClass, orderPropertyFilter.getTableName());
        } else if (StringUtils.equals(ReferenceClassFilter.TYPE, type)) {
            returnValue = new ReferenceClassFilter(null, null);
        } else {
            throw new UnsupportedOperationException(String.format("Unsupported filter type [%1$s]", type));
        }

        return returnValue;
    }

    private AbstractFilter getPropertyFilter(String propertyName, BaseClass baseClass)
    {
        PropertyInterface property = baseClass.get(propertyName);

        AbstractFilter returnValue;

        if (property instanceof NumberClass) {
            returnValue = new NumberFilter(property, baseClass);

        } else if (isDateFilter(property, propertyName)) {
            returnValue = new DateFilter(property, baseClass);

        } else if (property instanceof EncryptedClass) {
            returnValue = getEncryptedFilter(propertyName, property, baseClass);

        } else if (property instanceof BooleanClass) {
            returnValue = new BooleanFilter(property, baseClass);

        } else if (property instanceof PageClass) {
            return new PageFilter(property, baseClass);

        } else if (isLargeStringFilter(property)) {
            returnValue = new LargeStringFilter(property, baseClass);

        } else if (isListFilter(property)) {
            returnValue = new ListFilter(property, baseClass);

        } else {
            returnValue = new StringFilter(property, baseClass);
        }

        return returnValue;
    }

    private static boolean isListFilter(PropertyInterface property)
    {
        // NOTE: maybe we can check instanceof ListClass
        return property instanceof StaticListClass || property instanceof DBListClass;
    }

    private static boolean isDateFilter(PropertyInterface property, String propertyName)
    {
        return (property instanceof DateClass)
            || (PropertyName.isDocProperty(propertyName)
            && StringUtils.endsWithIgnoreCase(propertyName, DATE_INDICATOR));
    }

    private static boolean isLargeStringFilter(PropertyInterface property)
    {
        return property instanceof TextAreaClass || property instanceof UsersClass || property instanceof GroupsClass;
    }

    private static AbstractFilter getEncryptedFilter(String propertyName, PropertyInterface property,
        BaseClass baseClass)
    {
        if (StringUtils.contains(propertyName, DATE_INDICATOR)) {
            return new DateFilter(property, baseClass);
        }

        return null;
    }
}
