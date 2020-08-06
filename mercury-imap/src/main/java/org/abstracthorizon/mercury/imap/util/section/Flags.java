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
package org.abstracthorizon.mercury.imap.util.section;

/**
 * Flags
 *
 * @author Daniel Sendula
 */
public class Flags {

    /** Plus keyword */
    public boolean plus = false;

    /** Minus keyword */
    public boolean minus = false;

    /** Silend request */
    public boolean silent = false;

    /** Flags */
    public javax.mail.Flags flags = new javax.mail.Flags();

    /**
     * Returns string representation of this object
     * @return string representation of this object
     */
    public String toString() {
        return "FLAGS";
    }
}
