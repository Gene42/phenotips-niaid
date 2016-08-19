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

import org.json.JSONObject;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

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
        String enteredValue = obj.getStringValue("date_of_birth_entered");
        if (enteredValue != null && !enteredValue.isEmpty()) {
            JSONObject enteredJSON = new JSONObject(enteredValue);

            // One of these two strings is guaranteed to be null
            String yearOfBirth = enteredJSON.optString("year");
            String decadeOfBirth = enteredJSON.optString("decade");


            if (yearOfBirth != null && !yearOfBirth.isEmpty()) {
                int year = Integer.parseInt(yearOfBirth);
                // Set year value
                obj.setIntValue("lower_year_of_birth", year);
                obj.setIntValue("upper_year_of_birth", year);
            }
            else if (decadeOfBirth != null && !decadeOfBirth.isEmpty())
            {
                int decade = Integer.parseInt(decadeOfBirth.substring(0, decadeOfBirth.length() - 1));
                // Set decade values
                obj.setIntValue("lower_year_of_birth", decade);
                obj.setIntValue("upper_year_of_birth", decade + 9);
            }
            // else it's an empty JSON object
        }

        // Record date of death info
        enteredValue = obj.getStringValue("date_of_death_entered");
        if (enteredValue != null && !enteredValue.isEmpty()) {
            JSONObject enteredJSON = new JSONObject(enteredValue);

            // One of these two strings is guaranteed to be null
            String yearOfDeath = enteredJSON.optString("year");
            String decadeOfDeath = enteredJSON.optString("decade");

            if (yearOfDeath != null && !yearOfDeath.isEmpty()) {
                int year = Integer.parseInt(yearOfDeath);
                // Set year value
                obj.setIntValue("lower_year_of_death", year);
                obj.setIntValue("upper_year_of_death", year);
            }
            else if (decadeOfDeath != null && !decadeOfDeath.isEmpty())
            {
                int decade = Integer.parseInt(decadeOfDeath.substring(0, decadeOfDeath.length() - 1));
                // Set decade values
                obj.setIntValue("lower_decade_of_death", decade);
                obj.setIntValue("upper_decade_of_death", decade + 9);
            }
            // else it's an empty object
        }

        String fullValue = obj.getStringValue("last_name");
        // Gets first letter from last name
        String stub = StringUtils.substring(fullValue, 0, 1);
        // Records to initial
        obj.setStringValue("initial", StringUtils.defaultIfBlank(stub, null));
    }
}
