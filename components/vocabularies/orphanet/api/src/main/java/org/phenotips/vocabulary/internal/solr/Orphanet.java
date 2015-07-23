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
package org.phenotips.vocabulary.internal.solr;

import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

/**
 * Provides access to the ORPHANET ontology. The ontology prefix is {@code ORPHANET}.
 *
 * @version $Id$
 * @since 1.2M5
 */
@Component
@Named("orphanet")
@Singleton
public class Orphanet extends AbstractSolrVocabulary
{
    @Override
    protected String getName()
    {
        return "ORPHANET";
    }

    @Override
    public String getDefaultSourceLocation()
    {
        return "http://data.bioontology.org/ontologies/ORDO/submissions/7/download?apikey=8b5b7825-538d-40e0-9e9e-5ab9274a9aeb&download_format=owl";
    }

    @Override
    public VocabularyTerm getTerm(String id)
    {
        VocabularyTerm result = super.getTerm(id);
        if (result == null) {
            String optionalPrefix = this.getName() + ":";
            if (StringUtils.startsWith(id, optionalPrefix)) {
                result = getTerm(StringUtils.substringAfter(id, optionalPrefix));
            }
        }
        return result;
    }

    @Override
    public Set<String> getAliases()
    {
        Set<String> result = new HashSet<String>();
        result.add(getName());
        return result;
    }

}
