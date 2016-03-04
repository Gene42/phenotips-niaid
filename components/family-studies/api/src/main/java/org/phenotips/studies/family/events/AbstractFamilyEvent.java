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
package org.phenotips.studies.family.events;

import org.phenotips.studies.family.Family;

import org.xwiki.users.User;

import org.apache.commons.lang3.StringUtils;

/**
 * Base class for implementing {@link FamilyEvent}.
 *
 * @version $Id$
 * @since 1.3M1
 */
public abstract class AbstractFamilyEvent implements FamilyEvent
{
    /** The type of this event. */
    protected final String eventType;

    /** The affected family. */
    protected final Family family;

    /** The user performing this action. */
    protected final User author;

    /**
     * Constructor initializing the required fields.
     *
     * @param eventType the type of this event
     * @param family the affected family
     * @param author the user performing this action
     */
    protected AbstractFamilyEvent(String eventType, Family family, User author)
    {
        this.eventType = eventType;
        this.family = family;
        this.author = author;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        if (otherEvent instanceof FamilyEvent) {
            FamilyEvent otherFamilyEvent = (FamilyEvent) otherEvent;
            if (!StringUtils.equals(otherFamilyEvent.getEventType(), this.eventType)) {
                return false;
            }
            if (this.family == null) {
                return false;
            }
            return this.family.equals(otherFamilyEvent.getFamily());
        }
        return false;
    }

    @Override
    public String getEventType()
    {
        return this.eventType;
    }

    @Override
    public Family getFamily()
    {
        return this.family;
    }

    @Override
    public User getAuthor()
    {
        return this.author;
    }
}
