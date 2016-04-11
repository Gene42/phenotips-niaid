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
package org.phenotips.data.permissions.script;

import org.phenotips.data.Patient;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.data.permissions.Visibility;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * @version $Id$
 */
@Component
@Named("permissions")
@Singleton
public class PermissionsManagerScriptService implements ScriptService
{
    @Inject
    private PermissionsManager manager;

    public Collection<Visibility> listVisibilityOptions()
    {
        return this.manager.listVisibilityOptions();
    }

    public Visibility resolveVisibility(String name)
    {
        return this.manager.resolveVisibility(name);
    }

    public Collection<AccessLevel> listAccessLevels()
    {
        return this.manager.listAccessLevels();
    }

    public AccessLevel resolveAccessLevel(String name)
    {
        return this.manager.resolveAccessLevel(name);
    }

    public PatientAccess getPatientAccess(Patient targetPatient)
    {
        return new SecurePatientAccess(this.manager.getPatientAccess(targetPatient), this.manager);
    }
}
