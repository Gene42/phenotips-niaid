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
package org.phenotips.data.push.internal;

import net.sf.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DefaultPushServerSendPatientResponseTest {

    private DefaultPushServerSendPatientResponse pushResponse;

    @Before
    public void setUp()
    {
        JSONObject jsonObject = new JSONObject();
        pushResponse = new DefaultPushServerSendPatientResponse(jsonObject);
    }

    @Test
    public void isActionFailed_incorrectGroupVerifiesTrueKey()
    {
        pushResponse.response.accumulate("incorrect_user_group", true);
        Assert.assertTrue(pushResponse.isActionFailed_incorrectGroup());
        pushResponse.response.clear();
        pushResponse.response.accumulate("incorrect_user_group", false);
        Assert.assertFalse(pushResponse.isActionFailed_incorrectGroup());
    }

    @Test
    public void isActionFailed_UpdateDisabledVerifiesTrueKey()
    {
        pushResponse.response.accumulate("updates_disabled", true);
        Assert.assertTrue(pushResponse.isActionFailed_UpdatesDisabled());
        pushResponse.response.clear();
        pushResponse.response.accumulate("updates_disabled", false);
        Assert.assertFalse(pushResponse.isActionFailed_IncorrectGUID());
    }

    @Test
    public void isActionFailed_IncorrectGUIDVerifiesTrueKey() {
        pushResponse.response.accumulate("incorrect_guid", true);
        Assert.assertTrue(pushResponse.isActionFailed_IncorrectGUID());
        pushResponse.response.clear();
        pushResponse.response.accumulate("incorrect_guid", false);
        Assert.assertFalse(pushResponse.isActionFailed_IncorrectGUID());
    }

    @Test
    public void isActionFailed_GUIDAccessDeniedVerifiesTrueKey() {
        pushResponse.response.accumulate("guid_access_denied", true);
        Assert.assertTrue(pushResponse.isActionFailed_GUIDAccessDenied());
        pushResponse.response.clear();
        pushResponse.response.accumulate("guid_access_denied", false);
        Assert.assertFalse(pushResponse.isActionFailed_GUIDAccessDenied());
    }

    @Test
    public void isActionFailed_knownReasonVerifiesTrueKey() {
        pushResponse.response.accumulate("guid_access_denied", true);
        Assert.assertTrue(pushResponse.isActionFailed_knownReason());
        pushResponse.response.clear();
        pushResponse.response.accumulate("guid_access_denied", false);
        Assert.assertFalse(pushResponse.isActionFailed_knownReason());
    }
}
