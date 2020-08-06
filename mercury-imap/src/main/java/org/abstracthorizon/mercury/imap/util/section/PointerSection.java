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
package org.abstracthorizon.mercury.imap.util.section;

import org.abstracthorizon.mercury.imap.util.Section;

/**
 * Section that has subsection
 *
 * @author Daniel Sendula
 */
public class PointerSection extends Section {

    /** Child element for this section */
    public Section child;

}
