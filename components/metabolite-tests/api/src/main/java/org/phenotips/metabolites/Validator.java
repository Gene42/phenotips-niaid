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

import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
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
            preparedReportData = insertEmptySpaces(preparedReportData, columnCount, 1);
            columnCount += 1;
            columnOrder.add("deviation");

            int lineStart = 0;
            int dataLength = preparedReportData.size();
            int specimenIndex = columnOrder.indexOf("specimen");
            int valueIndex = columnOrder.indexOf("value");
            int unitIndex = columnOrder.indexOf("unit");
            int normalLowIndex = columnOrder.indexOf("normal_low");
            int normalHighIndex = columnOrder.indexOf("normal_high");
            int deviationIndex = columnOrder.indexOf("deviation");
            int dobIndex = columnOrder.indexOf("dob");
            int sexIndex = columnOrder.indexOf("sex");
            int sdIndex = columnOrder.indexOf("sd");
            // todo. regexes aren't exactly correct.
            while (lineStart + columnCount < dataLength) {
                boolean valid = true;
                String sdStr = null;
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
                }
                if (normalHighIndex != -1) {
                    String normHigh = preparedReportData.get(lineStart + normalHighIndex);
                    valid = valid && normHigh.matches("[0-9.]*");
                }
                if (sdIndex != -1) {
                    sdStr = preparedReportData.get(lineStart + sdIndex);
                    valid = valid && sdStr.matches("[-+0-9.]*");
                    preparedReportData.set(lineStart + deviationIndex, sdStr);
                }
                if (normalHighIndex != -1 && normalLowIndex != -1 && StringUtils.isBlank(sdStr)) {
                    // if normal high/low is set, but also have sd, will use just sd for deviation
                    String normalHighStr = preparedReportData.get(lineStart + normalHighIndex);
                    String normalLowStr = preparedReportData.get(lineStart + normalLowIndex);
                    if (StringUtils.isNoneBlank(normalHighStr, normalLowStr)) {
                        Float normalHigh = Float.parseFloat(normalHighStr);
                        Float normalLow = Float.parseFloat(normalLowStr);
                        Float sd = (normalHigh - normalLow) / 2;
                        Float midpoint = ((normalHigh - normalLow) / 2) + normalLow;
                        Float deviation =
                            (Float.parseFloat(preparedReportData.get(lineStart + valueIndex)) - midpoint) / sd;
                        String deviationString = String.format("%.2f", deviation);
                        preparedReportData.set(lineStart + deviationIndex, deviationString);
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
                PatientData<Date> dates = patient.getData("dates");
                Date patientDob = null;
                if (dates != null) {
                 patientDob = dates.get("date_of_birth");
                }
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
