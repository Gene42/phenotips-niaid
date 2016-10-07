/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.rest.internal.filter;

import org.xwiki.query.Query;

import java.util.Map;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public abstract class AbstractFilter
{

    protected String name;

    public AbstractFilter parse(String name, Map<String, String> secondaryValues) {
        this.name = name;
        return this;
    }

    public abstract StringBuilder append(StringBuilder builder, Query query);
}
