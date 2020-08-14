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
package org.abstracthorizon.mercury.sync.cachedir;

import static java.util.Collections.sort;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Sync maildir cached dir
 *
 * @author Daniel Sendula
 */
public class MailCachedDir extends AbstractSubdirCachedDir {
    private File file;
    private long lastModified;

    private List<File> cache;
    private long cacheTimestamp = 0;
    private long created;

    protected MailCachedDir(CachedDirs parent, File root) {
        super(parent, "");
        this.file = root;
    }

    private MailCachedDir(CachedDirs parent, String path, File file) {
        super(parent, path);
        this.file = file;
        this.lastModified = file.lastModified();

        try {
            BasicFileAttributes fatr = Files.readAttributes(file.toPath(),
                    BasicFileAttributes.class);
            this.created = fatr.creationTime().toMillis() / 1000;
        } catch (IOException e) {
            this.created = lastModified;
            e.printStackTrace();
        }

    }

    protected File getFile() {
        return file;
    }

    @Override
    public void setLastModified(long lastModified) {
        if (this.file.lastModified() != lastModified) {
            this.file.setLastModified(lastModified);
        }
        this.lastModified = lastModified;
    }

    @Override
    public long getLastModified() {
        return lastModified;
    }

    @Override
    public void removeSubdir(String dirname) throws IOException {
        CachedDir cachedDir = subDirs.get(dirname);
        if (cachedDir instanceof MailCachedDir) {
            File subdir = ((MailCachedDir) cachedDir).getFile();

            // TODO not the cleanest place to be recursively deleting a subdir
            if (subdir.exists()) {
                FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        if (exc != null) {
                            throw exc;
                        }
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                };
                Files.walkFileTree(subdir.toPath(), visitor);

                // Add to deleted dirs
                File deletedDirs = new File(getParent().getRootFile(), ".deleted_dirs");
                if (!deletedDirs.exists()) {
                    try (FileOutputStream out = new FileOutputStream(deletedDirs)) {
                    }
                }

                String line = (System.currentTimeMillis() / 1000) + " " + getPath() + "/" + dirname + "\n";
                Files.write(deletedDirs.toPath(), line.getBytes(), StandardOpenOption.APPEND);
            }
        }
        subDirs.remove(dirname);

    }

    @Override
    public MailCachedDir addSubdir(String dirname) throws IOException {
        String newPath = CachedDirs.addPaths(getPath(), dirname);
        File dirFile = new File(file, dirname);
        if (!dirFile.exists() && !dirFile.mkdirs()) {
            throw new IOException("Cannot create dir " + dirFile.getAbsolutePath());
        }
        MailCachedDir subdir = new MailCachedDir(getParent(), newPath, dirFile);
        subDirs.put(dirname, subdir);
        return subdir;
    }

    @Override
    protected List<CachedDir> findUpdatedDirsRecursively(List<CachedDir> updatedDirs) {
        Set<CachedDir> deleted = new HashSet<CachedDir>(subDirs.values());
        File[] files = listFilesAfter(0);
        if (files != null) {
            for (File f : files) {
                String filename = f.getName();
                if (!".".equals(filename) && !"..".equals(filename)) {
                    if (f.isDirectory()) {
                        CachedDir subdir;
                        if (subDirs.containsKey(filename)) {
                            subdir = subDirs.get(filename);
                            deleted.remove(subdir);
                            if (f.lastModified() != subdir.getLastModified()) {
                                // TODO this is
                                subdir.setLastModified(f.lastModified());
                                updatedDirs.add(subdir);
                            }
                        } else {
                            try {
                                subdir = addSubdir(filename);
                            } catch (IOException ignore) {
                                throw new RuntimeException(ignore);
                                // This cannot happen as file with filename always exist
                            }
                        }
                        String name = f.getName();
                        if (!"new".equals(name) && !"cur".equals(name) && !"tmp".equals(name) && !"del".equals(name)) {
                            subdir.findUpdatedDirsRecursively(updatedDirs);
                        }
                    }
                }
            }
        }
        if (!deleted.isEmpty()) {
            updatedDirs.addAll(deleted);
        }
        return updatedDirs;
    }

    @Override
    public File getFile(String filename) {
        String name = getName();
        if ("cur".equals(name) || "new".equals(name) || "del".equals(name)) {
            return new File(file, filename);
        }

        return null;
    }

    @Override
    public void addFile(File file) {
        cachedFiles().add(file);
    }

    @Override
    public boolean deleteFile(File file) {
        List<File> cachedFiles = cachedFiles();
        for (int i = 0; i < cachedFiles.size(); i++) {
            if (cachedFiles.get(i).getName().equals(file.getName())) {
                cachedFiles.remove(i);
                if (file.exists() && !file.delete()) {
                    throw new RuntimeException(new IOException("Cannot remove " + file.getAbsolutePath()));
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public File[] listFilesAfter(final long lastModified) {
        List<File> files = new ArrayList<File>();
        if (file.exists()) {
            for (File f : cachedFiles()) {
                if (f.lastModified() >= lastModified) {
                    files.add(f);
                }
            }
        }
        sort(files, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return "del".equals(f1.getName()) ? -1 : ("del".equals(f2.getName()) ? 1 : f1.getName().compareTo(f2.getName()));
            }
        });

        return files.toArray(new File[files.size()]);
    }

    private List<File> cachedFiles() {
        if (cache == null || cacheTimestamp + getParent().getCachePeriod() < System.currentTimeMillis()) {
            cache = new ArrayList<File>(Arrays.asList(file.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return !".".equals(file.getName()) && !"..".equals(file.getName());
                }
            })));
        }
        return cache;
    }

    @Override
    public long getCreated() {
        return created;
    }
}