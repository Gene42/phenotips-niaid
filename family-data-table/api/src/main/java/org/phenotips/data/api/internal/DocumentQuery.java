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

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public static final String JOIN_MODE_KEY = "join_mode";

    /** JSON Object key */
    public static final String REFERENCE_CLASS_KEY = "reference_class";


    private List<AbstractFilter> referencedProperties = new LinkedList<>();
    private QueryExpression expression;
    private AbstractFilter orderFilter;

    /** Key: space.class, value: query object name/alias */
    private Map<SpaceAndClass, String> objNameMap = new LinkedHashMap<>();

    private Map<String, String> orExprObjNameMap = new LinkedHashMap<>();

    /** Key is space.class, value is map of [property name, table alias/property type] */
    //private Map<SpaceAndClass, Map<String, String>> propertyNameMap = new LinkedHashMap<>();
    private Map<SpaceAndClass, Set<PropertyName>> propertyNameMap = new LinkedHashMap<>();

    private DocumentQuery root;
    private DocumentQuery parent;
    private AbstractFilterFactory filterFactory;

    private String docName;

    private boolean countQuery;

    private int objNameCounter;
    private int docNameCounter = 1;
    private int expressionCounter = 1;

    /**
     * Constructor.
     * @param filterFactory the filter factory to use
     */
    public DocumentQuery(AbstractFilterFactory filterFactory)
    {
        this.filterFactory = filterFactory;
        this.root = this;
    }


    /**
     * Constructor.
     * @param filterFactory the filter factory to use
     * @param countQuery flag for determining whether or not this query is just used to perform a count(*)
     */
    public DocumentQuery(AbstractFilterFactory filterFactory, boolean countQuery)
    {
        this(filterFactory);
        this.countQuery = countQuery;
    }

    /**
     * Constructor.
     * @param parent the parent query
     */
    public DocumentQuery(DocumentQuery parent)
    {
        this.parent = parent;
        this.root = parent.root;
    }

    /**
     * Creates the hql query in the given StringBuilder, and adds any binding values to the provided list.
     * @param builder the StringBuilder to append to (if null a new one is created internally)
     * @param bindingValues the list of binding values to populate
     * @return the same given StringBuilder (or a brand new one if the given one was null)
     */
    public QueryBuffer hql(QueryBuffer builder, List<Object> bindingValues)
    {
        QueryBuffer hql = builder;
        if (hql == null) {
            hql = new QueryBuffer();
        }

        QueryBuffer select = new QueryBuffer();
        QueryBuffer from = new QueryBuffer();
        QueryBuffer where = new QueryBuffer();

        List<Object> whereValues = new LinkedList<>();

        this.selectHql(select);
        this.fromHql(from);
        this.whereHql(where, whereValues);

        bindingValues.addAll(whereValues);

        return hql.append(select).append(from).append(where);
    }

    public String getObjectName(SpaceAndClass spaceAndClass)
    {
        return this.getObjNameMap().get(spaceAndClass);
    }

    /*public void addOrGroupPropertyBinding(String groupName, String objectName)
    {
        this.orExprObjNameMap.put(groupName, objectName);
    }*/

    public void addPropertyBinding(SpaceAndClass spaceAndClass, PropertyName propertyName)
    {
        if (propertyName.isDocumentProperty()) {
            return;
        }

        this.addObjectBinding(spaceAndClass);

        //Map<String, String> propertyObjectTypeMap = this.propertyNameMap.get(spaceAndClass);
        Set<PropertyName> propertySet = this.propertyNameMap.get(spaceAndClass);

        if (propertySet == null) {
            propertySet = new HashSet<>();
            this.propertyNameMap.put(spaceAndClass, propertySet);
        }

        propertySet.add(propertyName);
    }

    public int getNextDocIndex()
    {
        return this.root.docNameCounter++;
    }

    public int getNextExpressionIndex()
    {
        return this.root.expressionCounter++;
    }

    /**
     * Initializes this DocumentQuery based on the input. The hql method should be called after this method is called.
     * @param input input object containing instructions to initialized the query
     * @return this object
     */
    public DocumentQuery init(JSONObject input)
    {
        SpaceAndClass mainSpaceClass = new SpaceAndClass(input);

        if (this.isRoot()) {
            this.docName = "doc";
        } else {
            this.docName = "doc_" + this.getNextDocIndex();
        }

        this.objNameMap.put(mainSpaceClass, this.docName + "_obj");

        this.expression = new QueryExpression(this).init(input);
        this.expression.createBindings();

        if (input.has(DocumentSearch.ORDER_KEY) && !this.countQuery) {
            JSONObject sortFilter = input.getJSONObject(DocumentSearch.ORDER_KEY);
            this.orderFilter = this.filterFactory.getFilter(sortFilter).init(sortFilter, this);
            this.orderFilter.setExpression(this.expression);
            this.orderFilter.createBindings();
        }

        return this;
    }


    /**
     * Getter for objNameMap.
     *
     * @return objNameMap
     */
    public Map<SpaceAndClass, String> getObjNameMap()
    {
        return this.objNameMap;
    }

    /**
     * Getter for parent.
     *
     * @return parent
     */
    public DocumentQuery getParent()
    {
        return this.parent;
    }

    /**
     * Getter for docName.
     *
     * @return docName
     */
    public String getDocName()
    {
        return this.docName;
    }

    /**
     * Getter for filterFactory.
     *
     * @return filterFactory
     */
    public AbstractFilterFactory getFilterFactory()
    {
        return this.root.filterFactory;
    }

    public void addToReferencedProperties(AbstractFilter filter)
    {
        if (filter != null && filter.isReference()) {
            //this.referencedProperties.add(filter);
            this.addPropertyBinding(filter.getSpaceAndClass(), filter.getPropertyName());
        }
    }

    public boolean isValid()
    {
        return this.expression.isValid();
    }

    public boolean isRoot()
    {
        return this.root == this;
    }

    private void addObjectBinding(SpaceAndClass spaceAndClass)
    {
        if (this.objNameMap.containsKey(spaceAndClass)) {
            return;
        }
        String extraObjName = String.format("%1$s_extraObj_%2$s", this.docName, this.objNameCounter);
        this.objNameMap.put(spaceAndClass, extraObjName);
        this.objNameCounter++;
    }

    private QueryBuffer selectHql(QueryBuffer select)
    {
        select.append("select ");
        if (this.countQuery) {
            select.append("count(*)");
        } else {
            select.append(this.docName).append(".fullName");
        }
        return select.append(" ");
    }

    private QueryBuffer fromHql(QueryBuffer from)
    {
        from.append(" from XWikiDocument ").append(this.docName);

        for (String extraObjectName : this.objNameMap.values()) {
            from.append(", BaseObject ").append(extraObjectName);
        }

        /** Key is space.class, value is map of [property name, table alias/property type] */
        /*for (Map.Entry<String, Map<String, String>> propertyNameMapEntry : this.propertyNameMap.entrySet()) {
            for (Map.Entry<String, String> entry : propertyNameMapEntry.getValue().entrySet()) {
                from.append(", ").append(entry.getValue()).append(" ");
                from.append(this.objNameMap.get(propertyNameMapEntry.getKey()));
                from.append("_").append(entry.getKey());
            }
        }*/

        for (Map.Entry<SpaceAndClass, Set<PropertyName>> propertyNameMapEntry : this.propertyNameMap.entrySet()) {
            for (PropertyName property : propertyNameMapEntry.getValue()) {
                from.append(", ").append(property.getObjectType()).append(" ");
                from.append(this.objNameMap.get(propertyNameMapEntry.getKey()));
                from.append("_").append(property.get());
            }
        }

        return from;
    }

    private QueryBuffer whereHql(QueryBuffer where, List<Object> bindingValues)
    {
        where.append(" where (");

        where.setOperator("and");


        for (Map.Entry<SpaceAndClass, String> objMapEntry : this.objNameMap.entrySet()) {
            where.appendOperator();
            //where.append(objMapEntry.getValue()).append(".name=").append(this.docName).append(".fullName and ");
            where.append(objMapEntry.getValue()).append(".name=").append(this.docName).append(".fullName ");
            //where.append(objMapEntry.getValue()).append(".className=? ");
            //bindingValues.add(objMapEntry.getKey().get());
        }

        // Bind properties
        for (Map.Entry<SpaceAndClass, Set<PropertyName>> propertyNameMapEntry : this.propertyNameMap.entrySet()) {
            for (PropertyName property : propertyNameMapEntry.getValue()) {
                this.bindProperty(where, bindingValues, property, propertyNameMapEntry.getKey());
            }
        }

        //this.expression.bindProperty(where, bindingValues);
        //this.handleFilters(where, bindingValues, this.referencedProperties, false);
        /*if (this.orderFilter != null && !this.countQuery) {
            this.orderFilter.bindProperty(where, bindingValues);
        }*/

        // Add value comparisons
        this.expression.addValueConditions(where, bindingValues);

        where.append(" and ").append(this.docName).append(".fullName not like '%Template%' ESCAPE '!' ) ");

        if (this.orderFilter != null) {
            this.orderFilter.addValueConditions(where, bindingValues);
        }

        return where;
    }

    private void bindProperty(QueryBuffer where, List<Object> bindingValues, PropertyName propertyName,
        SpaceAndClass spaceAndClass)
    {
        String baseObj = this.getObjectName(spaceAndClass);

        String objPropName = AbstractFilter.getPropertyNameForQuery(propertyName, spaceAndClass, this, 0);
        where.appendOperator().append(baseObj).append(".id=").append(objPropName).append(".id.id and ");
        where.append(objPropName).append(".id.name=? ");

        bindingValues.add(propertyName.get());
    }

    private void handleFilters(QueryBuffer where, List<Object> bindingValues, List<AbstractFilter> filters,
     boolean addValueConditions) {
        for (AbstractFilter filter : filters) {
            filter.bindProperty(where, bindingValues);

            if (addValueConditions) {
                filter.addValueConditions(where, bindingValues);
            }
        }
    }
}
