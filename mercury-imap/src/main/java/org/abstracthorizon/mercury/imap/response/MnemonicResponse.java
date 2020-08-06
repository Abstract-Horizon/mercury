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
 * Mnemonic response
 *
 * @author Daniel Sendula
 */
public class MnemonicResponse extends Response {
  
    /** Mnemonic */
    protected String mnemonic;
    
    /**
     * Mnemonic response
     * @param session session
     * @param type response type
     * @param mnemonic mnemonic
     */
    public MnemonicResponse(IMAPSession session, int type, String mnemonic) {
        super(session, type);
        append(mnemonic);
    }
    
    /**
     * Mnemonic response with message
     * @param session session
     * @param type response type
     * @param mnemonic mnemonic
     * @param msg message
     */
    public MnemonicResponse(IMAPSession session, int type, String mnemonic, String msg) {
        super(session, type);
        append(mnemonic);
        append(' ');
        append(msg);
    }
    
}
