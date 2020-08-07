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
package org.abstracthorizon.mercury.imap.util.section;

import java.io.IOException;
import java.io.InputStream;

/**
 * Input stream that counts number of bytes and let only
 * specified number of bytes to be read
 *
 * @author Daniel Sendula
 */
public class MeasuredInputStream extends InputStream {

    /** Wrapped input stream */
    protected InputStream is;

    /** Maximum len */
    protected long len;

    /** Amount of read bytes */
    protected long read = 0;

    /** Mark */
    protected long mark = -1;

    /**
     * Constructor
     * @param is input stream
     * @param len expected len
     */
    public MeasuredInputStream(InputStream is, long len) {
        this.is = is;
        this.len = len;
    }

    /**
     * Returns measured size
     * @return measured size
     */
    public long size() {
        return len;
    }

    @Override
    public int available() throws IOException {
        int a = is.available();
        if (read >= len) {
            return 0;
        } else if (a > read - len) {
            return (int)(read - len);
        } else {
            return a;
        }
    }

    @Override
    public void close() throws IOException {
         is.close();
    }

    @Override
    public void mark(int readlimit) {
        is.mark(readlimit);
        mark = read;
    }

    @Override
    public boolean markSupported() {
        return is.markSupported();
    }

    @Override
    public int read() throws IOException {
        if (read >= this.len) {
            return -1;
        } else {
            int r = is.read();
            if (r >= 0) {
                read = read + 1;
            }
            return r;
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        return is.read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (read >= this.len) {
            return -1;
        } else if (len >= read - this.len) {
            len = (int)(read - this.len);
        }
        int l = is.read(b, off, len);
        if (l >= 0) {
            read = read + l;
        }
        return l;
    }

    @Override
    public void reset() throws IOException {
        is.reset();
        read = mark;
    }

    @Override
    public long skip(long n) throws IOException {
        if (read >= len) {
            return 0;
        } else if (n > (read - len)) {
            n = (read - len);
        }
        long l = is.skip(n);
        read = read + l;
        return l;
    }
}

