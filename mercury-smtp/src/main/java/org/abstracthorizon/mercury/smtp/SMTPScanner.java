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
package org.abstracthorizon.mercury.smtp;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.abstracthorizon.mercury.smtp.exception.ParserException;
import org.abstracthorizon.mercury.smtp.util.Path;

/**
 * A class implementing a lexical scanner for an IMAP server.
 *
 * @author Daniel Sendula
 */
public class SMTPScanner {

    /** Input stream */
    protected InputStream in;

    /** Buffer */
    protected char[] buffer;

    /** Current literal */
    protected boolean literal;

    /** Current pointer */
    protected int ptr = -1;

    /** END OF LINE is recognised */
    protected boolean eol = false;

    /**
     * Constructor
     * @param in input stream
     */
    public SMTPScanner(InputStream in) {
        this.in = in;
        in.mark(128);
    }

    /**
     * Resets EOL indicator
     */
    public void resetEOL() {
        eol = false;
    }

    protected boolean atom_char(char c) {
        if ((c >= 31) && (c != '(') && (c != ')') && (c != '{') && (c != ' ') && (c != '%') && (c != '*') && (c != '"') && (c != ']') && (c != '\\') && (c != '.') && (c != '>') && (c != '@')) { return true; }
        return false;
    }

    protected boolean astring_char(char c) {
        if (atom_char(c) || (c == ']')) { return true; }
        return false;
    }

    protected boolean text_char(char c) {
        if ((c != '\r') && (c != '\n')) { return true; }
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

    protected boolean alfa(char c) {
        return ((c >= 'A') && (c <= 'Z')) || ((c >= 'a') && (c <= 'z'));
    }

    protected boolean alfa_digit(char c) {
        return alfa(c) || digit(c);
    }

    protected boolean ldh(char c) {
        return ((c == '-') || alfa(c) || digit(c));
    }

    public char readChar() throws IOException {
        int i = in.read();
        if (i >= 0) {
            return (char)i;
        }
        throw new EOFException();
    }

    public void skip_line() throws IOException {
        if (eol) {
            eol = false;
            return;
        }
        char c = readChar();
        while (true) {
            while (c != '\r') {
                c = readChar();
            } // while
            c = readChar();
            if (c == '\n') {
                eol = true;
                return;
            }
        } // while
    }

    public void check_eol() throws IOException, ParserException {
        char c = readChar();
        if (c != '\r') { throw new ParserException("<CR>"); }
        c = readChar();
        if (c != '\n') { throw new ParserException("<LF>"); }
        eol = true;
    }

    public boolean is_char(char r) throws IOException {
        in.mark(1);
        char c = readChar();
        if (c == r) { return true; }
        in.reset();
        return false;
    }

    public boolean peek_char(char r) throws IOException {
        in.mark(1);
        char c = readChar();
        in.reset();
        if (c == r) { return true; }
        return false;
    }

    public boolean number(Number num) throws IOException {
        in.mark(10);
        int len = 0;
        num.number = 0;
        char c = readChar();
        if ((c < '0') && (c > '9')) {
            in.reset();
            return false;
        }
        num.number = num.number * 10 + (c - '0');
        len = len + 1;
        c = readChar();
        while ((c >= '0') && (c <= '9')) {
            num.number = num.number * 10 + (c - '0');
            len = len + 1;
            c = readChar();
        } // while
        in.reset();
        in.skip(len);
        return true;
    }

    public boolean nz_number(Number num) throws IOException {
        in.mark(10);
        int len = 0;
        num.number = 0;
        char c = readChar();
        if ((c < '1') || (c > '9')) {
            in.reset();
            return false;
        }
        num.number = num.number * 10 + (c - '0');
        len = len + 1;
        c = readChar();
        while ((c >= '0') && (c <= '9')) {
            num.number = num.number * 10 + (c - '0');
            len = len + 1;
            c = readChar();
        } // while
        in.reset();
        in.skip(len);
        return true;
    }

    public boolean keyword(String keyword) throws IOException {
        in.mark(keyword.length() + 1);
        ptr = 0;
        while (ptr < keyword.length()) {
            char c = readChar();
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
        char c = readChar();
        if (c != '"') {
            in.reset();
            return false;
        }
        c = readChar();
        while (text_char(c) && (c != '"')) {
            if (c == '\\') {
                c = readChar();
                if ((c == '\\') || (c == '"')) {
                    quoted.append(c);
                } else {
                    in.reset();
                    throw new ParserException("'" + c + "'");
                }
            } else {
                quoted.append(c);
            }
            c = readChar();
        } // while
        if (c != '"') {
            in.reset();
            throw new ParserException("'\"'");
        }
        return true;
    }

    public boolean atom(StringBuffer buffer) throws IOException, ParserException {
        in.mark(128);
        char c = readChar();
        if (!atom_char(c)) {
            in.reset();
            return false;
        }
        buffer.append(c);
        c = readChar();
        while (atom_char(c)) {
            buffer.append(c);
            c = readChar();
        }
        in.reset();
        in.skip(buffer.length());
        return true;
    }

    public boolean ldhStr(StringBuffer buffer) throws IOException, ParserException {
        in.mark(128);
        char c = readChar();
        char l = c;
        if (!alfa_digit(c)) {
            in.reset();
            return false;
        }
        buffer.append(c);
        l = c;
        c = readChar();
        while (ldh(c)) {
            buffer.append(c);
            l = c;
            c = readChar();
        }
        if (!alfa_digit(l)) { throw new ParserException("alfa-digit"); }
        in.reset();
        in.skip(buffer.length());
        return true;
    }

    public boolean path(Path path) throws IOException, ParserException {
        char c = readChar();
        if (c != '<') {
            in.reset();
            return false;
        }
        StringBuffer domain = new StringBuffer();
        if (atDomain(domain)) {
            path.addReturnPath(domain.toString());
            domain.delete(0, domain.length());
            c = readChar();
            while (c == ',') {
                if (!atDomain(domain)) {
                    in.reset();
                    throw new ParserException("atDomain");
                }
                path.addReturnPath(domain.toString());
                domain.delete(0, domain.length());
                c = readChar();
            }
            if (c != ':') {
                in.reset();
                throw new ParserException(":");
            }
        }
        StringBuffer local = new StringBuffer();
        if (!localPart(local)) {
            in.reset();
            throw new ParserException("local-part");
        }
        path.setMailbox(local.toString());
        if (!atDomain(domain)) {
            in.reset();
            throw new ParserException("atDomain");
        }
        path.setDomain(domain.toString());
        if (!is_char('>')) { throw new ParserException(">"); }
        return true;
    }

    public boolean atDomain(StringBuffer domain) throws IOException, ParserException {
        in.mark(1);
        char c = readChar();
        if (c != '@') {
            in.reset();
            return false;
        }
        return domain(domain);
    }

    public boolean domain(StringBuffer domain) throws IOException, ParserException {
        if (!ldhStr(domain)) { return false; }
        in.mark(1);
        char c = readChar();
        while (c == '.') {
            domain.append(c);
            StringBuffer sd = new StringBuffer();
            if (!ldhStr(sd)) { throw new ParserException("ldhStr"); }
            domain.append(sd);
            sd.delete(0, sd.length());
            in.mark(1);
            c = readChar();
        }
        in.reset();
        return true;
    }

    public boolean localPart(StringBuffer buffer) throws IOException, ParserException {
        if (quoted(buffer)) { return true; }
        if (!atom(buffer)) { return false; }
        in.mark(1);
        char c = readChar();
        while (c == '.') {
            buffer.append(c);
            StringBuffer p = new StringBuffer();
            if (!atom(p)) { throw new ParserException("atom"); }
            buffer.append(p);
            in.mark(1);
            c = readChar();
        }
        in.reset();
        return true;
    }
    
    public boolean base64(StringBuffer buffer) throws IOException, ParserException {
        in.mark(128);
        char c = readChar();
        if (!alfa(c) && !digit(c) && c != '+' && c != '/') {
            in.reset();
            return false;
        }
        buffer.append(c);
        c = readChar();
        while (alfa(c) || digit(c) || c == '+' || c== '/') {
            buffer.append(c);
            c = readChar();
        }
        in.reset();
        in.skip(buffer.length());
        return true;
    }


    public static class Number {

        public int number = 0;
    }

}
