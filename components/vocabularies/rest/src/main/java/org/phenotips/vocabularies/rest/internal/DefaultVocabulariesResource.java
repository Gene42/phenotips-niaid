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
package org.phenotips.vocabularies.rest.internal;

import org.phenotips.data.rest.Relations;
import org.phenotips.vocabularies.rest.DomainObjectFactory;
import org.phenotips.vocabularies.rest.VocabulariesResource;
import org.phenotips.vocabularies.rest.VocabularyResource;
import org.phenotips.vocabularies.rest.VocabularyTermSuggestionsResource;
import org.phenotips.vocabularies.rest.model.Link;
import org.phenotips.vocabularies.rest.model.Vocabularies;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.UriBuilder;

/**
 * Default implementation for {@link VocabulariesResource} using XWiki's support for REST resources.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Component
@Named("org.phenotips.vocabularies.rest.internal.DefaultVocabulariesResource")
@Singleton
public class DefaultVocabulariesResource extends XWikiResource implements VocabulariesResource
{
    @Inject
    private VocabularyManager vm;

    @Inject
    private DomainObjectFactory objectFactory;

    @Override
    public Vocabularies getAllVocabularies()
    {
        Vocabularies result = new Vocabularies();
        List<String> vocabularyIDs = this.vm.getAvailableVocabularies();
        List<org.phenotips.vocabularies.rest.model.Vocabulary> availableVocabs = new ArrayList<>();
        for (String vocabularyID : vocabularyIDs) {
            Vocabulary vocab = this.vm.getVocabulary(vocabularyID);
            org.phenotips.vocabularies.rest.model.Vocabulary rep =
                this.objectFactory.createVocabularyRepresentation(vocab);
            List<Link> linkList = new ArrayList<>();
            linkList.add(new Link().withHref(
                UriBuilder.fromUri(this.uriInfo.getBaseUri()).path(VocabularyResource.class).build(vocabularyID)
                    .toString())
                .withRel(Relations.VOCABULARY));
            linkList.add(new Link().withRel(Relations.SUGGEST)
                .withHref(UriBuilder.fromUri(this.uriInfo.getBaseUri())
                    .path(VocabularyTermSuggestionsResource.class)
                    .build(vocabularyID)
                    .toString()));
            rep.withLinks(linkList);
            availableVocabs.add(rep);
        }
        result.withVocabularies(availableVocabs);
        result.withLinks(new Link().withRel(Relations.SELF)
            .withHref(UriBuilder.fromUri(this.uriInfo.getBaseUri()).path(VocabulariesResource.class).toString()));
        return result;
    }
}
