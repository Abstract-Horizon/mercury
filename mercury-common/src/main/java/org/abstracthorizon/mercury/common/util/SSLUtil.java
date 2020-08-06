/*
 * Copyright (c) 2004-2007 Creative Sphere Limited.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   Creative Sphere - initial API and implementation
 *
 */
package org.abstracthorizon.mercury.common.util;

import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;


/**
 * Utility class for SSL connections
 *
 * @author Daniel Sendula
 */
public class SSLUtil {

    /**
     * This method returns SSL socket factory based on passphrase from given keystore
     * @param passphrase passphrase of keystore
     * @param keystore keystore input stream
     * @return SSL socket factory
     */
    public static SSLSocketFactory getSocketFactory(char[] passphrase, InputStream keystore) {
        SSLSocketFactory ssf = null;
        try {
            // set up key manager to do server authentication
            SSLContext ctx;
            KeyManagerFactory kmf;
            KeyStore ks;
            //char[] passphrase = "imapimap".toCharArray();

            ctx = SSLContext.getInstance("SSL");
            kmf = KeyManagerFactory.getInstance("SunX509");
            ks = KeyStore.getInstance("JKS");
            //SSLUtil.class.getClassLoader().getResourceAsStream("META-INF/keystore"), passphrase

            ks.load(keystore, passphrase);
            kmf.init(ks, passphrase);
            ctx.init(kmf.getKeyManagers(), null, null);

//            System.out.println("Provider: " + ctx.getProvider());
//            System.out.println("KeySet: " + ctx.getProvider().keySet());

            ssf = ctx.getSocketFactory();
//            String[] dcs = ssf.getDefaultCipherSuites();
//            for (int i = 0; i < dcs.length; i++) {
//                System.out.println(i + ": " + dcs[i]);
//            }
//            String[] scs = ssf.getSupportedCipherSuites();
//            for (int i = 0; i < scs.length; i++) {
//                System.out.println(i + ": " + scs[i]);
//            }
            return ssf;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * This method returns SSL server socket factory based on passphrase from given keystore
     * @param passphrase passphrase of keystore
     * @param keystore keystore input stream
     * @return SSL socket factory
     */
    public static SSLServerSocketFactory getServerSocketFactory(char[] passphrase, InputStream keystore) {
        SSLServerSocketFactory ssf = null;
        try {
            // set up key manager to do server authentication
            SSLContext ctx;
            KeyManagerFactory kmf;
            KeyStore ks;

            ctx = SSLContext.getInstance("TLS");
            kmf = KeyManagerFactory.getInstance("SunX509");
            ks = KeyStore.getInstance("JKS");

            ks.load(keystore, passphrase);
            kmf.init(ks, passphrase);
            ctx.init(kmf.getKeyManagers(), null, null);

            ssf = ctx.getServerSocketFactory();
            return ssf;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
