package org.phenotips.familydashboard.internal;

import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyTerm;
import org.phenotips.vocabulary.internal.solr.MendelianInheritanceInMan;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @version $Id$
 */
public class OmimToHpoMapper
{
    List<String> omimTerms;
    List<String> hpoTerms;
    Map<String, JSONObject> omimToHpoMapping;
    JSONObject origObj;
    JSONObject result;

    public OmimToHpoMapper(JSONObject data)
    {
        this.setDisorders(data);
        this.setFeatures(data);
        this.setOmimToHpoMapping();
        this.origObj = data;
    }

    public Map<String, JSONObject> getOmimToHpoMapping()
    {
        return this.omimToHpoMapping;
    }

    public List<JSONObject> getOmimRelevantPhenotypes()
    {
        return (List) this.omimToHpoMapping.values();
    }

    public List<String> getOmimTerms()
    {
        return this.omimTerms;
    }

    public List<String> getHpoTerms()
    {
        return this.hpoTerms;
    }

    private void setOmimToHpoMapping()
    {
        omimToHpoMapping = new HashMap<>();
        for (String term : this.omimTerms) {
            JSONObject omimResult = getOmimTermResults(term);
            if (omimResult != null) {
                omimToHpoMapping.put(term, omimResult);
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
                    omimTerms.add((String) disorder);
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

    private JSONObject getOmimTermResults(String term)
    {
        Vocabulary vocabulary = new MendelianInheritanceInMan();
        VocabularyTerm omimTerm = vocabulary.getTerm(term);
        JSONObject omimJson;
        if (omimTerm != null) {
            return omimTerm.toJSON();
        }
        return null;
    }

    /**
     * Updates the family member's phenotype list with those that are relevant to the omim disorder(s) the
     * member is diagnosed with.
     *
     */
    public void updateFamilyMemberPhenotypes()
    {
        this.origObj.putOpt("features", this.getOmimRelevantPhenotypes());
    }
}
