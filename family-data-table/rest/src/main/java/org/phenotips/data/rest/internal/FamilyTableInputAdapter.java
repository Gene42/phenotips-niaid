package org.phenotips.data.rest.internal;

import org.phenotips.data.api.internal.filter.AbstractFilter;
import org.phenotips.data.api.internal.filter.EntityFilter;
import org.phenotips.data.api.internal.filter.ObjectFilter;
import org.phenotips.data.rest.InputAdapter;

import org.xwiki.model.EntityType;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class FamilyTableInputAdapter implements InputAdapter
{
    private static final Set<String> PATIENT_FILTERS = new HashSet<>();

    private static final Set<String> FAMILY_FILTERS = new HashSet<>();

    private static final Set<String> NON_FILTERS = new HashSet<>();

    static {
        Set<String> patientMap = new HashSet<>();



        PATIENT_FILTERS.addAll(patientMap);



        NON_FILTERS.add("classname");
        NON_FILTERS.add("limit");
        NON_FILTERS.add("offset");
        NON_FILTERS.add("sort");
        NON_FILTERS.add("reqno");
        NON_FILTERS.add("outputsyntax");
        NON_FILTERS.add("filterwhere");
        NON_FILTERS.add("filterfrom");
        NON_FILTERS.add("queryfilters");
        NON_FILTERS.add("dir");
        NON_FILTERS.add("collist");
        NON_FILTERS.add("transprefix");


    }


    @Override public JSONObject convert(UriInfo uriInfo)
    {
        //    <input type="hidden" name="initial/class" value="PhenoTips.EncryptedPatientDataClass"/>
        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
        JSONObject queryParamsJSON = new JSONObject();
       /* for (Map.Entry<String, List<String>> entry : queryParameters.entrySet()) {
            JSONArray queryParamsValuesJSON = new JSONArray();
            queryParamsJSON.put(entry.getKey(), queryParamsValuesJSON);
            for (String value : entry.getValue()) {
                queryParamsValuesJSON.put(value);
            }
        }*/

        String documentClassName = queryParameters.getFirst("classname");
        String limit = queryParameters.getFirst("limit");
        String offset = queryParameters.getFirst("offset");
        String sort = queryParameters.getFirst("sort");
        String transprefix = queryParameters.getFirst("transprefix");
        String reqNo = queryParameters.getFirst("reqno");
        String outputsyntax = queryParameters.getFirst("outputsyntax");
        String filterwhere = queryParameters.getFirst("filterwhere");
        String filterfrom = queryParameters.getFirst("filterfrom");
        String queryfilters = queryParameters.getFirst("queryfilters");
        String dir = queryParameters.getFirst("dir");
        String collist = queryParameters.getFirst("collist");


        JSONObject queryObj = new JSONObject();
        queryObj.put(AbstractFilter.TYPE_KEY, EntityType.DOCUMENT.toString());
        queryObj.put(AbstractFilter.CLASS_KEY, documentClassName);


        queryObj.put("limit", limit);
        queryObj.put("offset", offset);
        queryObj.put("collist", this.getColumnList(collist, documentClassName, queryParameters));


        //JSONArray filters = new JSONArray();
        //queryObj.put(EntityFilter.FILTERS_KEY, filters);

        //Map<String, JSONObject> parentMap = new HashMap<>();

        JSONObject childJSON = new JSONObject();
        //JSONObject familyJSON = new JSONObject();

        Collection<JSONObject> filterList = getFilters(queryParameters, documentClassName);



        for (JSONObject  entry : filterList) {
            if (StringUtils.equals(documentClassName, entry.optString("parentClass"))) {
                queryObj.append(EntityFilter.FILTERS_KEY, entry);
            }
            else {
                childJSON.append(EntityFilter.FILTERS_KEY, entry);
            }
        }

        if (StringUtils.equals(documentClassName, "PhenoTips.FamilyClass")) {
            childJSON.put(AbstractFilter.TYPE_KEY, EntityType.DOCUMENT.toString());
            childJSON.put(AbstractFilter.CLASS_KEY, "PhenoTips.PatientClass");
            queryObj.append(EntityFilter.FILTERS_KEY, childJSON);
        }


       /* JSONObject filter1 = new JSONObject();
        filter1.put(AbstractFilter.TYPE_KEY, EntityType.OBJECT.toString());
        filter1.put(AbstractFilter.CLASS_KEY, "PhenoTips.VisibilityClass");
        filter1.put(ObjectFilter.PROPERTY_NAME_KEY, "visibility");
        //filter1.put(StringFilter.VALUE_KEY, new JSONArray("[private,public,open]"));

        filters.put(filter1);*/


        return queryObj;

        //return null;
    }

    public static Collection<JSONObject> getFilters(MultivaluedMap<String, String> queryParameters, String defaultParentClass) {

        Map<String, JSONObject> filterMap = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : queryParameters.entrySet()) {
            //System.out.println("key=[" + entry.getKey() + "]");
            if (NON_FILTERS.contains(entry.getKey())) {
                continue;
            }

            String propertyName = null;
            String propertyParam = null;
            String parentClass = defaultParentClass;

            String key = entry.getKey();
            if (StringUtils.contains(key, "/")) {
                //propertyName = StringUtils.substringBefore(key, "/");
                String [] tokens = StringUtils.split(key, "/");
                propertyName = tokens[0];
                propertyParam = tokens[1];
            }
            else {
                propertyName = key;
            }

            if (StringUtils.equals(propertyParam, "parentClass")) {
                parentClass = propertyParam;
            }

            if (StringUtils.contains(propertyName, "_subterms")) {
                propertyName = StringUtils.substringBefore(propertyName, "_subterms");
                propertyParam = "extended";
            }

            JSONObject filter = filterMap.get(propertyName + "/" + parentClass);
            if (filter == null) {
                filter = new JSONObject();
                filterMap.put(propertyName + "/" + parentClass, filter);
                filter.put("parentClass", parentClass);
                filter.put(AbstractFilter.TYPE_KEY, EntityType.OBJECT.toString());
                filter.put(ObjectFilter.PROPERTY_NAME_KEY, propertyName);
            }

            if (propertyParam != null) {
                //filter.put(propertyParam, entry.getValue());
                filter.put(propertyParam, queryParameters.getFirst(key));
            }
            else if (entry.getValue() != null) {
                for (String val : entry.getValue()) {
                    filter.append(AbstractFilter.VALUES_KEY, val);
                }
            }

            //
        }

        return filterMap.values();
    }

    private void handleFamily() {

    }

    private void handleProperty(String paramName, MultivaluedMap<String, String> queryParameters)
    {

    }

    private JSONArray getColumnList(String collist, String className, MultivaluedMap<String, String> queryParameters)
    {
        String [] tokens = StringUtils.split(collist, ",");

        JSONArray array = new JSONArray();

        for (String token : tokens) {

            JSONObject obj = new JSONObject();
            if (StringUtils.startsWith(token, "doc.")) {
                obj.put(TableColumn.TYPE_KEY, EntityType.DOCUMENT.toString());
            }
            else {
                obj.put(TableColumn.TYPE_KEY, EntityType.OBJECT.toString());

                if (queryParameters.containsKey(token + "/class")) {
                    obj.put(TableColumn.CLASS_KEY, queryParameters.getFirst(token + "/class"));
                }
                else {
                    obj.put(TableColumn.CLASS_KEY, className);
                }
            }

            obj.put(TableColumn.COLUMN_NAME_KEY, token);

            array.put(obj);
        }

        return array;
    }
}
