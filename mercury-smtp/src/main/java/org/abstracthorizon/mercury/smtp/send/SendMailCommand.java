package org.abstracthorizon.mercury.smtp.send;

import java.io.IOException;

import org.abstracthorizon.mercury.common.StorageManager;
import org.abstracthorizon.mercury.smtp.SMTPResponse;
import org.abstracthorizon.mercury.smtp.SMTPResponses;
import org.abstracthorizon.mercury.smtp.SMTPSession;
import org.abstracthorizon.mercury.smtp.filter.FilterMailCommand;
import org.abstracthorizon.mercury.smtp.filter.MailSessionData;
import org.abstracthorizon.mercury.smtp.util.Path;

public class SendMailCommand extends FilterMailCommand {
    
    /**
     * Sets from path to the session. This method can test if and make an
     * different action (sending different response).
     *
     * @param path path to be stored in the session
     * @param session SMTP session
     * @throws IOException
     */
    protected void processMailFrom(SMTPSession session, Path path) throws IOException {
        MailSessionData data = session.getMailSessionData();
        StorageManager manager = session.getConnectionHandler().getStorageManager();
        
        path.setLocalDomain(manager.hasDomain(path.getDomain()));
        if (path.isLocalDomain()) {
            data.setSourceMailbox(path);
            session.sendResponse(SMTPResponses.OK_RESPONSE);
            session.setState(SMTPSession.STATE_MAIL);
        } else {
            SMTPResponse smtpResponse = SMTPResponses.AUTHENTICATION_CREDENTIALS_INVALID;
            session.sendResponse(smtpResponse);
        }
    }


}

