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

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * Ontologies processed from OWL files share much of the processing code.
 *
 * @version $Id$
 * @since 1.2M4 (under different names since 1.1)
 */
public abstract class AbstractOWLSolrVocabulary extends AbstractSolrVocabulary
{
    protected static final String VERSION_FIELD_NAME = "version";


    @Override
    public VocabularyTerm getTerm(String id) {
        VocabularyTerm result = super.getTerm(id);
        if (result == null) {
            Map<String, String> queryParameters = new HashMap<>();
            queryParameters.put("id", id);
            List<VocabularyTerm> results = search(queryParameters);
            if (results != null && !results.isEmpty()) {
                result = search(queryParameters).iterator().next();
            }
        }
        return result;
    }

    @Override
    public int reindex(String sourceUrl) {
        this.clear();
        return this.index(sourceUrl);
    }

    /**
     * Add a vocabulary to the index.
     *
     * @param sourceUrl the address from where to get the vocabulary source file
     *
     * @return {@code 0} if the indexing succeeded, {@code 1} if writing to the Solr server failed, {@code 2} if the
     * specified URL is invalid
     */
    protected int index(String sourceUrl) {
        // fetch the ontology. If this is over the network, it may take a while.
        OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        ontModel.read(sourceUrl);

        // get the root classes of the ontology that we can start the parsing with.
        Collection<OntClass> roots = getRootClasses(ontModel);
        // reusing doc for speed (see http://wiki.apache.org/lucene-java/ImproveIndexingSpeed)
        SolrInputDocument doc = new SolrInputDocument();
        for (OntClass root : roots) {
            addDoc(doc, root, roots);
        }

        try {
            this.externalServicesAccess.getSolrConnection().commit(true, true);
        } catch (SolrServerException ex) {
            this.logger.warn("Failed to index ontology: {}", ex.getMessage());
            return 1;
        } catch (IOException ex) {
            this.logger.warn("Failed to communicate with the Solr server while indexing ontology: {}",
                    ex.getMessage());
            return 1;
        }

        return 0;
    }

    private void addDoc(SolrInputDocument doc, OntClass ontClass, Collection<OntClass> roots) {
        this.parseSolrDocumentFromOntClass(doc, ontClass, roots);

        try {
            this.externalServicesAccess.getSolrConnection().add(doc);
            doc.clear();
        } catch (SolrServerException ex) {
            this.logger.warn("Failed to add a document when indexing ontology: {}", ex.getMessage());
            return;
        } catch (IOException ex) {
            this.logger.warn("Failed to communicate with the Solr server while adding a document to an ontology: {}",
                    ex.getMessage());
            return;
        } catch (OutOfMemoryError ex) {
            this.logger.warn("Failed to add terms to the Solr. Ran out of memory. {}", ex.getMessage());
            return;
        }


        // extract any subclasses, add our class to the roots, and recurse.
        // this shouldn't lead us to walk circles in the ontology, since subclasses are implicit.
        Collection<OntClass> newRoots = new HashSet<>(roots);
        newRoots.add(ontClass);
        for (OntClass subClass : ontClass.listSubClasses().toSet()) {
            addDoc(doc, subClass, newRoots);
        }
    }

    protected abstract Collection<OntClass> getRootClasses(OntModel ontModel);

    protected abstract SolrInputDocument parseSolrDocumentFromOntClass(SolrInputDocument doc,
                                                                       OntClass ontClass,
                                                                       Collection<OntClass> roots);

    /**
     * Delete all the data in the Solr index.
     *
     * @return {@code 0} if the command was successful, {@code 1} otherwise
     */
    protected int clear() {
        try {
            this.externalServicesAccess.getSolrConnection().deleteByQuery("*:*");
            return 0;
        } catch (SolrServerException ex) {
            this.logger.error("SolrServerException while clearing the Solr index", ex);
        } catch (IOException ex) {
            this.logger.error("IOException while clearing the Solr index", ex);
        }
        return 1;
    }

    @Override
    public String getVersion() {
        QueryResponse response;
        SolrQuery query = new SolrQuery();
        SolrDocumentList termList;
        SolrDocument firstDoc;

        query.setQuery("version:*");
        query.set("rows", "1");
        try {
            response = this.externalServicesAccess.getSolrConnection().query(query);
            termList = response.getResults();

            if (!termList.isEmpty()) {
                firstDoc = termList.get(0);
                return firstDoc.getFieldValue(VERSION_FIELD_NAME).toString();
            }
        } catch (SolrServerException | SolrException | IOException ex) {
            this.logger.warn("Failed to query ontology version: {}", ex.getMessage());
        }
        return null;
    }

    /** The number of documents to be added and committed to Solr at a time. */
    protected int getSolrDocsPerBatch() {
        return 0;
    }
}
