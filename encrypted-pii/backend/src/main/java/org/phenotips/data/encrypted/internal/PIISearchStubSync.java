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
        String fullValue = obj.getStringValue("date_of_birth");
        String stub = StringUtils.substringBefore(fullValue, DATE_SEPARATOR);
        obj.setStringValue("year_of_birth", StringUtils.defaultIfBlank(stub, null));

        fullValue = obj.getStringValue("date_of_death");
        stub = StringUtils.substringBefore(fullValue, DATE_SEPARATOR);
        obj.setStringValue("year_of_death", StringUtils.defaultIfBlank(stub, null));

        fullValue = obj.getStringValue("last_name");
        stub = StringUtils.substring(fullValue, 0, 1);
        obj.setStringValue("initial", StringUtils.defaultIfBlank(stub, null));
    }
}
