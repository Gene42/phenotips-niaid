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

import org.phenotips.configuration.RecordConfiguration;
import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import javax.inject.Provider;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Equator;
import org.apache.commons.lang3.ObjectUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.DateProperty;
import com.xpn.xwiki.objects.StringProperty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for BiospecimensController.
 * @version $Id$
 */
public class BiospecimensControllerTest
{

    private static final String JSON_DATE_FORMAT = "yyyy-MM-dd";

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            System.out.println("Running test: " + description.getMethodName());
        }
    };

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument xWikiDoc;

    private static final String CONTROLLER_NAME = new BiospecimensController().getName();

    private List<BaseObject> biospecimensXWikiObjects = new LinkedList<>();

    @Rule
    public MockitoComponentMockingRule<PatientDataController<Biospecimen>> mocker =
        new MockitoComponentMockingRule<PatientDataController<Biospecimen>>(BiospecimensController.class);

    /**
     * Class set up.
     *
     * @throws  Exception  on error
     */
    @BeforeClass
    public static void setUpClass() throws Exception {

    }

    /**
     * Class tear down.
     *
     * @throws  Exception  on error
     */
    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    /**
     * Test set up.
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        DocumentAccessBridge documentAccessBridge = this.mocker.getInstance(DocumentAccessBridge.class);

        DocumentReference patientDocument = new DocumentReference("wiki", "patient", "00000001");
        doReturn(patientDocument).when(this.patient).getDocument();
        doReturn(this.xWikiDoc).when(documentAccessBridge).getDocument(patientDocument);
        this.biospecimensXWikiObjects.clear();
        doReturn(this.biospecimensXWikiObjects).when(this.xWikiDoc).getXObjects(any(EntityReference.class));

        RecordConfiguration recordConfigManagerMock = mock(RecordConfiguration.class);
        doReturn(JSON_DATE_FORMAT).when(recordConfigManagerMock).getISODateFormat();
    }

    /**
     * Test tear down.
     */
    @After
    public void tearDown() {
    }

    @Test
    public void loadTestNoData() throws Exception {
        PatientData<Biospecimen> patientData = this.mocker.getComponentUnderTest().load(this.patient);
        assertNull(patientData);
    }

    @Test
    public void loadTestWithData() throws Exception {

        List<Biospecimen> data = new LinkedList<>();
        data.add(new Biospecimen().setType("Skin"));
        data.add(new Biospecimen().setType("Nails").setDateCollected(new DateTime()));
        data.add(new Biospecimen().setType("Saliva").setDateCollected(new DateTime()).setDateReceived(new DateTime()));

        for (Biospecimen datum : data) {
            this.biospecimensXWikiObjects.add(getBiospecimenObject(datum));
        }

        PatientData<Biospecimen> patientData = this.mocker.getComponentUnderTest().load(this.patient);

        assertNotNull(patientData);

        List<Biospecimen> patientDataResult = getBiospecimenDataFromPatientData(patientData);

        assertTrue(CollectionUtils.isEqualCollection(data, patientDataResult, new BiospecimenDataEquator()));
    }

    @Test
    public void loadTestWithDataIgnoreEmpty() throws Exception {

        List<Biospecimen> data = new LinkedList<>();
        data.add(new Biospecimen());
        data.add(new Biospecimen().setType("Saliva").setDateCollected(new DateTime()).setDateReceived(new DateTime()));

        for (Biospecimen datum : data) {
            this.biospecimensXWikiObjects.add(getBiospecimenObject(datum));
        }

        PatientData<Biospecimen> patientData = this.mocker.getComponentUnderTest().load(this.patient);

        assertNotNull(patientData);

        List<Biospecimen> patientDataResult = getBiospecimenDataFromPatientData(patientData);

        assertFalse(CollectionUtils.isEqualCollection(data, patientDataResult, new BiospecimenDataEquator()));
        assertEquals(patientDataResult.size(), 1);
    }

    @Test
    public void loadReturnsNullWhenPatientDoesNotHaveGeneClass() throws ComponentLookupException
    {
        doReturn(null).when(this.xWikiDoc).getXObjects(any(EntityReference.class));

        PatientData<Biospecimen> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNull(result);
    }

    // --------------------writeJSON() tests
    @Test
    public void writeJSONReturnsWhenGetDataReturnsNull() throws ComponentLookupException
    {
        doReturn(null).when(this.patient).getData(CONTROLLER_NAME);
        JSONObject json = new JSONObject();
        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, null);

        Assert.assertFalse(json.has(CONTROLLER_NAME));
        verify(this.patient).getData(CONTROLLER_NAME);
    }

    @Test
    public void writeJSONReturnsWhenDataIsEmpty() throws ComponentLookupException
    {
        List<Biospecimen> internalList = new LinkedList<>();
        PatientData<Biospecimen> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        doReturn(patientData).when(this.patient).getData(CONTROLLER_NAME);
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, null);

        Assert.assertFalse(json.has(CONTROLLER_NAME));
        verify(this.patient).getData(CONTROLLER_NAME);
    }

    @Test
    public void writeJSONReturnsWhenSelectedFieldsDoesAreInvalid() throws ComponentLookupException
    {
        List<Biospecimen> internalList = new LinkedList<>();
        PatientData<Biospecimen> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        doReturn(patientData).when(this.patient).getData(CONTROLLER_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add("some_string");

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertFalse(json.has(CONTROLLER_NAME));
    }

    @Test
    public void writeJSONDoesNotContainNullFields() throws ComponentLookupException
    {
        List<Biospecimen> internalList = new LinkedList<>();
        internalList.add(new Biospecimen().setType("Skin"));
        PatientData<Biospecimen> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        doReturn(patientData).when(this.patient).getData(CONTROLLER_NAME);
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);

        Assert.assertTrue(json.has(CONTROLLER_NAME));
        JSONArray biospecimensJSONArray = (JSONArray)json.get(CONTROLLER_NAME);
        JSONObject biospecimenJSONObject = biospecimensJSONArray.getJSONObject(0);
        Assert.assertTrue(biospecimenJSONObject.has(Biospecimen.TYPE_PROPERTY_NAME));
        Assert.assertFalse(biospecimenJSONObject.has(Biospecimen.DATE_COLLECTED_PROPERTY_NAME));
        Assert.assertFalse(biospecimenJSONObject.has(Biospecimen.DATE_RECEIVED_PROPERTY_NAME));
    }

    @Test
    public void writeJSONOnlySelectedFields() throws ComponentLookupException {
        DateTime date1 = new DateTime();
        DateTime date2 = new DateTime(date1.getMillis() + 1000);

        List<Biospecimen> internalList = new LinkedList<>();
        internalList.add(new Biospecimen().setType("Skin").setDateReceived(new DateTime()).setDateCollected(date1));
        internalList.add(new Biospecimen().setType("Nails").setDateReceived(new DateTime()));
        internalList.add(new Biospecimen().setType("Blood").setDateReceived(new DateTime()).setDateCollected(date2));
        PatientData<Biospecimen> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        doReturn(patientData).when(this.patient).getData(CONTROLLER_NAME);
        JSONObject json = new JSONObject();

        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(Biospecimen.DATE_COLLECTED_PROPERTY_NAME);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertTrue(json.has(CONTROLLER_NAME));
        JSONArray biospecimensJSONArray = (JSONArray)json.get(CONTROLLER_NAME);
        Assert.assertEquals(2, biospecimensJSONArray.length());

        for (int i = 0, len = biospecimensJSONArray.length(); i < len; i++) {
            JSONObject biospecimenJSONObject = biospecimensJSONArray.getJSONObject(i);
            Assert.assertFalse(biospecimenJSONObject.has(Biospecimen.TYPE_PROPERTY_NAME));
            Assert.assertTrue(biospecimenJSONObject.has(Biospecimen.DATE_COLLECTED_PROPERTY_NAME));
            Assert.assertFalse(biospecimenJSONObject.has(Biospecimen.DATE_RECEIVED_PROPERTY_NAME));
        }
    }

    // --------------------readJSON() tests
    @Test
    public void readWithNullJsonDoesNothing() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().readJSON(null));
    }

    @Test
    public void readWithEmptyJsonDoesNothing() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().readJSON(new JSONObject()));
    }

    @Test
    public void readWithEmptyBiospecimenArray() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();
        JSONArray biospecimensData = new JSONArray();
        json.put(CONTROLLER_NAME, biospecimensData);
        Assert.assertNotNull(this.mocker.getComponentUnderTest().readJSON(json));
    }

    @Test
    public void readWithWrongDataDoesNothing() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();
        JSONArray biospecimensData = new JSONArray();
        json.put(CONTROLLER_NAME, biospecimensData);
        JSONObject bio1 = new JSONObject();
        bio1.put(Biospecimen.DATE_COLLECTED_PROPERTY_NAME, "test-qw-12");
        biospecimensData.put(bio1);
        Assert.assertNull(this.mocker.getComponentUnderTest().readJSON(json));
    }

    @Test
    public void readSuccessful() throws ComponentLookupException, ParseException
    {
        String type = "Skin";
        String dateCollectedStr = "2016-03-26";
        String dateReceivedStr = "2016-04-02";
        DateTimeFormatter isoDateFormat = ISODateTimeFormat.date();
        DateTime dateCollected = isoDateFormat.parseDateTime(dateCollectedStr);
        DateTime dateReceived = isoDateFormat.parseDateTime(dateReceivedStr);

        JSONObject json = new JSONObject();
        JSONArray biospecimensData = new JSONArray();
        json.put(CONTROLLER_NAME, biospecimensData);
        JSONObject bio1 = new JSONObject();
        bio1.put(Biospecimen.TYPE_PROPERTY_NAME, type);
        bio1.put(Biospecimen.DATE_COLLECTED_PROPERTY_NAME, dateCollectedStr);
        bio1.put(Biospecimen.DATE_RECEIVED_PROPERTY_NAME, dateReceivedStr);
        biospecimensData.put(bio1);

        PatientData<Biospecimen> patientData = this.mocker.getComponentUnderTest().readJSON(json);

        Assert.assertNotNull(patientData);

        Biospecimen biospecimenData = patientData.iterator().next();

        Assert.assertNotNull(biospecimenData);

        assertEquals(type, biospecimenData.getType());
        assertEquals(dateCollected, biospecimenData.getDateCollected());
        assertEquals(dateReceived, biospecimenData.getDateReceived());
    }

    // --------------------save tests
    @Test
    public void saveWithNoDataDoesNothing() throws ComponentLookupException
    {
        this.mocker.getComponentUnderTest().save(this.patient);
        Mockito.verifyZeroInteractions(this.xWikiDoc);
    }

    @Test
    public void saveWithEmptyDataClearsGenes() throws ComponentLookupException
    {
        when(this.patient.getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(CONTROLLER_NAME, Collections.emptyList()));
        Provider<XWikiContext> xContextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        XWikiContext context = xContextProvider.get();
        when(context.getWiki()).thenReturn(mock(XWiki.class));
        this.mocker.getComponentUnderTest().save(this.patient);
        verify(this.xWikiDoc).removeXObjects(BiospecimensController.getClassReference());

        Mockito.verifyNoMoreInteractions(this.xWikiDoc);
    }

    @Test
    public void saveUpdate() throws ComponentLookupException, XWikiException
    {
        DateTime date1 = new DateTime();
        DateTime date2 = new DateTime();
        List<Biospecimen> biospecimenData = new LinkedList<>();
        biospecimenData.add(new Biospecimen().setType("Skin").setDateCollected(date1));
        biospecimenData.add(new Biospecimen().setType("Nails").setDateReceived(date2));

        when(this.patient.<Biospecimen>getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(CONTROLLER_NAME, biospecimenData));
        Provider<XWikiContext> xContextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        XWikiContext context = xContextProvider.get();
        when(context.getWiki()).thenReturn(mock(XWiki.class));

        BaseObject o1 = mock(BaseObject.class);
        BaseObject o2 = mock(BaseObject.class);
        when(this.xWikiDoc.newXObject(BiospecimensController.getClassReference(), context)).thenReturn(o1, o2);

        this.mocker.getComponentUnderTest().save(this.patient);

        verify(this.xWikiDoc).removeXObjects(BiospecimensController.getClassReference());
        verify(o1).set(Biospecimen.TYPE_PROPERTY_NAME, "Skin", context);
        verify(o1).set(Biospecimen.DATE_COLLECTED_PROPERTY_NAME, date1, context);
        verify(o2).set(Biospecimen.TYPE_PROPERTY_NAME, "Nails", context);
        verify(o2).set(Biospecimen.DATE_RECEIVED_PROPERTY_NAME, date2, context);
        verify(o2, Mockito.never()).set(eq(Biospecimen.DATE_COLLECTED_PROPERTY_NAME), Matchers.anyObject(), eq(context));

    }

    // -------------------- Helper functions
    private static BaseObject getBiospecimenObject(Biospecimen biospecimenData){
        BaseObject biospecimenObject = mock(BaseObject.class);

        List<Object> fieldList = new LinkedList<>();

        StringProperty typeProperty = mock(StringProperty.class);
        when(typeProperty.getValue()).thenReturn(biospecimenData.getType());
        doReturn(typeProperty).when(biospecimenObject).getField(Biospecimen.TYPE_PROPERTY_NAME);

        DateProperty dateCollectedProperty = mock(DateProperty.class);
        when(dateCollectedProperty.getValue()).thenReturn(biospecimenData.getDateCollected());
        doReturn(dateCollectedProperty).when(biospecimenObject).getField(Biospecimen.DATE_COLLECTED_PROPERTY_NAME);

        DateProperty dateReceivedProperty =  mock(DateProperty.class);
        when(dateReceivedProperty.getValue()).thenReturn(biospecimenData.getDateReceived());
        doReturn(dateReceivedProperty).when(biospecimenObject).getField(Biospecimen.DATE_RECEIVED_PROPERTY_NAME);

        fieldList.add(biospecimenData.getType());
        fieldList.add(biospecimenData.getDateCollected());
        fieldList.add(biospecimenData.getDateReceived());

        boolean allNull = true;
        for (Object property : fieldList) {
            if (property != null) {
                allNull = false;
                break;
            }
        }

        if (allNull) {
            doReturn(new LinkedList<>()).when(biospecimenObject).getFieldList();
        } else {
            doReturn(fieldList).when(biospecimenObject).getFieldList();
        }


        return biospecimenObject;
    }

    private static List<Biospecimen> getBiospecimenDataFromPatientData(PatientData<Biospecimen> patientData) {
        List<Biospecimen> data = new LinkedList<>();

        for (Biospecimen aPatientData : patientData) {
            data.add(aPatientData);
        }

       return data;
    }

    private static class BiospecimenDataEquator implements Equator<Biospecimen>
    {
        @Override public boolean equate(Biospecimen o1, Biospecimen o2)
        {
            if (o1 == null && o2 == null) {
                return true;
            }
            else if (o1 == null || o2 == null) {
                return false;
            }
            else {
                List<Integer> results = new LinkedList<>();
                results.add(ObjectUtils.compare(o1.getType(), o2.getType()));
                results.add(ObjectUtils.compare(o1.getDateCollected(), o2.getDateCollected()));
                results.add(ObjectUtils.compare(o1.getDateReceived(), o2.getDateReceived()));

                for (Integer result : results) {
                    if (!result.equals(0)) {
                        return false;
                    }
                }
                return true;
            }
        }

        @Override public int hash(Biospecimen o)
        {
            int hash = 1;
            hash = hash * 17 + (o.getType() == null ? 0 : o.getType().hashCode());
            hash = hash * 31 + (o.getDateCollected() == null ? 0 : o.getDateCollected().hashCode());
            hash = hash * 13 + (o.getDateReceived() == null ? 0 : o.getDateReceived().hashCode());
            return hash;
        }
    }
}
