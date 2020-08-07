/*
 * Copyright (c) 2004-2020 Creative Sphere Limited.
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
package org.abstracthorizon.mercury.common.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This is temporary storage. It stores first defined number of bytes in memory and if more are
 * written then it creates a temporary file and continues to write to the file
 *
 * @author Daniel Sendula
 */
public class TempStorage {

    /** Default maximum number of bytes to be stored in memory */
    public static final int MAX_MEMORY = 102400;

    /** Number of bytes to be stored in memory */
    protected int maxMemory = MAX_MEMORY;

    /** Temporary file */
    protected File file = null;

    /** Default output stream */
    protected OutputStream defaultOutputStream = new OutputStreamImpl();

    /** Output stream */
    protected OutputStream os = null;

    /** Buffer array output stream */
    protected ByteArrayOutputStream buffer = new ByteArrayOutputStream(MAX_MEMORY);

    /** Size */
    protected int size = 0;

    /** Prefix */
    protected String prefix = "tmp";

    /** Suffix */
    protected String suffix = ".tmp";

    /**
     * Constructor
     */
    public TempStorage() {
    }

    /**
     * Constructor
     * @param prefix file prefix
     * @param suffix file suffix
     */
    public TempStorage(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
    }

    /**
     * Constructor
     * @param prefix prefix
     * @param suffix suffix
     * @param maxMemory maximum number of bytes
     */
    public TempStorage(String prefix, String suffix, int maxMemory) {
        this(prefix, suffix);
        this.maxMemory = maxMemory;
    }

    /**
     * Returns temporary file (or null)
     * @return temporary file (or null)
     */
    public File getFile() {
        return file;
    }

    /**
     * Returns size of storage
     * @return size of storage
     */
    public int getSize() {
        return size;
    }

    /**
     * Returns default output stream
     * @return default output stream
     */
    public OutputStream getOutputStream() {
        return defaultOutputStream;
    }

    /**
     * Returns input stream (file or memory buffer)
     * @return input stream (file or memory buffer)
     * @throws IOException
     */
    public InputStream getInputStream() throws IOException {
        if (file != null) {
            return FileSharedInputStream.newInstance(file);
        } else {
            return new ByteArrayInputStream(buffer.toByteArray());
        }
    }

    /**
     * Clears storage for new use
     * @throws IOException
     */
    public void clear() throws IOException {
        if (file != null) {
            file.delete();
            os.close();
        }
        buffer = new ByteArrayOutputStream(MAX_MEMORY);
        size = 0;
    } // clear

    /**
     * Output stream implementation
     *
     * @author Daniel Sendula
     */
    protected class OutputStreamImpl extends OutputStream {

        @Override
        public void write(int b) throws IOException {
            byte[] bs = new byte[1];
            bs[0] = (byte)b;
            write(bs);
        }

        @Override
        public void write(byte b[]) throws IOException {
            write(b, 0, b.length);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (file != null) {
                os.write(b, off, len);
            } else {
                if (size + b.length > MAX_MEMORY) {
                    file = File.createTempFile(prefix, suffix);
                    file.deleteOnExit();
                    os = new FileOutputStream(file);
                    os.write(buffer.toByteArray());
                    os.write(b, off, len);
                    buffer = null;
                } else {
                    buffer.write(b, off, len);
                }
            }
            size = size + len;
        }
    }
}
