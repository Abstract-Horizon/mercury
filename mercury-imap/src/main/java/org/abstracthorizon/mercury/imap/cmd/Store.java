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

import java.io.IOException;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.UIDFolder;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.abstracthorizon.mercury.common.command.CommandException;
import org.abstracthorizon.mercury.imap.IMAPSession;
import org.abstracthorizon.mercury.imap.response.FetchResponse;
import org.abstracthorizon.mercury.imap.util.ComposedSequence;
import org.abstracthorizon.mercury.imap.util.FlagUtilities;
import org.abstracthorizon.mercury.imap.util.MessageUtilities;
import org.abstracthorizon.mercury.imap.util.ParserException;
import org.abstracthorizon.mercury.imap.util.IMAPScanner;
import org.abstracthorizon.mercury.imap.util.section.Flags;

/**
 * Store IMAP command
 *
 * @author Daniel Sendula
 */
public class Store extends UIDCommand {

    /** Logger */
    protected static final Logger logger = LoggerFactory.getLogger(Store.class);

    /** Flags */
    protected Flags flags = new Flags();

    /**
     * Constructor
     * @param mnemonic mnemonic
     */
    public Store(String mnemonic) {
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
        IMAPScanner scanner = session.getScanner();
        ComposedSequence sequenceSet = new ComposedSequence();
        if (!scanner.sequence_set(sequenceSet)) {
            throw new ParserException("<sequence-set>");
        }
        if (!scanner.is_char(' ')) {
            throw new ParserException("<SP>");
        }

        flags = new Flags();
        if (!store_atts(scanner, flags)) {
            throw new ParserException("<store-atts>");
        }
        checkEOL(session);

        Folder f = session.getSelectedFolder();

        logger.debug("Prepare to process "+sequenceSet);
        MessageUtilities.sequenceIterator(session, this, f, sequenceSet, asuid);
        sendOK(session);
    }

    /**
     * Processes the message
     * @param session session
     * @param m message
     * @throws MessagingException
     * @throws IOException
     */
    public void process(IMAPSession session, MimeMessage m) throws MessagingException, IOException {
        logger.debug("Processing "+((UIDFolder)m.getFolder()).getUID(m));
        int msgNo = m.getMessageNumber();
        FetchResponse response = null;
        if (!flags.silent) {
            response = new FetchResponse(session, msgNo);
            response.append('(');
        }
        if (!flags.plus && !flags.minus) {
            javax.mail.Flags flgs = m.getFlags();
            m.setFlags(flgs, false);
            m.setFlags(flags.flags, true);
            logger.debug("Removed "+flgs+" from message UID "+((UIDFolder)m.getFolder()).getUID(m));
            logger.debug("Set "+flags.flags+" to same message UID "+((UIDFolder)m.getFolder()).getUID(m));
        } else {
            m.setFlags(flags.flags, flags.plus);
            if (flags.plus) {
                logger.debug("Set "+flags.flags+" from message UID "+((UIDFolder)m.getFolder()).getUID(m));
            } else {
                logger.debug("Removed "+flags.flags+" from message UID "+((UIDFolder)m.getFolder()).getUID(m));
            }
        }
        if (!flags.silent) {
            response.append("FLAGS (").append(FlagUtilities.toString(m.getFlags())).append(')');
            response.append(')');
            response.submit();
        }
    }

    /**
     * Scans for store attributes
     * @param scanner scanner
     * @param flags flags
     * @return <code>true</code> if processing is successful
     * @throws ParserException
     * @throws IOException
     */
    public static boolean store_atts(IMAPScanner scanner, Flags flags) throws ParserException, IOException {

        if (scanner.is_char('+')) {
            flags.plus = true;
        } else if (scanner.is_char('-')) {
            flags.minus = true;
        }
        if (!scanner.keyword("FLAGS")) {
            if (flags.plus || flags.minus) {
                throw new ParserException("'FLAGS'");
            } else {
                return false;
            }
        }
        if (scanner.keyword(".SILENT")) {
            flags.silent = true;
        }
        if (!scanner.is_char(' ')) {
            throw new ParserException("<SP>");
        }
        if (scanner.flag_list(flags.flags)) {
            return true;
        }
        if (scanner.flag(flags.flags)) {
            while (scanner.is_char(' ')) {
                if (!scanner.flag(flags.flags)) {
                    throw new ParserException("<flags>");
                }
            }
            return true;
        }
        return false;
    }
}
