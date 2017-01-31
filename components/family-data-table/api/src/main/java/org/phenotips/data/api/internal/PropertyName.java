/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

/**
 * This class is a container for a filter property and all its associated variables.
 *
 * @version $Id$
 */
public class PropertyName
{
    /** Prefix a doc property would have. */
    public static final String DOC_PROPERTY_PREFIX = "doc.";

    /** Param key. */
    public static final String PROPERTY_NAME_KEY = "property_name";

    /** Param key. */
    public static final String SUBTERMS_KEY = "subterms";

    /** Prefix for extended parameter names (a filter is extended if the subterms key is present and set to true). */
    public static final String EXTENDED_PREFIX = "extended_";

    /** The name of the property. */
    private String name;

    /** The type of the object (ex: StringProperty, IntegerProperty etc.). */
    private String objectType;

    /** Whether or not this is a document property. */
    private boolean documentProperty;

    /** (a filter is extended if the subterms key is present and set to true). */
    private boolean extended;

    /**
     * Constructor.
     * @param propertyName the property name to use
     * @param objectType the object type to use (ex: StringProperty, IntegerProperty etc.)
     */
    public PropertyName(String propertyName, String objectType)
    {
        if (propertyName == null) {
            throw new IllegalArgumentException("Property Name was not found.");
        }

        this.name = sanitizeForHql(propertyName);

        if (isDocProperty(propertyName)) {
            this.name = StringUtils.removeStart(propertyName, PropertyName.DOC_PROPERTY_PREFIX);
            this.documentProperty = true;
        }

        this.name = sanitizeForHql(this.name);

        this.objectType = objectType;
    }

    /**
     * Constructor.
     * @param input input object
     * @param objectType the type of this property's object (ex: StringProperty, IntegerProperty etc.)
     */
    public PropertyName(JSONObject input, String objectType)

    {
        this(SearchUtils.getValue(input, PropertyName.PROPERTY_NAME_KEY), objectType);

        this.extended = SearchUtils.BOOLEAN_TRUE_SET.contains(String.valueOf(input.opt(PropertyName.SUBTERMS_KEY)));

        if (this.extended) {
            this.name = PropertyName.EXTENDED_PREFIX + this.name;
        }
    }

    /**
     * Getter for name.
     *
     * @return name
     */
    public String get()
    {
        return this.name;
    }

    /**
     * Getter for objectType.
     *
     * @return objectType
     */
    public String getObjectType()
    {
        return this.objectType;
    }

    @Override
    public int hashCode()
    {
        return this.name.hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == null || !(o instanceof PropertyName)) {
            return false;
        } else {
            return this.name.equals(((PropertyName) o).name);
        }
    }

    /**
     * Getter for extended.
     *
     * @return extended
     */
    public boolean isExtended()
    {
        return this.extended;
    }

    /**
     * Getter for documentProperty.
     *
     * @return documentProperty
     */
    public boolean isDocumentProperty()
    {
        return this.documentProperty;
    }

    /**
     * Sanitizes the given string for hql use.
     * @param string the String to sanitize
     * @return an hql sanitized string
     */
    public static String sanitizeForHql(String string)
    {
        return StringUtils.replace(string, "[^a-zA-Z0-9_.]", "");
    }

    /**
     * Returns true of the given property name starts with 'doc.', false otherwise.
     *
     * @param propertyName the property name string to check
     * @return boolean value
     */
    public static boolean isDocProperty(String propertyName)
    {
        return StringUtils.startsWith(propertyName, DOC_PROPERTY_PREFIX);
    }

    /**
     * Returns true of the given PropertyName is a document property, false otherwise.
     *
     * @param propertyName the PropertyName to check
     * @return boolean value
     */
    public static boolean isDocProperty(PropertyName propertyName)
    {
        return propertyName.isDocumentProperty();
    }
}
