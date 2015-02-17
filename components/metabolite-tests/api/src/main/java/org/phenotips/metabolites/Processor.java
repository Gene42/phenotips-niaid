package org.phenotips.metabolites;

import org.phenotips.configuration.RecordConfigurationManager;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.thoughtworks.xstream.InitializationException;
import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;

import net.sf.json.JSONArray;

/**
 * Does data integrity checking, with subsequent storing.
 */
@Component
@Singleton
public class Processor implements ProcessorRole, Initializable
{
    private static final String[] display_columns_array =
        { "low", "high", "metabolite_name", "specimen", "value", "unit" };

    private final List<String> DISPLAY_COLUMNS = Arrays.asList(display_columns_array);

    private DateTimeFormatter dateFormatter;

    @Inject
    private HibernateSessionFactory xwikiSessionFactory;

    @Inject
    private RecordConfigurationManager configurationManager;

    public void initialize() throws InitializationException
    {
        String dateFormat = configurationManager.getActiveConfiguration().getDateOfBirthFormat();
        dateFormatter = DateTimeFormat.forPattern(dateFormat);
    }

    /**
     * @return error code
     */
    public int process(Map<String, String> fieldMap)
    {
        long unixTime;
        List<String> preparedReportData = new LinkedList<>();
        int columnCount;
        List<String> columnOrder = new LinkedList<>();
        try {
            unixTime = dateFormatter.parseLocalDate(fieldMap.get("date")).toDate().getTime();
        } catch (Exception ex) {
            // Invalid date
            return 3;
        }
        try {
            for (String column : fieldMap.get("column_order").split(",")) {
                columnOrder.add(column.trim());
            }
            columnCount = this.prepareReportData(fieldMap.get("filepath"), preparedReportData);
            // not checking for columnOrder.length == columnCount, because there could be ignore fields at the end of
            // lines, which we do not care about
        } catch (Exception ex) {
            return 4;
        }

        // nothing to save
        if (columnCount > 0) {
            TestReport report = new TestReport();
            report.patientId = fieldMap.get("patient_id");
            report.date = unixTime;
            report.data = preparedReportData;
            report.columnOrder = columnOrder;
            report.columnCount = columnCount;
            // if there's an error, will return 1, else 0
            return store(report) * 5;
        }
        return 0;
    }

    private int prepareReportData(String csvString, List<String> prepared) throws Exception
    {
        int columnCount = -1;
        String[] lines = csvString.split("\n");
        for (String line : lines) {
            String[] columns = StringUtils.splitPreserveAllTokens(line, ",");
            if (columnCount < 0) {
                columnCount = columns.length;
            } else if (columnCount != columns.length) {
                throw new Exception("Column count is not consistent throughout the document.");
            }

            prepared.addAll(Arrays.asList(columns));
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

    @Override public JSONArray getJsonReports(String patientId)
    {
        try {
            return testReportsToJson(load(patientId));
        } catch (Exception ex) {
            return new JSONArray();
        }
    }

    private JSONArray testReportsToJson(List<TestReport> reports)
    {
        JSONArray json = new JSONArray();
        List<String> columns = getDisplayColumns(reports);

        for (TestReport report : reports) {
            List<Integer> displayIndices = new LinkedList<>();
            for (String c : columns) {
                displayIndices.add(report.columnOrder.indexOf(c));
            }

            // 1d data to 2d
            int lineStart = 0;
            int dataLength = report.data.size();
            while (lineStart + report.columnCount < dataLength) {
                JSONArray dataRow = new JSONArray();
                int displayColumnCounter = 0;
                for (Integer index : displayIndices) {
                    if (index > -1) {
                        dataRow.set(displayColumnCounter, "");
                    } else {
                        dataRow.set(displayColumnCounter, report.data.get(lineStart + index));
                    }
                    displayColumnCounter++;
                }
                json.add(dataRow);
                lineStart += report.columnCount;
            }
        }

        return json;
    }

    @Override public JSONArray getDisplayColumns(String patientId)
    {
        JSONArray columns = new JSONArray();
        try {
            columns.addAll(getDisplayColumns(load(patientId)));
            return columns;
        } catch (Exception ex) {
            return columns;
        }
    }

    private List<String> getDisplayColumns(List<TestReport> reports)
    {
        List<String> columns = new LinkedList<>();
        for (TestReport report : reports) {
            List<String> columnsToDisplay = filterColumnsForDisplay(report.columnOrder);
            columns.addAll(columnsToDisplay);
        }
        return columns;
    }

    private List<String> filterColumnsForDisplay(List<String> columns)
    {
        List<String> filtered = new LinkedList<>();
        filtered.retainAll(DISPLAY_COLUMNS);
        return filtered;
    }

    private Session getSession()
    {
        return this.xwikiSessionFactory.getSessionFactory().openSession();
    }
}
