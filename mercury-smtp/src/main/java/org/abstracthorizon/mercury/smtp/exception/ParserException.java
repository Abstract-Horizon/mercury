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
package org.abstracthorizon.mercury.smtp.exception;

/**
 * Parser exception
 *
 * @author Daniel Sendula
 */
public class ParserException extends SMTPCommandException {

    /**
     * Constructor
     */
    public ParserException() {
        super();
    }

    /**
     * Constructor - it constructs message &quot;Syntax error - expected <&quot; + msg + &quot;>&quot;
     * @param msg message
     */
    public ParserException(String msg) {
        this(true, "Syntax error - expected <" + msg + ">");
    }

    /**
     * Constructor
     * @param missing is message name of missing element
     * @param msg message
     */
    public ParserException(boolean missing, String msg) {
        super(compose(missing, msg));
    }

    /**
     * Composes string &quot;Syntax error - expected &quot; + msg.
     * @param missing is message name of missing element
     * @param msg message
     * @return new string
     */
    protected static String compose(boolean missing, String msg) {
        if (missing) { return "Syntax error - expected " + msg; }
        return msg;
    }

    public String toString() {
       return super.toString();
    }
}
