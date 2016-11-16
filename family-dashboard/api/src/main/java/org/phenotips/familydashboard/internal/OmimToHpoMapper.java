package org.phenotips.familydashboard.internal;

import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyTerm;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Class for determining which phenotype terms from a predefined set are relevant to a given OMIM disorder.
 *
 * @version $Id$
 */
public class OmimToHpoMapper
{
    private List<String> omimTerms;
    private List<String> hpoTerms;
    private Map<String, Map<String, JSONObject>> omimToHpoMapping;
    private JSONObject origObj;
    private JSONObject currentMatch;
    private final int matchThreshold;

    protected Vocabulary omimService;
    protected Vocabulary hpoService;

    public OmimToHpoMapper(JSONObject data, int matchThreshold, Vocabulary omimService, Vocabulary hpoService)
    {
        this.origObj = data;
        this.matchThreshold = matchThreshold;
        this.omimService = omimService;
        this.hpoService = hpoService;
        this.setDisorders(data);
        this.setFeatures(data);
        this.setOmimToHpoMapping();
    }

    /**
     * Updates the family member's phenotype list with those that are relevant to the omim disorder(s) the
     * member is diagnosed with.
     *
     */
    public void updateFamilyMemberVocabularies()
    {
        updateFamilyMemberDisorders();
        updateFamilyMemberPhenotypes();
    }

    private void updateFamilyMemberPhenotypes()
    {
        this.origObj.putOpt("features", this.getRelevantPhenotypes());
    }

    private void updateFamilyMemberDisorders()
    {
        this.origObj.putOpt("disorders", this.getOmimDisorders());
    }

    private JSONObject getOmimTermResults(String term)
    {
        VocabularyTerm omimTerm = this.omimService.getTerm(term);
        if (omimTerm != null) {
            return omimTerm.toJSON();
        }
        return null;
    }

    /**
     * This method checks:
     *  1\ Whether the actual symptom for an OMIM disorder matches with a patient's phenotype set
     *  2\ Whether a parent term to the actual symptom matches with a patient's phenotype set
     *  3\ Whether a parent term to each item in a patient's phenotype set matches with the actual symptom
     * If any of the above are true, the term from the patient's phenotype set which produced a match gets saved.
     *
     * @param symptom the actual symptom that is associated with an OMIM disorder
     * @return true if a match between any of the above 3 cases are found, and false otherwise
     */
    private boolean hasRelevantPhenotype(String symptom)
    {
        if (this.hpoTerms.contains(symptom)) {
            setCurrentMatch(this.hpoService.getTerm(symptom));
            return true;

        } else if (foundPatientPhenotypesToSymptomParentsMatch(symptom)) {
            return true;

        } else if (foundSymptomToPatientPhenotypeParentsMatch(symptom)) {
            return true;
        }
        return false;
    }

    /**
     * Checks for a match between the parent terms of the actual_symptom with any item in the patient's phenotype set.
     *
     * @param symptom the actual symptom that is associated with an OMIM disorder
     * @return
     */
    private boolean foundPatientPhenotypesToSymptomParentsMatch(String symptom)
    {
        VocabularyTerm symptomTerm = this.hpoService.getTerm(symptom);
        if (symptomTerm != null) {
            for (VocabularyTerm term : symptomTerm.getParents()) {
                System.out.println(term.getId() + ": " + term.getName());
                if (this.hpoTerms.contains(term.getId())) {
                    setCurrentMatch(term);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks for a match between the actual_symptom and the parent terms of each item in the patient's phenotype set.
     *
     * @param symptom the actual symptom that is associated with an OMIM disorder
     * @return
     */
    private boolean foundSymptomToPatientPhenotypeParentsMatch(String symptom)
    {
        for (String feature : this.hpoTerms) {
            VocabularyTerm patientTerm = this.hpoService.getTerm(feature);
            if (patientTerm != null) {

                for (VocabularyTerm parent : patientTerm.getParents()) {
                    System.out.println(parent.getId() + ": " + parent.getName());
                    if (symptom.equals(parent.getId())) {
                        setCurrentMatch(patientTerm);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void setOmimToHpoMapping()
    {
        this.omimToHpoMapping = new HashMap<>();
        for (String term : this.omimTerms) {
            JSONObject omimResult = getOmimTermResults(term);
            if (omimResult != null) {
                JSONArray actualSymptoms = omimResult.getJSONArray("actual_symptom");
                int threshold = actualSymptoms.length() <= this.matchThreshold ?
                    actualSymptoms.length() : this.matchThreshold;
                Map<String, JSONObject> matchedFeatures = new HashMap<>();
                for (int i = 0; i < this.matchThreshold; i++) {
                    String symptom = actualSymptoms.getString(i);
                    if (hasRelevantPhenotype(symptom) && !matchedFeatures.containsKey(getCurrentMatch().getString("id"))) {
                        matchedFeatures.put(getCurrentMatch().getString("id"), getCurrentMatch());
                    }
                }
                omimToHpoMapping.put(term, matchedFeatures);
            }
        }
    }

    private void setDisorders(JSONObject data)
    {
        this.omimTerms = new ArrayList<>();
        JSONArray array = data.optJSONArray("disorders");
        if (array != null) {
            for (Object disorder : array) {
                if (disorder instanceof JSONObject) {
                    JSONObject disorderObj = (JSONObject) disorder;
                    omimTerms.add(disorderObj.optString("id"));
                } else if (disorder instanceof String) {
                    String omimTerm = (String) disorder;
                    if (!omimTerm.startsWith("MIM:")) {
                        omimTerm = "MIM:".concat(omimTerm);
                    }
                    omimTerms.add(omimTerm);
                }
            }
        }
    }

    private void setFeatures(JSONObject data)
    {
        this.hpoTerms = new ArrayList<>();
        JSONArray array = data.optJSONArray("features");
        if (array != null) {
            for (Object feature : array) {
                if (feature instanceof JSONObject) {
                    JSONObject disorderObj = (JSONObject) feature;
                    hpoTerms.add(disorderObj.optString("id"));
                } else if (feature instanceof String) {
                    hpoTerms.add((String) feature);
                }
            }
        }
    }

    private void setCurrentMatch(VocabularyTerm term)
    {
        this.currentMatch = new JSONObject();
        currentMatch.accumulate("id", term.getId());
        currentMatch.accumulate("label", term.getName());
    }

    private JSONObject getCurrentMatch()
    {
        return this.currentMatch != null ? this.currentMatch : new JSONObject();
    }

    private JSONArray getRelevantPhenotypes()
    {
        JSONArray aggregatedPhenotypes = new JSONArray();
        if (this.omimToHpoMapping != null && this.omimToHpoMapping.size() != 0) {
            List<Map<String, JSONObject>> mapValues = new ArrayList<>(this.omimToHpoMapping.values());
            for (Map<String, JSONObject> omimToHpoMap : mapValues) {
                aggregatedPhenotypes.put(omimToHpoMap.values());
            }
        }
        return aggregatedPhenotypes;
    }

    private JSONArray getOmimDisorders()
    {
        JSONArray omimArray = new JSONArray();
        if (this.omimToHpoMapping != null && this.omimToHpoMapping.size() != 0) {
            for (String term : this.omimToHpoMapping.keySet()) {
                VocabularyTerm omimTerm = this.omimService.getTerm(term);
                if (omimTerm != null) {
                    // Add omim object for use in table
                    JSONObject omimObj = new JSONObject();
                    omimObj.accumulate("id", omimTerm.getId());
                    omimObj.accumulate("label", omimTerm.getName());
                    // Also add a list of relevant phenotypes to the object, to know which phenotypes are associated with which disorders
                    Map<String, JSONObject> relevantPhenotypes = this.omimToHpoMapping.get(term);
                    omimObj.accumulate("relevant_phenotypes", relevantPhenotypes.keySet());
                    omimArray.put(omimObj);
                }
            }
        }
        return omimArray;
    }

    private Map<String, Map<String, JSONObject>> getOmimToHpoMapping()
    {
        return this.omimToHpoMapping;
    }
}
