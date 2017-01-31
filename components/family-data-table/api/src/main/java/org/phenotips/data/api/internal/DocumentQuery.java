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
 * Object representing a document query. It is used to generate an hql statement given a JSON input.
 *
 * @version $Id$
 */
public class DocumentQuery
{

    /** JSON Object key. */
    public static final String QUERIES_KEY = "queries";

    /** JSON Object key. */
    public static final String FILTERS_KEY = "filters";

    /** JSON Object key. */
    public static final String JOIN_MODE_KEY = "join_mode";

    /** JSON Object key. */
    public static final String REFERENCE_CLASS_KEY = "reference_class";

    private QueryExpression expression;
    private AbstractFilter orderFilter;

    /** Key: space.class, value: query object name/alias. */
    private Map<SpaceAndClass, String> objNameMap = new LinkedHashMap<>();

    private Map<String, String> orExprObjNameMap = new LinkedHashMap<>();

    /** Key is space.class, value is map of [property name, table alias/property type]. */
    private Map<SpaceAndClass, Set<PropertyName>> propertyNameMap = new LinkedHashMap<>();

    private DocumentQuery root;
    private DocumentQuery parent;
    private AbstractFilterFactory filterFactory;

    private String docName;

    private boolean countQuery;

    private int objNameCounter;
    private int docNameCounter = 1;
    private int expressionCounter = 1;

    private SpaceAndClass mainSpaceClass;

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

    /**
     * Returns the name of the object to be used in the query string given the SpaceAndClass object.
     * @param spaceAndClass the object to use as a key for retrieving the object name.
     * @return an object name
     */
    public String getObjectName(SpaceAndClass spaceAndClass)
    {
        return this.getObjNameMap().get(spaceAndClass);
    }

    /**
     * Uses the given SpaceAndClass to bind an extra BaseObject to the query, and the given PropertyName to
     * bind a property object to the query.
     *
     * @param spaceAndClass the SpaceAndClass to use
     * @param propertyName the PropertyName to use
     */
    public void addPropertyBinding(SpaceAndClass spaceAndClass, PropertyName propertyName)
    {
        if (propertyName.isDocumentProperty()) {
            return;
        }

        this.addObjectBinding(spaceAndClass);

        Set<PropertyName> propertySet = this.propertyNameMap.get(spaceAndClass);

        if (propertySet == null) {
            propertySet = new HashSet<>();
            this.propertyNameMap.put(spaceAndClass, propertySet);
        }

        propertySet.add(propertyName);
    }

    /**
     * Returns the next usable index for use in document naming. Only the root query keeps the value, and is shared
     * among all sub queries (thus avoiding naming conflicts in branches with similar structure).
     * @return an int value
     */
    public int getNextDocIndex()
    {
        return this.root.docNameCounter++;
    }

    /**
     * Returns the next usable index for use in query expression naming. Only the root query keeps the value, and is
     * shared among all sub queries (thus avoiding naming conflicts in branches with similar structure).
     * @return an int value
     */
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
        this.mainSpaceClass = new SpaceAndClass(input);

        if (this.isRoot()) {
            this.docName = "doc";
        } else {
            this.docName = "doc_" + this.getNextDocIndex();
        }

        this.objNameMap.put(this.mainSpaceClass, this.docName + "_obj");

        this.expression = new QueryExpression(this).init(input);

        if (this.expression.isValid() && this.expression.validatesQuery()) {
            this.expression.createBindings();
        }

        if (input.has(DocumentSearch.ORDER_KEY) && !this.countQuery) {
            JSONObject sortFilter = input.getJSONObject(DocumentSearch.ORDER_KEY);
            this.orderFilter = this.filterFactory.getFilter(sortFilter).init(sortFilter, this, this.expression);
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

    /**
     * Returns true if this query is valid (ie contains a valid expression), false otherwise.
     *
     * @return boolean value
     */
    public boolean isValid()
    {
        return this.expression.isValid() && this.expression.validatesQuery();
    }

    /**
     * Returns true if this query is the root query, false otherwise.
     * @return boolean value
     */
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
        select.append("select distinct ");
        if (this.countQuery) {
            select.append("count(*)");
        } else {
            select.append(this.docName).append(".fullName");
        }

        if (this.orderFilter != null) {
            select.append(", ").append(this.orderFilter.getPropertyValueNameForQuery());
        }

        return select.append(" ");
    }

    private QueryBuffer fromHql(QueryBuffer from)
    {
        from.append(" from XWikiDocument ").append(this.docName);

        for (String extraObjectName : this.objNameMap.values()) {
            from.append(", BaseObject ").append(extraObjectName);
        }

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

        this.objNameMap.put(this.mainSpaceClass, this.docName + "_obj");

        where.appendOperator().append(this.objNameMap.get(this.mainSpaceClass)).append(".className=? ");
        bindingValues.add(this.mainSpaceClass.get());

        for (Map.Entry<SpaceAndClass, String> objMapEntry : this.objNameMap.entrySet()) {
            where.appendOperator();
            where.append(objMapEntry.getValue()).append(".name=").append(this.docName).append(".fullName ");
        }

        // Bind properties
        for (Map.Entry<SpaceAndClass, Set<PropertyName>> propertyNameMapEntry : this.propertyNameMap.entrySet()) {
            for (PropertyName property : propertyNameMapEntry.getValue()) {
                this.bindPropertyID(where, property, propertyNameMapEntry.getKey());
            }
        }

        if (this.orderFilter != null) {
            this.orderFilter.bindPropertyClass(where, bindingValues);
        }

        // Add value comparisons
        this.expression.addValueConditions(where, bindingValues);

        where.append(" and ").append(this.docName).append(".fullName not like '%Template%' ESCAPE '!' ) ");

        if (this.orderFilter != null) {
            this.orderFilter.addValueConditions(where, bindingValues);
        }

        return where;
    }

    private void bindPropertyID(QueryBuffer where, PropertyName propertyName, SpaceAndClass spaceAndClass)
    {
        String baseObj = this.getObjectName(spaceAndClass);
        String objPropName = AbstractFilter.getPropertyNameForQuery(propertyName, spaceAndClass, this, 0);
        where.appendOperator().append(baseObj).append(".id=").append(objPropName).append(".id.id ");
    }
}
