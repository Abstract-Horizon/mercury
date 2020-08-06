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
package org.abstracthorizon.mercury.common.command;

import org.abstracthorizon.danube.service.server.SocketConnection;

/**
 * Abstract command.
 *
 * @author Daniel Sendula
 */
public abstract class AbstractCommand implements Command {

    /** Session */
    protected SocketConnection session;

    /** Command's mnemonic */
    protected String mnemonic;

    /**
     * Constructor
     * @param mnemonic mnemonic
     */
    public AbstractCommand(String mnemonic) {
        this.mnemonic = mnemonic;
    }

    /**
     * Returns session
     * @return
     */
    public SocketConnection getSession() {
        return session;
    }

    /**
     * Sets session
     * @param session session
     */
    protected void setSession(SocketConnection session) {
        this.session = session;
    } // setSession

    /**
     * Returns mnemonic
     * @return mnemonic
     */
    public String getMnemonic() {
        return mnemonic;
    }

    /**
     * Sets session
     * @param session
     */
    public void init(SocketConnection session) {
        setSession(session);
    }

}
