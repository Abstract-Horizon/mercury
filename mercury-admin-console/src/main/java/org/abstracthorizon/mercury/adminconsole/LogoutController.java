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

import static org.abstracthorizon.danube.http.cookie.CookieUtilities.getRequestCookies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.abstracthorizon.danube.connection.Connection;
import org.abstracthorizon.danube.http.HTTPConnection;
import org.abstracthorizon.danube.http.Status;
import org.abstracthorizon.danube.http.auth.JAASAuthenticatedHTTPContext;
import org.abstracthorizon.danube.http.cookie.Cookie;
import org.abstracthorizon.danube.http.cookie.CookieUtilities;
import org.abstracthorizon.danube.http.session.SimpleCookieSessionManager;
import org.abstracthorizon.danube.http.util.MultiStringMap;
import org.abstracthorizon.danube.mvc.Controller;
import org.abstracthorizon.danube.mvc.ModelAndView;
import org.abstracthorizon.mercury.accounts.spring.MaildirKeystoreStorageManager;


/**
 *
 * @author Daniel Sendula
 */
public class LogoutController
    implements
        Controller,
        RequiresStorageManager,
        RequiresIndexController {

    private MaildirKeystoreStorageManager storageManager;
    private IndexController indexController;
    private SimpleCookieSessionManager cookieManager;

    @Override
    public ModelAndView handleRequest(Connection connection) {

        HTTPConnection httpConnection = connection.adapt(HTTPConnection.class);
        MultiStringMap requestParameters = httpConnection.getRequestParameters();

        Map<String, Object> model = new HashMap<String, Object>();

        model.put("connection", connection);

        String message = requestParameters.getOnly("message");
        if (message == null) {
            model.put("message", "");
        } else {
            model.put("message", message);
        }

        Cookie logoutCookie = getRequestCookies(httpConnection).get("logout_cookie");
        if (logoutCookie == null) {
            logoutCookie = new Cookie();
            logoutCookie.setPath("/");
            logoutCookie.setName("logout_cookie");
            logoutCookie.setValue("false");
        }

        if (logoutCookie != null && "true".equals(logoutCookie.getValue())) {
            logoutCookie.setValue("false");
            List<Cookie> cookies = new ArrayList<Cookie>();
            cookies.add(logoutCookie);
            CookieUtilities.addResponseCookies(httpConnection, cookies);
        } else {
            httpConnection.setResponseStatus(Status.UNAUTHORIZED);
            httpConnection.getResponseHeaders().add(JAASAuthenticatedHTTPContext.AUTHORIZATION_RESPONSE_HEADER, "Basic realm=\"/\"");
            logoutCookie.setValue("true");
            List<Cookie> cookies = new ArrayList<Cookie>();
            cookies.add(logoutCookie);
            CookieUtilities.addResponseCookies(httpConnection, cookies);
        }

        ModelAndView modelAndView = new ModelAndView("html/redirect_to_index", model);
        return modelAndView;
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

    public SimpleCookieSessionManager getCookieManager() {
        return cookieManager;
    }

    public void setCookieManager(SimpleCookieSessionManager cookieManager) {
        this.cookieManager = cookieManager;
    }
}
