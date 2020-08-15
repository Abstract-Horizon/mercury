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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestSyncProcess {

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
        localCachedDirs.setRootFile(setup.getLocalMailboxes());

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
    public void testNoChanges() throws IOException, InterruptedException {
        syncConnectionHandler.syncWith("localhost", setup.getPort());

        testForErrors(setup);
    }

    @Test
    public void testFromRemoteChangesToLocal() throws IOException, InterruptedException {
        setup.getServerDirSetup().createMessage("testmailbox1", ".testfolder1", "new", null, lastSyncedTime + 10000);
        syncConnectionHandler.syncWith("localhost", setup.getPort());

        testForErrors(setup);
    }

    @Test
    public void testFromLocalChangesToRemote() throws IOException, InterruptedException {
        setup.getLocalDirSetup().createMessage("testmailbox1", ".testfolder1", "new", null, lastSyncedTime + 10000);
        syncConnectionHandler.syncWith("localhost", setup.getPort());

        testForErrors(setup);
    }

    @Test
    public void testDeletedNewFromRemote() throws IOException, InterruptedException {
        File localMessage = setup.getLocalDirSetup().createMessage("testmailbox1", ".testfolder1", "new", "2,S", lastSyncedTime - 10000);
        File remoteMessage = setup.getServerDirSetup().createMessage("testmailbox1", ".testfolder1", "new", "2,S", lastSyncedTime - 10000);

        setup.getServerDirSetup().deleteMessage(remoteMessage).setLastModified(lastSyncedTime + 8000);
        assertTrue("Remote message still exists", !remoteMessage.exists());

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        testForErrors(setup);
        assertTrue("Local message still exists", !localMessage.exists());
    }

    @Test
    public void testDeletedNewFromLocal() throws IOException, InterruptedException {
        File localMessage = setup.getLocalDirSetup().createMessage("testmailbox1", ".testfolder1", "new", "2,S", lastSyncedTime - 10000);
        File remoteMessage = setup.getServerDirSetup().createMessage("testmailbox1", ".testfolder1", "new", "2,S", lastSyncedTime - 10000);

        setup.getLocalDirSetup().deleteMessage(localMessage).setLastModified(lastSyncedTime + 8000);
        assertTrue("Local message still exists", !localMessage.exists());

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        testForErrors(setup);
        assertTrue("Remote message still exists", !remoteMessage.exists());
    }

    @Test
    public void testDeletedCurFromRemote() throws IOException, InterruptedException {
        File localMessage = setup.getLocalDirSetup().createMessage("testmailbox1", ".testfolder1", "cur", "2,S", lastSyncedTime - 10000);
        File remoteMessage = setup.getServerDirSetup().createMessage("testmailbox1", ".testfolder1", "cur", "2,S", lastSyncedTime - 10000);

        setup.getServerDirSetup().deleteMessage(remoteMessage).setLastModified(lastSyncedTime + 8000);

        assertTrue("Remote message still exists", !remoteMessage.exists());

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        testForErrors(setup);
        assertTrue("Local message still exists", !localMessage.exists());
    }

    @Test
    public void testDeletedCurFromLocal() throws IOException, InterruptedException {
        File localMessage = setup.getLocalDirSetup().createMessage("testmailbox1", ".testfolder1", "cur", "2,S", lastSyncedTime - 10000);
        File remoteMessage = setup.getServerDirSetup().createMessage("testmailbox1", ".testfolder1", "cur", "2,S", lastSyncedTime - 10000);

        setup.getLocalDirSetup().deleteMessage(localMessage).setLastModified(lastSyncedTime + 8000);
        assertTrue("Local message still exists", !localMessage.exists());

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        testForErrors(setup);
        assertTrue("Remote message still exists", !remoteMessage.exists());
    }

    @Test
    public void testNewFolderInRemoteToLocal() throws IOException, InterruptedException {
        setup.getServerDirSetup().createFolder("testmailbox1", ".newtestfolder");

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        testForErrors(setup);
    }

    @Test
    public void testNewFolderInLocalToRemote() throws IOException, InterruptedException {
        setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        testForErrors(setup);
    }

    @Test
    public void testNewFolderPopulationInRemote() throws IOException, InterruptedException {
        setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");
        setup.getLocalDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null, lastSyncedTime + 10000);
        setup.getLocalDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null, lastSyncedTime + 9000);

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        testForErrors(setup);
    }

    @Test
    public void testNewFolderPopulationInLocal() throws IOException, InterruptedException {
        setup.getServerDirSetup().createFolder("testmailbox1", ".newtestfolder");
        setup.getServerDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null, lastSyncedTime + 10000);
        setup.getServerDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null, lastSyncedTime + 9000);
        syncConnectionHandler.syncWith("localhost", setup.getPort());

        testForErrors(setup);
    }

    @Test
    public void testNewToCurFolderRemote() throws IOException, InterruptedException {
        setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");
        setup.getServerDirSetup().createFolder("testmailbox1", ".newtestfolder");
        File newMessage = setup.getLocalDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null, lastSyncedTime - 10000);
        File tempCurMessage = setup.getServerDirSetup().createMessage("testmailbox1", ".newtestfolder", "cur", null, lastSyncedTime - 10000);
        File curMessage = new File(tempCurMessage.getParent(), newMessage.getName() + ":2,S");

        tempCurMessage.delete();
        copyFile(newMessage, curMessage);

        curMessage.setLastModified(lastSyncedTime + 10000);

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        testForErrors(setup);
    }

    @Test
    public void testCurToNewFolderRemote() throws IOException, InterruptedException {
        setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");
        setup.getServerDirSetup().createFolder("testmailbox1", ".newtestfolder");
        File curMessage = setup.getLocalDirSetup().createMessage("testmailbox1", ".newtestfolder", "cur", ":2,S", lastSyncedTime - 10000);
        File tempNewMessage = setup.getServerDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null, lastSyncedTime - 10000);
        File newMessage = new File(tempNewMessage.getParent(), baseFilename(curMessage.getName()));

        tempNewMessage.delete();
        copyFile(curMessage, newMessage);

        newMessage.setLastModified(lastSyncedTime + 10000);

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        testForErrors(setup);
    }

    @Test
    public void testNewToNewAndCurToCurFolderRemote() throws IOException, InterruptedException {
        setup.getServerDirSetup().createFolder("testmailbox1", ".newtestfolder");
        setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");
        File serverNewMessage = setup.getServerDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null, lastSyncedTime - 100000);
        File serverCurMessage = setup.getServerDirSetup().createMessage("testmailbox1", ".newtestfolder", "cur", ":2,S", lastSyncedTime - 90000);
        File localNewMessage = setup.getLocalDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null, lastSyncedTime - 100000);
        File localCurMessage = setup.getLocalDirSetup().createMessage("testmailbox1", ".newtestfolder", "cur", ":2,S", lastSyncedTime - 90000);

        copyFile(serverNewMessage, localNewMessage);
        copyFile(serverCurMessage, localCurMessage);

        localNewMessage.setLastModified(lastSyncedTime + 100000);
        localCurMessage.setLastModified(lastSyncedTime + 100000);

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        testForErrors(setup);
    }

    @Test
    public void testNewToNewAndCurToCurBothFolderRemoteLast() throws IOException, InterruptedException {
        setup.getServerDirSetup().createFolder("testmailbox1", ".newtestfolder");
        setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");
        File serverNewMessage = setup.getServerDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null, lastSyncedTime - 100000);
        File serverCurMessage = setup.getServerDirSetup().createMessage("testmailbox1", ".newtestfolder", "cur", ":2,S", lastSyncedTime - 90000);
        File localNewMessage = setup.getLocalDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null, lastSyncedTime - 100000);
        File localCurMessage = setup.getLocalDirSetup().createMessage("testmailbox1", ".newtestfolder", "cur", ":2,S", lastSyncedTime - 90000);

        copyFile(serverNewMessage, localNewMessage);
        copyFile(serverCurMessage, localCurMessage);

        localNewMessage.setLastModified(lastSyncedTime + 50000);
        localCurMessage.setLastModified(lastSyncedTime + 40000);

        serverNewMessage.setLastModified(lastSyncedTime + 100000);
        serverCurMessage.setLastModified(lastSyncedTime + 90000);

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        testForErrors(setup);
    }

    @Test
    public void testNewToCurFolderLocal() throws IOException, InterruptedException {
        setup.getServerDirSetup().createFolder("testmailbox1", ".newtestfolder");
        setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");
        File newMessage = setup.getServerDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null, lastSyncedTime - 100000);
        File tempCurMessage = setup.getLocalDirSetup().createMessage("testmailbox1", ".newtestfolder", "cur", null, lastSyncedTime - 100000);
        File curMessage = new File(tempCurMessage.getParent(), newMessage.getName() + ":2,S");

        tempCurMessage.delete();
        copyFile(newMessage, curMessage);

        curMessage.setLastModified(lastSyncedTime + 100000);

        CachedDirs localCachedDirs = new CachedDirs();
        localCachedDirs.setRootFile(setup.getLocalMailboxes());

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        testForErrors(setup);
    }

    @Test
    public void testCurToNewFolderLocal() throws IOException, InterruptedException {
        setup.getServerDirSetup().createFolder("testmailbox1", ".newtestfolder");
        setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");
        File curMessage = setup.getServerDirSetup().createMessage("testmailbox1", ".newtestfolder", "cur", ":2,S", lastSyncedTime - 100000);
        File tempNewMessage = setup.getLocalDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null, lastSyncedTime - 100000);
        File newMessage = new File(tempNewMessage.getParent(), baseFilename(curMessage.getName()));

        tempNewMessage.delete();
        copyFile(curMessage, newMessage);

        newMessage.setLastModified(lastSyncedTime + 100000);

        CachedDirs localCachedDirs = new CachedDirs();
        localCachedDirs.setRootFile(setup.getLocalMailboxes());

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        testForErrors(setup);
    }

    @Test
    public void testNewToNewAndCurToCurFolderLocal() throws IOException, InterruptedException {
        setup.getServerDirSetup().createFolder("testmailbox1", ".newtestfolder");
        setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");
        File serverNewMessage = setup.getServerDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null, lastSyncedTime - 100000);
        File serverCurMessage = setup.getServerDirSetup().createMessage("testmailbox1", ".newtestfolder", "cur", ":2,S", lastSyncedTime - 90000);
        File localNewMessage = setup.getLocalDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null, lastSyncedTime - 100000);
        File localCurMessage = setup.getLocalDirSetup().createMessage("testmailbox1", ".newtestfolder", "cur", ":2,S", lastSyncedTime - 90000);

        copyFile(localNewMessage, serverNewMessage);
        copyFile(localCurMessage, serverCurMessage);

        serverNewMessage.setLastModified(lastSyncedTime + 100000);
        serverCurMessage.setLastModified(lastSyncedTime + 90000);

        CachedDirs localCachedDirs = new CachedDirs();
        localCachedDirs.setRootFile(setup.getLocalMailboxes());

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        testForErrors(setup);
    }

    @Test
    public void testNewToNewAndCurToCurFolderBothLocalLast() throws IOException, InterruptedException {
        setup.getServerDirSetup().createFolder("testmailbox1", ".newtestfolder");
        setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");
        File serverNewMessage = setup.getServerDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null, lastSyncedTime - 100000);
        File serverCurMessage = setup.getServerDirSetup().createMessage("testmailbox1", ".newtestfolder", "cur", ":2,S", lastSyncedTime - 90000);
        File localNewMessage = setup.getLocalDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null, lastSyncedTime - 100000);
        File localCurMessage = setup.getLocalDirSetup().createMessage("testmailbox1", ".newtestfolder", "cur", ":2,S", lastSyncedTime - 90000);

        copyFile(localNewMessage, serverNewMessage);
        copyFile(localCurMessage, serverCurMessage);

        serverNewMessage.setLastModified(lastSyncedTime + 50000);
        serverCurMessage.setLastModified(lastSyncedTime + 40000);

        localNewMessage.setLastModified(lastSyncedTime + 100000);
        localCurMessage.setLastModified(lastSyncedTime + 90000);

        CachedDirs localCachedDirs = new CachedDirs();
        localCachedDirs.setRootFile(setup.getLocalMailboxes());

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        testForErrors(setup);
    }

    @Test
    public void testDeletedDirRemote() throws IOException, InterruptedException {
        File localFolder = setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");
        // Set the local folder to be older than when the remote one is deleted
        localFolder.setLastModified(1);

        @SuppressWarnings("unused")
        File remoteMailbox = setup.getServerDirSetup().createMailbox("testmailbox1");

        File deletedFiles = new File(setup.getServerCachedDirs().getRootFile(), ".deleted_dirs");
        TestUtils.writeFile(deletedFiles, "100 testmailbox1/.newtestfolder\n");

        setup.getServerCachedDirs().refresh();

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        List<String> result = compareRecursively(setup.getServerMailboxes(), setup.getLocalMailboxes(), new ArrayList<String>());

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
        File deletedFiles = new File(setup.getLocalDirSetup().getMailboxes(), ".deleted_dirs");
        String string = "testmailbox1/.newtestfolder";
        TestUtils.writeFile(deletedFiles, "1000 " + string + "\n");

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        List<String> result = compareRecursively(setup.getServerMailboxes(), setup.getLocalMailboxes(), new ArrayList<String>());

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
        File deletedFiles = new File(setup.getLocalDirSetup().getMailboxes(), ".deleted_dirs");
        String string = "testmailbox1/.newtestfolder";
        TestUtils.writeFile(deletedFiles, (System.currentTimeMillis() / 1000) + " " + string + "\n");

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        assertTrue("Remote Folder was not deleted", !remoteFolder.exists());
        List<String> result = compareRecursively(setup.getServerMailboxes(), setup.getLocalMailboxes(), new ArrayList<String>());
        assertTrue("There were differences after first sync (delete) \n" + join(result), result.isEmpty());

        File newRemoteFolder = setup.getServerDirSetup().createFolder("testmailbox1", ".newtestfolder");
        newRemoteFolder.setLastModified(newRemoteFolder.lastModified() + 1000);

        setup.getServerCachedDirs().forceRefresh();

        assertTrue("Remote Folder was not created", newRemoteFolder.exists());

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        List<String> result2 = compareRecursively(setup.getServerMailboxes(), setup.getLocalMailboxes(), new ArrayList<String>());

        assertTrue("Remote Folder was re-deleted", newRemoteFolder.exists());
        assertTrue("New folder was not synced back to local", localFolder.exists());

        assertTrue("There were differences after second sync (recreate) \n" + join(result2), result2.isEmpty());
    }

    @Test
    public void testRecreatingDeletedDirInLocal() throws IOException, InterruptedException {
        File localFolder = setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");
        localFolder.setLastModified(localFolder.lastModified() - 1000);
        assertTrue("Local Folder was not created", localFolder.exists());

        @SuppressWarnings("unused")
        File remoteMailbox = setup.getServerDirSetup().createMailbox("testmailbox1");

        // simulate deleting folder
        File deletedFiles = new File(setup.getServerDirSetup().getMailboxes(), ".deleted_dirs");
        String string = "testmailbox1/.newtestfolder";
        TestUtils.writeFile(deletedFiles, (System.currentTimeMillis() / 1000) + " " + string + "\n");

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        assertTrue("Local Folder was not deleted", !localFolder.exists());
        List<String> result = compareRecursively(setup.getServerMailboxes(), setup.getLocalMailboxes(), new ArrayList<String>());
        assertTrue("There were differences after first sync (delete) \n" + join(result), result.isEmpty());

        File newLocalFolder = setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");
        newLocalFolder.setLastModified(newLocalFolder.lastModified() + 1000);

        localCachedDirs.forceRefresh();

        assertTrue("Local Folder was not created", newLocalFolder.exists());

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        List<String> result2 = compareRecursively(setup.getServerMailboxes(), setup.getLocalMailboxes(), new ArrayList<String>());

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
        File deletedDirsLocal = new File(setup.getLocalDirSetup().getMailboxes(), ".deleted_dirs");
        File deletedDirsRemote = new File(setup.getServerCachedDirs().getRootFile(), ".deleted_dirs");

        String string = "testmailbox1/.newtestfolder";
        TestUtils.writeFile(deletedDirsLocal, (System.currentTimeMillis() / 1000) + " " + string + "\n");

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        List<String> result = compareRecursively(setup.getServerMailboxes(), setup.getLocalMailboxes(), new ArrayList<String>());

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
        File deletedDirsLocal = new File(setup.getLocalDirSetup().getMailboxes(), ".deleted_dirs");

        TestUtils.writeFile(deletedDirsRemote, (System.currentTimeMillis() / 1000) + " testmailbox1/.newtestfolder\n");

        setup.getServerCachedDirs().refresh();

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        List<String> result = compareRecursively(setup.getServerMailboxes(), setup.getLocalMailboxes(), new ArrayList<String>());

        assertTrue("Got errors\n" + join(result), result.isEmpty());
        assertTrue("Local folder still exists", !localFolder.exists());

        assertTrue("Deleted Dirs remote doesnt exist", deletedDirsRemote.exists());
        assertTrue("Deleted dirs was not copied", deletedDirsLocal.exists());
    }

    @Test
    public void testRemovingExpiredDeletedDirs() throws IOException {
        File deletedFiles = new File(setup.getLocalMailboxes(), ".deleted_dirs");

        String keep = System.currentTimeMillis() + " .newtestfolder2\n" + System.currentTimeMillis() + " .newtestfolder3\n";

        String data = "1 .newtestfolder\n" + keep;
        TestUtils.writeFile(deletedFiles, data);

        localCachedDirs.refresh();

        assertTrue("Deleted dirs file was deleted", deletedFiles.exists());

        String loadFile = TestUtils.loadFile(deletedFiles);
        assertEquals("Line wasnt deleted from deleted dirs", keep, loadFile);
    }

    @Test
    public void testDeletingEmptyDeletedDirs() throws IOException {
        File deletedFiles = new File(setup.getLocalMailboxes(), ".deleted_dirs");

        String data = "1 .newtestfolder\n" + "2 .newtestfolder\n";

        TestUtils.writeFile(deletedFiles, data);

        localCachedDirs.refresh();

        assertTrue("Deleted dirs file was not deleted", !deletedFiles.exists());
    }

    @Test
    public void testRemoveExistingDirectoryEntries() throws IOException {
        File deletedFiles = new File(setup.getLocalMailboxes(), ".deleted_dirs");
        setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");

        // a directory that was deleted and it stays that way
        String keep = System.currentTimeMillis() + " testmailbox1/.newtestfolder2\n";

        String data = System.currentTimeMillis() + " testmailbox1/.newtestfolder\n" + keep;

        TestUtils.writeFile(deletedFiles, data);

        localCachedDirs.refresh();

        String loadFile = TestUtils.loadFile(deletedFiles);

        assertEquals("Line wasnt deleted from deleted dirs", keep, loadFile);
    }

    private void testForErrors(MercuryTestSyncSetup setup) throws IOException {
        List<String> result = compareRecursively(setup.getServerMailboxes(), setup.getLocalMailboxes(), new ArrayList<String>());
        testForDeletedAndDuplicates(setup.getServerDirSetup().getMailboxes(), result);
        testForDeletedAndDuplicates(setup.getLocalDirSetup().getMailboxes(), result);
        assertTrue("Got errors\n" + join(result), result.isEmpty());
    }

    private void createTypical(MercuryTestSyncSetup setup) throws IOException {
        setup.getServerDirSetup().createFolder("testmailbox1", ".testfolder1");
        setup.getServerDirSetup().createFolder("testmailbox2", ".testfolder2");

        setup.getServerDirSetup().createMessage("testmailbox1", ".testfolder1", "new", null, lastSyncedTime + 1000);
        setup.getServerDirSetup().createMessage("testmailbox1", ".testfolder1", "cur", null, lastSyncedTime + 3000);
        setup.getServerDirSetup().createMessage("testmailbox1", ".testfolder1", "del", null, lastSyncedTime + 5000);

        setup.getServerDirSetup().createMessage("testmailbox2", ".testfolder2", "cur", null, lastSyncedTime + 1000);
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

    private static String baseFilename(String filename) {
        return filename.split(":")[0];
    }
}
