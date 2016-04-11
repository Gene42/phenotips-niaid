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
package org.phenotips.vocabularies.rest;

import org.phenotips.vocabularies.rest.model.VocabularyTermSummary;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Factory for converting internal java objects into their REST representations.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Unstable("New API introduced in 1.3")
@Role
public interface DomainObjectFactory
{
    /**
     * Converts a {@link Vocabulary} into its REST representation,
     * {@link org.phenotips.vocabularies.rest.model.Vocabulary}.
     *
     * @param vocabulary the vocabulary to be converted
     * @return the REST representation without links
     */
    org.phenotips.vocabularies.rest.model.Vocabulary createVocabularyRepresentation(Vocabulary vocabulary);

    /**
     * Converts a {@link VocabularyTerm} into a summary representation,
     * {@link org.phenotips.vocabularies.rest.model.VocabularyTermSummary}.
     *
     * @param term the term to be converted
     * @return a REST representation summarizing the term information
     */
    VocabularyTermSummary createVocabularyTermRepresentation(VocabularyTerm term);
}
