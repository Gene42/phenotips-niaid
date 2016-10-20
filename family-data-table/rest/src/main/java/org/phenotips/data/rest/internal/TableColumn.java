package org.phenotips.data.rest.internal;

import org.xwiki.model.EntityType;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class TableColumn
{
    public static final String TYPE_KEY = "type";

    public static final String CLASS_KEY = "class";

    public static final String PROPERTY_NAME_KEY = "propertyName";

    public static final String COLUMN_NAME_KEY = "colName";


    private EntityType type;

    private String colName;

    private String className;

    private String propertyName;

    public TableColumn populate(JSONObject obj)
    {
        this.type = EntityType.valueOf(StringUtils.upperCase(getProperty(obj, TYPE_KEY, false)));

        this.className = getProperty(obj, CLASS_KEY, EntityType.DOCUMENT.equals(this.type));

        this.colName = getProperty(obj, COLUMN_NAME_KEY, false);

        this.propertyName = getProperty(obj, PROPERTY_NAME_KEY, true);

        if (StringUtils.isBlank(this.propertyName)) {
            this.propertyName = this.colName;
        }

        return this;
    }

    /**
     * Getter for type.
     *
     * @return type
     */
    public EntityType getType()
    {
        return type;
    }

    /**
     * Getter for colName.
     *
     * @return colName
     */
    public String getColName()
    {
        return colName;
    }

    /**
     * Getter for className.
     *
     * @return className
     */
    public String getClassName()
    {
        return className;
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

    private static String getProperty(JSONObject obj, String key, boolean canBeBlank) {
        String propStr = obj.optString(key);
        if (StringUtils.isBlank(propStr) && !canBeBlank) {
            throw new IllegalArgumentException(String.format("No %1$s provided", key));
        }
        return propStr;
    }
}
