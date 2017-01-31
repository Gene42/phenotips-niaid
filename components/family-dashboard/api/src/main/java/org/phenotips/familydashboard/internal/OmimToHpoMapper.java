/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.familydashboard.internal;

import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyTerm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;

/**
 * Class for determining which phenotype terms from a predefined set are relevant to a given set of OMIM disorders.
 *
 * @version $Id$
 */
public class OmimToHpoMapper
{
    protected Vocabulary omimService;
    protected Vocabulary hpoService;

    private Map<String, VocabularyTerm> omimTerms;
    private Map<String, VocabularyTerm> hpoTerms;

    /* Container for the omim to hpo mapping */
    private Map<String, Map<String, VocabularyTerm>> internalMap;

    /**
     * Constructor for this class.
     *
     * @param omimService - the omim vocabulary ontology service
     * @param hpoService - the hpo vocabulary ontology service
     */
    public OmimToHpoMapper(Vocabulary omimService, Vocabulary hpoService)
    {
        this.omimService = omimService;
        this.hpoService = hpoService;
    }

    /**
     * Initiates the omim-to-hpo mapping and returns the result of the mapping.
     *
     * This is done by taking the set of disorders and mapping the actual symptoms (phenotypes) associated with the
     * given disorder to phenotype terms in the input list. Checks whether any of the actual symptoms are an exact
     * match or is a related term (is parent or child to) to the initial set of phenotype terms. Those phenotypes
     * from the initial input set of phenotypes that produce an exact match or is related to any of the actual
     * symptoms for an OMIM disorder get stored in a list as the values in the map, with the OMIM id it is relevant
     * to as the key.
     *
     * @param disorders - the list of omim disorders (i.e. "MIM:908880" or "908880")
     * @param features - the list of hpo terms (i.e. "HP:321245")
     * @return a map with the omim disorder id as keys and a list of hpo vocabulary terms as values
     */
    public Map<String, List<VocabularyTerm>> getOmimToHpoMap(List<String> disorders, List<String> features)
    {
        this.setDisorders(disorders);
        this.setFeatures(features);
        this.mapOmimToHpo();
        return getOmimToHpoMap();
    }

    /**
     * Converts the internal map structure to the output map structure.
     *
     * @return a map with the omim disorder id as keys and a list of hpo vocabulary terms as values
     */
    private Map<String, List<VocabularyTerm>> getOmimToHpoMap()
    {
        Map<String, List<VocabularyTerm>> output = new HashMap<>();
        if (this.internalMap != null && this.internalMap.size() != 0) {
            for (Map.Entry<String, Map<String, VocabularyTerm>> entry : this.internalMap.entrySet()) {
                List<VocabularyTerm> relevantPhenotypes = new ArrayList<>(entry.getValue().values());
                output.put(entry.getKey(), relevantPhenotypes);
            }
        }
        return output;
    }

    /**
     * Maps the actual symptoms of each OMIM disorder onto the input list of phenotypes. The actual
     * symptoms associated with each omim disorder are retrieved and mapped against the input phenotype list.
     * Along with mapping for exact matches between an actual symptom and terms of the input phenotype list,
     * parents of the actual symptom as well as parents of each term in the input phenotype list are also
     * evaluated.
     */
    private void mapOmimToHpo()
    {
        this.internalMap = new HashMap<>();
        for (VocabularyTerm omimTerm : this.omimTerms.values()) {
            JSONArray actualSymptoms = omimTerm.toJSON().optJSONArray("actual_symptom");
            if (actualSymptoms == null) {
                continue;
            }
            Map<String, VocabularyTerm> matchedFeatures = new HashMap<>();
            for (int i = 0; i < actualSymptoms.length(); i++) {
                String symptom = actualSymptoms.getString(i);
                VocabularyTerm relevantPhenotype = findRelevantPhenotype(symptom);
                if (relevantPhenotype != null && !matchedFeatures.containsKey(relevantPhenotype.getId())) {
                    matchedFeatures.put(relevantPhenotype.getId(), relevantPhenotype);
                }
            }
            this.internalMap.put(omimTerm.getId(), matchedFeatures);
        }
    }

    /**
     * This method looks for a match between:
     *  1\ the actual symptom for an OMIM disorder and terms inside the original phenotype set,
     *  2\ the actual symptom for an OMIM disorder and the parent terms of the original phenotype set,
     *  3\ the parent terms of the actual symptom for an OMIM disorder and terms inside the original phenotype set.
     *
     * @param symptomQuery - the actual symptom that is associated with an OMIM disorder
     * @return the term from the original phenotype set that produces the match if there's a match, else returns null
     */
    private VocabularyTerm findRelevantPhenotype(String symptomQuery)
    {
        VocabularyTerm match = findExactMatch(symptomQuery);
        if (match != null) {
            return match;
        }
        match = findFeatureParentsMatch(symptomQuery);
        if (match != null) {
            return match;
        }
        match = findDisorderSymptomParentsMatch(symptomQuery);
        if (match != null) {
            return match;
        }
        return null;
    }

    /**
     * Looks for an exact match between the actual symptom for an OMIM disorder and the terms of each
     * phenotype in the original phenotype set.
     *
     * @param symptomQuery - the actual symptom that is associated with an OMIM disorder
     * @return the relevant phenotype term from the original set that produced a match
     */
    private VocabularyTerm findExactMatch(String symptomQuery)
    {
        if (this.hpoTerms.containsKey(symptomQuery)) {
            return this.hpoTerms.get(symptomQuery);
        }
        return null;
    }

    /**
     * Looks for a match between the actual symptom for an OMIM disorder and the parent terms of each
     * phenotype in the original phenotype set.
     *
     * @param symptomQuery - the actual symptom that is associated with an OMIM disorder
     * @return the relevant phenotype term from the original set that produced a match
     */
    private VocabularyTerm findFeatureParentsMatch(String symptomQuery)
    {
        for (Map.Entry<String, VocabularyTerm> entry : this.hpoTerms.entrySet()) {
            VocabularyTerm feature = entry.getValue();
            for (VocabularyTerm parent : feature.getParents()) {
                if (symptomQuery.equals(parent.getId())) {
                    return feature;
                }
            }
        }
        return null;
    }

    /**
     * Looks for a match between the parent terms of the actual symptom for an OMIM disorder and
     * the terms inside the original phenotype set.
     *
     * @param symptomQuery - the actual symptom that is associated with an OMIM disorder
     * @return the relevant phenotype term from the original set that produced a match
     */
    private VocabularyTerm findDisorderSymptomParentsMatch(String symptomQuery)
    {
        VocabularyTerm symptom = this.hpoService.getTerm(symptomQuery);
        if (symptom != null) {
            for (VocabularyTerm parent : symptom.getParents()) {
                if (this.hpoTerms.containsKey(parent.getId())) {
                    return this.hpoTerms.get(parent.getId());
                }
            }
        }
        return null;
    }

    private void setDisorders(List<String> disorders)
    {
        this.omimTerms = new HashMap<>();
        for (String disorder : disorders) {
            VocabularyTerm term = this.omimService.getTerm(disorder);
            if (term != null) {
                this.omimTerms.put(term.getId(), term);
            }
        }
    }

    private void setFeatures(List<String> features)
    {
        this.hpoTerms = new HashMap<>();
        for (String feature : features) {
            VocabularyTerm term = this.hpoService.getTerm(feature);
            if (term != null) {
                this.hpoTerms.put(term.getId(), term);
            }
        }
    }
}
