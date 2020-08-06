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
package org.abstracthorizon.mercury.smtp.filter.quiet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.abstracthorizon.mercury.common.command.CommandException;
import org.abstracthorizon.mercury.smtp.SMTPResponses;
import org.abstracthorizon.mercury.smtp.SMTPSession;
import org.abstracthorizon.mercury.smtp.command.DataCommand;
import org.abstracthorizon.mercury.smtp.filter.Filter;
import org.abstracthorizon.mercury.smtp.filter.MailSessionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SMTP DATA command.
 *
 * @author Daniel Sendula
 */
public class QuietFilterDataCommand extends DataCommand {

    /** Logger */
    protected static final Logger logger = LoggerFactory.getLogger(QuietFilterDataCommand.class);

    /**
     * Constructor
     */
    public QuietFilterDataCommand() {
        super();
    }


    /**
     * Returns <code>true</code> in case it i   s ok with proceeding with the
     * reading input stream. This method is responsible of sending response back
     * to the client
     *
     * <br>
     * Method to be overriden for filtering purposes.
     *
     * @param session SMTP session
     * @return <code>true</code> in case it is ok with proceeding with the
     *         reading input stream.
     * @throws IOException
     */
    protected boolean precheck(SMTPSession session) throws CommandException, IOException {
        boolean drop = "true".equals(session.getMailSessionData().getAttribute("drop"));

        SMTPQuietFilterCommandFactory factory = (SMTPQuietFilterCommandFactory)session.getCommandFactory();
        String response = factory.doPreLoadCheck(session.getMailSessionData());
        if (!drop && Filter.POSITIVE_RESPONSE.equals(response)) {
            session.sendResponse(SMTPResponses.START_DATA_RESPONSE);
            return true;
        } else {
            session.sendResponse(SMTPResponses.START_DATA_RESPONSE);
            try {
                session.setStreamDebug(false);
                flushMail(session.getInputStream(), factory.getMaxFlushSpeed());
            } catch (IOException e) {
                session.sendResponse(SMTPResponses.GENERIC_ERROR_RESPONSE);
            } catch (InterruptedException e) {
                session.sendResponse(SMTPResponses.GENERIC_ERROR_RESPONSE);
            } finally {
                session.setStreamDebug(true);
            }
            session.sendResponse(SMTPResponses.OK_RESPONSE);
            return false;
        }
    }

    protected boolean postcheck(SMTPSession connection) throws IOException {
        // This musn't be called in case of spam
        String response = ((SMTPQuietFilterCommandFactory)connection.getCommandFactory()).doPostLoadCheck(connection.getMailSessionData());
        if (Filter.POSITIVE_RESPONSE.equals(response)) {
            return super.postcheck(connection);
        } else {
            // just smile and do nothing
            connection.sendResponse(SMTPResponses.OK_RESPONSE);
            return false;
        }
    }

    protected void postProcessing(SMTPSession session, boolean hasSuccessfuls) throws IOException {
        session.sendResponse(SMTPResponses.OK_RESPONSE);

        MailSessionData mailSessionData = session.getMailSessionData();

        ((SMTPQuietFilterCommandFactory)session.getCommandFactory()).finish(mailSessionData);
    }

    /**
     * Reads raw mail from the input stream until &quot;.&quot;
     * It ensures speed does not exceeds given
     *
     * @param in input stream mail is read from. Usually input stream from the
     *            socket
     * @param max speed
     * @throws IOException in case of an exception while reading mail
     */
    protected void flushMail(InputStream in, int maxSpeed) throws IOException, InterruptedException {
        boolean first = true;
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "ISO-8859-1"));

        long started = System.currentTimeMillis();
        long read = 0;
        String line = reader.readLine();
        while (line != null) {
            if (first) {
                first = false;
            } else {
                if (line.equals(".")) { return; }
            }
            read = read + line.length();
            long now = System.currentTimeMillis();
            long secs = (now - started) / 1000;
            if (secs == 0) {
                secs = 1;
            }
            long max = secs * maxSpeed;
            if (read > max) {
                int millis = 0;
                if (max > 0) {
                    millis = (int)((read - max) / max) * 1000;
                } else {
                    millis = (int)((read - max) / 1) * 1000;
                }
                if (millis <= 0) {
                    millis = 500;
                } else if (millis > 1000) {
                    millis = 1000;
                }
                Thread.sleep(millis);
            }
            line = reader.readLine();
        }
    }
}
