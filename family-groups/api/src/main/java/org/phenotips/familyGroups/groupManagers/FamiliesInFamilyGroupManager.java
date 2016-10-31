/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.familyGroups.groupManagers;

import org.phenotips.entities.PrimaryEntityGroupManager;
import org.phenotips.entities.internal.AbstractInternalPrimaryEntityGroupManager;
import org.phenotips.familyGroups.Family;
import org.phenotips.familyGroups.FamilyGroup;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.util.DefaultParameterizedType;

import java.lang.reflect.ParameterizedType;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Group manager for Families in Family Groups, implemented using the default Entities API implementation.
 *
 * @version $Id$
 */
@Component
@Named("FamilyGroup:Family")
@Singleton
public class FamiliesInFamilyGroupManager
    extends AbstractInternalPrimaryEntityGroupManager<FamilyGroup, Family>
    implements PrimaryEntityGroupManager<FamilyGroup, Family>
{
    /** Type instance for lookup. */
    public static final ParameterizedType TYPE = new DefaultParameterizedType(null, PrimaryEntityGroupManager.class,
        FamilyGroup.class, Family.class);

    /**
     * Public constructor.
     */
    public FamiliesInFamilyGroupManager()
    {
        super(FamilyGroup.CLASS_REFERENCE, Family.CLASS_REFERENCE);
    }
}
