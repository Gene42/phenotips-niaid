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
 * Filter dealing with reference class objects (which map objects to other objects).
 *
 * @version $Id$
 */
public class ReferenceClassFilter extends AbstractFilter<String>
{

    /** The type of this filter. */
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

    @Override
    public QueryBuffer addValueConditions(QueryBuffer where, List<Object> bindingValues)
    {
        // TODO: see if this needs to be added in bindProperty method
        this.startElement(where, bindingValues);

        String objPropName = super.getPropertyNameForQuery() + ".value";
        String docName = super.getParent().getParent().getDocName();

        where.append(" ").append(objPropName).append("=concat('xwiki:',").append(docName).append(".fullName) ");

        return this.endElement(where);
    }

    @Override
    public boolean isValid()
    {
        return true;
    }

    @Override
    public boolean validatesQuery()
    {
        return false;
    }
}
