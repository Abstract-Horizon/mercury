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

import org.abstracthorizon.mercury.imap.IMAPSession;
import org.abstracthorizon.mercury.imap.response.CapabilityResponse;
import org.abstracthorizon.mercury.imap.util.ParserException;


/**
 * Capability IMAP command
 *
 * @author Daniel Sendula
 */
public class Capability extends IMAPCommand {

    /**
     * Constructor
     * @param mnemonic mnemonic
     */
    public Capability(String mnemonic) {
        super(mnemonic);
    }

    /**
     * Executes the command
     * @param session
     * @throws ParserException
     * @throws IOException
     */
    protected void execute(IMAPSession session) throws IOException, ParserException {
        checkEOL(session);
        new CapabilityResponse(session).submit();
        sendOK(session);
    }
}
