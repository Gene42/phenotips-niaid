/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.data;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import net.sf.json.JSONObject;

/**
 * API that provides access to patient data. No access rights are checked here.
 * 
 * @version $Id$
 * @since 1.0M8
 */
@Unstable
@Role
public interface PatientData
{
    /**
     * Retrieve a {@link Patient patient} by it's PhenoTips identifier.
     * 
     * @param id the patient identifier, i.e. the serialized document reference
     * @return the patient data, or {@code null} if the requested patient does not exist or is not a valid patient
     */
    Patient getPatientById(String id);

    /**
     * Retrieve a {@link Patient patient} by it's clinical identifier. Only works if external identifiers are enabled
     * and used.
     * 
     * @param externalId the patient's clinical identifier, as set by the patient's reporter
     * @return the patient data, or {@code null} if the requested patient does not exist or is not a valid patient
     */
    Patient getPatientByExternalId(String externalId);

    /**
     * Create and return a new empty patient record.
     * 
     * @return the created patient record
     */
    Patient createNewPatient();

    /**
     * Create a new patient record based on patient representation in the given JSON object.
     * 
     * @return the created patient record
     */
    Patient createNewPatient(JSONObject patientData);
}
