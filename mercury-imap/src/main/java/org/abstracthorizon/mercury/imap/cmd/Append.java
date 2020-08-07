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
package org.abstracthorizon.mercury.imap.cmd;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.abstracthorizon.mercury.common.command.CommandException;
import org.abstracthorizon.mercury.common.io.TempStorage;
import org.abstracthorizon.mercury.imap.IMAPSession;
import org.abstracthorizon.mercury.imap.util.IMAPScanner;
import org.abstracthorizon.mercury.imap.util.ParserException;

/**
 * Append IMAP command
 *
 * @author Daniel Sendula
 */
public class Append extends IMAPCommand {

    /** Default buffer len */
    public static final int BUFFER_LEN = 10240;

    /**
     * Constructor
     * @param mnemonic mnemonic
     */
    public Append(String mnemonic) {
        super(mnemonic);
    }

    /**
     * Executes method
     * @param session
     * @throws ParserException
     * @throws MessagingException
     * @throws CommandException
     * @throws IOException
     */
    public void execute(IMAPSession session) throws ParserException, MessagingException, CommandException, IOException {
        //APPEND" SP mailbox [SP flag-list] [SP date-time] SP literal
        StringBuffer mailboxName = new StringBuffer();
        if (!session.getScanner().mailbox(mailboxName)) {
            throw new ParserException("<mailbox_name>");
        }
        Folder folder = session.getStore().getFolder(mailboxName.toString());
        if (folder == null) {
            throw new MissingMailbox(mailboxName.toString());
        }

        IMAPScanner scanner = session.getScanner();
        if (!scanner.is_char(' ')) {
            throw new ParserException("<SP>");
        }
        Flags flags = new Flags();
        if (scanner.flag_list(flags)) {
            if (!scanner.is_char(' ')) {
                throw new ParserException("<SP>");
            }
        }

        boolean haveDate = false;
        GregorianCalendar calendar = new GregorianCalendar();
        if (scanner.date_time(calendar)) {
            haveDate = true;
            if (!scanner.is_char(' ')) {
                throw new ParserException("<SP>");
            }
        }
        long size = scanner.raw_literal();
        if (size == -1) {
            throw new ParserException("<literal>");
        }

        InputStream in = (InputStream)session.adapt(InputStream.class);

        TempStorage tempStorage = new TempStorage();

        int buflen = BUFFER_LEN;
        if (buflen > size) {
            buflen = (int)size;
        }

        byte[] buf = new byte[buflen];

        OutputStream out = tempStorage.getOutputStream();

        while (size > 0) {
            int rdsz = buf.length;
            if (rdsz > size) {
                rdsz = (int)size;
            }
            int rd = in.read(buf, 0, rdsz);
            if (rd < 0) {
                size = -1;
                throw new IOException("Premature end of literal");
            } else {
                out.write(buf, 0, rd);
                size = size - rd;
            }
        }
        checkEOL(session);

        out.close();

        MimeMessage message = null;
        if (haveDate) {
            message = new MimeMessage(session.getJavaMailSession(), tempStorage.getInputStream());
        } else {
            message = new MMessage(session.getJavaMailSession(), tempStorage.getInputStream(), calendar.getTime());
        }

        File file = tempStorage.getFile();
        if (file != null) {
            message.setFileName(file.getAbsolutePath());
        }
        folder.appendMessages(new Message[]{message});

        //session.close();
        sendOK(session);
    }

    /**
     * Message wrapper that adds received date
     *
     * @author Daniel Sendula
     */
    public static class MMessage extends MimeMessage {

        /** Received date */
        protected Date receivedDate;

        /**
         * Constructor
         * @param session JavaMail session
         * @param stream input stream
         * @param date received message date
         * @throws MessagingException
         */
        protected MMessage(Session session, InputStream stream, Date date) throws MessagingException {
            super(session, stream);
            receivedDate = date;
        }

        /**
         * Returns received date
         *
         * @return received date
         */
        public Date getReceivedDate() {
            return receivedDate;
        }
    }

}
