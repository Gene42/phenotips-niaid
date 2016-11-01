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
        StringTokenizer tokenizer = new StringTokenizer(queryString, "&");
        try {
            while (tokenizer.hasMoreTokens()) {
                String [] values = StringUtils.split(
                    URLDecoder.decode(tokenizer.nextToken(), StandardCharsets.UTF_8.toString()), "=");

                if (values.length == 2) {
                    queryParameters.add(values[0], values[1]);
                }
                else {
                    queryParameters.put(values[0], null);
                }
            }
        } catch (UnsupportedEncodingException e) {
            //LOGGER.warn(e.getMessage(), e);
        }
        return queryParameters;
    }
}
