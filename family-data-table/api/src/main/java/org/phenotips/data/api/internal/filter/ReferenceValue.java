package org.phenotips.data.api.internal.filter;

import org.phenotips.data.api.internal.SpaceAndClass;

import org.json.JSONObject;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class ReferenceValue
{

    /** Property key. */
    public static final String PARENT_LEVEL_KEY = "parent_level";

    private SpaceAndClass spaceAndClass;
    private PropertyName propertyName;
    private int parentLevel;

    /**
     * Constructor.
     * @param input input object
     * @param currentLevel current level of the fitter
     */
    public ReferenceValue(JSONObject input, int currentLevel)
    {
        this.parentLevel = Integer.parseInt(input.getString(PARENT_LEVEL_KEY));

        if (this.parentLevel > 0) {
            throw new IllegalArgumentException(
                String.format("Level for reference value [%1$s] is grater than 0.", input));
        }

        if (this.parentLevel + currentLevel < 0) {
            throw new IllegalArgumentException(
                String.format("Level for reference value [%1$s] is too great, " +
                    "no parent exists at that level. Current level is [%2$s]", input, currentLevel));
        }

        this.spaceAndClass = new SpaceAndClass(input);
        this.propertyName = new PropertyName(input);
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
     * Getter for propertyName.
     *
     * @return propertyName
     */
    public PropertyName getPropertyName()
    {
        return propertyName;
    }

    /**
     * Getter for parentLevel.
     *
     * @return parentLevel
     */
    public int getParentLevel()
    {
        return parentLevel;
    }
}
