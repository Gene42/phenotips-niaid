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

    private static final String CLASS_NAME = "BiospecimenClass";

    /**
     * A reference to a document representing a Biospecimen.
     */
    private static final EntityReference CLASS_REFERENCE = getClassReference();

    /** The name of the data this controller handles. */
    private static final String DATA_NAME = "biospecimens";


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

            List<BaseObject> biospecimenXWikiObjects = doc.getXObjects(CLASS_REFERENCE);
            if (CollectionUtils.isEmpty(biospecimenXWikiObjects)) {
                return null;
            }

            List<BiospecimenData> biospecimens = new LinkedList<>();

            for (BaseObject biospecimenObject : biospecimenXWikiObjects) {

                if (biospecimenObject == null || biospecimenObject.getFieldList().isEmpty()) {
                    continue;
                }

                biospecimens.add(new BiospecimenData().parse(biospecimenObject));
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
                if (isBiospecimenDataValid(biospecimen)) {
                    this.addBiospecimenDataToDoc(biospecimen, doc, context);
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

        PatientData<BiospecimenData> biospecimenData = patient.getData(getName());

        if (biospecimenData == null || biospecimenData.size() == 0) {
            return;
        }

        Collection<String> propertiesToInclude = propertyNamesToIterateOver(selectedFieldNames);

        DateFormat dateFormat =
            new SimpleDateFormat(this.configurationManager.getActiveConfiguration().getISODateFormat());

        JSONArray biospecimensJsonArray = new JSONArray();

        for (BiospecimenData biospecimen : biospecimenData) {
            if (biospecimen == null) {
                continue;
            }
            JSONObject obj = getJSONObjectFromBiospecimenData(biospecimen, propertiesToInclude, dateFormat);
            if (obj != null) {
                biospecimensJsonArray.put(obj);
            }
        }

        if (biospecimensJsonArray.length() > 0) {
            jsonObject.put(getName(), biospecimensJsonArray);
        }
    }

    @Override
    public PatientData<BiospecimenData> readJSON(JSONObject jsonObject)
    {
        if (jsonObject == null || jsonObject.optJSONArray(getName()) == null) {
            return null;
        }

        JSONArray biospecimensJsonArray = jsonObject.getJSONArray(getName());

        List<BiospecimenData> result = new LinkedList<>();

        DateFormat jsonDateFormat =
            new SimpleDateFormat(this.configurationManager.getActiveConfiguration().getISODateFormat());

        try {
            for (Object biospecimenObject : biospecimensJsonArray) {
                BiospecimenData biospecimen = parseBiospecimenObject(biospecimenObject, jsonDateFormat);
                if (biospecimen != null) {
                    result.add(biospecimen);
                }
            }
        } catch (ParseException e) {
            this.logger.error("Unable to parse JSON data [{}]", e.getMessage());
            return null;
        }

        return new IndexedPatientData<>(getName(), result);
    }

    /**
     * Gets an EntityReference to the xWiki class representing BiospecimenData in the patient document.
     * @return a EntityReference object
     */
    public static EntityReference getClassReference()
    {
        return new EntityReference(CLASS_NAME, EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);
    }

    /***************************************** Private Methods ****************************************/

    /**
     * Helper function. If selectedFieldNames is no null it will be returned, otherwise the PROPERTIES set will be
     * returned otherwise.
     *
     * @param selectedFieldNames a collection of field names to return if not null
     * @return a Collection
     */
    private static Collection<String> propertyNamesToIterateOver(Collection<String> selectedFieldNames)
    {
        if (CollectionUtils.isEmpty(selectedFieldNames)) {
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
        Collection<String> propertiesToIterateOver = propertiesToInclude;
        if (CollectionUtils.isEmpty(propertiesToIterateOver)) {
            propertiesToIterateOver = BiospecimenData.PROPERTIES;
        }

        JSONObject biospecimenObject = new JSONObject();

        boolean jsonIsEmpty = true;
        for (String propertyName : propertiesToIterateOver) {

            switch (propertyName) {
                case BiospecimenData.TYPE_PROPERTY_NAME:
                    if (biospecimen.hasType()) {
                        biospecimenObject.put(propertyName, biospecimen.getType());
                        jsonIsEmpty = false;
                    }
                    break;
                case BiospecimenData.DATE_COLLECTED_PROPERTY_NAME:
                    if (biospecimen.hasDateCollected()) {
                        biospecimenObject.put(propertyName, jsonDateFormat.format(biospecimen.getDateCollected()));
                        jsonIsEmpty = false;
                    }
                    break;
                case BiospecimenData.DATE_RECEIVED_PROPERTY_NAME:
                    if (biospecimen.hasDateReceived()) {
                        biospecimenObject.put(propertyName, jsonDateFormat.format(biospecimen.getDateReceived()));
                        jsonIsEmpty = false;
                    }
                    break;
                default:
                    break;
            }
        }

        if (jsonIsEmpty) {
            return null;
        }

        return biospecimenObject;
    }

    private void addBiospecimenDataToDoc(BiospecimenData biospecimen, XWikiDocument doc, XWikiContext context)
    {
        try {
            BaseObject xWikiObject = doc.newXObject(CLASS_REFERENCE, context);

            if (biospecimen.hasType()) {
                xWikiObject.set(BiospecimenData.TYPE_PROPERTY_NAME, biospecimen.getType(), context);
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

    private static boolean isBiospecimenDataValid(BiospecimenData biospecimen)
    {
        return biospecimen != null && biospecimen.isNotEmpty();
    }
}
