package org.abstracthorizon.mercury.smtp.send;

import java.io.IOException;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.abstracthorizon.mercury.common.command.CommandException;
import org.abstracthorizon.mercury.smtp.SMTPResponses;
import org.abstracthorizon.mercury.smtp.SMTPSession;
import org.abstracthorizon.mercury.smtp.filter.FilterDataCommand;
import org.abstracthorizon.mercury.smtp.filter.MailSessionData;
import org.abstracthorizon.mercury.smtp.filter.SMTPFilterCommandFactory;
import org.abstracthorizon.mercury.smtp.util.Path;

public class SendDataCommand extends FilterDataCommand {

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
    protected boolean precheck(SMTPSession session) throws CommandException, IOException {
        session.sendResponse(SMTPResponses.START_DATA_RESPONSE);
        return true;
    }

    protected boolean postcheck(SMTPSession connection) throws IOException {
        connection.resetLastAccessed();
        return super.postcheck(connection);
    }

    protected void postProcessing(SMTPSession session, boolean hasSuccessfuls) throws IOException {
        super.postProcessing(session, hasSuccessfuls);
        MailSessionData mailSessionData = session.getMailSessionData();

        ((SMTPFilterCommandFactory)session.getCommandFactory()).finish(mailSessionData);

//        MailSessionData mailSessionData = session.getMailSessionData();
//        SMTPSendCommandFactory commandFactory = (SMTPSendCommandFactory)session.getCommandFactory();
//
//        MimeMessage message = mailSessionData.getMessage();
//
//        try {
//            commandFactory.getTransport().send(message, mailSessionData.getDestinationMailboxes(), mailSessionData.getSourceMailbox());
//        } catch (MessagingException e) {
//            hasSuccessfuls = false;
//        }
//        if (hasSuccessfuls) {
//            session.sendResponse(SMTPResponses.OK_RESPONSE);
//        } else {
//            session.sendResponse(SMTPResponses.MAILBOX_UNAVAILABLE_RESPONSE);
//        }
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
        MailSessionData mailSessionData = session.getMailSessionData();
        SMTPSendCommandFactory commandFactory = (SMTPSendCommandFactory)session.getCommandFactory();

        boolean hasSuccessfuls = true;
        try {
            commandFactory.getTransport().send(message, externals, mailSessionData.getSourceMailbox());
        } catch (MessagingException e) {
            hasSuccessfuls = false;
        }

        return hasSuccessfuls;
    }

}
