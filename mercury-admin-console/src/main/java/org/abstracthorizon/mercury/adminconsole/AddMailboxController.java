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
public class AddMailboxController
    implements
        Controller,
        RequiresStorageManager,
        RequiresIndexController {


    private MaildirKeystoreStorageManager storageManager;
    private IndexController indexController;

    public ModelAndView handleRequest(Connection connection) {
        HTTPConnection httpConnection = connection.adapt(HTTPConnection.class);

        Map<String, Object> model = new HashMap<String, Object>();

        model.put("connection", connection);

        MultiStringMap requestParameters = httpConnection.getRequestParameters();

        String domain = requestParameters.getOnly("domain");
        if (domain == null || "".contentEquals(domain)) {
            httpConnection.getRequestParameters().add("message", "No domain specified.");
            return indexController.handleRequest(httpConnection);
        }

        String mailbox = requestParameters.getOnly("mailbox");
        if (mailbox == null || "".contentEquals(mailbox)) {
            httpConnection.getRequestParameters().add("message", "No mailbox specified.");
            return indexController.handleRequest(httpConnection);
        }

        String password = requestParameters.getOnly("password");
        if (password == null || "".contentEquals(password)) {
            httpConnection.getRequestParameters().add("message", "No password specified.");
            return indexController.handleRequest(httpConnection);
        }

        String password2 = requestParameters.getOnly("password2");
        if (password2 == null || "".contentEquals(password2)) {
            httpConnection.getRequestParameters().add("message", "No password2 specified.");
            return indexController.handleRequest(httpConnection);
        }

        if (!password.equals(password2)) {
            httpConnection.getRequestParameters().add("message", "Passwords do not match.");
            return indexController.handleRequest(httpConnection);
        }

        storageManager.addMailbox(mailbox, domain, password.toCharArray());
        // PUSH CHANGES

        httpConnection.getRequestParameters().add("message", "Added mailbox " + mailbox + "@" + domain);
        return indexController.handleRequest(httpConnection);
    }

    public MaildirKeystoreStorageManager getStorageManager() {
        return storageManager;
    }

    public void setStorageManager(MaildirKeystoreStorageManager storageManager) {
        this.storageManager = storageManager;
    }

    public IndexController getIndexController() {
        return indexController;
    }

    public void setIndexController(IndexController indexController) {
        this.indexController = indexController;
    }
}
