/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.familyGroups.internal;

import org.phenotips.entities.internal.AbstractPrimaryEntity;
import org.phenotips.familyGroups.Family;
import org.phenotips.familyGroups.FamilyGroup;

import org.xwiki.model.reference.EntityReference;

import java.util.Collection;

import org.json.JSONObject;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Family Group implementation using the default Entities API implementation.
 *
 * @version $Id$
 */
public class DefaultFamilyGroup extends AbstractPrimaryEntity implements FamilyGroup
{
    /**
     *  Basic constructor.
     * @param doc document
     */
    public DefaultFamilyGroup(XWikiDocument doc)
    {
        super(doc);
    }

    @Override public EntityReference getType()
    {
        return FamilyGroup.CLASS_REFERENCE;
    }

    @Override public void updateFromJSON(JSONObject jsonObject)
    {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override public EntityReference getMemberType()
    {
        return null;
    }

    @Override public Collection<Family> getMembers()
    {
        return null;
    }

    @Override public Collection<Family> getMembersOfType(EntityReference entityReference)
    {
        return null;
    }

    @Override public boolean addMember(Family family)
    {
        return false;
    }

    @Override public boolean removeMember(Family family)
    {
        return false;
    }
}
