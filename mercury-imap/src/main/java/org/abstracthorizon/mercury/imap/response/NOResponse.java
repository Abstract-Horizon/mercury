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
package org.abstracthorizon.mercury.imap.response;

import org.abstracthorizon.mercury.imap.IMAPSession;

/**
 * NO response
 *
 * @author Daniel Sendula
 */
public class NOResponse extends MnemonicResponse {

    /**
     * Tagged NO response
     * @param session session
     * @param msg message
     */
    public NOResponse(IMAPSession session, String msg) {
        super(session, Response.TAGGED_RESPONSE, "NO", msg);
    }

    /**
     * NO response
     * @param session session
     * @param type respose type
     * @param msg message
     */
    public NOResponse(IMAPSession session, int type, String msg) {
        super(session, type, "NO", msg);
    }
}
