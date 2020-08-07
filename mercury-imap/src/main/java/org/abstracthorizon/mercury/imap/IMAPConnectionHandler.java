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
package org.abstracthorizon.mercury.imap;


import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.util.concurrent.Executor;

import javax.mail.Session;
import javax.mail.Store;

import org.abstracthorizon.danube.connection.Connection;
import org.abstracthorizon.danube.connection.ConnectionException;
import org.abstracthorizon.danube.connection.ConnectionHandler;
import org.abstracthorizon.mercury.common.StorageManager;
import org.abstracthorizon.mercury.common.command.CommandException;
import org.abstracthorizon.mercury.imap.cmd.IMAPCommandFactory;
import org.abstracthorizon.mercury.imap.cmd.UIDCommand;
import org.abstracthorizon.mercury.imap.response.BADResponse;
import org.abstracthorizon.mercury.imap.response.ByeResponse;
import org.abstracthorizon.mercury.imap.response.NOResponse;
import org.abstracthorizon.mercury.imap.response.OKResponse;
import org.abstracthorizon.mercury.imap.response.Response;
import org.abstracthorizon.mercury.imap.util.IMAPScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class for IMAP service
 *
 * @author Daniel Sendula
 */
public class IMAPConnectionHandler implements ConnectionHandler {

    /** Logger */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /** Storage manager */
    protected StorageManager storageManager;

    /** Selected store */
    protected Store store;

    /** Command factory */
    protected IMAPCommandFactory factory = new IMAPCommandFactory();

    /** Session */
    protected Session session;

    /** Thread pool */
    protected Executor threadPool;

    /** Keystore pass phrase */
    protected char[] passPhrase;

    /** Allow insecure connections */
    protected boolean allowInsecure = true;

    /**
     * Constructor
     */
    public IMAPConnectionHandler() {
    }

    /**
     * Sets storage manager
     * @param storageManager storage manager
     */
    public void setStorageManager(StorageManager storageManager) {
        this.storageManager = storageManager;
    }

    /**
     * Returns storage manager
     * @return storage manager
     */
    public StorageManager getStorageManager() {
        return storageManager;
    }

    /**
     * Returns command factory
     * @return command factory
     */
    public IMAPCommandFactory getFactory() {
        return factory;
    }

    /**
     * Returns javamail session that is used
     * @return javamail session that is used
     */
    public Session getJavaMailSession() {
        return session;
    }

    /**
     * Sets javamail session that to be used
     * @param session javamail session that to be used
     */
    public void setJavaMailSession(Session session) {
        this.session = session;
    }

    /**
     * Sets thread pool to be used for parallel tasks
     * @param executor thread pool
     */
    public void setThreadPool(Executor executor) {
        this.threadPool = executor;
    }

    /**
     * Returns thread pool to be used for parallel tasks
     * @return thread pool to be used for parallel tasks
     */
    public Executor getThreadPool() {
        return threadPool;
    }

    /**
     * Returns keystore as an input stream
     * @return keystore as an input stream
     */
    public InputStream getKeyStoreInputStream() {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream("META-INF/keystore");
    }

    /**
     * Sets pass phrase of a keystore to be used for switching to TLS
     * @param passPhrase password of a keystore
     */
    public void setPassPhrase(char[] passPhrase) {
        this.passPhrase = passPhrase;
    }

    /**
     * Returns pass phrase of a keystore to be used for switching to TLS
     * @return pass phrase of a keystore to be used for switching to TLS
     */
    public char[] getPassPhrase() {
        return passPhrase;
    }

    /**
     * Returns if insecure connections are allowed
     * @return if insecure connections are allowed
     */
    public boolean isInsecureAllowed() {
        return allowInsecure;
    }

    /**
     * Sets if insecure connections are allowed
     * @param allowInsecure is insecure connection allowed
     */
    public void setInsecureAllowed(boolean allowInsecure) {
        this.allowInsecure = allowInsecure;
    }

    /**
     * Handles IMAP connection
     * @param connection connection
     */
    public void handleConnection(Connection connection) {
        IMAPSession imapConnection = new IMAPSession(connection, this);

        try {
            try {
                try {
                    new OKResponse(imapConnection, Response.UNTAGGED_RESPONSE, "Service Ready").submit();
                    boolean persistConnection = true;
                    while (persistConnection) {
                        IMAPScanner scanner = imapConnection.getScanner();

                        processInput(imapConnection);

                        Socket socket = (Socket)connection.adapt(Socket.class);
                        persistConnection = (socket != null) && !socket.isInputShutdown() && !socket.isOutputShutdown() && !socket.isClosed();

                        if (((socket == null) || socket.isConnected()) && !imapConnection.isCleanInput()) {
                            scanner.skip_line();
                            imapConnection.setCleanInput(true);
                        }
                    }
                } catch (ConnectionException e) {
                    Throwable w = e.getCause();
                    if (w != null) {
                        throw w;
                    }
                }
            } catch (InterruptedIOException e) {
                imapConnection.setKeepLog(true);
                imapConnection.writeLogMessage("Closing because of inactivity");
                new ByeResponse(imapConnection); // ???
            } catch (EOFException e) {
                // Don't long sudden and proper stream closes.
            } catch (IOException e) {
                Socket socket = (Socket)connection.adapt(Socket.class);
                if ((socket != null) && socket.isConnected()) {
                    logger.error("End of session exception: ", e);
                }
            } catch (Throwable t) {
                logger.error("Got problem: ", t);
            }
        } finally {
            imapConnection.close();
        }
    }

    /**
     * Processes input
     * @param imapConnection imap connection
     * @throws IOException
     */
    public void processInput(IMAPSession imapConnection) throws IOException {
        imapConnection.setCleanInput(false);

        IMAPScanner scanner = imapConnection.getScanner();

        StringBuffer tagBuffer = new StringBuffer();
        try {
            if (!scanner.tag(tagBuffer)) {
                new BADResponse(imapConnection, Response.UNTAGGED_RESPONSE, "tag is missing").submit();
            }
        } catch (IOException e) {
            e.printStackTrace();
            // we don't care if someone just closes connection before tag read read.
            imapConnection.close();
            return;
        }
        String tag = tagBuffer.toString();
        imapConnection.setTag(tag);
        if (!scanner.is_char(' ')) {
            new BADResponse(imapConnection, "space after tag is missing").submit();
        }
        if (!imapConnection.isAuthorised()) {
            // Not Authenticated state
            if (commandNonAuth(imapConnection)) {
            } else if (commandAny(imapConnection)) {
            } else {
                new BADResponse(imapConnection, "Command not recognised or not allowed in non-authorised mode.").submit();
            }
        } else if (imapConnection.getSelectedFolder() == null) {
            // Authenticated state
            if (commandAuth(imapConnection)){
            } else if (commandAny(imapConnection)) {
            } else {
                new BADResponse(imapConnection, "Command not recognised or not allowed in not selected mode.").submit();
            }
        } else {
            // Selected state
            if (commandSelected(imapConnection)) {
            } else if (commandAuth(imapConnection)) {
            } else if (commandAny(imapConnection)) {
            } else {
                new BADResponse(imapConnection, "Command not recognised or not allowed in non-authorised mode.").submit();
            }
        }
    }

    /**
     * Invokes command
     * @param imapConnection imap connection
     * @param name comamnd name
     * @throws IOException
     */
    public void invokeCommand(IMAPSession imapConnection, String name) throws IOException {
        invokeCommand(imapConnection, name, false);
    }

    /**
     * Invokes command
     * @param imapConnection imap connection
     * @param name command name
     * @param uid is UID function
     * @throws IOException
     */
    public void invokeCommand(IMAPSession imapConnection, String name, boolean uid) throws IOException {
        IMAPScanner scanner = imapConnection.getScanner();
        if (scanner.is_char(' ') || scanner.peek_char('\r')) {
            try {
                ConnectionHandler command = factory.getCommand(name);
                // command.init(imapConnection);
                if (uid && (command instanceof UIDCommand)) {
                    ((UIDCommand)command).setAsUID();
                }
                command.handleConnection(imapConnection);
            } catch (NOCommandException e) {
                logger.debug("NO: ", e);
                new NOResponse(imapConnection, name+" "+e.getMessage()).submit();
                imapConnection.setKeepLog(true);
            } catch (BADCommandException e) {
                logger.error("BAD: ", e);
                new BADResponse(imapConnection, name+" "+e.getMessage()).submit();
                imapConnection.setKeepLog(true);
            } catch (CommandException e) {
                logger.error("UNEXPECTED: ", e);
                new BADResponse(imapConnection, e.getMessage()).submit();
                imapConnection.setKeepLog(true);
            }
        } else {
            new BADResponse(imapConnection, "Missing space after the command").submit();
        }
    }

    /**
     * Processes any command
     * @param imapConnection imap connection
     * @return command is recognised and processes
     * @throws IOException
     */
    public boolean commandAny(IMAPSession imapConnection) throws IOException {
        IMAPScanner scanner = imapConnection.getScanner();
        if (scanner.keyword("CAPABILITY")) {
            invokeCommand(imapConnection, "CAPABILITY");
            return true;
        } else if (scanner.keyword("LOGOUT")) {
            invokeCommand(imapConnection, "LOGOUT");
            return true;
        } else if (scanner.keyword("NOOP")) {
            invokeCommand(imapConnection, "NOOP");
            return true;
        } else {
            return false;
        }
    }

    /**
     * Processes non-authorised comamnds
     * @param imapConnection imap connection
     * @return command is found and executed
     * @throws IOException
     */
    public boolean commandNonAuth(IMAPSession imapConnection) throws IOException {
        IMAPScanner scanner = imapConnection.getScanner();
        if (scanner.keyword("LOGIN")) {
            invokeCommand(imapConnection, "LOGIN");
        } else if (scanner.keyword("AUTHENTICATE")) {
            invokeCommand(imapConnection, "AUTHENTICATE");
        } else if (scanner.keyword("STARTTLS")) {
            invokeCommand(imapConnection, "STARTTLS");
        } else {
            return false;
        }
        return true;
    }

    /**
     * Processes authorised commands
     * @param imapConnection imap connection
     * @return command is found and executed
     * @throws IOException
     */
    public boolean commandAuth(IMAPSession imapConnection) throws IOException {
        IMAPScanner scanner = imapConnection.getScanner();
        if (scanner.keyword("APPEND")) {
            invokeCommand(imapConnection, "APPEND");
        } else if (scanner.keyword("CREATE")) {
            invokeCommand(imapConnection, "CREATE");
        } else if (scanner.keyword("DELETE")) {
            invokeCommand(imapConnection, "DELETE");
        } else if (scanner.keyword("EXAMINE")) {
            invokeCommand(imapConnection, "EXAMINE");
        } else if (scanner.keyword("LIST")) {
            invokeCommand(imapConnection, "LIST");
        } else if (scanner.keyword("LSUB")) {
            invokeCommand(imapConnection, "LSUB");
        } else if (scanner.keyword("RENAME")) {
            invokeCommand(imapConnection, "RENAME");
        } else if (scanner.keyword("SELECT")) {
            invokeCommand(imapConnection, "SELECT");
        } else if (scanner.keyword("STATUS")) {
            invokeCommand(imapConnection, "STATUS");
        } else if (scanner.keyword("SUBSCRIBE")) {
            invokeCommand(imapConnection, "SUBSCRIBE");
        } else if (scanner.keyword("UNSUBSCRIBE")) {
            invokeCommand(imapConnection, "UNSUBSCRIBE");
        } else {
            return false;
        }
        return true;
    }

    /**
     * Processes select command
     * @param imapConnection imap connection
     * @return command is found and processed
     * @throws IOException
     */
    public boolean commandSelected(IMAPSession imapConnection) throws IOException {
        IMAPScanner scanner = imapConnection.getScanner();
        if (scanner.keyword("CHECK")) {
            invokeCommand(imapConnection, "CHECK");
        } else if (scanner.keyword("CLOSE")) {
            invokeCommand(imapConnection, "CLOSE");
        } else if (scanner.keyword("EXPUNGE")) {
            invokeCommand(imapConnection, "EXPUNGE");
        } else if (scanner.keyword("COPY")) {
            invokeCommand(imapConnection, "COPY");
        } else if (scanner.keyword("FETCH")) {
            invokeCommand(imapConnection, "FETCH");
        } else if (scanner.keyword("STORE")) {
            invokeCommand(imapConnection, "STORE");
        } else if (scanner.keyword("UID")) {
            invokeCommand(imapConnection, "UID");
        } else if (scanner.keyword("SEARCH")) {
            invokeCommand(imapConnection, "SEARCH");
        } else if (scanner.keyword("IDLE")) {
            invokeCommand(imapConnection, "IDLE");
        } else {
            return false;
        }
        return true;
    }
}
