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
package org.abstracthorizon.mercury.maildir.util;

import java.io.IOException;
import java.io.InputStream;
import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;

/**
 * This class reads headers from the input stream
 *
 * @author Daniel Sendula
 */
public class InternetHeadersImpl extends InternetHeaders {

    /**
     * Default constructor
     */
    public InternetHeadersImpl() {
        super();
    }

    /**
     * Constroctor that reads headers from the input stream
     * @param inputStream input stream
     * @throws MessagingException
     */
    public InternetHeadersImpl(InputStream inputStream) throws MessagingException {
        super(inputStream);
    }

    /**
     * Loads headers from the input stream
     * @param inputStream input stream
     * @throws MessagingException
     */
    public void load(InputStream inputStream) throws MessagingException {
        String s1 = null;

        StringBuffer stringbuffer = new StringBuffer();

        boolean run = true;
        while (run) {
            try {
                String s = readLine(inputStream);
                if ((s != null) && (s.startsWith(" ") || s.startsWith("\t"))) {


                    if (s1 != null) {
                        stringbuffer.append(s1);
                        s1 = null;
                    }
                    stringbuffer.append("\r\n");
                    stringbuffer.append(s);
                } else {
                    if (s1 != null) {
                        addHeaderLine(s1);
                    } else {
                        if (stringbuffer.length() > 0) {

                            addHeaderLine(stringbuffer.toString());
                            stringbuffer.setLength(0);
                        }
                    }
                    s1 = s;
                }

                if (s == null) {
                    run = false;
                } else {
                    if (s.length() <= 0) {
                        return;
                    }
                }
            } catch (IOException e) {
                throw new MessagingException("Error in input stream", e);
            }
        }
    }

    /**
     * This method reads a line from the input stream
     * @param in input stream
     * @return new string representing the read line
     * @throws IOException
     */
    public String readLine(InputStream in) throws IOException {
        byte[] line = new byte[1024];
        int l = 0;
        boolean maybeEOL = false;
        while (true) {
            int c = in.read();

            int k = -1;

            if (c == -1) {
                if (l == 0) {
                    return "";
                } else {
                    return new String(line, 0, l, "ISO-8859-1");
                }
            } else if (c == 13) {
                if (maybeEOL) {
                    k = c;
                } else {
                    maybeEOL = true;
                }
            } else if (c == 10) {
                //if (maybeEOL) {
                    return new String(line, 0, l);
                //} else {
                //    k = c;
                //    maybeEOL = false;
                //}
            } else {
                k = c;
                maybeEOL = false;
            }
            if (k != -1) {
                if (l == line.length) {
                    byte[] linet = new byte[line.length*2];
                    System.arraycopy(line, 0, linet, 0, line.length);
                    line = linet;
                }
                line[l] = (byte)k;
                l = l + 1;
            }
        }
    }
}
