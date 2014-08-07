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
package org.phenotips.integration.lims247.internal;

import org.phenotips.Constants;
import org.phenotips.integration.lims247.LimsServer;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;

import java.net.URLEncoder;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpContentTooLargeException;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

/**
 * Communication via HTTP requests with a LIMS server, configured in the wiki preferences via
 * {@code PhenoTips.LimsAuthServer} objects.
 *
 * @version $Id$
 * @since 1.0M8
 */
@Component
@Singleton
public class DefaultLimsServer implements LimsServer
{
    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** HTTP client used for communicating with the LIMS server. */
    private final HttpClient client = new HttpClient(new MultiThreadedHttpConnectionManager());

    /** Provides access to the current context. */
    @Inject
    private Execution execution;

    @Override
    public boolean checkToken(String token, String username, String pn)
    {
        PostMethod method = null;
        try {
            String checkURL = getTokenCheckURL(pn, getXContext());
            if (StringUtils.isNotBlank(checkURL)) {
                method = new PostMethod(checkURL);
                String body = String.format("%s=%s&%s=%s",
                    USERNAME_KEY, URLEncoder.encode(username, XWiki.DEFAULT_ENCODING),
                    TOKEN_KEY, URLEncoder.encode(token, XWiki.DEFAULT_ENCODING));
                method.setRequestEntity(new StringRequestEntity(body, PostMethod.FORM_URL_ENCODED_CONTENT_TYPE,
                    XWiki.DEFAULT_ENCODING));
                this.client.executeMethod(method);
                String response;
                try {
                    response = method.getResponseBodyAsString(128);
                } catch (HttpContentTooLargeException ex) {
                    response = method.getResponseBodyAsString();
                    this.logger.warn("LIMS token check returned wrong response: {} - [{}]", ex.getMessage(), response);
                }
                JSONObject responseJSON = (JSONObject) JSONSerializer.toJSON(response);
                boolean success = responseJSON.getBoolean("success");
                if (success) {
                    this.logger.debug("Successfully authenticated user [{}] on LIMS instance [{}] using token [{}]",
                        username, pn, token);
                    return true;
                } else {
                    this.logger.warn("Failed to authenticate user [{}] on LIMS instance [{}] using token [{}]",
                        username, pn, token);
                }
            }
        } catch (Exception ex) {
            this.logger.warn("Failed to check LIMS authentication token [{}] on server [{}]: {}", token, pn,
                ex.getMessage(), ex);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
        return false;
    }

    @Override
    public void notify(JSONObject payload, String pn)
    {
        // FIXME This should be asynchronous; reimplement once commons-httpclient 4 is released
        PostMethod method = null;
        try {
            String notificationURL = getNotificationURL(pn, getXContext());
            if (StringUtils.isNotBlank(notificationURL)) {
                method = new PostMethod(notificationURL);
                method.setRequestEntity(new StringRequestEntity(payload.toString(), "application/json",
                    XWiki.DEFAULT_ENCODING));
                this.client.executeMethod(method);
            }
        } catch (Exception ex) {
            this.logger.warn("Failed to notify LIMS server [{}] of patient update: {}", pn, ex.getMessage(), ex);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * Return the URL of the specified LIMS instance, where the authentication token can be checked.
     *
     * @param pn the LIMS instance identifier
     * @param context the current request context
     * @return the configured URL, in the format {@code http://lims.host.name/session/check_token}, or {@code null} if
     *         the LIMS instance isn't registered in the PhenoTips configuration
     * @throws XWikiException if accessing the configuration fails
     */
    private String getTokenCheckURL(String pn, XWikiContext context) throws XWikiException
    {
        String result = getBaseURL(pn, context);
        if (result != null) {
            return result + "/session/check_token";
        }
        return null;
    }

    /**
     * Return the URL of the specified LIMS instance, where the phenotype update notification should be sent.
     *
     * @param pn the LIMS instance identifier
     * @param context the current request context
     * @return the configured URL, in the format {@code http://lims.host.name/api/phenotype_updated}, or {@code null} if
     *         the LIMS instance isn't registered in the PhenoTips configuration
     * @throws XWikiException if accessing the configuration fails
     */
    private String getNotificationURL(String pn, XWikiContext context) throws XWikiException
    {
        String result = getBaseURL(pn, context);
        if (result != null) {
            return result + "/api/phenotype_updated";
        }
        return null;
    }

    /**
     * Return the base URL of the specified LIMS instance.
     *
     * @param pn the LIMS instance identifier
     * @param context the current request context
     * @return the configured URL, in the format {@code http://lims.host.name}, or {@code null} if the LIMS instance
     *         isn't registered in the PhenoTips configuration
     * @throws XWikiException if accessing the configuration fails
     */
    private String getBaseURL(String pn, XWikiContext context) throws XWikiException
    {
        XWiki xwiki = context.getWiki();
        XWikiDocument prefsDoc =
            xwiki.getDocument(new DocumentReference(xwiki.getDatabase(), "XWiki", "XWikiPreferences"), context);
        BaseObject serverConfiguration =
            prefsDoc.getXObject(new DocumentReference(xwiki.getDatabase(), Constants.CODE_SPACE, "LimsAuthServer"),
                INSTANCE_IDENTIFIER_KEY, pn);
        if (serverConfiguration != null) {
            String result = serverConfiguration.getStringValue("url");
            if (StringUtils.isBlank(result)) {
                return null;
            }

            if (!result.startsWith("http")) {
                result = "http://" + result;
            }
            return StringUtils.stripEnd(result, "/");
        }
        return null;
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
