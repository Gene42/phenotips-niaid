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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

//import javax.ws.rs.core.MultivaluedHashMap;
//import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public final class RequestUtils
{
    public static final String TRANS_PREFIX_KEY = "transprefix";

    private RequestUtils() {
        // Util class
    }

    public static Map<String, List<String>> getQueryParameters(String queryString)
    {
        Map<String, List<String>> queryParameters = new HashMap<>();

        //String []
        //StringTokenizer tokenizer = new StringTokenizer(queryString, "&");

        String [] queryParamPairs = StringUtils.splitPreserveAllTokens(queryString, "&");
        //System.out.println("pairs=" + Arrays.toString(queryParamPairs));
        //try {
            for (int i = 0, len = queryParamPairs.length; i < len; i++) {
                String pair = null;
                try {
                    pair = URLDecoder.decode(queryParamPairs[i], StandardCharsets.UTF_8.toString());
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                //System.out.println("pair=" + pair);

                if (StringUtils.isBlank(pair)) {
                    continue;
                }
                String [] values = StringUtils.split(pair, "=", 2);

                if (values.length == 2) {
                    //queryParameters.add(values[0], values[1]);
                    addToMap(queryParameters, values[0], values[1]);
                }
                else {
                    queryParameters.put(values[0], null);
                }
            }

            //while (tokenizer.hasMoreTokens()) {
                //String [] values = StringUtils.split(URLDecoder.decode(tokenizer.nextToken(), StandardCharsets.UTF_8.toString()), "=");

           // }
        //} catch (UnsupportedEncodingException e) {
            //LOGGER.warn(e.getMessage(), e);
        //}
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
}
