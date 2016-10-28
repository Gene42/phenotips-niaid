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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
 * Class for generating an HTML table.
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
    private final String notAvailableTag = "N/A";
    private final String cssClass = "class";
    private final String span = "span";

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
            JSONObject memberProperties = node.optJSONObject("prop");
            if (memberProperties != null && memberProperties.length() != 0 && !memberProperties.has("phenotipsId")) {
                nonPatientMembers.add(node);
            }
        }
        return nonPatientMembers;
    }

    private Element getTableHeaderRow()
    {
        Element tableHeaderEl = document.createElement("thead");

        for (String selectedField : selectedFields) {
            Element cellEl = document.createElement("th");
            cellEl.appendChild(document.createTextNode(tableHeaders.getString(selectedField)));
            tableHeaderEl.appendChild(cellEl);
        }
        return tableHeaderEl;
    }

    private Element getRow(JSONObject data, boolean isPatient) throws Exception
    {
        Element tableRowEl = document.createElement("tbody");
        tableRowEl.setAttribute(cssClass, "familyMemberRow");

        if (!isPatient && (data.optJSONObject("prop") == null || data.optJSONObject("prop").length() == 0)) {
            return tableRowEl;
        }

        for (String selectedField : selectedFields) {
            tableRowEl.appendChild(getRowColCell(selectedField, data, isPatient));
        }

        return tableRowEl;
    }

    private Element getRowColCell(String field, JSONObject data, boolean isPatient)
    {
        Element cellEl = document.createElement("td");

        JSONObject member = data;
        String nodeId = field;
        if (!isPatient) {
            nodeId = member.optString("id");
            member = data.optJSONObject("prop");
        }

        if (isId(field)) {
            setIdCell(cellEl, nodeId, member, isPatient);

        } else if (isName(field)) {
            setNameCell(cellEl, field, member, isPatient);

        } else if (isDate(field)) {
            setDateCell(cellEl, field, member, isPatient);

        } else if (isVocabulary(field)) {
            setVocabularyCell(cellEl, field, member, isPatient);

        } else if (isUser(field) && isPatient) {
            String username = member.optString(field);
            Element userLink = getLinkElement("/XWiki/" + username, "", " " + username, false);
            Element icon = document.createElement("i");
            icon.setAttribute(cssClass, "fa fa-user");
            cellEl.appendChild(icon);
            cellEl.appendChild(userLink);

        } else {
            setSimpleCell(cellEl, field, member, isPatient);
        }

        return cellEl;
    }

    private void setSimpleCell(Element cellEl, String field, JSONObject member, boolean isPatient)
    {
        String value = isPatient ? member.optString(field) : notAvailableTag;
        cellEl.appendChild(document.createTextNode(value));
    }

    private void setIdCell(Element cellEl, String field, JSONObject member, boolean isPatient) {
        if (isPatient) {
            String id = member.optString(field);
            cellEl.appendChild(getLinkElement("/" + id, "identifier", id, false));
        } else {
            Element idEl = document.createElement(span);
            idEl.setAttribute(cssClass, "identifier");
            idEl.setAttribute("style", "display: none");
            idEl.appendChild(document.createTextNode(field));
            cellEl.appendChild(document.createTextNode(notAvailableTag));
            cellEl.appendChild(idEl);
        }
    }

    private void setNameCell(Element cellEl, String field, JSONObject member, boolean isPatient) {
        if (isPatient) {
            JSONObject nameObj = member.optJSONObject("patient_name");
            if (nameObj != null) {
                cellEl.appendChild(document.createTextNode(nameObj.optString(field)));
            }
        } else {
            cellEl.appendChild(document.createTextNode(member.optString(translatedLabels.optString(field))));
        }
    }

    private void setDateCell(Element cellEl, String field, JSONObject member, boolean isPatient) {
        if (isPatient) {
            SimpleDateFormat dateFormatter = new SimpleDateFormat(dateFormat);
            try {
                Date date = dateFormatter.parse(member.getString(field));
                cellEl.appendChild(document.createTextNode(dateFormatter.format(date)));
            } catch (ParseException e) {
            }
        } else {
            setSimpleCell(cellEl, "", member, isPatient);
        }
    }

    private void setVocabularyCell(Element cellEl, String field, JSONObject member, boolean isPatient)
    {
        if (isPatient) {
            appendVocabularyTerms(cellEl, member.optJSONArray(field), false);
        } else {
            appendVocabularyTerms(cellEl, member.optJSONArray(translatedLabels.optString(field)), false);
        }
    }

    private void appendVocabularyTerms(Element cellEl, JSONArray vocabArray, boolean includeHyperlink)
    {
        if (cellEl == null || vocabArray == null) {
            return;
        }

        for (Object obj : vocabArray) {
            String val = null;
            String termId = null;
            if (obj instanceof String) {
                val = (String) obj;
            } else if (obj instanceof JSONObject) {
                JSONObject vocabObj = (JSONObject) obj;
                val = vocabObj.optString("label");
                termId = vocabObj.optString("id");
            }

            Element listNode = document.createElement("ul");
            if (termId != null) {
                String infoType = "";
                infoType = termId.startsWith("MIM:") ? "omim-disease-info" : "phenotype-info";

                if (includeHyperlink) {
                    String link = termId.startsWith("MIM:") ? "http://www.omim.org/entry/" + termId.substring(4)
                        : "http://compbio.charite.de/hpoweb/showterm?id=" + termId;
                    listNode.appendChild(getLinkElement(link, "vocabLink", "[" + termId + "]", true));
                }

                Element label = document.createElement(span);
                label.setAttribute(cssClass, "vocabLabel");
                label.appendChild(document.createTextNode(val));
                listNode.appendChild(label);

                Element helpButton = document.createElement(span);
                helpButton.setAttribute(cssClass, "fa fa-info-circle xHelpButton " + infoType);
                helpButton.setAttribute("title", termId);
                listNode.appendChild(helpButton);
            } else {
                listNode.appendChild(document.createTextNode(val));
            }
            cellEl.appendChild(listNode);
        }
    }

    private Element getLinkElement(String link, String innerClass, String innerHTML, boolean isExternal)
    {
        String wrapperClass = isExternal ? "wikiexternallink" : "wikilink";
        Element linkWrapper = document.createElement(span);
        linkWrapper.setAttribute(cssClass, wrapperClass);
        Element linkEl = document.createElement("a");
        linkEl.setAttribute(cssClass, innerClass);
        linkEl.setAttribute("target", "_blank");
        linkEl.setAttribute("href", link);
        linkEl.appendChild(document.createTextNode(innerHTML));
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

            DOMSource domSource = new DOMSource(document);
            transformer.transform(domSource, result);

            return writer.toString();
        } catch (TransformerConfigurationException e) {
            throw new Exception("Error writing HTML content for the table of family members", e);
        }
    }

    private boolean isName(String key) { return "first_name".equals(key) || "last_name".equals(key); }

    private boolean isVocabulary(String key) { return "disorders".equals(key) || "features".equals(key); }

    private boolean isDate(String key) { return "date".equals(key) || "last_modification_date".equals(key); }

    private boolean isId(String key) { return "id".equals(key); }

    private boolean isUser(String key) { return "reporter".equals(key); }
}
