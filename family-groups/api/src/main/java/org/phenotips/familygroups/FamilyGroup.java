/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.familygroups;

import org.phenotips.Constants;
import org.phenotips.entities.PrimaryEntity;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import java.util.Set;

/**
 * Family Group implementation using the Entities API.
 *
 * @version $Id$
 */
public interface FamilyGroup extends PrimaryEntity
{
    /** The XClass used for storing family group data. */
    EntityReference CLASS_REFERENCE = new EntityReference("FamilyGroupClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    /** The default space where family group data is stored. */
    EntityReference DEFAULT_DATA_SPACE = new EntityReference("FamilyGroups", EntityType.SPACE);

    /**
     * Returns the set of Family IDs inside this Family Group.
     *
     * @return the set of Family IDs inside this Family Group.
     */
    Set<String> getFamilyIds();
}
