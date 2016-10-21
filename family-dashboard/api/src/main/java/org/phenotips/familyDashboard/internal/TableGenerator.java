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

import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

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

/**
 * Class for generating an HTML table via Java.
 *
 * @version $Id$
 * @since 1.3
 */
public class TableGenerator
{
    private Document document;
    private final ArrayList<String> selectedFields;
    private final JSONObject translatedLabels;
    private final JSONObject tableHeaders;
    private final List<Patient> members;
    private final String dateFormat = "yyyy-MM-dd";

    private final Family family;

    private final JSONObject config;

    /**
     * Constructor for this class.
     *
     * @param family - the data object the table gets populated with.
     * @param config - the configuration object obtained from a XWiki document.
     * @throws Exception if the family table configuration is out of sync with current patient data representations or
     * if there is an error in building the dom Document.
     */
    public TableGenerator(Family family, JSONObject config) throws Exception
    {
        this.family = family;
        this.config = config;

        members = this.family.getMembers();

        try {
            selectedFields = new ArrayList<>();
            translatedLabels = this.config.getJSONObject("translatedLabels");
            tableHeaders = this.config.getJSONObject("labels");

            JSONArray order = this.config.getJSONArray("order");
            for (int i = 0; i < order.length(); i++) {
                selectedFields.add(order.getString(i));
            }
        } catch (JSONException e) {
            throw new Exception("Error retrieving table headers for the table of family members", e);
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.newDocument();
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
        if (selectedFields.isEmpty()) {
            return null;
        }

        Element table = document.createElement("table");
        table.setAttribute("id", "family-table");
        document.appendChild(table);

        table.appendChild(getTableHeaderRow());

        for (Patient member : members) {
            table.appendChild(getRow(member.toJSON(), true));
        }

        for (JSONObject member : getUnlinkedMembersFromPedigree()) {
            table.appendChild(getRow(member, false));
        }

        return getDocumentHtml();
    }

    private Element getTableHeaderRow()
    {
        Element tableHeaderEl = document.createElement("thead");

        for (String selectedField : selectedFields) {
            Element colEl = document.createElement("th");
            colEl.appendChild(document.createTextNode(tableHeaders.getString(selectedField)));
            tableHeaderEl.appendChild(colEl);
        }
        return tableHeaderEl;
    }

    private List<JSONObject> getUnlinkedMembersFromPedigree()
    {
        Pedigree pedigree = this.family.getPedigree();
        JSONArray data = pedigree.getData().optJSONArray("GG");
        List<JSONObject> nonPatientMembers = new LinkedList<>();
        for (Object nodeObj : data) {
            JSONObject node = (JSONObject) nodeObj;
            JSONObject memberProperties = node.optJSONObject("prop");
            if (memberProperties != null && memberProperties.length() != 0 && !memberProperties.has("phenotipsId")) {
                nonPatientMembers.add(node);
            }
        }
        return nonPatientMembers;
    }

    private Element getRow(JSONObject member, boolean isPatient) throws Exception
    {
        Element tableRowEl = document.createElement("tbody");
        tableRowEl.setAttribute("class", "familyMemberRow");

        for (String selectedField : selectedFields) {
            try {
                if (isPatient) {
                    tableRowEl.appendChild(getRowColCellForPatient(selectedField, member));
                } else {
                    tableRowEl.appendChild(getRowColCellForUnlinkedMember(selectedField, member));
                }
            } catch (JSONException e) {
                throw new Exception("Error retrieving a selected field from a family member JSON", e);
            }
        }

        return tableRowEl;
    }

    private Element getRowColCellForPatient(String field, JSONObject member)
    {
        Element colEl = document.createElement("td");

        if (isId(field)) {
            String id = member.optString(field);

            Element patientIdEl = document.createElement("span");
            patientIdEl.setAttribute("class", "wikilink");

            Element linkEl = document.createElement("a");
            linkEl.setAttribute("class", "identifier");
            linkEl.setAttribute("target", "_blank");
            linkEl.setAttribute("href", "/" + id);

            linkEl.appendChild(document.createTextNode(id));
            patientIdEl.appendChild(linkEl);
            colEl.appendChild(patientIdEl);
        } else if (isName(field)) {
            JSONObject nameObj = member.optJSONObject("patient_name");
            if (nameObj != null) {
                colEl.appendChild(document.createTextNode(nameObj.optString(field)));
            }
        } else if (isDate(field)) {
            DateFormat dateFormatter = new SimpleDateFormat(dateFormat);
            try {
                Date date = dateFormatter.parse(member.getString(field));
                colEl.appendChild(document.createTextNode(dateFormatter.format(date)));
            } catch (ParseException e) {
            }
        } else if (isDisorder(field) || isFeature(field)) {
            appendVocabularyContents(colEl, member.optJSONArray(field));
        } else {
            colEl.appendChild(document.createTextNode(member.optString(field)));
        }
        return colEl;
    }

    private Element getRowColCellForUnlinkedMember(String field, JSONObject node)
    {
        String translatedKey = translatedLabels.optString(field);
        String nodeId = node.optString("id");
        JSONObject member = node.optJSONObject("prop");
        Element colEl = document.createElement("td");

        if (nodeId == null || member == null || translatedKey == null) {
            return colEl;
        }

        if (isId(field)) {
            Element hiddenNodeIdEl = document.createElement("span");
            hiddenNodeIdEl.setAttribute("class", "identifier");
            hiddenNodeIdEl.setAttribute("style", "display: none");
            hiddenNodeIdEl.appendChild(document.createTextNode(nodeId));
            colEl.appendChild(document.createTextNode("N/A"));
            colEl.appendChild(hiddenNodeIdEl);
        } else if (isName(field)) {
            colEl.appendChild(document.createTextNode(member.optString(translatedKey)));
        } else if (isDisorder(field)) {
            JSONArray disorders = member.optJSONArray(translatedKey);
            for (Object disorder : disorders) {
                if (disorder instanceof String) {
                    Element listNode = document.createElement("ul");
                    listNode.appendChild(document.createTextNode((String) disorder));
                    colEl.appendChild(listNode);
                }
            }
        } else if (isFeature(field)) {
            appendVocabularyContents(colEl, member.optJSONArray(translatedKey));
        } else {
            colEl.appendChild(document.createTextNode("N/A"));
        }
        return colEl;
    }

    private void appendVocabularyContents(Element colEl, JSONArray vocabArray)
    {
        if (colEl != null && vocabArray != null) {
            for (Object obj : vocabArray) {
                JSONObject vocabObj = (JSONObject) obj;
                Element listNode = document.createElement("ul");
                listNode.appendChild(document.createTextNode(vocabObj.optString("label")));
                colEl.appendChild(listNode);
            }
        }
    }

    private String getDocumentHtml() throws Exception
    {
        try {
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            DOMSource domSource = new DOMSource(document);
            transformer.transform(domSource, result);

            return writer.toString();
        } catch (TransformerConfigurationException e) {
            throw new Exception("Error writing HTML content for the table of family members", e);
        }
    }

    private boolean isName(String key) { return "first_name".equals(key) || "last_name".equals(key); }

    private boolean isDisorder(String key) { return "disorders".equals(key); }

    private boolean isFeature(String key) { return "features".equals(key); }

    private boolean isDate(String key) { return "date".equals(key) || "last_modification_date".equals(key); }

    private boolean isId(String key) { return "id".equals(key); }
}
