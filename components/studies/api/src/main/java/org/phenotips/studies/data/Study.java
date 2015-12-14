/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.studies.data;

import org.phenotips.Constants;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

/**
 * @version $Id$
 */
public interface Study
{
    /** The XClass used for storing project data. */
    EntityReference CLASS_REFERENCE = new EntityReference("StudyClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    /** The default space where patient data is stored. */
    EntityReference DEFAULT_DATA_SPACE = new EntityReference("Studies", EntityType.SPACE);

    /**
     * @return id of study. e.g. Study.t1
     */
    String getId();

    /**
     * @return name of study. e.g. t1
     */
    String getName();

    /**
     * @return title of study
     */
    String getTitle();

    /**
     * @return reference of study document
     */
    DocumentReference getDocumentReference();
}
