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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrInputDocument;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * Provides access to the ORPHANET ontology. The ontology prefix is {@code ORPHANET}.
 *
 * @version $Id$
 * @since 1.2M5
 */
@Component
@Named("orphanet")
@Singleton
public class Orphanet extends AbstractOWLSolrVocabulary
{
    @Override
    public String getIdentifier()
    {
        return "orphanet";
    }

    @Override
    public String getName()
    {
        return "Orphanet";
    }

    @Override
    protected String getCoreName()
    {
        return getIdentifier();
    }

    @Override
    protected int getSolrDocsPerBatch()
    {
        return 15000;
    }

    @Override
    public String getDefaultSourceLocation()
    {
        return "http://data.bioontology.org/ontologies/ORDO/submissions/7/download";
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
    protected Collection<OntClass> getRootClasses(OntModel ontModel)
    {
        Collection<OntClass> keepers = new HashSet<>();
        List<OntClass> ontClasses = ontModel.listClasses().toList();
        for (OntClass ontClass : ontClasses) {
            if ("disease".equals(ontClass.getLabel(null))) {
                keepers.add(ontClass);
            } else if ("group of disorders".equals(ontClass.getLabel(null))) {
                keepers.add(ontClass);
            } else if ("genetic material".equals(ontClass.getLabel(null))) {
                keepers.add(ontClass);
            }
        }
        return keepers;
    }

    @Override
    public Set<String> getAliases()
    {
        Set<String> result = new HashSet<String>();
        result.add(getName());
        return result;
    }

    @Override
    protected SolrInputDocument parseSolrDocumentFromOntClass(SolrInputDocument doc, OntClass ontClass,
        Collection<OntClass> roots)
    {
        // orphanet label == phenotips vocab name
        doc.addField("id", getOrphanetId(ontClass.getLocalName()));
        doc.addField("name", ontClass.getLabel(null));

        // save all parents
        List<OntClass> parents = ontClass.listSuperClasses().toList();
        for (OntClass parent : parents) {
            if (parent.isRestriction() && parent.asRestriction().isSomeValuesFromRestriction()) {
                // a restriction, we only care about "SomeValuesFrom" restrictions
                // get get the value of the restriction, make sure it's under the roots we care about, not some other
                // property.
                // TODO in another iteration, later:
                //   epidemiology (1/2000000, family, etc)
                //   prevalence
                Restriction restriction = parent.asRestriction();
                OntClass value = restriction.asSomeValuesFromRestriction().getSomeValuesFrom().as(OntClass.class);
                for (OntClass root : roots) {
                    if (value.hasSuperClass(root)) {
                        doc.addField("term_category", getOrphanetId(value.getLocalName()));
                    }
                }
            } else if (isRegularClass(parent)) {
                // just a regular class.
                doc.addField("term_category", getOrphanetId(parent.getLocalName()));
                // we should be able to use the {{@link OntClass.getSuperClass()}} method, but since it returns an
                // **arbitrary** selection, we'll just pick the first superclass that's actually a class
                // that we stumble upon.
                if (doc.getField("is_a") == null) {
                    doc.addField("is_a", getOrphanetId(parent.getLocalName()));
                }
            }
        }

        extractProperties(doc, ontClass);
        return doc;
    }

    private boolean isRegularClass(OntClass ontClass)
    {
        return !(ontClass.isRestriction()
            || ontClass.isEnumeratedClass()
            || ontClass.isUnionClass()
            || ontClass.isIntersectionClass()
            || ontClass.isComplementClass());
    }

    private void extractProperties(SolrInputDocument doc, OntClass ontClass)
    {
        List<Statement> statements = ontClass.listProperties().toList();
        for (Statement statement : statements) {
            Resource subject = statement.getSubject();
            RDFNode object = statement.getObject();
            Property predicate = statement.getPredicate();

            if (!subject.as(OntClass.class).equals(ontClass)) {
                throw new RuntimeException("Subject is not the disease.");
            }

            String relation = predicate.getLocalName();
            switch (relation) {
                case "hasDbXref":
                    extractDBXref(doc, object);
                    break;
                case "alternative_term":
                    // pull alternative term
                    doc.addField("alternative_term", object.asLiteral().getString());
                    break;
                case "definition":
                    // pull definition term
                    doc.addField("def", object.asLiteral().getString());
                    break;
                case "symbol":
                    // pull gene symbol (only genes have this.)
                    doc.addField("gene_symbol", object.asLiteral().getString());
                    break;
                default:
            }
        }
    }

    private void extractDBXref(SolrInputDocument doc, RDFNode object)
    {
        // pull reference to other dbs (OMIM, HGNC, Ensembl)
        // stick external db id into solr
        String[] parts = object.asLiteral().getLexicalForm().split(":");
        String id = parts[1];
        String ontology = parts[0];
        switch (ontology) {
            case "OMIM":
            case "HGNC":
            case "Ensembl":
                doc.addField(ontology.toLowerCase() + "_id", id);
                break;
            default:
                break;
        }
    }

    /**
     * Get a numerical id string from a localName. Assuming the localName is in the form "Orphanet_XXX"
     *
     * @param localName the localName of an OWL class.
     * @return the string id.
     */
    private String getOrphanetId(String localName)
    {
        return localName.replace("Orphanet_", "");
    }

}
