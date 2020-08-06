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
import static org.abstracthorizon.mercury.sync.MercuryTestSyncSetup.dirLine;
import static org.abstracthorizon.mercury.sync.MercuryTestSyncSetup.listLine;
import static org.abstracthorizon.mercury.sync.MercuryTestSyncSetup.sleep1ms;
import static org.abstracthorizon.mercury.sync.TestUtils.loadFile;
//import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.abstracthorizon.mercury.sync.cachedir.CachedDirs;
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

            assertEquals(asList(new String[] { dirLine(mailbox1, mailbox1), dirLine(mailbox1, folder1), dirLine(mailbox1, new File(folder1, "cur")), dirLine(mailbox1, new File(folder1, "del")), dirLine(mailbox1, new File(folder1, "new")),
                    dirLine(mailbox1, new File(folder1, "tmp")), dirLine(mailbox2, mailbox2), dirLine(mailbox2, folder2), dirLine(mailbox2, new File(folder2, "cur")), dirLine(mailbox2, new File(folder2, "del")),
                    dirLine(mailbox2, new File(folder2, "new")), dirLine(mailbox2, new File(folder2, "tmp")) }), dirs);

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

            File deletedFiles = new File(mailbox1.getParentFile(), ".deleted_dirs");
            TestUtils.writeFile(deletedFiles, "1 deleted\n" + "2 deleted/subdir1\n");

            setup.getServerCachedDirs().refresh();

            CachedDirs cachedDirs = setup.getSyncClient().dir();

            List<String> dirs = cachedDirs.toStringList();

            assertEquals(asList(new String[] { "-1 deleted", "-2 deleted/subdir1", dirLine(mailbox1, mailbox1), dirLine(mailbox1, folder1), dirLine(mailbox1, new File(folder1, "cur")), dirLine(mailbox1, new File(folder1, "del")),
                    dirLine(mailbox1, new File(folder1, "new")), dirLine(mailbox1, new File(folder1, "tmp")), dirLine(mailbox2, mailbox2), dirLine(mailbox2, folder2), dirLine(mailbox2, new File(folder2, "cur")),
                    dirLine(mailbox2, new File(folder2, "del")), dirLine(mailbox2, new File(folder2, "new")), dirLine(mailbox2, new File(folder2, "tmp")) }), dirs);

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

            List<String> list = setup.getSyncClient().list(0, setup.getServerCachedDirs().forPath("testmailbox2/.testfolder2/new"));

            assertEquals(Arrays.asList(new String[] { listLine(msg2), listLine(msg3) }), list);

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
                setup.getSyncClient().download("testmailbox2/.testfolder2/new/" + msg2.getName(), remoteFile);
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

            setup.getSyncClient().upload("testmailbox1/.testfolder1/new/" + msg2.getName(), msg2);

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
    public void testDelete() throws IOException {
        MercuryTestSyncSetup setup = new MercuryTestSyncSetup();
        try {
            setup.create();
            setup.getServerDirSetup().createMessage("testmailbox1", ".testfolder1", "new", null);
            File msg2 = setup.getServerDirSetup().createMessage("testmailbox1", ".testfolder1", "new", null);

            setup.getServerCachedDirs().refresh();

            assertTrue("Setup is incorrect, cannot find created message " + msg2.getAbsolutePath(), msg2.exists());

            setup.getSyncClient().delete("testmailbox1/.testfolder1/new/" + msg2.getName(), System.currentTimeMillis());

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

            setup.getSyncClient().touch("testmailbox1/.testfolder1", newModified);

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

            String path = "testmailbox1/.testfolder1";
            File file = new File(setup.getServerDirSetup().getMailboxes(), path);

            assertTrue("Setup is incorrect, folder " + file.getPath() + " exists", !file.exists());

            setup.getSyncClient().mkdir("testmailbox1", System.currentTimeMillis());
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

            String path = "testmailbox1/.testfolder1";

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
                boolean exists = setup.getSyncClient().exists("testmailbox2/.testfolder2/new/" + msg2.getName());

                assertTrue("File should exist, but returned that it doesn't", exists);
            } finally {
                remoteFile.delete();
            }
        } finally {
            setup.cleanup();
        }
    }

}
