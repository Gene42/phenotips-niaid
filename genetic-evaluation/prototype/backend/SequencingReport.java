/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */

package org.phenotips.data.internal.controller;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONObject;

import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.DateProperty;
import com.xpn.xwiki.objects.StringProperty;

/**
 * Container for a SequencingReport.
 *
 * @version $Id$
 */
public class SequencingReport
{
    /**
     * The filename for the uploaded report.
     */
    public static final String REPORT_PROPERTY_NAME = "report";

    /**
     * Date when sequencing was performed.
     */
    public static final String DATESEQUENCED_PROPERTY_NAME = "date_sequenced";

    /**
     * Vendor used for sequencing.
     */
    public static final String VENDOR_PROPERTY_NAME = "vendor";

    /**
     * The ID of the vendor.
     */
    public static final String VENDORID_PROPERTY_NAME = "vendor_id";

    /**
     * Date when report was reviewed.
     */
    public static final String DATEREVIEWED_PROPERTY_NAME = "date_reviewed";

    /**
     * The user that reviewed the report.
     */
    public static final String REVIEWEDBY_PROPERTY_NAME = "reviewed_by";

    /**
     * External hyperlinks to data.
     */
    public static final String EXTERNALLINKS_PROPERTY_NAME = "external_links";

    /**
     * The method used for evaluation. One of [ target genes | del/dup | panel | wes | wgs ]
     */
    public static final String EVALUATIONTYPE_PROPERTY_NAME = "evaluation_type";

    /**
     * The genes targetted for testing.
     */
    public static final String TARGETGENES_PROPERTY_NAME = "target_genes";

    /**
     * The method of deletion/duplication testing.
     */
    public static final String DELDUP_PROPERTY_NAME = "deldup";

    /**
     * The method of panel testing.
     */
    public static final String PANEL_PROPERTY_NAME = "panel";

    /**
     * The whole exome sequencing platform.
     */
    public static final String WES_PROPERTY_NAME = "wes";

    /**
     * The whole genome sequencing platform.
     */
    public static final String WGS_PROPERTY_NAME = "wgs";

    /**
     * An ordered set of all the property names of the SequencingReportClass.
     */
    public static final Set<String> PROPERTIES = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays
        .asList(REPORT_PROPERTY_NAME, DATESEQUENCED_PROPERTY_NAME, VENDOR_PROPERTY_NAME,
            VENDORID_PROPERTY_NAME, DATEREVIEWED_PROPERTY_NAME, REVIEWEDBY_PROPERTY_NAME,
            EXTERNALLINKS_PROPERTY_NAME, EVALUATIONTYPE_PROPERTY_NAME, TARGETGENES_PROPERTY_NAME,
            DELDUP_PROPERTY_NAME, PANEL_PROPERTY_NAME, WES_PROPERTY_NAME, WGS_PROPERTY_NAME)));

    private String report;

    private DateTime dateSequenced;

    private String vendor;

    private String vendorId;

    private DateTime dateReviewed;

    private String reviewedBy; // Or EntityReference to the PT User

    private List<String> externalLinks;

    private String evaluationType;

    private List<String> targetGenes;

    private String delDupTest;

    private String panelTest;

    private String wesPlatform;

    private String wgsPlatform;

    private static final Set<String> EVALUATION_TYPE_FIELDS = Collections.unmodifiableSet(new LinkedHashSet<>(
        Arrays.asList(TARGETGENES_PROPERTY_NAME, DELDUP_PROPERTY_NAME, PANEL_PROPERTY_NAME, WES_PROPERTY_NAME,
            WGS_PROPERTY_NAME)));

    /**
     * Populates this SequencingReport object with the contents of the given XWiki BaseObject.
     *
     * @param xWikiObject the object to parse (can be null)
     * @throws IllegalArgumentException if any error happens during parsing
     */
    public SequencingReport(BaseObject xobj) throws IllegalArgumentException
    {
        if (xobj == null) {
            return;
        }

        this.setReport(xobj.getStringValue(REPORT_PROPERTY_NAME));
        this.setDateSequenced(getDateFromXWikiObject(xobj, DATESEQUENCED_PROPERTY_NAME));
        this.setVendor(xobj.getStringValue(VENDOR_PROPERTY_NAME));
        this.setVendorId(xobj.getStringValue(VENDORID_PROPERTY_NAME));
        this.setDateReviewed(getDateFromXWikiObject(xobj, DATEREVIEWED_PROPERTY_NAME));
        this.setReviewedBy(xobj.getStringValue(REVIEWEDBY_PROPERTY_NAME));
        this.setExternalLinks(xobj.getStringListValue(EXTERNALLINKS_PROPERTY_NAME));
        this.setEvaluationType(xobj.getStringValue(EVALUATIONTYPE_PROPERTY_NAME));
    }

    /**
     * Setter for report.
     *
     * @param report The uploaded report name.
     * @return this object
     */
    public SequencingReport setReport(String report)
    {
        this.report = report;
        return this;
    }

    /**
     * Setter for dateSequenced.
     *
     * @param dateSequenced The date of sequencing.
     * @return this object
     */
    public SequencingReport setDateSequenced(DateTime dateSequenced)
    {
        this.dateSequenced = dateSequenced;
        return this;
    }

    /**
     * Setter for vendor.
     *
     * @param vendor The vendor used.
     * @return this object
     */
    public SequencingReport setVendor(String vendor)
    {
        this.vendor = vendor;
        return this;
    }

    /**
     * Setter for vendorId.
     *
     * @param vendorId The id of the vendor.
     * @return this object
     */
    public SequencingReport setVendorId(String vendorId)
    {
        this.vendorId = vendorId;
        return this;
    }


    /**
     * Setter for dateReviewed.
     *
     * @param dateReviewed The date the report was reviewed.
     * @return this object
     */
    public SequencingReport setDateReviewed(DateTime dateReviewed)
    {
        this.dateReviewed = dateReviewed;
        return this;
    }

    /**
     * Setter for reviewedBy.
     *
     * @param reviewedBy The name of the user that reviewed the report.
     * @return this object
     */
    public SequencingReport setReviewedBy(String reviewedBy)
    {
        this.reviewedBy = reviewedBy;
        return this;
    }

    /**
     * Setter for externalLinks.
     *
     * @param externalLinks The list of external hyperlinks to data about the report.
     * @return this object
     */
    public SequencingReport setExternalLinks(List<String> externalLinks)
    {
        this.externalLinks = externalLinks;
        return this;
    }

    /**
     * Setter for evaluationType.
     *
     * @param evaluationType The evaluation type used for sequencing.
     * @return this object
     */
    public SequencingReport setEvaluationType(String evaluationType)
    {
        this.evaluationType = evaluationType;
        return this;
    }

    private static DateTime getDateFromXWikiObject(BaseObject xWikiObject, String propertyName)
    {
        DateProperty dateField = (DateProperty) xWikiObject.getField(propertyName);
        if (dateField == null || dateField.getValue() == null) {
            return null;
        }
        return new DateTime(dateField.getValue());
    }
}