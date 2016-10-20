package org.phenotips.data.api.internal.filter;

import org.phenotips.data.api.internal.DocumentSearchUtils;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.json.JSONObject;

import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public abstract class ObjectFilter<T> extends AbstractFilter

    //http://localhost:8080/rest/search?outputSyntax=plain&transprefix=patient.livetable.&classname=PhenoTips.PatientClass&collist=doc.name%2Cexternal_id%2Cdoc.creator%2Cdoc.author%2Cdoc.creationDate%2Cdoc.date%2Cfirst_name%2Clast_name%2Creference&queryFilters=currentlanguage%2Chidden&&filterFrom=%2C+LongProperty+iid&filterWhere=and+iid.id.id+%3D+obj.id+and+iid.id.name+%3D+%27identifier%27+and+iid.value+%3E%3D+0&offset=1&limit=25&reqNo=7&last_name=Tr&reference=6&visibility=private&visibility=public&visibility=open&visibility%2Fclass=PhenoTips.VisibilityClass&owner%2Fclass=PhenoTips.OwnerClass&doc.creationDate%2Fafter=10%2F26%2F2016&omim_id=600274&omim_id%2Fjoin_mode=OR&phenotype%2Fjoin_mode=OR&phenotype_subterms=yes&gene%2Fclass=PhenoTips.GeneClass&gene%2Fmatch=ci&status%2Fclass=PhenoTips.GeneClass&status%2Fjoin_mode=OR&status%2FdependsOn=gene&status=candidate&status=solved&reference%2Fclass=PhenoTips.FamilyReferenceClass&sort=doc.name&dir=asc
{
    //public static final String FILTERS_KEY = "filters";

    public static final String PROPERTY_NAME_KEY = "propertyName";

    protected String propertyName;

    protected boolean extended;

    protected boolean negate;

    protected PropertyInterface property;
    protected BaseClass baseClass;

    protected T min;

    protected T max;

    protected List<T> values = new LinkedList<>();

    public ObjectFilter(PropertyInterface property, BaseClass baseClass)
    {
        this.property = property;
        this.baseClass = baseClass;
    }

    @Override public ObjectFilter populate(JSONObject input, int level)
    {
        super.populate(input, level);

        /*if (!StringUtils.equals(obj.optString("type"), "object")) {
            throw new IllegalArgumentException("An entity filter given a non object type config");
        }*/

        this.propertyName = DocumentSearchUtils.sanitizeForHql(input.getString(PROPERTY_NAME_KEY));


        return this;
    }

    @Override public StringBuilder fromHql(StringBuilder from, List<Object> bindingValues, int level, String baseObj, String parentDoc)
    {
        return from.append(", ").append(tableName).append(" ").append(baseObj).append("_").append(propertyName);
    }

    @Override public StringBuilder whereHql(StringBuilder builder, List<Object> bindingValues, int level, String baseObj, String parentDoc)
    {
        /*String objPropName = baseObj + "_" + propertyName;
        builder.append(" ");
        builder.append(baseObj).append(".className='").append(spaceAndClassName).append("' and ");
        builder.append(baseObj).append(".name=").append(parentDoc).append(".fullName and ");
        builder.append(baseObj).append(".id=").append(objPropName).append(".id.id and ");
        builder.append(objPropName).append(".id.name='").append(propertyName).append("' ");*/

        // NOTE: getSafeAlias not the best solution, I might use random strings
        String objPropName = this.getObjectPropertyName(baseObj); //baseObj + "_" + propertyName;
        builder.append(" ");
        builder.append(baseObj).append(".className=? and ");
        builder.append(baseObj).append(".name=").append(parentDoc).append(".fullName and ");
        builder.append(baseObj).append(".id=").append(objPropName).append(".id.id and ");
        builder.append(objPropName).append(".id.name=? ");

        bindingValues.add(spaceAndClassName);
        bindingValues.add(propertyName);

        //and extraobj1.className = "PhenoTips.VisibilityClass"
        //and extraobj1.name = doc.fullName
        // and extraobj1.id=visibility.id.id
        // and visibility.id.name = "visibility"
        return builder;
    }

    public String getObjectPropertyName(String baseObj) {
        return baseObj + "_" + propertyName;
    }

    @Override
    public boolean isValid(){
        return CollectionUtils.isNotEmpty(this.values) || this.min != null || this.max != null;
    }
}
