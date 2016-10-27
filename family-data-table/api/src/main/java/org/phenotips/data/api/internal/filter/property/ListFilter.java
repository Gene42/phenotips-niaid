/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal.filter.property;

import org.phenotips.data.api.internal.filter.AbstractPropertyFilter;
import org.phenotips.data.api.internal.filter.DocumentQuery;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.ListClass;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class ListFilter extends AbstractPropertyFilter<String>
{
    /** Filter param key. */
    public static final String JOIN_MODE_KEY = "join_mode";

    /** Value for JOIN_MODE key. */
    public static final String JOIN_MODE_VALUE_AND = "and";

    /** Value for JOIN_MODE key. */
    public static final String JOIN_MODE_VALUE_OR = "or";

    /** Value for JOIN_MODE key. */
    public static final String JOIN_MODE_DEFAULT_VALUE = JOIN_MODE_VALUE_AND;

    private boolean multiSelect;
    private boolean relationalStorage;
    private String joinMode;

    /**
     * Constructor.
     * @param property PropertyInterface
     * @param baseClass BaseClass
     */
    public ListFilter(PropertyInterface property, BaseClass baseClass)
    {
        super(property, baseClass);
    }

    @Override public AbstractPropertyFilter populate(JSONObject input, DocumentQuery parent)
    {
        super.populate(input, parent);

        this.multiSelect = ((ListClass) super.getProperty()).isMultiSelect();
        this.relationalStorage = ((ListClass) super.getProperty()).isRelationalStorage();
        this.joinMode = StringUtils.lowerCase(input.optString(JOIN_MODE_KEY));

        if (!StringUtils.equals(this.joinMode, JOIN_MODE_VALUE_AND)
            && !StringUtils.equals(this.joinMode, JOIN_MODE_VALUE_OR)) {
            this.joinMode = JOIN_MODE_DEFAULT_VALUE;
        }

        if (this.multiSelect) {
            if (this.relationalStorage) {
                super.setTableName("DBStringListProperty");
            } else {
                super.setTableName("StringListProperty");
            }
        } else {
            super.setTableName("StringProperty");
        }

        super.setValues(AbstractPropertyFilter.getValues(input, VALUES_KEY));

        return this;
    }

    @Override public StringBuilder whereHql(StringBuilder where, List<Object> bindingValues)
    {
        if (CollectionUtils.isEmpty(super.getValues())) {
            return where;
        }

        super.whereHql(where, bindingValues);

        where.append(" (");

        String objPropName = super.getPropertyNameForQuery();

        if (this.multiSelect) {
            if (this.relationalStorage) {
                for (int i = 0, len = super.getValues().size(); i < len; i++) {
                    super.appendQueryOperator(where, this.joinMode, i);
                    where.append(" ? in elements(").append(objPropName).append(".list) ");
                    bindingValues.add(super.getValues().get(i));
                }
            } else {
                where.append(" concat('|', concat(").append(objPropName);
                where.append(".textValue), '|')) like upper(?) ESCAPE '!' ");
                bindingValues.add("%|" + super.getValues().get(0).replaceAll("[\\[_%!]", "!$0") + "|%");
            }
        } else {
            for (int i = 0, len = super.getValues().size(); i < len; i++) {
                super.appendQueryOperator(where, this.joinMode, i);
                where.append(objPropName).append("=? ");
                bindingValues.add(super.getValues().get(0));
            }
        }

        where.append(" and )");

        return where;
    }
}
