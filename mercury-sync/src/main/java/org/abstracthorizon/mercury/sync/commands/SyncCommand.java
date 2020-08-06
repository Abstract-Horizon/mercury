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

import java.io.IOException;

import org.abstracthorizon.mercury.common.command.CommandException;
import org.abstracthorizon.mercury.sync.SyncSession;
import org.abstracthorizon.mercury.sync.exception.ParserException;

/**
 * Base Sync command
 *
 * @author Daniel Sendula
 */
public abstract class SyncCommand {

    /**
     * Constructor
     */
    public SyncCommand() {
    }

    /**
     * Executed the command
     * @param connection smtp session
     * @throws CommandException
     * @throws IOException
     * @throws ParserException
     */
    public abstract void execute(SyncSession connection, String command, String cmdLine) throws CommandException, IOException, ParserException;

    protected int trim(String cmdLine, int i) {
        while (i < cmdLine.length() && cmdLine.charAt(i) == ' ') {
            i++;
        }
        return i;
    }

    protected long parseLong(String cmdLine) {
        int i = trim(cmdLine, 0);
        long res = 0;
        boolean minus = (i < cmdLine.length() && cmdLine.charAt(i) == '-');
        if (minus) {
            i++;
        }

        while (i < cmdLine.length() && Character.isDigit(cmdLine.charAt(i))) {
            res = res * 10 + cmdLine.charAt(i) - '0';
            i++;
        }

        if (minus) {
            res = -res;
        }

        return res;
    }
}
