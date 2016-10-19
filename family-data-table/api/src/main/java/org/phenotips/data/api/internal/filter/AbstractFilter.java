/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal.filter;

import org.phenotips.data.api.internal.DocumentSearchUtils;

import org.xwiki.model.EntityType;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public abstract class AbstractFilter
{
    public static final String CLASS_KEY = "class";

    public static final String TYPE_KEY = "type";

    public static final String VALUES_KEY = "values";

    protected String spaceAndClassName;

    protected String className;

    protected String spaceName;

    protected String tableName;

    protected int level;
   /* protected String xWikiClass;

    protected String dependsOn;

    protected JoinMode joinMode;

    protected List<T> values = Collections.unmodifiableList((List<T>) new LinkedList<T>());

    protected T min;

    protected T max;*/

 /*   public AbstractFilter(String name, Map<String, String> secondaryValues) {
        this.name = name;
    }
*/

    /**
     *
     * @param input JSONObject to use for populating this filter
     * @param level the level of this json tree structure
     * @param filterFactory used to create child filters
     * @throws org.json.JSONException if any error occurs while retrieving keys
     * @throws IllegalArgumentException if any inputs are not valid
     * @return this object
     */
    public AbstractFilter populate(JSONObject input, int level)
    {
        if (!input.has(CLASS_KEY)) {
            throw new IllegalArgumentException(String.format("[%s] key not present", CLASS_KEY));
        }
        this.spaceAndClassName = input.getString(CLASS_KEY);
        String [] tokens = DocumentSearchUtils.getSpaceAndClass(this.spaceAndClassName);
        this.level = level;
        this.spaceName = tokens[0];
        this.className = tokens[1];

        return this;
    }

    public StringBuilder hql(StringBuilder hql, List<Object> bindingValues, int level, String baseObj, String parentDoc) {
        return hql;
    }
    public StringBuilder selectHql(StringBuilder select, List<Object> bindingValues, int level, String baseObj, String parentDoc) {
        return select;
    }
    public abstract StringBuilder fromHql(StringBuilder from, List<Object> bindingValues, int level, String baseObj, String parentDoc);
    public abstract StringBuilder whereHql(StringBuilder where, List<Object> bindingValues, int level, String baseObj, String parentDoc);

   /* public boolean hasSingleNonNullValue() {
        return this.values.size() == 1 && this.values.get(0) != null;
    }*/

   public static EntityType getFilterType(JSONObject input)
   {
       if (!input.has(AbstractFilter.TYPE_KEY)) {
           throw new IllegalArgumentException(
               String.format("Given json does not have the [%s] key", AbstractFilter.TYPE_KEY));
       }

       return EntityType.valueOf(StringUtils.upperCase(input.getString(AbstractFilter.TYPE_KEY)));
   }
}
