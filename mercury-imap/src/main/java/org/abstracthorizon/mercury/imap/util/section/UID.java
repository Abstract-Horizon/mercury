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
 * UID section
 *
 * @author Daniel Sendula
 */
public class UID {

    /** UID string */
    public static final String UID = "UID";

    /** Hash code of UID string */
    public static final int hash = UID.hashCode();

    /** Static UID instance */
    public static final UID instance = new UID();

    /**
     * Returns string representation of this object
     * @return string representation of this object
     */
    public String toString() {
        return UID;
    }

    /**
     * Returns <code>true</code> if object is of UID type
     * @retrun <code>true</code> if object is of UID type
     */
    public boolean equals(Object o) {
        return (o instanceof UID);
    }

    /**
     * Returns hash code
     * @return hash code
     */
    public int hashCode() {
        return hash;
    }
}

