/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal;

import org.phenotips.data.api.DocumentSearch;
import org.phenotips.data.api.internal.filter.AbstractFilter;

import java.util.Collections;
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
    private List<AbstractFilter> referencedProperties = new LinkedList<>();
    private List<DocumentQuery> documentQueries = new LinkedList<>();
    private AbstractFilter orderFilter;

    private Map<String, String> objNameMap = new LinkedHashMap<>();
    private Map<String, Map<String, String>> propertyNameMap = new LinkedHashMap<>();


    private int objNameMapCurrentIndex;

    private AbstractFilterFactory filterFactory;

    private String docName;

    private DocumentQuery parent;

    private int validFilters;

    private boolean count;

    /**
     * Constructor.
     * @param filterFactory the filter factory to use
     */
    public DocumentQuery(AbstractFilterFactory filterFactory)
    {
        this(filterFactory, false);
    }

    /**
     * Constructor.
     * @param filterFactory the filter factory to use.
     * @param count flag fro determining whether or not to perform a count instead of a doc search
     *              (if true it creates a count query)
     */
    public DocumentQuery(AbstractFilterFactory filterFactory, boolean count)
    {
        this.filterFactory = filterFactory;
        this.count = count;
    }

    /**
     * Initializes this DocumentQuery based on the input. The hql method should be called after this method is called.
     * @param input input object containing instructions to initialized the query
     * @return this object
     */
    public DocumentQuery init(JSONObject input)
    {
        return this.init(input, null, 0, 0);
    }

    /**
     * Creates the hql query in the given StringBuilder, and adds any binding values to the provided list.
     * @param builder the StringBuilder to append to (if null a new one is created internally)
     * @param bindingValues the list of binding values to populate
     * @return the same given StringBuilder (or a brand new one if the given one was null)
     */
    public StringBuilder hql(StringBuilder builder, List<Object> bindingValues)
    {
        StringBuilder hql = builder;
        if (hql == null) {
            hql = new StringBuilder();
        }

        StringBuilder select = new StringBuilder();
        StringBuilder from = new StringBuilder();
        StringBuilder where = new StringBuilder();

        List<Object> whereValues = new LinkedList<>();

        this.selectHql(select);
        this.fromHql(from);
        this.whereHql(where, whereValues);

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
        return this.validFilters > 0 || CollectionUtils.isNotEmpty(this.documentQueries);
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
        SpaceAndClass mainSpaceClass = new SpaceAndClass(input);

        this.docName = "doc" + vLevel + "_" + hLevel;
        String baseObjName = this.docName + "_obj";

        this.parent = parent;

        this.objNameMap.put(mainSpaceClass.get(), baseObjName);

        if (input.has(FILTERS_KEY)) {
            JSONArray filterJSONArray = input.getJSONArray(FILTERS_KEY);

            for (int i = 0, len = filterJSONArray.length(); i < len; i++) {
                this.processFilterJSON(filterJSONArray.optJSONObject(i));
            }
        }

        if (input.has(DocumentSearch.ORDER_KEY)) {
            JSONObject sortFilter = input.getJSONObject(DocumentSearch.ORDER_KEY);
            this.orderFilter = this.filterFactory.getFilter(sortFilter).init(sortFilter, this).createBindings();
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
        select.append("select ");
        if (this.count) {
            select.append("count(*)");
        } else {
            select.append(this.docName);
        }
        return select.append(" ");
    }

    private StringBuilder fromHql(StringBuilder from)
    {
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

        this.handleFilters(where, bindingValues, this.propertyFilters, true);
        this.handleFilters(where, bindingValues, this.referencedProperties, false);

        for (DocumentQuery documentQuery : this.documentQueries) {
            where.append(" and exists(");
            documentQuery.hql(where, bindingValues).append(") ");
        }

        where.append(" and ").append(this.docName).append(".fullName not like '%Template%' ESCAPE '!' ");

        if (this.orderFilter != null && !this.count) {
            this.handleFilters(where, bindingValues, Collections.singletonList(this.orderFilter), true);
        }

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

    private void processFilterJSON(JSONObject filterJson)
    {
        if (filterJson == null) {
            return;
        }

        AbstractFilter objectFilter = this.filterFactory.getFilter(filterJson);
        if (objectFilter != null && objectFilter.init(filterJson, this).isValid()) {
            this.propertyFilters.add(objectFilter.createBindings());

            if (objectFilter.validatesQuery()) {
                this.validFilters++;
            }
        }
    }
}
