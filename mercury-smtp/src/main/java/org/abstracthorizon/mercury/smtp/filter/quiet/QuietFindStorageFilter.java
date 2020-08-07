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
package org.abstracthorizon.mercury.smtp.filter.quiet;

import javax.mail.MessagingException;

import org.abstracthorizon.mercury.common.StorageManager;
import org.abstracthorizon.mercury.common.exception.UnknownUserException;
import org.abstracthorizon.mercury.common.exception.UserRejectedException;
import org.abstracthorizon.mercury.smtp.SMTPResponses;
import org.abstracthorizon.mercury.smtp.filter.Filter;
import org.abstracthorizon.mercury.smtp.filter.MailSessionData;
import org.abstracthorizon.mercury.smtp.util.Path;


/**
 * Filter that checks if mailbox is in local storage but it doesn't do anything if
 * it can't be found.
 *
 * @author Daniel Sendula
 */
public class QuietFindStorageFilter implements Filter {

    public int features() {
        return Filter.CAN_PROCESS_DESTINATION_MAILBOX;
    }

    public void startSession(MailSessionData data) {
    }

    public String processSourceDomain(MailSessionData data) {
        return null;
    }

    public String processSourceMailbox(MailSessionData data) {
        return null;
    }

    public String processDestinationMailbox(MailSessionData data, Path path) {
        StorageManager manager = (StorageManager)data.getAttribute("manager");

        path.setLocalDomain(manager.hasDomain(path.getDomain()));
        if (path.isLocalDomain()) {
            try {
                //Store store = manager.getLocalMailbox(path.getMailbox(), path.getDomain());
                //path.setStore(store);
                path.setFolder(manager.findInbox(path.getMailbox(), path.getDomain(), null));
                data.getDestinationMailboxes().add(path);
                // return Filter.POSITIVE_RESPONSE; // STOP with processing here
                //return null;
            } catch (MessagingException e) {
                return SMTPResponses.GENERIC_ERROR_RESPONSE.toString().substring(4); // TODO not nice
            } catch (UnknownUserException e) {
                //return SMTPResponses.MAILBOX_UNAVAILABLE_RESPONSE.toString().substring(4); // TODO not nice
            } catch (UserRejectedException e) {
                //return SMTPResponses.MAILBOX_UNAVAILABLE_RESPONSE.toString().substring(4); // TODO not nice
            }
        } else {
            //return SMTPResponses.MAILBOX_UNAVAILABLE_RESPONSE.toString().substring(4); // TODO not nice
            //return Filter.POSITIVE_RESPONSE; // STOP with processing here
        }
        return null;
    }

    public String preLoadCheck(MailSessionData data) {
        return null;
    }

    public String postLoadCheck(MailSessionData data) {
        return null;
    }

    public void finishSession(MailSessionData data) {
    }

}
