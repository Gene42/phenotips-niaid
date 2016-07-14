/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.data.internal.controller;

import org.phenotips.Constants;
import org.phenotips.configuration.RecordConfigurationManager;
import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.DateProperty;
import com.xpn.xwiki.objects.StringProperty;

/**
 * Handles Biospecimens.
 *
 * @version $Id$
 */
@Component(roles = { PatientDataController.class })
@Named("biospecimens")
@Singleton
public class BiospecimensController implements PatientDataController<Map<String, String>>
{
    private static final String DATA_NAME = "biospecimens";

    /**
     * The type of biospecimen. It is a static list: {Skin|Hair|Nails|Blood plasma|Serum}.
     */
    private static final String PROPERTY_NAME_TYPE = "type";

    /**
     * Date when biospecimen was collected.
     */
    private static final String PROPERTY_NAME_DATE_COLLECTED = "date_collected";

    /**
     * Date when biospecimen was received.
     */
    private static final String PROPERTY_NAME_DATE_RECEIVED = "date_received";

    /**
     * An ordered set of all the property names of the BiospecimenClass.
     */
    private static final Set<String> PROPERTIES = new LinkedHashSet<>(Arrays
        .asList(PROPERTY_NAME_TYPE, PROPERTY_NAME_DATE_COLLECTED, PROPERTY_NAME_DATE_RECEIVED));

    /**
     * A reference to a document representing a Biospecimen.
     */
    private static final EntityReference CLASS_REFERENCE =
        new EntityReference("BiospecimenClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    /** Formatter for dates. The documentation for the class states that it is thread safe, thus I made this a static
     * field. */
    private static final DateTimeFormatter PATIENT_DATA_DATE_FORMATTER =
        ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC);

    @Inject
    private RecordConfigurationManager configurationManager;

    /**
     * Logging helper object.
     */
    @Inject
    private Logger logger;


    /**
     * Provides access to the underlying data storage.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /** Provides access to the current execution context. */
    @Inject
    private Provider<XWikiContext> xContextProvider;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName()
    {
        return DATA_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PatientData<Map<String, String>> load(Patient patient)
    {

        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());

            BaseObject biospecimenXWikiObject = doc.getXObject(CLASS_REFERENCE);

            if (biospecimenXWikiObject == null) {
                return null;
            }

            List<BaseObject> biospecimenXWikiObjects = doc.getXObjects(CLASS_REFERENCE);
            if (CollectionUtils.isEmpty(biospecimenXWikiObjects)) {
                return null;
            }

            List<Map<String, String>> biospecimens = new LinkedList<>();

            DateFormat dateFormat =
                new SimpleDateFormat(this.configurationManager.getActiveConfiguration().getISODateFormat());


            for (BaseObject biospecimenObject : biospecimenXWikiObjects) {

                if (biospecimenObject == null || biospecimenObject.getFieldList().isEmpty()) {
                    continue;
                }

                Map<String, String> biospecimen = new LinkedHashMap<>();

                for (String property : PROPERTIES) {

                    String value = getValue(biospecimenObject, property, dateFormat);

                    if (value == null) {
                        continue;
                    }

                    biospecimen.put(property, value);
                }

                biospecimens.add(biospecimen);
            }

            if (CollectionUtils.isNotEmpty(biospecimens)) {
                return new IndexedPatientData<>(getName(), biospecimens);
            }

        } catch (Exception e) {
            this.logger.error("Could not find requested document or some unforeseen"
                + " error has occurred during controller loading [{}]", e.getMessage(), e);
        }
        return null;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void save(Patient patient)
    {
        try {
            PatientData<Map<String, String>> data = patient.getData(getName());
            if (data == null || !data.isIndexed()) {
                return;
            }

            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            if (doc == null) {
                throw new NullPointerException(ERROR_MESSAGE_NO_PATIENT_CLASS);
            }

            XWikiContext context = this.xContextProvider.get();
            doc.removeXObjects(CLASS_REFERENCE);

            for (Map<String, String> biospecimen : data) {
                try {
                    BaseObject xWikiObject = doc.newXObject(CLASS_REFERENCE, context);

                    for (String property : PROPERTIES) {

                        String value = biospecimen.get(property);

                        if (value != null) {
                            xWikiObject.set(property, value, context);
                        }
                    }

                } catch (Exception e) {
                    this.logger.error("Failed to save a specific biospecimen: [{}]", e.getMessage());
                }
            }
            context.getWiki().saveDocument(doc, "Updated biospecimens from JSON", true, context);

        } catch (Exception e) {
            this.logger.error("Failed to save biospecimens: [{}]", e.getMessage());
        }
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void writeJSON(Patient patient, JSONObject jsonObject)
    {
        this.writeJSON(patient, jsonObject, null);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void writeJSON(Patient patient, JSONObject jsonObject, Collection<String> selectedFieldNames)
    {
        if (selectedFieldNames != null && !PROPERTIES.containsAll(selectedFieldNames)) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Some of the given selectedFieldNames [{}] do not belong to the allowed values [{}]",
                    selectedFieldNames, PROPERTIES);
            }
            return;
        }

        PatientData<Map<String, String>> biospecimensData = patient.getData(getName());

        if (biospecimensData == null || biospecimensData.size() == 0) {
            return;
        }

        Collection<String> propertiesToInclude = propertyNamesToIterateOver(selectedFieldNames);

        DateFormat jsonDateFormat =
            new SimpleDateFormat(this.configurationManager.getActiveConfiguration().getISODateFormat());

        JSONArray biospecimensJsonArray = new JSONArray();
        for (Map<String, String> biospecimen : biospecimensData) {

            JSONObject biospecimenObject = new JSONObject();

            for (String propertyName : propertiesToInclude) {

                String value = biospecimen.get(propertyName);

                if (StringUtils.isBlank(value)) {
                    continue;
                }

                switch (propertyName) {
                    case PROPERTY_NAME_TYPE:
                        biospecimenObject.put(propertyName, value);
                        break;
                    case PROPERTY_NAME_DATE_COLLECTED:
                    case PROPERTY_NAME_DATE_RECEIVED:
                        biospecimenObject.put(propertyName,
                            // Convert from patient data format to the outgoing format needed in the json
                            jsonDateFormat.format(PATIENT_DATA_DATE_FORMATTER.parseDateTime(value)));
                        break;
                    default:
                        break;
                }
            }

            biospecimensJsonArray.put(biospecimenObject);
        }

        jsonObject.put(getName(), biospecimensJsonArray);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public PatientData<Map<String, String>> readJSON(JSONObject jsonObject)
    {
        if (jsonObject == null || !jsonObject.has(getName())) {
            return null;
        }

        Object biospecimensObject = jsonObject.get(getName());

        if (!(biospecimensObject instanceof JSONArray)) {
            return null;
        }

        List<Map<String, String>> result = new LinkedList<>();

        JSONArray  biospecimensJsonArray = (JSONArray) biospecimensObject;

        DateFormat jsonDateFormat =
            new SimpleDateFormat(this.configurationManager.getActiveConfiguration().getISODateFormat());

        for (Object biospecimenObject : biospecimensJsonArray) {
            Map<String, String> biospecimen = parseBiospecimenObject(biospecimenObject, jsonDateFormat);
            if (biospecimen != null) {
                result.add(biospecimen);
            }
        }

        if (CollectionUtils.isEmpty(result)) {
            return null;
        }

        return new IndexedPatientData<>(getName(), result);
    }

    /**
     * Helper function. If selectedFieldNames is no null it will be returned, otherwise the PROPERTIES set will be
     *  returned otherwise.
     * @param selectedFieldNames a collection of field names to return if not null
     * @return a Collection
     */
    private static Collection<String> propertyNamesToIterateOver(Collection<String> selectedFieldNames) {
        if (selectedFieldNames == null) {
            return PROPERTIES;
        } else {
            return selectedFieldNames;
        }
    }

    /**
     * Returns the value of the given XWiki object based on the type of property.
     *
     * @param biospecimenObject the XWiki object to use for getting value
     * @param propertyName the property to load the value for
     * @return a String value or null if no property with the given propertyName was found
     */
    private static String getValue(BaseObject biospecimenObject, String propertyName)
    {
        switch (propertyName) {
            case PROPERTY_NAME_TYPE:
                StringProperty typeField = (StringProperty) biospecimenObject.getField(propertyName);
                if (typeField == null) {
                    return null;
                }
                return typeField.getValue();
            case PROPERTY_NAME_DATE_COLLECTED:
            case PROPERTY_NAME_DATE_RECEIVED:
                DateProperty dateField = (DateProperty) biospecimenObject.getField(propertyName);
                if (dateField == null || dateField.getValue() == null) {
                    return null;
                }
                return PATIENT_DATA_DATE_FORMATTER.print(new DateTime(dateField.getValue()));
            default:
                return null;
        }
    }

    /**
     * Attempts to parse the given object as a JSONObject and return a map representing the biospecimen properties.
     * @param biospecimenObject the object to parse
     * @param jsonDateFormat the json date formatter to use
     * @return a Map<String, String> or null if the object could not be parsed properly
     */
    private static Map<String, String> parseBiospecimenObject(Object biospecimenObject, DateFormat jsonDateFormat) {

        if (!(biospecimenObject instanceof JSONObject)) {
            return null;
        }

        JSONObject biospecimenJSONObject = (JSONObject) biospecimenObject;

        Map<String, String> biospecimen = new LinkedHashMap<>();

        for (String property : PROPERTIES) {
            if (biospecimenJSONObject.has(property)) {

                Object propertyObject = biospecimenJSONObject.get(property);

                if (propertyObject == null) {
                    continue;
                }

                switch (property) {
                    case PROPERTY_NAME_TYPE:
                        biospecimen.put(property, String.valueOf(propertyObject));
                        break;
                    case PROPERTY_NAME_DATE_COLLECTED:
                    case PROPERTY_NAME_DATE_RECEIVED:
                        try {
                            // Convert from json formatted data to patient data date format
                            Date date = jsonDateFormat.parse(String.valueOf(propertyObject));
                            biospecimen.put(property, PATIENT_DATA_DATE_FORMATTER.print(new DateTime(date)));
                        } catch (ParseException e) {
                            break;
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        if (biospecimen.isEmpty()) {
            return null;
        } else {
            return biospecimen;
        }
    }
}
