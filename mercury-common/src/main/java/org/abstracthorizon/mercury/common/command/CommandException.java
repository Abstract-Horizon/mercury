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
package org.abstracthorizon.mercury.common.command;

import org.abstracthorizon.danube.connection.ConnectionException;

/**
 * Command exception
 *
 * @author Daniel Sendula
 */
public class CommandException extends ConnectionException {

    /**
     * COnstructor
     */
    public CommandException() {
        super();
    }

    /**
     * Constructor
     * @param msg message
     */
    public CommandException(String msg) {
        super(msg);
    }

    /**
     * Constructor
     * @param msg message
     * @param e cause
     */
    public CommandException(String msg, Exception e) {
        super(msg, e);
    }

    /**
     * Constructor
     * @param e cause
     */
    public CommandException(Exception e) {
        super(e);
    }
}
