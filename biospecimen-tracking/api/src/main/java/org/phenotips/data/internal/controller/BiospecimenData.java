/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.data.internal.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.DateProperty;
import com.xpn.xwiki.objects.StringProperty;

/**
 * Container for biospecimens data.
 *
 * @version $Id$
 */
public class BiospecimenData
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

    private Date dateCollected;

    private Date dateReceived;

    /**
     * Constructor.
     */
    public BiospecimenData() { }

    /**
     * Constructor.
     * @param xWikiObject the object to parse
     */
    public BiospecimenData(BaseObject xWikiObject)
    {

        StringProperty typeField = (StringProperty) xWikiObject.getField(TYPE_PROPERTY_NAME);
        if (typeField != null) {
            this.type = typeField.getValue();
        }

        this.dateCollected = getDateFromXWikiObject(xWikiObject, DATE_COLLECTED_PROPERTY_NAME);
        this.dateReceived = getDateFromXWikiObject(xWikiObject, DATE_RECEIVED_PROPERTY_NAME);

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
    public BiospecimenData setType(String type)
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
    public BiospecimenData setDateCollected(Date dateCollected)
    {
        this.dateCollected = ObjectUtils.clone(dateCollected);
        return this;
    }

    /**
     * Setter for dateReceived.
     *
     * @param dateReceived the dateReceived
     * @return this object
     */
    public BiospecimenData setDateReceived(Date dateReceived)
    {
        this.dateReceived = ObjectUtils.clone(dateReceived);
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
    public Date getDateCollected()
    {
        return ObjectUtils.clone(dateCollected);
    }

    /**
     * Getter for dateReceived.
     * @return the dateReceived
     */
    public Date getDateReceived()
    {
        return ObjectUtils.clone(dateReceived);
    }

    @Override
    public String toString()
    {
        return "(type=" + this.type + ", dateCollected=" + this.dateCollected + ", dateReceived="
            + this.dateReceived + ")";
    }

    private static Date getDateFromXWikiObject(BaseObject xWikiObject, String propertyName)
    {
        DateProperty dateField = (DateProperty) xWikiObject.getField(propertyName);
        if (dateField == null || dateField.getValue() == null) {
            return null;
        }
        return (Date) dateField.getValue();
    }
}
