package org.phenotips.data.internal.controller;

import org.phenotips.configuration.RecordConfiguration;
import org.phenotips.configuration.RecordConfigurationManager;
import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Equator;
import org.apache.commons.lang3.ObjectUtils;
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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test class for BiospecimensController.
 * Created by Sebastian on 2016-07-18.
 */
public class BiospecimensControllerTest
{

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            System.out.println("Running test: " + description.getMethodName());
        }
    };

    //@Mock
    private RecordConfigurationManager configurationManager;

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument xWikiDoc;

    private static final String CONTROLLER_NAME = new BiospecimensController().getName();

    private List<BaseObject> biospecimensXWikiObjects = new LinkedList<>();

    @Rule
    public MockitoComponentMockingRule<PatientDataController<BiospecimenData>> mocker =
        new MockitoComponentMockingRule<PatientDataController<BiospecimenData>>(BiospecimensController.class);

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

        this.configurationManager = this.mocker.getInstance(RecordConfigurationManager.class);

        DocumentReference patientDocument = new DocumentReference("wiki", "patient", "00000001");
        doReturn(patientDocument).when(this.patient).getDocument();
        doReturn(this.xWikiDoc).when(documentAccessBridge).getDocument(patientDocument);
        this.biospecimensXWikiObjects.clear();
        doReturn(this.biospecimensXWikiObjects).when(this.xWikiDoc).getXObjects(any(EntityReference.class));
    }

    /**
     * Test tear down.
     */
    @After
    public void tearDown() {
    }

    @Test
    public void loadTestNoData() throws Exception {
        PatientData<BiospecimenData> patientData = this.mocker.getComponentUnderTest().load(this.patient);
        assertNull(patientData);
    }

    @Test
    public void loadTestWithData() throws Exception {

        List<BiospecimenData> data = new LinkedList<>();
        data.add(new BiospecimenData().setType("Skin"));
        data.add(new BiospecimenData().setType("Nails").setDateCollected(new Date()));
        data.add(new BiospecimenData().setType("Saliva").setDateCollected(new Date()).setDateReceived(new Date()));

        for (BiospecimenData datum : data) {
            this.biospecimensXWikiObjects.add(getBiospecimenObject(datum));
        }

        PatientData<BiospecimenData> patientData = this.mocker.getComponentUnderTest().load(this.patient);

        assertNotNull(patientData);

        List<BiospecimenData> patientDataResult = getBiospecimenDataFromPatientData(patientData);

        System.out.println("data=" + Arrays.toString(data.toArray()));
        System.out.println("pata=" + Arrays.toString(patientDataResult.toArray()));

        assertTrue(CollectionUtils.isEqualCollection(data, patientDataResult, new BiospecimenDataEquator()));
    }

    @Test
    public void loadTestWithDataIgnoreEmpty() throws Exception {

        List<BiospecimenData> data = new LinkedList<>();
        data.add(new BiospecimenData());
        data.add(new BiospecimenData().setType("Saliva").setDateCollected(new Date()).setDateReceived(new Date()));

        for (BiospecimenData datum : data) {
            this.biospecimensXWikiObjects.add(getBiospecimenObject(datum));
        }

        PatientData<BiospecimenData> patientData = this.mocker.getComponentUnderTest().load(this.patient);

        assertNotNull(patientData);

        List<BiospecimenData> patientDataResult = getBiospecimenDataFromPatientData(patientData);

        assertFalse(CollectionUtils.isEqualCollection(data, patientDataResult, new BiospecimenDataEquator()));
        assertEquals(patientDataResult.size(), 1);
    }

    @Test
    public void loadReturnsNullWhenPatientDoesNotHaveGeneClass() throws ComponentLookupException
    {
        doReturn(null).when(this.xWikiDoc).getXObjects(any(EntityReference.class));

        PatientData<BiospecimenData> result = this.mocker.getComponentUnderTest().load(this.patient);

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
        List<BiospecimenData> internalList = new LinkedList<>();
        PatientData<BiospecimenData> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        doReturn(patientData).when(this.patient).getData(CONTROLLER_NAME);
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, null);

        Assert.assertFalse(json.has(CONTROLLER_NAME));
        verify(this.patient).getData(CONTROLLER_NAME);
    }

    @Test
    public void writeJSONReturnsWhenSelectedFieldsDoesAreInvalid() throws ComponentLookupException
    {
        List<BiospecimenData> internalList = new LinkedList<>();
        PatientData<BiospecimenData> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
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
        List<BiospecimenData> internalList = new LinkedList<>();
        internalList.add(new BiospecimenData().setType("Skin"));
        PatientData<BiospecimenData> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        doReturn(patientData).when(this.patient).getData(CONTROLLER_NAME);
        JSONObject json = new JSONObject();

        // Set up configuration controller
        RecordConfiguration recordConfigManagerMock = mock(RecordConfiguration.class);
        doReturn(recordConfigManagerMock).when(this.configurationManager).getActiveConfiguration();
        doReturn("yyyy-MM-dd'T'HH:mm'Z'").when(recordConfigManagerMock).getISODateFormat();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);

        Assert.assertTrue(json.has(CONTROLLER_NAME));
        JSONArray biospecimensJSONArray = (JSONArray)json.get(CONTROLLER_NAME);
        JSONObject biospecimenJSONObject = biospecimensJSONArray.getJSONObject(0);
        Assert.assertTrue(biospecimenJSONObject.has(BiospecimenData.TYPE_PROPERTY_NAME));
        Assert.assertFalse(biospecimenJSONObject.has(BiospecimenData.DATE_COLLECTED_PROPERTY_NAME));
        Assert.assertFalse(biospecimenJSONObject.has(BiospecimenData.DATE_RECEIVED_PROPERTY_NAME));
    }

    @Test
    public void writeJSONOnlySelectedFields() throws ComponentLookupException {
        Date date1 = new Date();
        Date date2 = new Date(date1.getTime() + 1000);

        List<BiospecimenData> internalList = new LinkedList<>();
        internalList.add(new BiospecimenData().setType("Skin").setDateReceived(new Date()).setDateCollected(date1));
        internalList.add(new BiospecimenData().setType("Nails").setDateReceived(new Date()));
        internalList.add(new BiospecimenData().setType("Blood").setDateReceived(new Date()).setDateCollected(date2));
        PatientData<BiospecimenData> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        doReturn(patientData).when(this.patient).getData(CONTROLLER_NAME);
        JSONObject json = new JSONObject();

        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(BiospecimenData.DATE_COLLECTED_PROPERTY_NAME);

        // Set up configuration controller
        RecordConfiguration recordConfigManagerMock = mock(RecordConfiguration.class);
        doReturn(recordConfigManagerMock).when(this.configurationManager).getActiveConfiguration();
        doReturn("yyyy-MM-dd'T'HH:mm'Z'").when(recordConfigManagerMock).getISODateFormat();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertTrue(json.has(CONTROLLER_NAME));
        JSONArray biospecimensJSONArray = (JSONArray)json.get(CONTROLLER_NAME);
        Assert.assertEquals(2, biospecimensJSONArray.length());

        for (int i = 0, len = biospecimensJSONArray.length(); i < len; i++) {
            JSONObject biospecimenJSONObject = biospecimensJSONArray.getJSONObject(i);
            Assert.assertFalse(biospecimenJSONObject.has(BiospecimenData.TYPE_PROPERTY_NAME));
            Assert.assertTrue(biospecimenJSONObject.has(BiospecimenData.DATE_COLLECTED_PROPERTY_NAME));
            Assert.assertFalse(biospecimenJSONObject.has(BiospecimenData.DATE_RECEIVED_PROPERTY_NAME));
        }
    }


    // -------------------- Helper functions
    private static BaseObject getBiospecimenObject(BiospecimenData biospecimenData){
        BaseObject biospecimenObject = mock(BaseObject.class);

        List<Object> fieldList = new LinkedList<>();

        StringProperty typeProperty = mock(StringProperty.class);
        Mockito.when(typeProperty.getValue()).thenReturn(biospecimenData.getType());
        doReturn(typeProperty).when(biospecimenObject).getField(BiospecimenData.TYPE_PROPERTY_NAME);

        DateProperty dateCollectedProperty = mock(DateProperty.class);
        Mockito.when(dateCollectedProperty.getValue()).thenReturn(biospecimenData.getDateCollected());
        doReturn(dateCollectedProperty).when(biospecimenObject).getField(BiospecimenData.DATE_COLLECTED_PROPERTY_NAME);

        DateProperty dateReceivedProperty =  mock(DateProperty.class);
        Mockito.when(dateReceivedProperty.getValue()).thenReturn(biospecimenData.getDateReceived());
        doReturn(dateReceivedProperty).when(biospecimenObject).getField(BiospecimenData.DATE_RECEIVED_PROPERTY_NAME);

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

    private static List<BiospecimenData> getBiospecimenDataFromPatientData(PatientData<BiospecimenData> patientData) {
        List<BiospecimenData> data = new LinkedList<>();

        for (BiospecimenData aPatientData : patientData) {
            data.add(aPatientData);
        }

       return data;
    }

    private static class BiospecimenDataEquator implements Equator<BiospecimenData>
    {
        @Override public boolean equate(BiospecimenData o1, BiospecimenData o2)
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

        @Override public int hash(BiospecimenData o)
        {
            int hash = 1;
            hash = hash * 17 + (o.getType() == null ? 0 : o.getType().hashCode());
            hash = hash * 31 + (o.getDateCollected() == null ? 0 : o.getDateCollected().hashCode());
            hash = hash * 13 + (o.getDateReceived() == null ? 0 : o.getDateReceived().hashCode());
            return hash;
        }
    }
}
