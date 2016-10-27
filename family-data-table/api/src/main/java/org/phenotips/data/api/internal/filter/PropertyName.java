package org.phenotips.data.api.internal.filter;

import org.phenotips.data.api.internal.DocumentUtils;

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
    private boolean documentProperty;
    private boolean extended;

    public PropertyName(JSONObject input)

    {
        String unsanitizedPropertyName = input.getString(PROPERTY_NAME_KEY);

        this.value = sanitizeForHql(unsanitizedPropertyName);

        if (isDocProperty(unsanitizedPropertyName)) {
            this.value = StringUtils.removeStart(unsanitizedPropertyName, DOC_PROPERTY_PREFIX);
            this.documentProperty = true;
        }

        this.value = sanitizeForHql(this.value);

        this.extended = DocumentUtils.BOOLEAN_TRUE_SET.contains(String.valueOf(input.opt(SUBTERMS_KEY)));

        if (this.extended) {
            this.value = EXTENDED_PREFIX + this.value;
        }
    }

    /**
     * Getter for value.
     *
     * @return value
     */
    public String get()
    {
        return value;
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

    public static boolean isValid(PropertyName propertyName)
    {
        return propertyName != null && StringUtils.isNotBlank(propertyName.value);
    }
}
