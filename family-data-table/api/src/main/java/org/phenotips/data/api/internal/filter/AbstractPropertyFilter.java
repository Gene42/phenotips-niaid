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
    /** Prefix a doc property would have. */
    public static final String DOC_PROPERTY_PREFIX = "doc.";

    /** Param key. */
    public static final String PROPERTY_NAME_KEY = "property_name";

    /** Param key. */
    public static final String DOC_CLASS_KEY = "doc_class";

    /** Param key. */
    public static final String VALUES_KEY = "values";

    /** Param key. */
    public static final String SUBTERMS_KEY = "subterms";

    /** Logger, can be used by implementing classes. */
    public static final Logger LOGGER = LoggerFactory.getLogger(AbstractPropertyFilter.class);

    private int level;

    private String tableName;
    private String propertyName;
    private String documentPropertyName;
    private SpaceAndClass spaceAndClass;

    private boolean extended;
    private boolean negate;
    private boolean documentProperty;

    private DocumentQuery parent;
    private PropertyInterface property;
    private BaseClass baseClass;

    private T min;
    private T max;
    private List<T> values = new LinkedList<>();

    /**
     * Constructor.
     * @param property PropertyInterface
     * @param baseClass BaseClass
     */
    public AbstractPropertyFilter(PropertyInterface property, BaseClass baseClass)
    {
        this.property = property;
        this.baseClass = baseClass;
    }

    /**
     * Takes the given JSONObject and initializes the filter.
     * @param input JSONObject to use for getting necessary data for filter initialization
     * @param parent the parent document query this filter belongs to
     * @return this AbstractPropertyFilter object
     */
    public AbstractPropertyFilter populate(JSONObject input, DocumentQuery parent)
    {
        this.parent = parent;

        if (!input.has(SpaceAndClass.CLASS_KEY)) {
            throw new IllegalArgumentException(String.format("[%s] key not present", SpaceAndClass.CLASS_KEY));
        }

        this.spaceAndClass = new SpaceAndClass(input);

        String unsanitizedPropertyName = input.getString(PROPERTY_NAME_KEY);

        this.propertyName = DocumentSearchUtils.sanitizeForHql(unsanitizedPropertyName);

        if (isPropertyADocProperty(unsanitizedPropertyName)) {
            this.documentProperty = true;
            this.documentPropertyName =
                DocumentSearchUtils.sanitizeForHql(StringUtils.removeStart(this.propertyName, DOC_PROPERTY_PREFIX));
        }

        return this;
    }

    /**
     * Appends to the given StringBuilder any relevant hql terms belonging to the from block of the query.
     * @param from the StringBuilder to append to
     * @param bindingValues the list of values to add to
     * @return the same StringBuilder that was given
     */
    public StringBuilder fromHql(StringBuilder from, List<Object> bindingValues)
    {
        if (!this.isDocumentProperty()) {
            from.append(", ").append(this.tableName).append(" ");
            from.append(this.parent.getObjNameMap().get(this.spaceAndClass.get()));
            from.append("_").append(this.propertyName);
        }
        return from;
    }

    /**
     * Appends to the given StringBuilder any relevant hql terms belonging to the where block of the query.
     * @param where the StringBuilder to append to
     * @param bindingValues the list of values to add to
     * @return the same StringBuilder that was given
     */
    public StringBuilder whereHql(StringBuilder where, List<Object> bindingValues)
    {
        if (this.isDocumentProperty()) {
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

    public static boolean isPropertyADocProperty(String propertyName)
    {
       return StringUtils.startsWith(propertyName, DOC_PROPERTY_PREFIX);
    }

    public String getDocName()
    {
        return this.parent.getDocName();
    }

    public String getObjectPropertyName()
    {
        return this.parent.getObjNameMap().get(this.spaceAndClass.get()) + "_" + this.propertyName;
    }

    public String getDocumentPropertyName()
    {
        return this.getDocName() + "." + this.documentPropertyName;
    }

    public boolean isValid()
    {
        return CollectionUtils.isNotEmpty(this.values) || this.min != null || this.max != null;
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
     * Getter for propertyName.
     *
     * @return propertyName
     */
    public String getPropertyName()
    {
        return propertyName;
    }

    /**
     * Setter for propertyName.
     *
     * @param propertyName propertyName to set
     * @return this object
     */
    public AbstractPropertyFilter setPropertyName(String propertyName)
    {
        this.propertyName = propertyName;
        return this;
    }

    /**
     * Getter for extended.
     *
     * @return extended
     */
    public boolean isExtended()
    {
        return extended;
    }

    /**
     * Setter for extended.
     *
     * @param extended extended to set
     * @return this object
     */
    public AbstractPropertyFilter setExtended(boolean extended)
    {
        this.extended = extended;
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
        this.values = values;
        return this;
    }

    /**
     * Getter for documentProperty.
     *
     * @return documentProperty
     */
    public boolean isDocumentProperty()
    {
        return documentProperty;
    }

    /**
     * Setter for documentProperty.
     *
     * @param documentProperty documentProperty to set
     * @return this object
     */
    public AbstractPropertyFilter setDocumentProperty(boolean documentProperty)
    {
        this.documentProperty = documentProperty;
        return this;
    }

    /**
     * Setter for spaceAndClass.
     *
     * @param spaceAndClass spaceAndClass to set
     * @return this object
     */
    public AbstractPropertyFilter setSpaceAndClass(SpaceAndClass spaceAndClass)
    {
        this.spaceAndClass = spaceAndClass;
        return this;
    }

    /**
     * Setter for documentPropertyName.
     *
     * @param documentPropertyName documentPropertyName to set
     * @return this object
     */
    public AbstractPropertyFilter setDocumentPropertyName(String documentPropertyName)
    {
        this.documentPropertyName = documentPropertyName;
        return this;
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
}
