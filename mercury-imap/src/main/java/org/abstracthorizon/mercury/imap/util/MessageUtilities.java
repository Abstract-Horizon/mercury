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
package org.abstracthorizon.mercury.imap.util;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.UIDFolder;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;

import org.abstracthorizon.mercury.common.util.RFCDate;
import org.abstracthorizon.mercury.imap.IMAPSession;

/**
 * Utility methods for working with messages
 *
 * @author Daniel Sendula
 */
public class MessageUtilities {

    /** Date format */
    public static SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");

    /** CR/LF */
    public static final String CRLF = "\r\n";

    /**
     * Find message by UID
     * @param f folder
     * @param uid uid
     * @return message
     * @throws MessagingException if folder is not of {@link UIDFolder} type
     */
    public static Message findMessageByUID(Folder f, long uid) throws MessagingException {
        if (f instanceof UIDFolder) {
            return ((UIDFolder) f).getMessageByUID(uid);
        } else {
            throw new MessagingException("Not Implemented");
        }
    }

    /**
     * Returns messages's UID
     * @param m message
     * @return UID
     * @throws MessagingException if folder is not of {@link UIDFolder} type
     */
    public static long findUID(Message m) throws MessagingException {
        Folder f = m.getFolder();
        if (f instanceof UIDFolder) {
            return ((UIDFolder) f).getUID(m);
        } else {
            throw new MessagingException("Not Implemented");
        }
    }

    /**
     * Creates string representation of headers for mime part
     * @param m mime part
     * @return string representation of headers
     * @throws MessagingException
     */
    public static String createHeaders(MimePart m) throws MessagingException {
        Enumeration<?> en = m.getAllHeaderLines();
        // StringBuffer buf = new StringBuffer(512);
        return createHeaders(m, en);
        // while (en.hasMoreElements()) {
        // buf.append(en.nextElement().toString()).append(MessageUtilities.CRLF);
        // }
        // if (m.getSize() > 0) {
        // buf.append(MessageUtilities.CRLF);
        // }
        // return buf.toString();
    }

    /**
     * Creates string representation of headers for mime part
     * @param m mime part
     * @param headers headers
     * @param not should headers form the list be included or excluded
     * @return string representation of headers
     * @throws MessagingException
     */
    public static String createHeaders(MimePart m, List<String> headers, boolean not)
            throws MessagingException {
        String[] list = new String[headers.size()];
        list = headers.toArray(list);
        Enumeration<?> en = null;
        if (not) {
            en = m.getNonMatchingHeaderLines(list);
        } else {
            en = m.getMatchingHeaderLines(list);
        }
        return createHeaders(m, en);
    }

    /**
     * Creates string representation of headers for mime part
     * @param m mime part
     * @param headers headers
     * @return stirng representation of headers
     * @throws MessagingException
     */
    public static String createHeaders(MimePart m, Enumeration<?> headers)
            throws MessagingException {
        StringBuffer buf = new StringBuffer(512);
        while (headers.hasMoreElements()) {
            String headerLine = headers.nextElement().toString();
            if (headerLine.startsWith("Date: ")) {
                String date = headerLine.substring(6).trim();
                if (RFCDate.validate(date)) {
                    buf.append(headerLine).append(CRLF);
                } else {
                    Date d;
                    if (m instanceof MimeMessage) {
                        d = ((MimeMessage) m).getReceivedDate();
                    } else {
                        d = new Date(); // !!!
                    }
                    String newHeaderLine = "Date: "
                            + RFCDate.DATE_FORMAT.format(d);
                    buf.append(newHeaderLine).append(CRLF);
                }
            } else {
                buf.append(headerLine).append(CRLF);
            }
        }
        if (m.getSize() > 0) {
            buf.append(CRLF);
        }
        return buf.toString();
    }

    /**
     * Iterates over sequence for given folder's messages
     * @param session imap session
     * @param processor message processor
     * @param f folder
     * @param sequenceSet sequence
     * @param asuid does sequence represent UIDs or positions of messages
     * @throws IOException
     * @throws MessagingException
     */
    public static void sequenceIterator(IMAPSession session, MessageProcessor processor, Folder f,
            Sequence sequenceSet, boolean asuid) throws IOException,
            MessagingException {
        if (!asuid) {
            int msgCount = f.getMessageCount();
            sequenceSet.setLowerLimit(1);
            sequenceSet.setUpperLimit(msgCount);
        }
        if (sequenceSet instanceof ComposedSequence) {
            composedSequenceIterator(session, processor, f, (ComposedSequence) sequenceSet, asuid);
        } else if (sequenceSet instanceof SimpleSequence) {
            simpleSequenceIterator(session, processor, f, (SimpleSequence) sequenceSet, asuid);
        }
    }

    /**
     * Iterates over sequence for given folder's messages
     * @param session imap session
     * @param processor message processor
     * @param f folder
     * @param sequenceSet sequence
     * @param asuid does sequence represent UIDs or positions of messages
     * @throws IOException
     * @throws MessagingException
     */
    public static void composedSequenceIterator(IMAPSession session, MessageProcessor processor,
            Folder f, ComposedSequence sequenceSet, boolean asuid)
            throws IOException, MessagingException {
        Iterator<Sequence> iterator = sequenceSet.getSequencesAsIterator();
        while (iterator.hasNext()) {
            Sequence set = iterator.next();
            if (set instanceof ComposedSequence) {
                composedSequenceIterator(session, processor, f, (ComposedSequence) set, asuid);
            } else if (set instanceof SimpleSequence) {
                simpleSequenceIterator(session, processor, f, (SimpleSequence) set, asuid);
            }
        }
    }

    /**
     * Iterates over sequence for given folder's messages
     * @param session imap session
     * @param processor message processor
     * @param f folder
     * @param sequenceSet sequence
     * @param asuid does sequence represent UIDs or positions of messages
     * @throws IOException
     * @throws MessagingException
     */
    public static void simpleSequenceIterator(IMAPSession session, MessageProcessor processor,
            Folder f, SimpleSequence sequenceSet, boolean asuid)
            throws IOException, MessagingException {
        int min = sequenceSet.getMin();
        int max = sequenceSet.getMax();
        if (min < 1) {
            min = 1;
        }

        if (min == max) {
            MimeMessage msg;
            if (asuid) {
                msg = (MimeMessage) ((UIDFolder) f).getMessageByUID(min);
            } else {
                msg = (MimeMessage) f.getMessage(min);
            }
            if (msg != null) {
                processor.process(session, msg);
            }
        } else {
            Message[] msgs;
            if (asuid) {
                msgs = ((UIDFolder) f).getMessagesByUID(min, max);
            } else {
                int msgCount = f.getMessageCount();
                if (max > msgCount) {
                    max = msgCount;
                }
                msgs = f.getMessages(min, max);
            }
            for (int i = 0; i < msgs.length; i++) {
                MimeMessage msg = (MimeMessage) msgs[i];
                if (msg != null) {
                    processor.process(session, msg);
                }
            }
        }
    }

    /**
     * Iterates over sequence for given folder's messages
     * @param session imap session
     * @param processor message processor
     * @param f folder
     * @param sequenceSet sequence
     * @param asuid does sequence represent UIDs or positions of messages
     * @throws IOException
     * @throws MessagingException
     */
    public static void sequenceIteratorOld(IMAPSession session, MessageProcessor processor,
            Folder f, Sequence sequenceSet, boolean asuid) throws IOException,
            MessagingException {
        int msgs = f.getMessageCount();
        // logger.debug("1) min="+sequenceSet.getMin()+"
        // max="+sequenceSet.getMax());
        sequenceSet.setLowerLimit(1);
        // logger.debug("2) min="+sequenceSet.getMin()+"
        // max="+sequenceSet.getMax());
        if (asuid) {
            int max = (int) MessageUtilities.maxUID(f);
            // logger.debug("max="+max);
            sequenceSet.setUpperLimit(max);
        } else {
            // logger.debug("max="+msgs);
            sequenceSet.setUpperLimit(msgs);
        }
        // logger.debug("3) min="+sequenceSet.getMin()+"
        // max="+sequenceSet.getMax());

        sequenceSet.first();
        // logger.debug("4) min="+sequenceSet.getMin()+"
        // max="+sequenceSet.getMax());
        if (asuid) {
            // logger.debug("as uid!");
            int msgNo = -1;
            while (sequenceSet.more()) {
                int uid = sequenceSet.next();
                // if (uid > 573) {
                // logger.debug(": "+uid);
                // }
                MimeMessage m = null;
                if (msgNo < 0) {
                    if (uid == 2764) {
                        uid = uid * 2 / 2;
                    }
                    m = (MimeMessage) MessageUtilities.findMessageByUID(f, uid);
                    if (m != null) {
                        msgNo = m.getMessageNumber();
                        // logger.debug(":(1) #="+msgNo);
                    } else {
                        // logger.debug(":(1) = null!");
                        msgNo = -1;
                    }
                } else {
                    m = (MimeMessage) f.getMessage(msgNo + 1);
                    if (m != null) {
                        // logger.debug(":(2) #="+m.getMessageNumber());
                        int u = (int) MessageUtilities.findUID(m);
                        if (u > uid) {
                            // logger.debug(":(3) = u="+u);
                            sequenceSet.setLowerLimit(u);
                            sequenceSet.first();
                            m = null;
                        } else if (u < uid) {
                            // logger.debug(":(4) = u="+u);
                            m = (MimeMessage) MessageUtilities
                                    .findMessageByUID(f, uid);
                            if (m != null) {
                                msgNo = m.getMessageNumber();
                                // logger.debug(":(4) = #="+msgNo);
                            } else {
                                // logger.debug(":(4) = null");
                                msgNo = -1;
                            }
                        } else {
                            msgNo = msgNo + 1;
                            // logger.debug(":(4) = msgNo = msgNo+1 u="+u+"
                            // uid="+uid);
                        }
                    } else {
                        // logger.debug(":(2) = null!");
                        sequenceSet.setLowerLimit(sequenceSet.getMax() + 1);
                    }
                }

                if (m != null) {
                    processor.process(session, m);
                }
            }
        } else {
            while (sequenceSet.more()) {
                int msgNo = sequenceSet.next();
                MimeMessage m = (MimeMessage) f.getMessage(msgNo);
                if (m != null) {
                    processor.process(session, m);
                }

            } // while
        }
    }

    /**
     * Returns maximum UID form the folder
     * @param f folder
     * @return maxumum UID
     * @throws MessagingException if folder is not of {@link UIDFolder} type
     */
    public static long maxUID(Folder f) throws MessagingException {
        // logger.debug("count="+f.getMessageCount()+"
        // #="+f.getMessage(f.getMessageCount()).getMessageNumber());
        return findUID(f.getMessage(f.getMessageCount()));
    }
}
