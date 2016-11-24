/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal.filter;

import org.phenotips.data.api.internal.DocumentQuery;
import org.phenotips.data.api.internal.PropertyName;
import org.phenotips.data.api.internal.QueryBuffer;
import org.phenotips.data.api.internal.QueryElement;
import org.phenotips.data.api.internal.QueryExpression;
import org.phenotips.data.api.internal.SearchUtils;
import org.phenotips.data.api.internal.SpaceAndClass;
import org.phenotips.security.encryption.internal.EncryptedClass;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Abstract Filter Class.
 *
 * @param <T> the value type of the filter (used for values, min and max)
 *
 * @version $Id$
 */
public abstract class AbstractFilter<T> implements QueryElement
{
    /** Filter param key. */
    public static final String JOIN_MODE_KEY = "join_mode";

    /** Value for JOIN_MODE key. */
    public static final String JOIN_MODE_VALUE_AND = "and";

    /** Value for JOIN_MODE key. */
    public static final String JOIN_MODE_VALUE_OR = "or";

    /** Value for JOIN_MODE key. */
    public static final String JOIN_MODE_DEFAULT_VALUE = AbstractFilter.JOIN_MODE_VALUE_AND;

    /** Param key. */
    public static final String DOC_CLASS_KEY = "doc_class";

    /** Param key. */
    public static final String VALUES_KEY = "values";

    /** Param key. */
    public static final String TYPE_KEY = "type";

    /** Param key. */
    public static final String VALIDATES_QUERY_KEY = "validates_query";

    /** Param key. */
    public static final String REF_VALUES_KEY = "ref_values";

    /** Param key. */
    public static final String PARENT_LEVEL_KEY = "parent_level";

    /** Param key. */
    public static final String NOT_KEY = "negate";

    /** Logger, can be used by implementing classes. */
    public static final Logger LOGGER = LoggerFactory.getLogger(AbstractFilter.class);

    private static final List<String> VALUE_PROPERTY_NAMES = ListUtils.unmodifiableList(
        Arrays.asList(AbstractFilter.VALUES_KEY, AbstractFilter.REF_VALUES_KEY)
    );

    private int level;

    private String tableName;
    private PropertyName propertyName;
    private SpaceAndClass spaceAndClass;

    private boolean negate;
    private String joinMode;
    private boolean reference;
    private boolean validatesQuery;

    private DocumentQuery parent;
    private QueryExpression parentExpression;

    private PropertyInterface property;
    private BaseClass baseClass;

    private T min;
    private T max;
    private List<T> values = new LinkedList<>();
    private List<AbstractFilter> refValues = new LinkedList<>();

    /**
     * Constructor.
     * @param property PropertyInterface
     * @param baseClass BaseClass
     */
    public AbstractFilter(PropertyInterface property, BaseClass baseClass)
    {
        this(property, baseClass, null);
    }

    /**
     * Constructor.
     * @param property PropertyInterface
     * @param baseClass BaseClass
     * @param tableName the object type of the property
     */
    public AbstractFilter(PropertyInterface property, BaseClass baseClass, String tableName)
    {
        this.property = property;
        this.baseClass = baseClass;
        this.tableName = tableName;

        if (this.isEncrypted()) {
            this.tableName = "EncryptedProperty";
        }
    }

    /**
     * Takes the given JSONObject and initializes the filter.
     * @param input JSONObject to use for getting necessary data for filter initialization
     * @param parent the parent document query this filter belongs to
     * @param parentExpression the parent expression this filter belongs to
     * @return this AbstractPropertyFilter object
     */
    public AbstractFilter init(JSONObject input, DocumentQuery parent, QueryExpression parentExpression)
    {
        if (input.has(AbstractFilter.PARENT_LEVEL_KEY)) {
            this.reference = true;
            this.parent = getParent(parent,
                Integer.valueOf(SearchUtils.getValue(input, AbstractFilter.PARENT_LEVEL_KEY)));
        } else {
            this.parent = parent;
        }

        this.parentExpression = parentExpression;

        if (!input.has(SpaceAndClass.CLASS_KEY)) {
            throw new IllegalArgumentException(String.format("[%s] key not present", SpaceAndClass.CLASS_KEY));
        }

        this.negate = SearchUtils.BOOLEAN_TRUE_SET.contains(SearchUtils.getValue(input, AbstractFilter.NOT_KEY));

        this.joinMode = StringUtils.lowerCase(input.optString(AbstractFilter.JOIN_MODE_KEY));

        if (!StringUtils.equals(this.joinMode, AbstractFilter.JOIN_MODE_VALUE_AND)
            && !StringUtils.equals(this.joinMode, AbstractFilter.JOIN_MODE_VALUE_OR)) {
            this.joinMode = JOIN_MODE_DEFAULT_VALUE;
        }

        this.validatesQuery = SearchUtils.BOOLEAN_TRUE_SET.contains(
            SearchUtils.getValue(input, VALIDATES_QUERY_KEY, CollectionUtils.get(SearchUtils.BOOLEAN_TRUE_SET, 0)));


        this.spaceAndClass = new SpaceAndClass(input);
        this.propertyName = new PropertyName(input, this.getTableName());

        this.handleRefValues(input);

        return this;
    }

    @Override
    public QueryElement createBindings()
    {
        if (this.isValid()) {
            this.parent.addPropertyBinding(this.spaceAndClass, this.propertyName);

            for (AbstractFilter refValue : this.refValues) {
                refValue.createBindings();
            }
        }
        return this;
    }

    /**
     * Getter for expression.
     *
     * @return expression
     */
    public QueryExpression getParentExpression()
    {
        return this.parentExpression;
    }

    /**
     * This method binds the name of the filter property and the class of the base object the property belongs to
     * the query.
     *
     * (extraObj.className=PhenoTips.VisibilityClass and extraObj_visibility.id.name=visibility)
     *
     * @param where the query buffer to add too
     * @param bindingValues the binding value list to add values to
     * @return the given QueryBuffer
     */
    public QueryBuffer bindPropertyClass(QueryBuffer where, List<Object> bindingValues)
    {
        if (this.isDocumentProperty()) {
            return where;
        }

        String baseObj;

        if (this.getParentExpression().isOrMode()) {
            baseObj = this.parent.getObjectName(this.getParentExpression().getSpaceAndClass());
        } else {
            baseObj = this.parent.getObjectName(this.spaceAndClass);
        }

        where.appendOperator().append(" ").append(baseObj).append(".className=? and ");
        where.append(this.getPropertyNameForQuery()).append(".id.name=? ");

        bindingValues.add(this.spaceAndClass.get());
        bindingValues.add(this.propertyName.get());

        return where;
    }

    /**
     * Starts a new expression block. It appends the current operator (if needed) and a opening bracket: '('
     * It also resets and saves the operator of the buffer (since right after a starting bracket we don't need any
     * operators).
     *
     * If this filter is not a document property filter, this method also calls the bindPropertyClass method
     * for convenience.
     *
     * @param where the query buffer to add too
     * @param bindingValues the binding value list to add values to
     * @return the given QueryBuffer
     */
    public QueryBuffer startElement(QueryBuffer where, List<Object> bindingValues)
    {
        where.appendOperator().saveAndReset().startGroup();

        if (this.isDocumentProperty()) {
            return where;
        }

        this.bindPropertyClass(where, bindingValues).append(" and ").startGroup();

        return where;
    }

    /**
     * This method ends an expression block opened with the startElement method. It appends a closing bracket: ')'
     *
     * This method also returns the operator to the value saved by the startElement method.
     * @param where the query buffer to add too
     * @return the given QueryBuffer
     */
    public QueryBuffer endElement(QueryBuffer where)
    {
        where.load();
        if (this.isDocumentProperty()) {
            return where.endGroup();
        } else {
            return where.endGroup().endGroup();
        }
    }

    /**
     * Returns the document name of the parent query this filter belongs to.
     * @return the document name
     */
    public String getDocName()
    {
        return this.parent.getDocName();
    }

    /**
     * This method returns the alias of the property of this filter to be used in the query.
     * Example: extraObj_visibility
     *
     * @return the the property name to be used in the query
     */
    public String getPropertyNameForQuery()
    {
        if (this.getParentExpression().isOrMode()) {
            return getPropertyNameForQuery(
                this.getParentExpression().getPropertyName(), this.getParentExpression().getSpaceAndClass(),
                this.parent, 0);
        } else {
            return getPropertyNameForQuery(this.propertyName, this.spaceAndClass, this.parent, 0);
        }
    }

    /**
     * This method returns the alias of the property value to be used in the query. Usually this means getting
     * the property alias name via the getPropertyNameForQuery() and appending .value to it. Some filter may override
     * this method if needed. Also if the property of this filter is a document property, the value alias will be
     * the same as just the property name alias (no .value is added).
     *
     * Example: extraObj_visibility.value
     *          fullName (in case of document property)
     *
     * @return the property value name
     */
    public String getPropertyValueNameForQuery()
    {
        return getPropertyValueNameForQuery(this.getPropertyNameForQuery(), this.isDocumentProperty());
    }


    /**
     * Getter for refValues.
     *
     * @return refValues
     */
    public List<AbstractFilter> getRefValues()
    {
        return this.refValues;
    }

    /**
     * Getter for propertyName.
     *
     * @return propertyName
     */
    public PropertyName getPropertyName()
    {
        return this.propertyName;
    }

    @Override
    public boolean isValid()
    {
        boolean hasMinOrMax = this.min != null || this.max != null;
        boolean hasValues = CollectionUtils.isNotEmpty(this.values) || CollectionUtils.isNotEmpty(this.refValues);
        return hasValues || hasMinOrMax || this.isReference();
    }

    @Override
    public boolean validatesQuery()
    {
        return this.validatesQuery;
    }


    /**
     * Getter for joinMode.
     *
     * @return joinMode
     */
    public String getJoinMode()
    {
        return this.joinMode;
    }

    /**
     * Getter for encrypted.
     *
     * @return encrypted
     */
    public boolean isEncrypted()
    {
        return this.property instanceof EncryptedClass;
    }

    /**
     * Adds the given value to the values list. If the value is null it will not be added.
     * @param value the value to add (if null will be ignored)
     */
    public void addValue(T value)
    {
        if (value != null) {
            this.values.add(value);
        }
    }

    /**
     * Adds a null to the values list.
     */
    public void addNullValue()
    {
        this.values.add(null);
    }

    /**
     * Getter for reference.
     *
     * @return reference
     */
    public boolean isReference()
    {
        return this.reference;
    }

    /**
     * Getter for spaceAndClass.
     *
     * @return spaceAndClass
     */
    public SpaceAndClass getSpaceAndClass()
    {
        return this.spaceAndClass;
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
     * Getter for tableName.
     *
     * @return tableName
     */
    public String getTableName()
    {
        return this.tableName;
    }

    /**
     * Setter for tableName.
     *
     * @param tableName tableName to set
     * @return this object
     */
    public AbstractFilter setTableName(String tableName)
    {
        if (!this.isEncrypted()) {
            this.tableName = tableName;
        }
        return this;
    }

    /**
     * Getter for level.
     *
     * @return level
     */
    public int getLevel()
    {
        return this.level;
    }

    /**
     * Setter for level.
     *
     * @param level level to set
     * @return this object
     */
    public AbstractFilter setLevel(int level)
    {
        this.level = level;
        return this;
    }


    /**
     * Getter for negate.
     *
     * @return negate
     */
    public boolean isNegate()
    {
        return this.negate;
    }

    /**
     * Setter for negate.
     *
     * @param negate negate to set
     * @return this object
     */
    public AbstractFilter setNegate(boolean negate)
    {
        this.negate = negate;
        return this;
    }

    /**
     * Getter for property.
     *
     * @return property
     */
    public PropertyInterface getProperty()
    {
        return this.property;
    }

    /**
     * Setter for property.
     *
     * @param property property to set
     * @return this object
     */
    public AbstractFilter setProperty(PropertyInterface property)
    {
        this.property = property;
        return this;
    }

    /**
     * Getter for baseClass.
     *
     * @return baseClass
     */
    public BaseClass getBaseClass()
    {
        return this.baseClass;
    }

    /**
     * Setter for baseClass.
     *
     * @param baseClass baseClass to set
     * @return this object
     */
    public AbstractFilter setBaseClass(BaseClass baseClass)
    {
        this.baseClass = baseClass;
        return this;
    }

    /**
     * Getter for min.
     *
     * @return min
     */
    public T getMin()
    {
        return this.min;
    }

    /**
     * Setter for min.
     *
     * @param min min to set
     * @return this object
     */
    public AbstractFilter setMin(T min)
    {
        this.min = min;
        return this;
    }

    /**
     * Getter for max.
     *
     * @return max
     */
    public T getMax()
    {
        return this.max;
    }

    /**
     * Setter for max.
     *
     * @param max max to set
     * @return this object
     */
    public AbstractFilter setMax(T max)
    {
        this.max = max;
        return this;
    }

    /**
     * Getter for values.
     *
     * @return values
     */
    public List<T> getValues()
    {
        return this.values;
    }

    /**
     * Setter for values.
     *
     * @param values values to set
     * @return this object
     */
    public AbstractFilter setValues(List<T> values)
    {
        if (values == null) {
            this.values = new LinkedList<>();
        } else {
            this.values = values;
        }

        return this;
    }

    /**
     * Getter for documentProperty.
     *
     * @return documentProperty
     */
    public boolean isDocumentProperty()
    {
        return this.propertyName.isDocumentProperty();
    }

    /**
     * Getter for VALUE_PROPERTY_NAMES.
     *
     * @return VALUE_PROPERTY_NAMES
     */
    public static List<String> getValueParameterNames()
    {
        return VALUE_PROPERTY_NAMES;
    }


    /**
     * This method returns the alias of the property value to be used in the query. If the property is a document
     * property the given propertyNameForQuery is simply returned unchanged, otherwise the returned value is
     * propertyNameForQuery.value.
     *
     * @param propertyNameForQuery the name of the property (can be retrieved using getPropertyNameForQuery() method)
     * @param isDocumentProperty flag determining whether or not to consider the property as a document property
     * @return the property value name
     */
    public static String getPropertyValueNameForQuery(String propertyNameForQuery, boolean isDocumentProperty)
    {
        if (isDocumentProperty) {
            return propertyNameForQuery;
        } else {
            return propertyNameForQuery + ".value";
        }
    }

    /**
     * Returns the parent query of this filter, given the starting parent and the numbers of levels to go up the chain.
     *
     * A level of '0' will return the given parent object. A level of '1' will return parent.getParent() and so on.
     *
     * @param parent the starting parent
     * @param levelsUp the number of levels to go up the chain
     * @return a DocumentQuery object
     */
    public static DocumentQuery getParent(DocumentQuery parent, int levelsUp)
    {
        DocumentQuery parentToReturn = parent;

        int curLevel = levelsUp;
        while (curLevel++ < 0 && parentToReturn != null) {
            parentToReturn = parentToReturn.getParent();
        }

        return parentToReturn;
    }

    /**
     * This method returns the alias of the property of this filter to be used in the query.
     *
     * @param propertyName the PropertyName name object to use for determining the alias
     * @param spaceAndClass the SpaceAndClass name object to use for determining the alias
     * @param parent the starting parent DocumentQuery to use
     * @param levelsUp the numbers of levels to go up the parent chain for retrieving the true parent of this filter
     *                  use 0 for regular situations. It must be a number <= 0.
     * @return the the property name to be used in the query
     */
    public static String getPropertyNameForQuery(PropertyName propertyName, SpaceAndClass spaceAndClass,
        DocumentQuery parent, int levelsUp)
    {
        StringBuilder name = new StringBuilder();

        if (propertyName.isDocumentProperty()) {
            name.append(getParent(parent, levelsUp).getDocName()).append(".").append(propertyName.get());
        } else {
            name.append(getParent(parent, levelsUp).getObjNameMap().get(spaceAndClass)).append("_");
            name.append(propertyName.get());
        }

        return name.toString();
    }

    private void handleRefValues(JSONObject input)
    {
        for (Object refValueObj : SearchUtils.getJSONArray(input, AbstractFilter.REF_VALUES_KEY)) {

            if (!(refValueObj instanceof JSONObject)) {
                continue;
            }
            AbstractFilter filter = this.parent.getFilterFactory().getFilter((JSONObject) refValueObj);

            if (filter != null) {
                this.refValues.add(filter.init((JSONObject) refValueObj, this.parent, this.parentExpression));
            }
        }
    }
}
