/*
 * Copyright (c) 2006-2020 Creative Sphere Limited.
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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;

import org.abstracthorizon.danube.connection.Connection;
import org.abstracthorizon.danube.connection.ConnectionException;
import org.abstracthorizon.danube.connection.ConnectionWrapper;
import org.abstracthorizon.danube.support.logging.LoggingConnection;
import org.abstracthorizon.mercury.common.io.TempStorage;
import org.abstracthorizon.mercury.smtp.command.SMTPCommandFactory;
import org.abstracthorizon.mercury.smtp.filter.MailSessionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SMTP connection which represents SMTP session.
 *
 * @author Daniel Sendula
 */
public class SMTPSession extends ConnectionWrapper {

    /** Human readable format */
    public static final SimpleDateFormat USER_READABLE_FORMAT = new SimpleDateFormat("HH:mm:ss dd:MM:yyyy");

    /** Logger */
    public static Logger logger = LoggerFactory.getLogger(SMTPConnectionHandler.class);

    /** State is not defined - UNKNOWN state */
    public static final int STATE_UNKNOWN = 0;

    /** Session is connected - no command is received yet */
    public static final int STATE_CONNECTED = 1;

    /** EHLO is received - waiting for MAIL FROM */
    public static final int STATE_READY = 2;

    /** Receiving RCPT TO and DATA commands */
    public static final int STATE_MAIL = 3;

    /** EHLO is received - waiting for AUTH */
    public static final int STATE_AUTH_NEEDED = 4;

    /** Temporary storage for receiving mail */
    protected TempStorage mail = new TempStorage();

    /** Scanner object to be used */
    protected SMTPScanner scanner;

    /** State of the session. Defaults to {@link SMTPSession#STATE_UNKNOWN} */
    protected int state = STATE_UNKNOWN;

    /** Session data */
    protected MailSessionData data = new MailSessionData();

    /** Connection handler that created this connection */
    protected SMTPConnectionHandler parent;

    /** Cached input stream */
    protected InputStream inputStream;

    /** Cached reference to command factory */
    protected SMTPCommandFactory factory;

    /** When session is created */
    protected long created;

    /** When session is accessed */
    protected long lastAccessed;
    
    protected OutputStream outputStream;
    
    /**
     * Constructor
     * @param connection underlay connection
     * @param parent handler that created this session
     * @throws ConnectionException
     */
    public SMTPSession(Connection connection, SMTPConnectionHandler parent) throws ConnectionException {
        super(connection);
        this.parent = parent;

        inputStream = new BufferedInputStream((InputStream)connection.adapt(InputStream.class));
        scanner = new SMTPScanner(inputStream);

        data.setAttribute("session", this);
        data.setAttribute("manager", parent.getStorageManager());

        created = System.currentTimeMillis();
        lastAccessed = created;
        
        outputStream = (OutputStream)connection.adapt(OutputStream.class);
    }

    /**
     * Should we keep log after the session is finished
     *
     * @param keepLog keep log
     */
    public void setKeepLog(boolean keepLog) {
        LoggingConnection loggingConnection = (LoggingConnection)connection.adapt(LoggingConnection.class);
        if (loggingConnection != null) {
            loggingConnection.setTemporaryLog(!keepLog);
        }
    }

    /**
     * Returns shell logs be kept after session is finished
     * @return keep log
     */
    public boolean isKeepLog() {
        LoggingConnection loggingConnection = (LoggingConnection)connection.adapt(LoggingConnection.class);
        if (loggingConnection != null) {
            return !loggingConnection.isTermporaryLog();
        } else {
            return false;
        }
    }

    /**
     * Helper function to write messaage to the log stream
     *
     * @param msg message to be writen to log stream
     */
    public void writeLogMessage(String msg) {
        LoggingConnection loggingConnection = (LoggingConnection)connection.adapt(LoggingConnection.class);
        if (loggingConnection != null) {
            PrintStream out = new PrintStream(loggingConnection.getDebugOutputStream());
            out.println(msg);
            out.flush();
        }
    }

    /**
     * Sets debug stream's logging attribute ({@link LoggingConnection#setLogging(boolean)} if any.
     *
     * @param debug logging should start (<code>true</code>) or stop (<code>false</code>)
     */
    public void setStreamDebug(boolean debug) {
        LoggingConnection loggingConnection = (LoggingConnection)connection.adapt(LoggingConnection.class);
        if (loggingConnection != null) {
            loggingConnection.setLogging(debug);
        }
    }

    /**
     * Returns if logging is enabled
     * @return is logging enabled
     */
    public boolean isStreamDebug() {
        LoggingConnection loggingConnection = (LoggingConnection)connection.adapt(LoggingConnection.class);
        if (loggingConnection != null) {
            return loggingConnection.isLogging();
        } else {
            return false;
        }
    }

    /**
     * Returns debug output stream or <code>null</code>
     * @return debug output stream or <code>null</code>
     */
    public OutputStream getDebugStream() {
        LoggingConnection loggingConnection = (LoggingConnection)connection.adapt(LoggingConnection.class);
        if (loggingConnection != null) {
            return loggingConnection.getDebugOutputStream();
        } else {
            return null;
        }
    }

    /**
     * Returns cached input stream
     * @return cached input stream
     */
    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     * Sets session's state
     * @param state new session's state
     */
    public void setState(int state) {
        this.state = state;
    }

    /**
     * Returns session's state
     * @return session's state
     */
    public int getState() {
        return state;
    }

    /**
     * Returns scanner object
     * @return scanner object
     */
    public SMTPScanner getScanner() {
        return scanner;
    }

    /**
     * Returns mail session data object
     * @return mail session data object
     */
    public MailSessionData getMailSessionData() {
        return data;
    }

    /**
     * Returns refernece to handler that created this session
     * @return refernece to handler that created this session
     */
    public SMTPConnectionHandler getConnectionHandler() {
        return parent;
    }

    /**
     * Closes the session (connection)
     */
    public void close() {
        super.close();
    }

    /**
     * Sets SMTP response back
     * @param response response
     * @throws IOException
     */
    public void sendResponse(SMTPResponse response) throws IOException {
        data.setReturnCode(response.getCode());
        if (outputStream != null) {
            response.submit(outputStream);
        }
    }

    /**
     * Sets command factory to be used
     * @param factory command factory to be used
     */
    public void setCommandFactory(SMTPCommandFactory factory) {
        this.factory = factory;
    }

    /**
     * Returns command factory that is used
     * @return command factory that is used
     */
    public SMTPCommandFactory getCommandFactory() {
        return factory;
    }

    /**
     * Returns time when the session is created
     * @return time when the session is created
     */
    public long getSessionCreated() {
        return created;
    }

    /**
     * Returns time when the session is created in human readable format
     * @return time when the session is created in human readable format
     */
    public String getSessionCreatedString() {
        return USER_READABLE_FORMAT.format(created);
    }

    /**
     * Returns time when the session is last accessed
     * @return time when the session is last accessed
     */
    public long getSessionAccessed() {
        return lastAccessed;
    }

    /**
     * Returns time when the session is last accessed in the human readable format
     * @return time when the session is last accessed
     */
    public String getSessionAccessedString() {
        return USER_READABLE_FORMAT.format(lastAccessed);
    }

    /**
     * Resets last accessed time
     */
    public void resetLastAccessed() {
        lastAccessed = System.currentTimeMillis();
    }
}
