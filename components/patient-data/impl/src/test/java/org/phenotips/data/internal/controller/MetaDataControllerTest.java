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
import org.phenotips.data.SimpleValuePatientData;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.doc.XWikiDocument;

import net.sf.json.JSONObject;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Test for the {@link MetaDataController} Component, only the overridden methods from {@link AbstractSimpleController}
 * are tested here
 */
public class MetaDataControllerTest
{

    @Rule
    public MockitoComponentMockingRule<PatientDataController<String>> mocker =
        new MockitoComponentMockingRule<PatientDataController<String>>(MetaDataController.class);

    private DocumentAccessBridge documentAccessBridge;

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    private DocumentReference documentReference;

    private DocumentReference creatorReference;

    private DocumentReference authorReference;

    private Date creationDate;

    private Date date;

    private DateTimeFormatter formatter;

    private static final String UNKNOWN_USER = "Unknown user";

    private static final String DOCUMENT_NAME = "doc.name";

    private static final String DOCUMENT_NAME_STRING = "report_id";

    private static final String REFERRER = "referrer";

    private static final String CREATION_DATE = "creationDate";

    private static final String AUTHOR = "author";

    private static final String AUTHOR_STRING = "last_modified_by";

    private static final String DATE = "date";

    private static final String DATE_STRING = "last_modification_date";

    private static final String CONTROLLER_NAME = "metadata";

    private static final String DATA_NAME = CONTROLLER_NAME;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.documentAccessBridge = this.mocker.getInstance(DocumentAccessBridge.class);

        DocumentReference patientDocument = new DocumentReference("wiki", "patient", "00000001");
        doReturn(patientDocument).when(this.patient).getDocument();
        doReturn(this.doc).when(this.documentAccessBridge).getDocument(patientDocument);

        this.documentReference = new DocumentReference("wiki", "phenotips", "document");
        doReturn(this.documentReference).when(this.doc).getDocumentReference();
        this.creatorReference = new DocumentReference("wiki", "phenotips", "creator");
        doReturn(this.creatorReference).when(this.doc).getCreatorReference();
        this.authorReference = new DocumentReference("wiki", "phenotips", "author");
        doReturn(this.authorReference).when(this.doc).getAuthorReference();
        this.date = new Date(10);
        doReturn(this.date).when(this.doc).getDate();
        this.creationDate = new Date(0);
        doReturn(this.creationDate).when(this.doc).getCreationDate();

        this.formatter = ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC);
    }

    @Test
    public void checkGetName() throws ComponentLookupException
    {
        Assert.assertEquals("metadata", this.mocker.getComponentUnderTest().getName());
    }

    @Test
    public void checkGetJsonPropertyName() throws ComponentLookupException
    {
        Assert.assertNull(((AbstractSimpleController) this.mocker.getComponentUnderTest()).getJsonPropertyName());
    }

    @Test
    public void checkGetProperties() throws ComponentLookupException
    {
        List<String> result = ((AbstractSimpleController) this.mocker.getComponentUnderTest()).getProperties();
        Assert.assertTrue(result.isEmpty());
    }


    //--------------------load() is Overridden from AbstractSimpleController--------------------

    @Test
    public void loadCatchesExceptionFromDocumentAccess() throws Exception
    {
        Exception exception = new Exception();
        doThrow(exception).when(this.documentAccessBridge).getDocument(any(DocumentReference.class));

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);

        verify(this.mocker.getMockedLogger()).error("Could not find requested document or some unforeseen"
            + " error has occurred during controller loading ", exception.getMessage());
        Assert.assertNull(result);
    }

    @Test
    public void loadAddsAllReferences() throws ComponentLookupException
    {
        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertEquals(5, result.size());
        Assert.assertEquals(this.documentReference.getName(), result.get(DOCUMENT_NAME));
        Assert.assertEquals(this.creatorReference.getName(), result.get(REFERRER));
        Assert.assertEquals(this.authorReference.getName(), result.get(AUTHOR));
        Assert.assertEquals(this.formatter.print(new DateTime(this.date)), result.get(DATE));
        Assert.assertEquals(this.formatter.print(new DateTime(this.creationDate)), result.get(CREATION_DATE));
    }

    @Test
    public void loadSetsUnknownUserWhenCreatorIsNull() throws ComponentLookupException
    {
        doReturn(null).when(this.doc).getCreatorReference();

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertEquals(UNKNOWN_USER, result.get(REFERRER));
    }

    @Test
    public void loadSetsUnknownUserWhenAuthorIsNull() throws ComponentLookupException
    {
        doReturn(null).when(this.doc).getAuthorReference();

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertEquals(UNKNOWN_USER, result.get(AUTHOR));
    }

    //--------------------writeJSON() is Overridden from AbstractSimpleController--------------------

    @Test
    public void writeJSONWithSelectedFieldsReturnsWhenGetDataReturnsNull() throws ComponentLookupException
    {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertTrue(json.isEmpty());
    }

    @Test
    public void writeJSONWithSelectedFieldsReturnsWhenDataIsNotKeyValueBased() throws ComponentLookupException
    {
        PatientData<String> patientData = new SimpleValuePatientData<>(DATA_NAME, "datum");
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertTrue(json.isEmpty());
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsAllValuesAndConvertedJsonKeys() throws ComponentLookupException
    {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(DOCUMENT_NAME, this.documentReference.getName());
        map.put(REFERRER, this.creatorReference.getName());
        map.put(CREATION_DATE, this.formatter.print(new DateTime(this.creationDate)));
        map.put(AUTHOR, this.authorReference.getName());
        map.put(DATE, this.formatter.print(new DateTime(this.date)));
        PatientData<String> patientData = new DictionaryPatientData<String>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(DOCUMENT_NAME);
        selectedFields.add(REFERRER);
        selectedFields.add(CREATION_DATE);
        selectedFields.add(AUTHOR);
        selectedFields.add(DATE);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertEquals(this.documentReference.getName(), json.get(DOCUMENT_NAME_STRING));
        Assert.assertEquals(this.creatorReference.getName(), json.get(REFERRER));
        Assert.assertEquals(this.authorReference.getName(), json.get(AUTHOR_STRING));
        Assert.assertEquals(this.formatter.print(new DateTime(this.creationDate)), json.get(DATE));
        Assert.assertEquals(this.formatter.print(new DateTime(this.date)), json.get(DATE_STRING));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsSelectedValues() throws ComponentLookupException
    {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(DOCUMENT_NAME, this.documentReference.getName());
        map.put(REFERRER, this.creatorReference.getName());
        map.put(CREATION_DATE, this.formatter.print(new DateTime(this.creationDate)));
        map.put(AUTHOR, this.authorReference.getName());
        map.put(DATE, this.formatter.print(new DateTime(this.date)));
        PatientData<String> patientData = new DictionaryPatientData<String>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(DOCUMENT_NAME);
        selectedFields.add(AUTHOR);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertEquals(this.documentReference.getName(), json.get(DOCUMENT_NAME_STRING));
        Assert.assertEquals(this.authorReference.getName(), json.get(AUTHOR_STRING));
        Assert.assertNull(json.get(REFERRER));
        Assert.assertNull(json.get(DATE));
        Assert.assertNull(json.get(DATE_STRING));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsAllValuesWhenSelectedFieldsNull()
        throws ComponentLookupException
    {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(DOCUMENT_NAME, this.documentReference.getName());
        map.put(REFERRER, this.creatorReference.getName());
        map.put(CREATION_DATE, this.formatter.print(new DateTime(this.creationDate)));
        map.put(AUTHOR, this.authorReference.getName());
        map.put(DATE, this.formatter.print(new DateTime(this.date)));
        PatientData<String> patientData = new DictionaryPatientData<String>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, null);

        Assert.assertEquals(this.documentReference.getName(), json.get(DOCUMENT_NAME_STRING));
        Assert.assertEquals(this.creatorReference.getName(), json.get(REFERRER));
        Assert.assertEquals(this.authorReference.getName(), json.get(AUTHOR_STRING));
        Assert.assertEquals(this.formatter.print(new DateTime(this.creationDate)), json.get(DATE));
        Assert.assertEquals(this.formatter.print(new DateTime(this.date)), json.get(DATE_STRING));
    }
}
