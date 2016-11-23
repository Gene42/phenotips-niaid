/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.familyGroups.internal;

import org.phenotips.entities.PrimaryEntityManager;
import org.phenotips.entities.internal.AbstractPrimaryEntityManager;
import org.phenotips.familyGroups.FamilyGroup;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Family Group entity manager implementation using the default Entities API implementation.
 *
 * @version $Id$
 */
@Component(roles = {PrimaryEntityManager.class })
@Named("FamilyGroup")
@Singleton
public class DefaultFamilyGroupManager
    extends AbstractPrimaryEntityManager<FamilyGroup>
    implements PrimaryEntityManager<FamilyGroup>
{
    @Override
    public EntityReference getDataSpace()
    {
        return FamilyGroup.DEFAULT_DATA_SPACE;
    }

    @Override
    protected Class<? extends FamilyGroup> getEntityClass()
    {
        return DefaultFamilyGroup.class;
    }
}
