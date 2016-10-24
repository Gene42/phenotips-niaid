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
public abstract class AbstractObjectFilterFactory
{
    public abstract AbstractPropertyFilter getFilter(JSONObject obj);

    public abstract AbstractPropertyFilter getBindingFilter(JSONObject obj);

}
