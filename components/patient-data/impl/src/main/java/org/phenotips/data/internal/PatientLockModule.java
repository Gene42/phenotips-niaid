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
package org.phenotips.data.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.translation.TranslationManager;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.locks.DocumentLock;
import org.xwiki.locks.LockModule;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;

/**
 * Prevents editing of the patient doc when the patient doc itself is locked.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Component
@Singleton
public class PatientLockModule implements LockModule
{

    @Inject
    private Execution execution;

    @Inject
    private UserManager userManager;

    @Inject
    private PatientRepository patientRepository;

    @Inject
    private TranslationManager tm;

    @Inject
    private Logger logger;

    @Override
    public int getPriority()
    {
        return 400;
    }

    @Override
    public DocumentLock getLock(DocumentReference doc)
    {
        XWikiContext context = (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
        XWikiDocument xdoc;

        try {
            xdoc = context.getWiki().getDocument(doc, context);
            if (xdoc == null) {
                return null;
            }
            String documentId = xdoc.getDocumentReference().getName();
            Patient patient = this.patientRepository.getPatientById(documentId);
            if (patient == null) {
                return null;
            }

            XWikiLock xlock = xdoc.getLock(context);
            User user = this.userManager.getUser(xlock.getUserName());
            if (xlock != null && !user.getId().equals(this.userManager.getCurrentUser().getId())) {
                Set<String> actions = Collections.singleton("edit");
                return new DocumentLock(user, xlock.getDate(), this.tm.translate("patient.locks.patientInUse",
                    user.getName()), actions, false);
            }
        } catch (XWikiException e) {
            this.logger.error("Failed to access the document lock: {}", e.getMessage(), e);
        }
        return null;
    }
}
