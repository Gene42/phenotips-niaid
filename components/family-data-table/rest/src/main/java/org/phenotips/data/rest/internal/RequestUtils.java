/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.rest.internal;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utils class providing helpful methods for dealing with URL parameters.
 *
 * @version $Id$
 */
public final class RequestUtils
{
    /** Transprefix key.*/
    public static final String TRANS_PREFIX_KEY = "transprefix";

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestUtils.class);

    private RequestUtils()
    {
        // Util class
    }

    /**
     * Returns a map of parameter names and their list of values. This method does a URL decode on the string.
     *
     * @param queryString the query string to parse (ie: param1=x&param2=y&param2=z.....)
     * @return a Map representation of the URL parameter list
     */
    public static Map<String, List<String>> getQueryParameters(String queryString)
    {
        Map<String, List<String>> queryParameters = new HashMap<>();


        String [] queryParamPairs = StringUtils.splitPreserveAllTokens(queryString, "&");

        for (String queryParamPair : queryParamPairs) {
            String pair = urlDecode(queryParamPair);

            if (StringUtils.isBlank(pair)) {
                continue;
            }
            String[] values = StringUtils.split(pair, "=", 2);

            if (values.length == 2) {
                addToMap(queryParameters, values[0], values[1]);
            } else {
                queryParameters.put(values[0], null);
            }
        }

        return queryParameters;
    }

    /**
     * Adds a value for a specific key in the given map. If the key does not exist, a new one is created with
     * a new empty list and the given value is added to this list. Otherwise the value gets added to the existing
     * list.
     *
     * @param map the map to add to
     * @param key the key to use
     * @param value the value to set
     */
    public static void addToMap(Map<String, List<String>> map, String key, String value)
    {
        List<String> values = map.get(key);

        if (values == null) {
            values = new LinkedList<>();
            map.put(key, values);
        }
        values.add(value);
    }

    /**
     * Returns the first item in the list found at the given key, or null if the key does not exist or the list
     * is empty.
     *
     * @param map the map to get from
     * @param key the key to use
     * @return a String or null
     */
    public static String getFirst(Map<String, List<String>> map, String key)
    {
        List<String> values = map.get(key);

        if (CollectionUtils.isEmpty(values)) {
            return null;
        } else {
            return values.get(0);
        }
    }

    /**
     * Returns the first item in the list found at the given key, or the defaultValue if the key does not exist
     * or the list is empty.
     *
     * @param map the map to get from
     * @param key the key to use
     * @param defaultValue the value to return in case the key search yields a null result
     * @return a String or the given defaultValue
     */
    public static String getFirst(Map<String, List<String>> map, String key, String defaultValue)
    {
        List<String> values = map.get(key);

        if (CollectionUtils.isEmpty(values)) {
            return defaultValue;
        } else {
            return values.get(0);
        }
    }

    private static String urlDecode(String urlEncodedStr)
    {
        try {
            return URLDecoder.decode(urlEncodedStr, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("This error should never happen", e);
            return null;
        }
    }
}
