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

import static org.abstracthorizon.mercury.sync.MercuryDirSetup.compareRecursively;
import static org.abstracthorizon.mercury.sync.MercuryDirSetup.testForDeletedAndDuplicates;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.abstracthorizon.mercury.sync.cachedir.CachedDirs;
import org.junit.Test;

public class TestSyncProcess {

    @Test
    public void testNoChanges() throws IOException, InterruptedException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
            createTypical(setup);

            CachedDirs localCachedDirs = new CachedDirs();
            localCachedDirs.setRootFile(setup.getLocalMailboxes());

            SyncConnectionHandler syncConnectionHandler = createSyncConnectionHandler(localCachedDirs);

            Thread.sleep(2);
            syncConnectionHandler.syncWith("localhost", setup.getPort());
            Thread.sleep(2);

            List<String> result = compareRecursively(setup.getServerMailboxes(), setup.getLocalMailboxes(), new ArrayList<String>());
            testForDeletedAndDuplicates(setup.getServerDirSetup().getMailboxes(), result);
            testForDeletedAndDuplicates(setup.getLocalDirSetup().getMailboxes(), result);

            assertTrue("Got errors\n" + join(result), result.isEmpty());
        } finally {
            setup.cleanup();
        }
    }

    @Test
    public void testFromRemoteChangesToLocal() throws IOException, InterruptedException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
            createTypical(setup);

            setup.getServerDirSetup().createMessage("testmailbox1", ".testfolder1", "new", null);

            CachedDirs localCachedDirs = new CachedDirs();
            localCachedDirs.setRootFile(setup.getLocalMailboxes());

            SyncConnectionHandler syncConnectionHandler = createSyncConnectionHandler(localCachedDirs);

            Thread.sleep(2);
            syncConnectionHandler.syncWith("localhost", setup.getPort());
            Thread.sleep(2);

            List<String> result = compareRecursively(setup.getServerMailboxes(), setup.getLocalMailboxes(), new ArrayList<String>());
            testForDeletedAndDuplicates(setup.getServerDirSetup().getMailboxes(), result);
            testForDeletedAndDuplicates(setup.getLocalDirSetup().getMailboxes(), result);

            assertTrue("Got errors\n" + join(result), result.isEmpty());
        } finally {
            setup.cleanup();
        }
    }

    @Test
    public void testFromLocalChangesToRemote() throws IOException, InterruptedException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
            createTypical(setup);

            setup.getLocalDirSetup().createMessage("testmailbox1", ".testfolder1", "new", null);

            CachedDirs localCachedDirs = new CachedDirs();
            localCachedDirs.setRootFile(setup.getLocalMailboxes());

            SyncConnectionHandler syncConnectionHandler = createSyncConnectionHandler(localCachedDirs);

            Thread.sleep(2);
            syncConnectionHandler.syncWith("localhost", setup.getPort());
            Thread.sleep(2);

            List<String> result = compareRecursively(setup.getServerMailboxes(), setup.getLocalMailboxes(), new ArrayList<String>());
            testForDeletedAndDuplicates(setup.getServerDirSetup().getMailboxes(), result);
            testForDeletedAndDuplicates(setup.getLocalDirSetup().getMailboxes(), result);

            assertTrue("Got errors\n" + join(result), result.isEmpty());
        } finally {
            setup.cleanup();
        }
    }

    @Test
    public void testDeletedNewFromRemote() throws IOException, InterruptedException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
            createTypical(setup);
            long currentTimeMillis = System.currentTimeMillis();
            File localMessage = setup.getLocalDirSetup().createMessage("testmailbox1", ".testfolder1", "new", "2,S", currentTimeMillis);
            File remoteMessage = setup.getServerDirSetup().createMessage("testmailbox1", ".testfolder1", "new", "2,S", currentTimeMillis);
            setup.getServerDirSetup().deleteMessage(remoteMessage);
            assertTrue("Remote message still exists", !remoteMessage.exists());

            CachedDirs localCachedDirs = new CachedDirs();
            localCachedDirs.setRootFile(setup.getLocalMailboxes());

            SyncConnectionHandler syncConnectionHandler = createSyncConnectionHandler(localCachedDirs);

            Thread.sleep(2);
            syncConnectionHandler.syncWith("localhost", setup.getPort());
            Thread.sleep(2);

            List<String> result = compareRecursively(setup.getServerMailboxes(), setup.getLocalMailboxes(), new ArrayList<String>());
            testForDeletedAndDuplicates(setup.getServerDirSetup().getMailboxes(), result);
            testForDeletedAndDuplicates(setup.getLocalDirSetup().getMailboxes(), result);

            assertTrue("Got errors\n" + join(result), result.isEmpty());
            assertTrue("Local message still exists", !localMessage.exists());

        } finally {
            setup.cleanup();
        }
    }

    @Test
    public void testDeletedNewFromLocal() throws IOException, InterruptedException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
            createTypical(setup);
            long currentTimeMillis = System.currentTimeMillis();
            File localMessage = setup.getLocalDirSetup().createMessage("testmailbox1", ".testfolder1", "new", "2,S", currentTimeMillis);
            File remoteMessage = setup.getServerDirSetup().createMessage("testmailbox1", ".testfolder1", "new", "2,S", currentTimeMillis);

            setup.getLocalDirSetup().deleteMessage(localMessage);
            assertTrue("Local message still exists", !localMessage.exists());

            CachedDirs localCachedDirs = new CachedDirs();
            localCachedDirs.setRootFile(setup.getLocalMailboxes());

            SyncConnectionHandler syncConnectionHandler = createSyncConnectionHandler(localCachedDirs);

            Thread.sleep(2);
            syncConnectionHandler.syncWith("localhost", setup.getPort());
            Thread.sleep(2);

            List<String> result = compareRecursively(setup.getServerMailboxes(), setup.getLocalMailboxes(), new ArrayList<String>());
            testForDeletedAndDuplicates(setup.getServerDirSetup().getMailboxes(), result);
            testForDeletedAndDuplicates(setup.getLocalDirSetup().getMailboxes(), result);

            assertTrue("Got errors\n" + join(result), result.isEmpty());
            assertTrue("Remote message still exists", !remoteMessage.exists());
        } finally {
            setup.cleanup();
        }
    }

    @Test
    public void testDeletedCurFromRemote() throws IOException, InterruptedException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
            createTypical(setup);
            long currentTimeMillis = System.currentTimeMillis();
            File localMessage = setup.getLocalDirSetup().createMessage("testmailbox1", ".testfolder1", "cur", "2,S", currentTimeMillis);
            File remoteMessage = setup.getServerDirSetup().createMessage("testmailbox1", ".testfolder1", "cur", "2,S", currentTimeMillis);
            setup.getServerDirSetup().deleteMessage(remoteMessage);
            assertTrue("Remote message still exists", !remoteMessage.exists());

            CachedDirs localCachedDirs = new CachedDirs();
            localCachedDirs.setRootFile(setup.getLocalMailboxes());

            SyncConnectionHandler syncConnectionHandler = createSyncConnectionHandler(localCachedDirs);

            Thread.sleep(2);
            syncConnectionHandler.syncWith("localhost", setup.getPort());
            Thread.sleep(2);

            List<String> result = compareRecursively(setup.getServerMailboxes(), setup.getLocalMailboxes(), new ArrayList<String>());
            testForDeletedAndDuplicates(setup.getServerDirSetup().getMailboxes(), result);
            testForDeletedAndDuplicates(setup.getLocalDirSetup().getMailboxes(), result);

            assertTrue("Got errors\n" + join(result), result.isEmpty());
            assertTrue("Local message still exists", !localMessage.exists());

        } finally {
            setup.cleanup();
        }
    }

    @Test
    public void testDeletedCurFromLocal() throws IOException, InterruptedException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
            createTypical(setup);
            long currentTimeMillis = System.currentTimeMillis();
            File localMessage = setup.getLocalDirSetup().createMessage("testmailbox1", ".testfolder1", "cur", "2,S", currentTimeMillis);
            File remoteMessage = setup.getServerDirSetup().createMessage("testmailbox1", ".testfolder1", "cur", "2,S", currentTimeMillis);

            setup.getLocalDirSetup().deleteMessage(localMessage);
            assertTrue("Local message still exists", !localMessage.exists());

            CachedDirs localCachedDirs = new CachedDirs();
            localCachedDirs.setRootFile(setup.getLocalMailboxes());

            SyncConnectionHandler syncConnectionHandler = createSyncConnectionHandler(localCachedDirs);

            Thread.sleep(2);
            syncConnectionHandler.syncWith("localhost", setup.getPort());
            Thread.sleep(2);

            List<String> result = compareRecursively(setup.getServerMailboxes(), setup.getLocalMailboxes(), new ArrayList<String>());
            testForDeletedAndDuplicates(setup.getServerDirSetup().getMailboxes(), result);
            testForDeletedAndDuplicates(setup.getLocalDirSetup().getMailboxes(), result);

            assertTrue("Got errors\n" + join(result), result.isEmpty());
            assertTrue("Remote message still exists", !remoteMessage.exists());
        } finally {
            setup.cleanup();
        }
    }

    @Test
    public void testNewFolderInRemoteToLocal() throws IOException, InterruptedException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
            createTypical(setup);

            setup.getServerDirSetup().createFolder("testmailbox1", ".newtestfolder");

            CachedDirs localCachedDirs = new CachedDirs();
            localCachedDirs.setRootFile(setup.getLocalMailboxes());

            SyncConnectionHandler syncConnectionHandler = createSyncConnectionHandler(localCachedDirs);

            Thread.sleep(2);
            syncConnectionHandler.syncWith("localhost", setup.getPort());
            Thread.sleep(2);

            List<String> result = compareRecursively(setup.getServerMailboxes(), setup.getLocalMailboxes(), new ArrayList<String>());
            testForDeletedAndDuplicates(setup.getServerDirSetup().getMailboxes(), result);
            testForDeletedAndDuplicates(setup.getLocalDirSetup().getMailboxes(), result);

            assertTrue("Got errors\n" + join(result), result.isEmpty());
        } finally {
            setup.cleanup();
        }
    }

    @Test
    public void testNewFolderInLocalToRemote() throws IOException, InterruptedException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
            createTypical(setup);

            setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");

            CachedDirs localCachedDirs = new CachedDirs();
            localCachedDirs.setRootFile(setup.getLocalMailboxes());

            SyncConnectionHandler syncConnectionHandler = createSyncConnectionHandler(localCachedDirs);

            Thread.sleep(2);
            syncConnectionHandler.syncWith("localhost", setup.getPort());
            Thread.sleep(2);

            List<String> result = compareRecursively(setup.getServerMailboxes(), setup.getLocalMailboxes(), new ArrayList<String>());
            testForDeletedAndDuplicates(setup.getServerDirSetup().getMailboxes(), result);
            testForDeletedAndDuplicates(setup.getLocalDirSetup().getMailboxes(), result);

            assertTrue("Got errors\n" + join(result), result.isEmpty());
        } finally {
            setup.cleanup();
        }
    }

    @Test
    public void testNewFolderPopulationInRemote() throws IOException, InterruptedException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
            createTypical(setup);

            setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");
            setup.getLocalDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null);
            setup.getLocalDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null);
            Thread.sleep(1000);
            CachedDirs localCachedDirs = new CachedDirs();
            localCachedDirs.setRootFile(setup.getLocalMailboxes());

            SyncConnectionHandler syncConnectionHandler = createSyncConnectionHandler(localCachedDirs);

            Thread.sleep(2);
            syncConnectionHandler.syncWith("localhost", setup.getPort());
            Thread.sleep(2);

            List<String> result = compareRecursively(setup.getServerMailboxes(), setup.getLocalMailboxes(), new ArrayList<String>());
            testForDeletedAndDuplicates(setup.getServerDirSetup().getMailboxes(), result);
            testForDeletedAndDuplicates(setup.getLocalDirSetup().getMailboxes(), result);

            assertTrue("Got errors\n" + join(result), result.isEmpty());
        } finally {
            setup.cleanup();
        }

    }

    @Test
    public void testNewFolderPopulationInLocal() throws IOException, InterruptedException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
            createTypical(setup);

            setup.getServerDirSetup().createFolder("testmailbox1", ".newtestfolder");
            setup.getServerDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null);
            setup.getServerDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null);

            CachedDirs localCachedDirs = new CachedDirs();
            localCachedDirs.setRootFile(setup.getLocalMailboxes());

            SyncConnectionHandler syncConnectionHandler = createSyncConnectionHandler(localCachedDirs);

            Thread.sleep(2);
            syncConnectionHandler.syncWith("localhost", setup.getPort());
            Thread.sleep(2);

            List<String> result = compareRecursively(setup.getServerMailboxes(), setup.getLocalMailboxes(), new ArrayList<String>());
            testForDeletedAndDuplicates(setup.getServerDirSetup().getMailboxes(), result);
            testForDeletedAndDuplicates(setup.getLocalDirSetup().getMailboxes(), result);

            assertTrue("Got errors\n" + join(result), result.isEmpty());
        } finally {
            setup.cleanup();
        }
    }

    @Test
    public void testNewToCurFolderRemote() throws IOException, InterruptedException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
            createTypical(setup);

            Thread.sleep(1200);

            long lastSyncedTime = System.currentTimeMillis() - 10000;

            setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");
            setup.getServerDirSetup().createFolder("testmailbox1", ".newtestfolder");
            File newMessage = setup.getLocalDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null, lastSyncedTime - 10000);
            File tempCurMessage = setup.getServerDirSetup().createMessage("testmailbox1", ".newtestfolder", "cur", null, lastSyncedTime - 10000);
            File curMessage = new File(tempCurMessage.getParent(), newMessage.getName() + ":2,S");

            tempCurMessage.delete();
            copyFile(newMessage, curMessage);

            curMessage.setLastModified(lastSyncedTime + 10000);

            Thread.sleep(1000);
            CachedDirs localCachedDirs = new CachedDirs();
            localCachedDirs.setRootFile(setup.getLocalMailboxes());

            SyncConnectionHandler syncConnectionHandler = createSyncConnectionHandler(localCachedDirs);
            syncConnectionHandler.setLastSyncedTime(lastSyncedTime);

            Thread.sleep(2);
            syncConnectionHandler.syncWith("localhost", setup.getPort());
            Thread.sleep(2);

            List<String> result = compareRecursively(setup.getServerMailboxes(), setup.getLocalMailboxes(), new ArrayList<String>());
            testForDeletedAndDuplicates(setup.getServerDirSetup().getMailboxes(), result);
            testForDeletedAndDuplicates(setup.getLocalDirSetup().getMailboxes(), result);

            assertTrue("Got errors\n" + join(result), result.isEmpty());
        } finally {
            setup.cleanup();
        }
    }

    @Test
    public void testNewToCurFolderLocal() throws IOException, InterruptedException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
            createTypical(setup);

            long lastSyncedTime = System.currentTimeMillis() - 100000;

            setup.getServerDirSetup().createFolder("testmailbox1", ".newtestfolder");
            setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");
            File newMessage = setup.getServerDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null, lastSyncedTime - 100000);
            File tempCurMessage = setup.getLocalDirSetup().createMessage("testmailbox1", ".newtestfolder", "cur", null, lastSyncedTime - 100000);
            File curMessage = new File(tempCurMessage.getParent(), newMessage.getName() + ":2,S");

            tempCurMessage.delete();
            copyFile(newMessage, curMessage);

            curMessage.setLastModified(lastSyncedTime + 100000);

            Thread.sleep(1000);
            CachedDirs localCachedDirs = new CachedDirs();
            localCachedDirs.setRootFile(setup.getLocalMailboxes());

            SyncConnectionHandler syncConnectionHandler = createSyncConnectionHandler(localCachedDirs);
            syncConnectionHandler.setLastSyncedTime(lastSyncedTime);

            Thread.sleep(2);
            syncConnectionHandler.syncWith("localhost", setup.getPort());
            Thread.sleep(2);

            List<String> result = compareRecursively(setup.getServerMailboxes(), setup.getLocalMailboxes(), new ArrayList<String>());
            testForDeletedAndDuplicates(setup.getServerDirSetup().getMailboxes(), result);
            testForDeletedAndDuplicates(setup.getLocalDirSetup().getMailboxes(), result);

            assertTrue("Got errors\n" + join(result), result.isEmpty());
        } finally {
            setup.cleanup();
        }
    }

    @Test
    public void testDeletedDirRemote() throws IOException, InterruptedException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
            createTypical(setup);

            File localFolder = setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");
            // Set the local folder to be older than when the remote one is deleted
            localFolder.setLastModified(1);

            @SuppressWarnings("unused")
            File remoteMailbox = setup.getServerDirSetup().createMailbox("testmailbox1");

            File deletedFiles = new File(setup.getServerCachedDirs().getRootFile(), ".deleted_dirs");
            TestUtils.writeFile(deletedFiles, "100 testmailbox1/.newtestfolder\n");

            setup.getServerCachedDirs().refresh();

            CachedDirs localCachedDirs = new CachedDirs();
            localCachedDirs.setRootFile(setup.getLocalMailboxes());

            SyncConnectionHandler syncConnectionHandler = createSyncConnectionHandler(localCachedDirs);

            Thread.sleep(2);
            syncConnectionHandler.syncWith("localhost", setup.getPort());
            Thread.sleep(2);

            List<String> result = compareRecursively(setup.getServerMailboxes(), setup.getLocalMailboxes(), new ArrayList<String>());

            assertTrue("Got errors\n" + join(result), result.isEmpty());
            assertTrue("Local folder still exists", !localFolder.exists());

        } finally {
            setup.cleanup();
        }
    }

    @Test
    public void testDeletedDirLocal() throws IOException, InterruptedException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
            createTypical(setup);

            File remoteFolder = setup.getServerDirSetup().createFolder("testmailbox1", ".newtestfolder");
            // Set the remote folder to be older than when the local one is deleted
            remoteFolder.setLastModified(1);

            @SuppressWarnings("unused")
            File localMailbox = setup.getLocalDirSetup().createMailbox("testmailbox1");

            // simulate deleting folder
            File deletedFiles = new File(setup.getLocalDirSetup().getMailboxes(), ".deleted_dirs");
            String string = "testmailbox1/.newtestfolder";
            TestUtils.writeFile(deletedFiles, "1000 " + string + "\n");

            CachedDirs localCachedDirs = new CachedDirs();
            localCachedDirs.setRootFile(setup.getLocalMailboxes());

            SyncConnectionHandler syncConnectionHandler = createSyncConnectionHandler(localCachedDirs);

            Thread.sleep(2);
            syncConnectionHandler.syncWith("localhost", setup.getPort());
            Thread.sleep(2);

            List<String> result = compareRecursively(setup.getServerMailboxes(), setup.getLocalMailboxes(), new ArrayList<String>());

            assertTrue("Got errors\n" + join(result), result.isEmpty());
            assertTrue("Remote folder still exists", !remoteFolder.exists());

        } finally {
            setup.cleanup();
        }
    }

    @Test
    public void testRecreatingDeletedDirInRemote() throws IOException, InterruptedException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
            createTypical(setup);

            File remoteFolder = setup.getServerDirSetup().createFolder("testmailbox1", ".newtestfolder");
            remoteFolder.setLastModified(remoteFolder.lastModified() - 1000);
            assertTrue("Remote Folder was not created", remoteFolder.exists());

            File localMailbox = setup.getLocalDirSetup().createMailbox("testmailbox1");
            File localFolder = new File(localMailbox, ".newtestfolder");

            // simulate deleting folder
            File deletedFiles = new File(setup.getLocalDirSetup().getMailboxes(), ".deleted_dirs");
            String string = "testmailbox1/.newtestfolder";
            TestUtils.writeFile(deletedFiles, (System.currentTimeMillis() / 1000) + " " + string + "\n");

            CachedDirs localCachedDirs = new CachedDirs();
            localCachedDirs.setRootFile(setup.getLocalMailboxes());

            SyncConnectionHandler syncConnectionHandler = createSyncConnectionHandler(localCachedDirs);
            Thread.sleep(2);
            syncConnectionHandler.syncWith("localhost", setup.getPort());
            Thread.sleep(2);

            assertTrue("Remote Folder was not deleted", !remoteFolder.exists());
            List<String> result = compareRecursively(setup.getServerMailboxes(), setup.getLocalMailboxes(), new ArrayList<String>());
            assertTrue("There were differences after first sync (delete) \n" + join(result), result.isEmpty());

            File newRemoteFolder = setup.getServerDirSetup().createFolder("testmailbox1", ".newtestfolder");
            newRemoteFolder.setLastModified(newRemoteFolder.lastModified() + 1000);

            setup.getServerCachedDirs().forceRefresh();

            assertTrue("Remote Folder was not created", newRemoteFolder.exists());

            Thread.sleep(2);
            syncConnectionHandler.syncWith("localhost", setup.getPort());
            Thread.sleep(2);

            List<String> result2 = compareRecursively(setup.getServerMailboxes(), setup.getLocalMailboxes(), new ArrayList<String>());

            assertTrue("Remote Folder was re-deleted", newRemoteFolder.exists());
            assertTrue("New folder was not synced back to local", localFolder.exists());

            assertTrue("There were differences after second sync (recreate) \n" + join(result2), result2.isEmpty());

        } finally {
            setup.cleanup();
        }
    }

    @Test
    public void testRecreatingDeletedDirInLocal() throws IOException, InterruptedException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
            createTypical(setup);

            File localFolder = setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");
            localFolder.setLastModified(localFolder.lastModified() - 1000);
            assertTrue("Local Folder was not created", localFolder.exists());

            @SuppressWarnings("unused")
            File remoteMailbox = setup.getServerDirSetup().createMailbox("testmailbox1");

            // simulate deleting folder
            File deletedFiles = new File(setup.getServerDirSetup().getMailboxes(), ".deleted_dirs");
            String string = "testmailbox1/.newtestfolder";
            TestUtils.writeFile(deletedFiles, (System.currentTimeMillis() / 1000) + " " + string + "\n");

            CachedDirs localCachedDirs = new CachedDirs();
            localCachedDirs.setRootFile(setup.getLocalMailboxes());

            SyncConnectionHandler syncConnectionHandler = createSyncConnectionHandler(localCachedDirs);

            Thread.sleep(2);
            syncConnectionHandler.syncWith("localhost", setup.getPort());
            Thread.sleep(2);

            assertTrue("Local Folder was not deleted", !localFolder.exists());
            List<String> result = compareRecursively(setup.getServerMailboxes(), setup.getLocalMailboxes(), new ArrayList<String>());
            assertTrue("There were differences after first sync (delete) \n" + join(result), result.isEmpty());

            File newLocalFolder = setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");
            newLocalFolder.setLastModified(newLocalFolder.lastModified() + 1000);

            localCachedDirs.forceRefresh();

            assertTrue("Local Folder was not created", newLocalFolder.exists());

            Thread.sleep(2);
            syncConnectionHandler.syncWith("localhost", setup.getPort());
            Thread.sleep(2);

            List<String> result2 = compareRecursively(setup.getServerMailboxes(), setup.getLocalMailboxes(), new ArrayList<String>());

            assertTrue("Local Folder was re-deleted", newLocalFolder.exists());
            assertTrue("New folder was not synced back to remote", localFolder.exists());

            assertTrue("There were differences after second sync (recreate) \n" + join(result2), result2.isEmpty());

        } finally {
            setup.cleanup();
        }
    }

    @Test
    public void testSyncingDeletedDirsFileToRemote() throws IOException, InterruptedException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
            createTypical(setup);

            File remoteFolder = setup.getServerDirSetup().createFolder("testmailbox1", ".newtestfolder");
            // Set the remote folder to be older than when the local one is deleted
            remoteFolder.setLastModified(1);

            @SuppressWarnings("unused")
            File localMailbox = setup.getLocalDirSetup().createMailbox("testmailbox1");

            // simulate deleting folder
            File deletedDirsLocal = new File(setup.getLocalDirSetup().getMailboxes(), ".deleted_dirs");
            File deletedDirsRemote = new File(setup.getServerCachedDirs().getRootFile(), ".deleted_dirs");

            String string = "testmailbox1/.newtestfolder";
            TestUtils.writeFile(deletedDirsLocal, (System.currentTimeMillis() / 1000) + " " + string + "\n");

            CachedDirs localCachedDirs = new CachedDirs();
            localCachedDirs.setRootFile(setup.getLocalMailboxes());

            SyncConnectionHandler syncConnectionHandler = createSyncConnectionHandler(localCachedDirs);

            Thread.sleep(2);
            syncConnectionHandler.syncWith("localhost", setup.getPort());
            Thread.sleep(2);

            List<String> result = compareRecursively(setup.getServerMailboxes(), setup.getLocalMailboxes(), new ArrayList<String>());

            assertTrue("Got errors\n" + join(result), result.isEmpty());
            assertTrue("Remote folder still exists", !remoteFolder.exists());

            assertTrue("Deleted Dirs local doesnt exist", deletedDirsLocal.exists());
            assertTrue("Deleted dirs was not copied", deletedDirsRemote.exists());

        } finally {
            setup.cleanup();
        }
    }

    @Test
    public void testSyncingDeletedDirsFileToLocal() throws IOException, InterruptedException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
            createTypical(setup);

            File localFolder = setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");
            // Set the local folder to be older than when the remote one is deleted
            localFolder.setLastModified(1);

            @SuppressWarnings("unused")
            File remoteMailbox = setup.getServerDirSetup().createMailbox("testmailbox1");

            File deletedDirsRemote = new File(setup.getServerCachedDirs().getRootFile(), ".deleted_dirs");
            File deletedDirsLocal = new File(setup.getLocalDirSetup().getMailboxes(), ".deleted_dirs");

            TestUtils.writeFile(deletedDirsRemote, (System.currentTimeMillis() / 1000) + " testmailbox1/.newtestfolder\n");

            setup.getServerCachedDirs().refresh();

            CachedDirs localCachedDirs = new CachedDirs();
            localCachedDirs.setRootFile(setup.getLocalMailboxes());

            SyncConnectionHandler syncConnectionHandler = createSyncConnectionHandler(localCachedDirs);

            Thread.sleep(2);
            syncConnectionHandler.syncWith("localhost", setup.getPort());
            Thread.sleep(2);

            List<String> result = compareRecursively(setup.getServerMailboxes(), setup.getLocalMailboxes(), new ArrayList<String>());

            assertTrue("Got errors\n" + join(result), result.isEmpty());
            assertTrue("Local folder still exists", !localFolder.exists());

            assertTrue("Deleted Dirs remote doesnt exist", deletedDirsRemote.exists());
            assertTrue("Deleted dirs was not copied", deletedDirsLocal.exists());
        } finally {
            setup.cleanup();
        }
    }

    @Test
    public void testRemovingExpiredDeletedDirs() throws IOException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
            createTypical(setup);
            File deletedFiles = new File(setup.getLocalMailboxes(), ".deleted_dirs");

            String keep = System.currentTimeMillis() + " .newtestfolder2\n" + System.currentTimeMillis() + " .newtestfolder3\n";

            String data = "1 .newtestfolder\n" + keep;
            TestUtils.writeFile(deletedFiles, data);

            CachedDirs localCachedDirs = new CachedDirs();
            localCachedDirs.setRootFile(setup.getLocalMailboxes());
            localCachedDirs.refresh();

            assertTrue("Deleted dirs file was deleted", deletedFiles.exists());

            String loadFile = TestUtils.loadFile(deletedFiles);
            assertEquals("Line wasnt deleted from deleted dirs", keep, loadFile);
        } finally {
            setup.cleanup();
        }
    }

    @Test
    public void testDeletingEmptyDeletedDirs() throws IOException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
            createTypical(setup);
            File deletedFiles = new File(setup.getLocalMailboxes(), ".deleted_dirs");

            String data = "1 .newtestfolder\n" + "2 .newtestfolder\n";

            TestUtils.writeFile(deletedFiles, data);

            CachedDirs localCachedDirs = new CachedDirs();
            localCachedDirs.setRootFile(setup.getLocalMailboxes());
            localCachedDirs.refresh();

            assertTrue("Deleted dirs file was not deleted", !deletedFiles.exists());
        } finally {
            setup.cleanup();
        }
    }

    @Test
    public void testRemoveExistingDirectoryEntries() throws IOException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
            createTypical(setup);
            File deletedFiles = new File(setup.getLocalMailboxes(), ".deleted_dirs");
            setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");

            // a directory that was deleted and it stays that way
            String keep = System.currentTimeMillis() + " testmailbox1/.newtestfolder2\n";

            String data = System.currentTimeMillis() + " testmailbox1/.newtestfolder\n" + keep;

            TestUtils.writeFile(deletedFiles, data);

            CachedDirs localCachedDirs = new CachedDirs();
            localCachedDirs.setRootFile(setup.getLocalMailboxes());
            localCachedDirs.refresh();

            String loadFile = TestUtils.loadFile(deletedFiles);

            assertEquals("Line wasnt deleted from deleted dirs", keep, loadFile);
        } finally {
            setup.cleanup();
        }
    }

    private void createTypical(MercuryTestSyncSetup setup) throws IOException {
        setup.getServerDirSetup().createFolder("testmailbox1", ".testfolder1");
        setup.getServerDirSetup().createFolder("testmailbox2", ".testfolder2");

        setup.getServerDirSetup().createMessage("testmailbox1", ".testfolder1", "new", null);
        setup.getServerDirSetup().createMessage("testmailbox1", ".testfolder1", "cur", null);
        setup.getServerDirSetup().createMessage("testmailbox1", ".testfolder1", "del", null);

        setup.getServerDirSetup().createMessage("testmailbox2", ".testfolder2", "cur", null);

        setup.duplicateServerSetup("local");
    }

    private SyncConnectionHandler createSyncConnectionHandler(CachedDirs localCachedDirs) {
        SyncConnectionHandler syncConnectionHandler = new SyncConnectionHandler();
        syncConnectionHandler.setCachedDirs(localCachedDirs);
        syncConnectionHandler.setKeystoreURL(getClass().getResource("/localhost.keystore"));
        syncConnectionHandler.setPassword("password1234");
        return syncConnectionHandler;
    }

    public static String join(List<String> list) {
        StringBuilder res = new StringBuilder();
        for (String s : list) {
            res.append(s).append('\n');
        }
        return res.toString();
    }

    public static void copyFile(File newMessage, File curMessage) throws IOException {
        try (FileInputStream fis = new FileInputStream(newMessage);
                FileOutputStream fos = new FileOutputStream(curMessage)) {
            byte[] buf = new byte[10240];
            int r = fis.read(buf);
            while (r > 0) {
                fos.write(buf, 0, r);
                r = fis.read(buf);
            }
        }
        // TODO Auto-generated method stub

    }
}
