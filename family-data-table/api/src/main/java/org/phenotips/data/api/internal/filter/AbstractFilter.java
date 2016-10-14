/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal.filter;

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

    protected String className;

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
     * @param obj JSONObject to use for populating this filter
     * @param level the level of this json tree structure
     * @param filterFactory used to create child filters
     * @throws org.json.JSONException if any error occurs while retrieving keys
     * @throws IllegalArgumentException if any inputs are not valid
     * @return this object
     */
    public AbstractFilter populate(JSONObject obj, int level, AbstractFilterFactory filterFactory)
    {
        if (!obj.has(CLASS_KEY)) {
            throw new IllegalArgumentException(String.format("[%s] key not present", CLASS_KEY));
        }
        this.className = obj.getString(CLASS_KEY);
        this.level = level;
        return this;
    }

    public abstract StringBuilder hql(StringBuilder builder, int level, String parentDoc);
    public abstract StringBuilder selectHql(StringBuilder builder, int level, String parentDoc);
    public abstract StringBuilder fromHql(StringBuilder builder, int level, String parentDoc);
    public abstract StringBuilder whereHql(StringBuilder builder, int level, String parentDoc);

   /* public boolean hasSingleNonNullValue() {
        return this.values.size() == 1 && this.values.get(0) != null;
    }*/
}
