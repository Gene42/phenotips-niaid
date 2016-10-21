/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal.filter;

import org.phenotips.data.api.internal.DocumentSearchUtils;
import org.phenotips.data.api.internal.SpaceAndClass;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public abstract class AbstractPropertyFilter<T>

    //http://localhost:8080/rest/search?outputSyntax=plain&transprefix=patient.livetable.&classname=PhenoTips.PatientClass&collist=doc.name%2Cexternal_id%2Cdoc.creator%2Cdoc.author%2Cdoc.creationDate%2Cdoc.date%2Cfirst_name%2Clast_name%2Creference&queryFilters=currentlanguage%2Chidden&&filterFrom=%2C+LongProperty+iid&filterWhere=and+iid.id.id+%3D+obj.id+and+iid.id.name+%3D+%27identifier%27+and+iid.value+%3E%3D+0&offset=1&limit=25&reqNo=7&last_name=Tr&reference=6&visibility=private&visibility=public&visibility=open&visibility%2Fclass=PhenoTips.VisibilityClass&owner%2Fclass=PhenoTips.OwnerClass&doc.creationDate%2Fafter=10%2F26%2F2016&omim_id=600274&omim_id%2Fjoin_mode=OR&phenotype%2Fjoin_mode=OR&phenotype_subterms=yes&gene%2Fclass=PhenoTips.GeneClass&gene%2Fmatch=ci&status%2Fclass=PhenoTips.GeneClass&status%2Fjoin_mode=OR&status%2FdependsOn=gene&status=candidate&status=solved&reference%2Fclass=PhenoTips.FamilyReferenceClass&sort=doc.name&dir=asc
{
    //public static final String FILTERS_KEY = "filters";

    public static final String PROPERTY_NAME_KEY = "property_name";

    public static final String DOC_CLASS_KEY = "doc_class";

    public static final String CLASS_KEY = "class";

    //public static final String TYPE_KEY = "type";

    public static final String VALUES_KEY = "values";

    protected String tableName;

    protected int level;

    protected String propertyName;

    protected boolean extended;

    protected boolean negate;

    protected PropertyInterface property;
    protected BaseClass baseClass;

    protected T min;

    protected T max;

    protected List<T> values = new LinkedList<>();

    protected boolean isDocumentProperty;

    private SpaceAndClass spaceAndClass;

    private String documentPropertyName;

    private DocumentQuery parent;

    public AbstractPropertyFilter(PropertyInterface property, BaseClass baseClass)
    {
        this.property = property;
        this.baseClass = baseClass;
    }

    public AbstractPropertyFilter populate(JSONObject input, DocumentQuery parent)
    {
        this.parent = parent;

        if (!input.has(CLASS_KEY)) {
            throw new IllegalArgumentException(String.format("[%s] key not present", CLASS_KEY));
        }

        this.spaceAndClass = new SpaceAndClass(input);

        this.propertyName = DocumentSearchUtils.sanitizeForHql(input.getString(PROPERTY_NAME_KEY));

        if (StringUtils.startsWith(this.propertyName, "doc.")) {
            this.isDocumentProperty = true;
            this.documentPropertyName = StringUtils.removeStart(this.propertyName, "doc.");
        }

        return this;
    }

    public StringBuilder fromHql(StringBuilder from, List<Object> bindingValues)
    {
        if (!this.isDocumentProperty) {
            from.append(", ").append(this.tableName).append(" ").append(this.parent.getObjNameMap().get(this.spaceAndClass.get()));
            from.append("_").append(this.propertyName);
        }
        return from;
    }

    //http://localhost:8080/get/PhenoTips/LiveTableResults?outputSyntax=plain&transprefix=family.livetable.&classname=PhenoTips.FamilyClass&collist=doc.name%2Cexternal_id%2Cdoc.creator%2Cdoc.creationDate%2Cdoc.author%2Cdoc.date%2Cproband_id%2Cindividuals%2Cdescription%2Canalysis_status%2Cfamily_id%2Ccomplex_add_to_group&queryFilters=currentlanguage%2Chidden&&offset=1&limit=25&reqNo=13&doc.name=FAM0000001&sort=doc.date&dir=asc

    public StringBuilder whereHql(StringBuilder where, List<Object> bindingValues)
    {
        if (this.isDocumentProperty) {
            return where;
        }

        String baseObj = parent.getObjNameMap().get(this.spaceAndClass.get());

        // NOTE: getSafeAlias not the best solution, I might use random strings
        String objPropName = this.getObjectPropertyName();
        where.append(" ").append(baseObj).append(".className=? and ");
        where.append(baseObj).append(".name=").append(this.getDocName()).append(".fullName and ");
        where.append(baseObj).append(".id=").append(objPropName).append(".id.id and ");
        where.append(objPropName).append(".id.name=? ");

        bindingValues.add(this.spaceAndClass.get());
        bindingValues.add(this.propertyName);

        return where;
    }

    public String getDocName() {
        return this.parent.getDocName();
    }

    public String getObjectPropertyName() {
        return this.parent.getObjNameMap().get(this.spaceAndClass.get()) + "_" + this.propertyName;
    }

    public String getDocumentPropertyName() {
        return this.getDocName() + "." + this.documentPropertyName;
    }

    public boolean isValid(){
        return CollectionUtils.isNotEmpty(this.values) || this.min != null || this.max != null;
    }

    /**
     * Getter for spaceAndClass.
     *
     * @return spaceAndClass
     */
    public SpaceAndClass getSpaceAndClass()
    {
        return spaceAndClass;
    }
}
