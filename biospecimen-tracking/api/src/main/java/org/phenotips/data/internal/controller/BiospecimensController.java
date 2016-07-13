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

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

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
     * A list of all the property names of the BiospecimenClass.
     */
    private static final List<String> PROPERTIES = Arrays.asList(PROPERTY_NAME_TYPE, PROPERTY_NAME_DATE_COLLECTED,
        PROPERTY_NAME_DATE_RECEIVED);

    /**
     * A reference to a document representing a Biospecimen.
     */
    private static final EntityReference CLASS_REFERENCE =
        new EntityReference("BiospecimenClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    /**
     * Logging helper object.
     */
    @Inject
    private Logger logger;

    //@Inject
    //private Provider<XWikiContext> xcontext;

    /**
     * Provides access to the underlying data storage.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Returns the value of the given object based on the type of property.
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
                if (dateField == null) {
                    return null;
                }
                return dateField.toText();

            default:
                return null;
        }
    }

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
                logger.warn("biospecimenXWikiObject is null");
                return null;
            }

            //@SuppressWarnings("unchecked")
            //Collection fields = biospecimenXWikiObject.getFieldList();
            //biospecimenXWikiObject.getFie
            //logger.error(biospecimenXWikiObject.toString());
            //if (CollectionUtils.isEmpty(fields)) {
           //     logger.warn("Empty fields");
            //    return null;
           // }

            //BaseObject biospecimensData = (BaseObject) fields.iterator().next();
            List<BaseObject> biospecimenXWikiObjects = doc.getXObjects(CLASS_REFERENCE);
            if (CollectionUtils.isEmpty(biospecimenXWikiObjects)) {
                logger.warn("biospecimenXWikiObject is null");
                return null;
            }

            List<Map<String, String>> biospecimens = new LinkedList<>();

            //Map<String, String> biospecimen = new LinkedHashMap<>();

            //biospecimen.put("TEST", "HAHAHAHAHAH");
           // biospecimens.add(biospecimen);
            for (BaseObject biospecimenObject : biospecimenXWikiObjects) {
                logger.error(biospecimenObject.toString());

                if (biospecimenObject == null || biospecimenObject.getFieldList().isEmpty()) {
                    logger.warn("" + biospecimenObject.getFieldList().isEmpty());
                    continue;
                }

                Map<String, String> biospecimen = new LinkedHashMap<>();

                for (String property : PROPERTIES) {

                    String value = getValue(biospecimenObject, property);

                    if (value == null) {
                        continue;
                    }

                    biospecimen.put(property, value);
                }

                biospecimens.add(biospecimen);
            }

            if (CollectionUtils.isEmpty(biospecimens)) {
                return null;
            } else {
                return new IndexedPatientData<>(getName(), biospecimens);
            }
           // return new IndexedPatientData<>(getName(), biospecimens);
        } catch (Exception e) {
            this.logger.error("Could not find requested document or some unforeseen"
                + " error has occurred during controller loading: " + e.getMessage());
        }
        return null;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void save(Patient patient)
    {

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

        //if (selectedFieldNames != null && !PROPERTIES.containsAll(selectedFieldNames)) {
        //    return;
        //}

        PatientData<Map<String, String>> biospecimensData = patient.getData(getName());
        if (biospecimensData == null || biospecimensData.size() == 0) {
            if (this.logger.isDebugEnabled())
            {
                this.logger.debug("Biospecimen data is null or empty");
            }
        } else {
            JSONArray biospecimensJsonArray = new JSONArray();
            for (Map<String, String> biospecimen : biospecimensData) {

                JSONObject biospecimenObject = new JSONObject();

                for (String propertyName : PROPERTIES) {
                    biospecimenObject.put(propertyName, biospecimen.get(propertyName));
                }

                biospecimensJsonArray.put(biospecimenObject);
            }

            jsonObject.put(getName(), biospecimensJsonArray);
        }
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public PatientData<Map<String, String>> readJSON(JSONObject jsonObject)
    {
        return null;
    }
}
