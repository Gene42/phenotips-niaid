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
package org.phenotips.studies.family.internal;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

/**
 * Passed around to preserve important error information. Holds onto a status (modelled after HTTP statuses), a message,
 * and an error type.
 *
 * @version $Id$
 * @since 1.2RC1
 */
public class StatusResponse
{
    /** Status codes modelled after the standard HTTP status codes. */
    public int statusCode = 200;

    /** Should contain a message if there has been an error. */
    public String message = "";

    /** Should containt the type of error, if an error occured. */
    public String errorType = "";

    /**
     * Formats itself as a response to a processing request.
     *
     * @return JSON with an 'error' field which is true/false; true if an error has occured
     */
    public JSON asProcessing()
    {
        boolean isError = this.statusCode != 200;
        JSONObject json = baseErrorJson();
        json.put("error", isError);
        return json;
    }

    /**
     * Formats itself as a response to a validation request.
     *
     * @return JSON with a 'validLink' field which is true if no errors have occurred and the validation process was
     *         successful
     */
    public JSON asVerification()
    {
        boolean valid = this.statusCode == 200;
        JSONObject json = baseErrorJson();
        json.put("validLink", valid);
        return json;
    }

    private JSONObject baseErrorJson()
    {
        JSONObject json = new JSONObject();
        json.put("errorMessage", this.message);
        json.put("errorType", this.errorType);
        return json;
    }
}
