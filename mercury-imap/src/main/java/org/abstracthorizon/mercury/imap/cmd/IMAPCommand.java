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

import org.abstracthorizon.danube.connection.Connection;
import org.abstracthorizon.danube.connection.ConnectionHandler;
import org.abstracthorizon.danube.support.RuntimeIOException;
import org.abstracthorizon.mercury.common.command.CommandException;
import org.abstracthorizon.mercury.imap.BADCommandException;
import org.abstracthorizon.mercury.imap.IMAPSession;
import org.abstracthorizon.mercury.imap.NOCommandException;
import org.abstracthorizon.mercury.imap.response.ExistsResponse;
import org.abstracthorizon.mercury.imap.response.NOResponse;
import org.abstracthorizon.mercury.imap.response.OKResponse;
import org.abstracthorizon.mercury.imap.response.RecentResponse;
import org.abstracthorizon.mercury.imap.util.ParserException;

import java.io.IOException;

import javax.mail.Folder;
import javax.mail.MessagingException;

/**
 * A class that represents IMAP command
 *
 * @author Daniel Sendula
 */
public class IMAPCommand implements ConnectionHandler {

    public static final short SEND_WHEN_HAVE_NEW = 0;
    public static final short ALWAYS_SEND_UNILATERAL_DATA = 1;
    public static final short ALWAYS_SUPRESS_UNILATERAL_DATA = 2;

    protected short unilateral = SEND_WHEN_HAVE_NEW;

    /** Mnemonic */
    protected String mnemonic;

    /**
     * Constructor
     * @param mnemonic mnemonic
     */
    public IMAPCommand(String mnemonic) {
        this.mnemonic = mnemonic;
    }

    /**
     * Returns mnemonic
     * @return mnemonic
     */
    public String getMnemonic() {
        return mnemonic;
    }

    /**
     * Handles connection
     * @param connection connection
     */
    public void handleConnection(Connection connection) {
        IMAPSession session = (IMAPSession)connection;
        try {
            session.markCommandStarted();
            execute(session);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        } catch (ParserException e) {
            throw new BADCommandException(e);
        } catch (MessagingException e) {
            throw new NOCommandException(e);
        }
    }

    /**
     * Executes the command. This implemnetation returns {@link NOResponse}.
     * @param session session
     * @throws ParserException
     * @throws MessagingException
     * @throws CommandException
     * @throws IOException
     */
    protected void execute(IMAPSession session) throws ParserException, MessagingException, CommandException, IOException {
        checkEOL(session);
        new NOResponse(session, getMnemonic()+" unimplemented").submit();
    }

    /**
     * Sets {@link OKResponse}
     * @param session session
     * @throws IOException
     */
    protected void sendOK(IMAPSession session) throws IOException {
        new OKResponse(session, mnemonic+" Completed in "+session.getCommandLasted()+"ms").submit();
    }

    /**
     * Checks if command is read fully
     * @param session session
     * @throws IOException
     * @throws ParserException
     */
    protected void checkEOL(IMAPSession session) throws IOException, ParserException {
        session.getScanner().check_eol();
        if (unilateral != ALWAYS_SUPRESS_UNILATERAL_DATA) {
            try {
                Folder folder = session.getSelectedFolder();
                if (folder != null) {
                    if ((unilateral == ALWAYS_SEND_UNILATERAL_DATA) || (folder.getNewMessageCount() > 0)) {
                        new ExistsResponse(session, folder).submit();
                        new RecentResponse(session, folder).submit();
                    }
                }
            } catch (MessagingException ignore) {
                // We can't do much or even want to fix or report this...
            }
        }
    }
}
