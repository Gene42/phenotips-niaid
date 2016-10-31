/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.familyGroups;

import org.phenotips.entities.PrimaryEntity;

import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

/**
 * Family entity interface extending the default Entities API interface. This is a temporary solution until
 * Families use the Entities API in PhenoTips core. There may be inconsistencies between the behaviour of this module
 * and the behaviour of the Family Studies module from PhenoTips core.
 *
 * @version $Id$
 */
@Unstable("Will be removed after Families use the Entities API in PhenoTips core")
public interface Family extends PrimaryEntity
{
    /** The XClass used for storing family data. */
    EntityReference CLASS_REFERENCE = org.phenotips.studies.family.Family.CLASS_REFERENCE;

    /** The default space where family group data is stored. */
    EntityReference DEFAULT_DATA_SPACE = org.phenotips.studies.family.Family.DATA_SPACE;
}
