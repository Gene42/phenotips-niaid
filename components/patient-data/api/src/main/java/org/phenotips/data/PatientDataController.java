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
 * <p>
 * This role allows extending the patient record with custom data. The basic patient class only knows how to handle a
 * few essential patient record entries, while additional data can be made available, in a modular way, by custom
 * implementations of this role.
 * </p>
 * <p>
 * The {@link #load(Patient)} method is responsible for populating a {@link Patient} object with actual data from the
 * patient record. Data read from the record can be accessed using the {@link Patient}'s {@link Patient#getData(String)
 * custom data access method}. The {@link #save(Patient)} method is responsible for storing back into the database the
 * custom data.
 * </p>
 * <p>
 * The {@link #writeJSON(Patient, JSONObject)} method serializes custom data into the JSON being generated for a
 * patient, and {@link #readJSON(Patient, JSONObject)} reads back data from a JSON into a patient record. Since
 * different components might serialize their custom data into the same JSON property, the
 * {@link #writeJSON(Patient, JSONObject)} method should not always create a new property, but first try to extend an
 * existing one.
 * </p>
 * 
 * @param <T> the type of data being managed by this component; usually a key-value pair, where the {@code key} is a
 *            String (name)
 * @version $Id$
 * @since 1.0M10
 */
@Unstable
@Role
public interface PatientDataController<T>
{
    /**
     * Patient record initialization function: given a new patient being created, creates the custom data with initial
     * values and returns it to the patient.
     * 
     * @param patient the newly created patient
     * @return the initial data, if any, or {@code null}
     */
    PatientData<T> initialize(Patient patient);

    /**
     * Data loading function: given a patient, extracts data from the underlying document and returns it to the patient.
     * 
     * @param patient the patient being loaded
     * @return the loaded data, if any, or {@code null}
     */
    PatientData<T> load(Patient patient);

    /**
     * Serialization function: given a patient, saves the data that it {@link #load(Patient) loaded} for this patient in
     * the underlying document storing the patient record.
     * 
     * @param patient the patient being saved
     */
    void save(Patient patient);

    /**
     * Exports the data being managed by this data controller into the patient JSON export.
     * 
     * @param patient the patient being exported
     * @param json existing JSON object to which the data will be appended
     */
    void writeJSON(Patient patient, JSONObject json);

    /**
     * Reads custom data back from a JSON into a patient record.
     * 
     * @param json the JSON that is to be imported
     * @return the read data, if any, or {@code null}
     */
    PatientData<T> readJSON(JSONObject json);
}
