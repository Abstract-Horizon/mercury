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
package org.abstracthorizon.mercury.filter.spam;

import org.abstracthorizon.mercury.smtp.filter.Filter;
import org.abstracthorizon.mercury.smtp.filter.MailSessionData;
import org.abstracthorizon.mercury.smtp.util.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This filter checks if mailbox is local and if not slows down current thread for given amount of milliseconds.
 *
 * @author Daniel Sendula
 */
public class DestinationMailboxFilter implements Filter {

    protected static final Logger logger = LoggerFactory.getLogger(DestinationMailboxFilter.class);

    /** Spam slowdown */
    protected int spamSlowDown = 250;

    /**
     * Constructor
     */
    public DestinationMailboxFilter() {
    }

    /**
     * Sets time in millis for thread to sleep if spam is recognised
     * @param millis time in millis for thread to sleep if spam is recognised
     */
    public void setSpamSlowDown(int millis) {
        this.spamSlowDown = millis;
    }

    /**
     * Returns time in millis thread is going to sleep if spam recognised
     * @return time in millis for thread to sleep if spam is recognised
     */
    public int getSpamSlowDown() {
        return spamSlowDown;
    }

    /**
     * Returns {@link Filter#CAN_PROCESS_DESTINATION_MAILBOX}
     * @return {@link Filter#CAN_PROCESS_DESTINATION_MAILBOX}
     */
    public int features() {
        return Filter.CAN_PROCESS_DESTINATION_MAILBOX;
    }

    /**
     * Does nothing
     * @param data mail session
     */
    public void startSession(MailSessionData data) {
    }

    /**
     * Returns null
     * @param data mail session
     * @return null
     */
    public String processSourceDomain(MailSessionData data) {
        return null;
    }

    /**
     * Returns null
     * @param data mail session
     * @return null
     */
    public String processSourceMailbox(MailSessionData data) {
        return null;
    }

    /**
     * Checks if destination mailbox exists and if not so slows down thread for given number of milliseconds
     * @param data mail session
     * @param path path
     * @return null or {@link SPAMScore#MAIL_RECOGNISED_AS_SPAM}
     */
    public String processDestinationMailbox(MailSessionData data, Path path) {
        if (!data.getDestinationMailboxes().contains(path)) {
            SPAMScore spamScore = (SPAMScore)data.getAttribute(SPAMScore.ATTRIBUTE);
            // Once it can be a mistake. Twice - it is SPAM
            if (spamScore.add(SPAMScore.MAX/2+1)) {
                try {
                    Thread.sleep(spamSlowDown);
                } catch (InterruptedException ignore) {
                }
                // Do not bother receiving rest of it...
                return SPAMScore.MAIL_RECOGNISED_AS_SPAM;
            } else {
                return "Requested action not taken: mailbox unavailable";
            }
        }
        return null;
    }

    /**
     * Returns null
     * @param data mail session
     * @return null
     */
    public String preLoadCheck(MailSessionData data) {
        return null;
    }

    /**
     * Returns null
     * @param data mail session
     * @return null
     */
    public String postLoadCheck(MailSessionData data) {
        return null;
    }

    /**
     * Does nothing
     * @param data mail session
     */
    public void finishSession(MailSessionData data) {
    }
}
