/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal;

import org.phenotips.data.api.internal.filter.AbstractFilter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class DocumentQuery
{

    /** JSON Object key */
    public static final String QUERIES_KEY = "queries";

    /** JSON Object key */
    public static final String FILTERS_KEY = "filters";

    /** JSON Object key */
    public static final String REFERENCE_CLASS_KEY = "reference_class";


    private List<AbstractFilter> propertyFilters = new LinkedList<>();
    private List<AbstractFilter> referenceClasses = new LinkedList<>();
    private List<AbstractFilter> referencedProperties = new LinkedList<>();
    private List<DocumentQuery> documentQueries = new LinkedList<>();


    private SpaceAndClass mainSpaceClass;

    private Map<String, String> objNameMap = new LinkedHashMap<>();
    private Map<String, Map<String, String>> propertyNameMap = new LinkedHashMap<>();


    private int objNameMapCurrentIndex;

    private AbstractFilterFactory filterFactory;

    private String docName;
    private String baseObjName;

    private DocumentQuery parent;

    /**
     * Constructor.
     * @param filterFactory the filter factory to use
     */
    public DocumentQuery(AbstractFilterFactory filterFactory)
    {
        this.filterFactory = filterFactory;
    }

    public DocumentQuery init(JSONObject input)
    {
        return this.init(input, null, 0, 0);
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

    public String getObjectName(SpaceAndClass spaceAndClass)
    {
        return this.getObjNameMap().get(spaceAndClass.get());
    }


    public void addPropertyBinding(SpaceAndClass spaceAndClass, PropertyName propertyName)
    {
        if (propertyName.isDocumentProperty()) {
            return;
        }

        this.addObjectBinding(spaceAndClass);

        Map<String, String> propertyObjectTypeMap = this.propertyNameMap.get(spaceAndClass.get());

        if (propertyObjectTypeMap == null) {
            propertyObjectTypeMap = new HashMap<>();
            this.propertyNameMap.put(spaceAndClass.get(), propertyObjectTypeMap);
        }

        propertyObjectTypeMap.put(propertyName.get(), propertyName.getObjectType());
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

    /**
     * Getter for filterFactory.
     *
     * @return filterFactory
     */
    public AbstractFilterFactory getFilterFactory()
    {
        return filterFactory;
    }

    public void addToReferencedProperties(AbstractFilter filter)
    {
        if (filter != null && filter.isReference()) {
            this.referencedProperties.add(filter);
        }
    }

    public boolean isValid()
    {
        return CollectionUtils.isNotEmpty(this.propertyFilters) || CollectionUtils.isNotEmpty(this.documentQueries);
    }

    public static StringBuilder appendQueryOperator(StringBuilder buffer, String operator, int valuesIndex)
    {
        if (valuesIndex > 0) {
            buffer.append(" ").append(operator).append(" ");
        }

        return buffer;
    }

    private void addObjectBinding(SpaceAndClass spaceAndClass)
    {
        if (this.objNameMap.containsKey(spaceAndClass.get())) {
            return;
        }
        String extraObjName = String.format("%1$s_extraObj_%2$s", this.docName, this.objNameMapCurrentIndex);
        this.objNameMap.put(spaceAndClass.get(), extraObjName);
        this.objNameMapCurrentIndex++;
    }

    private DocumentQuery init(JSONObject input, DocumentQuery parent, int vLevel, int hLevel)
    {
        this.mainSpaceClass = new SpaceAndClass(input);

        this.docName = "doc" + vLevel + "_" + hLevel;
        this.baseObjName = this.docName + "_obj";

        this.parent = parent;

        this.objNameMap.put(this.mainSpaceClass.get(), this.baseObjName);

        if (input.has(FILTERS_KEY)) {
            JSONArray filterJSONArray = input.getJSONArray(FILTERS_KEY);

            for (int i = 0, len = filterJSONArray.length(); i < len; i++) {
                JSONObject filterJson = filterJSONArray.optJSONObject(i);
                if (filterJson == null) {
                    continue;
                }

                AbstractFilter objectFilter = this.filterFactory.getFilter(filterJson);
                if (objectFilter != null && objectFilter.init(filterJson, this).isValid()) {
                    this.propertyFilters.add(objectFilter.createBindings());
                }
            }
        }

        if (input.has(REFERENCE_CLASS_KEY)){
            JSONObject binding = input.getJSONObject(REFERENCE_CLASS_KEY);
            this.referenceClasses.add(
                this.filterFactory.getReferenceClassFilter(binding).init(binding, this).createBindings());
        }

        if (input.has(QUERIES_KEY)) {
            JSONArray queriesJSONArray = input.getJSONArray(QUERIES_KEY);

            for (int i = 0, len = queriesJSONArray.length(); i < len; i++) {
                JSONObject queryJson = queriesJSONArray.optJSONObject(i);

                if (queryJson == null) {
                    continue;
                }

                DocumentQuery query = new DocumentQuery(this.filterFactory).init(queryJson, this, vLevel + 1, i);

                if (query.isValid()) {
                    this.documentQueries.add(query);
                }
            }
        }

        return this;
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

        for (Map.Entry<String, Map<String, String>> propertyNameMapEntry : this.propertyNameMap.entrySet()) {
            for (Map.Entry<String, String> entry : propertyNameMapEntry.getValue().entrySet()) {
                from.append(", ").append(entry.getValue()).append(" ");
                from.append(this.objNameMap.get(propertyNameMapEntry.getKey()));
                from.append("_").append(entry.getKey());
            }
        }

        return from;
    }

    private StringBuilder whereHql(StringBuilder where, List<Object> bindingValues)
    {
        where.append(" where ");

        int i = 0;
        for (Map.Entry<String, String> objMapEntry : this.objNameMap.entrySet()) {
            appendQueryOperator(where, "and", i++);

            where.append(objMapEntry.getValue()).append(".name=").append(this.docName).append(".fullName and ");
            where.append(objMapEntry.getValue()).append(".className=? ");
            bindingValues.add(objMapEntry.getKey());
        }

        this.handleFilters(where, bindingValues, this.referenceClasses, true);
        this.handleFilters(where, bindingValues, this.propertyFilters, true);
        this.handleFilters(where, bindingValues, this.referencedProperties, false);

        for (DocumentQuery documentQuery : this.documentQueries) {
            where.append(" and exists(");
            documentQuery.hql(where, bindingValues).append(") ");
        }

        where.append(" and ").append(this.docName).append(".fullName not like '%Template%' ESCAPE '!' ");

        return where;
    }

    private void handleFilters(StringBuilder where, List<Object> bindingValues, List<AbstractFilter> filters,
     boolean addValueConditions) {
        for (AbstractFilter filter : filters) {

            filter.bindProperty(where, bindingValues);

            if (addValueConditions) {
                filter.addValueConditions(where, bindingValues);
            }
        }
    }
}
