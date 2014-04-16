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
package org.phenotips.data.securestorage.internal;

import org.phenotips.data.securestorage.PatientPushedToInfo;
import org.phenotips.data.securestorage.SecureStorageManager;
import org.phenotips.data.securestorage.LocalLoginToken;
import org.phenotips.data.securestorage.RemoteLoginData;
import org.phenotips.data.securestorage.PatientSourceServerInfo;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;

/**
 * Default implementation using Hibernate
 *
 * @version $Id$
 * @since 1.0M10
 */
@Component
@Singleton
public class DefaultSecureStorageManager implements SecureStorageManager
{
    /** Handles persistence. */
    @Inject
    private HibernateSessionFactory sessionFactory;

    /** Logging helper object. */
    @Inject
    private Logger logger;

    @Override
    public void removeRemoteLoginData(String localUserName, String serverName)
    {
        RemoteLoginData existing = getRemoteLoginData(localUserName, serverName);
        if (existing != null) {
            Session session = this.sessionFactory.getSessionFactory().openSession();
            Transaction t = session.beginTransaction();
            t.begin();
            this.logger.warn("DEBUG: removing stored token for [{}] @ [{}]", localUserName, serverName);
            session.delete(existing);
            t.commit();
        }
    }

    @Override
    public void storeRemoteLoginData(String localUserName, String serverName, String remoteUserName, String remoteLoginToken)
    {
        RemoteLoginData existing = getRemoteLoginData(localUserName, serverName);

        Session session = this.sessionFactory.getSessionFactory().openSession();
        Transaction t = session.beginTransaction();
        t.begin();

        if (existing != null)
        {
            //this.logger.warn("DEBUG: Updating token");
            existing.setRemoteUserName(remoteUserName);
            existing.setLoginToken(remoteLoginToken);
            session.update(existing);
        }
        else
        {
            //this.logger.warn("DEBUG: Saving new token");
            session.save(new RemoteLoginData(localUserName, serverName, remoteUserName, remoteLoginToken));
        }
        t.commit();
    }

    @Override
    public void storeLocalLoginToken(String userName, String sourceServerName, String loginToken)
    {
        LocalLoginToken existing = getLocalLoginToken(userName, sourceServerName);

        Session session = this.sessionFactory.getSessionFactory().openSession();
        Transaction t = session.beginTransaction();
        t.begin();

        if (existing != null)
        {
            //this.logger.warn("DEBUG: Updating token");
            existing.setLoginToken(loginToken);
            session.update(existing);
        }
        else
        {
            //this.logger.warn("DEBUG: Saving new token: [{}]-[{}]-[{}]", userName, sourceServerName, loginToken);
            session.save(new LocalLoginToken(userName, sourceServerName, loginToken));
        }
        t.commit();
    }

    @Override
    public RemoteLoginData getRemoteLoginData(String localUserName, String serverName)
    {
        if (localUserName == null || serverName == null) {
            return null;
        }

        Session session = this.sessionFactory.getSessionFactory().openSession();
        RemoteLoginData data = (RemoteLoginData) session.createCriteria(RemoteLoginData.class)
                                                 .add( Restrictions.eq("localUserName", localUserName) )
                                                 .add( Restrictions.eq("serverName",    serverName) )
                                                 .uniqueResult();

        if (data == null) {
            this.logger.warn("DEBUG: Token not found or more than one found");
            return null;
        }

        //this.logger.warn("DEBUG: Token found");
        return data;
    }

    @Override
    public LocalLoginToken getLocalLoginToken(String userName, String sourceServerName)
    {
        if (userName == null || sourceServerName == null) {
            return null;
        }

        Session session = this.sessionFactory.getSessionFactory().openSession();
        LocalLoginToken data = (LocalLoginToken) session.createCriteria(LocalLoginToken.class)
                                                 .add( Restrictions.eq("localUserName",    userName) )
                                                 .add( Restrictions.eq("sourceServerName", sourceServerName) )
                                                 .uniqueResult();

        if (data == null) {
            this.logger.warn("DEBUG: Token not found or more than one found");
            return null;
        }

        //this.logger.warn("DEBUG: Token found");
        return data;
    }

    @Override
    public void removeAllLocalTokens(String sourceServerName)
    {
        throw new RuntimeException();
    }

    @Override
    public void storePatientSourceServerInfo(String patientGUID, String sourceServerName)
    {
        PatientSourceServerInfo existing = getPatientSourceServerInfo(patientGUID);

        if (existing != null)
        {
            this.logger.warn("DEBUG: remote server already defined");
        }
        else
        {
            Session session = this.sessionFactory.getSessionFactory().openSession();
            Transaction t = session.beginTransaction();
            t.begin();
            //this.logger.warn("DEBUG: Saving remote source sever for [{}] = [{}]", patientGUID, sourceServerName);
            session.save(new PatientSourceServerInfo(patientGUID, sourceServerName));
            t.commit();
        }
    }

    @Override
    public PatientSourceServerInfo getPatientSourceServerInfo(String patientGUID)
    {
        Session session = this.sessionFactory.getSessionFactory().openSession();
        PatientSourceServerInfo data = (PatientSourceServerInfo) session.createCriteria(PatientSourceServerInfo.class)
                                       .add( Restrictions.eq("patientGUID", patientGUID ) )
                                       .uniqueResult();

        if (data == null) {
            this.logger.warn("DEBUG: No remote source server defined");
            return null;
        }

        this.logger.warn("DEBUG: Remote source server found");
        return data;
    }

    @Override
    public void storePatientPushInfo(String localPatientID, String remoteServerName,
                                     String remotePatientGUID, String remotePatientID, String remotePatientURL)
    {
        if (localPatientID == null || remoteServerName == null) return;

        PatientPushedToInfo existing = getPatientPushInfo(localPatientID, remoteServerName);

        Session session = this.sessionFactory.getSessionFactory().openSession();
        Transaction t = session.beginTransaction();
        t.begin();

        if (existing != null)
        {
            this.logger.warn("DEBUG: Updating patient push info");
            existing.setLastPushTimeToNow();
            existing.setRemotePatientID(remotePatientID);
            existing.setRemotePatientGUID(remotePatientGUID);
            existing.setRemotePatientURL(remotePatientURL);
            session.update(existing);
        }
        else
        {
            this.logger.warn("DEBUG: Saving new patient push info [{}] -> [{}] @ [{}]", localPatientID, remotePatientURL, remoteServerName);
            session.save(new PatientPushedToInfo(localPatientID, remoteServerName,
                                                 remotePatientGUID, remotePatientID, remotePatientURL));
        }
        t.commit();
    }

    @Override
    public PatientPushedToInfo getPatientPushInfo(String localPatientID, String remoteServerName)
    {
        if (localPatientID == null || remoteServerName == null) {
            return null;
        }

        Session session = this.sessionFactory.getSessionFactory().openSession();
        PatientPushedToInfo data = (PatientPushedToInfo) session.createCriteria(PatientPushedToInfo.class)
                                                 .add( Restrictions.eq("localPatientID", localPatientID) )
                                                 .add( Restrictions.eq("remoteServerName", remoteServerName) )
                                                 .uniqueResult();

        if (data == null) {
            this.logger.warn("DEBUG: Never pushed to this server");
            return null;
        }

        this.logger.warn("DEBUG: Previous push info found");
        return data;
    }
}