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
 * DESCRIPTION.
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

    private String value;
    private String objectType;
    private boolean documentProperty;
    private boolean extended;

    /**
     * Constructor.
     * @param input input object
     * @param objectType the type of this property's object
     */
    public PropertyName(JSONObject input, String objectType)

    {
        if (!input.has(PropertyName.PROPERTY_NAME_KEY)) {
            throw new IllegalArgumentException("Property Name was not found.");
        }

        String unSanitizedPropertyName = input.getString(PropertyName.PROPERTY_NAME_KEY);

        this.value = sanitizeForHql(unSanitizedPropertyName);

        if (isDocProperty(unSanitizedPropertyName)) {
            this.value = StringUtils.removeStart(unSanitizedPropertyName, PropertyName.DOC_PROPERTY_PREFIX);
            this.documentProperty = true;
        }

        this.value = sanitizeForHql(this.value);

        this.extended = SearchUtils.BOOLEAN_TRUE_SET.contains(String.valueOf(input.opt(PropertyName.SUBTERMS_KEY)));

        if (this.extended) {
            this.value = PropertyName.EXTENDED_PREFIX + this.value;
        }

        this.objectType = objectType;
    }

    /**
     * Getter for value.
     *
     * @return value
     */
    public String get()
    {
        return this.value;
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
        return this.value.hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == null || !(o instanceof PropertyName)) {
            return false;
        } else {
            return this.value.equals(((PropertyName) o).value);
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

    public boolean isDocumentProperty()
    {
        return this.documentProperty;
    }

    public static String sanitizeForHql(String string) {
        return StringUtils.replace(string, "[^a-zA-Z0-9_.]", "");
    }

    public static boolean isDocProperty(String propertyName)
    {
        return StringUtils.startsWith(propertyName, DOC_PROPERTY_PREFIX);
    }

    public static boolean isDocProperty(PropertyName propertyName)
    {
        return propertyName.isDocumentProperty();
    }
}
