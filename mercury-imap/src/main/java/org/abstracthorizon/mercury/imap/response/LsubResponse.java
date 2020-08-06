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
 * LSub response
 *
 * @author Daniel Sendula
 */
public class LsubResponse extends ListResponse {

    /**
     * Constructor
     * @param session imap session
     * @param f folder
     * @throws MessagingException
     */
    public LsubResponse(IMAPSession session, Folder f) throws MessagingException {
        super(session, "LSUB", f);
    }


}
