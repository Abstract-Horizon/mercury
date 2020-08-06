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

/**
 * Sync removed cached dir
 *
 * @author Daniel Sendula
 */
public class RemovedCachedDir extends AbstractSubdirCachedDir {

    private long removedTimestamp;

    public RemovedCachedDir(CachedDirs parent, String path, long removedTimestamp) {
        super(parent, path);
        this.removedTimestamp = removedTimestamp;
    }

    @Override
    public long getLastModified() {
        return removedTimestamp;
    }

    @Override
    public void setLastModified(long lastModified) {
        removedTimestamp = lastModified;
    }


    @Override
    public boolean isRemoved() {
        return true;
    }

    @Override
    public RemovedCachedDir addSubdir(String dirname) throws IOException {
        String newPath = CachedDirs.addPaths(getPath(), dirname);

        RemovedCachedDir subdir = new RemovedCachedDir(getParent(), newPath, 0);
        subDirs.put(dirname, subdir);
        return subdir;
    }

    @Override
    public File getFile(String filename) {
        return null;
    }

    @Override
    public File[] listFilesAfter(long lastModified) {
        return new File[0];
    }

    @Override
    public long getCreated() {
        return removedTimestamp;
    }

}
