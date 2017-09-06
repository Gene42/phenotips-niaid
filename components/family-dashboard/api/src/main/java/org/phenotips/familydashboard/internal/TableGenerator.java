/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.familydashboard.internal;

import org.phenotips.data.Patient;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.Pedigree;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.xpn.xwiki.XWikiContext;

/**
 * Class for generating an HTML table.
 *
 * @version $Id$
 * @since 1.3
 */
@SuppressWarnings({ "ClassFanOutComplexity", "CyclomaticComplexity" })
public class TableGenerator
{
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String NOT_AVAILABLE_TAG = "N/A";
    private static final String CSS_CLASS = "class";
    private static final String SPAN = "span";

    private static final String DISORDERS = "disorders";
    private static final String IDENTIFIER = "identifier";
    private static final String PROP = "prop";
    private static final String ID = "id";

    private static final String LABEL = "label";
    private static final String FEATURES = "features";

    protected Vocabulary omimService;
    protected Vocabulary hpoService;
    protected XWikiContext xWikiContext;
    protected AuthorizationManager authorizationManager;

    private Document document;
    private final ArrayList<String> selectedFields;
    private final JSONObject translatedLabels;
    private final JSONObject tableHeaders;
    private final List<Patient> members;


    private final Family family;

    //private final JSONObject config;

    /**
     * Constructor for this class.
     *
     * @param family - the data object the table gets populated with.
     * @param config - the configuration object obtained from a XWiki document.
     * @param omimService - the omim vocabulary ontology service.
     * @param hpoService - the hpo vocabulary ontology service.
     * @param xWikiContext - XWiki context object.
     * @param authorizationManager - the authorization manager for checking access level.
     * @throws Exception if the family table configuration is out of sync with current patient data representations or
     *                      if there is an error in building the dom Document.
     */
    public TableGenerator(Family family, JSONObject config, Vocabulary omimService, Vocabulary hpoService,
        XWikiContext xWikiContext, AuthorizationManager authorizationManager)
        throws Exception
    {
        this.family = family;
        this.omimService = omimService;
        this.hpoService = hpoService;
        this.xWikiContext = xWikiContext;
        this.authorizationManager = authorizationManager;

        this.members = this.family.getMembers();

        try {
            this.selectedFields = new ArrayList<>();
            this.translatedLabels = config.getJSONObject("translatedLabels");
            this.tableHeaders = config.getJSONObject("labels");

            JSONArray order = config.getJSONArray("order");
            for (int i = 0; i < order.length(); i++) {
                this.selectedFields.add(order.getString(i));
            }
        } catch (JSONException e) {
            throw new Exception("Error retrieving table headers for the table of family members", e);
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            this.document = builder.newDocument();
        } catch (ParserConfigurationException e) {
            throw new Exception("Error generating table of family members", e);
        }
    }

    /**
     * Gets the HTML content for the table.
     *
     * @return An HTML table.
     * @throws Exception if there is an error in generating the table HTML content.
     */
    public String getHtml() throws Exception
    {
        if (this.selectedFields.isEmpty()) {
            return null;
        }

        Element table = this.document.createElement("table");
        table.setAttribute(ID, "family-members-table");
        this.document.appendChild(table);

        table.appendChild(getTableHeaderRow());

        for (Patient member : this.members) {
            boolean viewable = this.authorizationManager.hasAccess(Right.VIEW, this.xWikiContext.getUserReference(),
                member.getDocumentReference());
            table.appendChild(getRow(member.toJSON(), true, viewable));
        }
        for (JSONObject member : getUnlinkedMembersFromPedigree()) {
            table.appendChild(getRow(member, false, true));
        }

        return getDocumentHtml();
    }

    private List<JSONObject> getUnlinkedMembersFromPedigree()
    {
        Pedigree pedigree = this.family.getPedigree();

        if (pedigree == null) {
            return new ArrayList<>();
        }

        JSONArray data = pedigree.getData().optJSONArray("GG");
        List<JSONObject> nonPatientMembers = new ArrayList<>();
        for (Object nodeObj : data) {
            JSONObject node = (JSONObject) nodeObj;
            JSONObject memberProperties = node.optJSONObject(PROP);
            if (memberProperties != null && memberProperties.length() != 0 && !memberProperties.has("phenotipsId")) {
                nonPatientMembers.add(node);
            }
        }
        return nonPatientMembers;
    }

    private Element getTableHeaderRow()
    {
        Element tableHeaderEl = this.document.createElement("thead");

        for (String selectedField : this.selectedFields) {
            Element cellEl = this.document.createElement("th");
            cellEl.appendChild(this.document.createTextNode(this.tableHeaders.getString(selectedField)));
            tableHeaderEl.appendChild(cellEl);
        }
        return tableHeaderEl;
    }

    private Element getRow(final JSONObject data, boolean isPatient, boolean viewable)
    {
        Element tableRowEl = this.document.createElement("tr");
        tableRowEl.setAttribute(CSS_CLASS, "family-member-row");

        if (!isPatient && (data.optJSONObject(PROP) == null || data.optJSONObject(PROP).length() == 0)) {
            return tableRowEl;
        }

        JSONObject dataInternal = data;

        if (!isPatient) {
            dataInternal = this.resolvePedigreeMemberOmimTerms(data);
        }
        for (String selectedField : this.selectedFields) {
            tableRowEl.appendChild(getRowColCell(selectedField, dataInternal, isPatient, viewable));
        }

        return tableRowEl;
    }

    private Element getRowColCell(String field, JSONObject data, boolean isPatient, boolean viewable)
    {
        Element cellEl = this.document.createElement("td");

        // TODO: Change when upgraded to 1.3m4 which will have a 'pedigreeId' in the JSON for a pedigree member
        JSONObject member = data;
        String nodeId = field;
        if (!isPatient) {
            nodeId = data.optString(ID);
            member = data.optJSONObject(PROP);
        }

        if (isId(field)) {
            setIdCell(cellEl, nodeId, member, isPatient);

        } else if (viewable) {
            if (isName(field)) {
                setNameCell(cellEl, field, member, isPatient);

            } else if (isReporter(field)) {
                setReporterCell(cellEl, field, member, isPatient);

            } else if (isDate(field)) {
                setDateCell(cellEl, field, member, isPatient);

            } else if (isVocabulary(field)) {
                setVocabularyCell(cellEl, field, member, isPatient);

            } else {
                setSimpleCell(cellEl, field, member, isPatient);
            }
        }
        return cellEl;
    }

    private void setSimpleCell(Element cellEl, String field, JSONObject member, boolean isPatient)
    {
        cellEl.setAttribute(CSS_CLASS, field);
        String value = isPatient ? member.optString(field) : NOT_AVAILABLE_TAG;
        cellEl.appendChild(this.document.createTextNode(value));
    }

    private void setIdCell(Element cellEl, String field, JSONObject member, boolean isPatient)
    {
        cellEl.setAttribute(CSS_CLASS, field);
        if (isPatient) {
            String id = member.optString(field);
            cellEl.appendChild(getLinkElement(getXWikiURLForLinkField(id), IDENTIFIER, id, false));
        } else {
            Element idEl = this.document.createElement(SPAN);
            idEl.setAttribute(CSS_CLASS, IDENTIFIER);
            idEl.setAttribute("style", "display: none");
            idEl.appendChild(this.document.createTextNode(field));
            cellEl.appendChild(this.document.createTextNode(NOT_AVAILABLE_TAG));
            cellEl.appendChild(idEl);
        }
    }

    private void setNameCell(Element cellEl, String field, JSONObject member, boolean isPatient)
    {
        cellEl.setAttribute(CSS_CLASS, field);
        if (isPatient) {
            JSONObject nameObj = member.optJSONObject("patient_name");
            if (nameObj != null) {
                cellEl.appendChild(this.document.createTextNode(nameObj.optString(field)));
            }
        } else {
            cellEl.appendChild(this.document.createTextNode(member.optString(this.translatedLabels.optString(field))));
        }
    }

    private void setDateCell(Element cellEl, String field, JSONObject member, boolean isPatient)
    {
        cellEl.setAttribute(CSS_CLASS, field);
        if (isPatient) {
            SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT);
            try {
                Date date = dateFormatter.parse(member.getString(field));
                cellEl.appendChild(this.document.createTextNode(dateFormatter.format(date)));
            } catch (ParseException e) {
                // Nothing
            }
        } else {
            setSimpleCell(cellEl, "", member, false);
        }
    }

    private void setReporterCell(Element cellEl, String field, JSONObject member, boolean isPatient)
    {
        cellEl.setAttribute(CSS_CLASS, field);
        if (isPatient) {
            String username = member.optString(field);
            Element reporterEl = getLinkElement(getXWikiURLForLinkField(username), "", username, false);
            Element icon = this.document.createElement("i");
            icon.setAttribute(CSS_CLASS, "fa fa-user");
            cellEl.appendChild(icon);
            cellEl.appendChild(reporterEl);
        } else {
            setSimpleCell(cellEl, "", member, isPatient);
        }
    }

    private void setVocabularyCell(Element cellEl, String field, JSONObject member, boolean isPatient)
    {
        cellEl.setAttribute(CSS_CLASS, field);
        if (isPatient) {
            JSONArray vocabArray = member.optJSONArray(field);
            if (DISORDERS.equals(field)) {
                vocabArray = getCombinedOmimAndOrdoTerms(vocabArray, member.optJSONArray("clinical-diagnosis"));
            }
            appendVocabularyTerms(cellEl, vocabArray, false);
        } else {
            appendVocabularyTerms(cellEl, member.optJSONArray(this.translatedLabels.optString(field)), false);
        }
    }

    /**
     * Gets a consolidated list of OMIM and ORDO disorder terms.
     *
     * @param omimDisorders the set of OMIM disorders as JSONObjects
     * @param ordoDisorders the set of ORDO disorders as JSONObjects
     * @return a JSONArray containing disorders
     */
    private JSONArray getCombinedOmimAndOrdoTerms(JSONArray omimDisorders, JSONArray ordoDisorders)
    {
        JSONArray allDisorderTerms = omimDisorders;
        if (ordoDisorders != null) {
            for (int i = 0; i < ordoDisorders.length(); i++) {
                if (ordoDisorders.get(i) instanceof JSONObject) {
                    allDisorderTerms.put(ordoDisorders.getJSONObject(i));
                }
            }
        }
        return allDisorderTerms;
    }

    @SuppressWarnings("CyclomaticComplexity")
    private void appendVocabularyTerms(Element cellEl, JSONArray vocabArray, boolean includeHyperlink)
    {
        if (cellEl == null || vocabArray == null) {
            return;
        }

        // TODO: Ensure pedigree members have OMIM label displayed
        for (Object obj : vocabArray) {
            String val = null;
            String termId = null;
            if (obj instanceof String) {
                val = (String) obj;
            } else if (obj instanceof JSONObject) {
                JSONObject vocabObj = (JSONObject) obj;
                val = vocabObj.optString(LABEL, null) == null
                    ? vocabObj.optString("name") : vocabObj.optString(LABEL);
                termId = vocabObj.optString(ID);
            }

            Element listNode = this.document.createElement("ul");
            if (termId != null) {
                String infoType;
                if (termId.startsWith("HP:")) {
                    infoType = "phenotype-info";
                } else if (termId.startsWith("ORDO:")) {
                    infoType = "ordo-disease-info";
                } else {
                    infoType = "omim-disease-info";
                }

                if (includeHyperlink) {
                    String link = termId.startsWith("MIM:") ? "http://www.omim.org/entry/" + termId.substring(4)
                        : "http://compbio.charite.de/hpoweb/showterm?id=" + termId;
                    listNode.appendChild(getLinkElement(link, "vocabLink", "[" + termId + "]", true));
                }

                cellEl.setAttribute(CSS_CLASS, infoType);

                Element label = this.document.createElement(SPAN);
                label.setAttribute(CSS_CLASS, "vocabLabel");
                label.appendChild(this.document.createTextNode(val));
                listNode.appendChild(label);

                Element helpButton = this.document.createElement(SPAN);
                helpButton.setAttribute(CSS_CLASS, "fa fa-info-circle xHelpButton " + infoType);
                helpButton.setAttribute("title", termId);
                listNode.appendChild(helpButton);
            } else {
                listNode.appendChild(this.document.createTextNode(val));
            }
            cellEl.appendChild(listNode);
        }
    }

    /**
     * Resolves OMIM terms for a pedigree virtual patient (a family member which is not a patient). The JSONObject for
     * the virtual member stores disorders as an array of OMIM disorder ID's (without the 'MIM:' prefix), unlike the
     * patient JSONObject which stores disorders as a JSONObject with 'id' and 'label' key value pairs. This method
     * retrieves the full JSONObject for the vocabulary term and replaces the virtual member's "disorders" key with the
     * list of objects for generic use with patient family members.
     *
     * @param familyMember the family member
     * @return the JSONObject for the vocabulary term
     */
    private JSONObject resolvePedigreeMemberOmimTerms(JSONObject familyMember)
    {
        JSONObject prop = familyMember.getJSONObject(PROP);
        JSONArray omimTermIds = prop.optJSONArray(DISORDERS);
        if (omimTermIds != null) {
            JSONArray omimTermObjs = new JSONArray();
            for (int i = 0; i < omimTermIds.length(); i++) {
                if (omimTermIds.get(i) instanceof String) {
                    VocabularyTerm omimTerm = this.omimService.getTerm(omimTermIds.getString(i));
                    if (omimTerm != null) {
                        omimTermObjs.put(omimTerm.toJSON());
                    }
                }
            }
            prop.put(DISORDERS, omimTermObjs);
        }
        return familyMember;
    }

    private void mapOmimSymptomsToFamilyMemberPhenotypes(JSONObject data, boolean isPatient)
    {
        try {
            OmimToHpoMapper mapper = new OmimToHpoMapper(this.omimService, this.hpoService);
            JSONObject familyMember = data;
            if (!isPatient) {
                familyMember = data.getJSONObject(PROP);
            }
            List<String> omimTerms = getVocabularies(familyMember, DISORDERS);
            List<String> hpoTerms = getVocabularies(familyMember, FEATURES);
            Map<String, List<VocabularyTerm>> omimToHpoMap = mapper.getOmimToHpoMap(omimTerms, hpoTerms);
            familyMember.put(FEATURES, getRelevantPhenotypesJsonArray(omimToHpoMap));
            familyMember.put(DISORDERS, getOmimDisordersJsonArray(omimToHpoMap));
        } catch (JSONException e) {
            throw new JSONException("Error handling family member JSON data for omim-to-hpo mapping in the "
                + "table of family members", e);
        }
    }

    /**
     * Gets the entire set of all omim disorders in JSON format.
     *
     * @return a list of all omim disorders
     */
    private JSONArray getOmimDisordersJsonArray(Map<String, List<VocabularyTerm>> omimToHpoMap)
    {
        JSONArray disordersArray = new JSONArray();
        if (omimToHpoMap == null || omimToHpoMap.size() == 0) {
            return disordersArray;
        }
        for (String omimKey : omimToHpoMap.keySet()) {
            VocabularyTerm omimTerm = this.omimService.getTerm(omimKey);
            if (omimTerm != null) {
                disordersArray.put(omimTerm.toJSON());
            }
        }
        return disordersArray;
    }

    /**
     * Gets the subset of all the family member's phenotypes that are relevant to the of omim disorders the
     * family member is diagnosed with in JSON format.
     *
     * @return an aggregated list of all matched hpo terms
     */
    private JSONArray getRelevantPhenotypesJsonArray(Map<String, List<VocabularyTerm>> omimToHpoMap)
    {
        JSONArray featuresArray = new JSONArray();
        if (omimToHpoMap == null || omimToHpoMap.size() == 0) {
            return featuresArray;
        }
        Map<String, String> aggregatedPhenotypes = new HashMap<>();
        for (Map.Entry<String, List<VocabularyTerm>> entry : omimToHpoMap.entrySet()) {
            for (VocabularyTerm phenotype : entry.getValue()) {
                if (!aggregatedPhenotypes.containsKey(phenotype.getId())) {
                    aggregatedPhenotypes.put(phenotype.getId(), null);
                    featuresArray.put(phenotype.toJSON());
                }
            }
        }
        return featuresArray;
    }

    /**
     * Gets the vocabulary sets (i.e. OMIM, HPO, HGNC) from the JSON input.
     *
     * @param data the json containing the vocabulary set
     * @param field the key for retrieving the vocabulary set
     * @return a List of Strings
     */
    private List<String> getVocabularies(JSONObject data, String field)
    {
        List<String> terms = new ArrayList<>();
        JSONArray vocabs = data.optJSONArray(field);
        if (vocabs != null) {
            for (int i = 0; i < vocabs.length(); i++) {
                Object obj = vocabs.opt(i);
                if (obj instanceof JSONObject) {
                    JSONObject vocabJSON = (JSONObject) obj;
                    String id = vocabJSON.optString(ID);
                    if (id != null) {
                        terms.add(id);
                    }
                } else if (obj instanceof String) {
                    String id = (String) obj;
                    terms.add(id);
                }
            }
        }
        return terms;
    }

    private String getXWikiURLForLinkField(String identifier)
    {
        DocumentReference ref = null;
        for (Patient patient : this.family.getMembers()) {
            if (identifier.equals(patient.getId())) {
                ref = patient.getDocument();
                break;
            } else if (patient.getReporter() != null && identifier.equals(patient.getReporter().getName())) {
                ref = patient.getReporter();
                break;
            }
        }
        String link = null;
        if (ref != null) {
            link = this.xWikiContext.getWiki().getURL(ref, "view", this.xWikiContext);
        }
        return link;
    }

    private Element getLinkElement(String link, String innerClass, String innerHTML, boolean isExternal)
    {
        String wrapperClass = isExternal ? "wikiexternallink" : "wikilink";
        Element linkWrapper = this.document.createElement(SPAN);
        linkWrapper.setAttribute(CSS_CLASS, wrapperClass);
        Element linkEl = this.document.createElement("a");
        linkEl.setAttribute(CSS_CLASS, innerClass);
        linkEl.setAttribute("href", link);
        linkEl.appendChild(this.document.createTextNode(innerHTML));
        linkWrapper.appendChild(linkEl);
        return linkWrapper;
    }

    private String getDocumentHtml() throws Exception
    {
        try {
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            DOMSource domSource = new DOMSource(this.document);
            transformer.transform(domSource, result);

            return writer.toString();
        } catch (TransformerConfigurationException e) {
            throw new Exception("Error writing HTML content for the table of family members", e);
        }
    }

    private boolean isName(String key)
    { return "first_name".equals(key) || "last_name".equals(key); }

    private boolean isVocabulary(String key)
    { return DISORDERS.equals(key) || FEATURES.equals(key); }

    private boolean isDate(String key)
    { return "date".equals(key) || "last_modification_date".equals(key); }

    private boolean isId(String key)
    { return ID.equals(key); }

    private boolean isReporter(String key)
    { return "reporter".equals(key); }
}
