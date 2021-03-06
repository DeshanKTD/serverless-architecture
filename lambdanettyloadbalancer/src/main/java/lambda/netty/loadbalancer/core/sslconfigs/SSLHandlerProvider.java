/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package lambda.netty.loadbalancer.core.sslconfigs;

import io.netty.handler.ssl.SslHandler;
import lambda.netty.loadbalancer.core.ConfigConstants;
import lambda.netty.loadbalancer.core.launch.Launcher;
import org.apache.log4j.Logger;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;

public class SSLHandlerProvider {
    private static final Logger logger = Logger.getLogger(SSLHandlerProvider.class);

    private static final String ALGORITHM_SUN_X509 = "SunX509";
    private static final String ALGORITHM = "ssl.KeyManagerFactory.algorithm";
    ;
    private static final String PROTOCOL = Launcher.getString(ConfigConstants.CONFIG_TRANSPORT_SSL_CONFIG_PROTOCOL);
    private static final String KEYSTORE = Launcher.getString(ConfigConstants.CONFIG_TRANSPORT_SSL_CONFIG_KEYSTORE_FILE);
    private static final String KEYSTORE_TYPE = Launcher.getString(ConfigConstants.CONFIG_TRANSPORT_SSL_CONFIG_KEYSTORE_TYPE);
    private static final String KEYSTORE_PASSWORD = Launcher.getString(ConfigConstants.CONFIG_TRANSPORT_SSL_CONFIG_KEYSTORE_PASSWORD);
    private static final String CERT_PASSWORD = Launcher.getString(ConfigConstants.CONFIG_TRANSPORT_SSL_CONFIG_CERT_PASSWORD);
    private static SSLContext serverSSLContext = null;


    private SSLHandlerProvider() {
    }

    public static SslHandler getSSLHandler() {
        SSLEngine sslEngine = null;
        if (serverSSLContext == null) {
            logger.error("Server SSL context is null");
            System.exit(-1);
        } else {
            sslEngine = serverSSLContext.createSSLEngine();
            sslEngine.setUseClientMode(false);
            sslEngine.setNeedClientAuth(false);

        }
        return new SslHandler(sslEngine);
    }

    public static void initSSLContext() {

        logger.info("Initiating SSL context");
        String algorithm = Security.getProperty(ALGORITHM);
        if (algorithm == null) {
            algorithm = ALGORITHM_SUN_X509;
        }
        KeyStore ks = null;
        InputStream inputStream = null;
        try {
            inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(KEYSTORE);
            ks = KeyStore.getInstance(KEYSTORE_TYPE);
            ks.load(inputStream, KEYSTORE_PASSWORD.toCharArray());
        } catch (IOException e) {
            logger.error("Cannot load the keystore file", e);
        } catch (CertificateException e) {
            logger.error("Cannot get the certificate", e);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Somthing wrong with the SSL algorithm", e);
        } catch (KeyStoreException e) {
            logger.error("Cannot initialize keystore", e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                logger.error("Cannot close keystore file stream ", e);
            }
        }
        try {

            // Set up key manager factory to use our key store
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
            kmf.init(ks, CERT_PASSWORD.toCharArray());
            KeyManager[] keyManagers = kmf.getKeyManagers();
            TrustManager[] trustManagers = null;

            serverSSLContext = SSLContext.getInstance(PROTOCOL);
            serverSSLContext.init(keyManagers, trustManagers, null);


        } catch (Exception e) {
            logger.error("Failed to initialize the server-side SSLContext", e);
        }


    }


}
