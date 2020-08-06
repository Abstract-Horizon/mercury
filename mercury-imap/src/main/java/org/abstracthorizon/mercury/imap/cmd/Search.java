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
import java.util.ArrayList;
import java.util.GregorianCalendar;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;
import javax.mail.search.AndTerm;
import javax.mail.search.BodyTerm;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.FromStringTerm;
import javax.mail.search.HeaderTerm;
import javax.mail.search.NotTerm;
import javax.mail.search.OrTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.RecipientStringTerm;
import javax.mail.search.SearchTerm;
import javax.mail.search.SentDateTerm;
import javax.mail.search.SizeTerm;
import javax.mail.search.StringTerm;
import javax.mail.search.SubjectTerm;

import org.abstracthorizon.mercury.common.command.CommandException;
import org.abstracthorizon.mercury.imap.IMAPSession;
import org.abstracthorizon.mercury.imap.response.SearchResponse;
import org.abstracthorizon.mercury.imap.util.ComposedSequence;
import org.abstracthorizon.mercury.imap.util.IMAPScanner;
import org.abstracthorizon.mercury.imap.util.MessageUtilities;
import org.abstracthorizon.mercury.imap.util.ParserException;
import org.abstracthorizon.mercury.imap.util.Sequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Search IMAP command
 *
 * @author Daniel Sendula
 */
public class Search extends UIDCommand {

    public static final Flags ANSWERED = new Flags(Flags.Flag.ANSWERED);
    public static final Flags DELETED = new Flags(Flags.Flag.DELETED);
    public static final Flags DRAFT = new Flags(Flags.Flag.DRAFT);
    public static final Flags SEEN = new Flags(Flags.Flag.SEEN);
    public static final Flags RECENT = new Flags(Flags.Flag.RECENT);
    public static final Flags FLAGGED = new Flags(Flags.Flag.FLAGGED);

    /** Runs as UID */
    protected boolean asuid = false;

    /** Charset */
    protected StringBuffer charset = new StringBuffer();

    /** Logger */
    protected static final Logger logger = LoggerFactory.getLogger(Search.class);

    /**
     * Constructor
     *
     * @param mnemonic mnemonic
     */
    public Search(String mnemonic) {
        super(mnemonic);
    }

    /**
     * Marks that command runs as UID command
     */
    @Override
    public void setAsUID() {
        asuid = true;
    }

    /**
     * Executes the command
     *
     * @param session
     * @throws ParserException
     * @throws MessagingException
     * @throws CommandException
     * @throws IOException
     */
    @Override
    protected void execute(IMAPSession session) throws ParserException, MessagingException, CommandException, IOException {
        IMAPScanner scanner = session.getScanner();
        SearchTerm term = search(scanner);

        Folder f = session.getSelectedFolder();
        Message[] ms = f.search(term);

        checkEOL(session);

        SearchResponse response = new SearchResponse(session);
        if (ms.length > 0) {
            for (int i = 0; i < ms.length; i++) {
                if (asuid) {
                    response.addMessageNumber(MessageUtilities.findUID(ms[i]));
                } else {
                    response.addMessageNumber(ms[i].getMessageNumber());
                }
            }
        }
        response.submit();
        sendOK(session);
    }

    @Override
    public void process(IMAPSession session, MimeMessage m) throws MessagingException {
    }

    public SearchTerm search(IMAPScanner scanner) throws IOException, ParserException, MessagingException {
        // search = "SEARCH" [SP "CHARSET" SP astring] 1*(SP search-key)
        // ; CHARSET argument to MUST be registered with IANA

        charset.delete(0, charset.length());
        if (scanner.keyword("CHARSET")) {
            if (!scanner.is_char(' ')) {
                throw new ParserException("<SP>");
            }
            if (!scanner.astring(charset)) {
                throw new ParserException("<charset>");
            }
        }

        ArrayList<SearchTerm> list = new ArrayList<SearchTerm>();
        list.add(searchKey(scanner));
        while (scanner.is_char(' ')) {
            list.add(searchKey(scanner));
        }
        if (list.size() == 1) {
            return list.get(0);
        } else {
            SearchTerm[] sts = new SearchTerm[list.size()];
            sts = list.toArray(sts);
            return new AndTerm(sts);
        }
    }

    protected SearchTerm searchKey(IMAPScanner scanner) throws IOException, ParserException, MessagingException {
        // search-key = "ALL" / "ANSWERED" / "BCC" SP astring /
        // "BEFORE" SP date / "BODY" SP astring /
        // "CC" SP astring / "DELETED" / "FLAGGED" /
        // "FROM" SP astring / "KEYWORD" SP flag-keyword /
        // "NEW" / "OLD" / "ON" SP date / "RECENT" / "SEEN" /
        // "SINCE" SP date / "SUBJECT" SP astring /
        // "TEXT" SP astring / "TO" SP astring /
        // "UNANSWERED" / "UNDELETED" / "UNFLAGGED" /
        // "UNKEYWORD" SP flag-keyword / "UNSEEN" /
        // ; Above this line were in [IMAP2]
        // "DRAFT" / "HEADER" SP header-fld-name SP astring /
        // "LARGER" SP number / "NOT" SP search-key /
        // "OR" SP search-key SP search-key /
        // "SENTBEFORE" SP date / "SENTON" SP date /
        // "SENTSINCE" SP date / "SMALLER" SP number /
        // "UID" SP sequence-set / "UNDRAFT" / sequence-set /
        // "(" search-key *(SP search-key) ")"

        ComposedSequence sequenceSet = new ComposedSequence();

        if (scanner.is_char('(')) {
            ArrayList<SearchTerm> list = new ArrayList<SearchTerm>();
            list.add(search(scanner));
            while (scanner.is_char(' ')) {
                list.add(search(scanner));
            }
            if (!scanner.is_char(')')) {
                throw new ParserException("')'");
            }
            if (list.size() == 1) {
                return list.get(0);
            } else {
                SearchTerm[] sts = new SearchTerm[list.size()];
                sts = list.toArray(sts);
                return new AndTerm(sts);
            }
        } else if (scanner.keyword("OR")) {
            if (!scanner.is_char(' ')) {
                throw new ParserException("<SP>");
            }
            SearchTerm t1 = searchKey(scanner);
            if (!scanner.is_char(' ')) {
                throw new ParserException("<SP>");
            }
            SearchTerm t2 = searchKey(scanner);
            return new OrTerm(t1, t2);
        } else if (scanner.keyword("NOT")) {
            if (!scanner.is_char(' ')) {
                throw new ParserException("<SP>");
            }
            SearchTerm t1 = searchKey(scanner);
            return new NotTerm(t1);
        } else if (scanner.keyword("ANSWERED")) {
            return new FlagTerm(ANSWERED, true);
        } else if (scanner.keyword("UNANSWERED")) {
            return new FlagTerm(ANSWERED, false);
        } else if (scanner.keyword("DRAFT")) {
            return new FlagTerm(DRAFT, true);
        } else if (scanner.keyword("UNDRAFT")) {
            return new FlagTerm(DRAFT, false);
        } else if (scanner.keyword("DELETED")) {
            return new FlagTerm(DELETED, true);
        } else if (scanner.keyword("UNDELETED")) {
            return new FlagTerm(DELETED, false);
        } else if (scanner.keyword("FLAGGED")) {
            return new FlagTerm(FLAGGED, true);
        } else if (scanner.keyword("UNFLAGGED")) {
            return new FlagTerm(FLAGGED, false);
        } else if (scanner.keyword("SEEN")) {
            return new FlagTerm(SEEN, true);
        } else if (scanner.keyword("UNSEEN")) {
            return new FlagTerm(SEEN, false);
        } else if (scanner.keyword("NEW")) {
            return new AndTerm(new FlagTerm(RECENT, true), new FlagTerm(SEEN, false));
        } else if (scanner.keyword("RECENT")) {
            return new FlagTerm(RECENT, true);
        } else if (scanner.keyword("OLD")) {
            return new FlagTerm(RECENT, false);
        } else if (scanner.keyword("BODY")) {
            if (!scanner.is_char(' ')) {
                throw new ParserException("<SP>");
            }
            StringBuffer body = new StringBuffer();
            if (!scanner.astring(body)) {
                throw new ParserException("<string>");
            }
            return new BodyTerm(body.toString());
        } else if (scanner.keyword("TEXT")) {
            if (!scanner.is_char(' ')) {
                throw new ParserException("<SP>");
            }
            StringBuffer body = new StringBuffer();
            if (!scanner.astring(body)) {
                throw new ParserException("<string>");
            }
            return new TextTerm(body.toString());
        } else if (scanner.keyword("SUBJECT")) {
            if (!scanner.is_char(' ')) {
                throw new ParserException("<SP>");
            }
            StringBuffer subject = new StringBuffer();
            if (!scanner.astring(subject)) {
                throw new ParserException("<string>");
            }
            return new SubjectTerm(subject.toString());
        } else if (scanner.keyword("FROM")) {
            if (!scanner.is_char(' ')) {
                throw new ParserException("<SP>");
            }
            StringBuffer from = new StringBuffer();
            if (!scanner.astring(from)) {
                throw new ParserException("<string>");
            }
            return new FromStringTerm(from.toString());
        } else if (scanner.keyword("TO")) {
            if (!scanner.is_char(' ')) {
                throw new ParserException("<SP>");
            }
            StringBuffer to = new StringBuffer();
            if (!scanner.astring(to)) {
                throw new ParserException("<string>");
            }
            return new RecipientStringTerm(Message.RecipientType.TO, to.toString());
        } else if (scanner.keyword("CC")) {
            if (!scanner.is_char(' ')) {
                throw new ParserException("<SP>");
            }
            StringBuffer cc = new StringBuffer();
            if (!scanner.astring(cc)) {
                throw new ParserException("<string>");
            }
            return new RecipientStringTerm(Message.RecipientType.CC, cc.toString());
        } else if (scanner.keyword("BCC")) {
            if (!scanner.is_char(' ')) {
                throw new ParserException("<SP>");
            }
            StringBuffer bcc = new StringBuffer();
            if (!scanner.astring(bcc)) {
                throw new ParserException("<string>");
            }
            return new RecipientStringTerm(Message.RecipientType.BCC, bcc.toString());
        } else if (scanner.keyword("HEADER")) {
            if (!scanner.is_char(' ')) {
                throw new ParserException("<SP>");
            }
            StringBuffer name = new StringBuffer();
            if (!scanner.astring(name)) {
                throw new ParserException("<string>");
            }
            if (!scanner.is_char(' ')) {
                throw new ParserException("<SP>");
            }
            StringBuffer value = new StringBuffer();
            if (!scanner.astring(value)) {
                throw new ParserException("<string>");
            }
            return new HeaderTerm(name.toString(), value.toString());
        } else if (scanner.keyword("LARGER")) {
            if (!scanner.is_char(' ')) {
                throw new ParserException("<SP>");
            }
            IMAPScanner.Number number = new IMAPScanner.Number();
            if (!scanner.number(number)) {
                throw new ParserException("<number>");
            }
            return new SizeTerm(ComparisonTerm.GT, number.number);
        } else if (scanner.keyword("SMALLER")) {
            if (!scanner.is_char(' ')) {
                throw new ParserException("<SP>");
            }
            IMAPScanner.Number number = new IMAPScanner.Number();
            if (!scanner.number(number)) {
                throw new ParserException("<number>");
            }
            return new SizeTerm(ComparisonTerm.LT, number.number);
        } else if (scanner.keyword("BEFORE")) {
            if (!scanner.is_char(' ')) {
                throw new ParserException("<SP>");
            }
            GregorianCalendar date = new GregorianCalendar();
            if (!scanner.date(date)) {
                throw new ParserException("<date>");
            }
            return new ReceivedDateTerm(ComparisonTerm.LT, date.getTime());
        } else if (scanner.keyword("ON")) {
            if (!scanner.is_char(' ')) {
                throw new ParserException("<SP>");
            }
            GregorianCalendar date = new GregorianCalendar();
            if (!scanner.date(date)) {
                throw new ParserException("<date>");
            }
            return new ReceivedDateTerm(ComparisonTerm.EQ, date.getTime());
        } else if (scanner.keyword("SINCE")) {
            if (!scanner.is_char(' ')) {
                throw new ParserException("<SP>");
            }
            GregorianCalendar date = new GregorianCalendar();
            if (!scanner.date(date)) {
                throw new ParserException("<date>");
            }
            return new ReceivedDateTerm(ComparisonTerm.GT, date.getTime());
        } else if (scanner.keyword("SENTBEFORE")) {
            if (!scanner.is_char(' ')) {
                throw new ParserException("<SP>");
            }
            GregorianCalendar date = new GregorianCalendar();
            if (!scanner.date(date)) {
                throw new ParserException("<date>");
            }
            return new SentDateTerm(ComparisonTerm.LT, date.getTime());
        } else if (scanner.keyword("SENTON")) {
            if (!scanner.is_char(' ')) {
                throw new ParserException("<SP>");
            }
            GregorianCalendar date = new GregorianCalendar();
            if (!scanner.date(date)) {
                throw new ParserException("<date>");
            }
            return new SentDateTerm(ComparisonTerm.EQ, date.getTime());
        } else if (scanner.keyword("SENTSINCE")) {
            if (!scanner.is_char(' ')) {
                throw new ParserException("<SP>");
            }
            GregorianCalendar date = new GregorianCalendar();
            if (!scanner.date(date)) {
                throw new ParserException("<date>");
            }
            return new SentDateTerm(ComparisonTerm.GT, date.getTime());
        } else if (scanner.keyword("KEYWORD")) {
            if (!scanner.is_char(' ')) {
                throw new ParserException("<SP>");
            }
            StringBuffer name = new StringBuffer();
            if (!scanner.astring(name)) {
                throw new ParserException("<string>");
            }
            return new NoneTerm(); // we are not supporting user flags!
        } else if (scanner.keyword("UNKEYWORD")) {
            if (!scanner.is_char(' ')) {
                throw new ParserException("<SP>");
            }
            StringBuffer name = new StringBuffer();
            if (!scanner.astring(name)) {
                throw new ParserException("<string>");
            }
            return new AllTerm(); // we are not supporting user flags!
        } else if (scanner.keyword("ALL")) {
            return new AllTerm();
        } else if (scanner.keyword("UID")) {
            if (!scanner.is_char(' ')) {
                throw new ParserException("<SP>");
            }
            if (!scanner.sequence_set(sequenceSet)) {
                throw new ParserException("<sequence_set>");
            }
            return new UIDTerm(sequenceSet);
        } else if (scanner.sequence_set(sequenceSet)) {
            if (!scanner.is_char(' ')) {
                throw new ParserException("<SP>");
            }
            SearchTerm t1 = searchKey(scanner);
            return new SequenceTerm(sequenceSet, t1);
        } else {
            throw new ParserException("'ALL', 'ANSWERED', 'BCC', " + "'BEFORE', 'BODY', 'CC', 'DELETED', 'FLAGGED', " + "'FROM', 'KEYWORD', 'NEW', 'OLD', 'ON', 'RECENT', 'SEEN', "
                    + "'SINCE', 'SUBJECT', 'TEXT', 'TO', 'UNANSWERED', 'UNDELETED', 'UNFLAGGED', " + "'UNKEYWORD', 'UNSEEN', " +
                    // Above this line were in [IMAP2]
                    "'DRAFT', 'HEADER', 'LARGER', 'NOT', 'OR', 'SENTBEFORE', " + "'SENTON', +'SENTSINCE', 'SMALLER', 'UNDRAFT', '('" + "\n\nBut got: " + scanner.peek_line());
        }

    }

    public class AllTerm extends SearchTerm {
        public AllTerm() {
        } // AllTerm

        @Override
        public boolean match(Message m) {
            return true;
        }
    }

    public class NoneTerm extends SearchTerm {
        public NoneTerm() {
        } // NoneTerm

        @Override
        public boolean match(Message m) {
            return false;
        }
    }

    public class TextTerm extends StringTerm {
        public TextTerm(String searchText) {
            super(searchText);
        }

        @Override
        public boolean match(Message m) {
            if (m instanceof MimePart) {
                try {
                    if (MessageUtilities.createHeaders((MimePart) m).indexOf(pattern) > 0) {
                        return true;
                    }
                } catch (MessagingException e) {
                    logger.error("Search message error", e);
                    // TODO !!! session.setKeepLog(true);
                }
            }
            BodyTerm bt = new BodyTerm(pattern);
            return bt.match(m);
        }

    }

    public class UIDTerm extends SearchTerm {
        Sequence sequence;

        public UIDTerm(Sequence sequence) {
            this.sequence = sequence;
        }

        @Override
        public boolean match(Message m) {
            try {
                int uid = (int) MessageUtilities.findUID(m);
                return sequence.belongs(uid);
            } catch (MessagingException e) {
            }
            return false;
        }

    }

    public static class SequenceTerm extends SearchTerm {
        Sequence sequence;
        SearchTerm searchTerm;

        public SequenceTerm(Sequence sequence, SearchTerm searchTerm) {
            this.sequence = sequence;
            this.searchTerm = searchTerm;
        }

        @Override
        public boolean match(Message m) {
            return sequence.belongs(m.getMessageNumber()) && searchTerm.match(m);
        }
    }

}
