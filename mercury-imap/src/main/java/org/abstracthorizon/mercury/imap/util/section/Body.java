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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.SequenceInputStream;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;
import javax.mail.internet.SharedInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.abstracthorizon.mercury.common.io.RangedInputStream;
import org.abstracthorizon.mercury.imap.util.MessageUtilities;

/**
 * BODY section
 *
 * @author Daniel Sendula
 */
public class Body extends PointerSection {

    /** Logger */
    protected static final Logger logger = LoggerFactory.getLogger(Body.class);

    /** Is this PEEK */
    public boolean peek = false;

    /** From */
    public int from = -1;

    /** To */
    public int to = -1;

    /** RFC822 keyword */
    public boolean rfc822 = false;

    /**
     * Constructor
     */
    public Body() {
    }

    /**
     * Returns measured input stream of a message
     * @param msg message
     * @return measured input stream
     * @throws IOException
     * @throws MessagingException
     */
    public MeasuredInputStream getInputStream(MimeMessage msg) throws IOException, MessagingException {
        long size = -1;
        InputStream is = null;
        String headers = null;


        Part part = msg;
        PointerSection section = this;

        while ((section.child != null) && (section.child instanceof MultipartSection)) {
            section = (MultipartSection)section.child;
            Object o = part.getContent();
            int partNo = ((MultipartSection)section).partNo-1;
            if (o instanceof Multipart) {
                part = ((Multipart)o).getBodyPart(partNo);
                if (part.getContentType().toLowerCase().startsWith("message/rfc822")) {
                    part = (MimePart)part.getContent();
                }
            } else if (o instanceof MimeMessage) {
                part = (MimeMessage)o;
            } else if (partNo == 0) {
                //part = part;
            } else {
                throw new MessagingException("Multipart part expected");
            }

        }
        if ((section.child != null) && (section.child instanceof HeaderSection)) {

            HeaderSection ss = (HeaderSection)section.child;
            if (ss.all) {
                headers = MessageUtilities.createHeaders((MimePart)part);
            } else {
                headers = MessageUtilities.createHeaders((MimePart)part, ss.fields, ss.not);
            }
            byte[] headerBytes = headers.getBytes();
            is = new ByteArrayInputStream(headerBytes);
            size = headerBytes.length;

        } else if ((section.child != null) && (section.child instanceof TextSection)) {
            is = getPartsInputStream(part);
            size = calcSize(part);
        } else if ((section.child != null) && (section.child instanceof MimeSection)) {
            headers = MessageUtilities.createHeaders((MimePart)part);
            byte[] headerBytes = headers.getBytes();
            is = new ByteArrayInputStream(headerBytes);
            size = headerBytes.length;
        } else if (part instanceof MimeMessage) {
            headers = MessageUtilities.createHeaders((MimePart)part);
            byte[] headerBytes = headers.getBytes();
            ByteArrayInputStream bais = new ByteArrayInputStream(headerBytes);
            is = getPartsInputStream(part);
            is = new SequenceInputStream(bais, is);
            long partSize = calcSize(part);
            long headersSize = headerBytes.length;
            size = partSize+headersSize;
        } else {
            is = getPartsInputStream(part);
            size = calcSize(part);
        }

        if ((from >= 0) && (to >= 0)) {
            if (to > size) {
                to = (int)size;
            }
            size = to-from;
            if (is instanceof SharedInputStream) {
                is = ((SharedInputStream)is).newStream(from, to);
            } else {
                is = new RangedInputStream(is, from, to);
            }
        }

        return new MeasuredInputStream(is, size);
    }

    /**
     * Returns input stream of a part
     * @param part part
     * @return input stream
     * @throws IOException
     * @throws MessagingException
     */
    public InputStream getPartsInputStream(Part part) throws IOException, MessagingException {
        try {
            if (part instanceof MimeBodyPart) {
                return ((MimeBodyPart)part).getRawInputStream();
                // Since this is raw stream we cannot skip headers as was thought before...
                // return skipHeaders(((MimeBodyPart)part).getRawInputStream());
            } else if (part instanceof MimeMessage) {
                return ((MimeMessage)part).getRawInputStream();
            } else {
                // This won't happen since part can be only BodyPart or Message (MimeBodyPart or MimeMessage)
                // This is only if previous statement is wrong.
                return part.getInputStream();
            }
        } catch (IOException e) {
            try {
                // Correcting file on the fly
                String oldEncoding = "";
                String[] encodings = part.getHeader("Content-Transfer-Encoding");
                if (encodings.length > 0) {
                    oldEncoding = encodings[0];
                }
                part.addHeader("X-Error", "Wrong: Content-Transfer-Encoding: "+oldEncoding);
                part.setHeader("Content-Transfer-Encoding", "7bit");
                saveChanges(part);
                return part.getInputStream();
            } catch (MessagingException me) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos);
                me.printStackTrace(ps);
                return new ByteArrayInputStream(baos.toByteArray());
            }
        }
    }

    /**
     * Saves changes of the part
     * @param part part
     * @throws MessagingException
     */
    protected void saveChanges(Part part) throws MessagingException {
        while ((part != null) && !(part instanceof MimeMessage)) {
            if (part instanceof BodyPart) {
                Multipart mp = ((BodyPart)part).getParent();
                part = mp.getParent();
            } else {
                // This shouldn't happened since Part can be or Message or BodyPart.
                // We are here not dealing with any other messages then MimeMessages and
                // then we are fine to be able to get to the top of hierarchy where
                // part is really MimeMessage so we can save it.

                // This is just in case previous statement is not entirely true.
                // Then we won't do anything.
                part = null;
            }
        }
        if (part instanceof MimeMessage) {
            MimeMessage mm = (MimeMessage)part;
            Folder f = mm.getFolder();
            f.appendMessages(new Message[]{mm});
            mm.setFlag(Flags.Flag.DELETED, true);
        }
    }

    /**
     * Returns <code>true</code> if child exists and child is instance of {@link HeaderSection}
     * @return <code>true</code> if child exists and child is instance of {@link HeaderSection}
     */
    public boolean hasStream() {
        return !((child != null) && (child instanceof HeaderSection));
    }

    /**
     * String representation of this class
     * @return string representation of this class
     */
    public String toString() {
        StringBuffer b = new StringBuffer();
        if (rfc822) {
            if ((child != null) && (child instanceof HeaderSection)) {
                b.append("RFC822.HEADER");
            } else if ((child != null) && (child instanceof TextSection)) {
                b.append("RFC822.TEXT");
            } else {
                b.append("RFC822");
            }
        } else {
            b.append("BODY");
            b.append("[");
            if (child != null) {
                b.append(child.toString());
            }
            b.append("]");
            if ((from >= 0) && (to >= 0)) {
                b.append("<").append(from).append('.').append(to).append(">");
            }
        }
        return b.toString();
    }

    /**
     * This method skips over header in the input stream
     * @param is input stream
     * @return same input stream
     * @throws IOException
     */
    protected InputStream skipHeaders(InputStream is) throws IOException {
        int state = 0;
        while (true) {
            int c = is.read();
            if (c < 0) {
                return is;
            }
            switch (state) {
                case 0: {
                    if (c == 13) {
                        state = 1;
                    }
                    break;
                }
                case 1: {
                    if (c == 10) {
                        state = 2;
                    } else {
                        state = 0;
                    }
                    break;
                }
                case 2: {
                    if (c == 13) {
                        state = 3;
                    } else {
                        state = 0;
                    }
                    break;
                }
                case 3: {
                    if (c == 10) {
                        return is;
                    }
                    break;
                }
            } // switch
        } // while
    }

    /**
     * Calculates the size of given part
     * @param part part
     * @return size of given part
     * @throws IOException
     * @throws MessagingException
     */
    protected long calcSize(Part part) throws IOException, MessagingException {
        InputStream is = getPartsInputStream(part);
        if (is instanceof ByteArrayInputStream) {
            return is.available();
        }
        if (is instanceof SharedInputStream) {
            return is.available();
        }
        try {
            long size = 0;
            byte[] buf = new byte[10240];
            int r = is.read(buf);
            while (r >= 0) {
                size = size+r;
                r = is.read(buf);
            } // while
            return size;
        } catch (Exception e) {
            // This is situation when decoding is incorrect or because of
            // any reason we can't really read the message. In that case
            // even simplest, undecoded, value is correct
            return part.getSize();
        }
    }
}
