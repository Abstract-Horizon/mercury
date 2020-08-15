/*
 * Copyright (c) 2004-2019 Creative Sphere Limited.
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
package org.abstracthorizon.mercury.sync;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

import org.abstracthorizon.danube.service.server.MultiThreadServerSSLSocketService;
import org.abstracthorizon.mercury.sync.cachedir.CachedDirs;
import org.abstracthorizon.mercury.sync.client.SyncClient;
import org.abstracthorizon.mercury.sync.client.SyncClient.RemoteFile;
import org.abstracthorizon.mercury.sync.commands.SyncCommandFactory;

public class MercuryTestSyncSetup {

    private MultiThreadServerSSLSocketService service;
    private SyncConnectionHandler syncConnectionHandler;
    private SyncCommandFactory syncCommandFactory;
    private CachedDirs serverCachedDirs;
    private SyncClient syncClient;
    private int port;
    private MercuryDirSetup serverDirSetup;
    private MercuryDirSetup localDirSetup;


    public MercuryTestSyncSetup() {
    }

    public MultiThreadServerSSLSocketService getService() {
        return service;
    }

    public SyncConnectionHandler getSyncConnectionHandler() {
        return syncConnectionHandler;
    }

    public SyncCommandFactory getSyncCommandFactory() {
        return syncCommandFactory;
    }

    public CachedDirs getServerCachedDirs() {
        return serverCachedDirs;
    }

    public SyncClient getSyncClient() {
        return syncClient;
    }

    public int getPort() {
        return port;
    }

    public MercuryDirSetup getServerDirSetup() throws IOException {
        if (serverDirSetup == null) {
            serverDirSetup = new MercuryDirSetup("remote");
            serverDirSetup.create();
        }
        return serverDirSetup;
    }

    public MercuryDirSetup getLocalDirSetup() throws IOException {
        if (localDirSetup == null) {
            localDirSetup = new MercuryDirSetup("local");
            localDirSetup.create();
        }
        return localDirSetup;
    }

    public File getServerMailboxes() throws IOException {
        return getServerDirSetup().getMailboxes();
    }

    public File getLocalMailboxes() throws IOException {
        return getLocalDirSetup().getMailboxes();
    }

    public void create() throws IOException {
        ServerSocket socket = new ServerSocket(0);
        port = socket.getLocalPort();
        socket.close();

        service = new MultiThreadServerSSLSocketService();
        service.setName("sync");
        service.setPort(port);
        service.setAddress("localhost");
        service.setServerSocketTimeout(1000);
        service.setNewSocketTimeout(60000);
        service.setKeyStoreURL(getClass().getResource("/ssl.keystore"));
        service.setKeyStorePassword("password1234");
        service.setTrustStoreURL(getClass().getResource("/mercury.truststore"));
        service.setTrustStorePassword("password1234");

        serverCachedDirs = new CachedDirs();
        serverCachedDirs.setRootFile(getServerMailboxes());
        // cachedDirs.setSpecialFiles("deploy/mercury-data/config/accounts.properties, deploy/mercury-data/config/accounts.keystore");

        syncConnectionHandler = new SyncConnectionHandler();
        syncConnectionHandler.setCachedDirs(serverCachedDirs);
        syncConnectionHandler.setKeystoreURL(getClass().getResource("/localhost.keystore")); // TODO!!!
        syncConnectionHandler.setPassword("password1234");

        service.setConnectionHandler(syncConnectionHandler);

        syncCommandFactory = new SyncCommandFactory();
        syncCommandFactory.setInactivityTimeout(60000);
        syncConnectionHandler.setConnectionHandler(syncCommandFactory);

        service.create();
        service.start();

//        System.out.println("Started server.");

        syncClient = new SyncClient();
        syncClient.setPort(port);
        syncClient.setAddress("localhost");
        syncClient.setSSL(true);
        syncClient.setKeyStoreURL(getClass().getResource("/localhost.keystore"));
        syncClient.setKeyStorePassword("password1234");

        syncClient.connect();
    }

    public void duplicateServerSetup(String newName) throws IOException {
        if (localDirSetup != null) {
            localDirSetup.cleanup();
        }

        localDirSetup = getServerDirSetup().duplicate(newName);
    }

    public void cleanup() throws IOException {

        if (syncClient != null) {
            syncClient.disconnect();
        }
        try {
            service.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            service.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (serverDirSetup != null) {
            serverDirSetup.cleanup();
        }
        if (localDirSetup != null) {
            localDirSetup.cleanup();
        }
    }

    public static RemoteFile listLine(File file) {
        return new RemoteFile(file.lastModified(), file.length(), "", file.getName());
    }

    public static String dirLine(File root, File file) {
        long timestamp = file.lastModified() / 1000;
        String path = file.getName();
        while (!root.equals(file)) {
            file = file.getParentFile();
            path = file.getName() + "/" + path;
        }

        return timestamp + " " + path;
    }

    public static void sleep1ms() {
        long now = System.currentTimeMillis();
        while (now == System.currentTimeMillis()) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ignore) {
            }
        }
    }

    public static void sleep1s() {
        try {
            Thread.sleep(1400);
        } catch (InterruptedException ignore) {
        }
    }
}
