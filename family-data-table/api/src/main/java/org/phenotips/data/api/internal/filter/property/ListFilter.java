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

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.ListClass;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class ListFilter extends AbstractPropertyFilter
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

        return this;
    }
}
