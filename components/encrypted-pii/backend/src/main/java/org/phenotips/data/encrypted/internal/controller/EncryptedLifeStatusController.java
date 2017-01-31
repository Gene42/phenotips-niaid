/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.encrypted.internal.controller;

import org.phenotips.Constants;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.PhenoTipsDate;
import org.phenotips.data.SimpleValuePatientData;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Handles the patient's life status, alive or deceased, based on the encrypted {@code date_of_death} and the
 * unencrypted {@code date_of_death_unknown}.
 *
 * @version $Id$
 * @since 1.0B4
 */
@Component(roles = { PatientDataController.class })
@Named("lifeStatus")
@Singleton
public class EncryptedLifeStatusController implements PatientDataController<String>
{
    private static final String DATA_NAME = "life_status";

    private static final String PATIENT_UNKNOWN_DATEOFDEATH_FIELDNAME = "date_of_death_unknown";

    private static final String PATIENT_DATEOFDEATH_FIELDNAME = "date_of_death";

    private static final String PATIENT_DATEOFDEATH_ENTERED_FIELDNAME = "date_of_death_entered";

    private static final String ALIVE = "alive";

    private static final String DECEASED = "deceased";

    private static final Set<String> ALL_LIFE_STATES = new HashSet<>(Arrays.asList(ALIVE, DECEASED));

    /** The XClass used for storing patient data. */
    private static final EntityReference ENCRYPTED_DATA_CLASS_REFERENCE =
        new EntityReference("EncryptedPatientDataClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Provides access to the underlying data storage. */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Override
    public PatientData<String> load(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseObject encryptedData = doc.getXObject(ENCRYPTED_DATA_CLASS_REFERENCE);
            BaseObject plainData = doc.getXObject(Patient.CLASS_REFERENCE);
            if (encryptedData == null || plainData == null) {
                return null;
            }

            String lifeStatus = ALIVE;
            String dodEntered = encryptedData.getStringValue(PATIENT_DATEOFDEATH_ENTERED_FIELDNAME);
            if (StringUtils.isNotBlank(dodEntered) && !"{}".equals(dodEntered)) {
                lifeStatus = DECEASED;
            } else {
                // check if "unknown death date" checkbox is checked
                Integer deathDateUnknown = plainData.getIntValue(PATIENT_UNKNOWN_DATEOFDEATH_FIELDNAME);
                if (deathDateUnknown == 1) {
                    lifeStatus = DECEASED;
                }
            }
            return new SimpleValuePatientData<>(DATA_NAME, lifeStatus);
        } catch (Exception e) {
            this.logger.error("Could not find requested document or some unforeseen"
                + " error has occurred during controller loading ", e.getMessage());
        }
        return null;
    }

    @Override
    public void save(Patient patient, DocumentModelBridge doc)
    {
        BaseObject data = ((XWikiDocument) doc).getXObject(Patient.CLASS_REFERENCE);
        if (data == null) {
            throw new NullPointerException(ERROR_MESSAGE_NO_PATIENT_CLASS);
        }

        PatientData<String> lifeStatus = patient.getData(DATA_NAME);
        PatientData<PhenoTipsDate> dates = patient.getData("dates");

        Integer deathDateUnknown = 0;
        if (lifeStatus != null && DECEASED.equals(lifeStatus.getValue())) {
            deathDateUnknown = 1;
        }
        if (dates != null && dates.isNamed()) {
            PhenoTipsDate deathDate = dates.get(PATIENT_DATEOFDEATH_FIELDNAME);
            // check if date_of_death is set - if it is unknown_death_date should be unset
            if (deathDate != null && deathDate.isSet()) {
                deathDateUnknown = 0;
            }
        }

        data.setIntValue(PATIENT_UNKNOWN_DATEOFDEATH_FIELDNAME, deathDateUnknown);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
        writeJSON(patient, json, null);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        if (selectedFieldNames != null && !selectedFieldNames.contains(DATA_NAME)) {
            return;
        }
        PatientData<String> lifeStatusData = patient.getData(DATA_NAME);
        if (lifeStatusData == null) {
            if (selectedFieldNames != null && selectedFieldNames.contains(DATA_NAME)) {
                json.put(DATA_NAME, ALIVE);
            }
            return;
        }
        json.put(DATA_NAME, lifeStatusData.getValue());
    }

    @Override
    public PatientData<String> readJSON(JSONObject json)
    {
        String propertyValue = json.optString(DATA_NAME, null);
        if (propertyValue != null) {
            // validate - only accept listed values
            if (ALL_LIFE_STATES.contains(propertyValue)) {
                return new SimpleValuePatientData<>(DATA_NAME, propertyValue);
            }
        }
        return null;
    }

    @Override
    public String getName()
    {
        return DATA_NAME;
    }
}
