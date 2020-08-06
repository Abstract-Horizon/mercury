/*
 * Copyright (c) 2006-2019 Creative Sphere Limited.
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
package org.abstracthorizon.mercury.adminconsole;

import java.security.UnrecoverableKeyException;
import java.util.HashMap;
import java.util.Map;

import org.abstracthorizon.danube.connection.Connection;
import org.abstracthorizon.danube.http.HTTPConnection;
import org.abstracthorizon.danube.http.util.MultiStringMap;
import org.abstracthorizon.danube.mvc.Controller;
import org.abstracthorizon.danube.mvc.ModelAndView;
import org.abstracthorizon.mercury.accounts.spring.MaildirKeystoreStorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Daniel Sendula
 */
public class ChangeMailboxPasswordController implements Controller {

    private static Logger logger = LoggerFactory.getLogger(ChangeMailboxPasswordController.class);

    private MaildirKeystoreStorageManager storageManager;
    private MailboxController mailboxController;

    public ModelAndView handleRequest(Connection connection) {
        HTTPConnection httpConnection = connection.adapt(HTTPConnection.class);

        String loggedAsMailbox = SubjectUtils.getLogginedInMailbox();
        boolean isAdmin = "admin".equalsIgnoreCase(loggedAsMailbox);

        Map<String, Object> model = new HashMap<String, Object>();

        model.put("connection", connection);

        MultiStringMap requestParameters = httpConnection.getRequestParameters();

        String domain = requestParameters.getOnly("domain");
        if (domain == null || "".contentEquals(domain)) {
            httpConnection.getRequestParameters().add("message", "No domain specified.");
            return getMailboxController().handleRequest(httpConnection);
        }

        String mailbox = requestParameters.getOnly("mailbox");
        if (mailbox == null || "".contentEquals(mailbox)) {
            httpConnection.getRequestParameters().add("message", "No mailbox specified.");
            return getMailboxController().handleRequest(httpConnection);
        }

        String oldPassword = requestParameters.getOnly("oldpassword");
        if (!isAdmin && oldPassword == null || "".contentEquals(oldPassword)) {
            httpConnection.getRequestParameters().add("message", "No old password specified.");
            return getMailboxController().handleRequest(httpConnection);
        }

        String password = requestParameters.getOnly("password");
        if (password == null || "".contentEquals(password)) {
            httpConnection.getRequestParameters().add("message", "No password specified.");
            return getMailboxController().handleRequest(httpConnection);
        }

        String password2 = requestParameters.getOnly("password2");
        if (password2 == null || "".contentEquals(password2)) {
            httpConnection.getRequestParameters().add("message", "No password2 specified.");
            return getMailboxController().handleRequest(httpConnection);
        }

        if (!password.equals(password2)) {
            httpConnection.getRequestParameters().add("message", "Passwords do not match.");
            return getMailboxController().handleRequest(httpConnection);
        }

        try {
            if (isAdmin) {
                storageManager.changeMailboxPassword(mailbox, domain, null, password.toCharArray());
            } else {
                storageManager.changeMailboxPassword(mailbox, domain, oldPassword.toCharArray(), password.toCharArray());
            }
        } catch (RuntimeException e) {
            if (e.getCause() instanceof UnrecoverableKeyException) {
                httpConnection.getRequestParameters().add("message", "Failed to change password.");
            } else {
                httpConnection.getRequestParameters().add("message", "Problem changing password.");
                logger.error("Failed changing password", e.getCause());
            }
            return getMailboxController().handleRequest(httpConnection);
        } catch (Exception e) {
            httpConnection.getRequestParameters().add("message", "Problem changing password.");
            logger.error("Failed changing password", e);
            return getMailboxController().handleRequest(httpConnection);
        }
        // PUSH CHANGES

        httpConnection.getRequestParameters().add("message", "Changed password for mailbox " + mailbox + "@" + domain);
        return getMailboxController().handleRequest(httpConnection);
    }

    public MaildirKeystoreStorageManager getStorageManager() {
        return storageManager;
    }

    public void setStorageManager(MaildirKeystoreStorageManager storageManager) {
        this.storageManager = storageManager;
    }

    public MailboxController getMailboxController() {
        return mailboxController;
    }

    public void setMailboxController(MailboxController mailboxController) {
        this.mailboxController = mailboxController;
    }

}
