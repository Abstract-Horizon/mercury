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
package org.abstracthorizon.mercury.sync.commands;

import static org.abstracthorizon.mercury.sync.commands.GetCommand.checkIfMaildirPath;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Scanner;

import org.abstracthorizon.mercury.common.command.CommandException;
import org.abstracthorizon.mercury.sync.SyncResponse;
import org.abstracthorizon.mercury.sync.SyncResponses;
import org.abstracthorizon.mercury.sync.SyncSession;
import org.abstracthorizon.mercury.sync.cachedir.CachedDir;
import org.abstracthorizon.mercury.sync.cachedir.CachedDirs;
import org.abstracthorizon.mercury.sync.exception.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sync PUT command.
 *
 * @author Daniel Sendula
 */
public class MoveCommand extends SyncCommand {

    protected static final Logger logger = LoggerFactory.getLogger(ListCommand.class);

    /**
     * Constructor
     */
    public MoveCommand() {
    }

    /**
     * Executed the command
     *
     * @param connection smtp session
     * @throws CommandException
     * @throws IOException
     * @throws ParserException
     */
    @Override
    public void execute(SyncSession connection, String command, String cmdLine) throws CommandException, IOException, ParserException {
        try {
            connection.setStreamDebug(false);
            long now = System.currentTimeMillis();

            try (Scanner scanner = new Scanner(cmdLine)) {
                scanner.useDelimiter(" ");

                if (!scanner.hasNextLong()) {
                    connection.sendResponse(SyncResponses.getSyntaxErrorResponse(command, "need from timestmap"));
                    return;
                }
                long lastModified = scanner.nextLong();

                String fromPath = scanner.next();
                String toPath = scanner.next();

                String[] fromPathElements = fromPath.split("/");
                if (fromPathElements.length < 2) {
                    connection.sendResponse(new SyncResponse("ERROR", "File does not exist, no enough path elements; " + fromPath));
                    return;
                }

                int e = fromPathElements.length - 1;
                if (!checkIfMaildirPath(fromPathElements[e - 1])) {
                    connection.sendResponse(new SyncResponse("ERROR", "File does not exist, not a maildir path; " + fromPath + ", " + fromPathElements[e - 1]));
                    return;
                }

                String fromFilename = fromPathElements[e];

                fromPath = fromPath.substring(0, fromPath.length() - fromFilename.length() - 1);

                CachedDirs fromCachedDirs = connection.getCachedDirs();

                CachedDir fromSelectedDirectory = fromCachedDirs.forPath(fromPath);
                File fromFile = fromSelectedDirectory.getFile(fromFilename);

                if (fromFile == null || !fromFile.exists()) {
                    connection.sendResponse(new SyncResponse("ERROR", "File does not exist; " + fromPath + "/" + fromFilename));
                    return;
                }

                String[] toPathElements = toPath.split("/");
                if (toPathElements.length < 2) {
                    connection.sendResponse(new SyncResponse("ERROR", "File does not exist, no enough path elements for 'to' path; " + toPathElements));
                    return;
                }

                e = toPathElements.length - 1;
                if (!checkIfMaildirPath(toPathElements[e - 1])) {
                    connection.sendResponse(new SyncResponse("ERROR", "File does not exist, 'to' path not a maildir path; " + toPathElements + ", " + toPathElements[e - 1]));
                    return;
                }

                String toFilename = toPathElements[e];

                toPath = toPath.substring(0, toPath.length() - toFilename.length() - 1);

                CachedDirs toCachedDirs = connection.getCachedDirs();

                CachedDir toSelectedDirectory = toCachedDirs.forPath(toPath);
                File toFile = toSelectedDirectory.getFile(toFilename);

                if (toFile == null || toFile.exists()) {
                    connection.sendResponse(SyncResponses.FILE_ALREADY_EXISTS);
                    return;
                }

                if (!fromFile.renameTo(toFile)) {
                    connection.sendResponse(SyncResponses.CANNOT_RENAME);
                    return;
                }

                toFile.setLastModified(lastModified);

                fromSelectedDirectory.deleteFile(fromFile);
                toSelectedDirectory.addFile(toFile);
            }

            connection.sendResponse(SyncResponses.getCommandReadyResponse("MOVE", now));
        } catch (FileNotFoundException notFound) {
            connection.sendResponse(SyncResponses.PATH_DOES_NOT_EXIST);
        } catch (IOException e) {
            // TODO should we drop the line here?
            // Scenario: data is late in the middle of e-mail
            // we have timeout + data arrive
            // -> loads of syntax errors and other side gives up
            // Solution: soon we have IO exception - we send response
            // and close the socket?
            if (!(e instanceof SocketTimeoutException)) {
                logger.error("Problem reading message", e);
            }
            connection.sendResponse(SyncResponses.GENERIC_ERROR_RESPONSE);
        } finally {
            connection.setStreamDebug(true);
        }
    }
}
