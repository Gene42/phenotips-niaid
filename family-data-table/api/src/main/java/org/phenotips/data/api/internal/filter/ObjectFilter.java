package org.phenotips.data.api.internal.filter;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class ObjectFilter extends AbstractFilter

    //http://localhost:8080/rest/search?outputSyntax=plain&transprefix=patient.livetable.&classname=PhenoTips.PatientClass&collist=doc.name%2Cexternal_id%2Cdoc.creator%2Cdoc.author%2Cdoc.creationDate%2Cdoc.date%2Cfirst_name%2Clast_name%2Creference&queryFilters=currentlanguage%2Chidden&&filterFrom=%2C+LongProperty+iid&filterWhere=and+iid.id.id+%3D+obj.id+and+iid.id.name+%3D+%27identifier%27+and+iid.value+%3E%3D+0&offset=1&limit=25&reqNo=7&last_name=Tr&reference=6&visibility=private&visibility=public&visibility=open&visibility%2Fclass=PhenoTips.VisibilityClass&owner%2Fclass=PhenoTips.OwnerClass&doc.creationDate%2Fafter=10%2F26%2F2016&omim_id=600274&omim_id%2Fjoin_mode=OR&phenotype%2Fjoin_mode=OR&phenotype_subterms=yes&gene%2Fclass=PhenoTips.GeneClass&gene%2Fmatch=ci&status%2Fclass=PhenoTips.GeneClass&status%2Fjoin_mode=OR&status%2FdependsOn=gene&status=candidate&status=solved&reference%2Fclass=PhenoTips.FamilyReferenceClass&sort=doc.name&dir=asc
{
    public static final String FILTERS_KEY = "filters";

    public static final String PROPERTY_NAME_KEY = "propertyName";

    private String propertyName;

    private boolean extended;

    private boolean negate;


    @Override public AbstractFilter populate(JSONObject obj, int level, AbstractFilterFactory filterFactory)
    {
        super.populate(obj, level, filterFactory);

        if (!StringUtils.equals(obj.optString("type"), "object")) {
            throw new IllegalArgumentException("An entity filter given a non object type config");
        }

        if (obj.has(FILTERS_KEY)) {
            JSONArray filterArray = obj.getJSONArray(FILTERS_KEY);


            for (Object filterObj : filterArray) {
                JSONObject filter = (JSONObject) filterObj;

            }
        }

        return this;
    }

    @Override public StringBuilder hql(StringBuilder builder, int level, String parentDoc)
    {
        return null;
    }

    @Override public StringBuilder selectHql(StringBuilder builder, int level, String parentDoc)
    {
        return null;
    }

    @Override public StringBuilder fromHql(StringBuilder builder, int level, String parentDoc)
    {
        return null;
    }

    @Override public StringBuilder whereHql(StringBuilder builder, int level, String parentDoc)
    {
        return null;
    }
}
