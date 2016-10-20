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

import java.io.StringWriter;
import java.util.ArrayList;
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
    private final JSONObject tableHeaders;
    private final List<Patient> members;

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
            tableHeaders = this.config.getJSONObject("labels");
            JSONArray order = this.config.getJSONArray("order");
            selectedFields = new ArrayList<>();
            for (int i = 0; i < order.length(); i++) {
                selectedFields.add(order.getString(i));
            }
        } catch (JSONException e) {
            throw new Exception("Error retrieving family table header labels", e);
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.newDocument();
        } catch (ParserConfigurationException e) {
            throw new Exception("Error generating family table", e);
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
            if (member != null) {
                table.appendChild(getRow(member));
            }
        }

        return getDocumentHtml();
    }

    private Element getRow(Patient member) throws Exception
    {
        JSONObject memberJson = member.toJSON();

        Element tableRowEl = document.createElement("tbody");
        tableRowEl.setAttribute("class", "familyMemberRow");
        tableRowEl.appendChild(document.createElement("td"));

        for (String selectedField : selectedFields) {
            try {
                tableRowEl.appendChild(getRowColumnCell(selectedField, memberJson));
            } catch (JSONException e) {
                throw new Exception("Error retrieving a selected field from a Patient JSON", e);
            }
        }
        return tableRowEl;
    }

    private Element getRowColumnCell(String field, JSONObject member)
    {
        Element colEl = document.createElement("td");

        if (isPatientNameField(field)) {
            colEl.appendChild(document.createTextNode(member.getJSONObject("patient_name").getString(field)));
        } else if (isVocabularyField(field)) {
            JSONArray vocabArray = member.getJSONArray(field);
            for (int j = 0; j < vocabArray.length(); j++) {
                Element listNode = document.createElement("ul");
                listNode.appendChild(document.createTextNode(vocabArray.getJSONObject(j).getString("label")));
                colEl.appendChild(listNode);
            }
        } else {
            if ("id".equals(field)) {
                colEl.setAttribute("class", "identifier");
            }
            colEl.appendChild(document.createTextNode((String) member.get(field)));
        }
        return colEl;
    }

    private Element getTableHeaderRow()
    {
        Element tableHeaderEl = document.createElement("thead");
        tableHeaderEl.appendChild(document.createElement("th"));

        for (String selectedField : selectedFields) {
            Element colEl = document.createElement("th");
            colEl.appendChild(document.createTextNode(tableHeaders.getString(selectedField)));
            tableHeaderEl.appendChild(colEl);
        }
        return tableHeaderEl;
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
            throw new Exception("Error generating family table", e);
        }
    }

    private boolean isPatientNameField(String key)
    {
        return "first_name".equals(key) || "last_name".equals(key);
    }

    private boolean isVocabularyField(String key)
    {
        return "disorders".equals(key) || "features".equals(key);
    }

    private boolean isDateField(String key)
    {
        return "report_date".equals(key) || "last_modification_date".equals(key);
    }
}
