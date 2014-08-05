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

package org.phenotips.data.shareprotocol;

import org.xwiki.stability.Unstable;

/**
 * Push protocol constants defining client HTTP POST fields and server JSON response fields.
 *
 * @version $Id$
 * @since 1.0M11
 */
@Unstable
public class ShareProtocol
{
    public static final String POST_PROTOCOL_VERSION = "1";

    // Every POST request should include the following parameters:
    public static final String CLIENT_POST_KEY_NAME_PROTOCOLVER  = "push_protocol_version";
    public static final String CLIENT_POST_KEY_NAME_ACTION       = "action";
    public static final String CLIENT_POST_KEY_NAME_USERNAME     = "username";
    // ...and either PASSWORD or USER_TOKEN:
    public static final String CLIENT_POST_KEY_NAME_PASSWORD     = "password";
    public static final String CLIENT_POST_KEY_NAME_USER_TOKEN   = "user_login_token";  // if user_token is provided password is ignored

    // possible ACTION key values are:
    public static final String CLIENT_POST_ACTIONKEY_VALUE_INFO  = "get_server_info";  // get patient data fields supported by the server and remote user groups
    public static final String CLIENT_POST_ACTIONKEY_VALUE_PUSH  = "push";             // push the patient
    public static final String CLIENT_POST_ACTIONKEY_VALUE_GETID = "get_patient_id";   // get remote ID and remote URL of the patient object with the given GUID

    // for the PUSH action the following fields must be set:
    public static final String CLIENT_POST_KEY_NAME_PATIENTJSON  = "patient_json";
    // ...and the following optional fields may be set:
    public static final String CLIENT_POST_KEY_NAME_GROUPNAME    = "groupname";    // if provided, the ownership of the newly created patient object will be transferred to this group
    public static final String CLIENT_POST_KEY_NAME_GUID         = "patient_guid"; // if provided, the existing remote patient object with the given GUID will be updated.
                                                                                   //   an error will be returned if updating is disabled or GUID is incorrect or the patient object
                                                                                   //   referenced does not belong to the given user and/or group (if provided)
    // for the GETURL action the String CLIENT_POST_KEY_NAME_GUID key must be set

    //=========================================================================

    public static final String JSON_RESPONSE_PROTOCOL_VERSION = "1";

    // every server response JSON will include the following fields:
    public static final String SERVER_JSON_KEY_NAME_PROTOCOLVER = "response_protocol_version";
    public static final String SERVER_JSON_KEY_NAME_SUCCESS     = "success";

    // in case of any failures (SERVER_JSON_KEY_NAME_SUCCESS is set to `false`) one of the
    // following keys is guaranteed to be included in response JSON with the `true` value
    // (and it is guaranteed only one of these is included):
    public static final String SERVER_JSON_KEY_NAME_ERROR_LOGINFAILED    = "login_failed";
    public static final String SERVER_JSON_KEY_NAME_ERROR_ACTIONFAILED   = "action_failed";
    public static final String SERVER_JSON_KEY_NAME_ERROR_PROTOCOLFAILED = "unsupported_post_protocol_version";

    // one of the following may be set in case SERVER_JSON_KEY_NAME_ERROR_LOGINFAILED is set:
    public static final String SERVER_JSON_KEY_NAME_ERROR_WRONGCREDENTIALS = "incorrect_credentials";     // incorrect username or password or token
    public static final String SERVER_JSON_KEY_NAME_ERROR_EXPIREDUSERTOKEN = "user_token_expired";
    public static final String SERVER_JSON_KEY_NAME_ERROR_NOUSERTOKENS     = "user_tokens_not_supported"; // user_tokens are disabled on the server (in case a token is provided)

    // one of the following may be set in case SERVER_JSON_KEY_NAME_ERROR_ACTIONFAILED is set:
    public static final String SERVER_JSON_KEY_NAME_ERROR_UNSUPPORTEDOP    = "unsupported_action";
    public static final String SERVER_JSON_KEY_NAME_ERROR_INCORRECTGROUP   = "incorrect_user_group";   // user group supplied either does not exist or user is not in the group
    public static final String SERVER_JSON_KEY_NAME_ERROR_UPDATESDISABLED  = "updates_disabled";       // in case GUID is provided in the request and updating is disabled on the server
    public static final String SERVER_JSON_KEY_NAME_ERROR_INCORRECTGUID    = "incorrect_guid";         // GUID provided in the request does not represents a patient document
    public static final String SERVER_JSON_KEY_NAME_ERROR_GUIDACCESSDENIED = "guid_access_denied";     // GUID provided in the request represents a document which is not
                                                                                                       //  authored or owned by the user provided

    // response to a GETINFO action request will include the following fields (iff successful):
    public static final String SERVER_JSON_GETINFO_KEY_NAME_USERGROUPS     = "user_groups";
    public static final String SERVER_JSON_GETINFO_KEY_NAME_ACCEPTEDFIELDS = "accepted_fields";
    public static final String SERVER_JSON_GETINFO_KEY_NAME_UPDATESENABLED = "updates_enabled";
    // (optional) ...and optionally this as well, if enabled on the server:
    public static final String SERVER_JSON_GETINFO_KEY_NAME_USERTOKEN      = "user_login_token";

    // response to a PUSH and GETID action requests will include the following fields (iff successful):
    public static final String SERVER_JSON_PUSH_KEY_NAME_PATIENTID   = "patient_id";      // ID of the patient (either updated or newly created)
    public static final String SERVER_JSON_PUSH_KEY_NAME_PATIENTURL  = "patient_url";     // URL of the patient (either updated or newly created)
    public static final String SERVER_JSON_PUSH_KEY_NAME_PATIENTGUID = "patient_guid";    // GUID of the patient object on the remote server which can be used to link to the
                                                                                          //  patient from the remote server and/or to update the patient later
}
