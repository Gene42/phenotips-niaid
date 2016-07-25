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
 * Container for a biospecimen.
 *
 * @version $Id$
 */
public class Biospecimen
{
    /**
     * The type of biospecimen. It is a static list: {Skin|Hair|Nails|Blood plasma|Serum}.
     */
    public static final String TYPE_PROPERTY_NAME = "type";

    /**
     * Date when biospecimen was collected.
     */
    public static final String DATE_COLLECTED_PROPERTY_NAME = "date_collected";

    /**
     * Date when biospecimen was received.
     */
    public static final String DATE_RECEIVED_PROPERTY_NAME = "date_received";

    /**
     * An ordered set of all the property names of the BiospecimenClass.
     */
    public static final Set<String> PROPERTIES = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays
        .asList(TYPE_PROPERTY_NAME, DATE_COLLECTED_PROPERTY_NAME, DATE_RECEIVED_PROPERTY_NAME)));

    private String type;

    private DateTime dateCollected;

    private DateTime dateReceived;

    /**
     * Base constructor.
     */
    public Biospecimen()
    {
    }

    /**
     * Populates this Biospecimen object with the contents of the given XWiki BaseObject.
     * @param xWikiObject the object to parse (can be null)
     * @throws IllegalArgumentException if any error happens during parsing
     */
    public Biospecimen(BaseObject xWikiObject) throws IllegalArgumentException
    {
        if (xWikiObject == null) {
            return;
        }

        StringProperty typeField = (StringProperty) xWikiObject.getField(TYPE_PROPERTY_NAME);
        if (typeField != null) {
            this.setType(typeField.getValue());
        }

        this.setDateCollected(getDateFromXWikiObject(xWikiObject, DATE_COLLECTED_PROPERTY_NAME));
        this.setDateReceived(getDateFromXWikiObject(xWikiObject, DATE_RECEIVED_PROPERTY_NAME));
    }

    /**
     * Populates this Biospecimen object with the contents of the given JSONObject.
     * @param jsonObject the JSONObject to parse (can be null)
     * @throws IllegalArgumentException if incoming object is invalid
     * @throws UnsupportedOperationException if any field parsing fails
     */
    public Biospecimen(JSONObject jsonObject) throws IllegalArgumentException, UnsupportedOperationException
    {
        if (jsonObject == null) {
            return;
        }

        DateTimeFormatter formatter = ISODateTimeFormat.date();

        for (String property : Biospecimen.PROPERTIES) {
            String value = jsonObject.optString(property);

            if (StringUtils.isBlank(value)) {
                continue;
            }

            switch (property) {
                case Biospecimen.TYPE_PROPERTY_NAME:
                    this.setType(value);
                    break;
                case Biospecimen.DATE_COLLECTED_PROPERTY_NAME:
                    this.setDateCollected(formatter.parseDateTime(value));
                    break;
                case Biospecimen.DATE_RECEIVED_PROPERTY_NAME:
                    this.setDateReceived(formatter.parseDateTime(value));
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Converts this biospecimen object into a JSONObject.
     * @param propertiesToInclude a list of properties to include, if null/empty, all properties will be added to the
     *                              resulting JSONObject
     * @return a JSONObject or null if the resulting JSONObject is empty
     */
    public JSONObject toJSON(Collection<String> propertiesToInclude) {

        Collection<String> propertiesToIterateOver = propertiesToInclude;
        if (CollectionUtils.isEmpty(propertiesToIterateOver)) {
            propertiesToIterateOver = Biospecimen.PROPERTIES;
        }

        JSONObject jsonObject = new JSONObject();
        DateTimeFormatter jsonDateFormat = ISODateTimeFormat.date();

        for (String propertyName : propertiesToIterateOver) {

            switch (propertyName) {
                case Biospecimen.TYPE_PROPERTY_NAME:
                    if (this.hasType()) {
                        jsonObject.put(propertyName, this.getType());
                    }
                    break;
                case Biospecimen.DATE_COLLECTED_PROPERTY_NAME:
                    if (this.hasDateCollected()) {
                        jsonObject.put(propertyName, jsonDateFormat.print(this.getDateCollected()));

                    }
                    break;
                case Biospecimen.DATE_RECEIVED_PROPERTY_NAME:
                    if (this.hasDateReceived()) {
                        jsonObject.put(propertyName, jsonDateFormat.print(this.getDateReceived()));
                    }
                    break;
                default:
                    break;
            }
        }

        if (jsonObject.length() == 0) {
            return null;
        }

        return jsonObject;
    }

    /**
     * Returns true only if all fields values are null/empty.
     * @return a boolean
     */
    public boolean isEmpty() {
        return (StringUtils.isEmpty(this.type) && this.dateCollected == null && this.dateReceived == null);
    }

    /**
     * Returns true if any of the fields is not null/empty.
     * @return a boolean
     */
    public boolean isNotEmpty() {
        return !this.isEmpty();
    }

    /**
     * Setter for type.
     *
     * @param type the type
     * @return this object
     */
    public Biospecimen setType(String type)
    {
        this.type = type;
        return this;
    }

    /**
     * Setter for dateCollected.
     *
     * @param dateCollected the dateCollected
     * @return this object
     */
    public Biospecimen setDateCollected(DateTime dateCollected)
    {
        this.dateCollected = dateCollected;
        return this;
    }

    /**
     * Setter for dateReceived.
     *
     * @param dateReceived the dateReceived
     * @return this object
     */
    public Biospecimen setDateReceived(DateTime dateReceived)
    {
        this.dateReceived = dateReceived;
        return this;
    }

    /**
     * Getter for type.
     * @return the type
     */
    public String getType()
    {
        return type;
    }

    /**
     * Getter for dateCollected.
     * @return the dateCollected
     */
    public DateTime getDateCollected()
    {
        return dateCollected;
    }

    /**
     * Getter for dateReceived.
     * @return the dateReceived
     */
    public DateTime getDateReceived()
    {
        return dateReceived;
    }

    /**
     * Checks if type exists, ie is not null/empty.
     * @return true if type not blank, false otherwise
     */
    public boolean hasType()
    {
        return StringUtils.isNotBlank(this.type);
    }

    /**
     * Checks if dateCollected exists (is not null).
     * @return true if dateCollected is not null, false otherwise
     */
    public boolean hasDateCollected()
    {
        return this.dateCollected != null;
    }

    /**
     * Checks if dateReceived exists (is not null).
     * @return true if dateReceived is not null, false otherwise
     */
    public boolean hasDateReceived()
    {
        return this.dateReceived != null;
    }

    @Override
    public String toString()
    {
        return "(type=" + this.type + ", dateCollected=" + this.dateCollected + ", dateReceived="
            + this.dateReceived + ")";
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
