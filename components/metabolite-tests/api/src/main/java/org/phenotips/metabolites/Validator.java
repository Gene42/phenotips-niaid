package org.phenotips.metabolites;

import org.phenotips.data.Patient;
import org.phenotips.data.internal.PhenoTipsPatient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Validates that the date of birth and sex match, the column contents match their regex patterns, and performs high/low
 * checks.
 */
public class Validator
{
    public static ValidationResult validate(XWikiDocument patientDoc, List<String> preparedReportData,
        List<String> columnOrder, Integer columnCount)
    {
        ValidationResult result = new ValidationResult();
        try {
            // 1d data to 2d
            preparedReportData = insertEmptySpaces(preparedReportData, columnCount, 2);
            columnCount += 2;
            columnOrder.add("low");
            columnOrder.add("high");

            int lineStart = 0;
            int dataLength = preparedReportData.size();
            int specimenIndex = columnOrder.indexOf("specimen");
            int valueIndex = columnOrder.indexOf("value");
            int unitIndex = columnOrder.indexOf("unit");
            int normalLowIndex = columnOrder.indexOf("normal_low");
            int normalHighIndex = columnOrder.indexOf("normal_high");
            int lowIndex = columnOrder.indexOf("low");
            int highIndex = columnOrder.indexOf("high");
            int dobIndex = columnOrder.indexOf("dob");
            int sexIndex = columnOrder.indexOf("sex");
            // todo. regexes aren't exactly correct.
            while (lineStart + columnCount < dataLength) {
                boolean valid = true;
                if (specimenIndex != -1) {
                    valid = valid && preparedReportData.get(lineStart + specimenIndex).matches("[a-zA-Z ]+");
                }
                if (valueIndex != -1) {
                    valid = valid && preparedReportData.get(lineStart + valueIndex).matches("[0-9.]+");
                }
                if (unitIndex != -1) {
                    valid = valid && preparedReportData.get(lineStart + unitIndex).matches("[^0-9]+");
                }
                // doing some low/high calculating here
                if (normalLowIndex != -1) {
                    String normLow = preparedReportData.get(lineStart + normalLowIndex);
                    valid = valid && normLow.matches("[0-9.]*");
                    if (Float.parseFloat(preparedReportData.get(lineStart + valueIndex)) < Float.parseFloat(normLow)) {
                        preparedReportData.set(lineStart + lowIndex, "L");
                    }
                }
                if (normalHighIndex != -1) {
                    String normHigh = preparedReportData.get(lineStart + normalHighIndex);
                    valid = valid && normHigh.matches("[0-9.]*");
                    if (Float.parseFloat(preparedReportData.get(lineStart + valueIndex)) > Float.parseFloat(normHigh)) {
                        preparedReportData.set(lineStart + highIndex, "H");
                    }
                }
                if (dobIndex != -1) {
                    valid = valid && preparedReportData.get(lineStart + dobIndex).matches("[0-9./-]+");
                }
                if (sexIndex != -1) {
                    valid = valid && preparedReportData.get(lineStart + sexIndex).matches("[a-zA-Z]+");
                }
                if (!valid) {
                    result.validated = 1;
                    return result;
                }

                lineStart += columnCount;
            }

            result.columnCount = columnCount;
            result.columnOrder = columnOrder;
            result.data = preparedReportData;

            // in case there's an exception thrown
            result.validated = 2;
            if (!matchPatientInfo(patientDoc, preparedReportData.get(dobIndex), preparedReportData.get(sexIndex))) {
                return result;
            }
        } catch (Exception ex) {
            return result;
        }

        result.validated = 0;
        return result;
    }

    private static List<String> insertEmptySpaces(List<String> preparedReportData, Integer columnCount,
        Integer insertNum)
    {
        List<String> newList = new LinkedList<>();
        int lineStart = 0;
        int dataLength = preparedReportData.size();
        while (lineStart + columnCount < dataLength) {
            newList.addAll(preparedReportData.subList(lineStart, lineStart + columnCount));
            for (int i = 0; i < insertNum; i++) {
                newList.add("");
            }
            lineStart += columnCount;
        }
        return newList;
    }

    private static boolean matchPatientInfo(XWikiDocument patientDoc, String dob, String sex) throws Exception
    {
        if (dob != null || sex != null) {
            Patient patient = new PhenoTipsPatient(patientDoc);
            if (dob != null) {
                Date patientDob = (Date) patient.getData("date_of_birth");
                if (patientDob != null) {
                    Date parsedDob = null;
                    if (dob.contains("/")) {
                        if (dob.length() > 8) {
                            parsedDob = new SimpleDateFormat("MM/dd/yyyy").parse(dob);
                        } else {
                            parsedDob = new SimpleDateFormat("MM/dd/yy").parse(dob);
                        }
                    } else if (dob.contains(".")) {
                        if (dob.length() > 8) {
                            parsedDob = new SimpleDateFormat("dd.MM.yyyy").parse(dob);
                        } else {
                            parsedDob = new SimpleDateFormat("dd.MM.yy").parse(dob);
                        }
                    } else if (dob.contains("-")) {
                        if (dob.length() > 8) {
                            parsedDob = new SimpleDateFormat("dd-MM-yyyy").parse(dob);
                        } else {
                            parsedDob = new SimpleDateFormat("dd-MM-yy").parse(dob);
                        }
                    }
                    if (parsedDob != null) {
                        return DateUtils.isSameDay(patientDob, parsedDob);
                    }
                }
            }
            if (sex != null) {
                String patientSex = (String) patient.getData("sex").getValue();
                if (!StringUtils.equalsIgnoreCase("U", patientSex)) {
                    if ((StringUtils.equalsIgnoreCase("male", sex) || StringUtils.equalsIgnoreCase("M", sex))) {
                        return StringUtils.equalsIgnoreCase("M", patientSex);
                    } else {
                        return StringUtils.equalsIgnoreCase("F", patientSex);
                    }
                }
            }
        }
        return true;
    }

    public static class ValidationResult
    {
        // 0 - success, 1 - regex failed, 2 - patient did not match
        public int validated = 1;

        public List<String> data;

        public Integer columnCount;

        public List<String> columnOrder;
    }
}
