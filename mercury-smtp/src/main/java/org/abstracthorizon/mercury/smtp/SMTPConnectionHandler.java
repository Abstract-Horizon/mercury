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
package org.abstracthorizon.mercury.smtp;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;

import org.abstracthorizon.danube.connection.Connection;
import org.abstracthorizon.danube.service.server.ServerConnectionHandler;
import org.abstracthorizon.danube.support.RuntimeIOException;
import org.abstracthorizon.mercury.common.StorageManager;
import org.abstracthorizon.mercury.smtp.filter.MailSessionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class that handles connection as SMTP connections. It creates {@link SMTPSession} wrapper
 * over session..
 *
 * @author Daniel Sendula
 */
public class SMTPConnectionHandler extends ServerConnectionHandler {

    /** Logger */
    protected static final Logger logger = LoggerFactory.getLogger(SMTPConnectionHandler.class);

    /** Starting response */
    public static final SMTPResponse READY_RESPONSE = new SMTPResponse(220, "Service ready");

    /** Cached value */
    protected StorageManager manager;

    /**
     * Constructor
     */
    public SMTPConnectionHandler() {
    }

    /**
     * Sets storage manager
     * @return storage manager
     */
    public StorageManager getStorageManager() {
        return manager;
    }

    /**
     * Sets storage manager
     * @param manager storage manager
     */
    public void setStorageManager(StorageManager manager) {
        this.manager = manager;
    }

    /**
     * This method creates {@link SMTPSession}, sends initial response and sets
     * state of session to {@link SMTPSession#STATE_CONNECTED}
     *
     * @param connection connection
     */
    protected Connection decorateConnection(Connection connection) {
        SMTPSession smtpConnection = new SMTPSession(connection, this);
        SMTPResponse ready = new SMTPResponse(220, getStorageManager().getMainDomain() + " Ready");
        try {
            smtpConnection.sendResponse(ready);
        } catch (IOException e) {
            smtpConnection.setKeepLog(true);
            OutputStream debugStream = smtpConnection.getDebugStream();
            if (debugStream != null) {
                PrintStream ps = new PrintStream(debugStream);
                ps.println("Unexpected IO problem");
                e.printStackTrace(ps);
            }
            try {
                smtpConnection.sendResponse(SMTPResponses.SHUTTING_DOWN_RESPONSE);
            } catch (IOException ignore) {
            }
            smtpConnection.close();
            throw new RuntimeIOException(e);
        }
        smtpConnection.setState(SMTPSession.STATE_CONNECTED);
        return smtpConnection;
    }

    /**
     * Resets smtp session
     * @param connection connection
     * @return persistConnection unchanged
     */
    protected boolean postProcessing(Connection connection) {
        boolean persistConnection = super.postProcessing(connection);
        SMTPSession smtpConnection = connection.adapt(SMTPSession.class);
        MailSessionData data = smtpConnection.getMailSessionData();
        boolean dropConnection = "true".equals(data.getAttribute("dropconnection"));
        persistConnection = !dropConnection & persistConnection;
        data.setTotalBytes(0);
        return persistConnection;
    }

    /**
     * Sends {@link SMTPResponses#SHUTTING_DOWN_RESPONSE} if possible
     * and closes the session
     * @param connection connection
     */
    protected void finishConnection(Connection connection) {
        SMTPSession smtpConnection = connection.adapt(SMTPSession.class);
        try {
            Socket socket = smtpConnection.adapt(Socket.class);
            if ((socket == null) || (!socket.isClosed() && !socket.isOutputShutdown())) {
                smtpConnection.sendResponse(SMTPResponses.SHUTTING_DOWN_RESPONSE);
            }
        } catch (IOException ignore) {
        }
        smtpConnection.close();
    }

}
