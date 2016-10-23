/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.rest.internal;

import org.phenotips.data.api.DocumentSearch;
import org.phenotips.data.api.internal.SpaceAndClass;
import org.phenotips.data.api.internal.filter.AbstractPropertyFilter;
import org.phenotips.data.api.internal.filter.DocumentQuery;
import org.phenotips.data.rest.EntitySearchInputAdapter;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
@Component(roles = { EntitySearchInputAdapter.class })
@Named("familyTable")
@Singleton
public class FamilyTableInputAdapter implements EntitySearchInputAdapter
{
    private static final String CLASS_POINTER = "@";
    private static final String PROPERTY_DELIMITER = "/";
    private static final String PARAM_DEFAULT_INDEX = "0";
    private static final String VALUE_DELIMITER = ",";
    private static final String CLASSNAME_KEY = "classname";

    private static final Set<String> NON_FILTERS = new HashSet<>();

    static {
        NON_FILTERS.add(CLASSNAME_KEY);
        NON_FILTERS.add(DocumentSearch.LIMIT_KEY);
        NON_FILTERS.add(DocumentSearch.OFFSET_KEY);
        NON_FILTERS.add(DocumentSearch.SORT_KEY);
        NON_FILTERS.add(DocumentSearch.REQUEST_NUMBER_KEY);
        NON_FILTERS.add(DocumentSearch.OUTPUT_SYNTAX_KEY);
        NON_FILTERS.add(DocumentSearch.FILTER_WHERE_KEY);
        NON_FILTERS.add(DocumentSearch.FILTER_FROM_KEY);
        NON_FILTERS.add(DocumentSearch.QUERY_FILTERS_KEY);
        NON_FILTERS.add(DocumentSearch.SORT_DIR_KEY);
        NON_FILTERS.add(DocumentSearch.COLUMN_LIST_KEY);
        NON_FILTERS.add(DocumentSearch.TRANS_PREFIX_KEY);
    }


    @Override public JSONObject convert(UriInfo uriInfo)
    {

        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();

        String documentClassName = queryParameters.getFirst(CLASSNAME_KEY);

        JSONObject queryObj = new JSONObject();
        queryObj.put(SpaceAndClass.CLASS_KEY, documentClassName);
        queryObj.put(DocumentSearch.LIMIT_KEY, queryParameters.getFirst(DocumentSearch.LIMIT_KEY));
        queryObj.put(DocumentSearch.OFFSET_KEY, queryParameters.getFirst(DocumentSearch.OFFSET_KEY));
        queryObj.put(DocumentSearch.COLUMN_LIST_KEY, this.getColumnList(documentClassName, queryParameters));

        JSONObject childJSON = new JSONObject();

        Map<String, JSONObject> filterMap = this.getFilters(queryParameters, documentClassName);

        for (Map.Entry<String, JSONObject> entry : filterMap.entrySet()) {
            String docClassName = StringUtils.substringAfter(entry.getKey(), PROPERTY_DELIMITER);
            if (StringUtils.equals(documentClassName, docClassName)) {
                queryObj.append(DocumentQuery.FILTERS_KEY, entry.getValue());
            } else {
                childJSON.append(DocumentQuery.FILTERS_KEY, entry.getValue());
            }
        }

        if (StringUtils.equals(documentClassName, "PhenoTips.FamilyClass")) {
            childJSON.put(SpaceAndClass.CLASS_KEY, "PhenoTips.PatientClass");
            queryObj.append(DocumentQuery.QUERIES_KEY, childJSON);
        }


        return queryObj;
    }

    private Map<String, JSONObject> getFilters(MultivaluedMap<String, String> queryParameters, String defaultDocClass) {

        Map<String, JSONObject> filterMap = new HashMap<>();

        //external_id/doc_class : 1/PhenoTips.PatientClass
        //external_id/1@join_mode : or
        //external_id/2@join_mode : and
        //external_id/1@ : dsa213
        //external_id/class

        Map<String, Map<String, String>> propertyToDocClassMap = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : queryParameters.entrySet()) {

            String key = entry.getKey();

            if (NON_FILTERS.contains(entry.getKey())
                || StringUtils.endsWith(key, AbstractPropertyFilter.DOC_CLASS_KEY)) {
                continue;
            }

            if (StringUtils.contains(key, PROPERTY_DELIMITER)) {
                this.handleFilterPropertyParam(
                    entry, propertyToDocClassMap, queryParameters, filterMap, defaultDocClass);
            } else {
                // It's a value
                this.populatePropertyToDocClassMap(key, propertyToDocClassMap, queryParameters, defaultDocClass);
                String documentClassName = this.getDocClass(key, null, propertyToDocClassMap);
                JSONObject filter = this.getFilter(key, documentClassName, filterMap);

                // It's a property value
                List<String> values = entry.getValue();
                for (String val : values) {
                    filter.append(AbstractPropertyFilter.VALUES_KEY, val);
                }
            }
        }

        return filterMap;
    }

    private void handleFilterPropertyParam(Map.Entry<String, List<String>> entry, Map<String, Map<String, String>>
        propertyToDocClassMap, MultivaluedMap<String, String> queryParameters, Map<String, JSONObject> filterMap,
        String defaultDocClass)
    {

        String [] keyTokens = StringUtils.split(entry.getKey(), PROPERTY_DELIMITER, 2);
        String propertyName = keyTokens[0];
        String parameter = keyTokens[1];

        this.populatePropertyToDocClassMap(propertyName, propertyToDocClassMap, queryParameters, defaultDocClass);

        List<String> values = entry.getValue();

        if (StringUtils.contains(parameter, CLASS_POINTER)) {
            // It's a param or value belonging to multiple classes, must see which one
            String [] paramTokens = StringUtils.split(parameter, CLASS_POINTER, 2);

            String index = paramTokens[0];

            String documentClassName = this.getDocClass(propertyName, index, propertyToDocClassMap);

            JSONObject filter = this.getFilter(propertyName, documentClassName, filterMap);

            if (paramTokens.length == 2) {
                String propertyParamName = paramTokens[1];
                this.addPropertyValueToFilter(propertyParamName, values, filter);
            } else if (CollectionUtils.isNotEmpty(values)) {
                // It's a value (paramTokens[0] is the index
                for (String val : values) {
                    filter.append(AbstractPropertyFilter.VALUES_KEY, val);
                }
            }
        } else {
            // It's a property param
            String documentClassName = this.getDocClass(propertyName, null, propertyToDocClassMap);
            JSONObject filter = this.getFilter(propertyName, documentClassName, filterMap);
            this.addPropertyValueToFilter(parameter, values, filter);
        }
    }

    private void addPropertyValueToFilter(String propertyParamName, List<String> values, JSONObject filter)
    {
        if (CollectionUtils.isEmpty(values)) {
            filter.put(propertyParamName, (Object) null);
        } else if (values.size() == 1) {
            filter.put(propertyParamName, values.get(0));
        } else {
            filter.put(propertyParamName, values);
        }
    }

    private JSONObject getFilter(String propertyName, String documentClassName, Map<String, JSONObject> filterMap)
    {
        JSONObject filter = filterMap.get(propertyName + PROPERTY_DELIMITER + documentClassName);
        if (filter == null) {
            filter = new JSONObject();
            filterMap.put(propertyName + PROPERTY_DELIMITER + documentClassName, filter);
            filter.put(AbstractPropertyFilter.DOC_CLASS_KEY, documentClassName);
            filter.put(AbstractPropertyFilter.PROPERTY_NAME_KEY, propertyName);
            filter.put(SpaceAndClass.CLASS_KEY, documentClassName);
        }
        return filter;
    }

    private String getDocClass(String property, String index, Map<String, Map<String, String>> propertyToDocClassMap) {

        String classIndex;

        if (StringUtils.isBlank(index)) {
            classIndex = PARAM_DEFAULT_INDEX;
        } else {
            classIndex = index;
        }

        String docClassName = propertyToDocClassMap.get(property).get(classIndex);

        if (StringUtils.isBlank(docClassName)) {
            throw new IllegalArgumentException(
                String.format("Invalid index [%1$s] for property [%2$s]", classIndex, property));
        }

        return docClassName;
    }

    private void populatePropertyToDocClassMap(String property, Map<String, Map<String, String>> propertyToDocClassMap,
        MultivaluedMap<String, String> queryParameters, String defaultDocClass)
    {
        //external_id/doc_class : 1/PhenoTips.PatientClass
        //external_id/1@join_mode : or
        //external_id/2@join_mode : and
        //external_id/1@ : dsa213
        //external_id/class

        Map<String, String> docClassMap = propertyToDocClassMap.get(property);

        if (docClassMap != null) {
            return;
        }

        docClassMap = new HashMap<>();
        propertyToDocClassMap.put(property, docClassMap);

        List<String> docClasses =
            queryParameters.get(property + PROPERTY_DELIMITER + AbstractPropertyFilter.DOC_CLASS_KEY);

        if (CollectionUtils.isEmpty(docClasses)) {
            docClassMap.put(PARAM_DEFAULT_INDEX, defaultDocClass);
            return;
        }

        for (String docClassFull : docClasses) {

            for (String docClass : StringUtils.split(docClassFull, VALUE_DELIMITER)) {
                String index;
                String docClassToUse;

                String [] tokens = StringUtils.split(docClass, PROPERTY_DELIMITER);

                if (tokens.length == 2) {
                    index = tokens[0];
                    docClassToUse = tokens[1];
                } else {
                    index = PARAM_DEFAULT_INDEX;
                    docClassToUse = docClass;
                }

                docClassMap.put(index, docClassToUse);
            }
        }
    }

    private JSONArray getColumnList(String className, MultivaluedMap<String, String> queryParameters)
    {
        String [] tokens = StringUtils.split(queryParameters.getFirst(DocumentSearch.COLUMN_LIST_KEY), VALUE_DELIMITER);

        JSONArray array = new JSONArray();

        for (String token : tokens) {

            JSONObject obj = new JSONObject();
            if (StringUtils.startsWith(token, "doc.")) {
                obj.put(TableColumn.TYPE_KEY, EntityType.DOCUMENT.toString());
            } else {
                obj.put(TableColumn.TYPE_KEY, EntityType.OBJECT.toString());

                String key = token + PROPERTY_DELIMITER + SpaceAndClass.CLASS_KEY;

                if (queryParameters.containsKey(key)) {
                    obj.put(TableColumn.CLASS_KEY, queryParameters.getFirst(key));
                } else {
                    obj.put(TableColumn.CLASS_KEY, className);
                }
            }

            obj.put(TableColumn.COLUMN_NAME_KEY, token);

            array.put(obj);
        }

        return array;
    }
}
