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
package org.phenotips.metabolites.listener;

import org.phenotips.data.events.PatientDeletedEvent;
import org.phenotips.metabolites.TestReport;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.event.Event;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;

/**
 * Listens for changes in patient documents to check for new or changed links between patients. Creates family
 * pages and
 * keeps them updated.
 */
@Component
@Singleton
public class PatientDeleteListener implements org.xwiki.observation.EventListener
{
    @Inject
    private HibernateSessionFactory xwikiSessionFactory;

    @Override
    public String getName()
    {
        return "patientdeletelistenermetabolites";
    }

    public List<Event> getEvents()
    {
        return Arrays.<Event>asList(new PatientDeletedEvent());
    }

    /** Receives a {@link org.phenotips.data.Patient} and {@link org.xwiki.users.User} objects. */
    @Override
    public void onEvent(Event event, Object p, Object u)
    {
        deleteTestReport(p.toString());
    }

    private List<TestReport> deleteTestReport(String patientId)
    {
        // fixme
        patientId = patientId.substring(patientId.indexOf(".P") + 1);
        Session session = getSession();
        Transaction transaction;
        List<TestReport> reports = new LinkedList<>();
        try {
            transaction = session.beginTransaction();
            reports = session.createCriteria(TestReport.class).add(Restrictions.eq("patientId", patientId)).list();
            for (TestReport report : reports) {
                session.delete(report);
            }
            transaction.commit();
        } catch (Exception ex) {
            // nothing to do
        } finally {
            session.close();
        }
        return reports;
    }

    private Session getSession()
    {
        return this.xwikiSessionFactory.getSessionFactory().openSession();
    }
}
