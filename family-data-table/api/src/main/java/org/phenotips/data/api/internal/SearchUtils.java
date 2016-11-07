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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public final class SearchUtils
{

    /** Allowed values for boolean true. */
    public static final Set<String> BOOLEAN_TRUE_SET = new HashSet<>();

    /** Allowed values for boolean false. */
    public static final Set<String> BOOLEAN_FALSE_SET = new HashSet<>();

    static {
        BOOLEAN_TRUE_SET.add("yes");
        BOOLEAN_TRUE_SET.add("true");
        BOOLEAN_TRUE_SET.add("1");

        BOOLEAN_FALSE_SET.add("no");
        BOOLEAN_FALSE_SET.add("false");
        BOOLEAN_FALSE_SET.add("0");
    }

    private SearchUtils(){ }

    /**
     * Returns a DocumentReference given a period delimited space and class name
     * @param spaceAndClass space and class "Space.Class"
     * @return a DocumentReference (never null)
     */
    public static EntityReference getClassReference(String spaceAndClass) {

        String [] tokens = SpaceAndClass.getSpaceAndClass(spaceAndClass);

        EntityReference ref;

        if (tokens.length == 2) {
            // Example: PhenoTips.GeneClass
            //new EntityReference(tokens[0], EntityType.SPACE);
            //EntityReference parent = new SpaceReference(tokens[0], new WikiReference("xwiki"));
            //EntityReference reference = new EntityReference(tokens[1], EntityType.DOCUMENT);
           // return new DocumentReference(new EntityReference(reference, parent));
            return getClassReference(tokens[0], tokens[1]);
        }
        else {
            return new EntityReference(spaceAndClass, EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);
        }

    }

    public static EntityReference getClassReference(SpaceReference spaceRef, String className) {
        EntityReference reference = new EntityReference(className, EntityType.DOCUMENT);
        return new EntityReference(reference, spaceRef);
    }

    public static EntityReference getClassReference(String space, String className) {
        SpaceReference parent = new SpaceReference(space, new WikiReference("xwiki"));
        /*;
        EntityReference reference = new EntityReference(className, EntityType.DOCUMENT);
        return new DocumentReference(new EntityReference(reference, parent));*/
        return getClassReference(parent, className);
    }

    public static DocumentReference getClassDocumentReference(String spaceAndClass) {
        return new DocumentReference(getClassReference(spaceAndClass));
    }

    public static JSONArray getJSONArray(JSONObject inputJSONObj, String key)
    {
        Object valueObj = inputJSONObj.opt(key);

        JSONArray toReturn = null;

        if (valueObj == null) {
            toReturn = new JSONArray();
        } else if (valueObj instanceof JSONArray) {
            toReturn = (JSONArray) valueObj;
        } else if (valueObj instanceof JSONObject) {
            toReturn = new JSONArray();
            toReturn.put(valueObj);
        }

        return toReturn;
    }

    public static List<String> getValues(JSONObject inputJSONObj, String key) {

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
        }

        return values;
    }

    public static String getValue(JSONObject inputJSONObj, String key) {

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
            }
            else {
                returnValue = String.valueOf(valuesArray.get(0));
            }
        } else if (input instanceof String) {
            returnValue = (String) input;
        } else {
            returnValue = String.valueOf(input);
        }
        return returnValue;
    }

    public static String getValue(JSONObject inputJSONObj, String key, String defaultValue) {
        String value = getValue(inputJSONObj, key);

        if (value == null) {
            return defaultValue;
        }
        else {
            return value;
        }
    }

    public static String getTableName()
    {
        return null;
    }
}
