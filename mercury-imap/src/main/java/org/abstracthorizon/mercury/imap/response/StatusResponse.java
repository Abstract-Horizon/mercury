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
 * Status response
 *
 * @author Daniel Sendula
 */
public class StatusResponse extends MnemonicResponse {

    /**
     * Constructor
     * @param session imap session
     */
    public StatusResponse(IMAPSession session) {
        super(session, Response.UNTAGGED_RESPONSE,  null, "STATUS");
    }
}
