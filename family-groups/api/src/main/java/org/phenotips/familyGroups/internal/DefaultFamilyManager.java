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
import org.phenotips.familyGroups.Family;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Family entity manager implementation using the default Entities API implementation.
 *
 * @version $Id$
 */
@Component(roles = {PrimaryEntityManager.class })
@Named("Family")
@Singleton
public class DefaultFamilyManager
    extends AbstractPrimaryEntityManager<Family>
    implements PrimaryEntityManager<Family>
{
    @Override public EntityReference getDataSpace()
    {
        return Family.DEFAULT_DATA_SPACE;
    }

    @Override
    protected Class<? extends Family> getEntityClass()
    {
        return DefaultFamily.class;
    }
}
