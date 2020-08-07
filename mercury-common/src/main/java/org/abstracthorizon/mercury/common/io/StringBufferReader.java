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
import java.io.Reader;

/**
 * StringBuffer reader.
 *
 * @author Daniel Sendula
 */
public class StringBufferReader extends Reader {

    /** Buffer */
    protected StringBuffer buffer;

    /** Pointer */
    protected int ptr;

    /** Marker */
    protected int mark = 0;

    /** Size */
    protected int size;

    /**
     * Constructor
     * @param buffer string buffer
     */
    public StringBufferReader(StringBuffer buffer) {
        this.buffer = buffer;
        size = buffer.length();
    }

    /**
     * Constructor
     * @param buffer string buffer
     * @param from starting offset
     * @param size size
     */
    public StringBufferReader(StringBuffer buffer, int from, int size) {
        this.buffer = buffer;
        ptr = from;
        size = ptr+size;
        if (ptr > buffer.length()) {
            ptr = buffer.length();
        }
        if (size > buffer.length()) {
            size = buffer.length();
        }
        if (size < ptr) {
            size = ptr;
        }
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        if (ptr >= buffer.length()) {
            return -1;
        }
        if (len > size-ptr) {
            len = size-ptr;
        }
        if (len == 0) {
            return 0;
        }
        buffer.getChars(ptr, ptr+len, cbuf, off);
        ptr = ptr + len;
        return len;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    /**
     * Marks position in string buffer
     */
    public void mark() {
        mark = ptr;
    }

    @Override
    public void reset() {
        ptr = mark;
    }

    @Override
    public boolean ready() {
        return true;
    }

    @Override
    public long skip(long n) {
        if (n > size - ptr) {
            n = size - ptr;
        }
        ptr = ptr + (int)n;
        return n;
    }

}
