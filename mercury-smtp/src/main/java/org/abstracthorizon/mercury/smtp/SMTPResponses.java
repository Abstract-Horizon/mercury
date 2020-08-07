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
package org.abstracthorizon.mercury.smtp;

/**
 * List of responses
 *
 * @author Daniel Sendula
 */
public class SMTPResponses {

    /** QUIT response */
    public static final SMTPResponse QUIT_RESPONSE = new SMTPResponse(221, "Service closing transmission channel");

    /** OK response */
    public static final SMTPResponse OK_RESPONSE = new SMTPResponse(250, "OK");

    /** Start data response */
    public static final SMTPResponse START_DATA_RESPONSE = new SMTPResponse(354, "Start mail input; end with <CRLF>.<CRLF>");

    /** Generic error response */
    public static final SMTPResponse GENERIC_ERROR_RESPONSE = new SMTPResponse(451, "Requested action aborted: local error in processing");

    /** Shutting down response */
    public static final SMTPResponse SHUTTING_DOWN_RESPONSE = new SMTPResponse(421, "Service not available, closing transmission channel");

    /** Command not recognised response */
    public static final SMTPResponse COMMAND_NOT_RECOGNISED_RESPONSE = new SMTPResponse(500, "Syntax error, command unrecognized");

    /** Syntax error response */
    public static final SMTPResponse SYNTAX_ERROR_RESPONSE = new SMTPResponse(501, "Syntax error in parameters or arguments");

    /** Command not implemented response */
    public static final SMTPResponse COMMAND_NOT_IMPLEMENTED_RESPONSE = new SMTPResponse(502, "Command not implemented");

    /** Bad sequence of commands response */
    public static final SMTPResponse BAD_SEQUENCE_OF_COMMANDS_RESPONSE = new SMTPResponse(503, "Bad sequence of commands");

    /** Mailbox unavailable response */
    public static final SMTPResponse MAILBOX_UNAVAILABLE_RESPONSE = new SMTPResponse(550, "Requested action not taken: mailbox unavailable");

    /** Authentication Succeeded */
    public static final SMTPResponse AUTHENTICATION_SUCCEEDED = new SMTPResponse(235, "Authentication Succeeded");

    /** Authentication Required */
    public static final SMTPResponse AUTHENTICATION_REQUIRED = new SMTPResponse(530, "Authentication Required");

    /** Authentication Credentials Invalid */
    public static final SMTPResponse AUTHENTICATION_CREDENTIALS_INVALID = new SMTPResponse(535, "Authentication Credentials Invalid");

}
