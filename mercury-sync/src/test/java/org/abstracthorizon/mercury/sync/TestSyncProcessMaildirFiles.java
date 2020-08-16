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
import static org.abstracthorizon.mercury.sync.MercuryDirSetup.createFile;
import static org.abstracthorizon.mercury.sync.MercuryDirSetup.testForDeletedAndDuplicates;
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

public class TestSyncProcessMaildirFiles {

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
    public void testNewFolderInRemoteToLocal() throws IOException, InterruptedException {
        setup.getServerDirSetup().createFolder("testmailbox1", ".newtestfolder");

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        testForErrors(setup);
    }

    // Remote

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

    // Remote both changed

    @Test
    public void testNewFolderBothRemoteFirst() throws IOException, InterruptedException {
        setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");
        setup.getLocalDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null, lastSyncedTime + 4000);
        File serverMsg = setup.getServerDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null, lastSyncedTime + 4000);

        serverMsg.setLastModified(lastSyncedTime + 5000);

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        testForErrors(setup);
    }

    @Test
    public void testNewToCurBothFolderRemoteLast() throws IOException, InterruptedException {
        setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");
        setup.getServerDirSetup().createFolder("testmailbox1", ".newtestfolder");
        File newMessage = setup.getLocalDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null, lastSyncedTime + 1000);
        File tempCurMessage = setup.getServerDirSetup().createMessage("testmailbox1", ".newtestfolder", "cur", null, lastSyncedTime + 1000);
        File curMessage = new File(tempCurMessage.getParent(), newMessage.getName() + ":2,S");

        tempCurMessage.delete();
        copyFile(newMessage, curMessage);

        curMessage.setLastModified(lastSyncedTime + 10000);

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        testForErrors(setup);
    }

    @Test
    public void testCurToNewBothFolderRemoteLast() throws IOException, InterruptedException {
        setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");
        setup.getServerDirSetup().createFolder("testmailbox1", ".newtestfolder");
        File curMessage = setup.getLocalDirSetup().createMessage("testmailbox1", ".newtestfolder", "cur", ":2,S", lastSyncedTime + 1000);
        File tempNewMessage = setup.getServerDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null, lastSyncedTime + 1000);
        File newMessage = new File(tempNewMessage.getParent(), baseFilename(curMessage.getName()));

        tempNewMessage.delete();
        copyFile(curMessage, newMessage);

        newMessage.setLastModified(lastSyncedTime + 10000);

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

    // Local

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
    public void testNewFolderPopulationInLocal() throws IOException, InterruptedException {
        setup.getServerDirSetup().createFolder("testmailbox1", ".newtestfolder");
        setup.getServerDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null, lastSyncedTime + 10000);
        setup.getServerDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null, lastSyncedTime + 9000);
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

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        testForErrors(setup);
    }

    // Local both changed

    @Test
    public void testNewFolderBothLocalLast() throws IOException, InterruptedException {
        setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");
        File localMsg = setup.getLocalDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null, lastSyncedTime + 4000);
        setup.getServerDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null, lastSyncedTime + 4000);

        localMsg.setLastModified(lastSyncedTime + 5000);

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        testForErrors(setup);
    }

    @Test
    public void testNewToCurFolderBothLocalLast() throws IOException, InterruptedException {
        setup.getServerDirSetup().createFolder("testmailbox1", ".newtestfolder");
        setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");
        File newMessage = setup.getServerDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null, lastSyncedTime + 4000);
        File tempCurMessage = setup.getLocalDirSetup().createMessage("testmailbox1", ".newtestfolder", "cur", null, lastSyncedTime + 40000);
        File curMessage = new File(tempCurMessage.getParent(), newMessage.getName() + ":2,S");

        tempCurMessage.delete();
        copyFile(newMessage, curMessage);

        curMessage.setLastModified(lastSyncedTime + 100000);

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        testForErrors(setup);
    }

    @Test
    public void testCurToNewFolderBothLocalLast() throws IOException, InterruptedException {
        setup.getServerDirSetup().createFolder("testmailbox1", ".newtestfolder");
        setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");
        File curMessage = setup.getServerDirSetup().createMessage("testmailbox1", ".newtestfolder", "cur", ":2,S", lastSyncedTime + 40000);
        File tempNewMessage = setup.getLocalDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null, lastSyncedTime + 40000);
        File newMessage = new File(tempNewMessage.getParent(), baseFilename(curMessage.getName()));

        tempNewMessage.delete();
        copyFile(curMessage, newMessage);

        newMessage.setLastModified(lastSyncedTime + 100000);

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

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        testForErrors(setup);
    }

    // Both changes at the same time!

    @Test
    public void testNewToCurFolderBothLocalAtSameTime() throws IOException, InterruptedException {
        setup.getServerDirSetup().createFolder("testmailbox1", ".newtestfolder");
        setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");
        File newMessage = setup.getServerDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null, lastSyncedTime + 3000);
        File tempCurMessage = setup.getLocalDirSetup().createMessage("testmailbox1", ".newtestfolder", "cur", null, lastSyncedTime + 30000);
        File curMessage = new File(tempCurMessage.getParent(), newMessage.getName() + ":2,S");

        tempCurMessage.delete();
        copyFile(newMessage, curMessage);

        curMessage.setLastModified(lastSyncedTime + 3000);

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        testForErrors(setup);
    }

    @Test
    public void testCurToNewFolderBothLocalAtSameTime() throws IOException, InterruptedException {
        setup.getServerDirSetup().createFolder("testmailbox1", ".newtestfolder");
        setup.getLocalDirSetup().createFolder("testmailbox1", ".newtestfolder");
        File curMessage = setup.getServerDirSetup().createMessage("testmailbox1", ".newtestfolder", "cur", ":2,S", lastSyncedTime + 4000);
        File tempNewMessage = setup.getLocalDirSetup().createMessage("testmailbox1", ".newtestfolder", "new", null, lastSyncedTime + 40000);
        File newMessage = new File(tempNewMessage.getParent(), baseFilename(curMessage.getName()));

        tempNewMessage.delete();
        copyFile(curMessage, newMessage);

        newMessage.setLastModified(lastSyncedTime + 4000);

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        testForErrors(setup);
    }

    // Config files

    @Test
    public void testConfigFromRemoteChangesToLocal() throws IOException, InterruptedException {
        File serverConfig = setup.getServerDirSetup().getConfig();
        File serverAccountProperties = createFile(new File(serverConfig, "account.properties"));
        File serverAccountKeystore = createFile(new File(serverConfig, "account.keystore"));

        File localConfig = setup.getLocalDirSetup().getConfig();
        File localAccountProperties = createFile(new File(localConfig, "account.properties"));
        File lcoalAccountKeystore = createFile(new File(localConfig, "account.keystore"));

        serverAccountProperties.setLastModified(lastSyncedTime + 10000);
        serverAccountKeystore.setLastModified(lastSyncedTime + 10000);

        localAccountProperties.setLastModified(lastSyncedTime - 10000);
        lcoalAccountKeystore.setLastModified(lastSyncedTime - 10000);

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        testForErrors(setup);
    }

    @Test
    public void testConfigFromLocalChangesToRemote() throws IOException, InterruptedException {
        File serverConfig = setup.getServerDirSetup().getConfig();
        File serverAccountProperties = createFile(new File(serverConfig, "account.properties"));
        File serverAccountKeystore = createFile(new File(serverConfig, "account.keystore"));

        File localConfig = setup.getLocalDirSetup().getConfig();
        File localAccountProperties = createFile(new File(localConfig, "account.properties"));
        File lcoalAccountKeystore = createFile(new File(localConfig, "account.keystore"));

        serverAccountProperties.setLastModified(lastSyncedTime - 100000);
        serverAccountKeystore.setLastModified(lastSyncedTime - 100000);

        localAccountProperties.setLastModified(lastSyncedTime + 100000);
        lcoalAccountKeystore.setLastModified(lastSyncedTime + 100000);

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        testForErrors(setup);
    }

    @Test
    public void testConfigChangedAtTheSameTime() throws IOException, InterruptedException {
        File serverConfig = setup.getServerDirSetup().getConfig();
        File serverAccountProperties = createFile(new File(serverConfig, "account.properties"));
        File serverAccountKeystore = createFile(new File(serverConfig, "account.keystore"));

        File localConfig = setup.getLocalDirSetup().getConfig();
        File localAccountProperties = createFile(new File(localConfig, "account.properties"));
        File lcoalAccountKeystore = createFile(new File(localConfig, "account.keystore"));

        serverAccountProperties.setLastModified(lastSyncedTime + 10000);
        serverAccountKeystore.setLastModified(lastSyncedTime + 10000);

        localAccountProperties.setLastModified(lastSyncedTime + 10000);
        lcoalAccountKeystore.setLastModified(lastSyncedTime + 10000);

        syncConnectionHandler.syncWith("localhost", setup.getPort());

        testForErrors(setup);
    }

    private void testForErrors(MercuryTestSyncSetup setup) throws IOException {
        List<String> result = compareRecursively(setup.getServerDeploy(), setup.getLocalDeploy(), new ArrayList<String>());
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
    }

    private static String baseFilename(String filename) {
        return filename.split(":")[0];
    }
}
