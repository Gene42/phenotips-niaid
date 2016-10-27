/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.familyGroups.script;

import org.phenotips.entities.PrimaryEntityGroupManager;
import org.phenotips.entities.PrimaryEntityManager;
import org.phenotips.familyGroups.Family;
import org.phenotips.familyGroups.FamilyGroup;
import org.phenotips.familyGroups.FamilyGroupPedigreeExporter;
import org.phenotips.studies.family.FamilyTools;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Utilities for working with Family Groups from scripts.
 *
 * @version $Id$
 */
@Component
@Named("familygroups")
@Singleton
public class FamilyGroupsScriptService implements ScriptService
{
    @Inject
    @Named("FamilyGroup")
    private PrimaryEntityManager familyGroupManager;

    @Inject
    @Named("Family")
    private PrimaryEntityManager familyManager;

    @Inject
    private FamilyTools familyTools;

    @Inject
    @Named("FamilyGroup:Family")
    private PrimaryEntityGroupManager<FamilyGroup, Family> familiesInFamilyGroupManager;

    @Inject
    private FamilyGroupPedigreeExporter familyGroupPedigreeExporter;

    /**
     * Returns the set of IDs for Families in the given Family Group, specified by ID.
     *
     * @param id the Family Group ID
     * @return the set of IDs for Families in the given Family Group.
     */
    public Collection<FamilyGroup> getFamilyGroupsForFamily(String id)
    {
        Family family = (Family) familyManager.get(id);
        return familiesInFamilyGroupManager.getGroupsForMember(family);
    }

    public String exportFamilyGroupAsPED(String familyGroupId, List<String> disorders)
    {
        return familyGroupPedigreeExporter.exportFamilyGroupAsPED(familyGroupId, disorders);
    }
}
