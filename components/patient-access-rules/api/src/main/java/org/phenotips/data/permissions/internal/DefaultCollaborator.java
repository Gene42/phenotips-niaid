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
package org.phenotips.data.permissions.internal;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.Collaborator;
import org.phenotips.groups.internal.UsersAndGroups;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.EntityReference;

/**
 * @version $Id$
 */
public class DefaultCollaborator implements Collaborator
{
    private final EntityReference user;

    private final AccessLevel access;

    public DefaultCollaborator(EntityReference user, AccessLevel access)
    {
        this.user = user;
        this.access = access;
    }

    @Override
    public String getType()
    {
        return this.getUsersAndGroups().getType(this.user);
    }

    @Override
    public boolean isUser()
    {
        return UsersAndGroups.USER.equals(getType());
    }

    @Override
    public boolean isGroup()
    {
        return UsersAndGroups.GROUP.equals(getType());
    }

    @Override
    public EntityReference getUser()
    {
        return this.user;
    }

    @Override
    public String getUsername()
    {
        if (this.user == null) {
            return null;
        }
        return this.user.getName();
    }

    @Override
    public AccessLevel getAccessLevel()
    {
        return this.access;
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof Collaborator)) {
            return false;
        }
        Collaborator otherCollaborator = (Collaborator) other;
        return this.user.equals(otherCollaborator.getUser()) && this.access.equals(otherCollaborator.getAccessLevel());
    }

    @Override
    public int hashCode()
    {
        return this.user.hashCode() + this.access.hashCode();
    }

    @Override
    public String toString()
    {
        return "[" + getUser() + ", " + getAccessLevel() + "]";
    }

    private UsersAndGroups getUsersAndGroups()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager().getInstance(UsersAndGroups.class);
        } catch (ComponentLookupException e) {
            // Should not happen
        }
        return null;
    }
}
