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
package org.phenotips.data.receive.script;

import org.phenotips.data.receive.ReceivePatientData;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;  // TODO: remove
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

import javax.inject.Inject;
import javax.inject.Named;

import groovy.lang.Singleton;

/**
 * API that allows receiving patient data from a remote PhenoTips instance.
 * 
 * @version $Id$
 * @since 1.0M11
 */
@Unstable
@Component
@Named("receivePatientData")
@Singleton
public class ReceivePatientDataScriptService implements ScriptService, Initializable
{
    /** Wrapped trusted API, doing the actual work. */
    @Inject
    private ReceivePatientData internalService;

    /** Logging helper object. */
    @Inject
    private Logger logger;
    
    public void initialize()
    {
    	this.logger.warn("[SCRIPTSERVICE1] Initialize");
    }
    
    public boolean isTrusted()
    {
    	this.logger.warn("[SCRIPTSERVICE1] Token check");
    	return this.internalService.isTrusted();
    }
    
    public int receivePatient()
    {
    	this.logger.warn("[SCRIPTSERVICE1] Receive patient");
        return this.internalService.receivePatient();
    }
    
    public String getRecordFieldConfig()
    {
    	this.logger.warn("[SCRIPTSERVICE1] Send config");
    	return this.internalService.getRecordFieldConfig();
    }    
}
