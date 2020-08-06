/*
 * Copyright (c) 2005-2007 Creative Sphere Limited.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import javax.mail.internet.SharedInputStream;

/**
 * <code>SharedInputStream</code> implementation
 *
 * @author Daniel Sendula
 */
public class SharedInputStreamImpl extends InputStream implements SharedInputStream {

    /** Next Unique Id - needed for debug purposes only */
    protected static int num = 0;

    /** Unique Id - needed for debug purposes only */
    protected int id;

    /** Stream start offset */
    protected long start;

    /** Stream end offset */
    protected long end;

    /** Stream's pointer */
    protected long ptr;

    /** Mark */
    protected long mark;

    /** Stream's buffer size */
    public static final int BUFFER_SIZE = 1024 * 16;

    /** Stream's buffer */
    protected byte[] buffer;

    /** Stream's buffer start pointer */
    protected long bufptr;

    /** Stream's buffer content len */
    protected int buflen;

    /** Cached value of file's size */
    protected long fileSize = -1;

    /** File provider reference */
    protected FileProvider fileProvider;

    /** Random access file reference */
    protected RandomAccessFile raf;

    /** Pool that created this object */
    protected SharedInputStreamPool parent;

    /** Timestamp this stream's file is accessed last time */
    protected long lastAccessed;

    /**
     * Constructor.
     * @param parent pool that is creating this stream
     * @param fileProvider file provider
     * @param start stream start
     * @param end stream end
     */
    public SharedInputStreamImpl(SharedInputStreamPool parent, FileProvider fileProvider, long start, long end) {
        this.fileProvider = fileProvider;
        this.parent = parent;
        this.end = end;
        this.start = start;
        if (end < 0) {
            try {
                fileSize = fileProvider.getFile().length();
                this.end = fileSize;
            } catch (IOException ignore) {
                ignore.printStackTrace();
            }
        }
        mark = start;
        ptr = start;
        lastAccessed = System.currentTimeMillis();
        num = num + 1;
        id = num;
    }

    /**
     * Reads bytes from the underlaying file. It opens it if it wasn't open at the moment
     * of call. Also, uses internal buffer too.
     * @param buf buffer to be read into
     * @param off offset in buffer
     * @param len length to be read
     * @return number of actually read bytes
     * @throws IOException
     */
    public int read(byte[] buf, int off, int len) throws IOException {
        lastAccessed = System.currentTimeMillis();
        if (len == 0) {
            return 0;
        }
        if (ptr >= end) {
            return -1;
        }
        int rem = (int) (end - ptr);
        if (len > rem) {
            len = rem;
        }
        int ret = 0;
        boolean firstRead = true;
        while ((len > 0) && (ptr < end)) {
            int l = len;
            if ((buffer != null) && (ptr >= bufptr) && (ptr < bufptr + buflen)) {
                // we have part of requested data in buffer
                int bufoff = (int) (ptr - bufptr);
                if (l > buflen - bufoff) {
                    l = buflen - bufoff;
                }
                System.arraycopy(buffer, bufoff, buf, off, l);
            } else {
                synchronized (this) {
                    if (firstRead) {
                        checkOpened();
                        long p = raf.getFilePointer();
                        if ((p != ptr) && (ptr <= getFileSize())) {
                            raf.seek(ptr);
                        }
                        firstRead = false;
                    }
                    // requested is not in buffer
                    if (l >= BUFFER_SIZE) {
                        // resulted read is just in the middle - so save
                        // reading in buffer for last part
                        if ((buffer == null) || (buffer.length < BUFFER_SIZE)) {
                            buffer = new byte[BUFFER_SIZE];
                        }
                        l = raf.read(buf, off, BUFFER_SIZE);
                        if (l < 0) {
                            // Reached unexpeced end of file prematurely
                            ptr = end;
                            l = 0;
                            len = 0;
                        }
                    } else {
                        // Last part
                        rem = (int) (end - ptr);
                        if (rem > BUFFER_SIZE) {
                            rem = BUFFER_SIZE;
                        }
                        if ((buffer == null) || (buffer.length < rem)) {
                            buffer = new byte[rem];
                        }
                        bufptr = ptr;
                        buflen = raf.read(buffer, 0, rem);
                        if (buflen >= 0) {
                            if (l > buflen) {
                                l = buflen;
                            }
                            if (l > 0) {
                                try {
                                    System.arraycopy(buffer, 0, buf, off, l);
                                } catch (ArrayIndexOutOfBoundsException e) {
                                    throw e;
                                }
                            }
                        } else {
                            // Reached unexpeced end of file prematurely
                            len = 0;
                            ptr = end;
                            l = 0;
                        }
                    }
                } // synch
            }
            off = off + l;
            ret = ret + l;
            ptr = ptr + l;
            len = len - l;
        } // while
        if (ptr >= end) {
            close();
        }
// TODO - check if this is working - should be ok but not tested.
//        if (bufptr + buflen >= end) {
//            synchronized (this) {
//                raf.close();
//                raf = null;
//            }
//        }
        return ret;
    } // read

    /**
     * Reads whole buffer.
     * @param buf buffer to be read into
     * @return number of actually read bytes
     * @throws IOException
     */
    public int read(byte[] buf) throws IOException {
        return read(buf, 0, buf.length);
    } // read

    /**
     * Reads one byte or returns -1 if EOF is reached (end of stream really).
     * @return read byte
     * @throws IOException
     */
    public int read() throws IOException {
        // lastAccessed = System.currentTimeMillis();
        if (ptr >= end) {
            return -1;
        }
        if ((buffer != null) && (ptr >= bufptr) && (ptr < bufptr + buflen)) {
            int ret = buffer[(int) (ptr - bufptr)];
            ptr = ptr + 1;
            if (ptr >= end) {
                close();
            }
            return ret;
        }
        byte[] b = new byte[1];
        int i = read(b);
        if (i > 0) {
            return b[0];
        } else {
            return -1;
        }
    } // read

    /**
     * Returns number of available bytes in stream (to end of stream).
     * @return number of available bytes
     * @throws IOException
     */
    public int available() throws IOException {
        return (int) (end - ptr);
    } // available

    /**
     * Closes the stream and releases allocated resources.
     * @throws IOException
     */
    public void close() throws IOException {
        if (raf != null) {
            closeImpl();
            parent.closed(this);
        }
    } // close

    /**
     * This method actually releases the resources (<code>random access file</code>)
     * @throws IOException
     */
    public synchronized void closeImpl() throws IOException {
        raf.close();
        raf = null;
        buffer = null;
    } // close

    /**
     * Returns <code>true</code>
     * @return <code>true</code>
     */
    public boolean markSupported() {
        return true;
    } // isMarkSupported

    /**
     * Sets mark.
     * @param readlimit ignored
     */
    public void mark(int readlimit) {
        mark = ptr;
    } // mark

    /**
     * Resets stream to the mark
     * @throws IOException
     */
    public void reset() throws IOException {
        ptr = mark;
    } // reset

    /**
     * Returns <code>true</code>
     * @return <code>true</code>
     */
    public boolean ready() {
        return true;
    } // ready

    /**
     * Skips number of bytes
     * @param n number of bytes to be skipped
     * @return current pointer in stream
     * @throws IOException
     */
    public long skip(long n) throws IOException {
        lastAccessed = System.currentTimeMillis();
        if (n > end - ptr) {
            n = end - ptr;
        }
        ptr = ptr + (int) n;
        if (ptr >= end) {
            close();
        }
        return n;
    } // skip

    /**
     * Returns current pointer in stream
     * @return current pointer in stream
     */
    public long getPosition() {
        return ptr - start;
    } // getPosition

    /**
     * Creates new stream from this stream.
     * @param pos new relative start position
     * @param end new relative end position
     * @return new instance with given start and end.
     */
    public InputStream newStream(long pos, long end) {
        pos = this.start + pos;
        if (end < 0) {
            return parent.newStream(fileProvider, pos, this.end);
        } else {
            end = this.start + end;
            return parent.newStream(fileProvider, pos, end);
        }
    } // newStream

    /**
     * Checks if underlaying file is opened. It uses <code>FileProvider</code>
     * to obtain file.
     * @throws IOException
     */
    protected void checkOpened() throws IOException {
        if (raf == null) {
            raf = new RandomAccessFile(fileProvider.getFile(), "r");
            raf.seek(start);
            parent.opened(this);
        }
    }

    /**
     * Returns file's size. If not cached then caches it using <code>FileProvider</code>
     * @return file's size.
     */
    protected long getFileSize() {
        if (fileSize >= 0) {
            return fileSize;
        }
        fileSize = fileProvider.getFileSize();
        if (fileSize < 0) {
            try {
                synchronized (this) {
                    checkOpened();
                    fileSize = raf.length();
                }
            } catch (IOException ignore) {
            }
        }
        return fileSize;
    }
}
