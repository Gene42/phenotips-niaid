/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal.filter;

import org.phenotips.data.api.internal.SpaceAndClass;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.json.JSONArray;
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
public abstract class AbstractPropertyFilter<T>
{
    /** Param key. */
    public static final String DOC_CLASS_KEY = "doc_class";

    /** Param key. */
    public static final String VALUES_KEY = "values";

    /** Param key. */
    public static final String REF_VALUES_KEY = "ref_values";

    /** Logger, can be used by implementing classes. */
    public static final Logger LOGGER = LoggerFactory.getLogger(AbstractPropertyFilter.class);

    private int level;

    private String tableName;

    private PropertyName propertyName;
    private SpaceAndClass spaceAndClass;

    private boolean negate;
    private DocumentQuery parent;
    private PropertyInterface property;
    private BaseClass baseClass;

    private T min;
    private T max;
    private List<T> values = new LinkedList<>();
    private List<ReferenceValue> refValues = new LinkedList<>();

    private boolean reference;

    /**
     * Constructor.
     * @param property PropertyInterface
     * @param baseClass BaseClass
     */
    public AbstractPropertyFilter(PropertyInterface property, BaseClass baseClass)
    {
        this(property, baseClass, null);
    }

    /**
     * Constructor.
     * @param property PropertyInterface
     * @param baseClass BaseClass
     * @param tableName the object type of the property
     */
    public AbstractPropertyFilter(PropertyInterface property, BaseClass baseClass, String tableName)
    {
        this.property = property;
        this.baseClass = baseClass;
        this.tableName = tableName;
    }

    /**
     * Takes the given JSONObject and initializes the filter.
     * @param input JSONObject to use for getting necessary data for filter initialization
     * @param parent the parent document query this filter belongs to
     * @return this AbstractPropertyFilter object
     */
    public AbstractPropertyFilter init(JSONObject input, DocumentQuery parent)
    {
        this.parent = parent;

        if (!input.has(SpaceAndClass.CLASS_KEY)) {
            throw new IllegalArgumentException(String.format("[%s] key not present", SpaceAndClass.CLASS_KEY));
        }

        this.spaceAndClass = new SpaceAndClass(input);
        this.propertyName = new PropertyName(input, this.tableName);

        this.handleRefValues(input);

        parent.addPropertyBinding(this.spaceAndClass, this.propertyName);

        return this;
    }


    /*public StringBuilder fromHql(StringBuilder from, List<Object> bindingValues)
    {
        if (!this.isDocumentProperty()) {
            from.append(", ").append(this.tableName).append(" ");
            from.append(this.parent.getObjNameMap().get(this.spaceAndClass.get()));
            from.append("_").append(this.propertyName.get());
        }
        return from;
    }*/

    /**
     * Appends to the given StringBuilder any relevant hql terms belonging to the where block of the query.
     * @param where the StringBuilder to append to
     * @param bindingValues the list of values to add to
     * @return the same StringBuilder that was given
     */
    public StringBuilder whereHql(StringBuilder where, List<Object> bindingValues)
    {
        if (this.isDocumentProperty() || this.parent.containsPropertyBinding(this.spaceAndClass, this.propertyName)) {
            return where;
        }

        String baseObj = this.parent.getObjectName(this.spaceAndClass);

        // NOTE: getSafeAlias not the best solution, I might use random strings
        String objPropName = this.getPropertyNameForQuery();
        where.append(" ").append(baseObj).append(".className=? and ");
        where.append(baseObj).append(".name=").append(this.getDocName()).append(".fullName and ");
        where.append(baseObj).append(".id=").append(objPropName).append(".id.id and ");
        where.append(objPropName).append(".id.name=? ");

        bindingValues.add(this.spaceAndClass.get());
        bindingValues.add(this.propertyName.get());

        return where.append(" and ");
    }

    public static StringBuilder whereHqlForPropertyBinding(StringBuilder where, List<Object> bindingValues, SpaceAndClass spaceAndClass, DocumentQuery parent)
    {
        String baseObj = parent.getObjectName(spaceAndClass);

        // NOTE: getSafeAlias not the best solution, I might use random strings
        String objPropName = this.getPropertyNameForQuery();
        where.append(" ").append(baseObj).append(".className=? and ");
        where.append(baseObj).append(".name=").append(this.getDocName()).append(".fullName and ");
        where.append(baseObj).append(".id=").append(objPropName).append(".id.id and ");
        where.append(objPropName).append(".id.name=? ");

        bindingValues.add(this.spaceAndClass.get());
        bindingValues.add(this.propertyName.get());
    }

    public String getDocName()
    {
        return this.parent.getDocName();
    }

    public String getPropertyNameForQuery(PropertyName propertyName, SpaceAndClass spaceAndClass, int levelsUp)
    {
        StringBuilder name = new StringBuilder();

        if (this.isDocumentProperty()) {
            name.append(this.getParent(levelsUp).getDocName()).append(".").append(propertyName.get());
        } else {
            name.append(this.getParent(levelsUp).getObjNameMap().get(spaceAndClass.get())).append("_");
            name.append(propertyName.get());
        }

        return name.toString();
    }

    public DocumentQuery getParent(int levelsUp)
    {
        DocumentQuery parentToReturn = this.parent;

        int curLevel = levelsUp;
        while (curLevel++ < 0 && parentToReturn != null) {
            parentToReturn = parentToReturn.getParent();
        }

        return parentToReturn;
    }

    public String getPropertyNameForQuery()
    {
        return this.getPropertyNameForQuery(this.propertyName, this.spaceAndClass, 0);
    }

    public String getPropertyValueNameForQuery()
    {
        return getPropertyValueNameForQuery(this.getPropertyNameForQuery(), this.isDocumentProperty());
    }

    public static String getPropertyValueNameForQuery(String propertyNameForQuery, boolean isDocumentProperty)
    {
        if (isDocumentProperty) {
            return propertyNameForQuery;
        } else {
            return propertyNameForQuery + ".value";
        }
    }

    /*public String getPropertyNameForQuery(String objPrefix, String objSuffix, String docPrefix, String docSuffix)
    {
        StringBuilder name = new StringBuilder();
        if (this.isDocumentProperty()) {

            if (StringUtils.isNotBlank(docPrefix)) {
                name.append(docPrefix);
            }

            name.append(this.getDocName()).append(".").append(this.propertyName.get());

            if (StringUtils.isNotBlank(docSuffix)) {
                name.append(docSuffix);
            }
        } else {

            if (StringUtils.isNotBlank(objPrefix)) {
                name.append(objPrefix);
            }

            name.append(this.parent.getObjNameMap().get(this.spaceAndClass.get())).append("_");
            name.append(this.propertyName.get());

            if (StringUtils.isNotBlank(objSuffix)) {
                name.append(objSuffix);
            }
        }

        return name.toString();
    }*/

    //public String getPropertyNameForQuery()
    //{
    //    return  this.getPropertyNameForQuery(null, null, null, null);
    //}

    /**
     * Getter for propertyName.
     *
     * @return propertyName
     */
    public PropertyName getPropertyName()
    {
        return propertyName;
    }

    public boolean isValid()
    {
        return CollectionUtils.isNotEmpty(this.values)
            || this.min != null
            || this.max != null
            || CollectionUtils.isNotEmpty(this.refValues);
    }

    /**
     * Adds the given value to the values list. If the value is null it will not be added.
     * @param value the value to add (if null will be ignored)
     */
    public void addValue(T value) {
        if (value != null) {
            this.values.add(value);
        }
    }

    public StringBuilder appendQueryOperator(StringBuilder buffer, String operator, int valuesIndex)
    {
        if (valuesIndex > 0) {
            buffer.append(" ").append(operator).append(" ");
        }

        return buffer;
    }

    /**
     * Getter for reference.
     *
     * @return reference
     */
    public boolean isReference()
    {
        return reference;
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
     * Getter for tableName.
     *
     * @return tableName
     */
    public String getTableName()
    {
        return tableName;
    }

    /**
     * Setter for tableName.
     *
     * @param tableName tableName to set
     * @return this object
     */
    public AbstractPropertyFilter setTableName(String tableName)
    {
        this.tableName = tableName;
        return this;
    }

    /**
     * Getter for level.
     *
     * @return level
     */
    public int getLevel()
    {
        return level;
    }

    /**
     * Setter for level.
     *
     * @param level level to set
     * @return this object
     */
    public AbstractPropertyFilter setLevel(int level)
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
        return negate;
    }

    /**
     * Setter for negate.
     *
     * @param negate negate to set
     * @return this object
     */
    public AbstractPropertyFilter setNegate(boolean negate)
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
        return property;
    }

    /**
     * Setter for property.
     *
     * @param property property to set
     * @return this object
     */
    public AbstractPropertyFilter setProperty(PropertyInterface property)
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
        return baseClass;
    }

    /**
     * Setter for baseClass.
     *
     * @param baseClass baseClass to set
     * @return this object
     */
    public AbstractPropertyFilter setBaseClass(BaseClass baseClass)
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
        return min;
    }

    /**
     * Setter for min.
     *
     * @param min min to set
     * @return this object
     */
    public AbstractPropertyFilter setMin(T min)
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
        return max;
    }

    /**
     * Setter for max.
     *
     * @param max max to set
     * @return this object
     */
    public AbstractPropertyFilter setMax(T max)
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
        return values;
    }

    /**
     * Setter for values.
     *
     * @param values values to set
     * @return this object
     */
    public AbstractPropertyFilter setValues(List<T> values)
    {
        if (values == null) {
            this.values = new LinkedList<>();
        } else {
            this.values = values;
        }

        return this;
    }

    /**
     * Getter for refValues.
     *
     * @return refValues
     */
    public List<ReferenceValue> getRefValues()
    {
        return refValues;
    }

    /**
     * Setter for refValues.
     *
     * @param refValues refValues to set
     * @return this object
     */
    public AbstractPropertyFilter setRefValues(List<ReferenceValue> refValues)
    {
        this.refValues = refValues;
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
     * Setter for parent.
     *
     * @param parent parent to set
     * @return this object
     */
    public AbstractPropertyFilter setParent(DocumentQuery parent)
    {
        this.parent = parent;
        return this;
    }

    public static JSONArray getJSONArray(JSONObject inputJSONObj, String key)
    {
        Object valueObj = inputJSONObj.opt(key);

        JSONArray toReturn = null;

        if (valueObj == null) {
            toReturn = new JSONArray();
        } else if (valueObj instanceof JSONArray) {
            toReturn = (JSONArray) valueObj;
        } else if (valueObj instanceof JSONObject) {
            toReturn = new JSONArray();
            toReturn.put(valueObj);
        }

        return toReturn;
    }

    public static List<String> getValues(JSONObject inputJSONObj, String key) {

        Object valueObj = inputJSONObj.opt(key);

        List<String> values = new LinkedList<>();

        if (valueObj == null) {
            return values;
        }

        if (valueObj instanceof JSONArray) {
            JSONArray valuesArray = (JSONArray) valueObj;
            for (Object objValue : valuesArray) {
                if (objValue instanceof String) {
                    values.add((String) objValue);
                } else {
                    values.add(String.valueOf(objValue));
                }
            }
        } else if (valueObj instanceof String) {
            values.add((String) valueObj);
        }

        return values;
    }

    public static String getValue(JSONObject inputJSONObj, String key) {

        if (inputJSONObj == null) {
            return null;
        }

        Object input = inputJSONObj.opt(key);

        if (input == null) {
            return null;
        }

        if (input instanceof JSONArray) {
            JSONArray valuesArray = (JSONArray) input;
            if (valuesArray.length() == 0) {
                return null;
            }
            else {
                return String.valueOf(valuesArray.get(0));
            }
        } else if (input instanceof String) {
            return (String) input;
        } else {
            return null;
        }
    }

    private void handleRefValues(JSONObject input)
    {
        JSONArray refValueArray = getJSONArray(input, REF_VALUES_KEY);
        if (refValueArray == null) {
            return;
        }

        for (Object refValueObj : refValueArray) {
            if (!(refValueObj instanceof JSONObject)) {
                continue;
            }
            this.refValues.add(new ReferenceValue((JSONObject) refValueObj, this.level));
        }
    }
}
