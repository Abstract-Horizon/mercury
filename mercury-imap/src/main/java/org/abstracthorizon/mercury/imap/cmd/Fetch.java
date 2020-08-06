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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import javax.mail.Folder;
import javax.mail.MessagingException;
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
import org.abstracthorizon.mercury.imap.util.section.Body;
import org.abstracthorizon.mercury.imap.util.section.BodyStructure;
import org.abstracthorizon.mercury.imap.util.section.Envelope;
import org.abstracthorizon.mercury.imap.util.section.Flags;
import org.abstracthorizon.mercury.imap.util.section.HeaderSection;
import org.abstracthorizon.mercury.imap.util.section.Internaldate;
import org.abstracthorizon.mercury.imap.util.section.RFC822Size;
import org.abstracthorizon.mercury.imap.util.section.TextSection;
import org.abstracthorizon.mercury.imap.util.section.UID;


/**
 * Fetch IMAP command
 *
 * @author Daniel Sendula
 */
public class Fetch extends UIDCommand {

    /** Logger */
    protected static final Logger logger = LoggerFactory.getLogger(Fetch.class);

    /** Internal date */
    protected static SimpleDateFormat internalDate = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss Z");

    /** Tag */
    protected boolean tagSent = false;

    /** Attributes */
    protected List<Object> attrs;

    /**
     * Constructor
     * @param mnemonic mnemonic
     */
    public Fetch(String mnemonic) {
        super(mnemonic);
        unilateral = IMAPCommand.ALWAYS_SUPRESS_UNILATERAL_DATA;
    }

    /**
     * Defines as UID function
     */
    public void setAsUID() {
        asuid = true;
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
        fetch(session);
    }

    /**
     * Executes the command
     * @param session
     * @throws ParserException
     * @throws MessagingException
     * @throws CommandException
     * @throws IOException
     */
    public void fetch(IMAPSession session) throws IOException, ParserException, MessagingException {
        IMAPScanner scanner = session.getScanner();
        attrs = new ArrayList<Object>();

        ComposedSequence sequenceSet = new ComposedSequence();
        if (!scanner.sequence_set(sequenceSet)) {
            throw new ParserException("<sequence_set>");
        }
        //logger.debug("0) min="+sequenceSet.getMin()+" max="+sequenceSet.getMax());
        if (!scanner.is_char(' ')) {
            throw new ParserException("<SP>");
        }

        if (scanner.keyword("ALL")) {
            attrs.add(new Flags());
            attrs.add(new Internaldate());
            RFC822Size rfc822 = new RFC822Size();
            attrs.add(rfc822);
            attrs.add(new Envelope());
        } else if (scanner.keyword("FULL")) {
            attrs.add(new Flags());
            attrs.add(new Internaldate());
            RFC822Size rfc822 = new RFC822Size();
            attrs.add(rfc822);
            attrs.add(new Envelope());
            attrs.add(new Body());
        } else if (scanner.keyword("FAST")) {
            attrs.add(new Flags());
            attrs.add(new Internaldate());
            RFC822Size rfc822 = new RFC822Size();
            attrs.add(rfc822);
        } else if (scanner.is_char('(')) {
            if (fetch_att(scanner, attrs)) {
                boolean ok = true;
                while (ok && scanner.is_char(' ')) {
                    ok = fetch_att(scanner, attrs);
                } // while
                if (ok && !scanner.is_char(')')) {
                    throw new ParserException("')'");
                }
            }
        } else {
            if (!fetch_att(scanner, attrs)) {
                throw new ParserException("'ALL','FULL', 'FAST', '('");
            }
        }

        if (asuid) { // Implicit UID in UID FETCH
            if (!attrs.contains(UID.instance)) {
                attrs.add(0, UID.instance);
            }
        }
        checkEOL(session);

        Folder f = session.getSelectedFolder();

        MessageUtilities.sequenceIterator(session, this, f, sequenceSet, asuid);
        sendOK(session);
    }

    /**
     * Parses fetch attributes
     * @param scanner scanner
     * @param atts attributes
     * @return <code>true</code> if parsing was correct
     * @throws IOException
     * @throws ParserException
     */
    public static boolean fetch_att(IMAPScanner scanner, List<Object> atts) throws IOException, ParserException {
        if (scanner.keyword("ENVELOPE")) {
            atts.add(new Envelope());
        } else if (scanner.keyword("FLAGS")) {
            atts.add(new Flags());
        } else if (scanner.keyword("INTERNALDATE")) {
            atts.add(new Internaldate());
        } else if (scanner.keyword("RFC822")) {
            if (scanner.is_char('.')) {
                if (scanner.keyword("HEADER")) {
                    Body body = new Body();
                    body.peek = true;
                    body.rfc822 = true;
                    HeaderSection hs = new HeaderSection();
                    body.child = hs;
                    if (scanner.is_char('.')) { // from obsolete rfc-1730
                        if (scanner.keyword("LINES")) {
                            hs.fields = new ArrayList<String>();
                            if (scanner.is_char(' ')) {
                                if (scanner.header_list(hs.fields)) {
                                }
                            }
                        }
                    }
                    atts.add(body);
                } else if (scanner.keyword("SIZE")) {
                    RFC822Size rfc822 = new RFC822Size();
                    atts.add(rfc822);
                } else if (scanner.keyword("TEXT")) {
                    Body body = new Body();
                    body.rfc822 = true;
                    body.child = new TextSection();
                    atts.add(body);
                } else if (scanner.keyword("PEEK")) {
                    // Non standard Outlook express dialect
                    Body body = new Body();
                    body.rfc822 = true;
                    body.peek = true;
                    atts.add(body);
                } else {
                    throw new ParserException("'HEADER', 'SIZE', 'TEXT'");
                }
            } else {
                Body body = new Body();
                body.rfc822 = true;
                atts.add(body);
            }
        } else if (scanner.keyword("BODY")) {
            //Body body = new Body();
            //atts.add(body);
            if (scanner.keyword("STRUCTURE")) {
                BodyStructure b = new BodyStructure();
                b.structure = true;
                atts.add(b);
            } else {
                boolean peek = false;
                if (scanner.keyword(".PEEK")) {
                    peek = true;
                }
                Body body = new Body();
                if (scanner.section(body)) {
                    body.peek = peek;
                    atts.add(body);
                    //throw new ParserException("Syntax error - expecting BODY section");
                    if (scanner.is_char('<')) {
                        IMAPScanner.Number num1 = new IMAPScanner.Number();
                        if (!scanner.number(num1)) {
                            throw new ParserException("<number>");
                        }
                        body.from = num1.number;
                        if (!scanner.is_char('.')) {
                            throw new ParserException("'.'");
                        }
                        IMAPScanner.Number num2 = new IMAPScanner.Number();
                        if (!scanner.nz_number(num2)) {
                            throw new ParserException("<number>");
                        }
                        if (!scanner.is_char('>')) {
                            throw new ParserException("'>'");
                        }
                        body.from = num2.number;
                    }
                } else {
                    BodyStructure b = new BodyStructure();
                    b.structure = false;
                    atts.add(b);
                }
            }
        } else if (scanner.keyword("UID")) {
            atts.add(UID.instance);
        } else {
            throw new ParserException("'ENVELOPE', 'FLAGS', 'INTERNALDATE', 'RFC822', 'BODY', 'UID'");
        }
        return true;
    }

    /**
     * Processes message
     * @param session session
     * @param m mime message
     * @throws MessagingException
     */
    public void process(IMAPSession session, MimeMessage m) throws IOException, MessagingException {
        try {
            //StringBuffer response = new StringBuffer(256);
            boolean first = true;
            // Not sure is this really needed and what to do with messages that are
            // expunged in the middle of Fetch.
            boolean expunged = m.isExpunged();
            int msgNo = m.getMessageNumber();
            FetchResponse response = new FetchResponse(session, msgNo);
            response.append('(');
            for (int j = 0; j < attrs.size(); j++) {
                Object o = attrs.get(j);
                if (o instanceof UID) {
                    first = spaceSeparator(first, response);
                    response.append("UID ");
                    response.append(MessageUtilities.findUID(m));
                } else if (o instanceof Flags) {
                    first = spaceSeparator(first, response);
                    response.append("FLAGS (").append(FlagUtilities.toString(m.getFlags())).append(')');
                } else if (o instanceof Envelope) {
                    if (!expunged) {
                        first = spaceSeparator(first, response);
                        response.append("ENVELOPE ");
                        response.createEnvelope(m);
                    }
                } else if (o instanceof Internaldate) {
                    if (!expunged) {
                        first = spaceSeparator(first, response);
                        response.append("INTERNALDATE ");
                        response.append('"');
                        response.append(internalDate.format(m.getReceivedDate()));
                        response.append('"');
                    }
                } else if (o instanceof RFC822Size) {
                    if (!expunged) {
                        first = spaceSeparator(first, response);
                        int size = m.getSize();
                        if (size < 0) {
                            // Nothing we can do in order to find out size of the message
                            // so we will just assume that it is zero!
                            size = 0;
                        }
                        String headers = MessageUtilities.createHeaders(m);
                        int headersSize = headers.getBytes().length;
                        size = size + headersSize;

                        //RFC822Size rfc = (RFC822Size) o;
                        response.append("RFC822.SIZE ");
                        response.append(size);
                    }
                } else if (o instanceof Body) {
                    if (!expunged) {
                        first = spaceSeparator(first, response);
                        Body b = (Body) o;
                        response.append(b, m);
                        if (!b.peek) {
                            if (!m.getFlags().contains(javax.mail.Flags.Flag.RECENT)) {
                                m.setFlag(javax.mail.Flags.Flag.RECENT, false);
                            }
                            if (!m.getFlags().contains(javax.mail.Flags.Flag.SEEN)) {
                                m.setFlag(javax.mail.Flags.Flag.SEEN, true);
                            }
                        }
                    }
                } else if (o instanceof BodyStructure) {
                    if (!expunged) {
                        first = spaceSeparator(first, response);
                        response.append("BODYSTRUCTURE ");
                        //response.append("BODY ");
                        response.createBodyStructure(m, true);
                    }
                }

            } // for

            response.append(')');
            response.submit();
            tagSent = false;
        } catch (MessagingException e) {
            session.setKeepLog(true);
            logger.error("Fetch processing message error", e);
            // we don't want to stop processing other messages only because of this one.
        }
    }

    /**
     * Adds space if not first
     * @param flag flag
     * @param response fetch response
     * @return negated flag
     */
    protected boolean spaceSeparator(boolean flag, FetchResponse response) {
        if (flag) {
            flag = false;
        } else {
            response.append(' ');
        }
        return flag;
    }
}
