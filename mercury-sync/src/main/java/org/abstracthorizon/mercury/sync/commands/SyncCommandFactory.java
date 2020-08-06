/*
 * Copyright (c) 2004-2019 Creative Sphere Limited.
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
package org.abstracthorizon.mercury.sync.commands;

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
import org.abstracthorizon.mercury.sync.SyncResponses;
import org.abstracthorizon.mercury.sync.SyncSession;
import org.abstracthorizon.mercury.sync.exception.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sync command factory. This class is implementing parsing commands and invoking them.
 *
 *
 * @author Daniel Sendula
 */
public class SyncCommandFactory extends ServerConnectionHandler {

    /** Logger */
    protected static final Logger logger = LoggerFactory.getLogger(SyncCommandFactory.class);

    /** Map of commands */
    protected Map<String, SyncCommand> commands = new HashMap<String, SyncCommand>();

    /** Inactivity timeout */
    protected int inactivityTimeout = 120000;

    /**
     * Constructor. It updates the map.
     */
    public SyncCommandFactory() {
        commands.put("GET", new GetCommand());
        commands.put("PUT", new PutCommand());
        commands.put("LIST", new ListCommand());
        commands.put("DELETE", new DeleteCommand());
        commands.put("DIR", new DirCommand());
        commands.put("MKDIR", new MKDirCommand());
        commands.put("RMDIR", new RMDirCommand());
        commands.put("TOUCH", new TouchCommand());
        commands.put("QUIT", new QuitCommand());
        commands.put("BYE", new QuitCommand());
        commands.put("EXIT", new QuitCommand());
    }

    /**
     * Returns commands
     * @return commands
     */
    public Map<String, SyncCommand> getCommands() {
        return commands;
    }

    /**
     * Sets map of commands
     * @param commands commands
     */
    public void setCommands(Map<String, SyncCommand> commands) {
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
        SyncSession syncConnection = connection.adapt(SyncSession.class);
        try {
            processKeywords(syncConnection);
        } catch (InterruptedIOException e) {
            // Got interrupted exception - so we cannot continue - that means we need to stop!!!
            syncConnection.setDropConnection(true);
            throw new RuntimeException(e);
        } catch (IOException e) {
            // Got interrupted exception - so we cannot continue - that means we need to stop!!!
            syncConnection.setDropConnection(true);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets state to {@link SMTPSession#STATE_READY}
     *
     * @param connection connection
     */
    protected Connection decorateConnection(Connection connection) {
        SyncSession syncConnection = connection.adapt(SyncSession.class);
        syncConnection.setCommandFactory(this);
        return syncConnection;
    }

    /**
     * Returns <code>false</code> only if state of connection is {@link SMTPSession#STATE_CONNECTED}
     * @param connection connection
     */
    protected boolean postProcessing(Connection connection) {
        boolean persistConnection = super.postProcessing(connection);
        SyncSession session = connection.adapt(SyncSession.class);
        if ((System.currentTimeMillis() - session.getSessionAccessed()) > getInactivityTimeout()) {
            return false;
        }

//        SMTPSession syncConnection = (SMTPSession)connection.adapt(SMTPSession.class);
//        if (syncConnection.getState() == SMTPSession.STATE_CONNECTED) {
//            persistConnection = false;
//        }
        persistConnection = !session.isDropConnection() && persistConnection;

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
    public SyncCommand getCommand(String mnemonic) throws CommandException {
        SyncCommand command = commands.get(mnemonic);
        return command;
    }

    /**
     * Checks all defined commands in map of commands. As soon as command is
     * found it is executed.
     *
     * @param syncConnection smtp connection
     * @throws IOException
     */
    public void processKeywords(SyncSession syncConnection) throws IOException {
        String cmdLine = syncConnection.readLine();

        String command = parseCommand(cmdLine);

        if (cmdLine.length() > command.length()) {
            cmdLine = cmdLine.substring(command.length());
            if (cmdLine.startsWith(" ")) {
                cmdLine = cmdLine.substring(1);
            }
        }

        for (Entry<String, SyncCommand> entry : commands.entrySet()) {
            if (command.equals(entry.getKey())) {
                invokeCommand(syncConnection, entry.getKey(), cmdLine, entry.getValue());
                return;
            }
        }
        syncConnection.sendResponse(SyncResponses.COMMAND_NOT_RECOGNISED_RESPONSE);
    }

    private String parseCommand(String cmdLine) {
        StringBuilder command = new StringBuilder();
        int i = 0;
        while (i < cmdLine.length() && cmdLine.charAt(i) != ' ') {
            command.append(cmdLine.charAt(i));
            i++;
        }
        return command.toString();
    }

    /**
     * Invokes command
     *
     * @param syncConnection smtp connection
     * @param commandName command name
     * @param command command itself
     * @throws IOException
     */
    public void invokeCommand(SyncSession syncConnection, String commandName, String cmdLine, SyncCommand command) throws IOException {
        try {
            long started = System.currentTimeMillis();
            try {
                command.execute(syncConnection, commandName, cmdLine);
            } finally {
                if (logger.isDebugEnabled()) {
                    logger.debug("Command " + commandName + " completed in " + (System.currentTimeMillis() - started) + "ms");
                }
            }
        // } catch (InterruptedIOException e) {
        //     throw e;
        } catch (ParserException e) {
            syncConnection.setKeepLog(true);
            if (logger.isDebugEnabled()) {
                logger.debug("Problem", e);
            }
            OutputStream debugStream = syncConnection.getDebugStream();
            if (debugStream != null) {
                e.printStackTrace(new PrintStream(debugStream));
            }
            syncConnection.sendResponse(SyncResponses.SYNTAX_ERROR_RESPONSE);
        } catch (CommandException e) {
            syncConnection.setKeepLog(true);
            logger.error("Problem", e);
            OutputStream debugStream = syncConnection.getDebugStream();
            if (debugStream != null) {
                e.printStackTrace(new PrintStream(debugStream));
            }
            syncConnection.sendResponse(SyncResponses.GENERIC_ERROR_RESPONSE);
        } catch (Throwable e) {
            syncConnection.setKeepLog(true);
            logger.error("Problem", e);
            OutputStream debugStream = syncConnection.getDebugStream();
            if (debugStream != null) {
                e.printStackTrace(new PrintStream(debugStream));
            }
            syncConnection.sendResponse(SyncResponses.GENERIC_ERROR_RESPONSE);
        }
    }
}
