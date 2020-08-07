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
package org.abstracthorizon.mercury.smtp.command;

import org.abstracthorizon.danube.service.server.ServerConnectionHandlerBeanInfo;

/**
 * Bean info for {@link SMTPCommandFactory} class
 *
 * @author Daniel Sendula
 */
public class SMTPCommandFactoryBeanInfo extends ServerConnectionHandlerBeanInfo {

    /**
     * Constructor
     */
    public SMTPCommandFactoryBeanInfo() {
        this(SMTPCommandFactory.class);
    }

    /**
     * Constructor
     * @param cls class
     */
    protected SMTPCommandFactoryBeanInfo(Class<?> cls) {
        super(cls);
    }

    /**
     * Init method
     */
    public void init() {
        addProperty("inactivityTimeout", "Inactivity timeout");
        addProperty("commands", "Commands", true, false);
    }

}
