/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.rest.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientRepository;
import org.phenotips.data.rest.PatientSuggestionsResource;
import org.phenotips.security.authorization.AuthorizationService;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.UserManager;

import java.io.StringWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Default implementation of PatientSuggestionsResource, using an XWQL query to fetch the result, the PhenoTips
 * Patient API to get patient data, and the W3C DOM API to build the XML response.
 *
 * @version $Id$
 */
@Component
@Named("org.phenotips.data.rest.internal.DefaultPatientSuggestionsResource")
@Singleton
@SuppressWarnings({"checkstyle:classfanoutcomplexity"})
public class DefaultPatientSuggestionsResource implements PatientSuggestionsResource
{
    @Inject
    private QueryManager qm;

    @Inject
    private Logger logger;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private UserManager users;

    @Inject
    private PatientRepository patientRepository;

    @Override
    public Response suggestPatients(String input, String limit) throws Exception
    {
        String lastNameInitial = null;
        Matcher lastNameInitialMatcher = Pattern.compile("[A-z]").matcher(input);
        if (lastNameInitialMatcher.find()) {
            lastNameInitial = lastNameInitialMatcher.group().substring(0, 1);
        }
        Integer yearOfBirth = null;
        Matcher yearOfBirthMatcher = Pattern.compile("\\d{4}").matcher(input);
        if (yearOfBirthMatcher.find()) {
            yearOfBirth = new Integer(yearOfBirthMatcher.group());
        }

        Document doc = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.newDocument();
        } catch (ParserConfigurationException e) {
            throw new Exception("Error generating patients XML result", e);
        }

        Element resultsEl = doc.createElement("results");
        doc.appendChild(resultsEl);

        if (lastNameInitial != null && yearOfBirth != null) {
            List<String> queryResults = runQuery(lastNameInitial, yearOfBirth, Integer.parseInt(limit));
            addElementsFromQueryResults(doc, queryResults, resultsEl);
        }

        return Response.ok(getXmlOutput(doc), MediaType.TEXT_XML).build();
    }

    private void addElementsFromQueryResults(Document doc, List<String> queryResults, Element resultsEl)
    {
        for (String queryResult : queryResults) {
            Patient patient = this.patientRepository.get(queryResult);
            if (patient == null) {
                continue;
            }

            if (!this.authorizationService.hasAccess(this.users.getCurrentUser(), Right.VIEW, patient.getDocument())) {
                continue;
            }

            Element resultEl = doc.createElement("rs");
            resultEl.setAttribute("id", "/" + patient.getId());
            resultEl.setAttribute("info", patient.getDocument().toString());

            String summaryText = getSummaryText(patient);
            resultEl.appendChild(doc.createTextNode(summaryText));

            resultsEl.appendChild(resultEl);
        }
    }

    private String getSummaryText(Patient patient)
    {
        PatientData<String> nameObj = patient.getData("patientName");
        StringBuilder summaryText = new StringBuilder(patient.getName());
        String fullName = null;
        if (nameObj != null) {
            StringBuilder fullNameBuilder = new StringBuilder();
            String firstName = nameObj.get("first_name");
            if (firstName != null && !firstName.isEmpty()) {
                fullNameBuilder.append(firstName);
            }
            String lastName = nameObj.get("last_name");
            if (lastName != null && !lastName.isEmpty()) {
                fullNameBuilder.append(" ");
                fullNameBuilder.append(lastName);
            }
            fullName = fullNameBuilder.toString().trim();
        }
        if (StringUtils.isNotBlank(fullName)) {
            summaryText.append(String.format(" (name: %s)", fullName));
        }
        return summaryText.toString();
    }

    private List<String> runQuery(String lastNameInitial, Integer yearOfBirth, int limit) throws Exception
    {
        StringBuilder querySb = new StringBuilder();
        querySb.append("select doc.name ");
        querySb.append(" from  Document doc, ");
        querySb.append("       doc.object(PhenoTips.EncryptedPatientDataClass) as encryptedData ");
        querySb.append(" where doc.name <> 'PatientTemplate'");
        querySb.append(" and encryptedData.upper_year_of_birth = :yearOfBirth");
        querySb.append(" and encryptedData.lower_year_of_birth = :yearOfBirth");
        querySb.append(" and lower(encryptedData.initial) = :lastNameInitial");

        String queryString = querySb.toString();
        Query query;
        List<String> queryResults = null;
        try {
            query = this.qm.createQuery(queryString, Query.XWQL);
            query.setLimit(limit);
            query.bindValue("lastNameInitial", lastNameInitial.toLowerCase());
            query.bindValue("yearOfBirth", yearOfBirth);
            queryResults = query.execute();
        } catch (QueryException e) {
            throw new Exception("Error while performing Patients suggest query", e);
        }
        return queryResults;
    }

    private String getXmlOutput(Document doc) throws Exception
    {
        try {
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();

            DOMSource domSource = new DOMSource(doc);
            transformer.transform(domSource, result);

            return writer.toString();
        } catch (TransformerException e) {
            throw new Exception("Error generating patients XML result", e);
        }
    }
}
