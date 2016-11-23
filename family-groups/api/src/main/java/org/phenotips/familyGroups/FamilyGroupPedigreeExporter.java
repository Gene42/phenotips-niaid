/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.familyGroups;

import org.xwiki.component.annotation.Role;

import java.util.List;

/**
 * Exporter to produce an aggregate PED file for all pedigrees within a family group.
 *
 * @version $Id$
 */
@Role
public interface FamilyGroupPedigreeExporter
{
    /**
     * Exports all pedigrees in the family group with the given ID as a multi-family aggregate PED file, using the
     * specified disorders to determine "affected" status.
     *
     * @param familyGroupId ID of the family group to be exported.
     * @param disorders set of disorders to determine "affected" status.
     * @return a string containing the family group in PED format.
     *         an empty string if there are no individuals in the family group's families.
     */
    String exportFamilyGroupAsPED(String familyGroupId, List<String> disorders);
}
