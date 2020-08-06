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
package org.abstracthorizon.mercury.smtp.command;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.abstracthorizon.danube.connection.Connection;
import org.abstracthorizon.danube.service.server.ServerConnectionHandler;
import org.abstracthorizon.mercury.common.command.CommandException;
import org.abstracthorizon.mercury.smtp.SMTPResponses;
import org.abstracthorizon.mercury.smtp.SMTPScanner;
import org.abstracthorizon.mercury.smtp.SMTPSession;
import org.abstracthorizon.mercury.smtp.exception.ParserException;
import org.abstracthorizon.mercury.smtp.filter.MailSessionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SMTP command factory. This class is implementing parsing commands and invoking them.
 *
 *
 * @author Daniel Sendula
 */
public class SMTPCommandFactory extends ServerConnectionHandler {

    /** HELO commmand */
    public static String HELO = "HELO";

    /** EHLO command */
    public static String EHLO = "EHLO";

    /** MAIL command */
    public static String MAIL = "MAIL";

    /** RCPT command */
    public static String RCPT = "RCPT";

    /** DATA command */
    public static String DATA = "DATA";

    /** NOOP commmand */
    public static String NOOP = "NOOP";

    /** RSET (reset) command */
    public static String RSET = "RSET";

    /** QUIT command */
    public static String QUIT = "QUIT";

    /** VRFY command */
    public static String VRFY = "VRFY";

    /** EXPN command */
    public static String EXPN = "EXPN";

    /** AUTH command */
    public static String AUTH = "AUTH";

    /** Logger */
    protected static final Logger logger = LoggerFactory.getLogger(SMTPCommandFactory.class);

    /** Map of commands */
    protected Map<String, SMTPCommand> commands = new HashMap<String, SMTPCommand>();

    /** Inactivity timeout */
    protected int inactivityTimeout = 120000;

    /**
     * Constructor. It updates the map.
     */
    public SMTPCommandFactory() {

        commands.put(EHLO, new EhloCommand());
        commands.put(HELO, new EhloCommand());
        commands.put(MAIL, new MailCommand());
        commands.put(RCPT, new RcptCommand());
        commands.put(DATA, new DataCommand());

        commands.put(NOOP, new NoopCommand());
        commands.put(RSET, new ResetCommand());
        commands.put(QUIT, new QuitCommand());
        commands.put(VRFY, new NotImplementedCommand());
        commands.put(EXPN, new NotImplementedCommand());
        commands.put(AUTH, new NotImplementedCommand());
    }

    /**
     * Returns commands
     * @return commands
     */
    public Map<String, SMTPCommand> getCommands() {
        return commands;
    }

    /**
     * Sets map of commands
     * @param commands commands
     */
    public void setCommands(Map<String, SMTPCommand> commands) {
        this.commands = commands;
    }

    /**
     * Sets inactivity timeout
     * @param timeout timeout
     */
    public void setInactivityTimeout(int timeout) {
        this.inactivityTimeout = timeout;
    }

    /**
     * Returns inactivity timeout
     * @return inactivity timeout
     */
    public int getInactivityTimeout() {
        return inactivityTimeout;
    }

    /**
     * Processes keywords
     * @param connection connection
     */
    protected void processConnection(Connection connection) {
        SMTPSession smtpConnection = (SMTPSession)connection.adapt(SMTPSession.class);
        try {
            processKeywords(smtpConnection);
        } catch (InterruptedIOException e) {
            // Got interrupted exception - so we cannot continue - that means we need to stop!!!
            MailSessionData data = smtpConnection.getMailSessionData();
            data.setAttribute("dropconnection", "true");
            throw new RuntimeException(e);
        } catch (IOException e) {
            // Got interrupted exception - so we cannot continue - that means we need to stop!!!
            MailSessionData data = smtpConnection.getMailSessionData();
            data.setAttribute("dropconnection", "true");
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets state to {@link SMTPSession#STATE_READY}
     *
     * @param connection connection
     */
    protected Connection decorateConnection(Connection connection) {
        SMTPSession smtpConnection = (SMTPSession)connection.adapt(SMTPSession.class);
        smtpConnection.setCommandFactory(this);
        smtpConnection.setState(SMTPSession.STATE_READY);
        return smtpConnection;
    }

    /**
     * Returns <code>false</code> only if state of connection is {@link SMTPSession#STATE_CONNECTED}
     * @param connection connection
     */
    protected boolean postProcessing(Connection connection) {
        boolean persistConnection = super.postProcessing(connection);
        SMTPSession session = (SMTPSession)connection.adapt(SMTPSession.class);
        if ((System.currentTimeMillis() - session.getSessionAccessed()) > getInactivityTimeout()) {
            return false;
        }

//        SMTPSession smtpConnection = (SMTPSession)connection.adapt(SMTPSession.class);
//        if (smtpConnection.getState() == SMTPSession.STATE_CONNECTED) {
//            persistConnection = false;
//        }
        MailSessionData data = session.getMailSessionData();
        boolean dropConnection = "true".equals(data.getAttribute("dropconnection"));
        persistConnection = !dropConnection && persistConnection && (session.getState() != SMTPSession.STATE_READY);
        
        return persistConnection;
    }

    /**
     * Does nothing
     * @param connection connection
     * @param closedConnection is connection already closed.
     */
    protected void finishProcessingConnection(Connection connection, boolean closedConnection) {
        // DO nothing!
    }

    /**
     * Returns a command from the map of commands
     * @param mnemonic command name
     * @return command or <code>null</code>
     * @throws CommandException
     */
    public SMTPCommand getCommand(String mnemonic) throws CommandException {
        SMTPCommand command = commands.get(mnemonic);
        return command;
    }

    /**
     * Checks all defined commands in map of commands. As soon as command is
     * found it is executed.
     *
     * @param smtpConnection smtp connection
     * @throws IOException
     */
    public void processKeywords(SMTPSession smtpConnection) throws IOException {
        SMTPScanner scanner = smtpConnection.getScanner();
        for (Entry<String, SMTPCommand> entry : commands.entrySet()) {
            if (scanner.keyword(entry.getKey())) {
                invokeCommand(smtpConnection, entry.getKey(), entry.getValue());
                return;
            }
        }

        scanner.skip_line();
        scanner.resetEOL();
        smtpConnection.sendResponse(SMTPResponses.COMMAND_NOT_RECOGNISED_RESPONSE);
    }

    /**
     * Invokes command
     *
     * @param smtpConnection smtp connection
     * @param commandName command name
     * @param command command itself
     * @throws IOException
     */
    public void invokeCommand(SMTPSession smtpConnection, String commandName, SMTPCommand command) throws IOException {
        SMTPScanner scanner = smtpConnection.getScanner();
        if (scanner.peek_char(' ') || scanner.peek_char('\r')) {
            try {
                long started = System.currentTimeMillis();
                try {
                    if (command != null) {
                        command.handleConnection(smtpConnection);
                    } else {
                        smtpConnection.sendResponse(SMTPResponses.SYNTAX_ERROR_RESPONSE);
                    }
                } finally {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Command " + commandName + " completed in " + (System.currentTimeMillis() - started) + "ms");
                    }
                }
            } catch (InterruptedIOException e) {
                throw e;
            } catch (ParserException e) {
                smtpConnection.setKeepLog(true);
                if (logger.isDebugEnabled()) {
                    logger.debug("Problem", e);
                }
                OutputStream debugStream = smtpConnection.getDebugStream();
                if (debugStream != null) {
                    e.printStackTrace(new PrintStream(debugStream));
                }
                smtpConnection.sendResponse(SMTPResponses.SYNTAX_ERROR_RESPONSE);
            } catch (CommandException e) {
                smtpConnection.setKeepLog(true);
                logger.error("Problem", e);
                OutputStream debugStream = smtpConnection.getDebugStream();
                if (debugStream != null) {
                    e.printStackTrace(new PrintStream(debugStream));
                }
                smtpConnection.sendResponse(SMTPResponses.GENERIC_ERROR_RESPONSE);
            } catch (Throwable e) {
                smtpConnection.setKeepLog(true);
                logger.error("Problem", e);
                OutputStream debugStream = smtpConnection.getDebugStream();
                if (debugStream != null) {
                    e.printStackTrace(new PrintStream(debugStream));
                }
                smtpConnection.sendResponse(SMTPResponses.GENERIC_ERROR_RESPONSE);
            }
        } else {
            smtpConnection.sendResponse(SMTPResponses.SYNTAX_ERROR_RESPONSE);
        }
        scanner.skip_line();
        scanner.resetEOL();
    }
}
