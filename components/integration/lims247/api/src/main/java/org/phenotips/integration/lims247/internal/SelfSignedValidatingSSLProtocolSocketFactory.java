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

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.commons.httpclient.HttpClientError;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ControllerThreadSocketFactory;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation for the HTTPClient interface responsible for creating HTTPS connections,
 * {@link ProtocolSocketFactory}, which accepts self signed certificates by using a custom certificate validator.
 * 
 * @version $Id$
 */
public class SelfSignedValidatingSSLProtocolSocketFactory implements ProtocolSocketFactory
{

    /** Logging helper object. */
    private static Logger logger = LoggerFactory.getLogger(SelfSignedValidatingSSLProtocolSocketFactory.class);

    /** The context used for this factory. */
    private SSLContext sslcontext;

    /**
     * Basic constructor.
     */
    public SelfSignedValidatingSSLProtocolSocketFactory()
    {
        super();
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort) throws IOException
    {

        return getSSLContext().getSocketFactory().createSocket(host, port, clientHost, clientPort);
    }

    @Override
    public Socket createSocket(final String host, final int port, final InetAddress localAddress, final int localPort,
        final HttpConnectionParams params) throws IOException
    {
        if (params == null) {
            throw new IllegalArgumentException("Parameters may not be null");
        }
        int timeout = params.getConnectionTimeout();
        if (timeout == 0) {
            return createSocket(host, port, localAddress, localPort);
        } else {
            // To be eventually deprecated when migrated to Java 1.4 or above
            return ControllerThreadSocketFactory.createSocket(this, host, port, localAddress, localPort, timeout);
        }
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException
    {
        return getSSLContext().getSocketFactory().createSocket(host, port);
    }

    /**
     * Getter offering lazy initialization of the {@link #sslcontext} field.
     * 
     * @return a valid, custom SSL context
     */
    private SSLContext getSSLContext()
    {
        if (this.sslcontext == null) {
            this.sslcontext = createEasySSLContext();
        }
        return this.sslcontext;
    }

    /**
     * Creates a custom SSL context, with a specific trust manager that doesn't actually check certificate signature
     * chains.
     * 
     * @return a custom SSL context
     */
    private static SSLContext createEasySSLContext()
    {
        try {
            SSLContext context = SSLContext.getInstance("SSL");
            context.init(null, new TrustManager[] {new SelfSignedValidatingX509TrustManager(null)}, null);
            return context;
        } catch (Exception e) {
            logger.error("createEasySSLContext", e);
            throw new HttpClientError(e.toString());
        }
    }
}
