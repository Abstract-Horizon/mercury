package org.abstracthorizon.mercury.smtp.send;

import java.io.IOException;

import org.abstracthorizon.mercury.smtp.SMTPResponse;
import org.abstracthorizon.mercury.smtp.SMTPResponses;
import org.abstracthorizon.mercury.smtp.SMTPSession;
import org.abstracthorizon.mercury.smtp.filter.Filter;
import org.abstracthorizon.mercury.smtp.filter.FilterRcptCommand;
import org.abstracthorizon.mercury.smtp.filter.MailSessionData;
import org.abstracthorizon.mercury.smtp.filter.SMTPFilterCommandFactory;
import org.abstracthorizon.mercury.smtp.util.Path;

public class SendRcptCommand extends FilterRcptCommand {

    protected void processPath(SMTPSession session, Path path) throws IOException {
        MailSessionData data = session.getMailSessionData();
        String response = ((SMTPFilterCommandFactory)session.getCommandFactory()).setDestinationMailbox(data, path);

        if (!path.isLocalDomain()) {
            data.getDestinationMailboxes().add(path);
        }

        if (Filter.POSITIVE_RESPONSE.equals(response)) {
            session.sendResponse(SMTPResponses.OK_RESPONSE);
        } else {
            SMTPResponse smtpResponse = new SMTPResponse(550, response);
            session.sendResponse(smtpResponse);
        }
    }

}
