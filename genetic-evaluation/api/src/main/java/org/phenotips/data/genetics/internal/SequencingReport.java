/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.genetics.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONArray;
import org.json.JSONObject;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.DateProperty;

/**
 * Container for a SequencingReport.
 *
 * @version $Id$
 * @since 1.3M1R2
 */
public class SequencingReport
{
    /**
     * The filename for the uploaded report.
     */
    public static final String FILEATTACHMENTS_PROPERTY_NAME = "file_attachments";

    /**
     * Date when sequencing was performed.
     */
    public static final String DATESEQUENCED_PROPERTY_NAME = "date_sequenced";

    /**
     * Vendor used for sequencing.
     */
    public static final String VENDOR_PROPERTY_NAME = "vendor";

    /**
     * The ID of the vendor.
     */
    public static final String VENDORID_PROPERTY_NAME = "vendor_id";

    /**
     * Date when report was reviewed.
     */
    public static final String DATEREVIEWED_PROPERTY_NAME = "date_reviewed";

    /**
     * The user that reviewed the report.
     */
    public static final String REVIEWEDBY_PROPERTY_NAME = "reviewed_by";

    /**
     * External hyperlinks to data.
     */
    public static final String EXTERNALLINKS_PROPERTY_NAME = "external_links";

    /**
     * The method used for evaluation. One of [ target genes | del/dup | panel | wes | wgs ]
     */
    public static final String EVALUATIONTYPE_PROPERTY_NAME = "evaluation_type";

    /**
     * The genes targetted for testing.
     */
    public static final String TARGETGENES_PROPERTY_NAME = "target_genes";

    /**
     * The method of deletion/duplication testing.
     */
    public static final String DELDUP_PROPERTY_NAME = "deldup";

    /**
     * The method of panel testing.
     */
    public static final String PANEL_PROPERTY_NAME = "panel";

    /**
     * The whole exome sequencing platform.
     */
    public static final String WES_PROPERTY_NAME = "wes";

    /**
     * The whole genome sequencing platform.
     */
    public static final String WGS_PROPERTY_NAME = "wgs";

    private static final List<String> PROPERTIES = Collections.unmodifiableList(Arrays
        .asList(FILEATTACHMENTS_PROPERTY_NAME, DATESEQUENCED_PROPERTY_NAME, VENDOR_PROPERTY_NAME,
            VENDORID_PROPERTY_NAME, DATEREVIEWED_PROPERTY_NAME, REVIEWEDBY_PROPERTY_NAME,
            EXTERNALLINKS_PROPERTY_NAME, EVALUATIONTYPE_PROPERTY_NAME, TARGETGENES_PROPERTY_NAME,
            DELDUP_PROPERTY_NAME, PANEL_PROPERTY_NAME, WES_PROPERTY_NAME, WGS_PROPERTY_NAME));

    private static final List<String> STRING_PROPERTIES = Collections.unmodifiableList(Arrays
        .asList(VENDOR_PROPERTY_NAME, VENDORID_PROPERTY_NAME, REVIEWEDBY_PROPERTY_NAME,
            EVALUATIONTYPE_PROPERTY_NAME, DELDUP_PROPERTY_NAME, PANEL_PROPERTY_NAME,
            WES_PROPERTY_NAME, WGS_PROPERTY_NAME));

    private static final List<String> DATE_PROPERTIES = Collections.unmodifiableList(Arrays
        .asList(DATESEQUENCED_PROPERTY_NAME, DATEREVIEWED_PROPERTY_NAME));

    private static final List<String> LIST_PROPERTIES = Collections.unmodifiableList(Arrays
        .asList(FILEATTACHMENTS_PROPERTY_NAME, EXTERNALLINKS_PROPERTY_NAME, TARGETGENES_PROPERTY_NAME));

    private Map<String, Object> internalReferenceMap = setInternalReferenceMap();

    private static final String FILEATTACHMENTS_JSON_KEY = FILEATTACHMENTS_PROPERTY_NAME;

    private static final String DATESEQUENCED_JSON_KEY = DATESEQUENCED_PROPERTY_NAME;

    private static final String VENDOR_JSON_KEY = VENDOR_PROPERTY_NAME;

    private static final String VENDORID_JSON_KEY = VENDORID_PROPERTY_NAME;

    private static final String DATEREVIEWED_JSON_KEY = DATEREVIEWED_PROPERTY_NAME;

    private static final String REVIEWEDBY_JSON_KEY = REVIEWEDBY_PROPERTY_NAME;

    private static final String EXTERNALLINKS_JSON_KEY = EXTERNALLINKS_PROPERTY_NAME;

    private static final String EVALUATIONTYPE_JSON_KEY = EVALUATIONTYPE_PROPERTY_NAME;

    private static final List<String> EVALUATIONTYPES = Collections.unmodifiableList(Arrays
        .asList(TARGETGENES_PROPERTY_NAME, DELDUP_PROPERTY_NAME, PANEL_PROPERTY_NAME,
            WES_PROPERTY_NAME, WGS_PROPERTY_NAME));

    /** Keys for data that will be outputted in the JSON representation of this object */
    private static final List<String> JSON_KEYS = Collections.unmodifiableList(Arrays
        .asList(FILEATTACHMENTS_JSON_KEY, DATESEQUENCED_JSON_KEY, VENDOR_JSON_KEY,
            VENDORID_JSON_KEY, DATEREVIEWED_JSON_KEY, REVIEWEDBY_JSON_KEY,
            EXTERNALLINKS_JSON_KEY, EVALUATIONTYPE_JSON_KEY));

    /**
     * Populates this SequencingReport object with the contents of the given XWiki BaseObject.
     *
     * @param xobj the XWiki object to parse (can be null)
     * @throws IllegalArgumentException if any error happens during parsing
     */
    public SequencingReport(BaseObject xobj) throws IllegalArgumentException
    {
        if (xobj == null
            || xobj.getField(FILEATTACHMENTS_PROPERTY_NAME) == null
            || xobj.getListValue(FILEATTACHMENTS_PROPERTY_NAME) == null
            || xobj.getListValue(FILEATTACHMENTS_PROPERTY_NAME).isEmpty()) {
            return;
        }

        this.setFromXObject(xobj);
    }

    /**
     * Populates this SequencingReport object with the contents of the given JSONObject.
     *
     * @param json the JSONObject to parse (can be null)
     * @throws IllegalArgumentException if any error happens during parsing
     */
    public SequencingReport(JSONObject json) throws Exception
    {
        if (json == null || !json.has(FILEATTACHMENTS_JSON_KEY)) {
            return;
        }

        this.setFromJSON(json);
    }

    /**
     * Converts contents of this object to a JSON. Evaluation method XProperties are skipped, as the specific
     * evaluation test method will be appended to an array as the value of evaluation_type.
     *
     * Recall: An evaluation method property is one of [ target genes | del/dup | panel | wes | wgs ]
     *
     * @return JSON representation of this object
     */
    public JSONObject toJSON()
    {
        JSONObject json = new JSONObject();
        for (String key : JSON_KEYS) {
            if (key.equals(EVALUATIONTYPE_JSON_KEY)) {
                json.put(key, getEvaluationTypeJSONValue());
            } else {
                json.put(key, this.internalReferenceMap.get(key));
            }
        }
        return json;
    }

    /**
     * Populates the provided XObject with the contents of this object.
     *
     * @param xobj
     * @param context
     */
    public void populateXWikiObject(BaseObject xobj, XWikiContext context)
    {
        for (String key : this.internalReferenceMap.keySet()) {
            if (this.internalReferenceMap.get(key) == null) {
                continue;
            }

            if (isDateProperty(key)) {
                DateTime date = (DateTime) this.internalReferenceMap.get(key);
                xobj.set(key, date.toDate(), context);
            } else {
                xobj.set(key, this.internalReferenceMap.get(key), context);
            }
        }
    }

    /**
     * Gets the name of all the XProperties of the {@code SequencingReportClass} for data held in this container class.
     *
     * @return List of property names
     */
    public static List<String> getProperties()
    {
        return PROPERTIES;
    }

    private Map<String, Object> setInternalReferenceMap()
    {
        Map<String, Object> internalReferenceMap = new LinkedHashMap<>();
        for (String key : SequencingReport.PROPERTIES) {
            if (isStringProperty(key)) {
                internalReferenceMap.put(key, new String());

            } else if (isDateProperty(key)) {
                internalReferenceMap.put(key, new DateTime());

            } else if (isListProperty(key)) {
                List<String> container = new LinkedList<>();
                internalReferenceMap.put(key, container);
            }
        }
        return internalReferenceMap;
    }

    private boolean isStringProperty(String property) { return SequencingReport.STRING_PROPERTIES.contains(property); }

    private boolean isDateProperty(String property) { return SequencingReport.DATE_PROPERTIES.contains(property); }

    private boolean isListProperty(String property) { return SequencingReport.LIST_PROPERTIES.contains(property); }

    private boolean hasStringValue(String key, JSONObject json)
    {
        try {
            json.getString(key);
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    private boolean hasJSONArrayValue(String key, JSONObject json)
    {
        return json != null && json.optJSONArray(key) != null;
    }

    private void setFromXObject(BaseObject xobj)
    {
        for (String key : this.internalReferenceMap.keySet()) {
            if (isStringProperty(key)) {
                this.internalReferenceMap.put(key, xobj.getStringValue(key));

            } else if (isDateProperty(key)) {
                this.internalReferenceMap.put(key, getDateFromXWikiObject(key, xobj));

            } else if (isListProperty(key)) {
                this.internalReferenceMap.put(key, (List<String>) xobj.getListValue(key));
            }
        }
    }

    private void setFromJSON(JSONObject json)
    {
        for (String key : this.internalReferenceMap.keySet()) {
            if (isStringProperty(key) && hasStringValue(key, json)) {
                this.internalReferenceMap.put(key, json.getString(key));

            } else if (isDateProperty(key) && hasStringValue(key, json)) {
                this.internalReferenceMap.put(key, getDateFromJSONObject(key, json));

            } else if (isListProperty(key) && hasJSONArrayValue(key, json)) {
                List<String> container = (List<String>) this.internalReferenceMap.get(key);
                container.clear();
                if (json.optJSONArray(key) != null) {
                    JSONArray array = json.getJSONArray(key);
                    for (Object item : array) {
                        if (item instanceof String) {
                            container.add((String) item);
                        }
                    }
                }
            }
        }
    }

    private static DateTime getDateFromXWikiObject(String property, BaseObject xWikiObject)
    {
        DateProperty field = (DateProperty) xWikiObject.getField(property);
        if (field == null || field.getValue() == null) {
            return null;
        }
        return new DateTime(field.getValue());
    }

    private DateTime getDateFromJSONObject(String key, JSONObject json)
    {
        DateTimeFormatter jsonDateFormat = ISODateTimeFormat.date();
        DateTime date = null;

        if (json != null && json.optString(key) != null) {
            date = jsonDateFormat.parseDateTime((String) json.get(key));
        }

        return date;
    }

    private String getEvaluationTypePrettyName(String property)
    {
        if (property.equals(TARGETGENES_PROPERTY_NAME)) {
            return "Target genes";
        } else if (property.equals(DELDUP_PROPERTY_NAME)) {
            return "Deletion/duplication testing";
        } else if (property.equals(PANEL_PROPERTY_NAME)) {
            return "Panel testing";
        } else if (property.equals(WES_PROPERTY_NAME)) {
            return "Whole exome sequencing";
        } else if (property.equals(WGS_PROPERTY_NAME)) {
            return "Whole genome sequencing";
        }
        return "";
    }
    /**
     * Gets the JSON value for the evaluation_type property whose value in XWiki is the name of
     * one of the following XWiki properties: target_genes, deldup, panel, wes, or wgs. The value of
     * these properties are specific testing methods, i.e. "ion torrent", "Sanger", "DNA microarray".
     *
     * @return List containing the evaluation type value at the 0th position, followed by the name of the specific
     * type of method or in the case of target genes, the genes tested
     */
    private List<String> getEvaluationTypeJSONValue()
    {
        List<String> output = new LinkedList<>();
        String evaluationTypeValue =  (String) this.internalReferenceMap.get(EVALUATIONTYPE_PROPERTY_NAME);

        if (evaluationTypeValue.equals(TARGETGENES_PROPERTY_NAME)) {
            output.add(getEvaluationTypePrettyName(TARGETGENES_PROPERTY_NAME));
            output.addAll((List<String>) this.internalReferenceMap.get(evaluationTypeValue));
        } else {
            for (String property : EVALUATIONTYPES) {
                if (property.equals(evaluationTypeValue)) {
                    output.add(getEvaluationTypePrettyName(evaluationTypeValue));
                    output.add((String) this.internalReferenceMap.get(evaluationTypeValue));
                }
            }
        }
        return output;
    }
}
