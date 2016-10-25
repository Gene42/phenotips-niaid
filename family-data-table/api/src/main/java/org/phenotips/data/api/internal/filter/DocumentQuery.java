/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal.filter;

import org.phenotips.data.api.internal.SpaceAndClass;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class DocumentQuery
{

    public static final String QUERIES_KEY = "queries";

    public static final String FILTERS_KEY = "filters";

    public static final String BINDING_KEY = "binding";

    private List<AbstractPropertyFilter> propertyFilters = new LinkedList<>();

    //private AbstractPropertyFilter binding;

    private List<DocumentQuery> documentQueries = new LinkedList<>();

    private SpaceAndClass mainSpaceClass;

    private Map<String, String> objNameMap = new LinkedHashMap<>();

    private AbstractObjectFilterFactory filterFactory;

    private String docName;
    private String baseObjName;

    private DocumentQuery parent;

    private int vLevel;

    private int hLevel;

    public DocumentQuery(AbstractObjectFilterFactory filterFactory)
    {
        this.filterFactory = filterFactory;
    }

    public DocumentQuery populate(JSONObject input, DocumentQuery parent, int vLevel, int hLevel)
    {

        this.vLevel = vLevel;
        this.hLevel = hLevel;
        this.mainSpaceClass = new SpaceAndClass(input);

        this.docName = "doc" + this.vLevel + "_" + this.hLevel;
        this.baseObjName = this.docName + "_obj";

        this.parent = parent;

        if (input.has(FILTERS_KEY)) {
            JSONArray filterJSONArray = input.getJSONArray(FILTERS_KEY);

            for (int i = 0, len = filterJSONArray.length(); i < len; i++) {
                JSONObject filterJson = filterJSONArray.optJSONObject(i);
                if (filterJson == null) {
                    continue;
                }

                AbstractPropertyFilter objectFilter = this.filterFactory.getFilter(filterJson);
                if (objectFilter != null && objectFilter.populate(filterJson, this).isValid()) {
                    this.propertyFilters.add(objectFilter);
                }
            }
        }

        if (input.has(QUERIES_KEY)) {
            JSONArray queriesJSONArray = input.getJSONArray(QUERIES_KEY);

            for (int i = 0, len = queriesJSONArray.length(); i < len; i++) {
                JSONObject queryJson = queriesJSONArray.optJSONObject(i);

                if (queryJson == null) {
                    continue;
                }

                this.documentQueries.add(
                    new DocumentQuery(this.filterFactory).populate(queryJson, this, vLevel + 1, i));
            }
        }

        if (input.has(BINDING_KEY)){
            JSONObject binding = input.getJSONObject(BINDING_KEY);
            this.propertyFilters.add(this.filterFactory.getBindingFilter(binding).populate(binding, this));
        }

        this.populateObjNameMap();

        return this;
    }

    public StringBuilder hql(StringBuilder builder, List<Object> bindingValues)
    {
        StringBuilder hql = builder;
        if (hql == null) {
            hql = new StringBuilder();
        }

        StringBuilder select = new StringBuilder();
        StringBuilder from = new StringBuilder();
        StringBuilder where = new StringBuilder();

        List<Object> fromValues = new LinkedList<>();
        List<Object> whereValues = new LinkedList<>();

        this.selectHql(select);
        this.fromHql(from, fromValues);
        this.whereHql(where, whereValues);

        bindingValues.addAll(fromValues);
        bindingValues.addAll(whereValues);

        return hql.append(select).append(from).append(where);
    }


    /**
     * Getter for objNameMap.
     *
     * @return objNameMap
     */
    public Map<String, String> getObjNameMap()
    {
        return objNameMap;
    }

    /**
     * Getter for parent.
     *
     * @return parent
     */
    public DocumentQuery getParent()
    {
        return parent;
    }

    /**
     * Getter for docName.
     *
     * @return docName
     */
    public String getDocName()
    {
        return docName;
    }

    private StringBuilder selectHql(StringBuilder select)
    {
        return select.append("select ").append(this.docName).append(" ");
    }

    private StringBuilder fromHql(StringBuilder from, List<Object> bindingValues)
    {
        //"select doc.space, doc.name, doc.author from XWikiDocument doc, BaseObject obj where doc.fullName=obj.name and obj.className='XWiki.WikiMacroClass'"

        from.append(" from XWikiDocument ").append(this.docName);

        for (String extraObjectName : this.objNameMap.values()) {
            from.append(", BaseObject ").append(extraObjectName);
        }

        for (AbstractPropertyFilter propertyFilter : this.propertyFilters) {
            propertyFilter.fromHql(from, bindingValues);
        }

        return from;
    }

    private StringBuilder whereHql(StringBuilder where, List<Object> bindingValues)
    {
        where.append(" where ").append(this.docName).append(".fullName=").append(this.baseObjName);
        where.append(".name and ").append(this.baseObjName).append(".className=? and ");
        where.append(this.docName).append(".fullName not like '%Template%' ESCAPE '!' ");

        bindingValues.add(this.mainSpaceClass.get());

        for (AbstractPropertyFilter propertyFilter : this.propertyFilters) {
            where.append(" and ");
            propertyFilter.whereHql(where, bindingValues);
        }

        for (DocumentQuery documentQuery : this.documentQueries) {
            where.append(" and exists(");
            documentQuery.hql(where, bindingValues).append(") ");
        }

        return where;
    }

    private void populateObjNameMap()
    {
        int currentLevel = 0;

        this.objNameMap.put(this.mainSpaceClass.get(), this.baseObjName);

        for (AbstractPropertyFilter filter : this.propertyFilters) {
            if (this.objNameMap.containsKey(filter.getSpaceAndClass().get())) {
                continue;
            }
            String extraObjName = String.format("%1$s_extraObj_%2$s", this.docName, currentLevel);
            this.objNameMap.put(filter.getSpaceAndClass().get(), extraObjName);
            currentLevel++;
        }
    }
}
