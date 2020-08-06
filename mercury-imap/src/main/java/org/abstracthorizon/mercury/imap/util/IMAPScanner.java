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
package org.abstracthorizon.mercury.imap.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import javax.mail.Flags;
import org.abstracthorizon.mercury.imap.util.section.HeaderSection;
import org.abstracthorizon.mercury.imap.util.section.MimeSection;
import org.abstracthorizon.mercury.imap.util.section.MultipartSection;
import org.abstracthorizon.mercury.imap.util.section.PointerSection;
import org.abstracthorizon.mercury.imap.util.section.TextSection;


/**
 * A class implementing a lexical scanner for an IMAP server.
 *
 * @author Daniel Sendula
 */
public class IMAPScanner {

    /** Input stream */
    protected InputStream in;

    /** Output stream */
    protected OutputStream out;

    /** Buffer */
    protected char[] buffer;

    /** Is literal recognised */
    protected boolean literal;

    /** Current pointer */
    protected int ptr = -1;

    /** Have we reached EOL */
    protected boolean eol = false;

    /**
     * Constructor
     * @param in input stream
     * @param out output stream
     */
    public IMAPScanner(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
        in.mark(128);
    }

    protected boolean atom_char(char c) {
        if ((c >= 31) &&
            (c != '(') && (c != ')') && (c != '{') && (c != ' ') &&
            (c != '%') && (c != '*') && (c != '"') && (c != ']') &&
            (c != '\\')
           )
        {
            return true;
        }
        return false;
    }

    protected boolean astring_char(char c) {
        if (atom_char(c) || (c == ']')) {
            return true;
        }
        return false;
    }

    protected boolean text_char(char c) {
        if ((c != '\r') && (c != '\n')) {
            return true;
        }
        return false;
    }

    protected boolean digit_nz(char c) {
        return (c >= '1') && (c <= '9');
    }

    protected boolean digit(char c) {
        return (c >= '0') && (c <= '9');
    }

    protected boolean list_char(char c) {
        return atom_char(c) || (c == '%') || (c == '*') || (c == ']');
    }


    public void skip_line() throws IOException {
        if (eol) {
            eol = false;
            return;
        }
        try {
            int i = in.read();
            char c = (char)i;
            while (i >= 0) {
                while ((i >= 0) && (c != '\r')) {
                    i = in.read();
                    c = (char)i;
                } // while
                i = in.read();
                c = (char)i;
                if (c == '\n') {
                    return;
                }
            } // while
        } catch (IOException e) {
        }
    }

    public String peek_line() throws IOException {
        in.mark(2000);
        StringBuilder res = new StringBuilder();
        if (eol) {
            eol = false;
            in.reset();
            return res.toString();
        }
        try {
            int i = in.read();
            char c = (char)i;
            while (i >= 0) {
                while ((i >= 0) && (c != '\r')) {
                    res.append(c);
                    i = in.read();
                    c = (char)i;
                } // while
                res.append(c);
                i = in.read();
                c = (char)i;
                if (c == '\n') {
                    in.reset();
                    return res.toString();
                }
            } // while
        } catch (IOException e) {
        }
        in.reset();
        return res.toString();
    }

    public void check_eol() throws IOException, ParserException {
        try {
            char c = (char)in.read();
            if (c != '\r') {
                throw new ParserException("<CR>");
            }
            c = (char)in.read();
            if (c != '\n') {
                throw new ParserException("<LF>");
            }
        } catch (IOException e) {
        }
        eol = true;
    }

    public boolean is_char(char r) throws IOException {
        in.mark(1);
        char c = (char)in.read();
        if (c == r) {
            return true;
        }
        in.reset();
        return false;
    }

    public boolean peek_char(char r) throws IOException {
        in.mark(1);
        char c = (char)in.read();
        in.reset();
        if (c == r) {
            return true;
        }
        return false;
    }

    public boolean number(Number num) throws IOException {
        in.mark(10);
        int len = 0;
        num.number = 0;
        char c = (char)in.read();
        if ((c < '0') && (c > '9')) {
            in.reset();
            return false;
        }
        num.number = num.number*10 + (c-'0');
        len = len + 1;
        c = (char)in.read();
        while ((c >= '0') && (c <= '9')) {
            num.number = num.number*10 + (c-'0');
            len = len + 1;
            c = (char)in.read();
        } // while
        in.reset();
        in.skip(len);
        return true;
    }

    public boolean nz_number(Number num) throws IOException {
        in.mark(10);
        int len = 0;
        num.number = 0;
        char c = (char)in.read();
        if ((c < '1') || (c > '9')) {
            in.reset();
            return false;
        }
        num.number = num.number*10 + (c-'0');
        len = len + 1;
        c = (char)in.read();
        while ((c >= '0') && (c <= '9')) {
            num.number = num.number*10 + (c-'0');
            len = len + 1;
            c = (char)in.read();
        } // while
        in.reset();
        in.skip(len);
        return true;
    }

    public boolean tag(StringBuffer res) throws IOException {
        in.mark(128);
        int i = in.read();
        char c = (char)i;
        if ((i != -1) && (c != '+') && astring_char(c)) {
            res.append(c);
            i = in.read();
            c = (char)i;
            while ((i != -1) && (c != '+') && astring_char(c)) {
                res.append(c);
                i = in.read();
                c = (char)i;
            }
            in.reset();
            in.skip(res.length());
            return true;
        }
        in.reset();
        res.delete(0, res.length());
        return false;
    }

    public boolean keyword(String keyword) throws IOException {
        in.mark(keyword.length()+1);
        ptr = 0;
        while (ptr < keyword.length()) {
            char c = (char)in.read();
            if (Character.toUpperCase(c) != Character.toUpperCase(keyword.charAt(ptr))) {
                in.reset();

                return false;
            }
            ptr = ptr + 1;
        } // while
        return true;
    }

    public boolean quoted(StringBuffer quoted) throws IOException, ParserException {
        in.mark(512);
        char c = (char)in.read();
        if (c != '"') {
            in.reset();
            return false;
        }
        c = (char)in.read();
        while (text_char(c) && (c != '"')) {
            if (c == '\\') {
                c = (char)in.read();
                if ((c == '\\') || (c == '"')) {
                    quoted.append(c);
                } else {
                    in.reset();
                    throw new ParserException("'"+c+"'");
                }
            } else {
                quoted.append(c);
            }
            c = (char)in.read();
        } // while
        if (c != '"') {
            in.reset();
            throw new ParserException("'\"'");
        }
        return true;
    }

    public boolean literal(StringBuffer literal) throws IOException, ParserException {
        in.mark(10);
        char c = (char)in.read();
        if (c != '{') {
            in.reset();
            return false;
        }
        c = (char)in.read();
        if (!digit(c)) {
            in.reset();
            throw new ParserException("<digit>");
        }
        int len = (c - '0');
        c = (char)in.read();
        while (digit(c)) {
            len = len*10 + (c - '0');
            c = (char)in.read();
        } // while

        if (c != '}') {
            in.reset();
            throw new ParserException("'}'");
        }
        c = (char)in.read();
        if (c != '\r') {
            in.reset();
            throw new ParserException("<CR>");
        }
        c = (char)in.read();
        if (c != '\n') {
            in.reset();
            throw new ParserException("<LF>");
        }
        synchronized (out) {
            out.write("+ Ready for literal data\r\n".getBytes());
            out.flush();
        }
        byte[] buf = new byte[len];
        int size = 0;
        size = in.read(buf);
        while (size < buf.length) {
            int s = in.read(buf, size, buf.length-size);
            if (s >= 0) {
                size = size + s;
            } else {
                throw new ParserException(false, "Wrong literal - premature EOF reached!");
            }
        }
        literal.append(new String(buf));
        return true;
    }

    public long raw_literal() throws IOException, ParserException {
        in.mark(10);
        char c = (char)in.read();
        if (c != '{') {
            in.reset();
            return -1;
        }
        c = (char)in.read();
        if (!digit(c)) {
            throw new ParserException("<digit>");
        }
        long len = (c - '0');
        c = (char)in.read();
        while (digit(c)) {
            len = len*10 + (c - '0');
            c = (char)in.read();
        } // while

        if (c != '}') {
            throw new ParserException("'}'");
        }
        c = (char)in.read();
        if (c != '\r') {
            throw new ParserException("<CR>");
        }
        c = (char)in.read();
        if (c != '\n') {
            throw new ParserException("<LF>");
        }
        synchronized (out) {
            out.write("+ Ready for literal data\r\n".getBytes());
            out.flush();
        }
        return len;
    }


    public boolean string(StringBuffer buffer) throws IOException, ParserException {
        if (quoted(buffer) || literal(buffer)) {
            return true;
        }
        return false;
    }

    public boolean atom(StringBuffer buffer) throws IOException, ParserException {
        in.mark(128);
        char c = (char)in.read();
        if (!atom_char(c)) {
            in.reset();
            return false;
        }
        buffer.append(c);
        c = (char)in.read();
        while (atom_char(c)) {
            buffer.append(c);
            c = (char)in.read();
        }
        in.reset();
        in.skip(buffer.length());
        return true;
    }

    public boolean astring(StringBuffer buffer) throws IOException, ParserException {
        if (string(buffer)) {
            return true;
        }
        in.mark(128);
        char c = (char)in.read();
        if (!astring_char(c)) {
            in.reset();
            return false;
        }
        buffer.append(c);
        c = (char)in.read();
        while (astring_char(c)) {
            buffer.append(c);
            c = (char)in.read();
        }
        in.reset();
        in.skip(buffer.length());
        return true;
    }

    public boolean readBase64Line(StringBuffer line) throws IOException, ParserException {
        in.mark(128);
        char c = (char)in.read();
        while (((c >= 'A') && (c <= 'Z')) || ((c >= 'a') && (c <= 'z')) || ((c >= '0') && (c <= '9')) || (c == '+') || (c == '=') || (c == '/')) {
            line.append(c);
            c = (char)in.read();
        }
        if (c != '\r') {
            throw new ParserException("<CR>");
        }
        c = (char)in.read();
        if (c != '\n') {
            throw new ParserException("<LF>");
        }
        return true;
    }


    public boolean list_mailbox(StringBuffer buffer) throws IOException, ParserException {
        if (string(buffer)) {
            return true;
        }
        in.mark(128);
        char c = (char)in.read();
        if (!list_char(c)) {
            in.reset();
            return false;
        }
        buffer.append(c);
        c = (char)in.read();
        while (list_char(c)) {
            buffer.append(c);
            c = (char)in.read();
        } // while
        in.reset();
        in.skip(buffer.length());
        return true;
    }

    public boolean sequence_set(ComposedSequence sequence) throws IOException, ParserException {
        SimpleSequence seq = new SimpleSequence();
        if (!seq_range(seq)) {
            return false;
        }
        sequence.add(seq);
        while (is_char(',')) {
            seq = new SimpleSequence();
            if (!seq_range(seq)) {
                throw new ParserException("<seq_range>");
            }
            sequence.add(seq);
        }
        return true;
    }

    public boolean seq_range(SimpleSequence sequence) throws IOException, ParserException {
        Number num = new Number();
        if (is_char('*')) {
        } else {
            if (!number(num)) {
                return false;
            }
            sequence.setMin(num.number);
        }
        if (is_char(':')) {
            if (is_char('*')) {
            } else {
                if (!number(num)) {
                    throw new ParserException("<number>");
                }
                sequence.setMax(num.number);
            }
        } else {
            sequence.setMax(num.number);
        }
        return true;
    }

    public boolean section(PointerSection section) throws IOException, ParserException {
        in.mark(1);

        char c = (char)in.read();

        if (c != '[') {
            in.reset();
            return false;
        }
        if (section_msgtext(section)) {

        } else {
            int i = section_part(section);
            if (i == 0) {
                //in.reset();
                //throw new ParserException("Wrong body section");
            } else if (i == 2) {
                while (section.child != null) {
                    section = (PointerSection)section.child;
                }
                if (!section_text(section)) {
                    throw new ParserException("<section_text>");
                }
            } else {
                while (section.child != null) {
                    section = (PointerSection)section.child;
                }
                //section.child = new TextSection();
                // add TEXT implicitly. This might not be right but Java IMAP acts as it is!

                // section.child = new Section();

            }
        }
        c = (char)in.read();
        if (c != ']') {
            throw new ParserException("']'");
        }

        return true;
    }

    public int section_part(PointerSection section) throws IOException, ParserException {
        Number number = new Number();
        if (!nz_number(number)) {
            return 0;
        }
        MultipartSection sec = new MultipartSection();
        section.child = sec;
        sec.partNo = number.number;
        section = sec;
        in.mark(1);
        char c = (char)in.read();
        while (c == '.') {
            if (!nz_number(number)) {
                return 2;
                //throw new ParserException("Wrong section part - missing non zero number");
            }
            sec = new MultipartSection();
            section.child = sec;
            sec.partNo = number.number;
            section = sec;
            in.mark(1);
            c = (char)in.read();
        } // while
        in.reset();
        return 1;
    }

    public boolean section_text(PointerSection section) throws IOException, ParserException {
        if (section_msgtext(section)) {
            return true;
        } else if (keyword("MIME")) {
            section.child = new MimeSection();
            return true;
        }
        return false;
    }

    public boolean section_msgtext(PointerSection section) throws IOException, ParserException {
        if (keyword("HEADER")) {
            HeaderSection sec = new HeaderSection();
            section.child = sec;
            if (keyword(".FIELDS")) {
                sec.all = false;
                if (keyword(".NOT")) {
                    sec.not = true;
                }
                if (!is_char(' ')) {
                    throw new ParserException("<SP>");
                }
                sec.fields = new ArrayList<String>();
                if (!header_list(sec.fields)) {
                    throw new ParserException("<header_list>");
                }
            }
            return true;
        } else if (keyword("TEXT")) {
            section.child = new TextSection();
            return true;
        } else {
            return false;
        }
        //"HEADER" / "HEADER.FIELDS" [".NOT"] SP header-list /
        //                 "TEXT"        return false;
    }

    public boolean header_list(List<String> list) throws IOException, ParserException {
        in.mark(1);
        char c = (char)in.read();
        if (c != '(') {
            in.reset();
            return false;
        }
        StringBuffer s = new StringBuffer();
        if (!astring(s)) {
            throw new ParserException("<string>");
        }
        list.add(s.toString());
        s.delete(0, s.length());
        while (is_char(' ')) {
            if (!astring(s)) {
                throw new ParserException("<string>");
            }
            list.add(s.toString());
            s.delete(0, s.length());
        } // while
        if (!is_char(')')) {
            throw new ParserException("')'");
        }
        return true;
    }

    public boolean mailbox(StringBuffer mailbox) throws IOException, ParserException {
        if (keyword("INBOX")) {
            mailbox.append("INBOX");
            return true;
        } else {
            return astring(mailbox);
        }
    }

    public boolean flag(Flags flags) throws IOException, ParserException {
         if (keyword("\\Answered")) {
             flags.add(Flags.Flag.ANSWERED);
             return true;
         } else if (keyword("\\Flagged")) {
             flags.add(Flags.Flag.FLAGGED);
             return true;
         } else if (keyword("\\Deleted")) {
             flags.add(Flags.Flag.DELETED);
             return true;
         } else if (keyword("\\Seen")) {
             flags.add(Flags.Flag.SEEN);
             return true;
         } else if (keyword("\\Draft")) {
             flags.add(Flags.Flag.DRAFT);
             return true;
         } else if (is_char('\\')) {
             StringBuffer b = new StringBuffer();
             if (!atom(b)) {
                 throw new ParserException("<atom>");
             } else {
                 flags.add(b.toString());
             }
             return true;
         } else {
             StringBuffer b = new StringBuffer();
             if (atom(b)) {
                 flags.add(b.toString());
                 return true;
             }
         }
        return false;
    }

    public boolean flag_list(Flags fs) throws IOException, ParserException {
        //flag-list       = "(" [flag *(SP flag)] ")"
        if (!is_char('(')) {
            return false;
        }
        if (flag(fs)) {
           while (is_char(' ')) {
               if (!flag(fs)) {
                   throw new ParserException("<flag>");
               }
           }

        }

        if (!is_char(')')) {
            throw new ParserException("')'");
        }
        return true;
    }

    public boolean date_day_fixed(Number num) throws IOException, ParserException {
        if (is_char(' ')) {
            in.mark(1);
            char c = (char)in.read();
            if (digit(c)) {
                num.number = c-'0';
            } else {
                throw new ParserException("<digit>");
            }
        } else {
            in.mark(1);
            char c = (char)in.read();
            if (digit(c)) {
                num.number = c-'0';
                c = (char)in.read();
                if (digit(c)) {
                    num.number = num.number*10+c-'0';
                } else {
                    throw new ParserException("<digit>");
                }
            } else {
                return false;
            }
        }
        return true;
    }

    public boolean date_day(Number num) throws IOException, ParserException {
        in.mark(1);
        char c = (char)in.read();
        if (digit(c)) {
            num.number = c-'0';
            c = (char)in.read();
            if (digit(c)) {
                num.number = num.number*10+c-'0';
            }
            return true;
        } else {
            return false;
        }
    }


    public boolean date_month(Number num) throws IOException, ParserException {
        //date-month      = "Jan" / "Feb" / "Mar" / "Apr" / "May" / "Jun" /
        //                  "Jul" / "Aug" / "Sep" / "Oct" / "Nov" / "Dec"
        if (keyword("Jan")) {
            num.number = 1;
        } else if (keyword("Feb")) {
            num.number = 2;
        } else if (keyword("Mar")) {
            num.number = 3;
        } else if (keyword("Apr")) {
            num.number = 4;
        } else if (keyword("May")) {
            num.number = 5;
        } else if (keyword("Jun")) {
            num.number = 6;
        } else if (keyword("Jul")) {
            num.number = 7;
        } else if (keyword("Aug")) {
            num.number = 8;
        } else if (keyword("Sep")) {
            num.number = 9;
        } else if (keyword("Oct")) {
            num.number = 10;
        } else if (keyword("Nov")) {
            num.number = 11;
        } else if (keyword("Dec")) {
            num.number = 12;
        } else {
            return false;
        }
        return true;
    }

    public boolean four_digit(Number num) throws IOException, ParserException {
        in.mark(1);
        char c = (char)in.read();
        if (digit(c)) {
            num.number = c-'0';

            for (int i=0; i<3; i++) {
                c = (char)in.read();
                if (!digit(c)) {
                    throw new ParserException("<digit>");
                }
                num.number = num.number*10 + c-'0';
            } // for

            return true;
        } else {
            in.reset();
            return false;
        }

    }

    public boolean two_digit(Number num) throws IOException, ParserException {
        in.mark(1);
        char c = (char)in.read();
        if (digit(c)) {
            num.number = c-'0';
            c = (char)in.read();
            if (!digit(c)) {
                throw new ParserException("<digit>");
            }
            num.number = num.number*10 + c-'0';
            return true;
        } else {
            in.reset();
            return false;
        }
    }

    public boolean time(Number hour, Number min, Number sec) throws IOException, ParserException {
//      time            = 2DIGIT ":" 2DIGIT ":" 2DIGIT
        if (!two_digit(hour)) {
            return false;
        }
        if (!is_char(':')) {
            throw new ParserException("':'");
        }
        if (!two_digit(min)) {
            throw new ParserException("<2DIGIT>");
        }
        if (!is_char(':')) {
            throw new ParserException("':'");
        }
        if (!two_digit(sec)) {
            throw new ParserException("<2DIGIT>");
        }
        return true;
    }

    public boolean zone(StringBuffer zone) throws IOException, ParserException {
        in.mark(1);
        char c = (char)in.read();
        if (c == '+') {
            zone.append("GMT");
            zone.append('+');
        } else if (c == '-') {
            zone.append("GMT");
            zone.append('-');
        } else {
            in.reset();
            return false;
        }
        c = (char)in.read();
        if (digit(c)) {
            zone.append(c);

            for (int i=0; i<3; i++) {
                c = (char)in.read();
                if (!digit(c)) {
                    throw new ParserException("<digit>");
                }
                if (i == 1) {
                    zone.append(':');
                }
                zone.append(c);
            } // for

            return true;
        } else {
            throw new ParserException("<digit>");
        }
    }

    public boolean date_time(GregorianCalendar calendar) throws IOException, ParserException {
        //DQUOTE date-day-fixed "-" date-month "-" date-year SP time SP zone DQUOTE
        if (!is_char('"')) {
            return false;
        }
        Number day = new Number();
        if (!date_day_fixed(day)) {
            throw new ParserException("<date_day_fixed>");
        }
        if (!is_char('-')) {
            throw new ParserException("'-'");
        }
        Number month = new Number();
        if (!date_month(month)) {
            throw new ParserException("<date_month>");
        }
        if (!is_char('-')) {
            throw new ParserException("'-'");
        }
        Number year = new Number();
        if (!four_digit(year)) {
            throw new ParserException("<date_year>");
        }
        if (!is_char(' ')) {
            throw new ParserException("<SP>");
        }
        Number hour = new Number();
        Number min = new Number();
        Number sec = new Number();
        if (!time(hour, min, sec)) {
            throw new ParserException("<time>");
        }
        if (!is_char(' ')) {
            throw new ParserException("<SP>");
        }
        StringBuffer zone = new StringBuffer();
        if (!zone(zone)) {
            throw new ParserException("<time>");
        }
        TimeZone timeZone = TimeZone.getTimeZone(zone.toString());

        //GregorianCalendar calendar =
        //    new GregorianCalendar(year.number, month.number, day.number,
        //                          hour.number, min.number, sec.number);
        calendar.set(year.number, month.number, day.number,
                                  hour.number, min.number);
        calendar.set(Calendar.SECOND, sec.number);
        calendar.setTimeZone(timeZone);
        if (!is_char('"')) {
            throw new ParserException("'\"'");
        }

        return true;
    }

    public boolean date_text(GregorianCalendar calendar) throws IOException, ParserException {
        Number day = new Number();
        if (!date_day(day)) {
            throw new ParserException("<date_day_fixed>");
        }
        if (!is_char('-')) {
            throw new ParserException("'-'");
        }
        Number month = new Number();
        if (!date_month(month)) {
            throw new ParserException("<date_month>");
        }
        if (!is_char('-')) {
            throw new ParserException("'-'");
        }
        Number year = new Number();
        if (!four_digit(year)) {
            throw new ParserException("<date_year>");
        }
        calendar.set(year.number, month.number, day.number);
        return true;
    }

    public boolean date(GregorianCalendar calendar) throws IOException, ParserException {
        if (is_char('"')) {
            if (!date_text(calendar)) {
                throw new ParserException("<date_text>");
            }
            if (!is_char('"')) {
                throw new ParserException("'\"'");
            }
            return true;
        } else {
            return date_text(calendar);
        }
    }


    public static class Number {
        public int number = 0;
    }

}
