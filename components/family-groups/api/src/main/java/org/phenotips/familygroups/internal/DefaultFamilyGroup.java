/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.familygroups.internal;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.entities.PrimaryEntityGroupManager;
import org.phenotips.entities.internal.AbstractPrimaryEntity;
import org.phenotips.familygroups.Family;
import org.phenotips.familygroups.FamilyGroup;
import org.phenotips.familygroups.groupmanagers.FamiliesInFamilyGroupManager;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.EntityReference;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONObject;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Family Group implementation using the default Entities API implementation.
 *
 * @version $Id$
 */
public class DefaultFamilyGroup extends AbstractPrimaryEntity implements FamilyGroup
{
    /** Name of the XProperty holding the name in the Family Group XClass. */
    public static final String NAME_XPROPERTY_KEY = "name";

    /**
     * Basic constructor.
     *
     * @param doc the XWikiDocument representing this family
     */
    public DefaultFamilyGroup(XWikiDocument doc)
    {
        super(doc);
    }

    @Override
    public EntityReference getType()
    {
        return FamilyGroup.CLASS_REFERENCE;
    }

    @Override
    public String getName()
    {
        String result = null;
        BaseObject obj = this.document.getXObject(getType());
        BaseClass cls = obj.getXClass(getXContext());
        if (cls.getField(NAME_XPROPERTY_KEY) != null) {
            result = obj.getLargeStringValue(NAME_XPROPERTY_KEY);
        }
        return result;
    }

    @Override
    public Set<String> getFamilyIds()
    {
        Set<String> familyIds = new HashSet<>();
        Collection<Family> families = this.getFamiliesInFamilyGroupManager().getMembers(this);

        for (Family family : families) {
            familyIds.add(family.getId());
        }

        return familyIds;
    }

    /**
     * Returns the Families in Family Group manager instance.
     *
     * @return the Families in Family Group manager instance.
     */
    private PrimaryEntityGroupManager<FamilyGroup, Family> getFamiliesInFamilyGroupManager()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager().getInstance(
                FamiliesInFamilyGroupManager.TYPE, "FamilyGroup:Family");
        } catch (ComponentLookupException e) {
            this.logger.error("Unexpected exception while getting FamiliesInFamilyGroupManager: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public JSONObject toJSON()
    {
        JSONObject result = new JSONObject();
        result.put("id", this.getId());
        result.put("name", this.getName());
        result.put("description", this.getDescription());
        result.put("familyIds", this.getFamilyIds());

        return result;
    }

    @Override
    public void updateFromJSON(JSONObject jsonObject)
    {
        throw new UnsupportedOperationException("Not implemented.");
    }
}
