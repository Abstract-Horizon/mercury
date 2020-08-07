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
package org.abstracthorizon.mercury.imap.util;


/**
 * Parser exception
 *
 * @author Daniel Sendula
 */
public class ParserException extends Exception {

    /**
     * Constructor
     */
    public ParserException() {
        super();
    }

    /**
     * Constructor
     * @param msg message
     */
    public ParserException(String msg) {
        this(true, "Syntax error - expected <"+msg+">");
    }

    /**
     * Constructor
     * @param missing is missing keyword exception
     * @param msg keyword
     */
    public ParserException(boolean missing, String msg) {
        super(compose(missing, msg));
    }

    /**
     * Composes string
     * @param missing is missing keyword or should this method just return message
     * @param msg keyword or message
     * @return composed stirng
     */
    protected static String compose(boolean missing, String msg) {
        if (missing) {
            return "Syntax error - expected "+msg;
        } else {
            return msg;
        }
    }
}
