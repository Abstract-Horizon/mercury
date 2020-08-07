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

import org.abstracthorizon.mercury.imap.IMAPSession;
import org.abstracthorizon.mercury.imap.util.MessageUtilities;
import org.abstracthorizon.mercury.imap.util.section.Body;
import org.abstracthorizon.mercury.imap.util.section.MeasuredInputStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;
import javax.mail.internet.NewsAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetch response
 *
 * @author Daniel Sendula
 */
public class FetchResponse extends NumberResponse {

    /** Logger */
    protected static final Logger logger = LoggerFactory.getLogger(FetchResponse.class);

    /** Empty buffer */
    public static final byte[] empty = new byte[10240];

    static {
        for (int i=0; i<empty.length; i++) {
            empty[i] = 32;
        }
    }

    /**
     * Constructor
     * @param session imap session
     * @param num number
     */
    public FetchResponse(IMAPSession session, int num) {
        super(session, "FETCH", num);
        append(' ');
    }

    /**
     * Append message to response
     * @param b body request
     * @param msg message
     * @return this
     * @throws IOException
     * @throws MessagingException
     */
    public FetchResponse append(Body b, MimeMessage msg) throws IOException, MessagingException {
        MeasuredInputStream is = b.getInputStream(msg);
        OutputStream out = (OutputStream)session.adapt(OutputStream.class);
        synchronized (out) {
            long size = 0;
            boolean f = true;
            try {
                byte[] buf = new byte[10240];
                int r = is.read(buf);
                while (r >= 0) {
                    if (f) {
                        // Moved response here since is.read can fail because
                        // of bad decoding. In case everything is ok we can
                        // react here. In case of an error we still have
                        // enough time to react!
                        size = is.size();
                        // append(' ');
                        append(b.toString());
                        append(" {").append(size).append("}");
                        commit();
                        f = false;
                    }
                    append(buf, 0, r);
                    size = size - r;
                    r = is.read(buf);
                } // while
                if (f) {
                    // Moved response here since is.read can fail because
                    // of bad decoding. In case everything is ok we can
                    // react here. In case of an error we still have
                    // enough time to react!
                    size = is.size();
                    // append(' ');
                    append(b.toString());
                    append(" {").append(size).append("}");
                    commit();
                    f = false;
                }
            } catch (Exception e) {
                session.getDebugStream().write(e.getMessage().getBytes());
                session.setKeepLog(true);
                logger.error("Error reading body for message with id="+msg.getMessageID()+" subject="+msg.getSubject(), e);
                if (size == 0) {
                    // nothing got sent yet so we can send whatever we think.
                    StringWriter exc = new StringWriter();
                    PrintWriter eout = new PrintWriter(exc);
                    e.printStackTrace(eout);
                    append(b.toString()).append(" \"").append(exc.toString()).append('"');
                } else {

                    while (size > 0) {
                        if (size > 10240) {
                            append(empty, 0, 10240);
                            size = size - 10240;
                        } else {
                            append(empty, 0, (int)size);
                            size = 0;
                        }
                    }
                    // just fill rest of it with zeroes...
                }
            } finally {
                is.close();
            }
        }
        return this;
    }

    /**
     * Creates envelope and appends it to response
     * @param m message
     * @throws MessagingException
     */
    public void createEnvelope(MimeMessage m) throws MessagingException {
        appendOpenP();

        Date sentDate = m.getSentDate();
        if (sentDate == null) {
            sentDate = m.getReceivedDate();
        }
        appendDate(sentDate);

        appendSpace();

        appendNString(m.getSubject());

        appendSpace();
        appendAddress(safeFrom(m));

        appendSpace();
        appendAddress(safeSender(m));

        appendSpace();
        Address[] tos = null;
        try {
            tos = m.getReplyTo();
        } catch (Exception e) {
        }
        appendAddress(tos);

        appendSpace();
        appendAddress(safeRecipients(m, Message.RecipientType.TO));

        appendSpace();
        appendAddress(safeRecipients(m, Message.RecipientType.CC));

        appendSpace();
        appendAddress(safeRecipients(m, Message.RecipientType.BCC));

        appendSpace();
        //env.append("NIL");
        String[] s = m.getHeader("in-reply-to");
        if ((s == null) || (s.length == 0)) {
            appendNString((String)null);
        } else {
            appendNString(s[0]);
        }
        //addString(env, m.getHeader());

        appendSpace();
        appendNString(m.getMessageID());

        appendCloseP();
    }

    /**
     * Returns list of recipients or <code>null</code>
     * @param m message
     * @param type type of recipients
     * @return addresses or <code>null</code> (in case of an error)
     */
    public Address[] safeRecipients(MimeMessage m, Message.RecipientType type) {
        try {
            return m.getRecipients(type);
        } catch (Throwable e) {
            // Don't want to make any fuss about wrong fields...
            return null;
        }
    }

    /**
     * Returns from addresses or <code>null</code> in case of an error)
     * @param m message
     * @return addresses or <code>null</code>
     */
    public Address[] safeFrom(MimeMessage m) {
        try {
            return m.getFrom();
        } catch (Throwable e) {
            // Don't want to make any fuss about wrong fields...
            return null;
        }
    }

    /**
     * Returns sender addresses or <code>null</code> in case of an error)
     * @param m message
     * @return addresses or <code>null</code>
     */
    public Address[] safeSender(MimeMessage m) {
        try {
            Address a = m.getSender();
            if (a == null) {
                return null;
            } else {
                return new Address[]{a};
            }
        } catch (Throwable e) {
            // Don't want to make any fuss about wrong fields...
            return null;
        }
    }

    /**
     * Creates body structore and appends it to response
     * @param p mime part
     * @param extensible is extensible
     * @throws IOException
     * @throws MessagingException
     */
    public void createBodyStructure(MimePart p, boolean extensible) throws IOException, MessagingException {
        appendOpenP();
        Object o = null;
        try {
            o = p.getContent();
        } catch (UnsupportedEncodingException e) {

            // Multipart shouldn't fail with this exception.
            // For else this shouldn't matter!
            // o stays null
        } catch (IOException e) {
            // o stays null
            // TODO this should be strengthened a bit
        }
        String type = p.getContentType();
        if (o instanceof Multipart) {
            Multipart mp = (Multipart)o;
            int count = mp.getCount();
            if (count > 0) {
                for (int i=0; i<count; i++) {
                    //if (i > 0) {
                    //    appendSpace();
                    //}
                    MimePart innerPart = (MimePart)mp.getBodyPart(i);
                    createBodyStructure(innerPart, extensible);
                } // for
            }
            appendSpace();
            type = mp.getContentType();
            appendObject(getSubtype(type));

            if (extensible) {
                // Extension
                appendSpace();
                appendParameters(type);
                appendSpace();
                appendDisposition(p);
                appendSpace();
                appendObject(p.getContentLanguage());
            }
        } else {
            appendObject(getType(type));
            appendSpace();
            appendObject(getSubtype(type));
            appendSpace();
            //res.append("()"); // Body Parameters List
            //res.append("NIL");
            appendParameters(type);
            appendSpace();

            appendObject(p.getContentID());
            appendSpace();

            appendObject(p.getDescription());
            appendSpace();

            String encoding = p.getEncoding();
            if (encoding == null) {
                encoding = "7BIT";
            }
            appendObject(encoding);
            appendSpace();

            int size = p.getSize();
            if (size < 0) {
                size = 0;
            }
            if (size >= 0) {
                append(size);
            } else {
                appendNil();
            }
            if (type.startsWith("text")) {
                appendSpace();
                int lines = countLines(p);
                //lines = 25;
                if (lines >= 0) {
                    append(lines);
                } else {
                    appendNil();
                }
            } else if (type.startsWith("message/rfc822") && (o != null)) {
                // o is null only if there was IOException or UnsupportedEncodingException
                appendSpace();
                createEnvelope((MimeMessage)o);
                appendSpace();
                createBodyStructure((MimeMessage)o, extensible);
                appendSpace();
                int lines = countLines(p);
                //lines = 25;
                if (lines >= 0) {
                    append(lines);
                } else {
                    appendNil();
                }
            }
            /*
            if (extensible) {
                // Extension ???
                appendSpace();
                appendNil();
                appendSpace();
                appendNil();
                appendSpace();
                appendNil();
            }
            */
            appendSpace();
            appendObject(p.getContentMD5());
            appendSpace();
            appendDisposition(p);
            appendSpace();
            appendObject(p.getContentLanguage());
        }
        appendCloseP();

    }

    /**
     * Appends NIL
     */
    public void appendNil() {
        append("NIL");
    }

    /**
     * Appends space
     */
    public void appendSpace() {
        append(' ');
    }

    /**
     * Appends (
     */
    public void appendOpenP() {
        append('(');
    }

    /**
     * Appends )
     */
    public void appendCloseP() {
        append(')');
    }

    /**
     * Appends quotation marks
     */
    public void appendQuote() {
        append('"');
    }

    /**
     * Appends date.
     * @param d date. Maybe null (and it will append NIL then)
     */
    protected void appendDate(Date d) {
        if (d != null) {
            appendQuote();
            append(MessageUtilities.dateFormat.format(d));
            appendQuote();
        } else {
            appendNil();
        }
    }

    /**
     * Appends string escaping all offending characters
     * @param s string
     */
    protected void appendString(String s) {
        if (s != null) {
            boolean literal = false;
            if ((s.indexOf('\n') >= 0) || (s.indexOf('\r') >= 0)) {
                literal = true;
            }

            if (literal) {
                try {
                    append('{').append(s.length()).append('}');
                    commit();
                    append(s.getBytes());
                } catch (IOException ignore) {
                    // There isn't much we can do right now so we will ignore it.
                }
            } else {
                int i = s.indexOf('"');
                int j = s.indexOf('\\');
                if ((i < 0) && (j < 0)) {
                    append('"').append(s).append('"');
                } else {

                    StringBuffer buf = new StringBuffer();
                    char c;
                    for (i=0; i<s.length(); i++) {
                        c = s.charAt(i);
                        if (c == '"') {
                            buf.append('\\').append(c);
                        } else if (c == '\\') {
                            buf.append(c).append(c);
                        } else {
                            buf.append(c);
                        }
                    } // for
                    appendQuote();
                    append(buf);
                    appendQuote();
                }
            }
        }
    }

    /**
     * Appends NSTRING.
     * @param s string. Maybe null (and it will append NIL then)
     */
    protected void appendNString(String s) {
        if (s == null) {
           appendNil();
        } else {
            appendString(s);
        }
    }

    /**
     * Appends strings separated with spaces
     * @param s strings. Maybe null (and it will append NIL then)
     */
    protected void appendString(String[] s) {
        if (s == null) {
            appendNil();
        } else {
            appendQuote();
            boolean first = true;
            for (int i=0; i<s.length; i++) {
                if (s[i] != null) {
                    if (first) {
                        first = false;
                    } else {
                        append(' ');
                    }
                    appendString(s[i]);
                }
            }
            appendQuote();
        }
    }

    /**
     * Appends addresses
     * @param ss addresses as strings
     */
    protected void appendAddress(String[] ss) {
        if (ss != null) {
            ArrayList<InternetAddress> list = new ArrayList<InternetAddress>();
            if (ss.length > 0) {
                for (int i=0; i<ss.length; i++) {
                    try {
                        list.add(new InternetAddress(ss[i]));
                    } catch (AddressException e) {
                        // we don't care - we can't do anything - anyway
                    }
                }
            }
            if (list.size() > 0) {
                Address[] as = new Address[list.size()];
                as = list.toArray(as);
                appendAddress(as);
            } else {
                appendNil();
            }
        } else {
            appendNil();
        }
    }

    /**
     * Appends addresses
     * @param a addresses. May be null and it will append NIL then.
     */
    protected void appendAddress(Address[] a) {
        if ((a == null) || (a.length == 0)) {
             appendNil();
        } else {
            appendOpenP();
            for (int i=0; i<a.length; i++) {
                appendOpenP();
                if (a[i] instanceof InternetAddress) {
                    InternetAddress ia = (InternetAddress)a[i];
                    appendNString(ia.getPersonal());
                    appendSpace();
                    appendNil();
                    appendSpace();
                    String adr = ia.getAddress();
                    if (adr == null) {
                        appendSpace();
                        appendNil();
                        appendSpace();
                        appendNil();
                    } else {
                        int j = adr.indexOf('@');
                        if (j >= 0) {
                            String n = adr.substring(0, j);
                            String ad = adr.substring(j+1);
                            appendNString(n);
                            append(' ');
                            appendNString(ad);
                        } else {
                            appendNil();
                            appendSpace();
                            appendNString(adr);
                        }
                    }
                } else {
                    appendNil();
                    appendSpace();
                    appendNil();
                    appendSpace();
                    appendNil();
                    appendSpace();
                    append(((NewsAddress)a[i]).getHost());
                }
                appendCloseP();
            } // for
            appendCloseP();
        }
    }

    /**
     * Appends object calling toString method
     * @param o object. May be null and it will append NIL then
     */
    protected void appendObject(Object o) {
        if (o == null) {
            appendNil();
        } else {
            //appendString(o.toString().toUpperCase());
            appendString(o.toString());
        }
    }

    /**
     * Appends parameters
     * @param type type of parameters
     */
    protected void appendParameters(String type) {
        int i = type.indexOf(';');
        if (i < 0) {
            appendNil();
            return;
        }

        boolean first = true;
        int j = i+1;
        while (i > 0) {
            String p = null;
            i = type.indexOf(';',j);
            if (i < 0) {
                p = type.substring(j).trim();
            } else {
                p = type.substring(j, i).trim();
                j = i + 1;
            }
            if (p.length() > 0) {
                p = appendParameter(p);
                if (p != null) {
                    if (first) {
                        appendOpenP();
                        first = false;
                    } else {
                        appendSpace();
                    }
                    append(p);
                }
            }
        }

        if (first) {
            appendNil();
        } else {
            appendCloseP();
        }
    }

    /**
     * Appends parameter
     * @param param existing parameters
     * @return new parameter
     */
    protected String appendParameter(String param) {
        param = param.trim();
        int i = param.indexOf('=');
        if (i + 1 >= param.length()) {
            return null;
        }
        if (i < 0) {
            return null;
        }

        String name = param.substring(0, i).trim().toUpperCase();
        String value = param.substring(i + 1).trim();
        if (value.charAt(0) == '"') {
            if (value.charAt(value.length()-1) == '"') {
                return "\""+name+"\" "+value;
            } else {
                return null;
            }
        } else {
            return "\""+name+"\" \""+value.toUpperCase()+"\"";
        }
    }

    /**
     * Appends disposition or NIL
     * @param part mime part
     * @throws MessagingException
     */
    protected void appendDisposition(MimePart part) throws MessagingException {
        String disposition = part.getDisposition();
        if (disposition == null) {
            appendNil();
            return;
        }
        String[] ss = part.getHeader("Content-Disposition");
        if ((ss == null) || (ss.length == 0)) {
            appendNil();
            return;
        }
        appendOpenP();
        appendObject(disposition);
        appendSpace();
        appendParameters(ss[0]);
        appendCloseP();
    }

    /**
     * Returns mime type
     * @param type mime type
     * @return type only without extra parameters
     */
    protected String getType(String type) {
        int i = type.indexOf('/');
        if (i >= 0) {
            return type.substring(0, i).toUpperCase();
        }
        i = type.indexOf(';');
        if (i >= 0) {
            return type.substring(0, i).toUpperCase();
        }
        return type.toUpperCase();
    }

    /**
     * Returns mime sub type
     * @param type mime type
     * @return sub type only
     */
    protected String getSubtype(String type) {
        int i = type.indexOf('/');
        if (i >= 0) {
            int j = type.indexOf(';');
            if (j >= 0) {
                return type.substring(i+1, j).toUpperCase();
            } else {
                return type.substring(i+1).toUpperCase();
            }
        } else {
            return null;
        }
    }

    /**
     * Counts lines in given part
     * @param part part
     * @return number or lines
     * @throws MessagingException
     */
    protected int countLines(Part part) throws MessagingException {
        String type = part.getContentType();
        if (type.startsWith("text") || type.startsWith("message/rfc822")) {
            int lines = 0;
            try {
                InputStream is = part.getInputStream();
                try {
                    InputStreamReader reader = new InputStreamReader(part.getInputStream(), "ISO-8859-1");
                    BufferedReader in = new BufferedReader(reader);
                    String line = in.readLine();
                    while (line != null) {
                        lines = lines + 1;
                        line = in.readLine();
                    } // while
                } finally {
                    is.close();
                }
            } catch (IOException e) {
                // throw new MessagingException("Cannot count lines", e);
                // ignore
                lines = 1; // Lets see if this do...
            }
            return lines;
        } else {
            return -1;
        }

    }
}
