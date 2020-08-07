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
import org.abstracthorizon.mercury.imap.response.BADResponse;

/**
 * Artificial command representing unknown command
 *
 * @author Daniel Sendula
 */
public class Bad extends IMAPCommand {

    /** Error message */
    protected String error;

    /**
     * Constructor
     */
    public Bad() {
        super("");
    }

    /**
     * Sets error message
     * @param error error message
     */
    public void setErrorString(String error) {
        this.error = error;
    }

    /**
     * Executes the command
     * @param session
     * @throws IOException
     */
    public void execute(IMAPSession session) throws IOException {
        if (error == null) {
            new BADResponse(session, "Command").submit();
        } else {
            new BADResponse(session, error).submit();
        }
    }
}
