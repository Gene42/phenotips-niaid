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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public final class RequestUtils
{
    public static final String TRANS_PREFIX_KEY = "transprefix";

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestUtils.class);

    private RequestUtils() {
        // Util class
    }

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

    public static void addToMap(Map<String, List<String>> map, String key, String value)
    {
        List<String> values = map.get(key);

        if (values == null) {
            values = new LinkedList<>();
            map.put(key, values);
        }
        values.add(value);
    }

    public static String getFirst(Map<String, List<String>> map, String key)
    {
        List<String> values = map.get(key);

        if (CollectionUtils.isEmpty(values)) {
            return null;
        } else {
            return values.get(0);
        }
    }

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
