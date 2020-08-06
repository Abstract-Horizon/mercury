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


/**
 * Exception for NO IMAP commands errors.
 *
 * @author Daniel Sendula
 */
public class NOCommandException extends IMAPCommandException {

    /**
     * Constructor
     */
    public NOCommandException() {
        super();
    }

    /**
     * Constructor
     * @param msg message
     */
    public NOCommandException(String msg) {
        super(msg);
    }

    /**
     * Constructor
     * @param msg message
     * @param e cause
     */
    public NOCommandException(String msg, Exception e) {
        super(msg, e);
    }

    /**
     * Constructor
     * @param e cause
     */
    public NOCommandException(Exception e) {
        super(e);
    }
}
