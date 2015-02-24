/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.metabolites;

import org.phenotips.configuration.RecordConfigurationManager;
import org.phenotips.data.internal.PhenoTipsPatient;
import org.phenotips.security.authorization.AuthorizationService;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.internal.WikiUser;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.thoughtworks.xstream.InitializationException;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Does data integrity checking, with subsequent storing.
 */
@Component
@Singleton
public class Processor implements ProcessorRole, Initializable
{
    private static final String[] display_columns_array =
        { "low", "high", "metabolite_name", "specimen", "value", "unit" };

    protected final static List<String> DISPLAY_COLUMNS = Arrays.asList(display_columns_array);

    private DateTimeFormatter dateFormatter;

    @Inject
    private HibernateSessionFactory xwikiSessionFactory;

    @Inject
    private RecordConfigurationManager configurationManager;

    @Inject
    private DocumentAccessBridge dab;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    @Named("current")
    private EntityReferenceResolver<EntityReference> entityReferenceResolver;

    public void initialize() throws InitializationException
    {
        String dateFormat = configurationManager.getActiveConfiguration().getDateOfBirthFormat();
        dateFormatter = DateTimeFormat.forPattern(dateFormat);
    }

    /**
     * @return error code
     */
    public int process(Map<String, String> fieldMap, XWikiContext xwikiContext, XWiki wiki) throws XWikiException
    {
        if (!checkAccess(xwikiContext, fieldMap.get("patient_id"), Right.EDIT)) {
            return 8;
        }
        long unixTime;
        List<String> preparedReportData = new LinkedList<>();
        Integer columnCount;
        List<String> columnOrder = new LinkedList<>();
        int error = 0;
        try {
            unixTime = dateFormatter.parseLocalDate(fieldMap.get("date")).toDate().getTime();
        } catch (Exception ex) {
            // Invalid date
            error = 3;
            return error;
        }
        try {
            for (String column : fieldMap.get("column_order").split(",")) {
                String trimmed = column.trim();
                if (StringUtils.isNotBlank(trimmed)) {
                    columnOrder.add(trimmed);
                }
            }
            List<Integer> skipIndices = new LinkedList<>();
            List<String> finalColumnOrder = new LinkedList<>();
            int index = 0;
            for (String column : columnOrder) {
                if (StringUtils.equalsIgnoreCase("-", column)) {
                    skipIndices.add(index);
                } else {
                    finalColumnOrder.add(column);
                }
                index++;
            }
            columnOrder = finalColumnOrder;
            boolean hasHeader = fieldMap.containsKey("has_header");
            columnCount = this.prepareReportData(fieldMap.get("file"), preparedReportData, skipIndices, hasHeader);
            // not checking for columnOrder.length == columnCount, because there could be ignore fields at the end of
            // lines, which we do not care about
        } catch (Exception ex) {
            error = 4;
            return error;
        }

        EntityReference patientReference = new EntityReference(fieldMap.get("patient_id"), EntityType.DOCUMENT,
            PhenoTipsPatient.DEFAULT_DATA_SPACE);
        XWikiDocument patientDocument = wiki.getDocument(patientReference, xwikiContext);
        // handles exceptions on it's own
        // adds high/low to column order, and modifies preparedReportData
        Validator.ValidationResult validation =
            Validator.validate(patientDocument, preparedReportData, columnOrder, columnCount);
        if (validation.validated > 0) {
            return validation.validated + 4;
        } else {
            preparedReportData = validation.data;
            columnOrder = validation.columnOrder;
            columnCount = validation.columnCount;
        }

        // nothing to save
        if (columnCount > 0) {
            TestReport report = new TestReport();
            report.patientId = fieldMap.get("patient_id");
            report.filepath = fieldMap.get("filepath");
            report.date = unixTime;
            report.data = preparedReportData;
            report.columnOrder = columnOrder;
            report.columnCount = columnCount;
            // if there's an error, will return 1, else 0
            error = store(report) * 7;
            return error;
        }
        return error;
    }

    private int prepareReportData(String csvString, List<String> prepared, List<Integer> skipIndices, boolean hasHeader) throws Exception
    {
        int columnCount = -1;
        boolean firstLine = true;
        String[] lines = csvString.split("\n");

        for (String line : lines) {
            List<String> columns = new LinkedList<>();
            String subline = line;
            int columnIndex = 0;
            int commaIndex = subline.indexOf(',');
            int quoteIndex = subline.indexOf('"');
            boolean openQuote = false;
            while (commaIndex != -1) {
                if (!skipIndices.contains(columnIndex)) {
                    if (openQuote) {
                        columns.add(subline.substring(0, quoteIndex));
                    } else {
                        columns.add(subline.substring(0, commaIndex));
                    }
                }

                if (commaIndex + 1 == quoteIndex) {
                    subline = subline.substring(quoteIndex + 1);
                    openQuote = true;
                } else {
                    if (openQuote) {
                        openQuote = false;
                        subline = subline.substring(quoteIndex);
                        commaIndex = subline.indexOf(',');
                    }
                    subline = subline.substring(commaIndex + 1);
                }
                commaIndex = subline.indexOf(',');
                quoteIndex = subline.indexOf('"');
                columnIndex += 1;
            }
            if (!skipIndices.contains(columnIndex)) {
                columns.add(subline);
            }

            if (columnCount < 0) {
                columnCount = columns.size();
            } else if (columnCount != columns.size()) {
                throw new Exception("Column count is not consistent throughout the document.");
            }

            if (hasHeader && firstLine) {
                firstLine = false;
            } else {
                prepared.addAll(columns);
            }
        }
        return columnCount;
    }

    private int store(TestReport report)
    {
        Session session = getSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            session.save(report);
            transaction.commit();
        } catch (HibernateException ex) {
            // could not store
            if (transaction != null) {
                transaction.rollback();
            }
            return 1;
        } finally {
            session.close();
        }
        return 0;
    }

    private List<TestReport> load(String patientId)
    {
        Session session = getSession();
        Transaction transaction;
        List<TestReport> reports = new LinkedList<>();
        try {
            transaction = session.beginTransaction();
            reports = session.createCriteria(TestReport.class).add(Restrictions.eq("patientId", patientId)).list();
            for (TestReport report : reports) {
                Hibernate.initialize(report.data);
                Hibernate.initialize(report.columnOrder);
            }
            transaction.commit();
        } catch (Exception ex) {
            // nothing to do
        } finally {
            session.close();
        }
        return reports;
    }

    @Override
    public JSONObject getJsonReports(String patientId, Integer offset, Integer limit, String sortColumn, String sortDir,
        Map<String, String> filters, XWikiContext xWikiContext)
    {
        if (!checkAccess(xWikiContext, patientId, Right.VIEW)) {
            return new JSONObject();
        }
        try {
            return testReportsToJson(load(patientId), offset - 1, limit, sortColumn, sortDir, filters);
        } catch (Exception ex) {
            return new JSONObject();
        }
    }

    private JSONObject testReportsToJson(List<TestReport> reports, Integer offset, Integer limit,
        final String sortColumn, String sortDir, final Map<String, String> filters)
    {
        JSONObject json = new JSONObject();
        List<JSONObject> rows = testReportsToRows(reports);
        json.put("totalrows", rows.size());

        if (!filters.isEmpty()) {
            rows.removeIf(new Predicate<JSONObject>()
            {
                @Override public boolean test(JSONObject jsonObject)
                {
                    for (Map.Entry<String, String> filter : filters.entrySet()) {
                        if (jsonObject.containsKey(filter.getKey()) && !StringUtils.containsIgnoreCase(
                            jsonObject.getString(filter.getKey()), filter.getValue()))
                        {
                            return true;
                        }
                    }
                    return false;
                }
            });
        }

        if (StringUtils.isNotBlank(sortColumn)) {
            Collections.sort(rows, new Comparator<JSONObject>()
            {
                @Override public int compare(JSONObject o1, JSONObject o2)
                {
                    if (StringUtils.equalsIgnoreCase(sortColumn, "date")) {
                        DateTime d1 = new DateTime(((JSONObject) o1.get(sortColumn)).get("millis"));
                        DateTime d2 = new DateTime(((JSONObject) o2.get(sortColumn)).get("millis"));
                        if (d1.isAfter(d2)) {
                            return -1;
                        } else {
                            return 1;
                        }
                    } else {
                        return String.CASE_INSENSITIVE_ORDER
                            .compare(o1.getString(sortColumn), o2.getString(sortColumn));
                    }
                }
            });
        }
        if (StringUtils.isNotBlank(sortDir) && StringUtils.equalsIgnoreCase("desc", sortDir)) {
            Collections.reverse(rows);
        }

        if (offset != null && limit != null) {
            int to = offset + limit;
            rows = rows.subList(offset, to > rows.size() ? rows.size() : to);
        }

        for (JSONObject row : rows) {
            DateTime date = new DateTime(((JSONObject) row.get("date")).get("millis"));
            row.put("date", dateFormatter.print(date));
        }

        JSONArray jsonRows = new JSONArray();
        jsonRows.addAll(rows);
        json.put("returnedrows", jsonRows.size());
        json.put("rows", jsonRows);
        return json;
    }

    private List<JSONObject> testReportsToRows(List<TestReport> reports)
    {
        List<JSONObject> rows = new LinkedList<>();

        for (TestReport report : reports) {
            Map<String, Integer> displayIndices = new LinkedHashMap<>();
            for (String column : DISPLAY_COLUMNS) {
                displayIndices.put(column, report.columnOrder.indexOf(column));
            }
            DateTime date = new DateTime(report.date);

            // 1d data to 2d
            int lineStart = 0;
            int dataLength = report.data.size();
            while (lineStart + report.columnCount < dataLength) {
                boolean isNotEmpty = false;
                JSONObject dataRow = new JSONObject();
                dataRow.put("doc_viewable", true);
                dataRow.put("date", date);

                for (Map.Entry<String, Integer> index : displayIndices.entrySet()) {
                    if (index.getValue() == -1) {
                        dataRow.put(index.getKey(), "");
                    } else {
                        isNotEmpty = true;
                        dataRow.put(index.getKey(), report.data.get(lineStart + index.getValue()));
                    }
                }
                if (isNotEmpty) {
                    rows.add(dataRow);
                }
                lineStart += report.columnCount;
            }
        }
        return rows;
    }

    private Session getSession()
    {
        return this.xwikiSessionFactory.getSessionFactory().openSession();
    }

    private Boolean checkAccess(XWikiContext xWikiContext, String patientId, Right right) {
        try {
            // don't repeat yourself
            EntityReference entityDocumentReference = new EntityReference(patientId, EntityType.DOCUMENT,
                PhenoTipsPatient.DEFAULT_DATA_SPACE);
            XWiki wiki = xWikiContext.getWiki();
            XWikiDocument patientDoc = wiki.getDocument(entityDocumentReference, xWikiContext);
            DocumentReference documentReference = patientDoc.getDocumentReference();
            DocumentReference userRef = xWikiContext.getUserReference();
            User user = new WikiUser(userRef, entityReferenceSerializer, dab, entityReferenceResolver);
            return authorizationService.hasAccess(user, right, documentReference);
        } catch (Exception ex) {
            return false;
        }
    }
}
