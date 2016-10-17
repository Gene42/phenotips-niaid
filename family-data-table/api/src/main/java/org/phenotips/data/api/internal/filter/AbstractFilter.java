/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal.filter;

import org.phenotips.Constants;

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
    public AbstractFilter populate(JSONObject input, int level, AbstractObjectFilterFactory filterFactory)
    {
        if (!input.has(CLASS_KEY)) {
            throw new IllegalArgumentException(String.format("[%s] key not present", CLASS_KEY));
        }
        this.spaceAndClassName = input.getString(CLASS_KEY);
        String [] tokens = getSpaceAndClass(this.spaceAndClassName);
        this.level = level;
        this.spaceName = tokens[0];
        this.className = tokens[1];

        return this;
    }

    public abstract StringBuilder hql(StringBuilder hql, List<String> bindingValues, int level, String baseObj, String parentDoc);
    public abstract StringBuilder selectHql(StringBuilder select, List<String> bindingValues, int level, String baseObj, String parentDoc);
    public abstract StringBuilder fromHql(StringBuilder from, List<String> bindingValues, int level, String baseObj, String parentDoc);
    public abstract StringBuilder whereHql(StringBuilder where, List<String> bindingValues, int level, String baseObj, String parentDoc);

   /* public boolean hasSingleNonNullValue() {
        return this.values.size() == 1 && this.values.get(0) != null;
    }*/

   public static FilterType getFilterType(JSONObject input)
   {
       if (!input.has(AbstractFilter.TYPE_KEY)) {
           throw new IllegalArgumentException(
               String.format("Given json does not have the [%s] key", AbstractFilter.TYPE_KEY));
       }

       return FilterType.valueOf(StringUtils.upperCase(input.getString(AbstractFilter.TYPE_KEY)));
   }

   public static String [] getSpaceAndClass(String classAndSpace)
   {
       if (StringUtils.isBlank(classAndSpace)) {
           throw new IllegalArgumentException("class provided is null/empty");
       }

       String [] tokens = StringUtils.split(classAndSpace, ".");

       if (tokens.length == 2) {
           return tokens;
       }
       else {
           return new String [] { Constants.CODE_SPACE, tokens[0] };
       }
   }

    public static String getSafeAlias(String alias) {
        return StringUtils.replace(alias, "[^a-zA-Z0-9_.]", "");
        //tableAlias.replaceAll('[^a-zA-Z0-9_.]', '')
    }
}
