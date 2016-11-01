/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal;

import org.phenotips.Constants;
import org.phenotips.data.api.DocumentSearch;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

/**
 * Container for PhenoTips Space and Class.
 *
 * @version $Id$
 */
public class SpaceAndClass
{
    /** Class property key. */
    public static final String CLASS_KEY = DocumentSearch.CLASS_KEY;

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

        String [] tokens = getSpaceAndClass(this.spaceAndClassName);

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

    /**
     * Returns whether or not the given object is valid.
     * @param spaceAndClass the object to inspect (null input returns false)
     * @return true if valid false otherwise
     */
    public static boolean isValid(SpaceAndClass spaceAndClass)
    {
        return spaceAndClass != null && StringUtils.isNotBlank(spaceAndClass.get());
    }

    /**
     * Returns a String array with the space at index 0 and class at index 1.
     * @param classAndSpace the string to split
     * @return a String array
     */
    public static String [] getSpaceAndClass(String classAndSpace)
    {
        if (StringUtils.isBlank(classAndSpace)) {
            throw new IllegalArgumentException("class provided is null/empty");
        }

        String [] tokens = StringUtils.split(classAndSpace, ".");

        if (tokens.length == 2) {
            return tokens;
        }
        else {
            return new String [] { Constants.CODE_SPACE, tokens[0] };
        }
    }
}
