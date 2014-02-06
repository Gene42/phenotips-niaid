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

import org.phenotips.Constants;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

import java.util.Set;
import java.util.Collection;

import net.sf.json.JSONObject;

/**
 * Information about a patient.
 * 
 * @version $Id$
 * @since 1.0M8
 */
@Unstable
public interface Patient
{
    /** The XClass used for storing patient data. */
    EntityReference CLASS_REFERENCE = new EntityReference("PatientClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    /** The XClass used for storing version data of different ontologies. */
    EntityReference VERSION_REFERENCE = new EntityReference("OntologyVersionClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    /**
     * Returns a reference to the document where the patient data is stored.
     * 
     * @return a valid document reference
     */
    DocumentReference getDocument();

    /**
     * Returns a reference to the profile of the user that created the patient record.
     * 
     * @return a valid document reference
     * @todo Replace with a UserReference once they're available
     */
    DocumentReference getReporter();

    /**
     * Returns the list of recorded features, both positive and negative observations.
     * 
     * @return an unmodifiable set of {@link Feature features}, or an empty set if no features are recorded yet
     */
    Set<? extends Feature> getFeatures();

    /**
     * Returns the list of recorded disorders.
     * 
     * @return an unmodifiable set of {@link Disorder disorders}, or an empty set if no disorders have been identified
     *         yet
     */
    Set<? extends Disorder> getDisorders();

    /**
     * Retrieve all the patient data in a JSON format. For example:
     * 
     * <pre>
     * {
     *   "id": "xwiki:data.P0000001",
     *   "reporter": "xwiki.XWiki.PatchAdams",
     *   "features": [
     *     // See the documentation for {@link Feature#toJSON()}
     *   ],
     *   "disorders": [
     *     // See the documentation for {@link Disorder#toJSON()}
     *   ]
     * }
     * </pre>
     * 
     * @return the patient data, using the json-lib classes
     */
    JSONObject toJSON();
    
    /**
     * Retrieve patient data in a JSON format, restricted to the {@link RecordConfiguration} fields listed in onlyFieldNames
     * 
     * @param onlyFieldNames list of fields which should be included in the resulting JSON.
     *                       All available fields will be included if null (in which case this method is equivalent to {@link toJSON()})
     * @return selected sub-set of patient data, using the json-lib classes
     */
    JSONObject toJSON(Collection<String> onlyFieldNames);    
}
