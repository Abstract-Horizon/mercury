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
package org.abstracthorizon.mercury.smtp.filter.quiet;

import org.abstracthorizon.mercury.smtp.filter.SMTPFilterCommandFactoryBeanInfo;

/**
 * Bean info for {@link SMTPQuietFilterCommandFactory} class
 *
 * @author Daniel Sendula
 */
public class SMTPQuietFilterCommandFactoryBeanInfo extends SMTPFilterCommandFactoryBeanInfo {

    /**
     * Constructor
     */
    public SMTPQuietFilterCommandFactoryBeanInfo() {
        this(SMTPQuietFilterCommandFactory.class);
    }

    /**
     * Constructor
     * @param cls class
     */
    protected SMTPQuietFilterCommandFactoryBeanInfo(Class<?> cls) {
        super(cls);
    }

    /**
     * Init method
     */
    public void init() {
        super.init();

        addProperty("maxFlushSpeed", "Maximum flush read speed in bytes per second");

    }

}
