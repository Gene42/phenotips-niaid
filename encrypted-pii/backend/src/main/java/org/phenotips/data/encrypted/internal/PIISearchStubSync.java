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
        String fullValue = obj.getStringValue("date_of_birth");
        String stub = StringUtils.substringBefore(fullValue, "-");
        obj.setStringValue("year_of_birth", StringUtils.defaultIfBlank(stub, null));

        fullValue = obj.getStringValue("date_of_death");
        stub = StringUtils.substringBefore(fullValue, "-");
        obj.setStringValue("year_of_death", StringUtils.defaultIfBlank(stub, null));

        fullValue = obj.getStringValue("last_name");
        stub = StringUtils.substring(fullValue, 0, 1);
        obj.setStringValue("initial", StringUtils.defaultIfBlank(stub, null));
    }
}
