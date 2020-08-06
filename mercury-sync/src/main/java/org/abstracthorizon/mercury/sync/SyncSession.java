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
package org.abstracthorizon.mercury.sync;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import org.abstracthorizon.danube.connection.Connection;
import org.abstracthorizon.danube.connection.ConnectionException;
import org.abstracthorizon.danube.connection.ConnectionWrapper;
import org.abstracthorizon.danube.support.logging.LoggingConnection;
import org.abstracthorizon.mercury.sync.cachedir.CachedDirs;
import org.abstracthorizon.mercury.sync.commands.SyncCommandFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sync connection which represents Sync session.
 *
 * @author Daniel Sendula
 */
public class SyncSession extends ConnectionWrapper {

    /** Logger */
    public static Logger logger = LoggerFactory.getLogger(SyncConnectionHandler.class);

    /** Cached input stream */
    protected InputStream inputStream;

    protected OutputStream outputStream;

    /** Cached reference to command factory */
    protected SyncCommandFactory factory;

    /** When session is created */
    protected long created;

    /** When session is accessed */
    protected long lastAccessed;

    /** Connection handler that created this connection */
    protected SyncConnectionHandler parent;

    /** Command factory */
    private SyncCommandFactory commandFactory;

    /** Should connection be dropped */
    private boolean dropConnection;

    private String clientId;

    private CachedDirs cachedDirs;

    /**
     * Constructor
     * @param connection underlay connection
     * @param parent handler that created this session
     * @throws ConnectionException
     */
    public SyncSession(Connection connection, SyncConnectionHandler parent) throws ConnectionException {
        super(connection);

        this.parent = parent;
        this.cachedDirs = parent.getCachedDirs();

        inputStream = new BufferedInputStream(connection.adapt(InputStream.class));
        outputStream = connection.adapt(OutputStream.class);

        created = System.currentTimeMillis();
        lastAccessed = created;

        Certificate[] certs = connection.adapt(Certificate[].class);
        if (certs == null) {
            throw new ConnectionException("Connection must be SSL connection and client must have been authenticated.");
        }
        int i = 0;
        while (i < certs.length && clientId == null) {
            X509Certificate x509Cert = (X509Certificate)certs[i];
            Principal pricipal = x509Cert.getSubjectDN();

            clientId = extractCN(pricipal.getName());
        }
    }

    private String extractCN(String name) {
        String[] elements = name.split(", ");
        for (String element : elements) {
            String[] kp = element.split("=");
            if ("CN".equals(kp[0])) {
                return kp[1];
            }
        }

        return null;
    }

    /**
     * Should we keep log after the session is finished
     *
     * @param keepLog keep log
     */
    public void setKeepLog(boolean keepLog) {
        LoggingConnection loggingConnection = connection.adapt(LoggingConnection.class);
        if (loggingConnection != null) {
            loggingConnection.setTemporaryLog(!keepLog);
        }
    }

    /**
     * Returns shell logs be kept after session is finished
     * @return keep log
     */
    public boolean isKeepLog() {
        LoggingConnection loggingConnection = connection.adapt(LoggingConnection.class);
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
        LoggingConnection loggingConnection = connection.adapt(LoggingConnection.class);
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
        LoggingConnection loggingConnection = connection.adapt(LoggingConnection.class);
        if (loggingConnection != null) {
            loggingConnection.setLogging(debug);
        }
    }

    /**
     * Returns if logging is enabled
     * @return is logging enabled
     */
    public boolean isStreamDebug() {
        LoggingConnection loggingConnection = connection.adapt(LoggingConnection.class);
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
        LoggingConnection loggingConnection = connection.adapt(LoggingConnection.class);
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

    public String getClientId() {
        return clientId;
    }

    public CachedDirs getCachedDirs() {
        cachedDirs.refresh();
        return cachedDirs;
    }

    /**
     * Closes the session (connection)
     */
    public void close() {
        super.close();
    }

    /**
     * Resets last accessed time
     */
    public void resetLastAccessed() {
        lastAccessed = System.currentTimeMillis();
    }

    /**
     * Returns time when the session is last accessed
     * @return time when the session is last accessed
     */
    public long getSessionAccessed() {
        return lastAccessed;
    }

    /**
     * Utility method that reads a line from input stream
     * @return line string or <code>null</code> if <code>EOF</code> is reached
     * @throws IOException
     */
    public String readLine() throws IOException {
        // TODO - this blocks socket and thread. It needs to have a timeout!!!
        StringBuffer result = new StringBuffer();
        int r = inputStream.read();
        if (r < 0) {
            return null;
        }
        while ((r >= 0) && (r != '\n')) {
            if (r >= ' ') {
                result.append((char)r);
            }
            r = inputStream.read();
        }
        return result.toString();
    }

    public void setCommandFactory(SyncCommandFactory commandFactory) {
        this.commandFactory = commandFactory;
    }

    public SyncCommandFactory getCommandFactory() {
        return commandFactory;
    }

    /**
     * Sets SMTP response back
     * @param response response
     * @throws IOException
     */
    public void sendResponse(SyncResponse response) throws IOException {
        if (outputStream != null) {
            response.submit(outputStream);
        }
    }

    public boolean isDropConnection() {
        return dropConnection;
    }

    public void setDropConnection(boolean dropConnection) {
        this.dropConnection = dropConnection;
    }
}
