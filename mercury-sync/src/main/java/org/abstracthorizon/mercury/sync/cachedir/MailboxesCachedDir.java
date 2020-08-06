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

public class MailboxesCachedDir extends MailCachedDir {

    protected MailboxesCachedDir(CachedDirs parent, File root) {
        super(parent, root);
    }

    public File getFile(String filename) {
        File f = new File(getFile(), filename);
        if (f.exists()) {
            return f;
        }
        return null;
    }
}
