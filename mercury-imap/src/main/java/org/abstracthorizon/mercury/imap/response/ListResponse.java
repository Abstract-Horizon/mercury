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
package org.abstracthorizon.mercury.imap.response;

import javax.mail.Folder;
import javax.mail.MessagingException;
import org.abstracthorizon.mercury.imap.IMAPSession;

/**
 * List response
 *
 * @author Daniel Sendula
 */
public class ListResponse extends MnemonicResponse {

    /** Message */
    protected String msg;

    /**
     * Constructor
     * @param session imap session
     * @param f folder
     * @throws MessagingException
     */
    public ListResponse(IMAPSession session, Folder f) throws MessagingException {
        this(session, "LIST", f);
    }

    /**
     * Constructor
     * @param session imap session
     * @param mnemonic mnemonic
     * @param f folder
     * @throws MessagingException
     */
    protected ListResponse(IMAPSession session, String mnemonic, Folder f) throws MessagingException {
        super(session, Response.UNTAGGED_RESPONSE, mnemonic, composeMessage(f));
    }

    /**
     * Composes the message
     * @param f folder
     * @return string representation
     * @throws MessagingException
     */
    public static final String composeMessage(Folder f) throws MessagingException {
        String flags = "";
        if ((f.getType() & Folder.HOLDS_FOLDERS) == 0) {
            flags = "\\Noinferiors";
        } else if ((f.getType() & Folder.HOLDS_MESSAGES) == 0) {
            flags = "\\Noselect";
        }
        return "("+flags+") \""+f.getSeparator()+"\" \""+f.getFullName()+"\"";
    }

}
