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

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.nc2.util.net.EasyX509TrustManager;

/**
 * Custom trust manager that doesn't require a trusted certificate chain, but accepts self-signed certificates that are
 * otherwise in their valid period.
 * 
 * @version $Id$
 */
public class SelfSignedValidatingX509TrustManager implements X509TrustManager
{
    /** Logging helper object. */
    private static Logger logger = LoggerFactory.getLogger(EasyX509TrustManager.class);

    /** Default trust manager used for validating certificates that are signed. */
    private X509TrustManager standardTrustManager;

    /**
     * Basic constructor.
     * 
     * @param keystore the keystore holding custom root certificates; may be {@code null}
     * @throws NoSuchAlgorithmException if no implementations handling X509 are available
     * @throws KeyStoreException if the specified keystore cannot be opened successfully
     */
    public SelfSignedValidatingX509TrustManager(KeyStore keystore) throws NoSuchAlgorithmException, KeyStoreException
    {
        super();
        TrustManagerFactory factory = TrustManagerFactory.getInstance("SunX509");
        factory.init(keystore);
        TrustManager[] trustmanagers = factory.getTrustManagers();
        if (trustmanagers.length == 0) {
            throw new NoSuchAlgorithmException("SunX509 trust manager not supported");
        }
        this.standardTrustManager = (X509TrustManager) trustmanagers[0];
    }

    @Override
    public void checkClientTrusted(X509Certificate[] certificates, String authType) throws CertificateException
    {
        this.standardTrustManager.checkClientTrusted(certificates, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] certificates, String authType) throws CertificateException
    {
        if ((certificates != null) && logger.isDebugEnabled()) {
            logger.debug("Server certificate chain:");
            for (int i = 0; i < certificates.length; i++) {
                logger.debug("X509Certificate[" + i + "]=" + certificates[i]);
            }
        }
        if ((certificates != null) && (certificates.length == 1)) {
            // Self signed certificate, just check validity
            X509Certificate certificate = certificates[0];
            try {
                certificate.checkValidity();
            } catch (CertificateException e) {
                logger.error(e.toString());
            }
            return;
        } else {
            this.standardTrustManager.checkServerTrusted(certificates, authType);
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers()
    {
        return this.standardTrustManager.getAcceptedIssuers();
    }

}
