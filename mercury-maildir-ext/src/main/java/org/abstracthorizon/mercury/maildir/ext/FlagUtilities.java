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
package org.abstracthorizon.mercury.maildir.ext;

import javax.mail.Flags;

/**
 * Utility class for handling maildir flags
 *
 *  @author Daniel Sendula
 */
public class FlagUtilities {

    /**
     * Masked private constructor
     */
    private FlagUtilities() {
    }

    /**
     * Creates maildir string from given flags.
     * <ul>
     * <li>&quot;D&quot; for DRAFT</li>
     * <li>&quot;F&quot; for FLAGGED</li>
     * <li>&quot;R&quot; for ANSWERED</li>
     * <li>&quot;S&quot; for SEEN</li>
     * <li>&quot;T&quot; for DELETED</li>
     * </ul>
     *
     * @param flags flags to be converted
     * @return string in maildir format
     */
    public static String toMaildirString(Flags flags) {
        if (flags == null) {
            return "";
        }
        StringBuffer buf = new StringBuffer();
        if (flags.contains(javax.mail.Flags.Flag.DRAFT)) {
            buf.append('D');
        }
        if (flags.contains(javax.mail.Flags.Flag.FLAGGED)) {
            buf.append('F');
        }
        if (flags.contains(javax.mail.Flags.Flag.ANSWERED)) {
            buf.append('R');
        }
        if (flags.contains(javax.mail.Flags.Flag.SEEN)) {
            buf.append('S');
        }
        if (flags.contains(javax.mail.Flags.Flag.DELETED)) {
            buf.append('T');
        }
        return buf.toString();
    }

    /**
     * Creates set of flags from maildir flags string.
     * <ul>
     * <li>For &quot;D&quot; adds DRAFT</li>
     * <li>For &quot;F&quot; adds FLAGGED</li>
     * <li>For &quot;R&quot; adds ANSWERED</li>
     * <li>For &quot;S&quot; adds SEEN</li>
     * <li>For &quot;T&quot; adds DELETED</li>
     * </ul>
     * @param str maildir flags string
     * @return flags set
     */
    public static Flags fromMaildirString(String str) {
        Flags flags = new Flags();
        int len = str.length();
        if ((str == null) || (len == 0)) {
            return flags;
        }
        for (int i = 0; i < len; i++) {
            char c = Character.toUpperCase(str.charAt(i));
            if (c == 'D') {
                flags.add(Flags.Flag.DRAFT);
            } else if (c == 'F') {
                flags.add(Flags.Flag.FLAGGED);
            } else if (c == 'R') {
                flags.add(Flags.Flag.ANSWERED);
            } else if (c == 'S') {
                flags.add(Flags.Flag.SEEN);
            } else if (c == 'T') {
                flags.add(Flags.Flag.DELETED);
            }
        }
        return flags;
    }

}
