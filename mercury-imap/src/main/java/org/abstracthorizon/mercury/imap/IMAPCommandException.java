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
package org.abstracthorizon.mercury.imap;

import org.abstracthorizon.mercury.common.command.CommandException;

/**
 * Exceptions thrown by IMAP commands.
 *
 * @author Daniel Sendula
 */
public class IMAPCommandException extends CommandException {

    /**
     * Constructor
     */
    public IMAPCommandException() {
        super();
    }

    /**
     * Constructor
     * @param msg message
     */
    public IMAPCommandException(String msg) {
        super(msg);
    }

    /**
     * Constructor
     * @param msg message
     * @param e cause
     */
    public IMAPCommandException(String msg, Exception e) {
        super(msg, e);
    }

    /**
     * Constructor
     * @param e cause
     */
    public IMAPCommandException(Exception e) {
        super(e);
    }
}
