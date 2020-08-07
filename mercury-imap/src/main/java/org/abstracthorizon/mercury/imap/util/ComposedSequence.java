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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Composed sequence - an ordered sequence of sequences.
 *
 * @author Daniel Sendula
 */
public class ComposedSequence implements Sequence {

    /** Sequences */
    protected List<Sequence> sequences = new ArrayList<Sequence>();

    /** Current pointer */
    protected int ptr = 0;

    /**
     * Constructor
     */
    public ComposedSequence() {
    }

    /**
     * Adds new sequence to composed sequence
     * @param s sequence to be added
     */
    public void add(Sequence s) {
        if (!sequences.contains(s)) {
            sequences.add(s);
        }
        Collections.sort(sequences);
    }

    /**
     * Removes sequence from list of sequences
     * @param s sequence to be removed
     */
    public void remove(Sequence s) {
        sequences.remove(s);
    }

    /**
     * Clears list of sequences
     */
    public void clear() {
        sequences.clear();
    }

    /**
     * Returns <code>true</code> if sequence exists
     * @param s sequence
     * @return <code>true</code> if sequence exists
     */
    public boolean contains(Sequence s) {
        return sequences.contains(s);
    }

    /**
     * Returns minimum number from the sequence
     *
     * @return minimum number from the sequence
     */
    public int getMin() {
        if (sequences.size() > 0) {
            Sequence s = (Sequence) sequences.get(0);
            return s.getMin();
        }
        return 0;
    }

    /**
     * Returns maximum number from the sequence
     *
     * @return maximum number from the sequence
     */
    public int getMax() {
        if (sequences.size() > 0) {
            Sequence s = (Sequence) sequences.get(sequences.size() - 1);
            return s.getMax();
        }
        return 0;
    }

    /**
     * Sets the lower limit in the sequence removing subsequences below that point
     * @param lower lower limit
     */
    public void setLowerLimit(int lower) {
        int i = 0;
        boolean ok = true;
        while ((i < sequences.size()) && ok) {
            Sequence s = (Sequence) sequences.get(i);
            int min = s.getMin();
            int max = s.getMax();
            if (lower > max) {
                sequences.remove(i);
            } else if (lower > min) {
                s.setLowerLimit(lower);
                i = i + 1;
            } else if (lower <= min) {
                ok = false;
            }
        } // while
    }

    /**
     * Sets the upper limit in the sequence removing subsequences above that point
     * @param lower upper limit
     */
    public void setUpperLimit(int upper) {
        int i = sequences.size();
        boolean ok = true;
        while ((i > 0) && ok) {
            i = i - 1;
            Sequence s = (Sequence) sequences.get(i);
            int min = s.getMin();
            int max = s.getMax();
            if (upper < min) {
                sequences.remove(i);
            } else if (upper < max) {
                s.setUpperLimit(upper);
                i = i - 1;
            } else if (upper >= max) {
                ok = false;
            }
        } // while
    }

    /**
     * Sets pointer to first element in sequences
     */
    public void first() {
        int ptr = 0;
        if (sequences.size() > 0) {
            ((Sequence) sequences.get(ptr)).first();
        }
    }

    /**
     * Returns <code>true</code> if there are more elements in the sequence
     * @return <code>true</code> if there are more elements in the sequence
     */
    public boolean more() {
        if (ptr == sequences.size()) {
            return false;
        }
        Sequence s = (Sequence) sequences.get(ptr);
        if (s.more()) {
            return true;
        }

        ptr = ptr + 1;
        if (ptr == sequences.size()) {
            return false;
        }
        ((Sequence) sequences.get(ptr)).first();
        return true;
    }

    /**
     * Returns next element from the sequence
     * @return next element from the sequence
     */
    public int next() {
        if (ptr == sequences.size()) {
            return 0;
        }
        Sequence s = (Sequence) sequences.get(ptr);
        return s.next();
    }

    /**
     * Returns sequences list iterator
     * @return sequences list iterator
     */
    public Iterator<Sequence> getSequencesAsIterator() {
        return sequences.iterator();
    }

    /**
     * Creates a string representing all sequences
     * @return a string representing all sequences
     */
    public String toString() {
        StringBuffer res = new StringBuffer();
        if (sequences.size() > 0) {
            for (int i = 0; i < sequences.size(); i++) {
                if (i > 0) {
                    res.append(',');
                }
                res.append(sequences.get(i).toString());
            }
        }
        return res.toString();
    }

    /**
     * Compares two sequences returning -1, 0 or 1
     * @param other other sequence
     * @return -1, 0 or 1
     */
    public int compareTo(Sequence other) {
        Sequence s = (Sequence) other;
        int i = getMin() + getMax();
        int t = s.getMax() + s.getMin();
        if (i < t) {
            return -1;
        } else if (i > t) {
            return 1;
        } else {
            return 0;
        }
    }

    public boolean belongs(int i) {
        for (Sequence s : sequences) {
            if (s.belongs(i)) {
                return true;
            }
        }
        return false; 
    }
}
