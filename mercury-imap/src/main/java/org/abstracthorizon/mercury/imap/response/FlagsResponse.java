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
package org.abstracthorizon.mercury.imap.response;

import javax.mail.Flags;
import org.abstracthorizon.mercury.imap.IMAPSession;

/**
 * Flags response
 *
 * @author Daniel Sendula
 */
public class FlagsResponse extends MnemonicResponse {

    /**
     * Response
     * @param session imap session
     * @param flags flags
     */
    public FlagsResponse(IMAPSession session, Flags flags) {
        super(session, Response.UNTAGGED_RESPONSE, "FLAGS", getFlagsList(flags));
    }

    /**
     * Returns list of flags as string
     * @param flags flags
     * @return list of flags as string
     */
    public static String getFlagsList(Flags flags) {
        StringBuffer flgs = new StringBuffer();
        flgs.append('(');
        if (flags.contains(Flags.Flag.ANSWERED)) {
            flgs.append("\\Answered");
        }
        if (flags.contains(Flags.Flag.FLAGGED)) {
            if (flgs.length() > 0) {
                flgs.append(' ');
            }
            flgs.append("\\Flagged");
        }
        if (flags.contains(Flags.Flag.DELETED)) {
            if (flgs.length() > 0) {
                flgs.append(' ');
            }
            flgs.append("\\Deleted");
        }
        if (flags.contains(Flags.Flag.SEEN)) {
            if (flgs.length() > 0) {
                flgs.append(' ');
            }
            flgs.append("\\Seen");
        }
        if (flags.contains(Flags.Flag.DRAFT)) {
            if (flgs.length() > 0) {
                flgs.append(' ');
            }
            flgs.append("\\Draft");
        }
        if (flags.contains(Flags.Flag.RECENT)) {
            if (flgs.length() > 0) {
                flgs.append(' ');
            }
            flgs.append("\\Recent");
        }
        flgs.append(')');
        return flgs.toString();
    }

}
