/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.genetics.internal;

import org.phenotips.Constants;
import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.internal.controller.AbstractComplexController;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Handles the patient's sequencing report information.
 *
 * @version $Id$
 * @since 1.3M1R2
 */
@Component(roles = { PatientDataController.class })
@Named("sequencing_report")
@Singleton
public class SequencingReportController extends AbstractComplexController<SequencingReport>
{
    /** The XClass used for storing sequencing report data. */
    private static final EntityReference SEQUENCINGREPORT_CLASS_REFERENCE = new EntityReference("SequencingReportClass",
        EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String SEQUENCINGREPORTS_STRING = "sequencing_reports";

    private static final String CONTROLLER_NAME = SEQUENCINGREPORTS_STRING;

    private static final String SEQUENCINGREPORTS_ENABLING_FIELD_NAME = SEQUENCINGREPORTS_STRING;

    @Inject
    private Logger logger;

    /** Provides access to the current execution context. */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public String getName()
    {
        return CONTROLLER_NAME;
    }

    @Override
    protected String getJsonPropertyName()
    {
        return CONTROLLER_NAME;
    }

    @Override
    protected List<String> getProperties()
    {
        return SequencingReport.getProperties();
    }

    @Override
    protected List<String> getBooleanFields()
    {
        return Collections.emptyList();
    }

    @Override
    protected List<String> getCodeFields()
    {
        return Collections.emptyList();
    }

    @Override
    public PatientData<SequencingReport> load(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            List<BaseObject> xObjects = doc.getXObjects(SEQUENCINGREPORT_CLASS_REFERENCE);
            if (xObjects == null || xObjects.isEmpty()) {
                return null;
            }

            List<SequencingReport> allInternalSequencingReports = new LinkedList<>();
            for (BaseObject reportXObj : xObjects) {
                if (reportXObj == null || reportXObj.getFieldList().isEmpty()) {
                    continue;
                }
                SequencingReport singleReport = new SequencingReport(reportXObj);
                if (singleReport == null) {
                    continue;
                }
                allInternalSequencingReports.add(singleReport);
            }

            if (allInternalSequencingReports.isEmpty()) {
                return null;
            } else {
                return new IndexedPatientData<>(getName(), allInternalSequencingReports);
            }
        } catch (Exception e) {
            this.logger.error("Could not find requested document or some unforeseen "
                + "error has occurred during sequencing reports controller loading: [{}]", e.getMessage());
        }
        return null;
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        if (selectedFieldNames != null && !selectedFieldNames.contains(SEQUENCINGREPORTS_ENABLING_FIELD_NAME)) {
            return;
        }

        PatientData<SequencingReport> data = patient.getData(getName());
        if (data == null) {
            return;
        }
        Iterator<SequencingReport> iterator = data.iterator();
        if (!iterator.hasNext()) {
            return;
        }

        // put() is placed here because we want to create the property iff at least one field is set/enabled
        // (by this point we know there is some data since iterator.hasNext() == true)
        json.put(getJsonPropertyName(), new JSONArray());
        JSONArray container = json.getJSONArray(getJsonPropertyName());

        while (iterator.hasNext()) {
            SequencingReport singleReport = iterator.next();
            container.put(singleReport.toJSON()); // might need to declare new jsonobject
        }
    }

    @Override
    public PatientData<SequencingReport> readJSON(JSONObject json)
    {
        if (!json.has(getJsonPropertyName()) || json.optJSONArray(getJsonPropertyName()) == null) {
            return null;
        }

        try {
            List<SequencingReport> allParsedSequencingReports = new LinkedList<>();
            JSONArray reportsJson = json.getJSONArray(getJsonPropertyName());
            for (int i = 0; i < reportsJson.length(); i++) {
                JSONObject reportJson = reportsJson.getJSONObject(i);
                SequencingReport singleReport = new SequencingReport(reportJson);
                if (singleReport == null) {
                    continue;
                }
                allParsedSequencingReports.add(singleReport);
            }
            if (allParsedSequencingReports.isEmpty()) {
                return null;
            } else {
                return new IndexedPatientData<>(getName(), allParsedSequencingReports);
            }
        } catch (Exception e) {
            this.logger.error("Could not load sequencing reports from JSON: [{}]", e.getMessage());
        }
        return null;
    }

    @Override
    public void save(Patient patient)
    {
        try {
            PatientData<SequencingReport> data = patient.getData(this.getName());
            if (data == null || !data.isIndexed()) {
                return;
            }

            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            if (doc == null) {
                throw new NullPointerException(ERROR_MESSAGE_NO_PATIENT_CLASS);
            }

            XWikiContext context = this.xcontextProvider.get();
            doc.removeXObjects(SEQUENCINGREPORT_CLASS_REFERENCE);
            Iterator<SequencingReport> iterator = data.iterator();
            while (iterator.hasNext()) {
                try {
                    SequencingReport singleReport = iterator.next();
                    BaseObject xwikiObject = doc.newXObject(SEQUENCINGREPORT_CLASS_REFERENCE, context);

                    singleReport.populateXWikiObject(xwikiObject, context);

                } catch (Exception e) {
                    this.logger.error("Failed to save a specific sequencing report: [{}]", e.getMessage());
                }
            }

            context.getWiki().saveDocument(doc, "Updated genes from JSON", true, context);
        } catch (Exception e) {
            this.logger.error("Failed to save sequencing reports: [{}]", e.getMessage());
        }
    }
}
