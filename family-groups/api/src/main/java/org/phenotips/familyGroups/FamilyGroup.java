/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.familyGroups;

import org.phenotips.Constants;
import org.phenotips.entities.PrimaryEntity;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

/**
 * Family Group implementation using the Entities API.
 *
 * @version $Id$
 */
@SuppressWarnings("checkstyle:interfaceistype")
public interface FamilyGroup extends PrimaryEntity
{
    /** The XClass used for storing family group data. */
    EntityReference CLASS_REFERENCE = new EntityReference("FamilyGroupClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    /** The default space where family group data is stored. */
    EntityReference DEFAULT_DATA_SPACE = new EntityReference("FamilyGroups", EntityType.SPACE);
}
