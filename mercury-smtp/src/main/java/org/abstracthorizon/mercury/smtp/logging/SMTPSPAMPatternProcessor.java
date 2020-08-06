/*
 * Copyright (c) 2006-2007 Creative Sphere Limited.
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
package org.abstracthorizon.mercury.smtp.logging;

import org.abstracthorizon.danube.connection.Connection;
import org.abstracthorizon.danube.support.logging.patternsupport.PatternProcessor;
import org.abstracthorizon.danube.support.logging.util.StringUtil;
import org.abstracthorizon.mercury.smtp.SMTPSession;
import org.abstracthorizon.mercury.smtp.filter.MailSessionData;
import org.abstracthorizon.mercury.smtp.util.Path;

/**
 * This processor adds following pattern codes:
 *
 * <ul>
 * <li><code>%b</code> - bytes sent excluding headers or &quot;-&quot; if nothing</li>
 * <li><code>%B</code> - bytes sent excluding headers or 0 if nothing</li>
 * <li><code>%E</code> - helo/ehlo string</li>
 * <li><code>%r</code> - return code
 * <li><code>%S</code> - source mailbox (MAIL FROM directive)
 * <li><code>%R</code> - recipients
 * </ul>
 *
 * @author Daniel Sendula
 */
public class SMTPSPAMPatternProcessor implements PatternProcessor {

    /** Cached index of bytes sent */
    protected int bytesSentIndex = -1;

    /** Cached index of bytes sent with zero */
    protected int bytesSent0Index = -1;

    /** Cached index of ehlo source domain string */
    protected int ehloIndex = -1;

    /** Cached index of return code */
    protected int returnCodeIndex = -1;

    /** Cached index of source */
    protected int sourceIndex = -1;

    /** Cached index of recipients */
    protected int recipientsIndex = -1;


    /**
     * Constructor
     */
    public SMTPSPAMPatternProcessor() {
    }

    /**
     * Checks if parameters are present and if so replaces it and caches their indexes
     * @param index next index to be used
     * @param message message to be altered
     */
    public int init(int index, StringBuffer message) {
        bytesSentIndex = -1;
        bytesSent0Index = -1;
        ehloIndex = -1;

        if (message.indexOf("%b") >= 0) {
            StringUtil.replaceAll(message, "%b", "{" + index + "}");
            bytesSentIndex = index;
            index = index + 1;
        }

        if (message.indexOf("%B") >= 0) {
            StringUtil.replaceAll(message, "%B", "{" + index + "}");
            bytesSent0Index = index;
            index = index + 1;
        }

        if (message.indexOf("%E") >= 0) {
            StringUtil.replaceAll(message, "%E", "{" + index + "}");
            ehloIndex = index;
            index = index + 1;
        }

        if (message.indexOf("%R") >= 0) {
            StringUtil.replaceAll(message, "%R", "{" + index + "}");
            recipientsIndex = index;
            index = index + 1;
        }

        if (message.indexOf("%r") >= 0) {
            StringUtil.replaceAll(message, "%r", "{" + index + "}");
            returnCodeIndex = index;
            index = index + 1;
        }

        if (message.indexOf("%S") >= 0) {
            StringUtil.replaceAll(message, "%S", "{" + index + "}");
            sourceIndex = index;
            index = index + 1;
        }

        return index;
    }

    /**
     * Adds parameter values to cached index positions
     * @param connection connection
     * @param array array
     */
    public void process(Connection connection, Object[] array) {
        SMTPSession smtpSession = (SMTPSession)connection.adapt(SMTPSession.class);
        MailSessionData data = smtpSession.getMailSessionData();

        if (bytesSentIndex >= 0) {
            long b = data.getTotalBytes();
            if (b <= 0) {
                array[bytesSentIndex] = "-";
            } else {
                array[bytesSentIndex] = Long.toString(b);
            }
        }

        if (bytesSent0Index >= 0) {
            array[bytesSent0Index] = Long.toString(data.getTotalBytes());
        }

        if (ehloIndex >= 0) {
            array[ehloIndex] = data.getSourceDomain();
        }

        if (returnCodeIndex >= 0) {
            int code = data.getReturnCode();
            if (code == 250) {
                code = 1;
            }
            array[returnCodeIndex] = Integer.toString(code);
        }

        if (sourceIndex >= 0) {
            Path source = data.getSourceMailbox();
            if (source == null) {
                array[sourceIndex] = "-";
            } else {
                array[sourceIndex] = source.toMailboxString();
            }
        }

        if (recipientsIndex >= 0) {
            StringBuffer recipients = new StringBuffer();
            boolean first = true;
            if (data.getDestinationMailboxes() != null) {
                for (Path path : data.getDestinationMailboxes()) {
                    if (first) {
                        first = false;
                    } else {
                        recipients.append(',');
                    }
                    recipients.append(path.toMailboxString());
                }
                if (recipients.length() == 0) {
                    array[recipientsIndex] = "-";
                } else {
                    array[recipientsIndex] = recipients;
                }
            } else {
                array[recipientsIndex] = "-";
            }
        }
    }
}
