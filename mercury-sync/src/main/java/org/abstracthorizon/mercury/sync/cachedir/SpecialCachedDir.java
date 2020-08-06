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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Sync special files cached dir
 *
 * @author Daniel Sendula
 */
public class SpecialCachedDir extends CachedDir {

    private List<File> files;

    public SpecialCachedDir(CachedDirs parent, String path, List<File> files) {
        super(parent, path);
        this.files = files;
    }

    @Override
    public void setLastModified(long lastModified) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLastModified() {
        long lastModified = 0;
        for (File f : files) {
            if (f.lastModified() > lastModified) {
                lastModified = f.lastModified();
            }
        }

        return lastModified;
    }

    @Override
    public long getCreated() {
        long created = 0;
        for (File f : files) {
            long fcreated;
            try {
                BasicFileAttributes fatr = Files.readAttributes(f.toPath(),
                        BasicFileAttributes.class);
                fcreated = fatr.creationTime().toMillis() / 1000;
            } catch (IOException e) {
                fcreated = f.lastModified();
                e.printStackTrace();
            }
            if (fcreated < created) {
                created = fcreated;
            }
        }

        return created;
    }

    @Override
    public File[] listFilesAfter(final long lastModified) {
        List<File> modified = new ArrayList<File>();
        for (File f : files) {
            if (f.lastModified() > lastModified) {
                modified.add(f);
            }
        }

        File[] result = modified.toArray(new File[modified.size()]);
        return result;
    }

    @Override
    public void clear() {
    }

    @Override
    public File getFile(String filename) {
        for (File f : files) {
            if (f.getName().equals(filename)) {
                return f;
            }
        }
        return null;
    }


    public CachedDir addSubdir(String dirname, boolean isNew) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected List<CachedDir> findUpdatedDirsRecursively(List<CachedDir> updatedDirs) {
        return updatedDirs;
    }


}