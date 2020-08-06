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
package org.abstracthorizon.mercury.imap.cmd;

import org.abstracthorizon.mercury.common.command.CommandException;
import org.abstracthorizon.mercury.imap.IMAPSession;
import org.abstracthorizon.mercury.imap.util.ComposedSequence;
import org.abstracthorizon.mercury.imap.util.MessageUtilities;
import org.abstracthorizon.mercury.imap.util.ParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copy IMAP Command
 *
 * @author Daniel Sendula
 */
public class Copy extends UIDCommand {

    /** Logger */
    public static Logger logger = LoggerFactory.getLogger(Copy.class);

    /** List of message to be processed */
    protected List<MimeMessage> toProcess = new ArrayList<MimeMessage>();

    /**
     * Constructor
     * @param mnemonic mnemonic
     */
    public Copy(String mnemonic) {
        super(mnemonic);
    }

    /**
     * Executes the command
     * @param session
     * @throws ParserException
     * @throws MessagingException
     * @throws CommandException
     * @throws IOException
     */
    protected void execute(IMAPSession session) throws ParserException, MessagingException, CommandException, IOException {
        // "COPY" SP sequence-set SP mailbox
        ComposedSequence sequenceSet = new ComposedSequence();
        if (!session.getScanner().sequence_set(sequenceSet)) {
            throw new ParserException("<sequence-set>");
        }
        if (!session.getScanner().is_char(' ')) {
            throw new ParserException("<SP>");
        }
        StringBuffer mailboxName = new StringBuffer();
        if (!session.getScanner().mailbox(mailboxName)) {
            throw new ParserException("<mailbox_name>");
        }
        checkEOL(session);

        Folder fromFolder = session.getSelectedFolder();

        Folder toFolder = session.getStore().getFolder(mailboxName.toString());
        if (toFolder == null) {
            throw new MissingMailbox(mailboxName.toString());
        }
        toFolder.open(Folder.READ_WRITE);
        try {
            MessageUtilities.sequenceIterator(session, this, fromFolder, sequenceSet, asuid);
            Message[] messages = new Message[toProcess.size()];
            messages = (Message[])toProcess.toArray(messages);
            toFolder.appendMessages(messages);
            sendOK(session);
        } finally {
            try {
                toFolder.close(false);
            } catch (MessagingException ee) {
                session.setKeepLog(true);
                logger.error("Copy", ee);
                // We don't want possible exception from close to mess with real exception
            }
        }
    }

    /**
     * Processes messages
     * @param session session
     * @param m mime message
     * @throws MessagingException
     */
    public void process(IMAPSession session, MimeMessage m) throws MessagingException {
        toProcess.add(m);
    }
}
