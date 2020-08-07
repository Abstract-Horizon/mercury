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


/**
 * This simple object contains scoring for recognising spam messages.
 *
 *
 * @author Daniel Sendula
 */
public class SPAMScore {

    /** Mail session data attribute name for this object */
    public static final String ATTRIBUTE = "spam.score";

    /** Response if recognised as a spam */
    public static final String MAIL_RECOGNISED_AS_SPAM = "Mail recognised as a SPAM";

    /** Zero score */
    public static final int ZERO = 0;

    /** Maximum score - 100000 after which mail is recognised as SPAM */
    public static final int MAX = 100000;

    /** Current score */
    protected int score = 0;

    /** Should e-mail be rejected immediately or not. Default is <code>true</code> */
    protected boolean rejectImmediatelly = true;

    /**
     * Constructor
     */
    public SPAMScore() {
    }

    /**
     * Returns score
     * @return score
     */
    public int getScore() {
        return score;
    }

    /**
     * Returns if score is greater then max
     * @return <code>true</code> if score is greater then max
     */
    public boolean isSPAM() {
        return score > MAX;
    }

    /**
     * Clears score - sets it to {@link #ZERO} value
     */
    public void clear() {
        score = ZERO;
    }

    /**
     * Adds value to score
     * @param value value to be added
     * @return if recognised as SPAM and reject immediately being set
     */
    public boolean add(int value) {
        score = score + value;
        return rejectImmediatelly && (score >= MAX);
    }

    /**
     * Lower the score for given value
     * @param value value
     */
    public void lower(int value) {
        score = score - value;
    }

    /**
     * Returns score divided by 1000
     * @return score divided by 1000
     */
    public int getLevel() {
        return score / 1000;
    }

    /**
     * Sets reject immediately attribute
     * @param rejectImmediatelly reject immediately attribute
     */
    public void setRejectImmediatelly(boolean rejectImmediatelly) {
        this.rejectImmediatelly = rejectImmediatelly;
    }

    /**
     * Returns reject immediately attribute
     * @return reject immediately attribute
     */
    public boolean isRejectImmediatelly() {
        return rejectImmediatelly;
    }
}
