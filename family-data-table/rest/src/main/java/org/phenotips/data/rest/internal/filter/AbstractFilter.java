/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.rest.internal.filter;

import org.xwiki.query.Query;

import org.json.JSONObject;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public abstract class AbstractFilter<T>
{
    protected String name;

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

    public abstract StringBuilder hql(StringBuilder builder, int level, String parentDoc);
    public abstract StringBuilder selectHql(StringBuilder builder, int level, String parentDoc);
    public abstract StringBuilder fromHql(StringBuilder builder, int level, String parentDoc);
    public abstract StringBuilder whereHql(StringBuilder builder, int level, String parentDoc);

   /* public boolean hasSingleNonNullValue() {
        return this.values.size() == 1 && this.values.get(0) != null;
    }*/
}
