/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal.filter;

import org.phenotips.data.api.internal.DocumentQuery;

import org.json.JSONObject;

import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class LargeStringFilter extends StringFilter
{
    /**
     * Constructor.
     *
     * @param property PropertyInterface
     * @param baseClass BaseClass
     */
    public LargeStringFilter(PropertyInterface property, BaseClass baseClass)
    {
        super(property, baseClass);
        super.setTableName("LargeStringProperty");
    }

    @Override public AbstractFilter init(JSONObject input, DocumentQuery parent)
    {
        super.init(input, parent);
        super.setMatch(StringFilter.MATCH_SUBSTRING);
        return this;
    }
}