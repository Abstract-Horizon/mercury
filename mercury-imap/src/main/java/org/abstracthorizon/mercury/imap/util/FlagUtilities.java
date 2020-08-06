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
package org.abstracthorizon.mercury.imap.util;

import javax.mail.Flags;

/**
 * Utilities for flags
 *
 * @author Daniel Sendula
 */
public class FlagUtilities {

    /**
     * Returns string representation of flags
     * @param flags flags
     * @return  // DateHolder
     */
    public static String toString(Flags flags) {
        if (flags == null) {
            return "";
        }
        StringBuffer buf = new StringBuffer();
        Flags.Flag[] fs = flags.getSystemFlags();
        boolean first = true;
        if ((fs != null) && (fs.length > 0)) {
            for (int i=0; i<fs.length; i++) {
                if (i > 0) {
                    buf.append(' ');
                }
                if (fs[i].equals(Flags.Flag.ANSWERED)) {
                    buf.append("\\Answered");
                } else if (fs[i].equals(Flags.Flag.DELETED)) {
                    buf.append("\\Deleted");
                } else if (fs[i].equals(Flags.Flag.FLAGGED)) {
                    buf.append("\\Flagged");
                } else if (fs[i].equals(Flags.Flag.SEEN)) {
                    buf.append("\\Seen");
                } else if (fs[i].equals(Flags.Flag.DRAFT)) {
                    buf.append("\\Draft");
                } else if (fs[i].equals(Flags.Flag.RECENT)) {
                    buf.append("\\Recent");
                }
            } // for
            first = false;
        }
        String[] ss = flags.getUserFlags();
        if ((ss != null) && (ss.length > 0)) {
            for (int i=0; i<ss.length; i++) {
                if (!first) {
                    buf.append(',');
                } else {
                    first = false;
                }
                buf.append(ss[i]);
            } // for
        }
        return buf.toString();
    }

    /**
     * Returns maildir representation of flags
     * @param flags flags
     * @return maildir string of flags
     */
    public static String toMaildirString(Flags flags) {
        if (flags == null) {
            return "";
        }
        StringBuffer buf = new StringBuffer();
        if (flags.contains(Flags.Flag.DRAFT)) {
            buf.append('D');
        }
        if (flags.contains(Flags.Flag.FLAGGED)) {
            buf.append('F');
        }
        if (flags.contains(Flags.Flag.ANSWERED)) {
            buf.append('R');
        }
        if (flags.contains(Flags.Flag.SEEN)) {
            buf.append('S');
        }
        if (flags.contains(Flags.Flag.DELETED)) {
            buf.append('T');
        }
        return buf.toString();
    }
}
