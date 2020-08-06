/*
 * Copyright (c) 2004-2007 Creative Sphere Limited.
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
package org.abstracthorizon.mercury.smtp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.abstracthorizon.danube.support.RuntimeIOException;

/**
 * SMTP response
 *
 * @author Daniel Sendula
 */
public class SMTPResponse {

    /** CR as byte array */
    protected static final byte[] CRLF = new byte[] { '\r', '\n' };

    /** Response code */
    protected int code;

    /** List of messages */
    protected String[] msg;

    /** Empty message */
    private static final String[] empty = new String[] { "" };

    private byte[] cachedResult;
    
    /**
     * Constructor
     * @param code code
     */
    public SMTPResponse(int code) {
        this.code = code;
        msg = empty;
        cachedResult = null;
    }

    /**
     * Constructor
     * @param code code
     * @param msg message
     */
    public SMTPResponse(int code, String msg) {
        this.code = code;
        if (msg == null) {
            msg = "";
        }
        this.msg = new String[] { msg };
        makeCachedResult();
    }

    /**
     * Constructor
     * @param code code
     * @param msg array of strings
     */
    public SMTPResponse(int code, String[] msg) {
        this.code = code;
        this.msg = msg;
        makeCachedResult();
    }

    /**
     * Sets line
     * @param line line
     */
    public void setLine(String line) {
        msg = new String[] { line };
        makeCachedResult();
    }

    /**
     * Adds new line
     * @param line line
     */
    public void addLine(String line) {
        String[] n = new String[msg.length + 1];
        n[msg.length] = line;
        msg = n;
        cachedResult = null;
    }

    /**
     * Submits response
     * @param out output stream
     * @throws IOException
     */
    public void submit(OutputStream out) throws IOException {
        if (cachedResult == null) {
            makeCachedResult();
        }
        out.write(cachedResult);
        out.flush();
    }

    /**
     * Returns code.
     * @return code
     */
    public int getCode() {
        return code;
    }

    protected void makeCachedResult() {
        ByteArrayOutputStream res = new ByteArrayOutputStream();
        try {
            if (msg.length > 1) {
                for (int i = 0; i < msg.length - 1; i++) {
                    res.write((code + "-" + msg[i]).getBytes());
                    res.write(CRLF);
                }
            }
            res.write((code + " " + msg[msg.length - 1]).getBytes());
            res.write(CRLF);
            cachedResult = res.toByteArray();
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }
    
    public byte[] getBytes() {
        if (cachedResult == null) {
            makeCachedResult();
        }
        return cachedResult;
    }
    
    /**
     * Returns string representation of response
     * @return string representation of response
     */
    public String toString() {
        if (cachedResult == null) {
            makeCachedResult();
        }
        return new String(cachedResult);
    }
}
