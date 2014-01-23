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
package org.phenotips.data.receive.internal;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.URLDecoder;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.List;
import java.util.Enumeration;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.XWikiRequest;

import net.sf.json.JSON;
import net.sf.json.JSONSerializer;
import net.sf.json.JSONObject;
import net.sf.json.JSONArray;

import org.phenotips.Constants;
import org.phenotips.configuration.RecordConfiguration;
import org.phenotips.configuration.RecordConfigurationManager;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.internal.PhenoTipsPatientData;
import org.phenotips.data.receive.ReceivePatientData;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.apache.commons.lang3.StringUtils;

import groovy.lang.Singleton;

/**
 * Default implementation for the {@link PushPatientData} component.
 * 
 * @version $Id$
 * @since 1.0M11
 */
@Component
@Singleton
public class DefaultReceivePatientData implements ReceivePatientData
{   
    /** Logging helper object. */
    @Inject
    private Logger logger;
    
    /** Provides access to the current request context. */
    @Inject
    private Execution execution;
    
    @Inject
    private PatientData patientData;    
    
    /** Used for getting the configured mapping. */
    @Inject
    private RecordConfigurationManager configurationManager;    

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;
    
    /**
     * Check that the token received in the request is valid. The expected token is configured in the
     * {@code xwiki.properties} configuration file, under the {@code phenotips.remoteAuthentication.trustedToken} key.
     * 
     * @return {@code true} if a token parameter is present in the request, and it's value matches the configuration
     */
    public boolean isTrusted()
    {
    	XWikiContext context = getXContext();
    	
        String token = context.getRequest().getParameter("token");
        
        //XWiki xwiki = context.getWiki();
        //XWikiDocument prefsDoc = xwiki.getDocument(new DocumentReference(xwiki.getDatabase(), "XWiki", "XWikiPreferences"), context);
        //prefsDoc.getXObject(new DocumentReference(xwiki.getDatabase(), Constants.CODE_SPACE, "PushPatientServer"), SERVER_CONFIG_ID_PROPERTY_NAME, serverID);
        
        String expected = "abc";
        
        this.logger.warn("Received token: [{}]", token);
        this.logger.warn("Expected token: [{}]", expected);
        return StringUtils.equals(expected, token);
    }    
        
    @Override
    public int receivePatient()
    {
    	try {    	
			XWikiContext context = getXContext();
			XWikiRequest request = context.getRequest();
			
			this.logger.warn("Push request from remote [{}]", request.getRemoteAddr());
						
			String patientJSONRaw = context.getRequest().getParameter("patientJSON");
			
			String patientJSON = URLDecoder.decode(patientJSONRaw, XWiki.DEFAULT_ENCODING);
			
			if (patientJSON == null) {
				this.logger.error("No patient data provided by {})", request.getRemoteAddr());
				return -1;				
			}
			    	
			this.logger.warn("ENCODED JSON: <<{}>>", patientJSONRaw);			
			this.logger.warn("JSON: <<{}>>", patientJSON);
			
			JSONObject patient = JSONObject.fromObject(patientJSON);
	    	
	    	Patient result = this.patientData.createNewPatient(patient);
				
		    if (result == null) {
		    	this.logger.error("Can not create the patient specified by {}", patientJSON);
		    	return -1;
		    }
			
		    this.logger.warn("Imported successfully");	    
    	} catch (Exception ex) {
    		this.logger.error("Error importing patient [{}] {}", ex.getMessage(), ex);
    		return -1;
    	}
	    
		return 0;
    }
    
    @Override
    public String getRecordFieldConfig()
    {
    	XWikiContext context = getXContext();
    	XWikiRequest request = context.getRequest();
    	
    	this.logger.warn("Config request from remote [{}, {}]", request.getRemoteAddr(), request.getRemoteHost());
    	
    	RecordConfiguration patientConfig = this.configurationManager.getActiveConfiguration();
    	
    	List<String> acceptedFields = patientConfig.getEnabledNonIdentifiableFieldNames();
    	
    	JSON responseJSON = JSONSerializer.toJSON(acceptedFields);
    	
    	String response = responseJSON.toString();
    	
    	this.logger.warn("Config response:  [{}]", response);
    	
    	return response;
    }
    
    /**
     * Helper method for obtaining a valid xcontext from the execution context.
     * 
     * @return the current request context
     */
    private XWikiContext getXContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
    }    
}
