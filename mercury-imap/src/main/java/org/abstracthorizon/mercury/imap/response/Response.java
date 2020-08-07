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
package org.abstracthorizon.mercury.imap.response;

import java.io.IOException;
import java.io.OutputStream;
import org.abstracthorizon.mercury.imap.IMAPSession;

/**
 * IMAP response
 *
 * @author Daniel Sendula
 */
public abstract class Response {

    public static final int TAGGED_RESPONSE = 1;
    public static final int UNTAGGED_RESPONSE = 2;
    public static final int CONTINUATION_RESPONSE = 3;

    /** IMAP session */
    protected IMAPSession session;

    /** Message */
    private StringBuffer msg = new StringBuffer();

    /**
     * Response
     *
     * @param session session
     * @param type type of response
     */
    public Response(IMAPSession session, int type) {
        this.session = session;
        if (type == TAGGED_RESPONSE) {
            this.msg.append(session.getTag()).append(' ');
        } else if (type == UNTAGGED_RESPONSE) {
            this.msg.append("* ");
        } else if (type == CONTINUATION_RESPONSE) {
            this.msg.append("+ ");
        }
    }

    /**
     * Response with message
     *
     * @param session session
     * @param type type of response
     * @param msg message
     */
    public Response(IMAPSession session, int type, String msg) {
        this(session, type);
        this.msg.append(msg);
    }

    /**
     * Appends string to response
     * @param string string to be appended
     * @return this response
     */
    public Response append(String string) {
        msg.append(string);
        return this;
    }

    /**
     * Appends content string buffer
     * @param sbuf string buffer
     * @return this response
     */
    public Response append(StringBuffer sbuf) {
        msg.append(sbuf);
        return this;
    }

    /**
     * Appends integer
     * @param i integer
     * @return this response
     */
    public Response append(int i) {
        msg.append(i);
        return this;
    }

    /**
     * Appends long number
     * @param l long
     * @return this response
     */
    public Response append(long l) {
        msg.append(l);
        return this;
    }

    /**
     * Appends char
     * @param c char
     * @return this response
     */
    public Response append(char c) {
        msg.append(c);
        return this;
    }

    /**
     * Appends object
     * @param object object
     * @return this response
     */
    public Response append(Object object) {
        msg.append(object);
        return this;
    }

    /**
     * Sends directly buffer to output
     * @param buf buffer
     * @throws IOException
     */
    public void append(byte[] buf) throws IOException {
        append(buf, 0, buf.length);
    }

    /**
     * Appends buffer
     * @param buf buffer
     * @param off offset
     * @param len lebgth
     * @throws IOException
     */
    public void append(byte[] buf, int off, int len) throws IOException {
        OutputStream out = session.adapt(OutputStream.class);
        out.write(buf, off, len);
        out.flush();
    }

    /**
     * Commits response
     * @throws IOException
     */
    protected void commit() throws IOException {
        OutputStream out = session.adapt(OutputStream.class);
        synchronized (out) {
            out.write(msg.toString().getBytes());
            out.write(13);
            out.write(10);
            out.flush();
        }
        msg.delete(0, msg.length());
    }

    /**
     * Submits response
     * @throws IOException
     */
    public void submit() throws IOException {
        if (msg.length() > 0) {
            OutputStream out = session.adapt(OutputStream.class);
            synchronized (out) {
                out.write(msg.toString().getBytes());
                out.write(13);
                out.write(10);
                out.flush();
            }
            msg.delete(0, msg.length());
        }
    }

}
