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
import org.phenotips.studies.family.FamilyTools;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;

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
    private PrimaryEntityManager<FamilyGroup> familyGroupManager;

    @Inject
    private FamilyTools familyTools;

    @Inject
    @Named("FamilyGroup:Family")
    private PrimaryEntityGroupManager<FamilyGroup, Family> familiesInFamilyGroupManager;

    public PrimaryEntityManager<FamilyGroup> getFamilyGroupManager()
    {
        return familyGroupManager;
    }

    public PrimaryEntityGroupManager<FamilyGroup, Family> getFamiliesInFamilyGroupManager()
    {
        return familiesInFamilyGroupManager;
    }
}
