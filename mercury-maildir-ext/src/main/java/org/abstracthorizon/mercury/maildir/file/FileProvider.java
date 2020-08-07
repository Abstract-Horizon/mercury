/*
 * Copyright (c) 2005-2020 Creative Sphere Limited.
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
package org.abstracthorizon.mercury.maildir.file;

import java.io.File;
import java.io.IOException;

/**
 * Interface needed for <code>SharedInputStreamImpl</code> objects.
 *
 * @author Daniel Sendula
 */
public interface FileProvider {

    /**
     * Returns file.
     * @return file
     * @throws IOException
     */
    File getFile() throws IOException;

    /**
     * Returns file size or -1 if not known
     * @return file size or -1 if not known
     */
    long getFileSize();

}
