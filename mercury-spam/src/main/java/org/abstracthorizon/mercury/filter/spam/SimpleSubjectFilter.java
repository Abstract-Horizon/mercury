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
import javax.mail.internet.MimeMessage;

import org.abstracthorizon.mercury.smtp.filter.Filter;
import org.abstracthorizon.mercury.smtp.filter.MailSessionData;
import org.abstracthorizon.mercury.smtp.util.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author DSendula
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class SimpleSubjectFilter implements Filter {

    protected static final Logger logger = LoggerFactory.getLogger(SimpleSubjectFilter.class);

    public int features() {
        return Filter.CAN_DO_POSTLOAD_CHECK;
    }

    public void startSession(MailSessionData data) {
    }

    public String processSourceDomain(MailSessionData data) {
        return null;
    }

    public String processSourceMailbox(MailSessionData data) {
        return null;
    }

    public String processDestinationMailbox(MailSessionData data, Path path) {
        return null;
    }

    public String preLoadCheck(MailSessionData data) {
        return null;
    }

    public String postLoadCheck(MailSessionData data) {
        SPAMScore spamScore = (SPAMScore)data.getAttribute(SPAMScore.ATTRIBUTE);
        try {
            MimeMessage msg = data.getMessage();
            if ((msg.getSubject() == null) || (msg.getSubject().length() == 0)) {
                if (spamScore.add(20000)) {
                    return SPAMScore.MAIL_RECOGNISED_AS_SPAM;
                }
            } else {
                // TODO - needs proper scanner
                if (msg.getSubject().indexOf("spam") >= 0) {
                    spamScore.score = spamScore.score + 2000;
                }
            }
        } catch (MessagingException e) {
            logger.error("Problem", e);
        }
        return null;
    }

    public void finishSession(MailSessionData data) {
    }
}
