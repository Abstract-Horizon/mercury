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
package org.abstracthorizon.mercury.imap.util;



/**
 * An interface representing a sequence. Sequence is any ordered sequence of
 * integers.
 *
 * @author Daniel Sendula
 */
public interface Sequence extends Comparable<Sequence> {

    /**
     * Minimum element in the sequence
     * @return minimum element in the sequence
     */
    public int  getMin();

    /**
     * Maximum element in the sequence
     * @return maximum element in the sequence
     */
    public int  getMax();

    /**
     * Sets the lower limit
     * @param lower lower limit
     */
    public void setLowerLimit(int lower);

    /**
     * Sets upper limit
     * @param upper upper limit
     */
    public void setUpperLimit(int upper);

    /**
     * Resets internal iterator
     */
    public void first();

    /**
     * Returns <code>true</code> if there are more elements in internal interator
     * @return <code>true</code> if there are more elements in internal interator
     */
    public boolean more();

    /**
     * Returns next element from the internal interator
     * @return next element from the internal interator
     */
    public int next();

    /**
     * Returns <code>true</code> if element is in the sequence
     * @returns <code>true</code> if element is in the sequence
     */
    public boolean belongs(int i);
}
