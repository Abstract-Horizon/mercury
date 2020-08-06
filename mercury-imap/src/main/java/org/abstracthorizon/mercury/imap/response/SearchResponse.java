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
 * Response for search command
 *
 * @author Daniel Sendula
 */
public class SearchResponse extends MnemonicResponse {

    /**
     * Constructor
     * @param session
     */
    public SearchResponse(IMAPSession session) {
        super(session, Response.UNTAGGED_RESPONSE, "SEARCH");
    }

    /**
     * Adds number of message that confirms to selected criteria
     * @param number message number or UID
     */
    public void addMessageNumber(long number) {
        append(' ');
        append(Long.toString(number));
    }

}
