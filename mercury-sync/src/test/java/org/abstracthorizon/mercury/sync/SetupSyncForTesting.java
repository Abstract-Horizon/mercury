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

import static org.abstracthorizon.mercury.sync.TestUtils.loadFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.abstracthorizon.danube.service.server.MultiThreadServerSSLSocketService;
import org.abstracthorizon.mercury.sync.cachedir.CachedDir;
import org.abstracthorizon.mercury.sync.cachedir.CachedDirs;
import org.abstracthorizon.mercury.sync.client.SyncClient;
import org.abstracthorizon.mercury.sync.client.SyncClient.RemoteFile;
import org.abstracthorizon.mercury.sync.commands.SyncCommandFactory;

public class SetupSyncForTesting {

    public static void main(String[] args) throws Exception {

        try {

            MultiThreadServerSSLSocketService service = new MultiThreadServerSSLSocketService();
            service.setName("sync");
            service.setPort(8852);
            service.setAddress("localhost");
            service.setServerSocketTimeout(1000);
            service.setNewSocketTimeout(60000);
            service.setKeyStoreFile(new File("config/ssl.keystore"));
            service.setKeyStorePassword("password1234");
            service.setTrustStoreFile(new File("config/mercury.truststore"));
            service.setTrustStorePassword("password1234");

            SyncConnectionHandler syncConnectionHandler = new SyncConnectionHandler();
            service.setConnectionHandler(syncConnectionHandler);
            syncConnectionHandler.setKeystoreURL(SetupSyncForTesting.class.getResource("/localhost.keystore")); // TODO!!!
            syncConnectionHandler.setPassword("password1234");

            SyncCommandFactory syncCommandFactory = new SyncCommandFactory();
            syncConnectionHandler.setConnectionHandler(syncCommandFactory);

            CachedDirs cachedDirs = new CachedDirs();
            cachedDirs.setRootFile(new File(new File(System.getProperty("user.home")), "temp/mercury/deploy/mercury-data/mailboxes"));
            cachedDirs.setSpecialFiles("deploy/mercury-data/config/accounts.properties, deploy/mercury-data/config/accounts.keystore");

            syncCommandFactory.setInactivityTimeout(60000);

            service.create();
            service.start();

            System.out.println("Started.");

            SyncClient syncClient = new SyncClient();
            syncClient.setPort(8852);
            syncClient.setAddress("localhost");
            syncClient.setSSL(true);
            syncClient.setKeyStoreFile(new File("config/localhost.keystore"));
            syncClient.setKeyStorePassword("password1234");

            syncClient.connect();
            try {
                System.out.println("-- DIR ---------------------------------------------------------------------------");
                syncClient.dir();
                List<String> dirs = syncClient.getCachedDirs().toStringList();
                for (String d : dirs) {
                   System.out.println(d);
                }
                System.out.println("-- LIST --------------------------------------------------------------------------");

                list("config", 0, syncClient);

                System.out.println("-- LIST --------------------------------------------------------------------------");

                List<RemoteFile> list1 = list("sendula.com/daniel/.inbox/cur", 1526113317915L, syncClient);

                System.out.println("-- GET ---------------------------------------------------------------------------");

                File localFile = File.createTempFile(list1.get(0).getName(), ".tmp");
                localFile.deleteOnExit();
                syncClient.download("sendula.com/daniel/.inbox/cur/" + list1.get(0).getName(), localFile);

                String mail = loadFile(localFile);
                System.out.println(mail);

                System.out.println("-- PUT ---------------------------------------------------------------------------");

                syncClient.upload("sendula.com/daniel/.inbox/cur/new-file", localFile);

                System.out.println("-- LIST --------------------------------------------------------------------------");

                List<RemoteFile> list2 = list("sendula.com/daniel/.inbox/cur", 1526113317915L, syncClient);
                if (!list2.get(list2.size() - 1).getName().equals("new-file")) {
                    System.err.println("Failed to upload file 'new-file'");
                }
                System.out.println("-- DELETE ------------------------------------------------------------------------");

                syncClient.delete("sendula.com/daniel/.inbox/cur/new-file", 1526113317915L);

                System.out.println("-- LIST --------------------------------------------------------------------------");

                List<RemoteFile> list3 = list("sendula.com/daniel/.inbox/cur", 1526113317915L, syncClient);
                if (list3.size() != list1.size()) {
                    System.err.println("Failed to delete file 'new-file'");
                }
                System.out.println("-----------------------------------------------------------------------------");

                SyncClient syncClient2 = new SyncClient();
                syncClient2.setPort(8852);
                syncClient2.setAddress("localhost");
                syncClient2.setSSL(true);
                syncClient2.setKeyStoreFile(new File("config/localhost.keystore"));
                syncClient2.setKeyStorePassword("password1234");

                syncClient2.connect();
                Thread.sleep(1000);

                System.out.println("-- LIST2 -------------------------------------------------------------------------");

                list("sendula.com/daniel/.inbox/cur", 1526113317915L, syncClient);

            } finally {
                syncClient.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }

    private static List<RemoteFile> list(String path, long since, SyncClient syncClient) throws FileNotFoundException, IOException {
        CachedDir inbox = syncClient.getCachedDirs().forPath(path);
        List<RemoteFile> list = syncClient.list(since, inbox);
        for (RemoteFile f : list) {
            System.out.println(f);
        }
        return list;
    }

}
