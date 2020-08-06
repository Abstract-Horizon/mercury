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
import java.util.List;

/**
 * Sync remode representation of cached dir
 *
 * @author Daniel Sendula
 */
public class RemoteCachedDir extends AbstractSubdirCachedDir {
    private long lastModified;
    private long created;

    public RemoteCachedDir(CachedDirs parent, String path, long lastModified) {
        super(parent, path);
        this.lastModified = lastModified;

    }

    @Override
    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public long getLastModified() {
        return lastModified;
    }

    @Override
    public File[] listFilesAfter(final long lastModified) {
        throw new UnsupportedOperationException();
    }

    @Override
    public File getFile(String filename) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RemoteCachedDir addSubdir(String dirname) {
        String newPath = CachedDirs.addPaths(getPath(), dirname);
        RemoteCachedDir subdir = new RemoteCachedDir(getParent(), newPath, 0L);
        subDirs.put(dirname, subdir);
        return subdir;
    }

    @Override
    protected List<CachedDir> findUpdatedDirsRecursively(List<CachedDir> updatedDirs) {
        return updatedDirs;
    }

    @Override
    public long getCreated() {
        return created;
    }
}