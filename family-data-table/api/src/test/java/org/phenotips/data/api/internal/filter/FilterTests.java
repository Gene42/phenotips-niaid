package org.phenotips.data.api.internal.filter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

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

        //DocumentReference patientDocument = new DocumentReference("wiki", "patient", "00000001");
        doReturn(this.baseClasses.get("PhenoTips.PatientClass")).when(this.context)
            .getBaseClass(AbstractObjectFilterFactory.getClassDocumentReference("PhenoTips.PatientClass"));

        doReturn(this.baseClasses.get("PhenoTips.VisibilityClass")).when(this.context)
            .getBaseClass(AbstractObjectFilterFactory.getClassDocumentReference("PhenoTips.VisibilityClass"));


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
        queryObj.put(AbstractFilter.TYPE_KEY, FilterType.DOCUMENT.toString());
        queryObj.put(AbstractFilter.CLASS_KEY, "PhenoTips.PatientClass");

        JSONArray filters = new JSONArray();
        queryObj.put(EntityFilter.FILTERS_KEY, filters);

        JSONObject filter1 = new JSONObject();
        filter1.put(AbstractFilter.TYPE_KEY, FilterType.OBJECT.toString());
        filter1.put(AbstractFilter.CLASS_KEY, "PhenoTips.VisibilityClass");
        filter1.put(ObjectFilter.PROPERTY_NAME_KEY, "visibility");
        filter1.put(ObjectFilter.VALUES_KEY, new JSONArray("[hidden,private,public,open]"));

        filters.put(filter1);

        /*{
            "type" : "object",
            "class": "Phenotips.VisibilityClass",
            "propertyName": "visibility",
            "values": [ "hidden", "private", "public", "open" ],
            "joinMode": "OR",
        },*/

        EntityFilter query = new EntityFilter().populate(queryObj, 0, new DefaultObjectFilterFactory(this.context));

        List<String> bindingValues = new LinkedList<>();

        StringBuilder hql = query.hql(null, bindingValues, 0, null, null);

        System.out.println("[" + hql + "]");

        System.out.println("values=" + Arrays.toString(bindingValues.toArray()));

    }
}
