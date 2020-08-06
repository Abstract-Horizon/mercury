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

import org.abstracthorizon.mercury.imap.IMAPSession;

/**
 * Numeric response
 *
 * @author Daniel Sendula
 */
public class NumberResponse extends Response {

    /** Number */
    protected int number;

    /** Mnemonic */
    protected String mnemonic;

    /**
     * Constructor
     * @param session imap session
     * @param mnemonic mnemonic
     * @param number number
     */
    public NumberResponse(IMAPSession session, String mnemonic, int number) {
        super(session, Response.UNTAGGED_RESPONSE);
        append(number);
        append(' ');
        append(mnemonic);
    }

    /**
     * Constructor
     * @param session imap session
     * @param mnemonic mnemonic
     * @param number nuimber
     * @param tagged is tagged
     */
    public NumberResponse(IMAPSession session, String mnemonic, int number, boolean tagged) {
        super(session, tagged ? Response.TAGGED_RESPONSE : Response.UNTAGGED_RESPONSE);
        append(number);
        append(' ');
        append(mnemonic);
    }

}
