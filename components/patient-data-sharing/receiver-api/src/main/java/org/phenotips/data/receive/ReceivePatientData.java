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

package org.phenotips.data.receive;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * API that allows receiving patient data from a remote PhenoTips instance.
 * 
 * @version $Id$
 * @since 1.0M11
 */
@Unstable
@Role
public interface ReceivePatientData
{    	
    /**
     * Check that the token received in the request is valid. The expected token is configured in the
     * {@code xwiki.properties} configuration file, under the {@code phenotips.remoteAuthentication.trustedToken} key.
     * 
     * @return {@code true} if a token parameter is present in the request, and it's value matches the configuration
     */
    boolean isTrusted();
    
    int receivePatient();
    
    String getRecordFieldConfig();
}
