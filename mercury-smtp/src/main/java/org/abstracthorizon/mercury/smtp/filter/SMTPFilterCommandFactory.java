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
package org.abstracthorizon.mercury.smtp.filter;

import java.util.ArrayList;
import java.util.List;

import org.abstracthorizon.mercury.smtp.command.SMTPCommandFactory;
import org.abstracthorizon.mercury.smtp.util.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SMTP factory that uses filter API ({@link Filter}) to delegate various parts of
 * SMTP commands handling.
 *
 * @author Daniel Sendula
 */
public class SMTPFilterCommandFactory extends SMTPCommandFactory {

    /** Logger */
    protected static final Logger logger = LoggerFactory.getLogger(SMTPFilterCommandFactory.class);

    /** List of all filters */
    protected List<Filter> filters = new ArrayList<Filter>();

    /** List of filters that process source domain */
    protected List<Filter> processSourceDomain = new ArrayList<Filter>();

    /** List of filters that process source mailbox */
    protected List<Filter> processSourceMailbox = new ArrayList<Filter>();

    /** List of filters that process destination mailbox */
    protected List<Filter> processDestinationMailbox = new ArrayList<Filter>();

    /** List of filters that process pre-load checks */
    protected List<Filter> preloadCheck = new ArrayList<Filter>();

    /** List of filters that process post-load checks */
    protected List<Filter> postloadCheck = new ArrayList<Filter>();

    /**
     * Constructor
     */
    public SMTPFilterCommandFactory() {
        super();
        commands.put(EHLO, new FilterEhloCommand());
        commands.put(HELO, new FilterEhloCommand());
        commands.put(MAIL, new FilterMailCommand());
        commands.put(RCPT, new FilterRcptCommand());
        commands.put(DATA, new FilterDataCommand());
        commands.put(RSET, new FilterResetCommand());
    }

    /**
     * Sets filters
     * @param filters filters
     */
    public void setFilters(List<Filter> filters) {
        this.filters = filters;
    }

    /**
     * Returns filters
     * @return filters
     */
    public List<Filter> getFilters() {
        return filters;
    }

    /**
     * Initialises filters
     */
    public void init() {
        for (Filter filter: filters) {
            int features = filter.features();
            if ((features & Filter.CAN_PROCESS_SOURCE_DOMAIN) != 0) {
                processSourceDomain.add(filter);
            }
            if ((features & Filter.CAN_PROCESS_SOURCE_MAILBOX) != 0) {
                processSourceMailbox.add(filter);
            }
            if ((features & Filter.CAN_PROCESS_DESTINATION_MAILBOX) != 0) {
                processDestinationMailbox.add(filter);
            }
            if ((features & Filter.CAN_DO_PRELOAD_CHECK) != 0) {
                preloadCheck.add(filter);
            }
            if ((features & Filter.CAN_DO_POSTLOAD_CHECK) != 0) {
                postloadCheck.add(filter);
            }
        }
    }

    /**
     * Cleans up
     */
    public void cleanup() {
        processDestinationMailbox.clear();
        processSourceDomain.clear();
        processSourceMailbox.clear();
        preloadCheck.clear();
        postloadCheck.clear();
    }

    /**
     * Starts processing
     * @param data mail session data
     */
    public void start(MailSessionData data) {
        for (Filter filter: filters) {
            filter.startSession(data);
        }
    }

    /**
     * Finishes processing
     * @param data mail session data
     */
    public void finish(MailSessionData data) {
        for (Filter filter: filters) {
            filter.finishSession(data);
        }
    }

    /**
     * Sets source domain
     * @param data mail session data
     * @return result of processing
     */
    public String setSourceDomain(MailSessionData data) {
        for (int i=0; i<processSourceDomain.size(); i++) {
            Filter filter = (Filter)processSourceDomain.get(i);
            String response = filter.processSourceDomain(data);
            if (response != null) {
                processMailSessionData(response, data);
                return response;
            }
        }
        return Filter.POSITIVE_RESPONSE;
    }

    /**
     * Sets source mailbox
     * @param data mail session data
     * @return result of processing
     */
    public String setSourceMailbox(MailSessionData data) {
        for (int i=0; i<processSourceMailbox.size(); i++) {
            Filter filter = (Filter)processSourceMailbox.get(i);
            String response = filter.processSourceMailbox(data);
            if (response != null) {
                processMailSessionData(response, data);
                return response;
            }
        }
        return Filter.POSITIVE_RESPONSE;
    }

    /**
     * Sets destination mailbox
     * @param data mail session data
     * @param destinationMailbox
     * @return result of processing
     */
    public String setDestinationMailbox(MailSessionData data, Path destinationMailbox) {
        //data.getDestinationMailboxes().add(destinationMailbox);
        for (int i=0; i<processDestinationMailbox.size(); i++) {
            Filter filter = (Filter)processDestinationMailbox.get(i);
            String response = filter.processDestinationMailbox(data, destinationMailbox);
            if (response != null) {
                processMailSessionData(response, data);
                return response;
            }
        }
        return Filter.POSITIVE_RESPONSE;
    }

    /**
     * Does pre-load checks
     * @param data mail session data
     * @return result of processing
     */
    public String doPreLoadCheck(MailSessionData data) {
        for (int i=0; i<preloadCheck.size(); i++) {
            Filter filter = (Filter)preloadCheck.get(i);
            String response = filter.preLoadCheck(data);
            if (response != null) {
                processMailSessionData(response, data);
                return response;
            }
        }
        return Filter.POSITIVE_RESPONSE;
    }

    /**
     * Does post-load checks
     * @param data mail session data
     * @return result of processing
     */
    public String doPostLoadCheck(MailSessionData data) {
        for (int i=0; i<postloadCheck.size(); i++) {
            Filter filter = (Filter)postloadCheck.get(i);
            String response = filter.postLoadCheck(data);
            if (response != null) {
                processMailSessionData(response, data);
                return response;
            }
        }
        return Filter.POSITIVE_RESPONSE;
    }

    /**
     * Does processing mail session data. Particulary this method sets attribute "rejected" to true
     * if response is not {@link Filter#POSITIVE_RESPONSE}
     * @param response response
     * @param data mail session data
     */
    protected void processMailSessionData(String response, MailSessionData data) {
        if (!Filter.POSITIVE_RESPONSE.equals(response)) {
            data.setAttribute("rejected", "true");
        }
    }










}
