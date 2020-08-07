/*
 * Copyright (c) 2005-2020 Creative Sphere Limited.
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
package org.abstracthorizon.mercury.maildir.uid;


/**
 * This class represents UID number
 *
 * @author Daniel Sendula
 */
public class UID {

    /** Uid number */
    protected long uid;

    /**
     * Constructor
     * @param uid uid
     */
    public UID(long uid) {
        this.uid = uid;
    }

    /**
     * Returns uid number
     * @return uid number
     */
    public long getUID() {
        return uid;
    }

    /**
     * Returns uid converted to integer as hash code.
     * @return uid converted to integer as hash code.
     */
    public int hashCode() {
        return (int)uid;
    }

    /**
     * Compares two <code>UID</code> objects
     * @param o object to be compared with
     * @return <code>true</code> if uids are the same
     */
    public boolean equals(Object o) {
        if (o instanceof UID) {
            return ((UID)o).uid == uid;
        }
        return false;
    }

    /**
     * Returns uid as long converted to string
     * @return uid as long converted to string
     */
    public String toString() {
        return Long.toString(uid);
    }
}
