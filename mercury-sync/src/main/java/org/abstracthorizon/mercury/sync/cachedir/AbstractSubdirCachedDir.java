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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract cached dir that suports subdirs
 *
 * @author Daniel Sendula
 */
public abstract class AbstractSubdirCachedDir extends CachedDir {

    protected Map<String, CachedDir> subDirs = new LinkedHashMap<String, CachedDir>();

    protected AbstractSubdirCachedDir(CachedDirs parent, String path) {
        super(parent, path);
    }
    @Override
    public void clear() {
        subDirs.clear();
    }

    @Override
    public Collection<CachedDir> subdirs() {
        return subDirs.values();
    }

    @Override
    public CachedDir getSubdir(String dirname) {
        return subDirs.get(dirname);
    }

    @Override
    public void addSubdir(CachedDir cachedDir) {
        subDirs.put(cachedDir.getName(), cachedDir);
    }

    @Override
    public void removeSubdir(String dirname) throws IOException {
        subDirs.remove(dirname);
    }

    @Override
    protected List<String> toStringList(String path, List<String> resultingList) {
        List<CachedDir> cachedDirs = new ArrayList<CachedDir>(subDirs.values());
        Collections.sort(cachedDirs, new Comparator<CachedDir>() {
            @Override public int compare(CachedDir a, CachedDir b) {
                return a.getName().compareTo(b.getName());
            }
        });

        for (CachedDir subDir : cachedDirs) {
            String filename;
            if ("".equals(path)) {
                filename = subDir.getName();
            } else {
                filename = path + "/" + subDir.getName();
            }
            resultingList.add(Long.toString(subDir.getLastModified() / 1000) + " " + filename);

            subDir.toStringList(filename, resultingList);
        }
        return resultingList;
    }
}
