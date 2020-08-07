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

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.mail.internet.SharedInputStream;


/**
 * Pool of shared input stream instance. Since each instance uses RandomAccessFile from
 * java.io package it is important managing these resources.
 *
 * @author Daniel Sendula
 */
public class SharedInputStreamPool {

    /** Pool default instance */
    protected static SharedInputStreamPool defaultInstance = new SharedInputStreamPool();

    /** Set of opened <code>SharedInputStreamImpl</code>s */
    protected Set<SharedInputStream> files = new HashSet<SharedInputStream>();

    /** Maximum number of files */
    protected int maxFiles = 200;

    /** Timeout for removing not closed files */
    protected int timeout = 1000; // one secs

    /**
     * Default constructor.
     */
    public SharedInputStreamPool() {
    }

    /**
     * This method returns default instance
     * @return default instance
     */
    public static SharedInputStreamPool getDefaultInstance() {
        return defaultInstance;
    }

    /**
     * This method creates new <code>SharedInputStreamImpl</code> instance.
     *
     * @param fileProvider file provider
     * @param start start of the stream
     * @param len end of the stream
     * @return new <code>SharedInputStreamImpl</code> instance
     */
    public SharedInputStreamImpl newStream(FileProvider fileProvider, long start, long len) {
        return new SharedInputStreamImpl(this, fileProvider, start, len);
    }

    /**
     * This is callback method used by <code>SharedInputStreamImpl</code> to register
     * that stream is now opened. This is called each time <code>RandomAccessFile</code>
     * is created over the stream. It is possible for stream to be implicitly &quot;clsoed&quot;
     * and (re)opened several times.
     *
     * @param stream stream that is opened
     */
    protected synchronized void opened(SharedInputStreamImpl stream) {
        files.add(stream);
        int size = files.size();
        if (size > maxFiles) {
            int removed = 0;
            int timeout = this.timeout * (size/maxFiles) * 2;
            Iterator<SharedInputStream> it = files.iterator();
            long now = System.currentTimeMillis();
            SharedInputStreamImpl oldest = stream;
            SharedInputStreamImpl s = null;
            while (it.hasNext()) {
                s = (SharedInputStreamImpl)it.next();
                if ((now - stream.lastAccessed) < timeout) {
                    try {
                        s.closeImpl();
                    } catch (IOException ignore) {
                    }
                    it.remove();
                    removed++;
                } else if ((removed == 0) && (s.lastAccessed <= oldest.lastAccessed)) {
                    oldest = stream;
                }
            } // while
            if (removed == 0) {
                closed(oldest);
            }
        }
    }

    /**
     * This is callback method used by <code>SharedInputStreamImpl</code>
     * to register that stream is now closed.
     * This is called each time <code>RandomAccessFile</code> used by the stream is released.
     * It is possible for stream to be implicitly &quot;clsoed&quot;
     * and (re)opened several times.
     * @param stream stream that is closed
     */
    protected synchronized void closed(SharedInputStreamImpl stream) {
        files.remove(stream);
    }

    /**
     * Method that implicitly releases all <code>RandomAccessFile</code>s from all
     * streams that use given <code>FileProvider</code>
     * @param provider file provider whos <code>SharedInputStreamImpl</code> belongs to
     */
    public synchronized void closeWithProvider(FileProvider provider) {
        Iterator<SharedInputStream> it = files.iterator();
        SharedInputStreamImpl s = null;
        while (it.hasNext()) {
            s = (SharedInputStreamImpl)it.next();
            if (s.fileProvider == provider) {
                try {
                    s.closeImpl();
                } catch (IOException ignore) {
                }
                it.remove();
            }
        } // while
    }
}
