/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.data.push.script;

import org.phenotips.data.Patient;
import org.phenotips.data.push.PushPatientData;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

import javax.inject.Inject;
import javax.inject.Named;

import groovy.lang.Singleton;

/**
 * API that allows pushing patient data to a remote PhenoTips instance.
 * 
 * @version $Id$
 * @since 1.0M11
 */
@Unstable
@Component
@Named("pushPatient")
@Singleton
public class PushPatientDataScriptService implements ScriptService
{
    /** Wrapped trusted API, doing the actual work. */
    @Inject
    private PushPatientData internalService;

    public int sendPatient(Patient patient, String remoteServerIdentifier)
    {
        return this.internalService.sendPatient(patient, remoteServerIdentifier);
    }
}
