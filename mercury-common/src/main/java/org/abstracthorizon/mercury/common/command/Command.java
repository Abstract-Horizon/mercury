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
package org.abstracthorizon.mercury.common.command;

import org.abstracthorizon.danube.service.server.SocketConnection;

/**
 * An interface that represents an command.
 *
 * @author Daniel Sendula
 */
public interface Command {

    /**
     * Initialises command by setting the connection
     * @param connection connection
     * @throws CommandException
     */
    public void init(SocketConnection connection) throws CommandException;

}
