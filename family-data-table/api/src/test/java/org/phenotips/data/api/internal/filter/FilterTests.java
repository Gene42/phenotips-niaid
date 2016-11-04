/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.api.internal.filter;

import org.phenotips.data.api.internal.DefaultFilterFactory;
import org.phenotips.data.api.internal.DocumentQuery;
import org.phenotips.data.api.internal.PropertyName;
import org.phenotips.data.api.internal.SearchUtils;
import org.phenotips.data.api.internal.SpaceAndClass;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

import javax.inject.Provider;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.classes.StringClass;

import static org.mockito.Mockito.doReturn;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class FilterTests
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
    //private Patient patient;

    @Mock
    private XWikiContext context;

    @Mock
    private Provider<XWikiContext> contextProvider;

    private Map<String, BaseClass> baseClasses = new HashMap<>();

    private Map<String, PropertyClass> propertyClasses = new HashMap<>();

    //@Mock
    //private User user;

    //private static final String CONTROLLER_NAME = new CaseCompleteController().getName();

    //@Rule
    //public MockitoComponentMockingRule<PatientDataController<CaseComplete>> mocker =
    //    new MockitoComponentMockingRule<PatientDataController<CaseComplete>>(CaseCompleteController.class);

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

        //DocumentAccessBridge documentAccessBridge = this.mocker.getInstance(DocumentAccessBridge.class);

        //BaseClass baseClass = context.getBaseClass(getClassDocumentReference(className));
        //PropertyInterface property = baseClass.get(propertyName);


        this.baseClasses.put("PhenoTips.PatientClass", Mockito.mock(BaseClass.class));
        this.baseClasses.put("PhenoTips.VisibilityClass", Mockito.mock(BaseClass.class));

        this.propertyClasses.put("StringClass", Mockito.mock(StringClass.class));

        doReturn(this.context).when(this.contextProvider).get();

        //DocumentReference patientDocument = new DocumentReference("wiki", "patient", "00000001");
        doReturn(this.baseClasses.get("PhenoTips.PatientClass")).when(this.context)
            .getBaseClass(SearchUtils.getClassDocumentReference("PhenoTips.PatientClass"));

        doReturn(this.baseClasses.get("PhenoTips.VisibilityClass")).when(this.context)
            .getBaseClass(SearchUtils.getClassDocumentReference("PhenoTips.VisibilityClass"));


        doReturn(this.propertyClasses.get("StringClass")).when(this.baseClasses.get("PhenoTips.PatientClass")).get("visibility");



        /*doReturn(this.xWikiDoc).when(documentAccessBridge).getDocument();

        RecordConfiguration recordConfigManagerMock = mock(RecordConfiguration.class);
        doReturn(JSON_DATE_FORMAT).when(recordConfigManagerMock).getISODateFormat();

        doReturn("Bob").when(this.user).getName();

        UserManager userManager = this.mocker.getInstance(UserManager.class);
        doReturn(this.user).when(userManager).getUser(any(String.class));
        doReturn(Boolean.TRUE).when(this.user).exists();*/
    }

    /**
     * Test tear down.
     */
    @After
    public void tearDown() {
    }


    @Test
    public void test1() throws Exception
    {

        JSONObject queryObj = new JSONObject();
        //queryObj.put(AbstractFilter.TYPE_KEY, EntityType.DOCUMENT.toString());
        queryObj.put(SpaceAndClass.CLASS_KEY, "PhenoTips.PatientClass");

        JSONArray filters = new JSONArray();
        queryObj.put(DocumentQuery.FILTERS_KEY, filters);

        JSONObject filter1 = new JSONObject();
        //filter1.put(AbstractFilter.TYPE_KEY, EntityType.OBJECT.toString());
        filter1.put(SpaceAndClass.CLASS_KEY, "PhenoTips.VisibilityClass");
        filter1.put(PropertyName.PROPERTY_NAME_KEY, "visibility");
        filter1.put(AbstractFilter.VALUES_KEY, new JSONArray("[hidden,private,public,open]"));

        filters.put(filter1);
        List<Object> bindingValues = new LinkedList<>();

        /*{
            "type" : "object",
            "class": "Phenotips.VisibilityClass",
            "propertyName": "visibility",
            "values": [ "hidden", "private", "public", "open" ],
            "joinMode": "OR",
        },*/

        DocumentQuery
            query = new DocumentQuery(new DefaultFilterFactory(this.contextProvider)).init(queryObj);



        StringBuilder hql = query.hql(null, bindingValues);

        System.out.println("[" + hql + "]");

        System.out.println("values=" + Arrays.toString(bindingValues.toArray()));

    }

    @Test
    public void test2() throws Exception
    {
        Pattern pat = Pattern.compile("[0-9]*[yYmMwWdD]?");

        printResult(pat, "30y");
        printResult(pat, "30");
        printResult(pat, "12d");
        printResult(pat, "30Y");
        printResult(pat, "y30w");
        printResult(pat, "30 M");
    }

    private static void printResult(Pattern pattern, String text) {
        System.out.println(String.format("%1$s=%2$s", text, pattern.matcher(text).matches()));
    }
}
