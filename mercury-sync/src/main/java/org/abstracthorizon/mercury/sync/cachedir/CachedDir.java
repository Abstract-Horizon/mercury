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

import static java.util.Collections.emptyList;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * Sync Cached dir.
 *
 * @author Daniel Sendula
 */
public abstract class CachedDir {

    private CachedDirs parent;
    private String path;
    private String name;

    protected CachedDir(CachedDirs parent, String path) {
        this.parent = parent;
        this.path = path;
        int i = path.lastIndexOf('/');
        if (i >= 0) {
            name = path.substring(i + 1);
        } else {
            name = path;
        }
    }

    protected CachedDirs getParent() {
        return parent;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public abstract long getLastModified();

    public abstract long getCreated();


    public void setLastModified(long lastModified) {
        throw new UnsupportedOperationException();
    }

    public void setCreated(long created) {
        throw new UnsupportedOperationException();
    }


    public boolean isRemoved() {
        return false;
    }

    public abstract void clear();

    public Collection<CachedDir> subdirs() {
        return emptyList();
    }

    public CachedDir getSubdir(String dirname) {
        return null;
    }

    public void addSubdir(CachedDir cachedDir) {
        throw new UnsupportedOperationException();
    }

    public CachedDir addSubdir(String dirname) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void removeSubdir(String dirname) throws IOException {
        if (getSubdir(dirname) != null) {
            throw new IOException(getPath() + "/" + dirname + " cannot be removed.");
        }
    }

    protected List<CachedDir> findUpdatedDirsRecursively(List<CachedDir> updatedDirs) {
        return updatedDirs;
    }

    protected List<String> toStringList(String path, List<String> resultingList) {
        return resultingList;
    }

    public abstract File getFile(String filename);

    public void addFile(File file) {
        throw new UnsupportedOperationException();
    }

    public boolean deleteFile(File file) {
        throw new UnsupportedOperationException();
    }

    public abstract File[] listFilesAfter(final long lastModified);

    @Override
    public boolean equals(Object o) {
        if (o instanceof CachedDir) {
            CachedDir other = (CachedDir) o;

            return name.equals(other.getName()) && path.equals(other.getPath());
        } else {
            return super.equals(o);
        }
    }

    @Override
    public int hashCode() {
        return name.hashCode() + path.hashCode();
    }
}