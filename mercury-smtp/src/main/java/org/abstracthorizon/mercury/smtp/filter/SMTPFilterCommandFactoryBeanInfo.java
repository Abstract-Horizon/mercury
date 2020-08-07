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

import org.abstracthorizon.mercury.smtp.command.SMTPCommandFactoryBeanInfo;

/**
 * Bean info for {@link SMTPFilterCommandFactory} class
 *
 * @author Daniel Sendula
 */
public class SMTPFilterCommandFactoryBeanInfo extends SMTPCommandFactoryBeanInfo {

    /**
     * Constructor
     */
    public SMTPFilterCommandFactoryBeanInfo() {
        this(SMTPFilterCommandFactory.class);
    }

    /**
     * Constructor
     * @param cls class
     */
    protected SMTPFilterCommandFactoryBeanInfo(Class<?> cls) {
        super(cls);
    }

    /**
     * Init method
     */
    public void init() {
        super.init();

        addProperty("filters", "Filters", true, false);
    }
}
