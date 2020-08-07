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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Reader;

/**
 * File reader that can uses random access file.
 *
 * @author Daniel Sendula
 */
public class AdvancedFileReader extends Reader {

    /** Size */
    protected long size;

    /** Mark */
    protected long mark;

    /** Random access file */
    protected RandomAccessFile file;

    /**
     * Constructor
     * @param file file
     * @param from starting offset
     * @param size size
     * @throws IOException
     */
    public AdvancedFileReader(File file, long from, long size) throws IOException {
        this.file = new RandomAccessFile(file, "r");
        this.file.seek(from);
        this.size = size+from;
        if (size > this.file.length()-from) {
            size = this.file.length()-from;
        }
        if (size < 0) {
            size = 0;
        }
        mark = from;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        if (len == 0) {
            return 0;
        }

        long ptr = file.getFilePointer();
        if (ptr >= size) {
            return -1;
        }

        if (len > size-ptr) {
            len = (int)(size-ptr);
        }

        if (len == 0) {
            return -1;
        }

        byte[] buf = new byte[len];
        len = file.read(buf);
        if (len == 0) {
            return 0;
        }
        for (int i=0; i<len; i++) { // TODO: this is slow...
            cbuf[off] = (char)buf[i];
            off = off + 1;
        }
        return len;
    }

    @Override
    public void close() throws IOException {
        file.close();
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    /**
     * Marks position in a file
     * @throws IOException
     */
    public void mark() throws IOException {
        mark = file.getFilePointer();
    }

    @Override
    public void reset() throws IOException {
        file.seek(mark);
    }

    @Override
    public boolean ready() {
        return true;
    }

    @Override
    public long skip(long n) throws IOException {
        long ptr = file.getFilePointer();
        if (n > size - ptr) {
            n = size - ptr;
        }
        ptr = ptr + (int)n;
        return n;
    }
}
