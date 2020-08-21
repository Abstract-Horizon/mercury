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

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.abstracthorizon.mercury.sync.MercuryTestSyncSetup.dirLine;
import static org.abstracthorizon.mercury.sync.MercuryTestSyncSetup.listLine;
import static org.abstracthorizon.mercury.sync.MercuryTestSyncSetup.sleep1ms;
import static org.abstracthorizon.mercury.sync.TestUtils.loadFile;
import static org.junit.Assert.assertArrayEquals;
//import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.abstracthorizon.mercury.sync.cachedir.CachedDirs;
import org.abstracthorizon.mercury.sync.client.SyncClient.RemoteFile;
import org.junit.Test;

public class TestSyncClient {

    @Test
    public void testDir() throws IOException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
            File folder1 = setup.getServerDirSetup().createFolder("testmailbox1", ".testfolder1");
            File folder2 = setup.getServerDirSetup().createFolder("testmailbox2", ".testfolder2");
            File mailbox1 = folder1.getParentFile();
            File mailbox2 = folder2.getParentFile();

            setup.getServerCachedDirs().refresh();

            CachedDirs cachedDirs = setup.getSyncClient().dir();

            List<String> dirs = cachedDirs.toStringList();


            File root = setup.getServerDirSetup().getRoot();
            File mailboxes = new File(root, "mailboxes");
            assertEquals(asList(new String[] {
                    dirLine(new File(root, "config")),
                    dirLine(mailboxes),
                    dirLine(mailboxes, mailbox1),
                    dirLine(mailboxes, folder1),
                    dirLine(mailboxes, new File(folder1, "cur")),
                    dirLine(mailboxes, new File(folder1, "del")),
                    dirLine(mailboxes, new File(folder1, "new")),
                    dirLine(mailboxes, new File(folder1, "tmp")),
                    dirLine(mailboxes, mailbox2),
                    dirLine(mailboxes, folder2),
                    dirLine(mailboxes, new File(folder2, "cur")),
                    dirLine(mailboxes, new File(folder2, "del")),
                    dirLine(mailboxes, new File(folder2, "new")),
                    dirLine(mailboxes, new File(folder2, "tmp")) }), dirs);

        } finally {
            setup.cleanup();
        }
    }

    @Test
    public void testDirWithDeletedFolders() throws IOException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
            File folder1 = setup.getServerDirSetup().createFolder("testmailbox1", ".testfolder1");
            File folder2 = setup.getServerDirSetup().createFolder("testmailbox2", ".testfolder2");
            File mailbox1 = folder1.getParentFile();
            File mailbox2 = folder2.getParentFile();

            File deletedFiles = new File(mailbox1.getParentFile().getParentFile(), ".deleted_dirs");
            TestUtils.writeFile(deletedFiles, "1 deleted\n" + "2 deleted/subdir1\n");

            setup.getServerCachedDirs().refresh();

            CachedDirs cachedDirs = setup.getSyncClient().dir();

            List<String> dirs = cachedDirs.toStringList();

            File root = setup.getServerDirSetup().getRoot();
            File mailboxes = new File(root, "mailboxes");

            assertEquals(asList(new String[] {
                    dirLine(new File(root, "config")),
                    "-1 deleted", "-2 deleted/subdir1",
                    dirLine(mailboxes),
                    dirLine(mailboxes, mailbox1),
                    dirLine(mailboxes, folder1),
                    dirLine(mailboxes, new File(folder1, "cur")),
                    dirLine(mailboxes, new File(folder1, "del")),
                    dirLine(mailboxes, new File(folder1, "new")),
                    dirLine(mailboxes, new File(folder1, "tmp")),
                    dirLine(mailboxes, mailbox2),
                    dirLine(mailboxes, folder2),
                    dirLine(mailboxes, new File(folder2, "cur")),
                    dirLine(mailboxes, new File(folder2, "del")),
                    dirLine(mailboxes, new File(folder2, "new")),
                    dirLine(mailboxes, new File(folder2, "tmp")) }), dirs);

        } finally {
            setup.cleanup();
        }
    }

    @Test
    public void testList() throws IOException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
            setup.getServerDirSetup().createMessage("testmailbox1", ".testfolder1", "new", null);
            File msg2 = setup.getServerDirSetup().createMessage("testmailbox2", ".testfolder2", "new", null);
            File msg3 = setup.getServerDirSetup().createMessage("testmailbox2", ".testfolder2", "new", null);

            setup.getServerCachedDirs().refresh();

            List<RemoteFile> list = setup.getSyncClient().list(0, setup.getServerCachedDirs().forPath("mailboxes/testmailbox2/.testfolder2/new")).stream()
                    .map(f -> new RemoteFile(f.lastModified(), f.length(), "", f.getName())).collect(toList());

            assertArrayEquals(new RemoteFile[] {listLine(msg2), listLine(msg3)}, list.toArray());
        } finally {
            setup.cleanup();
        }
    }

    @Test
    public void testGet() throws IOException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
            setup.getServerDirSetup().createMessage("testmailbox1", ".testfolder1", "new", null);
            File msg2 = setup.getServerDirSetup().createMessage("testmailbox2", ".testfolder2", "new", null);
            setup.getServerDirSetup().createMessage("testmailbox2", ".testfolder2", "new", null);

            setup.getServerCachedDirs().refresh();

            File remoteFile = File.createTempFile("downloaded-file", ".msg");
            remoteFile.deleteOnExit();
            try {
                setup.getSyncClient().download("mailboxes/testmailbox2/.testfolder2/new/" + msg2.getName(), remoteFile);
                String localContent = loadFile(msg2);
                String remoteContent = loadFile(remoteFile);

                assertEquals(localContent, remoteContent);
            } finally {
                remoteFile.delete();
            }
        } finally {
            setup.cleanup();
        }
    }

    @Test
    public void testPut() throws IOException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
            File msg1 = setup.getServerDirSetup().createMessage("testmailbox1", ".testfolder1", "new", null);
            File msg2 = setup.getServerDirSetup().createMessage("testmailbox2", ".testfolder2", "new", null);
            setup.getServerDirSetup().createMessage("testmailbox2", ".testfolder2", "new", null);

            setup.getServerCachedDirs().refresh();

            setup.getSyncClient().upload("mailboxes/testmailbox1/.testfolder1/new/" + msg2.getName(), msg2);

            File uploadedFile = new File(msg1.getParentFile(), msg2.getName());
            assertTrue("Cannot find uploaded file " + uploadedFile.getAbsolutePath(), uploadedFile.exists());

            String originalContent = loadFile(msg2);
            String uploadedContent = loadFile(uploadedFile);

            assertEquals(originalContent, uploadedContent);
        } finally {
            setup.cleanup();
        }
    }

    @Test
    public void testMove() throws IOException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
            long time1 = System.currentTimeMillis() - 20000;
            long time2 = System.currentTimeMillis() - 10000;

            File msg1 = setup.getServerDirSetup().createMessage("testmailbox1", ".testfolder1", "new", null);
            msg1.setLastModified(time1);

            String originalContent = loadFile(msg1);

            setup.getServerCachedDirs().refresh();

            setup.getSyncClient().move("mailboxes/testmailbox1/.testfolder1/new/" + msg1.getName(), "mailboxes/testmailbox1/.testfolder1/new/1234567890", time2);

            File movedFile = new File(msg1.getParentFile(), "1234567890");
            assertTrue("Cannot find uploaded file " + movedFile.getAbsolutePath(), movedFile.exists());

            String uploadedContent = loadFile(movedFile);

            assertEquals(originalContent, uploadedContent);
        } finally {
            setup.cleanup();
        }
    }

    @Test
    public void testDelete() throws IOException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
            setup.getServerDirSetup().createMessage("testmailbox1", ".testfolder1", "new", null);
            File msg2 = setup.getServerDirSetup().createMessage("testmailbox1", ".testfolder1", "new", null);

            setup.getServerCachedDirs().refresh();

            assertTrue("Setup is incorrect, cannot find created message " + msg2.getAbsolutePath(), msg2.exists());

            setup.getSyncClient().delete("mailboxes/testmailbox1/.testfolder1/new/" + msg2.getName(), System.currentTimeMillis());

            assertFalse("Delete didn't work, message is still present " + msg2.getAbsolutePath(), msg2.exists());
        } finally {
            setup.cleanup();
        }
    }

    @Test
    public void testTouch() throws IOException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
            File folder = setup.getServerDirSetup().createFolder("testmailbox1", ".testfolder1");

            setup.getServerCachedDirs().refresh();

            long newModified = (folder.lastModified() / 1000) * 1000 + 1000;

            setup.getSyncClient().touch("mailboxes/testmailbox1/.testfolder1", newModified);

            sleep1ms();

            assertEquals("Last modified date not what expected", newModified, folder.lastModified());
        } finally {
            setup.cleanup();
        }
    }

    @Test
    public void testMkdir() throws IOException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();

            setup.getServerCachedDirs().refresh();

            String path = "mailboxes/testmailbox1/.testfolder1";
            File file = new File(setup.getServerDirSetup().getMailboxes().getParent(), path);

            assertTrue("Setup is incorrect, folder " + file.getPath() + " exists", !file.exists());

            setup.getSyncClient().mkdir("mailboxes/testmailbox1", System.currentTimeMillis());
            setup.getSyncClient().mkdir(path, System.currentTimeMillis());

            assertTrue("Setup is incorrect, folder " + file.getPath() + " has not been created", file.exists() && file.isDirectory());
        } finally {
            setup.cleanup();
        }
    }

    @Test
    public void testRmdir() throws IOException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();

            String path = "mailboxes/testmailbox1/.testfolder1";

            File folder = setup.getServerDirSetup().createFolder("testmailbox1", ".testfolder1");
            setup.getServerCachedDirs().refresh();

            assertTrue("Setup is incorrect, folder " + folder.getPath() + " does not exist", folder.exists());

            setup.getSyncClient().rmdir(path);

            assertTrue("Setup is incorrect, folder " + folder.getPath() + " has not been deleted", !folder.exists());
        } finally {
            setup.cleanup();
        }
    }

    @Test
    public void testExists() throws IOException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
            setup.getServerDirSetup().createMessage("testmailbox1", ".testfolder1", "new", null);
            File msg2 = setup.getServerDirSetup().createMessage("testmailbox2", ".testfolder2", "new", null);
            setup.getServerDirSetup().createMessage("testmailbox2", ".testfolder2", "new", null);

            setup.getServerCachedDirs().refresh();

            File remoteFile = File.createTempFile("downloaded-file", ".msg");
            remoteFile.deleteOnExit();
            try {
                RemoteFile exists = setup.getSyncClient().exists("mailboxes/testmailbox2/.testfolder2/new/" + msg2.getName());

                assertTrue("File should exist, but returned that it doesn't", exists != null);
            } finally {
                remoteFile.delete();
            }
        } finally {
            setup.cleanup();
        }
    }

}
