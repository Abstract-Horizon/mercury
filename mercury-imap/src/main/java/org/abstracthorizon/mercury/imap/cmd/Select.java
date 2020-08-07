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

import java.io.IOException;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.UIDFolder;
import org.abstracthorizon.mercury.common.command.CommandException;
import org.abstracthorizon.mercury.imap.IMAPSession;
import org.abstracthorizon.mercury.imap.response.ExistsResponse;
import org.abstracthorizon.mercury.imap.response.FlagsResponse;
import org.abstracthorizon.mercury.imap.response.OKResponse;
import org.abstracthorizon.mercury.imap.response.RecentResponse;
import org.abstracthorizon.mercury.imap.response.Response;
import org.abstracthorizon.mercury.imap.util.FlagUtilities;
import org.abstracthorizon.mercury.imap.util.ParserException;

/**
 * Select IMAP command
 *
 * @author Daniel Sendula
 */
public class Select extends IMAPCommand {

    /** Flags */
    public static final Flags flags = new Flags();
    static {
        flags.add(Flags.Flag.ANSWERED);
        flags.add(Flags.Flag.FLAGGED);
        flags.add(Flags.Flag.DELETED);
        flags.add(Flags.Flag.SEEN);
        flags.add(Flags.Flag.DRAFT);
        //flags.add(Flags.Flag.RECENT);
    }

    /**
     * Constructor
     * @param mnemonic mnemonic
     */
    public Select(String mnemonic) {
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
        executeImpl(session, false);
    }

    /**
     * Executes the command
     * @param session
     * @param readonly is select readonly or not
     * @throws ParserException
     * @throws MessagingException
     * @throws CommandException
     * @throws IOException
     */
    protected void executeImpl(IMAPSession session, boolean readonly) throws ParserException, MessagingException, IOException {
        StringBuffer mailboxName = new StringBuffer();
        if (!session.getScanner().mailbox(mailboxName)) {
            throw new ParserException("<mailbox_name>");
        }
        checkEOL(session);

        Folder f = session.getStore().getFolder(mailboxName.toString());
        Folder selectedFolder = session.getSelectedFolder();
        if ((selectedFolder != null) && (selectedFolder.isOpen())) {
            selectedFolder.close(false);
        }

        if (readonly) {
            f.open(Folder.READ_ONLY);
        } else {
            f.open(Folder.READ_WRITE);
        }
        session.setSelectedFolder(f);

//        S: * 172 EXISTS
//        S: * 1 RECENT
//        S: * OK [UNSEEN 12] Message 12 is first unseen
//        S: * OK [UIDVALIDITY 3857529045] UIDs valid
//        S: * OK [UIDNEXT 4392] Predicted next UID
//        S: * FLAGS (\Answered \Flagged \Deleted \Seen \Draft)
//        S: * OK [PERMANENTFLAGS (\Deleted \Seen \*)] Limited

        int msgNum = f.getMessageCount();
        int unseen = f.getUnreadMessageCount();
        long uidValidity = 0;
        int nextUID = 0;
        if (f instanceof UIDFolder) {
            UIDFolder ff = (UIDFolder)f;
            uidValidity = ff.getUIDValidity();
            if (msgNum > 0) {
                Message m = f.getMessage(msgNum);
                nextUID = (int)(ff.getUID(m)+1);
            }
        }

        new ExistsResponse(session, f).submit();
        new RecentResponse(session, f).submit();

        new OKResponse(session, Response.UNTAGGED_RESPONSE, "[UNSEEN "+unseen+"]").submit();
        new OKResponse(session, Response.UNTAGGED_RESPONSE, "[UIDVALIDITY "+uidValidity+"]").submit();


        if (f instanceof UIDFolder) {
            new OKResponse(session, Response.UNTAGGED_RESPONSE, "[UIDNEXT "+nextUID+"] Predicted next UID").submit();
        }

        new FlagsResponse(session, flags).submit();

        new OKResponse(session, Response.UNTAGGED_RESPONSE, "[PERMANENTFLAGS ("+FlagUtilities.toString(f.getPermanentFlags())+")]").submit();
        sendOK(session);
    }
}
