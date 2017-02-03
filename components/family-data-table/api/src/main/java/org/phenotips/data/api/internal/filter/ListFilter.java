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

import org.apache.commons.collections4.CollectionUtils;
import org.json.JSONObject;

import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.ListClass;

/**
 * Filter dealing with list properties.
 *
 * @version $Id$
 */
public class ListFilter extends AbstractFilter<String>
{
    private boolean multiSelect;
    private boolean relationalStorage;

    /**
     * Constructor.
     * @param property PropertyInterface
     * @param baseClass BaseClass
     */
    public ListFilter(PropertyInterface property, BaseClass baseClass)
    {
        super(property, baseClass);

        this.multiSelect = ((ListClass) this.getProperty()).isMultiSelect();
        this.relationalStorage = ((ListClass) this.getProperty()).isRelationalStorage();

        if (this.multiSelect) {
            if (this.relationalStorage) {
                this.setTableName("DBStringListProperty");
            } else {
                this.setTableName("StringListProperty");
            }
        } else {
            this.setTableName("StringProperty");
        }
    }

    @Override
    public AbstractFilter init(JSONObject input, DocumentQuery parent, QueryExpression expressionParent)
    {
        super.init(input, parent, expressionParent);

        this.setValues(SearchUtils.getValues(input, VALUES_KEY));

        return this;
    }

    @Override
    public QueryBuffer addValueConditions(QueryBuffer where, List<Object> bindingValues)
    {
        if (CollectionUtils.isEmpty(this.getValues())) {
            return where;
        }

        this.startElement(where, bindingValues);

        where.saveAndReset(this.getJoinMode());

        String objPropName = this.getPropertyNameForQuery();

        if (this.multiSelect) {
            if (this.relationalStorage) {
                for (String value : this.getValues()) {
                    where.appendOperator().append(" ? in elements(").append(objPropName).append(".list) ");
                    bindingValues.add(value);
                }
            } else {
                where.append(" concat('|', concat(").append(objPropName);
                where.append(".textValue), '|')) like upper(?) ESCAPE '!' ");
                bindingValues.add("%|" + this.getValues().get(0).replaceAll("[\\[_%!]", "!$0") + "|%");
            }
        } else {
            for (String value : this.getValues()) {
                where.appendOperator().append(objPropName).append(".value=? ");
                bindingValues.add(value);
            }
        }

        where.load();

        return this.endElement(where);
    }
}
