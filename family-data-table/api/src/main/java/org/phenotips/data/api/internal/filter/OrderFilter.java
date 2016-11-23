/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal.filter;

import org.phenotips.data.api.internal.DocumentQuery;
import org.phenotips.data.api.internal.QueryBuffer;
import org.phenotips.data.api.internal.QueryExpression;
import org.phenotips.data.api.internal.SearchUtils;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * This filter handles the order by clause of the hql query.
 *
 * @version $Id$
 */
public class OrderFilter extends AbstractFilter<String>
{

    /** The type of this filter. */
    public static final String TYPE = "order_filter";

    private String orderDir;

    /**
     * Constructor.
     * @param property PropertyInterface
     * @param baseClass BaseClass
     * @param tableName the table name of this filter (The property type: StringProperty, IntegerProperty etc.)
     */
    public OrderFilter(PropertyInterface property, BaseClass baseClass, String tableName)
    {
        super(property, baseClass, tableName);
    }

    @Override
    public AbstractFilter init(JSONObject input, DocumentQuery parent, QueryExpression expressionParent)
    {
        super.init(input, parent, expressionParent);

        this.orderDir = SearchUtils.getValue(input, VALUES_KEY, "desc");
        if (!StringUtils.equals(this.orderDir, "asc") && !StringUtils.equals(this.orderDir, "desc")) {
            this.orderDir = "desc";
        }

        return this;
    }

    @Override
    public QueryBuffer addValueConditions(QueryBuffer where, List<Object> bindingValues)
    {
        String objPropName = this.getPropertyValueNameForQuery();
        where.append(" order by ").append(objPropName).append(" ").append(this.orderDir).append(" ");
        return where;
    }

    @Override
    public boolean isValid()
    {
        return StringUtils.isNotBlank(this.orderDir) && StringUtils.isNotBlank(this.getTableName());
    }
}
