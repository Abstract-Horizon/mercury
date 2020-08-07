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

import java.io.IOException;
import java.io.InputStream;


/**
 * Input stream that returns only a defined range
 *
 * @author Daniel Sendula
 */
public class RangedInputStream extends InputStream {

    /** Underlying input stream */
    protected InputStream is;

    /** Starting offset */
    protected long from;

    /** End offset */
    protected long to;

    /** Number of bytes left to be read */
    protected long left;

    /** Mark */
    protected long marked;

    /** Read limit */
    protected long readlimit;

    /** Read limit cache */
    protected long readlimitCache;

    /**
     * Constructor
     * @param is input stream
     * @param from from offset
     * @param to to offset
     * @throws IOException
     */
    public RangedInputStream(InputStream is, long from, long to) throws IOException {
        this.is = is;
        this.from = from;
        this.to = to;
        long l = is.skip(from);
        if (l < from) {
            left = 0;
        } else {
            left = to-from;
        }
        readlimit = -1;
        readlimitCache = -1;
    }

    @Override
    public int available() throws IOException {
        int i = is.available();
        if (i > left) {
            i = (int)left;
        }
        return i;
    }

    @Override
    public void close() throws IOException {
        is.close();
    }

    @Override
    public void mark(int readlimit) {
        is.mark(readlimit);
        this.readlimit = readlimit;
        this.readlimitCache = readlimit;
        marked = left;
    }

    @Override
    public boolean markSupported() {
        return is.markSupported();
    }

    @Override
    public int read() throws IOException {
        if (left == 0) {
            return -1;
        }
        int i = is.read();
        if (i >= 0) {
            left = left-1;
            if (readlimit >= 0) {
                readlimit = readlimit-1;
                if (readlimit < 0) {
                    readlimitCache = -1;
                }
            }
        }
        return i;
    }

    @Override
    public int read(byte[] b) throws IOException {
        if (left == 0) {
            return -1;
        }
        if (left < b.length) {
            return read(b, 0, (int)left);
        }
        int i = is.read(b);
        if (i >= 0) {
            left = left - i;
            if (readlimit >= 0) {
                readlimit = readlimit-i;
                if (readlimit < 0) {
                    readlimitCache = -1;
                }
            }
        } else {
            left = 0;
        }
        return i;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (left == 0) {
            return -1;
        }
        if (left < len) {
            len = (int)left;
        }
        int i = is.read(b, off, len);
        if (i >= 0) {
            left = left - i;
            if (readlimit >= 0) {
                readlimit = readlimit-i;
                if (readlimit < 0) {
                    readlimitCache = -1;
                }
            }
        } else {
            left = 0;
        }
        return i;
    }

    @Override
    public void reset() throws IOException {
        is.reset();
        if (readlimit >= 0) {
            left = marked;
            readlimit = readlimitCache;
        }
    }

    @Override
    public long skip(long n) throws IOException {
        if (left == 0) {
            return 0;
        }
        if (n > left) {
            n = left;
        }
        long i = is.skip(n);
        left = left - i;
        return i;
    }
}
