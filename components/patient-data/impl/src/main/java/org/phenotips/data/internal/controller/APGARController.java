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

import org.phenotips.data.DictionaryPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import net.sf.json.JSONObject;

/**
 * Handles the two APGAR scores.
 *
 * @version $Id$
 * @since 1.0RC1
 */
@Component(roles = { PatientDataController.class })
@Named("apgar")
@Singleton
public class APGARController implements PatientDataController<Integer>
{
    /** The name of this data. */
    private static final String DATA_NAME = "apgar";

    /** Provides access to the document behind the patient record. */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /** Logging helper object. */
    @Inject
    private Logger logger;

    @Override
    public PatientData<Integer> load(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                throw new NullPointerException(ERROR_MESSAGE_NO_PATIENT_CLASS);
            }
            Map<String, Integer> result = new LinkedHashMap<>();
            for (String propertyName : getProperties()) {
                String value = data.getStringValue(propertyName);
                if (NumberUtils.isDigits(value)) {
                    result.put(propertyName, Integer.valueOf(value));
                }
            }
            return new DictionaryPatientData<Integer>(DATA_NAME, result);
        } catch (Exception e) {
            this.logger.error("Could not find requested document or some unforeseen"
                + " error has occurred during controller loading ", e.getMessage());
        }
        return null;
    }

    @Override
    public void save(Patient patient)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
        writeJSON(patient, json, null);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        if (selectedFieldNames != null && !hasAnySelected(selectedFieldNames)) {
            return;
        }
        PatientData<Integer> data = patient.getData(getName());
        if (data == null || !data.isNamed()) {
            return;
        }

        Iterator<Entry<String, Integer>> iterator = data.dictionaryIterator();
        if (!iterator.hasNext()) {
            return;
        }

        JSONObject container = json.getJSONObject(DATA_NAME);
        if (container == null || container.isNullObject()) {
            json.put(DATA_NAME, new JSONObject());
            container = json.getJSONObject(DATA_NAME);
        }
        while (iterator.hasNext()) {
            Entry<String, Integer> item = iterator.next();
            container.put(item.getKey(), item.getValue());
        }
    }

    /**
     * Checks if any relevant field names were selected.
     *
     * @return true if relevant fields were selected, false otherwise
     */
    private boolean hasAnySelected(Collection<String> selectedFieldNames)
    {
        boolean hasAny = false;
        for (String selectedFieldName : selectedFieldNames) {
            if (StringUtils.startsWithIgnoreCase(selectedFieldName, this.getName())) {
                hasAny = true;
                break;
            }
        }
        return hasAny;
    }

    @Override
    public PatientData<Integer> readJSON(JSONObject json)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName()
    {
        return DATA_NAME;
    }

    protected List<String> getProperties()
    {
        List<String> list = new LinkedList<>();
        list.add("apgar1");
        list.add("apgar5");
        return list;
    }
}
