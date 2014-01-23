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
package org.phenotips.data.push.internal;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Named;

import net.sf.json.JSONObject;
import net.sf.json.JSONArray;
import net.sf.json.JSONSerializer;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang3.StringUtils;
import org.phenotips.Constants;
import org.phenotips.data.Patient;
import org.phenotips.data.internal.PhenoTipsPatient;
import org.phenotips.data.push.PushPatientData;
import org.phenotips.configuration.RecordConfiguration;
import org.phenotips.configuration.RecordConfigurationManager;
import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;

import groovy.lang.Singleton;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation for the {@link PushPatientData} component.
 * 
 * @version $Id$
 * @since 1.0M11
 */
@Component
@Singleton
@Named("pushPatients")  // TODO: remove with EvenListener
public class DefaultPushPatientData implements PushPatientData, EventListener
{
    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Provides access to the current request context. */
    @Inject
    private Execution execution;
    
    /** Used for getting the configured mapping. */
    @Inject
    private RecordConfigurationManager configurationManager;    

    /** HTTP client used for communicating with the remote server. */
    private final HttpClient client = new HttpClient(new MultiThreadedHttpConnectionManager());    
    
    /** Destination page. */
    private static final String PATIENT_DATA_SHARING_PAGE = "/bin/receivePatientData";    

    /** POST method Action key name. */
    private static final String ACTION_KEY = "action";
    
    /** POST method Token key name. */
    private static final String TOKEN_KEY = "token";

    /** POST method Patient JSON key name. */
    private static final String PATIENT_KEY = "patientJSON";
    
    /** Push action name. */
    private static final String SUBMIT_ACTION = "push";    
    
    /** Query server field configuration action name. */
    private static final String QUERY_CONFIG_ACTION = "getconfig";
    
    /** getconfig's action's JSON key containing the list of record fields accepted by the server */
    private static final String JSON_GETCONFIG_FIELDLIST_KEY = "fields";    
    
    /** Server configuration ID property name within the PushPatientServer class. */
    private static final String SERVER_CONFIG_ID_PROPERTY_NAME = "pn";    
    
   
    //===========================================================================================
    @Override
    public String getName()
    {
        return "sharePatients";
    }

    @Override
    public List<Event> getEvents()
    {
    	this.logger.warn("[GET EVENTS] DefaultPushPatientDat");
        return Arrays
            .<Event> asList(new DocumentCreatedEvent(), new DocumentUpdatedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        this.logger.warn("[ON EVENT] DefaultPushPatientData");
        
        XWikiDocument doc = (XWikiDocument) source;
        
        if (!isPatient(doc)) {
        	this.logger.warn("[HANDLE] not a patient");
            return;
        }
        
        this.logger.warn("Pushing updated document [{}]", doc.getDocumentReference());
        
        Patient patient = new PhenoTipsPatient(doc);
        
        XWikiContext context = getXContext();
        List<BaseObject> servers = getRegisteredServers(context);
        if (servers != null && !servers.isEmpty()) {
            for (BaseObject serverConfiguration : servers) {
            	this.logger.warn("   ...pushing to: {} [{}]", serverConfiguration.getStringValue(SERVER_CONFIG_ID_PROPERTY_NAME), serverConfiguration.getStringValue("url"));
            	List<Patient> toSendList = new LinkedList<Patient>();
            	toSendList.add(patient);
                sendPatient(toSendList, serverConfiguration.getStringValue(SERVER_CONFIG_ID_PROPERTY_NAME));                    
            }
        }
    }
    
    /**
     * Check if the modified document is a patient record.
     * 
     * @param doc the modified document
     * @return {@code true} if the document contains a PatientClass object and a non-empty external identifier,
     *         {@code false} otherwise
     */
    private boolean isPatient(XWikiDocument doc)
    {
        BaseObject o = doc.getXObject(Patient.CLASS_REFERENCE);
        return (o != null && !StringUtils.equals("PatientTemplate", doc.getDocumentReference().getName()));
    }    
    
    /**
     * Get all the trusted remote instances where data should be sent that are configured in the current instance.
     * 
     * @param context the current request object
     * @return a list of {@link BaseObject XObjects} with LIMS server configurations, may be {@code null}
     */
    private List<BaseObject> getRegisteredServers(XWikiContext context)
    {    	
        try {
            XWiki xwiki = context.getWiki();
            XWikiDocument prefsDoc =
                xwiki.getDocument(new DocumentReference(xwiki.getDatabase(), "XWiki", "XWikiPreferences"), context);
            return prefsDoc
                .getXObjects(new DocumentReference(xwiki.getDatabase(), Constants.CODE_SPACE, "PushPatientServer"));
        } catch (XWikiException ex) {
        	this.logger.error("Failed to get server info: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }
    //===========================================================================================    
       
    /**
     * Helper method for obtaining a valid xcontext from the execution context.
     * 
     * @return the current request context
     */
    private XWikiContext getXContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
    }
    
    /**
     * Return the the URL of the specified remote PhenoTips instance.
     * 
     * @param serverConfiguration the XObject holding the remote server configuration
     * @return the configured URL, in the format {@code http://remote.host.name/bin/}, or {@code null} if the
     *         configuration isn't valid
     */
    private String getBaseURL(BaseObject serverConfiguration)
    {
        if (serverConfiguration != null) {
            String result = serverConfiguration.getStringValue("url");
            if (StringUtils.isBlank(result)) {
                return null;
            }
            if (!result.startsWith("http")) {
                result = "http://" + result;
            }
            return StringUtils.stripEnd(result, "/") + PATIENT_DATA_SHARING_PAGE;
        }
        return null;
    }
           
    /**
     * Return the token given for the specified remote PhenoTips instance.
     *
     * @param serverConfiguration the XObject holding the remote server configuration
     * @return the token, as a free-form string of characters      
     */    
    private String getToken(BaseObject serverConfiguration)
    {
        if (serverConfiguration != null) {
            return serverConfiguration.getStringValue("token");
        }
        return null;
    }  
    
    private PostMethod getPostMethodWithActionAndToken(BaseObject serverConfiguration, String actionName)
    {
    	String submitURL = getBaseURL(serverConfiguration);        	
        if (submitURL == null) return null;
        
        this.logger.warn("SUBMIT URL: {}", submitURL);
        
        PostMethod method = new PostMethod(submitURL);
        
        method.addParameter("xpage",    "plain");
        method.addParameter(ACTION_KEY, actionName);
        try {
        	method.addParameter(TOKEN_KEY, URLEncoder.encode(getToken(serverConfiguration), XWiki.DEFAULT_ENCODING));
        } catch (Exception ex) {
            this.logger.error("Failed to encode token: {}", ex.getMessage());
        }

        return method;
    }
    
    /**
     * Get the push server configuration given its name/ID.
     * 
     * @param serverID name/id of the server
     * @param context the current request object 
     * @return {@link BaseObject XObjects} with server configuration, may be {@code null}
     */
    private BaseObject getPushServerConfiguration(String serverID, XWikiContext context)
    {    	
        try {
            XWiki xwiki = context.getWiki();
            XWikiDocument prefsDoc = xwiki.getDocument(new DocumentReference(xwiki.getDatabase(), "XWiki", "XWikiPreferences"), context);
            return prefsDoc.getXObject(new DocumentReference(xwiki.getDatabase(), Constants.CODE_SPACE, "PushPatientServer"), SERVER_CONFIG_ID_PROPERTY_NAME, serverID);
        } catch (XWikiException ex) {
        	this.logger.warn("Failed to get server info: [{}] {}", ex.getMessage(), ex);
            return null;
        }
    }
    
    /**
     * Send the changed document to a remote PhenoTips instance.
     * 
     * @param doc the serialized document to send
     * @param serverConfiguration the XObject holding the remote server configuration
     */
    private void submitData(String data, BaseObject serverConfiguration)
    {
        // FIXME This should be asynchronous; reimplement!
        PostMethod method = null;
        try {
            this.logger.warn("Pushing updated document to [{}]", serverConfiguration.getStringValue("url"));                
            method = getPostMethodWithActionAndToken(serverConfiguration, SUBMIT_ACTION);
            method.addParameter(PATIENT_KEY, URLEncoder.encode(data, XWiki.DEFAULT_ENCODING));            
            this.client.executeMethod(method);
        } catch (Exception ex) {
            this.logger.error("Failed to push patient to {}: [{}] {}", serverConfiguration.getStringValue("url"), ex.getMessage(), ex);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }             
    
    private Set<String> getExportFieldsGivenServerFields(Set<String> serverFields)
    {
    	RecordConfiguration patientConfig = this.configurationManager.getActiveConfiguration();
    	
    	Set<String> commonEnabledNonPersonalFields = new TreeSet<String>(patientConfig.getEnabledNonIdentifiableFieldNames());
    	
    	if (serverFields != null)
    		commonEnabledNonPersonalFields.retainAll(serverFields);
    	    	    	
    	//for (String field : commonEnabledNonPersonalFields) {
        //	this.logger.warn("   ...common field: {}", field);                    
        //}    	
    	
    	return commonEnabledNonPersonalFields;
    }
    
    private Set<String> getAcceptedFieldsFromPushServer(BaseObject serverConfiguration)
    {    	
    	PostMethod method = null;    	
        try {
            method = getPostMethodWithActionAndToken(serverConfiguration, QUERY_CONFIG_ACTION);
            if (method == null) return null;
            
            //this.logger.warn("Submit method info1: {} + {}", method.getURI().toString(), method.getPath());
            
            int returnCode = this.client.executeMethod(method);
                        
            this.logger.warn("Submit method return code: {}", returnCode);
            
            String response = method.getResponseBodyAsString();
            
            this.logger.warn("RESPONSE FROM SERVER: {}", response);
            
            JSONObject responseJSON = (JSONObject)JSONSerializer.toJSON(response);
            JSONArray fieldListJSON = responseJSON.getJSONArray(JSON_GETCONFIG_FIELDLIST_KEY);
            
            Set<String> serverAcceptedFields = new TreeSet<String>();
            
            for(Object field : fieldListJSON) {
                serverAcceptedFields.add(field.toString());
            }            
            
            return serverAcceptedFields;      
        } catch (Exception ex) {
            this.logger.error("Failed to get server fields - [{}] {}", ex.getMessage(), ex);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }    	    	
    	return null;
    }
    
    @Override
    public int sendPatient(List<Patient> patientList, String remoteServerIdentifier)
    {
    	this.logger.warn("===> Sending to server: {}", remoteServerIdentifier);
    	
    	XWikiContext context = getXContext();
    	
    	BaseObject serverConfiguration = this.getPushServerConfiguration(remoteServerIdentifier, context);
    	
    	Set<String> exportFields = this.getExportFieldsGivenServerFields(getAcceptedFieldsFromPushServer(serverConfiguration));
        
        for (Patient patient : patientList) {
        	String jsonPayload = patient.toJSON(exportFields).toString(); 

            this.logger.warn("===> Patient\\Document {} as JSON: {}", patient.getDocument().getName(), jsonPayload);
            
        	this.submitData(jsonPayload, serverConfiguration);
        }
    	
        return -1;
    }
}
