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
import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import java.util.Collection;
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
public class BiospecimensController implements PatientDataController<Biospecimen>
{

    private static final String CLASS_NAME = "BiospecimenClass";

    /**
     * A reference to a document representing a Biospecimen.
     */
    private static final EntityReference CLASS_REFERENCE = getClassReference();

    /** The name of the data this controller handles. */
    private static final String DATA_NAME = "biospecimens";

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
    public PatientData<Biospecimen> load(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());

            List<BaseObject> biospecimenXWikiObjects = doc.getXObjects(CLASS_REFERENCE);
            if (CollectionUtils.isEmpty(biospecimenXWikiObjects)) {
                return null;
            }

            List<Biospecimen> biospecimens = new LinkedList<>();

            for (BaseObject biospecimenObject : biospecimenXWikiObjects) {

                if (biospecimenObject == null || biospecimenObject.getFieldList().isEmpty()) {
                    continue;
                }

                biospecimens.add(new Biospecimen(biospecimenObject));
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
            PatientData<Biospecimen> data = patient.getData(getName());
            if (data == null || !data.isIndexed()) {
                return;
            }

            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            if (doc == null) {
                throw new NullPointerException(ERROR_MESSAGE_NO_PATIENT_CLASS);
            }

            XWikiContext context = this.xContextProvider.get();
            doc.removeXObjects(CLASS_REFERENCE);

            for (Biospecimen biospecimen : data) {
                if (isBiospecimenValid(biospecimen)) {
                    this.addBiospecimenToDoc(biospecimen, doc, context);
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
        if (selectedFieldNames != null && !Biospecimen.PROPERTIES.containsAll(selectedFieldNames)) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Some of the given selectedFieldNames [{}] do not belong to the allowed values [{}]",
                    selectedFieldNames, Biospecimen.PROPERTIES);
            }
            return;
        }

        PatientData<Biospecimen> patientData = patient.getData(getName());

        if (patientData == null || patientData.size() == 0) {
            return;
        }

        JSONArray biospecimensJsonArray = new JSONArray();

        for (Biospecimen biospecimen : patientData) {
            if (biospecimen == null) {
                continue;
            }
            JSONObject obj = biospecimen.toJSON(selectedFieldNames);
            if (obj != null) {
                biospecimensJsonArray.put(obj);
            }
        }

        if (biospecimensJsonArray.length() > 0) {
            jsonObject.put(getName(), biospecimensJsonArray);
        }
    }

    @Override
    public PatientData<Biospecimen> readJSON(JSONObject jsonObject)
    {
        if (jsonObject == null || jsonObject.optJSONArray(getName()) == null) {
            return null;
        }

        JSONArray biospecimensJsonArray = jsonObject.getJSONArray(getName());

        List<Biospecimen> result = new LinkedList<>();

        try {
            for (Object biospecimenObject : biospecimensJsonArray) {
                if (biospecimenObject instanceof JSONObject) {
                    result.add(new Biospecimen((JSONObject) biospecimenObject));
                }
            }
        } catch (IllegalArgumentException | UnsupportedOperationException e) {
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

    private void addBiospecimenToDoc(Biospecimen biospecimen, XWikiDocument doc, XWikiContext context)
    {
        try {
            BaseObject xWikiObject = doc.newXObject(CLASS_REFERENCE, context);

            if (biospecimen.hasType()) {
                xWikiObject.set(Biospecimen.TYPE_PROPERTY_NAME, biospecimen.getType(), context);
            }

            if (biospecimen.hasDateCollected()) {
                xWikiObject.set(Biospecimen.DATE_COLLECTED_PROPERTY_NAME, biospecimen.getDateCollected(), context);
            }

            if (biospecimen.hasDateReceived()) {
                xWikiObject.set(Biospecimen.DATE_RECEIVED_PROPERTY_NAME, biospecimen.getDateReceived(), context);
            }

        } catch (Exception e) {
            this.logger.error("Failed to save a specific biospecimen: [{}]", e.getMessage());
        }
    }

    private static boolean isBiospecimenValid(Biospecimen biospecimen)
    {
        return biospecimen != null && biospecimen.isNotEmpty();
    }
}
