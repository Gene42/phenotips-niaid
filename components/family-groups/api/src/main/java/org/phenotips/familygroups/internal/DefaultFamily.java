/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.familygroups.internal;

import org.phenotips.entities.internal.AbstractPrimaryEntity;
import org.phenotips.familygroups.Family;

import org.xwiki.model.reference.EntityReference;

import org.json.JSONObject;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Family implementation using the default Entities API implementation. This is a temporary solution until
 * Families use the Entities API in PhenoTips core. There may be inconsistencies between the behaviour of this module
 * and the behaviour of the Family Studies module from PhenoTips core.
 *
 * @version $Id$
 */
public class DefaultFamily extends AbstractPrimaryEntity implements Family
{
    /**
     * Constructor.
     *
     * @param doc the XWikiDocument representing this family
     */
    public DefaultFamily(XWikiDocument doc)
    {
        super(doc);
    }

    /**
     * Returns the EntityReference to this family object.
     *
     * @return a EntityReference
     */
    public EntityReference getType()
    {
        return Family.CLASS_REFERENCE;
    }

    @Override
    public void updateFromJSON(JSONObject jsonObject)
    {
        throw new UnsupportedOperationException("Not implemented.");
    }
}
