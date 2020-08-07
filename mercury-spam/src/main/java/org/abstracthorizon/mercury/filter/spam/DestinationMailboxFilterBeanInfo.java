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
package org.abstracthorizon.mercury.filter.spam;

import org.abstracthorizon.pasulj.PasuljInfo;

/**
 * Bean info for {@link DestinationMailboxFilter} class
 *
 * @author Daniel Sendula
 */
public class DestinationMailboxFilterBeanInfo extends PasuljInfo {

    /**
     * Constructor
     */
    public DestinationMailboxFilterBeanInfo() {
        this(DestinationMailboxFilter.class);
    }

    /**
     * Constructor
     * @param cls class
     */
    protected DestinationMailboxFilterBeanInfo(Class<?> cls) {
        super(cls);
    }

    /**
     * Init method
     */
    public void init() {
        addProperty("spamSlowDown", "SPAM slow down time in millis.");
    }

}
