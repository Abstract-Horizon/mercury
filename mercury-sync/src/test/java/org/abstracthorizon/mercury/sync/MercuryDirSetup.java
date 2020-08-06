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

import static org.abstracthorizon.mercury.sync.TestUtils.extractMessageName;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.abstracthorizon.mercury.sync.cachedir.CachedDirs;

public class MercuryDirSetup {

    private static Random random = new Random();

    private File tempDir;
    private File mailboxes;
    private CachedDirs cachedDirs;

    public MercuryDirSetup() {
    }

    private MercuryDirSetup(File tempDir) {
        this.tempDir = tempDir;
        this.mailboxes = new File(tempDir, "mailboxes");
    }

    public CachedDirs getCachedDirs() {
        return cachedDirs;
    }

    public void setCachedDirs(CachedDirs cachedDirs) {
        this.cachedDirs = cachedDirs;
    }

    public File getMailboxes() {
        return mailboxes;
    }

    public File create() throws IOException {
        tempDir = File.createTempFile("mercury-sync", ".test-dir");
        if (!tempDir.delete()) {
            throw new IOException("Cannot delete temp file " + tempDir.getAbsolutePath());
        }
        if (!tempDir.mkdir()) {
            throw new IOException("Cannot create " + tempDir.getAbsolutePath());
        }
        tempDir.deleteOnExit();

        mailboxes = new File(tempDir, "mailboxes");
        if (!mailboxes.mkdir()) {
            throw new IOException("Cannot create " + mailboxes.getAbsolutePath());
        }

        return tempDir;
    }

    public MercuryDirSetup duplicate() throws IOException {
        File duplicateTempDir = File.createTempFile("mercury-sync", ".test-dir");
        if (!duplicateTempDir.delete()) {
            throw new IOException("Cannot delete temp file " + duplicateTempDir.getAbsolutePath());
        }
        if (!duplicateTempDir.mkdir()) {
            throw new IOException("Cannot create " + duplicateTempDir.getAbsolutePath());
        }
        duplicateTempDir.deleteOnExit();

        copyRecursively(tempDir, duplicateTempDir);

        return new MercuryDirSetup(duplicateTempDir);
    }

    public File createMailbox(String mailbox) throws IOException {
        File mailboxDir = new File(mailboxes, mailbox);
        if (!mailboxDir.exists() && !mailboxDir.mkdir()) {
            throw new IOException("Cannot create mailbox " + mailboxDir.getAbsolutePath());
        }
        return mailboxDir;
    }

    public File createFolder(String mailbox, String folder) throws IOException {
        File mailboxDir = createMailbox(mailbox);

        File folderDir = new File(mailboxDir, folder);
        if (!folderDir.exists() && !folderDir.mkdir()) {
            throw new IOException("Cannot create folder " + folderDir.getAbsolutePath());
        }
        sleep1ms();

        File curDir = new File(folderDir, "cur");
        if (!curDir.exists() && !curDir.mkdir()) {
            throw new IOException("Cannot create folder's dir " + curDir.getAbsolutePath());
        }
        sleep1ms();

        File newDir = new File(folderDir, "new");
        if (!newDir.exists() && !newDir.mkdir()) {
            throw new IOException("Cannot create folder's dir " + newDir.getAbsolutePath());
        }
        sleep1ms();

        File tmpDir = new File(folderDir, "tmp");
        if (!tmpDir.exists() && !tmpDir.mkdir()) {
            throw new IOException("Cannot create folder's dir " + tmpDir.getAbsolutePath());
        }
        sleep1ms();

        File delDir = new File(folderDir, "del");
        if (!delDir.exists() && !delDir.mkdir()) {
            throw new IOException("Cannot create folder's dir " + delDir.getAbsolutePath());
        }

        return folderDir;
    }

    public File createMessage(String mailbox, String folder, long time) throws IOException {
        return createMessage(mailbox, folder, "cur", null, time);
    }

    public File createMessage(String mailbox, String folder) throws IOException {
        return createMessage(mailbox, folder, "cur", null, System.currentTimeMillis());
    }

    public File createMessage(String mailbox, String folder, String dirName, String flags) throws IOException {
        return createMessage(mailbox, folder, dirName, flags, System.currentTimeMillis());
    }

    public File createMessage(String mailbox, String folder, String dirName, String flags, long time) throws IOException {
        File folderDir = createFolder(mailbox, folder);
        File dir;
        if (dirName == null) {
            dir = new File(folderDir, "cur");
        } else {
            dir = new File(folderDir, dirName);
        }
        if (!dir.exists() && !dir.mkdir()) {
            throw new IOException("Cannot create folder's dir " + dir.getAbsolutePath());
        }

        sleep1ms();
        String messageName = time + ".V1U2.test";
        if (flags != null && "".equals(flags)) {
            messageName = messageName + ":" + flags;
        }

        File messageFile = new File(dir, messageName);
        if (!messageFile.createNewFile()) {
            throw new IOException("Cannot create mail file " + messageFile.getAbsolutePath());
        }

        if (!"del".equals(dirName)) {
            FileOutputStream fis = new FileOutputStream(messageFile);
            try {
                for (int i = 0; i < random.nextInt(4); i++) {
                    for (int j = 0; j < random.nextInt(3); j++) {
                        fis.write(Integer.toString(random.nextInt()).getBytes());
                    }
                    fis.write(13);
                    fis.write(10);
                }
            } finally {
                fis.close();
            }
        }

        return messageFile;
    }

    public File deleteMessage(File messageFile) throws IOException {
        File currentDir = messageFile.getParentFile();
        if (currentDir.getName().contentEquals("del")) {
            return messageFile;
        }
        File folderDir = currentDir.getParentFile();
        File delDir = new File(folderDir, "del");
        if (!delDir.exists()) {
            throw new FileNotFoundException("Cannot find 'del' dir" + delDir.getAbsolutePath());
        }
        File newDeletedMessageFile = new File(delDir, extractMessageName(messageFile));
        if (!newDeletedMessageFile.createNewFile()) {
            throw new IOException("Cannot create new deleted file " + newDeletedMessageFile.getAbsolutePath());
        }
        if (!messageFile.delete()) {
            throw new IOException("Cannot delete original file from " + currentDir.getName() + "; " + messageFile.getAbsolutePath());
        }
        return newDeletedMessageFile;
    }

    public void cleanup() throws IOException {
        deleteRecursively(tempDir);
    }

    public static void deleteRecursively(File file) throws IOException {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                if (!f.getName().equals(".") && !f.getName().equals("..")) {
                    deleteRecursively(f);
                }
            }
        }
        if (!file.delete()) {
            throw new IOException("Cannot delete temp file/dir " + file.getAbsolutePath());
        }
    }

    public static void copyRecursively(File from, File to) throws IOException {
        if (from.isFile()) {
            byte[] buf = new byte[10240];
            FileInputStream fis = new FileInputStream(from);
            try {
                FileOutputStream fos = new FileOutputStream(to);
                try {
                    int r = fis.read(buf);
                    while (r > 0) {
                        fos.write(buf, 0, r);
                        r = fis.read(buf);
                    }
                } finally {
                    fos.close();
                }
            } finally {
                fis.close();
            }
            to.setLastModified(from.lastModified());
        } else {
            if (!to.exists()) {
                if (!to.mkdirs()) {
                    throw new IOException("Cannot create dir " + to.getAbsolutePath());
                }
            }
            to.setLastModified(from.lastModified());
            File[] files = from.listFiles();
            if (files != null) {
                for (File file : files) {
                    String name = file.getName();
                    if (!".".equals(name) && !"..".equals(name)) {
                        File dest = new File(to, name);
                        copyRecursively(file, dest);
                    }
                }
            }
        }
    }

    public static List<String> compareRecursively(File from, File to, List<String> result) throws IOException {
        if (from.isFile()) {
            if (!to.isFile()) {
                result.add(to.getAbsolutePath() + " is not file");
                return result;
            }
            if (to.length() != from.length()) {
                result.add(to.getAbsolutePath() + " has different size; from=" + from.length() + " to=" + to.length());
                return result;
            }
            int toModified = (int) (to.lastModified() / 1000);
            int fromModified = (int) (from.lastModified() / 1000);
            if (toModified != fromModified) {
                if (!to.getName().equals(".deleted_dirs")) {
                    result.add(to.getAbsolutePath() + " file has different modified date; from=" + fromModified + " to=" + toModified);
                    return result;
                }
            }

            byte[] bufFrom = new byte[10240];
            byte[] bufTo = new byte[10240];
            FileInputStream ffs = new FileInputStream(from);
            try {
                FileInputStream fts = new FileInputStream(to);
                try {
                    int s1 = 0;
                    int s2 = 0;
                    int r1 = ffs.read(bufFrom);
                    int r2 = fts.read(bufTo);
                    while (r1 > 0 && r2 >= 0) {
                        if (r1 != r2) {
                            result.add(to.getAbsolutePath() + " has read different len @ pos=" + s1 + "(r_from=" + r1 + " r_to=" + r2 + ")");
                            return result;
                        }
                        for (int i = 0; i < r1; i++) {
                            if (bufFrom[i] != bufTo[i]) {
                                result.add(to.getAbsolutePath() + " has read different byte @ pos=" + (s1 + i) + " from_byte=" + bufFrom[i] + " to_byte=" + bufTo[i]);
                                return result;
                            }
                        }

                        s1 = s1 + r1;
                        s2 = s2 + r2;
                        r1 = ffs.read(bufFrom);
                        r2 = fts.read(bufTo);
                    }
                    if (r1 > 0) {
                        result.add("From " + from.getAbsolutePath() + " has more data @ pos=" + s1 + "(r_from=" + r1 + " r_to=" + r2 + ")");
                        return result;
                    }
                    if (r2 > 0) {
                        result.add("To " + from.getAbsolutePath() + " has more data @ pos=" + s1 + "(r_from=" + r1 + " r_to=" + r2 + ")");
                        return result;
                    }
                } finally {
                    fts.close();
                }
            } finally {
                ffs.close();
            }
        } else {
            if (!to.isDirectory()) {
                result.add(to.getAbsolutePath() + " is not directory");
                return result;
            }

//            if (to.lastModified() != from.lastModified()) {
//                result.add(to.getAbsolutePath() + " folder has different modified date; from=" + from.lastModified() + " to=" + to.lastModified());
//                return result;
//            }

            File[] fromFiles = from.listFiles();
            File[] toFiles = to.listFiles();

            Set<String> fromSet = asSet(fromFiles);
            Set<String> toSet = asSet(toFiles);

            Set<String> intersection = new LinkedHashSet<String>(fromSet);
            intersection.retainAll(toSet);

            fromSet.removeAll(intersection);
            toSet.removeAll(intersection);

            if (fromSet.size() > 0 || toSet.size() > 0) {
                if (fromSet.size() > 0) {
                    for (String filename : fromSet) {
                        if (!filename.equals(".deleted_dirs")) {
                            result.add("From " + from.getAbsolutePath() + " has extra file " + filename);
                        }
                    }
                }
                if (toSet.size() > 0) {
                    for (String filename : toSet) {
                        if (!filename.equals(".deleted_dirs")) {
                            result.add("To " + to.getAbsolutePath() + " has extra file " + filename);
                        }
                    }
                }
                return result;
            }

            for (String name : intersection) {
                File fromFile = new File(from, name);
                File toFile = new File(to, name);

                int fromFileLastModified = (int) (fromFile.lastModified() / 1000);
                int toFileLastModified = (int) (toFile.lastModified() / 1000);

                if (toFileLastModified != fromFileLastModified && toFile.isFile()) {
                    if (!to.getName().equals(".deleted_dirs")) {
                        result.add(toFile.getAbsolutePath() + " " + (toFile.isFile() ? "file" : "folder") + " has different modified date; from=" + fromFileLastModified + " to=" + toFileLastModified);
                        return result;
                    }
                }
                if (!name.equals(".deleted_dirs")) {
                    compareRecursively(fromFile, toFile, result);
                }
                if (result.size() > 0) {
                    return result;
                }
            }
        }
        return result;
    }

    private static Set<String> asSet(File[] files) {
        Set<String> res = new LinkedHashSet<String>();
        for (File file : files) {
            String name = file.getName();
            if (!".".equals(name) && !"..".equals(name)) {
                res.add(name);
            }
        }
        return res;
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
