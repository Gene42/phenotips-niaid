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

import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.SimpleValuePatientData;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Collection;
import java.util.LinkedList;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Test for the {@link SexController} component, implementation of the {@link org.phenotips.data.PatientDataController}
 * interface
 */
public class SexControllerTest
{
    @Rule
    public MockitoComponentMockingRule<PatientDataController<String>> mocker =
        new MockitoComponentMockingRule<PatientDataController<String>>(SexController.class);

    private DocumentAccessBridge documentAccessBridge;

    private Execution execution;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private XWiki xwiki;

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    @Mock
    private BaseObject data;

    private static final String DATA_NAME = "sex";

    private static final String INTERNAL_PROPERTY_NAME = "gender";

    private static final String SEX_MALE = "M";

    private static final String SEX_FEMALE = "F";

    private static final String SEX_OTHER = "O";

    private static final String SEX_UNKNOWN = "U";

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.documentAccessBridge = this.mocker.getInstance(DocumentAccessBridge.class);
        this.execution = this.mocker.getInstance(Execution.class);

        DocumentReference patientDocument = new DocumentReference("wiki", "patient", "00000001");
        doReturn(patientDocument).when(this.patient).getDocument();
        doReturn(this.doc).when(this.documentAccessBridge).getDocument(patientDocument);
        doReturn(this.data).when(this.doc).getXObject(Patient.CLASS_REFERENCE);

        doReturn(this.executionContext).when(this.execution).getContext();
        doReturn(this.xcontext).when(this.executionContext).getProperty("xwikicontext");
        doReturn(this.xwiki).when(this.xcontext).getWiki();
    }

    @Test
    public void loadCatchesExceptionFromDocumentAccess() throws Exception
    {
        doThrow(Exception.class).when(this.documentAccessBridge).getDocument(any(DocumentReference.class));

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);

        verify(this.mocker.getMockedLogger()).error("Failed to load patient gender: [{}]", (String) null);
        Assert.assertNull(result);
    }

    @Test
    public void loadCatchesExceptionWhenPatientDoesNotHavePatientClass() throws ComponentLookupException
    {
        doReturn(null).when(this.doc).getXObject(Patient.CLASS_REFERENCE);

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNull(result);
    }

    @Test
    public void loadReturnsCorrectSex() throws ComponentLookupException
    {
        doReturn(SEX_MALE).when(this.data).getStringValue(INTERNAL_PROPERTY_NAME);
        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);
        Assert.assertEquals(SEX_MALE, result.getValue());

        doReturn(SEX_FEMALE).when(this.data).getStringValue(INTERNAL_PROPERTY_NAME);
        result = this.mocker.getComponentUnderTest().load(this.patient);
        Assert.assertEquals(SEX_FEMALE, result.getValue());

        doReturn(SEX_OTHER).when(this.data).getStringValue(INTERNAL_PROPERTY_NAME);
        result = this.mocker.getComponentUnderTest().load(this.patient);
        Assert.assertEquals(SEX_OTHER, result.getValue());
    }

    @Test
    public void loadReturnsSexUnknown() throws ComponentLookupException
    {
        doReturn(SEX_UNKNOWN).when(this.data).getStringValue(INTERNAL_PROPERTY_NAME);
        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);
        Assert.assertEquals(SEX_UNKNOWN, result.getValue());

        doReturn("!!!!!").when(this.data).getStringValue(INTERNAL_PROPERTY_NAME);
        result = this.mocker.getComponentUnderTest().load(this.patient);
        Assert.assertEquals(SEX_UNKNOWN, result.getValue());

        doReturn(null).when(this.data).getStringValue(INTERNAL_PROPERTY_NAME);
        result = this.mocker.getComponentUnderTest().load(this.patient);
        Assert.assertEquals(SEX_UNKNOWN, result.getValue());
    }

    @Test
    public void saveCatchesExceptionFromDocumentAccess() throws Exception
    {
        doThrow(Exception.class).when(this.documentAccessBridge).getDocument(any(DocumentReference.class));

        this.mocker.getComponentUnderTest().save(this.patient);

        verify(this.mocker.getMockedLogger()).error("Failed to save patient gender: [{}]", (String) null);
    }

    @Test
    public void saveCatchesExceptionWhenPatientDoesNotHavePatientClass() throws ComponentLookupException
    {
        doReturn(null).when(this.doc).getXObject(Patient.CLASS_REFERENCE);

        this.mocker.getComponentUnderTest().save(this.patient);

        verify(this.mocker.getMockedLogger()).error("Failed to save patient gender: [{}]",
            PatientDataController.ERROR_MESSAGE_NO_PATIENT_CLASS);
    }

    @Test
    public void saveCatchesExceptionFromSaveDocument() throws XWikiException, ComponentLookupException
    {
        XWikiException exception = new XWikiException();
        doThrow(exception).when(this.xwiki).saveDocument(any(XWikiDocument.class),
            anyString(), anyBoolean(), any(XWikiContext.class));
        doReturn(new SimpleValuePatientData<String>(DATA_NAME, SEX_MALE)).when(this.patient).getData(DATA_NAME);

        this.mocker.getComponentUnderTest().save(this.patient);

        verify(this.xwiki).saveDocument(any(XWikiDocument.class),
            anyString(), anyBoolean(), any(XWikiContext.class));
        verify(this.mocker.getMockedLogger()).error("Failed to save patient gender: [{}]",
            exception.getMessage());
    }

    @Test
    public void saveSetsCorrectSex() throws XWikiException, ComponentLookupException
    {
        doReturn(new SimpleValuePatientData<String>(DATA_NAME, SEX_MALE)).when(this.patient).getData(DATA_NAME);
        this.mocker.getComponentUnderTest().save(this.patient);
        verify(this.data).setStringValue(INTERNAL_PROPERTY_NAME, SEX_MALE);
        verify(this.xwiki).saveDocument(this.doc, "Updated gender from JSON", true, this.xcontext);

        Mockito.reset(this.xwiki);
        doReturn(new SimpleValuePatientData<String>(DATA_NAME, SEX_FEMALE)).when(this.patient).getData(DATA_NAME);
        this.mocker.getComponentUnderTest().save(this.patient);
        verify(this.data).setStringValue(INTERNAL_PROPERTY_NAME, SEX_FEMALE);
        verify(this.xwiki).saveDocument(this.doc, "Updated gender from JSON", true, this.xcontext);

        Mockito.reset(this.xwiki);
        doReturn(new SimpleValuePatientData<String>(DATA_NAME, SEX_OTHER)).when(this.patient).getData(DATA_NAME);
        this.mocker.getComponentUnderTest().save(this.patient);
        verify(this.data).setStringValue(INTERNAL_PROPERTY_NAME, SEX_OTHER);
        verify(this.xwiki).saveDocument(this.doc, "Updated gender from JSON", true, this.xcontext);

        Mockito.reset(this.xwiki);
        doReturn(new SimpleValuePatientData<String>(DATA_NAME, SEX_UNKNOWN)).when(this.patient).getData(DATA_NAME);
        this.mocker.getComponentUnderTest().save(this.patient);
        verify(this.data).setStringValue(INTERNAL_PROPERTY_NAME, SEX_UNKNOWN);
        verify(this.xwiki).saveDocument(this.doc, "Updated gender from JSON", true, this.xcontext);

        Mockito.reset(this.xwiki);
        doReturn(new SimpleValuePatientData<String>(DATA_NAME, null)).when(this.patient).getData(DATA_NAME);
        this.mocker.getComponentUnderTest().save(this.patient);
        verify(this.data).setStringValue(INTERNAL_PROPERTY_NAME, null);
        verify(this.xwiki).saveDocument(this.doc, "Updated gender from JSON", true, this.xcontext);
    }

    @Test
    public void writeJSONReturnsWhenGetDataReturnsNull() throws ComponentLookupException
    {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);

        Assert.assertFalse(json.has(DATA_NAME));
    }

    @Test
    public void writeJSONWithSelectedFieldsReturnsWhenGetDataReturnsNull() throws ComponentLookupException
    {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertFalse(json.has(DATA_NAME));
    }

    @Test
    public void writeJSONWithSelectedFieldsReturnsWhenGenderNotSelected() throws ComponentLookupException
    {
        doReturn(new SimpleValuePatientData<>(DATA_NAME, SEX_MALE)).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add("dates");

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertFalse(json.has(DATA_NAME));
    }

    @Test
    public void writeJSONAddsSex() throws ComponentLookupException
    {
        doReturn(new SimpleValuePatientData<>(DATA_NAME, SEX_FEMALE)).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);

        Assert.assertEquals(SEX_FEMALE, json.get(DATA_NAME));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsSex() throws ComponentLookupException
    {
        doReturn(new SimpleValuePatientData<>(DATA_NAME, SEX_FEMALE)).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(INTERNAL_PROPERTY_NAME);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertEquals(SEX_FEMALE, json.get(DATA_NAME));
    }

    @Test
    public void readJSONEmptyJsonReturnsNull() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().readJSON(new JSONObject()));
    }

    @Test
    public void readJSONReturnsCorrectSex() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();
        json.put(DATA_NAME, SEX_MALE);
        PatientData<String> result = this.mocker.getComponentUnderTest().readJSON(json);
        Assert.assertEquals(SEX_MALE, result.getValue());

        json = new JSONObject();
        json.put(DATA_NAME, SEX_FEMALE);
        result = this.mocker.getComponentUnderTest().readJSON(json);
        Assert.assertEquals(SEX_FEMALE, result.getValue());

        json = new JSONObject();
        json.put(DATA_NAME, SEX_OTHER);
        result = this.mocker.getComponentUnderTest().readJSON(json);
        Assert.assertEquals(SEX_OTHER, result.getValue());
    }

    @Test
    public void readJSONReturnsUnknownSex() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();
        json.put(DATA_NAME, SEX_UNKNOWN);

        PatientData<String> result = this.mocker.getComponentUnderTest().readJSON(json);

        // Change this line to: Assert.assertEquals(SEX_UNKNOWN, result.getValue())
        // if PhenoTips Implementation Changes to Support Unknown Gender
        Assert.assertEquals("", result.getValue());
    }

    @Test
    public void checkGetName() throws ComponentLookupException
    {
        Assert.assertEquals(DATA_NAME, this.mocker.getComponentUnderTest().getName());
    }
}
