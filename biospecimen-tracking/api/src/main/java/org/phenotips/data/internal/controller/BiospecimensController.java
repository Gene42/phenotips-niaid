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
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Handles Biospecimens.
 *
 * @version $Id$
 */
@Component(roles = { PatientDataController.class })
@Named("biospecimens")
@Singleton
public class BiospecimensController implements PatientDataController<BiospecimenData>
{
    private static final String DATA_NAME = "biospecimens";

    /**
     * A reference to a document representing a Biospecimen.
     */
    private static final EntityReference CLASS_REFERENCE =
        new EntityReference("BiospecimenClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

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

    /**
     * Provides access to the current execution context.
     */
    @Inject
    private Provider<XWikiContext> xContextProvider;

    @Override
    public String getName()
    {
        return DATA_NAME;
    }

    @Override
    public PatientData<BiospecimenData> load(Patient patient)
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

            List<BiospecimenData> biospecimens = new LinkedList<>();

            for (BaseObject biospecimenObject : biospecimenXWikiObjects) {

                if (biospecimenObject == null || biospecimenObject.getFieldList().isEmpty()) {
                    continue;
                }

                biospecimens.add(new BiospecimenData(biospecimenObject));
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

    @Override
    public void save(Patient patient)
    {
        try {
            PatientData<BiospecimenData> data = patient.getData(getName());
            if (data == null || !data.isIndexed()) {
                return;
            }

            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            if (doc == null) {
                throw new NullPointerException(ERROR_MESSAGE_NO_PATIENT_CLASS);
            }

            XWikiContext context = this.xContextProvider.get();
            doc.removeXObjects(CLASS_REFERENCE);

            for (BiospecimenData biospecimen : data) {
                try {
                    BaseObject xWikiObject = doc.newXObject(CLASS_REFERENCE, context);

                    String type = biospecimen.getType();
                    if (StringUtils.isNotBlank(type)) {
                        xWikiObject.set(BiospecimenData.TYPE_PROPERTY_NAME, type, context);
                    }

                    Date dateCollected = biospecimen.getDateCollected();
                    if (dateCollected != null) {
                        xWikiObject.set(BiospecimenData.DATE_COLLECTED_PROPERTY_NAME, dateCollected, context);
                    }

                    Date dateReceived = biospecimen.getDateReceived();
                    if (dateReceived != null) {
                        xWikiObject.set(BiospecimenData.DATE_RECEIVED_PROPERTY_NAME, dateReceived, context);
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

    @Override
    public void writeJSON(Patient patient, JSONObject jsonObject)
    {
        this.writeJSON(patient, jsonObject, null);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject jsonObject, Collection<String> selectedFieldNames)
    {
        if (selectedFieldNames != null && !BiospecimenData.PROPERTIES.containsAll(selectedFieldNames)) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Some of the given selectedFieldNames [{}] do not belong to the allowed values [{}]",
                    selectedFieldNames, BiospecimenData.PROPERTIES);
            }
            return;
        }

        PatientData<BiospecimenData> biospecimensData = patient.getData(getName());

        if (biospecimensData == null || biospecimensData.size() == 0) {
            return;
        }

        Collection<String> propertiesToInclude = propertyNamesToIterateOver(selectedFieldNames);

        DateFormat dateFormat =
            new SimpleDateFormat(this.configurationManager.getActiveConfiguration().getISODateFormat());

        JSONArray biospecimensJsonArray = new JSONArray();

        for (BiospecimenData biospecimen : biospecimensData) {
            biospecimensJsonArray.put(getJSONObjectFromBiospecimenData(biospecimen, propertiesToInclude, dateFormat));
        }

        jsonObject.put(getName(), biospecimensJsonArray);
    }

    @Override
    public PatientData<BiospecimenData> readJSON(JSONObject jsonObject)
    {
        if (jsonObject == null || !jsonObject.has(getName())) {
            return null;
        }

        Object biospecimensObject = jsonObject.get(getName());

        if (!(biospecimensObject instanceof JSONArray)) {
            return null;
        }

        List<BiospecimenData> result = new LinkedList<>();

        JSONArray biospecimensJsonArray = (JSONArray) biospecimensObject;

        DateFormat jsonDateFormat =
            new SimpleDateFormat(this.configurationManager.getActiveConfiguration().getISODateFormat());

        try {
            for (Object biospecimenObject : biospecimensJsonArray) {
                BiospecimenData biospecimen = parseBiospecimenObject(biospecimenObject, jsonDateFormat);
                if (biospecimen.isNotEmpty()) {
                    result.add(biospecimen);
                }
            }
        } catch (ParseException e) {
            this.logger.error("Unable to parse JSON data [{}]", e.getMessage());
            return null;
        }


        if (CollectionUtils.isEmpty(result)) {
            return null;
        }

        return new IndexedPatientData<>(getName(), result);
    }

    /**
     * Helper function. If selectedFieldNames is no null it will be returned, otherwise the PROPERTIES set will be
     * returned otherwise.
     *
     * @param selectedFieldNames a collection of field names to return if not null
     * @return a Collection
     */
    private static Collection<String> propertyNamesToIterateOver(Collection<String> selectedFieldNames)
    {
        if (selectedFieldNames == null) {
            return BiospecimenData.PROPERTIES;
        } else {
            return selectedFieldNames;
        }
    }

    private static BiospecimenData parseBiospecimenObject(Object biospecimenObject, DateFormat jsonDateFormat)
        throws ParseException {


        if (!(biospecimenObject instanceof JSONObject)) {
            return null;
        }

        JSONObject biospecimenJSONObject = (JSONObject) biospecimenObject;

        BiospecimenData biospecimen = new BiospecimenData();

        for (String property : BiospecimenData.PROPERTIES) {
            if (!biospecimenJSONObject.has(property)) {
                continue;
            }

            Object propertyObject = biospecimenJSONObject.get(property);

            if (propertyObject == null) {
                continue;
            }

            switch (property) {
                case BiospecimenData.TYPE_PROPERTY_NAME:
                    biospecimen.setType(String.valueOf(propertyObject));
                    break;
                case BiospecimenData.DATE_COLLECTED_PROPERTY_NAME:
                    biospecimen.setDateCollected(jsonDateFormat.parse(String.valueOf(propertyObject)));
                    break;
                case BiospecimenData.DATE_RECEIVED_PROPERTY_NAME:
                    biospecimen.setDateReceived(jsonDateFormat.parse(String.valueOf(propertyObject)));
                    break;
                default:
                    break;
            }
        }

        return biospecimen;
    }

    private static JSONObject getJSONObjectFromBiospecimenData(BiospecimenData biospecimen,
        Collection<String> propertiesToInclude, DateFormat jsonDateFormat)
    {

        JSONObject biospecimenObject = new JSONObject();

        for (String propertyName : propertiesToInclude) {

            switch (propertyName) {
                case BiospecimenData.TYPE_PROPERTY_NAME:
                    putValueIntoJSONObjectIfNotNull(propertyName, biospecimen.getType(), biospecimenObject);
                    break;
                case BiospecimenData.DATE_COLLECTED_PROPERTY_NAME:
                    putValueIntoJSONObjectIfNotNull(propertyName, biospecimen.getDateCollected(),
                        biospecimenObject, jsonDateFormat);
                    break;
                case BiospecimenData.DATE_RECEIVED_PROPERTY_NAME:
                    putValueIntoJSONObjectIfNotNull(propertyName, biospecimen.getDateReceived(),
                        biospecimenObject, jsonDateFormat);

                    break;
                default:
                    break;
            }
        }

        return biospecimenObject;
    }

    private static void putValueIntoJSONObjectIfNotNull(String propertyName, String propertyValue,
        JSONObject jsonObject)
    {
        if (StringUtils.isNotBlank(propertyValue)) {
            jsonObject.put(propertyName, propertyValue);
        }
    }

    private static void putValueIntoJSONObjectIfNotNull(String propertyName, Date propertyValue, JSONObject jsonObject,
        DateFormat jsonDateFormat)
    {
        if (propertyValue != null) {
            jsonObject.put(propertyName, jsonDateFormat.format(propertyValue));
        }
    }
}
