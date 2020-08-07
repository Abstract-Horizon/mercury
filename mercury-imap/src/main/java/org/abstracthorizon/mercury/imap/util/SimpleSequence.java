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
 * A class representing simple implementation of Sequence. It has
 * min and max, and maintains current pointer.
 *
 * @author Daniel Sendula
 */
public class SimpleSequence implements Sequence {

    /** Default maximum value */
    protected int min = Integer.MIN_VALUE;

    /** Default minimum value */
    protected int max = Integer.MAX_VALUE;

    /** Current pointer */
    protected int ptr = min;

    /**
     * Constructor
     */
    public SimpleSequence() {
    }

    /**
     * Minimum element in the sequence
     * @return minimum element in the sequence
     */
    public int getMin() {
        return min;
    }

    /**
     * Sets minimum element in the sequence
     * @param min minimum element in the sequence
     */
    public void setMin(int min) {
        this.min = min;
    }

    /**
     * Maximum element in the sequence
     * @return maximum element in the sequence
     */
    public int getMax() {
        return max;
    }

    /**
     * Sets maximum element in the sequence
     * @param max maximum element in the sequence
     */
    public void setMax(int max) {
        this.max = max;
    }

    /**
     * Sets the lower limit
     * @param lower lower limit
     */
    public void setLowerLimit(int lower) {
        if (lower > min) {
            min = lower;
        }
    }

    /**
     * Sets upper limit
     * @param upper upper limit
     */
    public void setUpperLimit(int upper) {
        if (upper < max) {
            max = upper;
        }
    }

    /**
     * Resets internal iterator
     */
    public void first() {
        ptr = min;
    }

    /**
     * Returns <code>true</code> if there are more elements in internal interator
     * @return <code>true</code> if there are more elements in internal interator
     */
    public boolean more() {
        return ptr <= max;
    }

    /**
     * Returns next element from the internal interator
     * @return next element from the internal interator
     */
    public int next() {
        int r = ptr;
        ptr = ptr + 1;
        return r;
    }

    /**
     * Returns <code>true</code> if two sequences are equal
     * @param o other sequence
     * @return <code>true</code> if two sequences are equal
     */
    public boolean equals(Object o) {
        if (o instanceof SimpleSequence) {
            SimpleSequence s = (SimpleSequence)o;
            if ((s.getMax() == max) && (s.getMin() == min)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns sum of min and max
     * @return sum of min and max
     */
    public int hashCode() {
        return min+max;
    }

    /**
     * Compares two sequences
     * @param o other sequence
     * @return -1, 0 and 1
     */
    public int compareTo(Sequence o) {
        Sequence s = (Sequence)o;
        int i = getMin()+getMax();
        int t = s.getMax()+s.getMin();
        if (i < t) {
            return -1;
        } else if (i > t) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Returns sequence as string
     * @return sequence as string
     */
    public String toString() {
        StringBuffer res = new StringBuffer();
        if (min == max) {
            res.append(min);
        } else {
            if (min == Integer.MIN_VALUE) {
                res.append('*');
            } else {
                res.append(min);
            }
            res.append(':');
            if (max == Integer.MAX_VALUE) {
                res.append('*');
            } else {
                res.append(max);
            }
        }
        return res.toString();
    }

    public boolean belongs(int i) {
        return (i >= min && i <= max);
    }

}
