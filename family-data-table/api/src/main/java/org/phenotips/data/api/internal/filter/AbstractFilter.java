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
import org.phenotips.data.api.internal.SearchUtils;
import org.phenotips.data.api.internal.SpaceAndClass;
import org.phenotips.security.encryption.internal.EncryptedClass;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
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
public abstract class AbstractFilter<T>
{
    /** Param key. */
    public static final String DOC_CLASS_KEY = "doc_class";

    /** Param key. */
    public static final String VALUES_KEY = "values";

    /** Param key. */
    public static final String TYPE_KEY = "type";

    /** Param key. */
    public static final String REF_VALUES_KEY = "ref_values";

    /** Param key. */
    public static final String PARENT_LEVEL_KEY = "parent_level";

    /** Logger, can be used by implementing classes. */
    public static final Logger LOGGER = LoggerFactory.getLogger(AbstractFilter.class);

    public static final List<String> VALUE_PROPERTY_NAMES = ListUtils.unmodifiableList(
        Arrays.asList(AbstractFilter.VALUES_KEY, AbstractFilter.REF_VALUES_KEY)
    );


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
    private List<AbstractFilter> refValues = new LinkedList<>();

    private boolean reference;

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
     * @return this AbstractPropertyFilter object
     */
    public AbstractFilter init(JSONObject input, DocumentQuery parent)
    {
        if (input.has(AbstractFilter.PARENT_LEVEL_KEY)) {
            this.reference = true;
            this.parent = getParent(parent,
                Integer.valueOf(SearchUtils.getValue(input, AbstractFilter.PARENT_LEVEL_KEY)));
        } else {
            this.parent = parent;
        }

        if (!input.has(SpaceAndClass.CLASS_KEY)) {
            throw new IllegalArgumentException(String.format("[%s] key not present", SpaceAndClass.CLASS_KEY));
        }

        this.spaceAndClass = new SpaceAndClass(input);
        this.propertyName = new PropertyName(input, this.getTableName());

        this.handleRefValues(input);

        return this;
    }
    
    public AbstractFilter createBindings()
    {
        if (this.isValid()) {
            this.parent.addPropertyBinding(this.spaceAndClass, this.propertyName);

            if (this.isReference()) {
                this.parent.addToReferencedProperties(this);
            }

            for (AbstractFilter refValue : this.refValues) {
                refValue.createBindings();
            }
        }
        return this;
    }

    /**
     * Appends to the given StringBuilder any relevant hql terms belonging to the where block of the query.
     * @param whereHql the StringBuilder to append to
     * @param bindingValues the list of values to add to
     * @return the same StringBuilder that was given
     */
    public StringBuilder addValueConditions(StringBuilder whereHql, List<Object> bindingValues)
    {
        return whereHql.append(" and ");
    }

    public StringBuilder bindProperty(StringBuilder where, List<Object> bindingValues)
    {
        if (this.isDocumentProperty()) {
            return where;
        }

        String baseObj = this.parent.getObjectName(this.spaceAndClass);

        String objPropName = this.getPropertyNameForQuery();
        where.append(" and ").append(baseObj).append(".id=").append(objPropName).append(".id.id and ");
        where.append(objPropName).append(".id.name=? ");

        bindingValues.add(this.propertyName.get());

        return where;
    }

    public String getDocName()
    {
        return this.parent.getDocName();
    }

    public String getPropertyNameForQuery(PropertyName propertyName, SpaceAndClass spaceAndClass, int levelsUp)
    {
        StringBuilder name = new StringBuilder();

        if (this.isDocumentProperty()) {
            name.append(getParent(this.parent, levelsUp).getDocName()).append(".").append(propertyName.get());
        } else {
            name.append(getParent(this.parent, levelsUp).getObjNameMap().get(spaceAndClass.get())).append("_");
            name.append(propertyName.get());
        }

        return name.toString();
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

    public boolean isValid()
    {
        boolean hasMinOrMax = this.min != null || this.max != null;
        boolean hasValues = CollectionUtils.isNotEmpty(this.values) || CollectionUtils.isNotEmpty(this.refValues);
        return hasValues || hasMinOrMax || this.isReference();
    }

    public boolean validatesQuery()
    {
        return true;
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
    public void addValue(T value) {
        if (value != null) {
            this.values.add(value);
        }
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
        return level;
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
        return negate;
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
        return property;
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
        return baseClass;
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
        return min;
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
        return max;
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
        return values;
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
     * Setter for parent.
     *
     * @param parent parent to set
     * @return this object
     */
    /*public AbstractFilter setParent(DocumentQuery parent)
    {
        this.parent = parent;
        return this;
    }*/

    private void handleRefValues(JSONObject input)
    {
        JSONArray refValueArray = SearchUtils.getJSONArray(input, AbstractFilter.REF_VALUES_KEY);
        if (refValueArray == null) {
            return;
        }

        for (Object refValueObj : refValueArray) {
            if (!(refValueObj instanceof JSONObject)) {
                continue;
            }
            AbstractFilter filter = this.parent.getFilterFactory().getFilter((JSONObject) refValueObj);

            if (filter != null) {
                this.refValues.add(filter.init((JSONObject) refValueObj, this.parent));
            }
        }
    }
}
