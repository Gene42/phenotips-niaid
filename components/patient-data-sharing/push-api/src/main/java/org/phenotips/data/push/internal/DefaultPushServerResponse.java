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

import org.phenotips.data.push.PushServerResponse;
import org.phenotips.data.shareprotocol.ShareProtocol;

import net.sf.json.JSONObject;

public class DefaultPushServerResponse implements PushServerResponse
{
    protected final JSONObject response;

    DefaultPushServerResponse(JSONObject serverResponse)
    {
        this.response = serverResponse;
    }

    // TODO: come up with a better way to generate server responses of any kind locally
    public static JSONObject generateIncorrectCredentialsJSON()
    {
        JSONObject response = new JSONObject();
        response.element(ShareProtocol.SERVER_JSON_KEY_NAME_SUCCESS, false);
        response.element(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_LOGINFAILED, true);
        response.element(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_WRONGCREDENTIALS, true);
        return response;
    }

    public static JSONObject generateActionFailedJSON()
    {
        JSONObject response = new JSONObject();
        response.element(ShareProtocol.SERVER_JSON_KEY_NAME_SUCCESS, false);
        response.element(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_ACTIONFAILED, true);
        return response;
    }

    protected boolean hasKeySetToTrue(String key)
    {
        return this.response.containsKey(key) && this.response.getBoolean(key);
    }

    protected String valueOrNull(String key)
    {
        if (!this.response.containsKey(key)) {
            return null;
        }

        return this.response.getString(key);
    }

    @Override
    public boolean isSuccessful()
    {
        return hasKeySetToTrue(ShareProtocol.SERVER_JSON_KEY_NAME_SUCCESS);
    }

    @Override
    public boolean isIncorrectProtocolVersion()
    {
        return hasKeySetToTrue(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_PROTOCOLFAILED) ||
               !this.response.containsKey(ShareProtocol.SERVER_JSON_KEY_NAME_PROTOCOLVER);
    }

    @Override
    public boolean isLoginFailed()
    {
        return hasKeySetToTrue(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_LOGINFAILED);
    }

    @Override
    public boolean isActionFailed()
    {
        return hasKeySetToTrue(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_ACTIONFAILED);
    }

    @Override
    public boolean isLoginFailed_knownReason()
    {
        return (isLoginFailed_UnauthorizedServer() || isLoginFailed_IncorrectCredentials() ||
            isLoginFailed_TokensNotSuported() || isLoginFailed_UserTokenExpired());
    }

    @Override
    public boolean isLoginFailed_UnauthorizedServer()
    {
        return isLoginFailed() && hasKeySetToTrue(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_UNTRUSTEDSERVER);
    }

    @Override
    public boolean isLoginFailed_IncorrectCredentials()
    {
        return isLoginFailed() && hasKeySetToTrue(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_WRONGCREDENTIALS);
    }

    @Override
    public boolean isLoginFailed_UserTokenExpired()
    {
        return isLoginFailed() && hasKeySetToTrue(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_EXPIREDUSERTOKEN);
    }

    @Override
    public boolean isLoginFailed_TokensNotSuported()
    {
        return isLoginFailed() && hasKeySetToTrue(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_NOUSERTOKENS);
    }

    @Override
    public boolean isActionFailed_knownReason()
    {
        return isActionFailed_isUnknownAction();
    }

    @Override
    public boolean isActionFailed_isUnknownAction()
    {
        return isActionFailed() && hasKeySetToTrue(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_UNSUPPORTEDOP);
    }
}
