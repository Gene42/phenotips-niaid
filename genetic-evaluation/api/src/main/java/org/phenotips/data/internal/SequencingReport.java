/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONObject;

import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.DateProperty;

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
    public static final String FILEATTACHMENTS_PROPERTY_NAME = "file_attachments";

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
        .asList(FILEATTACHMENTS_PROPERTY_NAME, DATESEQUENCED_PROPERTY_NAME, VENDOR_PROPERTY_NAME,
            VENDORID_PROPERTY_NAME, DATEREVIEWED_PROPERTY_NAME, REVIEWEDBY_PROPERTY_NAME,
            EXTERNALLINKS_PROPERTY_NAME, EVALUATIONTYPE_PROPERTY_NAME, TARGETGENES_PROPERTY_NAME,
            DELDUP_PROPERTY_NAME, PANEL_PROPERTY_NAME, WES_PROPERTY_NAME, WGS_PROPERTY_NAME)));

    private static final String FILEATTACHMENTS_JSON_KEY = FILEATTACHMENTS_PROPERTY_NAME;

    private static final String DATESEQUENCED_JSON_KEY = DATESEQUENCED_PROPERTY_NAME;

    private static final String VENDOR_JSON_KEY = VENDOR_PROPERTY_NAME;

    private static final String VENDORID_JSON_KEY = VENDORID_PROPERTY_NAME;

    private static final String DATEREVIEWED_JSON_KEY = DATEREVIEWED_PROPERTY_NAME;

    private static final String REVIEWEDBY_JSON_KEY = REVIEWEDBY_PROPERTY_NAME;

    private static final String EXTERNALLINKS_JSON_KEY = EXTERNALLINKS_PROPERTY_NAME;

    private static final String EVALUATIONTYPE_JSON_KEY = EVALUATIONTYPE_PROPERTY_NAME;

    private static final String EVALUATIONMETHOD_JSON_KEY = "method";

    private List<String> fileAttachments;

    private DateTime dateSequenced;

    private String vendor;

    private String vendorId;

    private DateTime dateReviewed;

    private String reviewedBy;

    private List<String> externalLinks;

    private String evaluationType;

    /** Evaluation methods */
    private List<String> targetGenes;

    private String delDupTest;

    private String panelTest;

    private String wesPlatform;

    private String wgsPlatform;

    /**
     * Populates this SequencingReport object with the contents of the given XWiki BaseObject.
     *
     * @param xobj the object to parse (can be null)
     * @throws IllegalArgumentException if any error happens during parsing
     */
    public SequencingReport(BaseObject xobj) throws IllegalArgumentException
    {
        if (xobj == null) {
            return;
        }

        this.setFileAttachments(xobj.getListValue(FILEATTACHMENTS_PROPERTY_NAME));
        this.setDateSequenced(getDateFromXWikiObject(xobj, DATESEQUENCED_PROPERTY_NAME));
        this.setVendor(xobj.getStringValue(VENDOR_PROPERTY_NAME));
        this.setVendorId(xobj.getStringValue(VENDORID_PROPERTY_NAME));
        this.setDateReviewed(getDateFromXWikiObject(xobj, DATEREVIEWED_PROPERTY_NAME));
        this.setReviewedBy(xobj.getStringValue(REVIEWEDBY_PROPERTY_NAME));
        this.setExternalLinks(xobj.getListValue(EXTERNALLINKS_PROPERTY_NAME));
        this.setEvaluationType(xobj.getStringValue(EVALUATIONTYPE_PROPERTY_NAME));
        this.setEvaluationMethod(xobj);
    }

    /**
     * Converts object data to its json representation.
     *
     * @return the json representation of the object
     */
    public JSONObject toJSON()
    {
        JSONObject json = new JSONObject();
        DateTimeFormatter jsonDateFormat = ISODateTimeFormat.date();

        json.put(FILEATTACHMENTS_JSON_KEY, getFileAttachments());
        json.put(DATESEQUENCED_JSON_KEY, jsonDateFormat.print(getDateReviewed()));
        json.put(VENDOR_JSON_KEY, getVendor());
        json.put(VENDORID_JSON_KEY, getVendorId());
        json.put(DATEREVIEWED_JSON_KEY, jsonDateFormat.print(getDateReviewed()));
        json.put(REVIEWEDBY_JSON_KEY, getReviewedBy());
        json.put(EXTERNALLINKS_JSON_KEY, getExternalLinks());
        json.put(EVALUATIONTYPE_JSON_KEY, getEvaluationType());

        if (isMethodTargetGenes()) {
            json.put(EVALUATIONMETHOD_JSON_KEY, getListEvaluationMethod());
        } else {
            json.put(EVALUATIONMETHOD_JSON_KEY, getStringEvaluationMethod());
        }

        return json;
    }

    /**
     * Setter for fileAttachments.
     *
     * @param fileAttachments The names of the file attachments.
     * @return this object
     */
    public SequencingReport setFileAttachments(List<String> fileAttachments)
    {
        this.fileAttachments = fileAttachments;
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

    /**
     * Setter for one of the evaluation type methods. One of [ target genes | del/dup | panel | wes | wgs ]
     *
     * @param xobj The XObject for a sequencing report
     * @return this object
     */
    public SequencingReport setEvaluationMethod(BaseObject xobj)
    {
        if (StringUtils.isNotBlank(this.evaluationType)) {
            if (this.evaluationType.equals(TARGETGENES_PROPERTY_NAME)) {
                this.targetGenes.addAll(xobj.getListValue(TARGETGENES_PROPERTY_NAME));

            } else if (this.evaluationType.equals(DELDUP_PROPERTY_NAME)) {
                this.delDupTest = xobj.getStringValue(DELDUP_PROPERTY_NAME);

            } else if (this.evaluationType.equals(PANEL_PROPERTY_NAME)) {
                this.panelTest = xobj.getStringValue(PANEL_PROPERTY_NAME);

            } else if (this.evaluationType.equals(WES_PROPERTY_NAME)) {
                this.wesPlatform = xobj.getStringValue(WES_PROPERTY_NAME);

            } else if (this.evaluationType.equals(WGS_PROPERTY_NAME)) {
                this.wgsPlatform = xobj.getStringValue(WGS_PROPERTY_NAME);

            }
        }
        return this;
    }

    /**
     * Getter for fileAttachments.
     *
     * @return list of file attachment names or an empty list
     */
    public List<String> getFileAttachments()
    {
        return this.fileAttachments;
    }

    /**
     * Getter for dateSequenced.
     *
     * @return sequencing date or null
     */
    public DateTime getDateSequenced()
    {
        return this.dateSequenced;
    }

    /**
     * Getter for vendor.
     *
     * @return vendor name or an empty string
     */
    public String getVendor()
    {
        return this.vendor;
    }

    /**
     * Getter for vendorId.
     *
     * @return vendor id or an empty string
     */
    public String getVendorId()
    {
        return this.vendorId;
    }

    /**
     * Getter for dateReviewed.
     *
     * @return date report was reviewed or null
     */
    public DateTime getDateReviewed()
    {
        return this.dateReviewed;
    }

    /**
     * Getter for reviewedBy.
     *
     * @return user that reviewed the report or an empty string
     */
    public String getReviewedBy()
    {
        return this.reviewedBy;
    }

    /**
     * Getter for externalLinks.
     *
     * @return list of hyperlinks or an empty list
     */
    public List<String> getExternalLinks()
    {
        return this.externalLinks;
    }

    /**
     * Getter for evaluationType.
     *
     * @return the evaluation type or an empty string
     */
    public String getEvaluationType()
    {
        return this.evaluationType;
    }

    /**
     * Getter for target genes evaluationType method.
     *
     * @return the target genes used for evaluation or an empty list
     */
    public List<String> getListEvaluationMethod()
    {
        if (isMethodTargetGenes()) {
                return this.targetGenes;
        }
        return new ArrayList<>();
    }

    /**
     * Getter for one of the [deldup | panel | wes | wgs ] specific evaluationType methods.
     *
     * @return the evaluation type method (i.e. "microarray") or an empty string
     */
    public String getStringEvaluationMethod()
    {
        if (this.evaluationType.equals(DELDUP_PROPERTY_NAME)) {
            return this.delDupTest;

        } else if (this.evaluationType.equals(PANEL_PROPERTY_NAME)) {
            return this.panelTest;

        } else if (this.evaluationType.equals(WES_PROPERTY_NAME)) {
            return this.wesPlatform;

        } else if (this.evaluationType.equals(WGS_PROPERTY_NAME)) {
            return this.wgsPlatform;

        }
        return "";
    }

    private boolean isMethodTargetGenes()
    {
        return this.evaluationType.equals(TARGETGENES_PROPERTY_NAME) && this.targetGenes != null;
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