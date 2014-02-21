/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.data.internal.controller;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.ontology.OntologyManager;
import org.phenotips.ontology.OntologyTerm;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import net.sf.json.JSONObject;

/**
 * Handles the patient's date of birth and the exam date.
 * 
 * @version $Id$
 * @since 1.0M10
 */
@Component(roles = { PatientDataController.class })
@Named("global-qualifiers")
@Singleton
public class GlobalQualifiersController implements PatientDataController<ImmutablePair<String, OntologyTerm>>
{
    private static final String DATA_NAME = "global-qualifiers";

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Provides access to the underlying data storage. */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private OntologyManager ontologyManager;

    @Override
    public PatientData<ImmutablePair<String, OntologyTerm>> initialize(Patient patient)
    {
        return null;
    }

    @Override
    public PatientData<ImmutablePair<String, OntologyTerm>> load(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                throw new NullPointerException("The patient does not have a PatientClass");
            }
            List<ImmutablePair<String, OntologyTerm>> result = new LinkedList<ImmutablePair<String, OntologyTerm>>();
            for (String propertyName : getProperties()) {
                String propertyValue = data.getStringValue(propertyName);
                if (StringUtils.isNotBlank(propertyValue)) {
                    result.add(ImmutablePair.of(propertyName, this.ontologyManager.resolveTerm(propertyValue)));
                }
            }
            return new SimpleNamedData<OntologyTerm>(DATA_NAME, result);
        } catch (Exception e) {
            this.logger.error("Could not find requested document");
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
        for (ImmutablePair<String, OntologyTerm> data : patient.<ImmutablePair<String, OntologyTerm>>getData(DATA_NAME))
        {
            OntologyTerm term = data.getValue();
            JSONObject element = new JSONObject();
            element.put("id", term.getId());
            element.put("label", term.getName());
            json.put(data.getKey(), element);
        }
    }

    @Override
    public PatientData<ImmutablePair<String, OntologyTerm>> readJSON(JSONObject json)
    {
        throw new UnsupportedOperationException();
    }

    protected List<String> getProperties()
    {
        return Arrays.asList("global_age_of_onset", "global_mode_of_inheritance");
    }
}
