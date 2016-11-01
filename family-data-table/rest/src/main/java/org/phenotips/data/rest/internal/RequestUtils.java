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
import java.util.StringTokenizer;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

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

    public static MultivaluedMap<String, String> getQueryParameters(String queryString)
    {
        MultivaluedMap<String, String> queryParameters = new MultivaluedHashMap<>();

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
                    queryParameters.add(values[0], values[1]);
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
}
