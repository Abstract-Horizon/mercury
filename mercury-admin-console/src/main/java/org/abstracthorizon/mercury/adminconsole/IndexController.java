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

import java.util.HashMap;
import java.util.Map;

import org.abstracthorizon.danube.connection.Connection;
import org.abstracthorizon.danube.http.HTTPConnection;
import org.abstracthorizon.danube.http.util.MultiStringMap;
import org.abstracthorizon.danube.mvc.Controller;
import org.abstracthorizon.danube.mvc.ModelAndView;
import org.abstracthorizon.mercury.accounts.spring.MaildirKeystoreStorageManager;


/**
 *
 * @author Daniel Sendula
 */
public class IndexController implements Controller {

    private MaildirKeystoreStorageManager storageManager;
    private MailboxController mailboxController;

    public ModelAndView handleRequest(Connection connection) {

        HTTPConnection httpConnection = connection.adapt(HTTPConnection.class);
        MultiStringMap requestParameters = httpConnection.getRequestParameters();

        String loggedAsMailbox = SubjectUtils.getLogginedInMailbox();
        if (!"admin".equalsIgnoreCase(loggedAsMailbox)) {
            requestParameters.add("mailbox", loggedAsMailbox);

            return mailboxController.handleRequest(connection);
        }


        Map<String, Object> model = new HashMap<String, Object>();

        model.put("connection", connection);

        String message = requestParameters.getOnly("message");
        if (message == null) {
            model.put("message", "");
        } else {
            model.put("message", message);
        }

        model.put("storage_manager", storageManager);

        model.put("main_domain", storageManager.getMainDomain());
        model.put("domains", storageManager.getDomains());
        model.put("mailboxes", storageManager.getMailboxNames());
        model.put("aliases", storageManager.getAliases());

        ModelAndView modelAndView = new ModelAndView("html/index", model);
        return modelAndView;
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
