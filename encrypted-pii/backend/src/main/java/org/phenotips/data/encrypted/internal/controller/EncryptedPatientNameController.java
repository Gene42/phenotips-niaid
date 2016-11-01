/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.encrypted.internal.controller;

import org.phenotips.Constants;
import org.phenotips.data.DictionaryPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Handles the patient name, stored as {@code first_name} and {@code last_name}, encrypted.
 *
 * @version $Id$
 */
@Component(roles = { PatientDataController.class })
@Named("patient-name")
@Singleton
public class EncryptedPatientNameController implements PatientDataController<String>
{
    static final EntityReference CLASS_REFERENCE = new EntityReference("EncryptedPatientDataClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    /** Provides access to the underlying data storage. */
    @Inject
    protected DocumentAccessBridge documentAccessBridge;

    /** Logging helper object. */
    @Inject
    private Logger logger;

    @Override
    public PatientData<String> load(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseObject data = doc.getXObject(CLASS_REFERENCE);
            if (data == null) {
                return null;
            }
            Map<String, String> result = new LinkedHashMap<>();
            for (String propertyName : getProperties()) {
                String value = data.getStringValue(propertyName);
                result.put(propertyName, value);
            }
            return new DictionaryPatientData<>(getName(), result);
        } catch (Exception e) {
            this.logger.error("Could not find requested document or some unforeseen"
                + " error has occurred during controller loading ", e.getMessage());
        }
        return null;
    }

    @Override
    public void save(Patient patient, DocumentModelBridge doc)
    {
        BaseObject xwikiDataObject = ((XWikiDocument) doc).getXObject(CLASS_REFERENCE);
        if (xwikiDataObject == null) {
            throw new IllegalArgumentException("This patient does not have an encrypted data class.");
        }

        PatientData<String> data = patient.<String>getData(this.getName());
        if (!data.isNamed()) {
            return;
        }
        for (String property : this.getProperties()) {
            xwikiDataObject.setStringValue(property, data.get(property));
        }
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
        writeJSON(patient, json, null);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        PatientData<String> data = patient.getData(getName());
        if (data == null || !data.isNamed()) {
            return;
        }

        Iterator<Map.Entry<String, String>> dataIterator = data.dictionaryIterator();
        JSONObject container = json.optJSONObject(getJsonPropertyName());

        while (dataIterator.hasNext()) {
            Map.Entry<String, String> datum = dataIterator.next();
            String key = datum.getKey();
            if (selectedFieldNames == null || selectedFieldNames.contains(key)) {
                if (container == null) {
                    // put() is placed here because we want to create the property iff at least one field is set/enabled
                    json.put(getJsonPropertyName(), new JSONObject());
                    container = json.optJSONObject(getJsonPropertyName());
                }
                container.put(key, datum.getValue());
            }
        }
    }

    @Override
    public PatientData<String> readJSON(JSONObject json)
    {
        if (!json.has(this.getJsonPropertyName())) {
            // no data supported by this controller is present in provided JSON
            return null;
        }
        Map<String, String> result = new LinkedHashMap<>();

        // since the loader always returns dictionary data, this should always be a block.
        Object jsonBlockObject = json.get(this.getJsonPropertyName());
        if (!(jsonBlockObject instanceof JSONObject)) {
            return null;
        }
        JSONObject jsonBlock = (JSONObject) jsonBlockObject;
        for (String property : this.getProperties()) {
            if (jsonBlock.has(property)) {
                result.put(property, jsonBlock.getString(property));
            }
        }

        return new DictionaryPatientData<>(this.getName(), result);
    }

    protected List<String> getProperties()
    {
        return Arrays.asList("first_name", "last_name");
    }

    @Override
    public String getName()
    {
        return "patientName";
    }

    protected String getJsonPropertyName()
    {
        return "patient_name";
    }
}
