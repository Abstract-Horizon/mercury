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
 * Response for capability command
 *
 * @author Daniel Sendula
 */
public class CapabilityResponse extends MnemonicResponse {
    // TODO can be done better - with a list of capabilities!


    /**
     * Constructor
     * @param session
     */
    public CapabilityResponse(IMAPSession session) {
        super(session, Response.UNTAGGED_RESPONSE, "CAPABILITY", getCapabilityList(session));
    }

    /**
     * Retuns list of capability strings depending on session state
     * @param session
     * @return list of capability strings depending on session state
     */
    public static String getCapabilityList(IMAPSession session) {
        if (session.isInsecureAllowed()) {
            //return "IMAP4rev1 AUTH=LOGIN STARTTLS IDLE";
            return "IMAP4rev1 AUTH=PLAIN STARTTLS";
        } else {
            if (session.isSecure()) {
                //return "IMAP4rev1 AUTH=LOGIN IDLE";
                return "IMAP4rev1 AUTH=PLAIN";
            } else {
                // return "IMAP4rev1 STARTTLS LOGINDISABLED IDLE";
                return "IMAP4rev1 STARTTLS LOGINDISABLED";
            }
        }
    }

}
