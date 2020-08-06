/*
 * Copyright (c) 2004-2019 Creative Sphere Limited.
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
package org.abstracthorizon.mercury.sync.exception;

import org.abstracthorizon.mercury.common.command.CommandException;


/**
 * Exceptions thrown by IMAP commands.
 *
 * @author Daniel Sendula
 */
public class SyncCommandException extends CommandException {

    /**
     * Constructor
     */
    public SyncCommandException() {
        super();
    }

    /**
     * Constructor
     * @param msg message
     */
    public SyncCommandException(String msg) {
        super(msg);
    }

    /**
     * Constructor
     * @param msg message
     * @param e cause
     */
    public SyncCommandException(String msg, Exception e) {
        super(msg, e);
    }

    /**
     * Constructor
     * @param e cause
     */
    public SyncCommandException(Exception e) {
        super(e);
    }

}
