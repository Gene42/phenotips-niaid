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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

    private Map<String, String> objNameMap = new LinkedHashMap<>();
    private Map<String, Map<String, String>> propertyNameMap = new LinkedHashMap<>();

    private DocumentQuery root;
    private DocumentQuery parent;
    private AbstractFilterFactory filterFactory;

    private String docName;

    private boolean countQuery;

    private int objNameCounter;
    private int docNameCounter;

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

    public int getNextDocIndex()
    {
        return this.root.docNameCounter++;
    }

    /**
     * Initializes this DocumentQuery based on the input. The hql method should be called after this method is called.
     * @param input input object containing instructions to initialized the query
     * @return this object
     */
    public DocumentQuery init(JSONObject input)
    {
        SpaceAndClass mainSpaceClass = new SpaceAndClass(input);

        this.docName = "doc_" + this.getNextDocIndex();
        this.objNameMap.put(mainSpaceClass.get(), this.docName + "_obj");

        if (input.has(DocumentSearch.ORDER_KEY)) {
            JSONObject sortFilter = input.getJSONObject(DocumentSearch.ORDER_KEY);
            this.orderFilter = this.filterFactory.getFilter(sortFilter).init(sortFilter, this).createBindings();
        }

        this.expression = new QueryExpression(this).init(input);


        return this;
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
        return this.root.filterFactory;
    }

    public void addToReferencedProperties(AbstractFilter filter)
    {
        if (filter != null && filter.isReference()) {
            this.referencedProperties.add(filter);
        }
    }

    public boolean isValid()
    {
        return this.expression.isValid();
    }

    private void addObjectBinding(SpaceAndClass spaceAndClass)
    {
        if (this.objNameMap.containsKey(spaceAndClass.get())) {
            return;
        }
        String extraObjName = String.format("%1$s_extraObj_%2$s", this.docName, this.objNameCounter);
        this.objNameMap.put(spaceAndClass.get(), extraObjName);
        this.objNameCounter++;
    }

    private QueryBuffer selectHql(QueryBuffer select)
    {
        select.append("select ");
        if (this.countQuery) {
            select.append("countQuery(*)");
        } else {
            select.append(this.docName);
        }
        return select.append(" ");
    }

    private QueryBuffer fromHql(QueryBuffer from)
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

    private QueryBuffer whereHql(QueryBuffer where, List<Object> bindingValues)
    {
        where.append(" where (");

        where.setOperator("and");


        for (Map.Entry<String, String> objMapEntry : this.objNameMap.entrySet()) {
            where.appendOperator();
            where.append(objMapEntry.getValue()).append(".name=").append(this.docName).append(".fullName and ");
            where.append(objMapEntry.getValue()).append(".className=? ");
            bindingValues.add(objMapEntry.getKey());
        }

        // Bind properties
        this.expression.bindProperty(where, bindingValues);
        this.handleFilters(where, bindingValues, this.referencedProperties, false);
        if (this.orderFilter != null && !this.countQuery) {
            this.orderFilter.bindProperty(where, bindingValues);
        }

        // Add value comparisons
        this.expression.addValueConditions(where, bindingValues);


        where.append(" and ").append(this.docName).append(".fullName not like '%Template%' ESCAPE '!' ) ");

        if (this.orderFilter != null && !this.countQuery) {
            this.orderFilter.addValueConditions(where, bindingValues);
        }

        return where;
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
