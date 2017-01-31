/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal;

import org.phenotips.Constants;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.set.UnmodifiableSet;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Utility class providing JSONObject retrieval functions and EntityReference helper methods.
 *
 * @version $Id$
 */
public final class SearchUtils
{

    /** Allowed values for boolean true. */
    public static final Set<String> BOOLEAN_TRUE_SET = UnmodifiableSet.unmodifiableSet(
        new HashSet<>(Arrays.asList("yes", "true", "1")));

    /** Allowed values for boolean false. */
    public static final Set<String> BOOLEAN_FALSE_SET = UnmodifiableSet.unmodifiableSet(
        new HashSet<>(Arrays.asList("no", "false", "0")));

    private SearchUtils()
    {
        // Private constructor for util class
    }

    /**
     * Returns a DocumentReference given a period delimited space and class name.
     * @param spaceAndClass space and class "Space.Class"
     * @return a DocumentReference (never null)
     */
    public static EntityReference getClassReference(String spaceAndClass)
    {

        String [] tokens = SpaceAndClass.getSpaceAndClass(spaceAndClass);

        if (tokens.length == 2) {
            return getClassReference(tokens[0], tokens[1]);
        } else {
            return new EntityReference(spaceAndClass, EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);
        }

    }

    /**
     * Returns an EntityReference given the space reference and className (the class of the document).
     * @param spaceRef the SpaceReference to use
     * @param className the class name to use
     * @return a EntityReference
     */
    public static EntityReference getClassReference(SpaceReference spaceRef, String className)
    {
        EntityReference reference = new EntityReference(className, EntityType.DOCUMENT);
        return new EntityReference(reference, spaceRef);
    }

    /**
     * Returns an EntityReference given the space name and className (the class of the document).
     * @param space the name of the space to use
     * @param className the class name to use
     * @return a EntityReference
     */
    public static EntityReference getClassReference(String space, String className)
    {
        SpaceReference parent = new SpaceReference(space, new WikiReference("xwiki"));
        return getClassReference(parent, className);
    }

    /**
     * Returns a DocumentReference given the space and class string. This string needs to be in the format
     * [space name].[class name]
     *
     * Example: PhenoTips.VisibilityClass
     *
     * @param spaceAndClass a period delimited space and class name string
     * @return a EntityReference
     */
    public static DocumentReference getClassDocumentReference(String spaceAndClass)
    {
        return new DocumentReference(getClassReference(spaceAndClass));
    }

    /**
     * Retrieves a JSONArray from the given JSONObject with the specified key. If the key is not found an empty
     * JSONArray object is returned. If the key exists but is not a JSONArray, then a new JSONArray is created
     * the object at the specified key is added to the new array, and the new array is returned. Null key values
     * do not get added to the array.
     *
     * @param inputJSONObj the JSONObject to look into
     * @param key the key where the array should be found within the given JSONObject
     * @return a JSONArray object (is never null)
     */
    public static JSONArray getJSONArray(JSONObject inputJSONObj, String key)
    {
        Object valueObj = inputJSONObj.opt(key);

        JSONArray toReturn = new JSONArray();

        if (valueObj instanceof JSONArray) {
            toReturn = (JSONArray) valueObj;
        } else if (valueObj != null) {
            toReturn.put(valueObj);
        }

        return toReturn;
    }

    /**
     * Retrieves the values from the given JSONObject at the specified key, and returns them in a String list.
     * If the key does not exist an empty list is returned. If the key is a JSONArray any string value in the array
     * will be added to the result list, and any non string values will be passed through String.valueOf() before
     * being added. If the key value is a single value, the resulting list will be of size one containing that
     * value passed through String.valueOf().
     *
     * @param inputJSONObj the JSONObject to look into
     * @param key the key where the values should be found within the given JSONObject
     * @return  a String list (is never null)
     */
    public static List<String> getValues(JSONObject inputJSONObj, String key)
    {
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
        } else {
            values.add(String.valueOf(valueObj));
        }

        return values;
    }

    /**
     * Retrieves the value from the given JSONObject at the specified key as a String. If the key is missing or the
     * value is null, null is returned. If the key is a JSONArray the first entry in the array is returned as a string
     * using String.valueOf(). All other objects are returned after passing through String.valueOf().
     *
     * @param inputJSONObj the JSONObject to look into
     * @param key the key where the value should be found within the given JSONObject
     * @return a String or null if key does not exist or is null
     */
    public static String getValue(JSONObject inputJSONObj, String key)
    {

        if (inputJSONObj == null) {
            return null;
        }

        Object input = inputJSONObj.opt(key);

        String returnValue;

        if (input == null) {
            returnValue = null;
        } else if (input instanceof JSONArray) {
            JSONArray valuesArray = (JSONArray) input;
            if (valuesArray.length() == 0) {
                returnValue = null;
            } else {
                returnValue = String.valueOf(valuesArray.get(0));
            }
        } else if (input instanceof String) {
            returnValue = (String) input;
        } else {
            returnValue = String.valueOf(input);
        }
        return returnValue;
    }

    /**
     * Retrieves the value from the given JSONObject at the specified key as a String. If the value returned would be
     * null, the default value is returned instead.
     *
     * @param inputJSONObj the JSONObject to look into
     * @param key the key where the value should be found within the given JSONObject
     * @param defaultValue the value to use if key is not found or is null
     * @return a String or null if key does not exist or is null
     */
    public static String getValue(JSONObject inputJSONObj, String key, String defaultValue)
    {
        String value = getValue(inputJSONObj, key);

        if (value == null) {
            return defaultValue;
        } else {
            return value;
        }
    }

    /**
     * Helper function for negating a comparison operator. If the negate flag is false, the given operator is returned.
     * If it is true the following happens. If the operator is '=', the result is '!='. If the operator is a null
     * comparison 'is null' the result is ' is not null '. All other operators are negated as follows, if operator is
     * 'x' the result is ' not x '. Notice the extra spaces added before and after the operators, except when dealing
     * with the equals operator.
     *
     * @param operator the operator to process
     * @param negate flag for enabling negation
     * @return a String with the operator
     */
    public static String getComparisonOperator(String operator, boolean negate)
    {
        if (negate) {
            if (StringUtils.equals(operator, "=")) {
                return "!=";
            } else if (StringUtils.equals(operator, "is null")) {
                return " is not null ";
            } else {
                return " not " + operator + " ";
            }
        } else {
            return " " + operator + " ";
        }
    }

    /**
     * Returns a Set of names of filter parameters which store filter values.
     * Examples:
     * values, min, max, before, after ... etc
     *
     * @return a Set of Strings
     */
    public static Set<String> getValueParameterNames()
    {
        return new DefaultFilterFactory(null).getValueParameterNames();
    }
}
