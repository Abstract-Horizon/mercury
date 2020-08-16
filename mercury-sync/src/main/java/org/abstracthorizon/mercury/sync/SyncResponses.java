/*
 * Copyright (c) 2004-2019 Creative Sphere Limited.
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
package org.abstracthorizon.mercury.sync;

/**
 * List of responses
 *
 * @author Daniel Sendula
 */
public class SyncResponses {

    public static final SyncResponse READY_RESPONSE = new SyncResponse("READY", "Ready");

    public static final SyncResponse READY_TO_RECEIVE_RESPONSE = new SyncResponse("RECEIVING", "");

    public static final SyncResponse COMMAND_NOT_RECOGNISED_RESPONSE = new SyncResponse("ERROR", "Syntax error, command unrecognized");

    public static final SyncResponse SYNTAX_ERROR_RESPONSE = new SyncResponse("ERROR", "Syntax error in parameters or arguments");

    public static final SyncResponse GENERIC_ERROR_RESPONSE = new SyncResponse("ERROR", "Requested action aborted: local error in processing");

    public static final SyncResponse BYE_RESPONSE = new SyncResponse("BYE", "Bye");

    public static final SyncResponse FILE_DOES_NOT_EXIST = new SyncResponse("ERROR", "File does not exist");

    public static final SyncResponse PATH_DOES_NOT_EXIST = new SyncResponse("ERROR", "Path does not exist");

    public static final SyncResponse FILE_ALREADY_EXISTS = new SyncResponse("ERROR", "File already exists");

    public static final SyncResponse CANNOT_RENAME = new SyncResponse("ERROR", "File cannot be renamed");

    public static final SyncResponse CANNOT_DELETE = new SyncResponse("ERROR", "File cannot be deleted");

    public static final SyncResponse CANNOT_DELETE_EXISTING_FILE = new SyncResponse("ERROR", "Cannot delete existing file");

    public static final SyncResponse ONLY_LAST_PATH_ELEMENT_CAN_BE_CREATED = new SyncResponse("ERROR", "Only last path element can be created");

    public static final SyncResponse CANNOT_CREATE_PATH = new SyncResponse("ERROR", "Cannot create path");

    public static SyncResponse getCommandReadyResponse(String cmd, long now) {
        return new SyncResponse("READY", "Command " + cmd + " lasted " + (System.currentTimeMillis() - now) + "ms");
    }

    public static SyncResponse getSyntaxErrorResponse(String cmd, String msg) {
        return new SyncResponse("ERROR", "Syntax error for command " + cmd + ": " + msg);
    }
}
