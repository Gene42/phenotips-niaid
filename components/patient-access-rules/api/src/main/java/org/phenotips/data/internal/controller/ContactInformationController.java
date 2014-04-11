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
package org.phenotips.data.internal.controller;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.SimpleNamedData;
import org.phenotips.data.permissions.Owner;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.groups.Group;
import org.phenotips.groups.GroupManager;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import net.sf.json.JSONObject;

/**
 * Handle's the patient owner's contact information.
 *
 * @version $Id$
 * @since 1.0M11
 */
@Component(roles = { PatientDataController.class })
@Named("owner-contact")
@Singleton
public class ContactInformationController implements PatientDataController<ImmutablePair<String, String>>
{
    private static final String DATA_CONTACT = "contact";

    private static final String DATA_USER_ID = "user_id";

    private static final String DATA_EMAIL = "email";

    private static final String DATA_NAME = "name";

    private static final String DATA_INSTITUTION = "institution";

    private static final String ATTRIBUTE_INSTITUTION = "company";

    private static final String ATTRIBUTE_EMAIL_USER = DATA_EMAIL;

    private static final String ATTRIBUTE_EMAIL_GROUP = DATA_CONTACT;

    @Inject
    private Logger logger;

    @Inject
    private UserManager userManager;

    @Inject
    private GroupManager groupManager;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private PermissionsManager permissions;

    @Override
    public PatientData<ImmutablePair<String, String>> load(Patient patient)
    {
        Owner owner = this.permissions.getPatientAccess(patient).getOwner();
        List<ImmutablePair<String, String>> contactInfo = getContactInfo(owner);
        if (contactInfo == null) {
            return null;
        }
        return new SimpleNamedData<String>(DATA_CONTACT, contactInfo);
    }

    @Override
    public void save(Patient patient)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
        writeJSON(patient, json, null);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        if (selectedFieldNames != null && !selectedFieldNames.contains(getEnablingFieldName())) {
            return;
        }
        PatientData<ImmutablePair<String, String>> data = patient.getData(DATA_CONTACT);
        if (data == null || data.isEmpty()) {
            return;
        }
        JSONObject container = json.getJSONObject(DATA_CONTACT);
        if (container == null || container.isNullObject()) {
            json.put(DATA_CONTACT, new JSONObject());
            container = json.getJSONObject(DATA_CONTACT);
        }
        for (ImmutablePair<String, String> item : data) {
            container.put(item.getKey(), item.getValue());
        }
    }

    @Override
    public PatientData<ImmutablePair<String, String>> readJSON(JSONObject json)
    {
        throw new UnsupportedOperationException();
    }

    private List<ImmutablePair<String, String>> getContactInfo(Owner owner)
    {
        List<ImmutablePair<String, String>> contactInfo = new LinkedList<ImmutablePair<String, String>>();
        if (owner == null) {
            return null;
        }
        if (owner.isGroup()) {
            Group group = this.groupManager.getGroup((DocumentReference) owner.getUser());
            if (group == null) {
                return null;
            }
            populateGroupInfo(contactInfo, group);
        } else {
            User user = this.userManager.getUser(owner.getUser().toString());
            if (user == null) {
                return null;
            }
            populateUserInfo(contactInfo, user);
        }
        return contactInfo;
    }

    private void populateUserInfo(List<ImmutablePair<String, String>> contactInfo, User user)
    {
        String email = (String) user.getAttribute(ATTRIBUTE_EMAIL_USER);
        String institution = (String) user.getAttribute(ATTRIBUTE_INSTITUTION);

        addInfo(contactInfo, DATA_USER_ID, user.getUsername());
        addInfo(contactInfo, DATA_NAME, user.getName());
        addInfo(contactInfo, DATA_EMAIL, email);
        addInfo(contactInfo, DATA_INSTITUTION, institution);
    }

    private void populateGroupInfo(List<ImmutablePair<String, String>> contactInfo, Group group)
    {
        addInfo(contactInfo, DATA_NAME, group.getReference().getName());

        DocumentReference documentReference = group.getReference();
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(documentReference);
            BaseObject data = doc.getXObject(Group.CLASS_REFERENCE);
            addInfo(contactInfo, DATA_EMAIL, data.getStringValue(ATTRIBUTE_EMAIL_GROUP));
        } catch (Exception e) {
            this.logger.error("Could not find requested document");
        }
    }

    @Override
    public String getName()
    {
        return DATA_CONTACT;
    }

    /**
     * Unlike all other controllers, there is no field name controlling presence of version information in JSON output.
     * This method returns a name which can be used instead.
     * 
     * @return a name which can be included in the list of enabled fields to enable version info in JSON output
     */
    public static String getEnablingFieldName()
    {
        return DATA_CONTACT;
    }

    private void addInfo(List<ImmutablePair<String, String>> contactInfo, String key, String value)
    {
        if (StringUtils.isNotBlank(value)) {
            contactInfo.add(ImmutablePair.of(key, value));
        }
    }
}
