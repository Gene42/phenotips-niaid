package org.phenotips.data.rest.internal;

import org.phenotips.data.api.internal.filter.AbstractFilter;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.apache.velocity.util.StringUtils;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class FilterFactory
{

    public AbstractFilter getFilter(){
        return null;
    }

    private Map<String, List<String>> parameterMap = new HashMap<>();


    public void handle(UriInfo uriInfo) throws ParseException
    {
        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
        for (Map.Entry<String, List<String>> entry : queryParameters.entrySet()) {


        }
    }

    private void handleEntry(Map.Entry<String, List<String>> entry) {

        String [] complexEntry = StringUtils.split(entry.getKey(), "/");

        if (complexEntry.length > 1) {

        }
        else {

        }
    }
}
