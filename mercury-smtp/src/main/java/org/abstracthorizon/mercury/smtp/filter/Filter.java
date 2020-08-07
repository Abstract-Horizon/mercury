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
package org.abstracthorizon.mercury.smtp.filter;

import org.abstracthorizon.mercury.smtp.util.Path;

/**
 * This interface represents a filter for SMTP messages
 *
 * @author Daniel Sendula
 */
public interface Filter {

    /** Can this filter process source domain */
    int CAN_PROCESS_SOURCE_DOMAIN = 1;

    /** Can this filter process source mailbox */
    int CAN_PROCESS_SOURCE_MAILBOX = 2;

    /** Can this filter process destination mailbox */
    int CAN_PROCESS_DESTINATION_MAILBOX = 4;

    /** Can this filter do pre load checks */
    int CAN_DO_PRELOAD_CHECK = 8;

    /** Can this filter do post load checks */
    int CAN_DO_POSTLOAD_CHECK = 16;

    /** Positive response */
    String POSITIVE_RESPONSE = "OK";

    /**
     * This method returns features of implemented filter
     * @return bit array of features
     */
    int features();

    /**
     * Starts filter session. All internals should initialised to default values.
     *
     * @param data session data to be stored
     */
    void startSession(MailSessionData data);

    /**
     * This method is called when source domain is received.
     * Session data will contain only source domain.
     *
     * @return response
     */
    String processSourceDomain(MailSessionData data);

    /**
     * This method is called when source mailbox is received
     * Session data will contain source domain and source mailbox.
     *
     * @return response
     */
    String processSourceMailbox(MailSessionData data);

    /**
     * This method is called for each destination mailbox.
     * Session data will contain source domain and source mailbox
     * @param destinationMailbox destinationMailbox
     * @return response.
     */
    String processDestinationMailbox(MailSessionData data, Path destinationMailbox);

    /**
     * This method will be checked before message is received from the source.
     * Session data will contain all elements but message.
     * @return response
     */
    String preLoadCheck(MailSessionData data);

    /**
     * This method is called after message is received from the source.
     * Session data will contain all elements
     * @return response
     */
    String postLoadCheck(MailSessionData data);

    /**
     * Disposes all internal values connected to the session
     */
    void finishSession(MailSessionData data);

}
