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
package org.abstracthorizon.mercury.filter.spam;

import javax.mail.MessagingException;

import org.abstracthorizon.mercury.smtp.filter.Filter;
import org.abstracthorizon.mercury.smtp.filter.MailSessionData;
import org.abstracthorizon.mercury.smtp.util.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * SPAM filter that comes last in chain.
 *
 * @author Daniel Sendula
 */
public class FinalSPAMFilter implements Filter {

    /** Logger */
    protected static final Logger logger = LoggerFactory.getLogger(FinalSPAMFilter.class);

    /**
     * Constructor
     */
    public FinalSPAMFilter() {
    }

    /**
     * Returns {@link Filter#CAN_PROCESS_SOURCE_DOMAIN},
     *         {@link Filter#CAN_PROCESS_SOURCE_MAILBOX},
     *         {@link Filter#CAN_PROCESS_DESTINATION_MAILBOX},
     *         {@link Filter#CAN_DO_PRELOAD_CHECK} and
     *         {@link Filter#CAN_DO_POSTLOAD_CHECK}.
     *
     * @return features
     */
    public int features() {
        return Filter.CAN_PROCESS_SOURCE_DOMAIN
                | Filter.CAN_PROCESS_SOURCE_MAILBOX
                | Filter.CAN_PROCESS_DESTINATION_MAILBOX
                | Filter.CAN_DO_PRELOAD_CHECK
                | Filter.CAN_DO_POSTLOAD_CHECK;
    }

    /**
     * Sets {@link SPAMScore} to session.
     * @param data session
     */
    public void startSession(MailSessionData data) {
        SPAMScore score = new SPAMScore();
        data.setAttribute(SPAMScore.ATTRIBUTE, score);
        // TODO grab all other config data and set it up! ???
    }

    /**
     * Returns {@link SPAMScore#MAIL_RECOGNISED_AS_SPAM} if {@link SPAMScore#isSPAM()} returns <code>true</code>
     * @return {@link SPAMScore#MAIL_RECOGNISED_AS_SPAM} or <code>null</code>
     */
    public String processSourceDomain(MailSessionData data) {
        SPAMScore spamScore = (SPAMScore)data.getAttribute(SPAMScore.ATTRIBUTE);
        if (spamScore.isSPAM()) {
            return SPAMScore.MAIL_RECOGNISED_AS_SPAM;
        }
        return null;
    }

    /**
     * Returns {@link SPAMScore#MAIL_RECOGNISED_AS_SPAM} if {@link SPAMScore#isSPAM()} returns <code>true</code>
     * @return {@link SPAMScore#MAIL_RECOGNISED_AS_SPAM} or <code>null</code>
     */
    public String processSourceMailbox(MailSessionData data) {
        SPAMScore spamScore = (SPAMScore)data.getAttribute(SPAMScore.ATTRIBUTE);
        if (spamScore.isSPAM()) {
            return SPAMScore.MAIL_RECOGNISED_AS_SPAM;
        }
        return null;
    }

    /**
     * Returns {@link SPAMScore#MAIL_RECOGNISED_AS_SPAM} if {@link SPAMScore#isSPAM()} returns <code>true</code>
     * @return {@link SPAMScore#MAIL_RECOGNISED_AS_SPAM} or <code>null</code>
     */
    public String processDestinationMailbox(MailSessionData data, Path path) {
        SPAMScore spamScore = (SPAMScore)data.getAttribute(SPAMScore.ATTRIBUTE);
        if (spamScore.isSPAM()) {
            return SPAMScore.MAIL_RECOGNISED_AS_SPAM;
        }
        return null;
    }

    /**
     * Returns {@link SPAMScore#MAIL_RECOGNISED_AS_SPAM} if {@link SPAMScore#isSPAM()} returns <code>true</code>
     * @return {@link SPAMScore#MAIL_RECOGNISED_AS_SPAM} or <code>null</code>
     */
    public String preLoadCheck(MailSessionData data) {
        SPAMScore spamScore = (SPAMScore)data.getAttribute(SPAMScore.ATTRIBUTE);
        if (spamScore.isSPAM()) {
            return SPAMScore.MAIL_RECOGNISED_AS_SPAM;
        }
        return null;
    }

    /**
     * Returns {@link SPAMScore#MAIL_RECOGNISED_AS_SPAM} if {@link SPAMScore#isSPAM()} returns <code>true</code>.
     * It also adds &quot;X-Spam-Level&quot; header accordingly.
     *
     * @return {@link SPAMScore#MAIL_RECOGNISED_AS_SPAM} or <code>null</code>
     */
    public String postLoadCheck(MailSessionData data) {
        SPAMScore spamScore = (SPAMScore)data.getAttribute(SPAMScore.ATTRIBUTE);
        if (spamScore.isSPAM()) {
            return SPAMScore.MAIL_RECOGNISED_AS_SPAM;
        }
        int level = spamScore.getLevel();
        StringBuffer res = new StringBuffer(level);
        for (int i=0; i<level; i++) {
            res.append('*');
        }
        try {
            String v = res.toString();
            if (v.length() == 0) {
                v = "-";
            }
            data.getMessage().setHeader("X-Spam-Level", v);
        } catch (MessagingException e) {
            logger.error("Problem", e);
        }
        return null;
    }

    /**
     * Removes spam score attribute
     * @parma data session
     */
    public void finishSession(MailSessionData data) {
        data.removeAttribute(SPAMScore.ATTRIBUTE);
    }
}
