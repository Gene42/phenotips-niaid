/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.familygroups.script;

import org.phenotips.entities.PrimaryEntityGroupManager;
import org.phenotips.entities.PrimaryEntityManager;
import org.phenotips.familygroups.Family;
import org.phenotips.familygroups.FamilyGroup;
import org.phenotips.familygroups.FamilyGroupPedigreeExporter;
import org.phenotips.security.authorization.AuthorizationService;
import org.phenotips.studies.family.FamilyTools;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.ArrayList;
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

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private UserManager userManager;

    @Inject
    @Named("current")
    private EntityReferenceResolver<EntityReference> currentResolver;

    /**
     * Returns the set of Family Groups to which the family with the given ID belongs.
     *
     * @param id the Family ID
     * @return the set of Family Groups to which the family with the given ID belongs.
     */
    public Collection<FamilyGroup> getFamilyGroupsForFamily(String id)
    {
        Family family = (Family) this.familyManager.get(id);
        return this.familiesInFamilyGroupManager.getGroupsForMember(family);
    }

    /**
     * Returns a Family Group using its ID.
     *
     * @param familyGroupId the Family Group's ID.
     * @return the Family Group.
     */
    public FamilyGroup getFamilyGroup(String familyGroupId)
    {
        return (FamilyGroup) this.familyGroupManager.get(familyGroupId);
    }

    /**
     * Returns the set of IDs for Families in the given Family Group, specified by ID.
     *
     * @param familyGroupId the Family Group ID
     * @return the set of IDs for Families in the given Family Group.
     */
    public List<String> getFamilyIdsInFamilyGroup(String familyGroupId)
    {
        List<String> result = new ArrayList<>();

        FamilyGroup familyGroup = (FamilyGroup) this.familyGroupManager.get(familyGroupId);
        if (familyGroup == null) {
            return result;
        }
        Collection<Family> families = this.familiesInFamilyGroupManager.getMembers(familyGroup);
        for (Family family : families) {
            result.add(family.getId());
        }

        return result;
    }

    /**
     * Creates a new FamilyGroup for the current user.
     *
     * @return a FamilyGroup or null if the current user does not have permission to perform this action.
     */
    public FamilyGroup createFamilyGroup()
    {
        User creator = this.userManager.getCurrentUser();
        if (this.authorizationService.hasAccess(creator, Right.EDIT,
            this.currentResolver.resolve(FamilyGroup.DEFAULT_DATA_SPACE, EntityType.SPACE))) {
            return (FamilyGroup) this.familyGroupManager.create();
        } else {
            return null;
        }
    }
}
