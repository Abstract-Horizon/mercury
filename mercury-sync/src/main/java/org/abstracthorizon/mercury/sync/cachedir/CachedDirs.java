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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Sync cached dirs
 *
 * @author Daniel Sendula
 */
public class CachedDirs {

    private CachedDir root;
    private long cachePeriod = 10000; // 2 seconds
    private long lastScanned = 0;

    public CachedDirs() {
    }

    public CachedDirs(File root) {
        this.setRootFile(root);
    }

    public CachedDir forPath(String path) throws FileNotFoundException {
        CachedDir current = getRoot();
        String[] pathComponents = path.split("/");
        String pathSoFar = "";
        for (String p : pathComponents) {
            pathSoFar = addPaths(pathSoFar, p);

            current = current.getSubdir(p);
            if (current == null) {
                throw new FileNotFoundException("Failed to find " + p + " in " + pathSoFar);
            }
        }
        return current;
    }

    public void refresh() {
        if (lastScanned + cachePeriod < System.currentTimeMillis()) {
            forceRefresh();
        }
    }

    public void forceRefresh() {
        getRoot().findUpdatedDirsRecursively(new ArrayList<CachedDir>());

        File file = getRoot().getFile(".deleted_dirs");
        if (file != null && file.exists()) {
            List<String> linesToRemove = parseDeletedDirsFile(file);
            // TODO remove all older than 1 month and those that are 're-created' (exist in above)
            // Remove .deleted_dirs if empty after above operations
            try {
                deleteLinesFromFile(file, linesToRemove);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        lastScanned = System.currentTimeMillis();
    }

    /**
     * @param file
     * @return
     */
    private List<String> parseDeletedDirsFile(File file) {

        List<String> linesToRemove = new ArrayList<String>();
        try {
            try (FileInputStream fis = new FileInputStream(file)) {
                InputStreamReader isr = new InputStreamReader(fis);
                try {
                    BufferedReader reader = new BufferedReader(isr);
                    try {
                        String line = reader.readLine();
                        while (line != null) {
                            boolean delete = parseDeletedDirsLine(line);
                            if (delete) {
                                linesToRemove.add(line);
                            }
                            line = reader.readLine();
                        }
                    } finally {
                        reader.close();
                    }
                } finally {
                    isr.close();
                }
            }
        } catch (IOException ignore) {
            // Is this right?
        }
        return linesToRemove;
    }

    /**
     * @param line
     * @return boolean for whether to keep the line, true=keep false=delete
     */
    private boolean parseDeletedDirsLine(String line) {
        try (Scanner scanner = new Scanner(line)) {
            scanner.useDelimiter(" ");
            if (!scanner.hasNextLong()) {
                // Cannot parse incorrect line, so delete
                return true;
            }
            long lastModified = scanner.nextLong();

            String path = scanner.nextLine();
            if (path.startsWith(" ")) {
                path = path.substring(1);
            }

            String currentPath = "";
            String[] pathElements = path.split("/");
            CachedDir previousDir = getRoot();

            boolean created = false;

            int i = 0;
            while (i < pathElements.length) {
                currentPath = addPaths(currentPath, pathElements[i]);
                CachedDir cachedDir = previousDir.getSubdir(pathElements[i]);
                if (cachedDir == null) {
                    cachedDir = new RemovedCachedDir(this, currentPath, -lastModified * 1000);
                    previousDir.addSubdir(cachedDir);
                    created = true;
                }
                previousDir = cachedDir;
                i++;
            }

            if (!created) {
                return true;
            }

            long age = (System.currentTimeMillis() / 1000) - lastModified;
            long month = 60l * 60l * 24l * 30l;
            return age > month;
        }
    }

    private void deleteLinesFromFile(File file, List<String> lines) throws IOException {
        List<String> newLines = Files.lines(file.toPath()).filter(line -> !lines.contains(line)).collect(Collectors.toList());

        if (newLines.isEmpty()) {
            file.delete();
        } else {
            Files.write(file.toPath(), newLines, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        }

    }

    public long getCachePeriod() {
        return cachePeriod;
    }

    public void setCachePeriod(long cachePeriod) {
        this.cachePeriod = cachePeriod;
    }

    public CachedDir getRoot() {
        return root;
    }

    public void setRoot(CachedDir root) {
        this.root = root;
    }

    public void setRootFile(File file) {
        setRoot(new MailboxesCachedDir(this, file));
    }

    public File getRootFile() {
        return ((MailboxesCachedDir) root).getFile();
    }

    public List<String> toStringList() {
        return getRoot().toStringList("", new ArrayList<String>());
    }

    public void setSpecialFiles(String specialFiles) {
        String[] filenames = specialFiles.split(",");
        List<File> files = new ArrayList<File>();
        for (String filename : filenames) {
            files.add(new File(filename.trim()));
        }
        SpecialCachedDir specialDir = new SpecialCachedDir(this, "config", files);
        root.addSubdir(specialDir);
    }

    public static String addPaths(String path1, String path2) {
        if (path1 == null || "".equals(path1)) {
            return path2;
        }
        return path1 + "/" + path2;
    }
}
