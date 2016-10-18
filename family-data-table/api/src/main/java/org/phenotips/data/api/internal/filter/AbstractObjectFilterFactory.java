package org.phenotips.data.api.internal.filter;

import org.phenotips.Constants;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import org.json.JSONObject;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public abstract class AbstractObjectFilterFactory
{
    public abstract ObjectFilter getFilter(JSONObject obj);


}
