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
import static org.abstracthorizon.mercury.sync.TestUtils.writeFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.abstracthorizon.mercury.sync.cachedir.CachedDirs;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestSyncProcessFolders {

    private MercuryTestSyncSetup setup;
    private long lastSyncedTime;
    private CachedDirs localCachedDirs;
    private SyncConnectionHandler syncConnectionHandler;

    @Before
    public void setUp() throws IOException {
        lastSyncedTime = System.currentTimeMillis() - 20000;

        setup = new MercuryTestSyncSetup();
        setup.create();
        setup.duplicateServerSetup("local");

        localCachedDirs = new CachedDirs();
        localCachedDirs.setRootFile(setup.getLocalDeploy());

        createTypical(setup);

        syncConnectionHandler = createSyncConnectionHandler(localCachedDirs);
        syncConnectionHandler.setLastSyncedTime(lastSyncedTime);

        System.out.println("-------------------------------------------------------------------------");
        System.out.println("Local:  " + setup.getLocalDirSetup().getMailboxes().getAbsolutePath());
        System.out.println("Remote: " + setup.getServerDirSetup().getMailboxes().getAbsolutePath());
    }

    @After
    public void cleanup() throws IOException {
        setup.cleanup();
    }

    @Test
    public void testDeletedDirRemote() throws IOException, InterruptedException {
        File localFolder = setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");
        // Set the local folder to be older than when the remote one is deleted
        localFolder.setLastModified(1);

        @SuppressWarnings("unused")
        File remoteMailbox = setup.getServerDirSetup().createMailbox("testmailbox1");

        @SuppressWarnings("unused")
        File localMailbox = setup.getLocalDirSetup().createMailbox("testmailbox1");
        File deletedFiles = new File(setup.getServerCachedDirs().getRootFile(), ".deleted_dirs");
        writeFile(deletedFiles, "100 mailboxes/testmailbox1/.newtestfolder\n");

        setup.getServerCachedDirs().refresh();

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        List<String> result = compareRecursively(setup.getServerDeploy(), setup.getLocalDeploy(), new ArrayList<String>());

        assertTrue("Got errors\n" + join(result), result.isEmpty());
        assertTrue("Local folder still exists", !localFolder.exists());
    }

    @Test
    public void testDeletedDirLocal() throws IOException, InterruptedException {
        File remoteFolder = setup.getServerDirSetup().createFolder("testmailbox1", ".newtestfolder");
        // Set the remote folder to be older than when the local one is deleted
        remoteFolder.setLastModified(1);

        @SuppressWarnings("unused")
        File localMailbox = setup.getLocalDirSetup().createMailbox("testmailbox1");

        // simulate deleting folder
        File deletedFiles = new File(setup.getLocalDeploy(), ".deleted_dirs");
        String string = "mailboxes/testmailbox1/.newtestfolder";
        writeFile(deletedFiles, "1000 " + string + "\n");

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        List<String> result = compareRecursively(setup.getServerDeploy(), setup.getLocalDeploy(), new ArrayList<String>());

        assertTrue("Got errors\n" + join(result), result.isEmpty());
        assertTrue("Remote folder still exists", !remoteFolder.exists());
    }

    @Test
    public void testRecreatingDeletedDirInRemote() throws IOException, InterruptedException {
        File remoteFolder = setup.getServerDirSetup().createFolder("testmailbox1", ".newtestfolder");
        remoteFolder.setLastModified(remoteFolder.lastModified() - 1000);
        assertTrue("Remote Folder was not created", remoteFolder.exists());

        File localMailbox = setup.getLocalDirSetup().createMailbox("testmailbox1");
        File localFolder = new File(localMailbox, ".newtestfolder");

        // simulate deleting folder
        File deletedFiles = new File(setup.getLocalDeploy(), ".deleted_dirs");
        String string = "mailboxes/testmailbox1/.newtestfolder";
        writeFile(deletedFiles, (System.currentTimeMillis() / 1000) + " " + string + "\n");

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        assertTrue("Remote Folder was not deleted", !remoteFolder.exists());
        List<String> result = compareRecursively(setup.getServerDeploy(), setup.getLocalDeploy(), new ArrayList<String>());
        assertTrue("There were differences after first sync (delete) \n" + join(result), result.isEmpty());

        File newRemoteFolder = setup.getServerDirSetup().createFolder("testmailbox1", ".newtestfolder");
        newRemoteFolder.setLastModified(newRemoteFolder.lastModified() + 1000);

        setup.getServerCachedDirs().forceRefresh();

        assertTrue("Remote Folder was not created", newRemoteFolder.exists());

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        List<String> result2 = compareRecursively(setup.getServerDeploy(), setup.getLocalDeploy(), new ArrayList<String>());

        assertTrue("Remote Folder was re-deleted", newRemoteFolder.exists());
        assertTrue("New folder was not synced back to local", localFolder.exists());

        assertTrue("There were differences after second sync (recreate) \n" + join(result2), result2.isEmpty());
    }

    @Test
    public void testRecreatingDeletedDirInLocal() throws IOException, InterruptedException {
        File localFolder = setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");
        localFolder.setLastModified(localFolder.lastModified() - 1000);
        assertTrue("Local Folder was not created", localFolder.exists());

        setup.getServerDirSetup().createMailbox("testmailbox1");

        // simulate deleting folder
        File deletedFiles = new File(setup.getServerDeploy(), ".deleted_dirs");
        String string = "mailboxes/testmailbox1/.newtestfolder";
        writeFile(deletedFiles, (System.currentTimeMillis() / 1000) + " " + string + "\n");

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        assertTrue("Local Folder was not deleted", !localFolder.exists());
        List<String> result = compareRecursively(setup.getServerDeploy(), setup.getLocalDeploy(), new ArrayList<String>());
        assertTrue("There were differences after first sync (delete) \n" + join(result), result.isEmpty());

        File newLocalFolder = setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");
        newLocalFolder.setLastModified(newLocalFolder.lastModified() + 1000);

        localCachedDirs.forceRefresh();

        assertTrue("Local Folder was not created", newLocalFolder.exists());

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        List<String> result2 = compareRecursively(setup.getServerDeploy(), setup.getLocalDeploy(), new ArrayList<String>());

        assertTrue("Local Folder was re-deleted", newLocalFolder.exists());
        assertTrue("New folder was not synced back to remote", localFolder.exists());

        assertTrue("There were differences after second sync (recreate) \n" + join(result2), result2.isEmpty());
    }

    @Test
    public void testSyncingDeletedDirsFileToRemote() throws IOException, InterruptedException {
        File remoteFolder = setup.getServerDirSetup().createFolder("testmailbox1", ".newtestfolder");
        // Set the remote folder to be older than when the local one is deleted
        remoteFolder.setLastModified(1);

        @SuppressWarnings("unused")
        File localMailbox = setup.getLocalDirSetup().createMailbox("testmailbox1");

        // simulate deleting folder
        File deletedDirsLocal = new File(setup.getLocalDeploy(), ".deleted_dirs");
        File deletedDirsRemote = new File(setup.getServerDeploy(), ".deleted_dirs");

        String string = "mailboxes/testmailbox1/.newtestfolder";
        writeFile(deletedDirsLocal, (System.currentTimeMillis() / 1000) + " " + string + "\n");

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        List<String> result = compareRecursively(setup.getServerDeploy(), setup.getLocalDeploy(), new ArrayList<String>());

        assertTrue("Got errors\n" + join(result), result.isEmpty());
        assertTrue("Remote folder still exists", !remoteFolder.exists());

        assertTrue("Deleted Dirs local doesnt exist", deletedDirsLocal.exists());
        assertTrue("Deleted dirs was not copied", deletedDirsRemote.exists());
    }

    @Test
    public void testSyncingDeletedDirsFileToLocal() throws IOException, InterruptedException {
        File localFolder = setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");
        // Set the local folder to be older than when the remote one is deleted
        localFolder.setLastModified(1);

        @SuppressWarnings("unused")
        File remoteMailbox = setup.getServerDirSetup().createMailbox("testmailbox1");

        File deletedDirsRemote = new File(setup.getServerCachedDirs().getRootFile(), ".deleted_dirs");
        File deletedDirsLocal = new File(setup.getLocalDeploy(), ".deleted_dirs");

        writeFile(deletedDirsRemote, (System.currentTimeMillis() / 1000) + " mailboxes/testmailbox1/.newtestfolder\n");

        setup.getServerCachedDirs().refresh();

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        List<String> result = compareRecursively(setup.getServerDeploy(), setup.getLocalDeploy(), new ArrayList<String>());

        assertTrue("Got errors\n" + join(result), result.isEmpty());
        assertTrue("Local folder still exists", !localFolder.exists());

        assertTrue("Deleted Dirs remote doesnt exist", deletedDirsRemote.exists());
        assertTrue("Deleted dirs was not copied", deletedDirsLocal.exists());
    }

    @Test
    public void testRemovingExpiredDeletedDirs() throws IOException {
        File deletedFiles = new File(setup.getLocalDeploy(), ".deleted_dirs");

        String keep = System.currentTimeMillis() + " .newtestfolder2\n" + System.currentTimeMillis() + " .newtestfolder3\n";

        String data = "1 .newtestfolder\n" + keep;
        writeFile(deletedFiles, data);

        localCachedDirs.refresh();

        assertTrue("Deleted dirs file was deleted", deletedFiles.exists());

        String loadFile = TestUtils.loadFile(deletedFiles);
        assertEquals("Line wasnt deleted from deleted dirs", keep, loadFile);
    }

    @Test
    public void testDeletingEmptyDeletedDirs() throws IOException {
        File deletedFiles = new File(setup.getLocalDeploy(), ".deleted_dirs");

        String data = "1 .newtestfolder\n" + "2 .newtestfolder\n";

        writeFile(deletedFiles, data);

        localCachedDirs.refresh();

        assertTrue("Deleted dirs file was not deleted", !deletedFiles.exists());
    }

    @Test
    public void testRemoveExistingDirectoryEntries() throws IOException {
        File deletedFiles = new File(setup.getLocalDeploy(), ".deleted_dirs");
        setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");

        // a directory that was deleted and it stays that way
        String keep = System.currentTimeMillis() + " mailboxes/testmailbox1/.newtestfolder2\n";

        String data = System.currentTimeMillis() + " mailboxes/testmailbox1/.newtestfolder\n" + keep;

        writeFile(deletedFiles, data);

        localCachedDirs.refresh();

        String loadFile = TestUtils.loadFile(deletedFiles);

        assertEquals("Line wasnt deleted from deleted dirs", keep, loadFile);
    }

    private void createTypical(MercuryTestSyncSetup setup) throws IOException {
        setup.getServerDirSetup().createFolder("testmailbox1", ".testfolder1");
        setup.getServerDirSetup().createFolder("testmailbox2", ".testfolder2");

        setup.getLocalDirSetup().createFolder("testmailbox1", ".testfolder1");
        setup.getLocalDirSetup().createFolder("testmailbox2", ".testfolder2");
//
//        setup.getServerDirSetup().createMessage("testmailbox1", ".testfolder1", "new", null, lastSyncedTime + 1000);
//        setup.getServerDirSetup().createMessage("testmailbox1", ".testfolder1", "cur", null, lastSyncedTime + 3000);
//        setup.getServerDirSetup().createMessage("testmailbox1", ".testfolder1", "del", null, lastSyncedTime + 5000);
//
//        setup.getServerDirSetup().createMessage("testmailbox2", ".testfolder2", "cur", null, lastSyncedTime + 1000);
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
    }
}
