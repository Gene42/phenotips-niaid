/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal.filter;

import org.phenotips.data.api.internal.QueryBuffer;

import java.util.List;

import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class ReferenceClassFilter extends AbstractFilter<String>
{

    public static final String TYPE = "reference";

    /**
     * Constructor.
     * @param property PropertyInterface
     * @param baseClass BaseClass
     */
    public ReferenceClassFilter(PropertyInterface property, BaseClass baseClass)
    {
        super(property, baseClass, "StringProperty");
    }

    @Override public QueryBuffer addValueConditions(QueryBuffer where, List<Object> bindingValues)
    {
        //super.addValueConditions(where, bindingValues);
        // TODO: see if this needs to be added in bindProperty method

        String objPropName = super.getPropertyNameForQuery() + ".value";
        String docName = super.getParent().getParent().getDocName();

        where.appendOperator();
        where.append(" ").append(objPropName).append("=concat('xwiki:',").append(docName).append(".fullName) ");

        return where;
    }

    @Override public boolean isValid()
    {
        return true;
    }

    @Override public boolean validatesQuery()
    {
        return false;
    }
}
