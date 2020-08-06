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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Flags.Flag;
import javax.mail.internet.MimeMessage;

import org.abstracthorizon.mercury.common.command.CommandException;
import org.abstracthorizon.mercury.common.io.TempStorage;
import org.abstracthorizon.mercury.smtp.SMTPResponses;
import org.abstracthorizon.mercury.smtp.SMTPScanner;
import org.abstracthorizon.mercury.smtp.SMTPSession;
import org.abstracthorizon.mercury.smtp.exception.ParserException;
import org.abstracthorizon.mercury.smtp.util.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SMTP DATA command.
 *
 * @author Daniel Sendula
 */
public class DataCommand extends SMTPCommand {

    protected static final byte[] CRLF = new byte[] { '\r', '\n' };

    public static final SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");

    protected static final Logger logger = LoggerFactory.getLogger(DataCommand.class);

    public static Session mailSession = Session.getDefaultInstance(new Properties()); // Do this more gracefully


    /**
     * Constructor
     */
    public DataCommand() {
    }

    /**
     * Executed the command
     * @param connection smtp session
     * @throws CommandException
     * @throws IOException
     * @throws ParserException
     */
    protected void execute(SMTPSession connection) throws CommandException, IOException, ParserException {
        if (connection.getState() != SMTPSession.STATE_MAIL) {
            connection.sendResponse(SMTPResponses.BAD_SEQUENCE_OF_COMMANDS_RESPONSE);
            return;
        }
        try {
            SMTPScanner scanner = connection.getScanner();

            readExtraParameters(connection, scanner);

            if (precheck(connection)) {
                TempStorage tempStorage = new TempStorage();
                OutputStream out = tempStorage.getOutputStream();
                out.write(composeReceivedHeader(connection).getBytes());
                out.write(CRLF);
                try {
                    connection.setStreamDebug(false);
                    readMail(connection.getInputStream(), out);
                    out.close();
                } catch (IOException e) {
                    // TODO should we drop the line here?
                    // Scenario: data is late in the middle of e-mail
                    // we have timeout + data arrive
                    // -> loads of syntax errors and other side gives up
                    // Solution: soon we have IO exception - we send response
                    // and close the socket?
                    if (!(e instanceof SocketTimeoutException)) {
                        logger.error("Problem reading message", e);
                    }
                    connection.sendResponse(SMTPResponses.GENERIC_ERROR_RESPONSE);
                    connection.getMailSessionData().addToTotalBytes(tempStorage.getSize());
                    tempStorage.clear();
                    tempStorage = null;
                } finally {
                    connection.setStreamDebug(true);
                }

                // prevents further processing in case of an error while reading
                // message from the input
                if (tempStorage != null) {
                    try {
                        MimeMessage message = new MimeMessage(mailSession, tempStorage.getInputStream());
                        File file = tempStorage.getFile();
                        if (file != null) {
                            message.setFileName(file.getAbsolutePath());
                        }
                        message.setFlag(Flag.RECENT, true);
                        connection.getMailSessionData().setMessage(message);
                        processMail(connection, message);
                    } catch (MessagingException e) {
                        logger.error("Problem creating message", e);
                    } finally {
                        connection.getMailSessionData().setMessage(null);
                        connection.getMailSessionData().addToTotalBytes(tempStorage.getSize());
                        tempStorage.clear();
                    }
                }
            }
        } finally {
            connection.setState(SMTPSession.STATE_READY);
        }
    }

    /**
     * Adds &quot;Received:&quot; header.
     * @param connection SMTP session
     * @throws IOException io exception
     */
    protected String composeReceivedHeader(SMTPSession connection) throws IOException {
        Date now = new Date();
        StringBuffer received = new StringBuffer("Received:");
        Socket socket = connection.adapt(Socket.class);
        if (socket != null) {
            append(received, " from " + connection.getMailSessionData().getSourceDomain() + getTCPInfo(socket));
        } else {
            append(received, " from " + connection.getMailSessionData().getSourceDomain());
        }
        append(received, " by " + connection.getConnectionHandler().getStorageManager().getMainDomain());
        append(received, " for " + composeDestMailboxes(connection.getMailSessionData().getDestinationMailboxes()));
        append(received, "; " + format.format(now));
        return received.toString();
    }

    /**
     * Returns inet address as string
     * @param socket socket
     * @return string representation of a string
     */
    protected String getTCPInfo(Socket socket) {
        return " (["+((InetSocketAddress)socket.getRemoteSocketAddress()).getAddress().getHostAddress()+"])";
    }

    /**
     * Composes Received header's list of destination mailboxes
     * @param dest list of mailboxes
     * @return string representation of 's list of destination mailboxes
     */
    protected String composeDestMailboxes(List<Path> dest) {
        StringBuffer buf = new StringBuffer();
        int i = 0;
        while ((i < dest.size()) && (i < 3)) {
            Path path = dest.get(i);
            if (i > 0) {
                buf.append(',');
            }
            buf.append(path.getMailbox());
            i = i + 1;
        }
        if (i < dest.size()) {
            buf.append(",...");
        }
        return buf.toString();
    }

    /**
     * Appends elements to header value making it sure it is not over 999 chars.
     * @param buf buffer to append to
     * @param part new elemnt to added to header value
     */
    protected void append(StringBuffer buf, String part) {
        // TODO this is not optimised. No reason for lastIndexOf to be called that many times
        int i = buf.lastIndexOf("\r\n");
        if (i < 0) {
            i = 0;
        } else {
            i = i+2;
        }
        if (buf.length()-i+part.length() > 999) {
            buf.append("\r\n  ");
        }
        buf.append(part);
    }

    /**
     * Reads raw mail from the input stream. <br>
     * Method to be overriden for DATA extensions.
     *
     * @param in input stream mail is read from. Usually input stream from the
     *            socket
     * @param mail output stream mail is written to
     * @throws IOException in case of an exception while reading mail
     */
    protected void readMail(InputStream in, OutputStream mail) throws IOException {

        boolean first = true;
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "ISO-8859-1"));

        String line = reader.readLine();
        while (line != null) {
            if (first) {
                first = false;
            } else {
                if (line.equals(".")) { return; }
            }
            if (line.startsWith(".")) {
                line = line.substring(1);
            }
            mail.write(line.getBytes());
            mail.write(CRLF);
            line = reader.readLine();
        }
    }

    /**
     * Obtains extra parameters. <br>
     * Method to be overriden for DATA extensions.
     *
     * @param session SMTP session
     * @param scanner STMP scanner
     * @throws IOException io exception
     * @throws ParserException parsing exception
     * @throws CommandException command exception
     */
    protected void readExtraParameters(SMTPSession connection, SMTPScanner scanner) throws IOException, ParserException, CommandException {
        scanner.check_eol();
    }

    /**
     * Returns <code>true</code> in case it is ok with proceeding with the
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
    protected boolean precheck(SMTPSession connection) throws CommandException, IOException {
        connection.sendResponse(SMTPResponses.START_DATA_RESPONSE);
        return true;
    }

    /**
     * Returns <code>true</code>
     * @param connection smtp session
     * @return <code>true</code>
     * @throws IOException
     */
    protected boolean postcheck(SMTPSession connection) throws IOException {
        // Message is going to be delivered - so we can reset inactivity counter
        connection.resetLastAccessed();
        return true;
    }

    /**
     * Sets positive response if there are successful mailboxes
     * @param connection smtp session
     * @param hasSuccessfuls has successful mailboxes
     * @throws IOException
     */
    protected void postProcessing(SMTPSession connection, boolean hasSuccessfuls) throws IOException {
        if (hasSuccessfuls) {
            connection.sendResponse(SMTPResponses.OK_RESPONSE);
        } else {
//          session.sendResponse(SMTPResponses.GENERIC_ERROR_RESPONSE);
            connection.sendResponse(SMTPResponses.MAILBOX_UNAVAILABLE_RESPONSE);
        }
    }

    /**
     * Processes mail.
     *
     * @param connection SMTP session
     * @param message mime message
     * @throws IOException needed for sending responses
     */
    protected void processMail(SMTPSession connection, MimeMessage message) throws IOException {
        if (postcheck(connection)) {
            boolean hasSuccesfuls = false;
            List<Path> externals = new ArrayList<Path>();
            Iterator<Path> it = connection.getMailSessionData().getDestinationMailboxes().iterator();
            while (it.hasNext()) {
                Path path = it.next();
                if (path.isLocalMailbox()) {
                    if (processLocalMailbox(connection, path, message)) {
                        hasSuccesfuls = true;
                    }
                } else if (!path.isLocalDomain()) {
                    externals.add(path);
                }
            } // while

            if (externals.size() > 0) {
                processExternalMail(connection, externals, message);
            }

            postProcessing(connection, hasSuccesfuls);
        }
    }

    /**
     * Processes local storage mails.
     *
     * @param connection SMTP session
     * @param path path object
     * @param message message
     * @return <code>true</code> in case it succeded
     */
    protected boolean processLocalMailbox(SMTPSession connection, Path path, MimeMessage message) {
        try {
            Folder folder = path.getFolder();
            folder.open(Folder.READ_WRITE);

            if (message.getSentDate() == null) {
                Date rd = message.getReceivedDate();
                if (rd == null) {
                    rd = new Date(System.currentTimeMillis());
                }
                message.setSentDate(rd);
                message.setHeader("Date", format.format(rd));
            }

            try {
                folder.appendMessages(new Message[] { message });
            } finally {
                folder.close(false);
            }
            return true;
        } catch (MessagingException e) {
            logger.error("Storing problem", e);
        }
        return false;
    }

    /**
     * Processes external mails - invokes transport protocol in sending mail
     * further or caching it localing for delayed send.
     *
     * @param connection smtp session
     * @param externals list of <code>Path</code> objects
     * @param message mime message to be forwarded
     * @return <code>true</code> in case it succeded
     */
    protected boolean processExternalMail(SMTPSession session, List<Path> externals, MimeMessage message) {
        // TODO
        return false;
    }

}
