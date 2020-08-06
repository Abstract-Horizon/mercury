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
package org.abstracthorizon.mercury.common.util;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;

/**
 * Class that represents RFC's date
 *
 * @author Daniel Sendula
 */
public class RFCDate implements Serializable {

    /** Date format */
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");

    /** Date string */
    protected transient String dateString;

    /** Day */
    protected int day;

    /** Month */
    protected int month;

    /** Year */
    protected int year;

    /** Hour */
    protected int hour;

    /** Minute\*/
    protected int minute;

    /** Second */
    protected int second;

    /** Timezone */
    protected int timezone;

    /** Timezone Id */
    protected String timezoneId;

    /** Is valid date or not */
    protected boolean valid = false;

    protected transient int p;
    protected transient int l;

    /**
     * Constructor
     * @param s date
     */
    public RFCDate(String s) {
        this.dateString = s;
        p = 0;
        l = s.length();
        valid = parse();
    }

    /**
     * Returns is date valid
     * @return is date valid
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Trims date string
     * @return trims date string
     */
    protected boolean trim() {
        if (p < l) {
            char c = dateString.charAt(p);
            if (Character.isWhitespace(c)) {
                p = p + 1;
                if (p < l) {
                    c = dateString.charAt(p);
                    while ((p < l) && Character.isWhitespace(c)) {
                        p = p + 1;
                        if (p < l) {
                            c = dateString.charAt(p);
                        }
                    }
                }

            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Parses day of week
     * @return <code>true</code>  if valid
     */
    protected boolean parseDayOfWeek() {
        if (p+3 <= l) {
            String d = dateString.substring(p, p+3);
            if ("Mon".equals(d) || "Tue".equals(d) || "Wed".equals(d)
                    || "Thu".equals(d) || "Fri".equals(d)
                    || "Sat".equals(d) || "Sun".equals(d)) {
                p = p + 3;
                return true;
            }
        }
        return false;
    }

    /**
     * Checks next character is given character
     * @param ch character to be checked with
     * @return <code>true</code> if next character is given character
     */
    protected boolean character(char ch) {
        if (p < l) {
            if (dateString.charAt(p) == ch) {
                p = p + 1;
                return true;
            }
        }
        return false;
    }

    /**
     * Returns digit (oc next character) or -1
     * @return digit (oc next character) or -1
     */
    protected int digit() {
        if (p < l) {
            char c = dateString.charAt(p);
            if (Character.isDigit(c)) {
                p = p + 1;
                return c - '0';
            }
        }
        return -1;
    }

    /**
     * Parses day
     * @return <code>true</code> if valid
     */
    protected boolean parseDay() {
        day = digit();
        if (day < 0) {
            return false;
        }
        int d = digit();
        if (d >= 0) {
            day = day * 10 + d;
        }
        return true;
    }

    /**
     * Parses month
     * @return <code>true</code> if valid
     */
    protected boolean parseMonth() {
        if (p+3 <= l) {
            String d = dateString.substring(p, p+3);
            if (d.equals("Jan")) {
                month = 1;
                p = p + 3;
                return true;
            } else if (d.equals("Feb")) {
                month = 2;
                p = p + 3;
                return true;
            } else if (d.equals("Mar")) {
                month = 3;
                p = p + 3;
                return true;
            } else if (d.equals("Apr")) {
                month = 4;
                p = p + 3;
                return true;
            } else if (d.equals("May")) {
                month = 5;
                p = p + 3;
                return true;
            } else if (d.equals("Jun")) {
                month = 6;
                p = p + 3;
                return true;
            } else if (d.equals("Jul")) {
                month = 7;
                p = p + 3;
                return true;
            } else if (d.equals("Aug")) {
                month = 8;
                p = p + 3;
                return true;
            } else if (d.equals("Sep")) {
                month = 9;
                p = p + 3;
                return true;
            } else if (d.equals("Oct")) {
                month = 10;
                p = p + 3;
                return true;
            } else if (d.equals("Nov")) {
                month = 11;
                p = p + 3;
                return true;
            } else if (d.equals("Dec")) {
                month = 12;
                p = p + 3;
                return true;
            }
        }
        return false;
    }

    /**
     * Parses year
     * @return <code>true</code> if valid
     */
    protected boolean parseYear() {
        year = digit();
        if (year < 0) {
            return false;
        }

        int y = digit();
        if (y < 0) {
            return false;
        }

        year = year * 10 + y;

        y = digit();
        if (y >= 0) {
            year = year * 10 + y;
            y = digit();
            if (y < 0) {
                return false;
            }
            year = year * 10 + y;
        } else {
            if (year < 60) {
                year = year  + 2000;
            } else {
                year = year + 1900;
            }
        }

        return true;
    }

    /**
     * Parses hour
     * @return <code>true</code> if valid
     */
    protected boolean parseHour() {
        hour = digit();
        if (hour < 0) {
            return false;
        }
        int h = digit();
        if (h < 0) {
            return false;
        }
        hour = hour * 10 + h;
        return true;
    }

    /**
     * Parses minute
     * @return <code>true</code> if valid
     */
    protected boolean parseMinute() {
        minute = digit();
        if (minute < 0) {
            return false;
        }
        int m = digit();
        if (m < 0) {
            return false;
        }
        minute = minute * 10 + m;
        return true;
    }

    /**
     * Parses second
     * @return <code>true</code> if valid
     */
    protected boolean parseSecond() {
        second = digit();
        if (second < 0) {
            return false;
        }
        int ss = digit();
        if (ss < 0) {
            return false;
        }
        second = second * 10 + ss;
        return true;
    }

    /**
     * Parses timezone
     * @return <code>true</code> if valid
     */
    protected boolean parseTimeZone() {
        if (p < l) {
            boolean negative = false;
            char c = dateString.charAt(p);
            if (c == '+') {
                p = p + 1;
            } else if (c == '-') {
                negative = true;
                p = p + 1;
            } else {
                if ((p + 2 <= l) && ("UT".equals(dateString.substring(p, p + 2)))) {
                    timezoneId = "UT";
                } else if (p + 3 <= l) {
                    String tz = dateString.substring(p, p + 3);
                    if ("GMT".equals(tz)
                            || "EST".equals(tz) || "EDT".equals(tz)
                            || "CST".equals(tz) ||  "CDT".equals(tz)
                            || "MST".equals(tz) ||  "MDT".equals(tz)
                            || "PST".equals(tz) ||  "PDT".equals(tz)
                            ) {
                        timezoneId = tz;
                    }
                }
                if (timezoneId != null) {
                    return true;
                }
                return false;
            }
            if (p + 4 <= l) {

                timezone = digit();
                if (timezone < 0) {
                    return false;
                }
                int t = digit();
                if (t < 0) {
                    return false;
                }
                timezone = timezone * 10 + t;
                t = digit();
                if (t < 0) {
                    return false;
                }
                timezone = timezone * 10 + t;
                t = digit();
                if (t < 0) {
                    return false;
                }
                timezone = timezone * 10 + t;
                if (negative) {
                    timezone = -timezone;
                }
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    /**
     * Parses date
     * @return <code>true</code> if valid
     */
    protected boolean parse() {
        trim();
        if (parseDayOfWeek()) {
            if (!character(',')) {
                return false;
            }
            if (!trim()) {
                return false;
            }
        }
        if (!parseDay()) {
            return false;
        }
        if (!trim()) {
            return false;
        }
        if (!parseMonth()) {
            return false;
        }
        if (!trim()) {
            return false;
        }
        if (!parseYear()) {
            return false;
        }
        if (!trim()) {
            return false;
        }
        if (!parseHour()) {
            return false;
        }
        if (!character(':')) {
            return false;
        }
        if (!parseMinute()) {
            return false;
        }
        if (character(':')) {
            if (!parseSecond()) {
                return false;
            }
        }
        if (!trim()) {
            return false;
        }
        if (!parseTimeZone()) {
            return false;
        }
        return true;
    }

    /**
     * Returns date
     * @return date
     */
    public Date getDate() {
        GregorianCalendar calendar = new GregorianCalendar(year, month-1, day, hour, minute, second);
        int rawoffset = timezone/100*60*60*1000+(timezone % 100)*60*1000;
        SimpleTimeZone zone = new SimpleTimeZone(rawoffset, "");
        calendar.setTimeZone(zone);
        //calendar.set(Calendar.ZONE_OFFSET, timezone);
        return calendar.getTime();
    }

    /**
     * Returns calendar
     * @return calendar
     */
    public Calendar getCalendar() {
        GregorianCalendar calendar = new GregorianCalendar(year, month-1, day, hour, minute, second);
        int rawoffset = timezone/100*60*60*1000+(timezone % 100)*60*1000;
        SimpleTimeZone zone = new SimpleTimeZone(rawoffset, "");
        calendar.setTimeZone(zone);
        return calendar;
    }

    /**
     * Validates given date string
     * @param s date string
     * @return <code>true</code> if valid
     */
    public static boolean validate(String s) {
        RFCDate pd = new RFCDate(s);
        return pd.isValid();
    }



    public static void main(String[] args) throws Exception {
        System.out.println(validate("Fri, 2 Apr 2004 00:10:10 +0300"));
        //test("Mon, 6 Jun 2005 10:24:01 +0000");
        //test("5 Jun 2005 10:24:01 +0200");
        //test("25 Jun 05 10:24:01 +0930");
        //test("25 Jun 2005 10:24 -1000");
        //test("25 Jun 05 10:24:01 +0300");
        //test("Sat, 25 Jun 2005 10:24 +0200");
        //test("Sat,  25  Jun  2005  10:24:01  +0100");
    }

    public static void test(String d) {
        if (validate(d)) {
            RFCDate date = new RFCDate(d);
            if (d.equals(date)) {
                System.out.println(d + " is ok");
            } else {
                System.out.println(d + " is different to " + date);
            }
        } else {
            System.out.println(d + " is not valid date");
        }
    }
}
