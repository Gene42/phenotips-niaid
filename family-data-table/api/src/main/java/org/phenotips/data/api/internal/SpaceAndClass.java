/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal;

import org.json.JSONObject;

/**
 * Container for PT Space and Class.
 *
 * @version $Id$
 */
public class SpaceAndClass
{
    /** Class property key */
    public static final String CLASS_KEY = "class";

    private final String spaceAndClassName;

    private final String spaceName;

    private final String className;

    /**
     * Constructor.
     * @param input input object
     */
    public SpaceAndClass(JSONObject input)
    {
        if (!input.has(CLASS_KEY)) {
            throw new IllegalArgumentException(String.format("[%s] key not present", CLASS_KEY));
        }

        this.spaceAndClassName = input.getString(CLASS_KEY);

        String [] tokens = DocumentSearchUtils.getSpaceAndClass(this.spaceAndClassName);

        if (tokens.length != 2) {
            throw new IllegalArgumentException(
                String.format("Invalid [%1$s] format: [%2$s]", CLASS_KEY, this.spaceAndClassName));
        }

        this.spaceName = tokens[0];
        this.className = tokens[1];
    }

    /**
     * Getter for spaceAndClassName.
     *
     * @return spaceAndClassName
     */
    public String get()
    {
        return spaceAndClassName;
    }

    /**
     * Getter for spaceName.
     *
     * @return spaceName
     */
    public String getSpaceName()
    {
        return spaceName;
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
}
