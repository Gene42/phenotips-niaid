/*
 * This file is subject to the terms and conditions defined in file LICENSE,
 * which is part of this source code package.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 */
package org.phenotips.data.encrypted.internal;

import org.phenotips.Constants;
import org.phenotips.data.events.PatientChangingEvent;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Saves the year of birth in a non-encrypted field to allow filtering/searching by the year of birth, while keeping the
 * full date encrypted.
 *
 * @version $Id$
 */
@Component
@Named("patient-birthyear-sync")
@Singleton
public class PIISearchStubSync extends AbstractEventListener
{
    /** The XClass used for storing patient data. */
    private static final EntityReference CLASS_REFERENCE =
        new EntityReference("EncryptedPatientDataClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String DATE_SEPARATOR = "-";

    /** Default constructor, sets up the listener name and the list of events to subscribe to. */
    public PIISearchStubSync()
    {
        super("patient-birthyear-updater", new PatientChangingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument doc = (XWikiDocument) source;

        BaseObject obj = doc.getXObject(CLASS_REFERENCE);
        if (obj == null) {
            return;
        }

        // Record date of birth info
        readDates(obj, "date_of_birth_entered", "year_of_birth");

        // Record date of death info
        readDates(obj, "date_of_death_entered", "year_of_death");

        String fullValue = obj.getStringValue("last_name");
        // Gets first letter from last name
        String stub = StringUtils.substring(fullValue, 0, 1);
        // Records to initial
        obj.setStringValue("initial", StringUtils.defaultIfBlank(stub, null));
    }

    private void readDates(BaseObject obj, String fromDateProperty, String toYearProperty) {
        String enteredValue = obj.getStringValue(fromDateProperty);

        if (StringUtils.isNotEmpty(enteredValue)) {
            JSONObject enteredJSON = new JSONObject(enteredValue);

            // year and range of year
            String year = enteredJSON.optString("year");
            JSONObject rangeJSON = enteredJSON.optJSONObject("range");

            if (rangeJSON != null && StringUtils.isNotEmpty(year)) {
                String yearRange = rangeJSON.optString("years");
                if (!StringUtils.isNotEmpty(yearRange)) {
                    // A range was given but no "years" value was found
                    return;
                }

                int yearInt = Integer.parseInt(year);
                int rangeInt = Integer.parseInt(yearRange);
                // Set year values
                obj.setIntValue("lower_" + toYearProperty, yearInt);
                obj.setIntValue("upper_" + toYearProperty, yearInt + rangeInt);
            } else if (StringUtils.isNotEmpty(year)) {
                int yearInt = Integer.parseInt(year);
                // Set year values
                obj.setIntValue("lower_" + toYearProperty, yearInt);
                obj.setIntValue("upper_" + toYearProperty, yearInt);
            }
            // else it's an empty JSON object
            // occurs when an entered date is later removed
        }
    }
}
