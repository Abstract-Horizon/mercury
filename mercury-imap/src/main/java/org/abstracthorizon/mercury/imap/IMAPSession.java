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
package org.abstracthorizon.mercury.imap;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.net.Socket;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.abstracthorizon.danube.connection.Connection;
import org.abstracthorizon.danube.connection.ConnectionWrapper;
import org.abstracthorizon.danube.support.logging.LoggingConnection;
import org.abstracthorizon.mercury.common.util.SSLUtil;
import org.abstracthorizon.mercury.imap.response.ExistsResponse;
import org.abstracthorizon.mercury.imap.response.RecentResponse;
import org.abstracthorizon.mercury.imap.util.IMAPScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A class that represents IMAP session. This is connection wrapper.
 *
 * @author Daniel Sendula
 */
public class IMAPSession extends ConnectionWrapper implements MessageCountListener {

    /** Logger */
    protected static final Logger logger = LoggerFactory.getLogger(IMAPSession.class);

    /** Handler that created this session */
    protected IMAPConnectionHandler parent;

    /** Current tag */
    protected String tag;

    /** Current command line */
    protected String commandLine;

    /** Is user authenticated and authorised to use some commands */
    protected boolean authorised = false;

    /** Output writer */
    protected Writer writer;

    /** Input reader */
    protected Reader reader;

    /** IMAP scanner to be used */
    protected IMAPScanner scanner;

    /** Mail store */
    protected Store store;

    /** Selected folder */
    protected Folder selectedFolder = null;

    /** Mail session */
    protected Session session;

    /** Login context */
    protected LoginContext lc;

    /** Is secure conneciton */
    protected boolean secure = false;

    /** Are we in IDLE mode */
    protected boolean idling = false;

    /**
     * Clear flag is set to indicate that skip_line mustn't be called after command.
     * This was needed for STARTTLS command.
     */
    protected boolean cleanInput = false;

    /** Cached socket */
    protected Socket socket;

    /** Timestamp command started */
    protected long commandStarted;

    /** Default domain if not specified in username */
    protected String defaultDomain;

    /**
     * Constructor
     * @param connection connection
     * @param parent imap connection handler that is creating this object
     */
    public IMAPSession(Connection connection, IMAPConnectionHandler parent) {
        super(connection);
        this.parent = parent;
        InputStream inputStream = (InputStream)connection.adapt(InputStream.class);
        OutputStream outputStream = (OutputStream)connection.adapt(OutputStream.class);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        scanner = new IMAPScanner(bufferedInputStream, outputStream);

        Properties props = new Properties();
        // We don't want strict headers parsing!
        props.setProperty("mail.mime.address.strict", "false");
        session = Session.getInstance(props);
    }

    /**
     * Returns imap connection handler that created this object
     * @return imap connection handler that created this object
     */
    public IMAPConnectionHandler getParent() {
        return parent;
    }

    /**
     * Sets should log be kept or not
     * @param keepLog should log be kept
     */
    public void setKeepLog(boolean keepLog) {
        LoggingConnection loggingConnection = (LoggingConnection)connection.adapt(LoggingConnection.class);
        if (loggingConnection != null) {
            loggingConnection.setTemporaryLog(!keepLog);
        }
    }

    /**
     * Returns should log be kept or not
     * @return should log be kept or not
     */
    public boolean getKeepLog() {
        LoggingConnection loggingConnection = (LoggingConnection)connection.adapt(LoggingConnection.class);
        if (loggingConnection != null) {
            return !loggingConnection.isTermporaryLog();
        } else {
            return false;
        }
    }

    /**
     * Writes log message
     * @param msg log message
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
     * Returns debug output stream
     * @return debug output stream
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
     * Is session authorised or not
     * @return
     */
    public boolean isAuthorised() {
        return authorised;
    }

    /**
     * Returns number of millis how long last command execution lasted.
     * @return difference of time between now and when last command has been started
     */
    public long getCommandLasted() {
        return System.currentTimeMillis() - commandStarted;
    }

    /**
     * Marks when command has started
     */
    public void markCommandStarted() {
        commandStarted = System.currentTimeMillis();
    }

    /**
     * Returns JavaMail store
     * @return JavaMail store
     */
    public Store getStore() {
        return store;
    }

    /**
     * Returns selected Folder
     * @return selected Folder
     */
    public Folder getSelectedFolder() {
        return selectedFolder;
    }

    /**
     * Sets selected Folder
     * @param folder selected Folder
     */
    public void setSelectedFolder(Folder folder) {
        if (folder != null) {
            logger.debug("Already selected folder "+folder.getName());
            folder.removeMessageCountListener(this);
        }
        selectedFolder = folder;
        if (folder != null) {
            selectedFolder.addMessageCountListener(this);
        }
    }

    /**
     * IMAP scanner
     * @return IMAP scanner
     */
    public IMAPScanner getScanner() {
            return scanner;
    }

    /**
     * Returns JavaMail session
     * @return JavaMail session
     */
    public Session getJavaMailSession() {
        return session;
    }

    /**
     * Sets JavaMail session
     * @param session JavaMail session
     */
    public void setJavaMailSession(Session session) {
        this.session = session;
    }

    /**
     * Returns if insecure is allowed
     * @return if insecure is allowed
     */
    public boolean isInsecureAllowed() {
        return parent.isInsecureAllowed();
    }

    /**
     * Returns if socket is SSL socket if socket available. Otherwise <code>true</code>
     * @return if socket is SSL socket
     */
    public boolean isSecure() {
        Socket socket = (Socket)adapt(Socket.class);
        if (socket != null) {
            return socket instanceof SSLSocket;
        } else {
            return true;
        }
    }

    /**
     * Sets if IDLE command is allowed
     * @param idling if IDLE command is allowed
     */
    public synchronized void setIdling(boolean idling) {
        this.idling = idling;
    }

    /**
     * Returns if IDLE command is allowed
     * @return if IDLE command is allowed
     */
    public boolean isIdling() {
        return idling;
    }

    /**
     * Returns if it is clean input
     * @return if it is clean input
     */
    public boolean isCleanInput() {
        return cleanInput;
    }

    /**
     * Sets if it is clean input
     * @param cleanInput is clean input
     */
    public void setCleanInput(boolean cleanInput) {
        this.cleanInput = cleanInput;
    }

    /**
     * Returns current tag
     * @return current tag
     */
    public String getTag() {
        return tag;
    }

    /**
     * Sets current tag
     * @param tag current tag
     */
    public void setTag(String tag) {
        this.tag = tag;
    }

    /**
     * Returns default domain name
     * @return default domain name
     */
    public String getDefaultDomain() {
        return defaultDomain;
    }

    /**
     * Sets default domain name to be used with usernames
     * @param domain
     */
    public void setDefaultDomain(String domain) {
        this.defaultDomain = domain;
    }


    /**
     * Authorises session. It uses storage manager for it. If username doesn't contain
     * domain then it uses default domain name as set in {@link #setDefaultDomain(String)}
     * @param user username
     * @param pass password
     * @return <code>true</code> if it succeded
     * @throws MessagingException
     */
    public boolean authorise(String user, String pass) throws MessagingException {
        String domain;
        int i = user.indexOf('@');
        if (i >= 1) {
            domain = user.substring(i + 1);
            user = user.substring(0, i);
        } else if (defaultDomain != null) {
            domain = defaultDomain;
        } else {
            domain = parent.getStorageManager().getMainDomain();
        }


        if (domain != null) {
            logger.info("AUTHORISE: user " + user + " for domain " + domain);
        } else {
            logger.info("AUTHORISE: user " + user + " without domain");
        }


        try {
            store = parent.getStorageManager().findStore(user, domain, pass.toCharArray());
            authorised = true;
        } catch (Exception e) {
            logger.error("Failed to find a store", e);
        }

        return authorised;
    }

    /**
     * Removes authorisation
     */
    public void unauthorise() {
        authorised = false;
        if (lc != null) {
            try {
                lc.logout();
            } catch (LoginException ignore) {
            }
        }
    }

    /**
     * Closes IMAP session (connection)
     */
    public void close() {
        if ((selectedFolder != null) && (selectedFolder.isOpen())) {
            try {
                selectedFolder.removeMessageCountListener(this);
                if (selectedFolder.isOpen()) {
                    selectedFolder.close(false);
                }
            } catch (IllegalStateException ignore) {
            } catch (MessagingException e1) {
                // we don't care!
                logger.debug("Forced close on folder excetion ", e1);
            }
        }
        super.close();
        logger.debug("Finished session ");
    }

    /**
     * Switches to TLS (SSL) socket
     * @throws IOException
     */
    public void switchToTLS() throws IOException {
        char[] passPhrase = parent.getPassPhrase();
        InputStream keyStoreInputStream = parent.getKeyStoreInputStream();
        SSLSocketFactory factory = SSLUtil.getSocketFactory(passPhrase, keyStoreInputStream);
        if (factory == null) {
            throw new IOException("Cannot obtain SSLSocket Factory");
        }

        // TODO - this makes little sense!!!
        socket = factory.createSocket(socket, socket.getInetAddress().getHostName(), socket.getPort(), true);
        ((SSLSocket)socket).setUseClientMode(false);
    }

    /**
     * Notifies that new message is added
     * @param event message count event
     */
    public synchronized void messagesAdded(MessageCountEvent event) {
        if ((selectedFolder != null) && idling) {
            parent.getThreadPool().execute(new Runnable(){
                public void run() {
                    try {
                        new RecentResponse(IMAPSession.this, selectedFolder).submit();
                        new ExistsResponse(IMAPSession.this, selectedFolder).submit();
                    } catch (IOException ignore) {
                    } catch (MessagingException ignore) {
                    }
                }
            });
        }
    }

    /**
     * NOtifies that message is removed
     * @param even event
     */
    public void messagesRemoved(MessageCountEvent event) {
        messagesAdded(event);
    }
}
