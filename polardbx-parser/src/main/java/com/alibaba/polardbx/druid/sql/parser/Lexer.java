/*
 * Copyright 1999-2017 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.polardbx.druid.sql.parser;

import com.alibaba.polardbx.druid.DbType;
import com.alibaba.polardbx.druid.sql.ast.SQLObject;
import com.alibaba.polardbx.druid.sql.ast.expr.SQLNumberExpr;
import com.alibaba.polardbx.druid.sql.dialect.mysql.parser.MySqlLexer;
import com.alibaba.polardbx.druid.util.FnvHash;
import com.alibaba.polardbx.druid.util.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

import static com.alibaba.polardbx.druid.sql.parser.LayoutCharacters.EOI;
import static com.alibaba.polardbx.druid.sql.parser.SQLParserFeature.KeepComments;
import static com.alibaba.polardbx.druid.sql.parser.SQLParserFeature.KeepSourceLocation;
import static com.alibaba.polardbx.druid.sql.parser.SQLParserFeature.OptimizedForParameterized;
import static com.alibaba.polardbx.druid.sql.parser.SQLParserFeature.SkipComments;
import static com.alibaba.polardbx.druid.sql.parser.Token.COLON;
import static com.alibaba.polardbx.druid.sql.parser.Token.COLONCOLON;
import static com.alibaba.polardbx.druid.sql.parser.Token.COLONEQ;
import static com.alibaba.polardbx.druid.sql.parser.Token.COMMA;
import static com.alibaba.polardbx.druid.sql.parser.Token.DOT;
import static com.alibaba.polardbx.druid.sql.parser.Token.EOF;
import static com.alibaba.polardbx.druid.sql.parser.Token.EQ;
import static com.alibaba.polardbx.druid.sql.parser.Token.EQEQ;
import static com.alibaba.polardbx.druid.sql.parser.Token.EQGT;
import static com.alibaba.polardbx.druid.sql.parser.Token.ERROR;
import static com.alibaba.polardbx.druid.sql.parser.Token.IDENTIFIER;
import static com.alibaba.polardbx.druid.sql.parser.Token.LBRACE;
import static com.alibaba.polardbx.druid.sql.parser.Token.LBRACKET;
import static com.alibaba.polardbx.druid.sql.parser.Token.LITERAL_ALIAS;
import static com.alibaba.polardbx.druid.sql.parser.Token.LITERAL_CHARS;
import static com.alibaba.polardbx.druid.sql.parser.Token.LITERAL_PATH;
import static com.alibaba.polardbx.druid.sql.parser.Token.LPAREN;
import static com.alibaba.polardbx.druid.sql.parser.Token.RBRACE;
import static com.alibaba.polardbx.druid.sql.parser.Token.RBRACKET;
import static com.alibaba.polardbx.druid.sql.parser.Token.RPAREN;
import static com.alibaba.polardbx.druid.util.MySqlUtils.GBK;
import static com.alibaba.polardbx.druid.util.MySqlUtils.LATIN1;
import static com.alibaba.polardbx.druid.util.MySqlUtils.UTF8;

/**
 * @author wenshao [szujobs@hotmail.com]
 */
public class Lexer {
    protected static SymbolTable symbols_l2 = new SymbolTable(512);

    protected int features = 0; //SQLParserFeature.of(SQLParserFeature.EnableSQLBinaryOpExprGroup);
    protected TimeZone timeZone;
    public final ByteString text;
    protected int pos;
    protected int mark;
    protected int numberSale;
    protected boolean numberExp;

    protected char ch;

    protected byte[] buf;
    protected int bufPos;

    protected Token token;

    protected Keywords keywods = Keywords.DEFAULT_KEYWORDS;

    protected String stringVal;
    protected long hash_lower; // fnv1a_64
    protected long hash;
    protected byte[] binaryVal;

    protected int commentCount = 0;
    protected List<String> comments = null;
    protected boolean skipComment = true;
    private SavePoint savePoint = null;

    /*
     * anti sql injection
     */
    private boolean allowComment = true;
    private int varIndex = -1;
    protected CommentHandler commentHandler;
    protected boolean endOfComment = false;
    protected boolean keepComments = false;
    protected int line = 0;
    protected int lines = 0;
    protected DbType dbType;

    protected boolean optimizedForParameterized = false;
    protected boolean keepSourceLocaltion = false;

    protected int startPos;
    protected int posLine;
    protected int posColumn;

    private boolean lexSingleColon = false;

    protected final Charset charset;

    public Lexer(ByteString input) {
        this(input, (CommentHandler) null);
    }

//    public Lexer(String input, DbType dbType){
//        this(input, (CommentHandler) null, dbType);
//    }

    public Lexer(ByteString input, CommentHandler commentHandler) {
        this(input, true);
        this.commentHandler = commentHandler;
    }

    public Lexer(ByteString input, CommentHandler commentHandler, DbType dbType) {
        this(input, true);
        this.commentHandler = commentHandler;
        this.dbType = dbType;
    }

    public boolean isKeepComments() {
        return keepComments;
    }

    public void setKeepComments(boolean keepComments) {
        this.keepComments = keepComments;
    }

    public CommentHandler getCommentHandler() {
        return commentHandler;
    }

    public void setCommentHandler(CommentHandler commentHandler) {
        this.commentHandler = commentHandler;
    }

    public final char charAt(int index) {
        if (index >= text.length()) {
            return EOI;
        }

        return text.charAt(index);
    }

    public final String addSymbol() {
        return subString(mark, bufPos);
    }

    public final String subString(int offset, int count) {
        return text.substring(offset, offset + count);
    }

    public final char[] sub_chars(int offset, int count) {
        return subString(offset, count).toCharArray();
    }

    protected void initBuff(int size) {
        if (buf == null) {
            if (size < 32) {
                buf = new byte[32];
            } else {
                buf = new byte[size + 32];
            }
        } else if (buf.length < size) {
            buf = Arrays.copyOf(buf, size);
        }
    }

    public void arraycopy(int srcPos, byte[] dest, int destPos, int length) {
        text.getBytes(srcPos, srcPos + length, dest, destPos);
    }

    public boolean isAllowComment() {
        return allowComment;
    }

    public void setAllowComment(boolean allowComment) {
        this.allowComment = allowComment;
    }

    public int nextVarIndex() {
        return ++varIndex;
    }

    public static class SavePoint {
        int bp;
        int sp;
        int np;
        char ch;
        long hash;
        long hash_lower;
        public Token token;
        String stringVal;
    }

    public Keywords getKeywods() {
        return keywods;
    }

    public SavePoint mark() {
        SavePoint savePoint = new SavePoint();
        savePoint.bp = pos;
        savePoint.sp = bufPos;
        savePoint.np = mark;
        savePoint.ch = ch;
        savePoint.token = token;
        savePoint.stringVal = stringVal;
        savePoint.hash = hash;
        savePoint.hash_lower = hash_lower;
        return this.savePoint = savePoint;
    }

    public SavePoint getMark() {
        return this.savePoint;
    }

    public void reset(SavePoint savePoint) {
        this.pos = savePoint.bp;
        this.bufPos = savePoint.sp;
        this.mark = savePoint.np;
        this.ch = savePoint.ch;
        this.token = savePoint.token;
        this.stringVal = savePoint.stringVal;
        this.hash = savePoint.hash;
        this.hash_lower = savePoint.hash_lower;
    }

    public void reset() {
        this.reset(this.savePoint);
    }

    public void reset(int pos) {
        this.pos = pos;
        this.ch = charAt(pos);
    }

    public Lexer(ByteString input, boolean skipComment) {
        this.skipComment = skipComment;

        this.text = input;
        this.charset = input.getCharset();

        this.pos = 0;
        ch = charAt(pos);
        while (isZeroWidthSpace(ch) || ch == '\n') {
            if (ch == '\n') {
                line++;
            }
            if (isZeroWidthSpace(ch)) {
                pos += nchar - 1;
            }
            ch = charAt(++pos);
        }
    }

    protected final void scanChar() {
        ch = charAt(++pos);
    }

    protected void unscan() {
        ch = charAt(--pos);
    }

    public boolean isEOF() {
        return pos >= text.length();
    }

    /**
     * Report an error at the given position using the provided arguments.
     */
    protected void lexError(String key, Object... args) {
        token = ERROR;
    }

    /**
     * Return the current token, set by nextToken().
     */
    public final Token token() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
    }

    public final DbType getDbType() {
        return this.dbType;
    }

    public String info() {
        int line = 1;
        int column = 1;
        for (int i = 0; i < startPos; ++i, column++) {
            char ch = charAt(i);
            if (ch == '\n') {
                column = 1;
                line++;
            }
        }

        this.posLine = line;
        this.posColumn = column;

        StringBuilder buf = new StringBuilder();
        buf
            .append("pos ")
            .append(pos)
            .append(", line ")
            .append(line)
            .append(", column ")
            .append(column);
        if (token != null) {
            if (token.name != null) {
                buf.append(", token ").append(token.name);
            } else {
                buf.append(", token ").append(token.name());
            }
        }

        if (token == Token.IDENTIFIER
            || token == Token.LITERAL_ALIAS
            || token == Token.LITERAL_CHARS) {
            buf.append(" ")
                .append(
                    stringVal());
        }

        if (isEnabled(SQLParserFeature.PrintSQLWhileParsingFailed)) {
            buf.append(", SQL : ");
            buf.append(text);
        }

        return buf.toString();
    }

    public final void nextTokenComma() {
        if (ch == ' ') {
            scanChar();
        }

        if (isComma(ch)) {
            pos += nchar - 1;
            scanChar();
            token = COMMA;
            return;
        }

        if (isRightParen(ch)) {
            pos += nchar - 1;
            scanChar();
            token = RPAREN;
            return;
        }

        if (ch == '.') {
            scanChar();
            token = DOT;
            return;
        }

        if (ch == 'a' || ch == 'A') {
            char ch_next = charAt(pos + 1);
            if (ch_next == 's' || ch_next == 'S') {
                char ch_next_2 = charAt(pos + 2);
                if (ch_next_2 == ' ') {
                    pos += 2;
                    ch = ' ';
                    token = Token.AS;
                    stringVal = "AS";
                    return;
                }
            }
        }

        nextToken();
    }

    public final void nextTokenCommaValue() {
        if (ch == ' ') {
            scanChar();
        }

        if (isComma(ch)) {
            pos += nchar - 1;
            scanChar();
            token = COMMA;
            return;
        }

        if (isRightParen(ch)) {
            pos += nchar - 1;
            scanChar();
            token = RPAREN;
            return;
        }

        if (ch == '.') {
            scanChar();
            token = DOT;
            return;
        }

        if (ch == 'a' || ch == 'A') {
            char ch_next = charAt(pos + 1);
            if (ch_next == 's' || ch_next == 'S') {
                char ch_next_2 = charAt(pos + 2);
                if (ch_next_2 == ' ') {
                    pos += 2;
                    ch = ' ';
                    token = Token.AS;
                    stringVal = "AS";
                    return;
                }
            }
        }

        nextTokenValue();
    }

    public final void nextTokenEq() {
        if (ch == ' ') {
            scanChar();
        }

        if (ch == '=') {
            scanChar();
            if (ch == '=') {
                scanChar();
                token = EQEQ;
            } else if (ch == '>') {
                scanChar();
                token = EQGT;
            }
            token = EQ;
            return;
        }

        if (ch == '.') {
            scanChar();
            token = DOT;
            return;
        }

        if (ch == 'a' || ch == 'A') {
            char ch_next = charAt(pos + 1);
            if (ch_next == 's' || ch_next == 'S') {
                char ch_next_2 = charAt(pos + 2);
                if (ch_next_2 == ' ') {
                    pos += 2;
                    ch = ' ';
                    token = Token.AS;
                    stringVal = "AS";
                    return;
                }
            }
        }

        nextToken();
    }

    public final void nextTokenLParen() {
        if (ch == ' ') {
            scanChar();
        }

        if (isLeftParen(ch)) {
            pos += nchar - 1;
            scanChar();
            token = LPAREN;
            return;
        }
        nextToken();
    }

    public final void nextTokenValue() {
        this.startPos = pos;
        while (ch == ' ') {
            scanChar();
        }

        if (ch == '\'') {
            bufPos = 0;
            if (dbType == DbType.mysql) {
                scanString2();
            } else {
                scanString();
            }
            return;
        }

        if (ch == '"' && !isEnabled(SQLParserFeature.KeepNameQuotes)) {
            bufPos = 0;
            scanString2_d();
            return;
        }

        if (ch == '0') {
            bufPos = 0;
            if (charAt(pos + 1) == 'x') {
                scanChar();
                scanChar();
                scanHexaDecimal();
            } else {
                scanNumber();
            }
            return;
        }

        if (ch > '0' && ch <= '9') {
            bufPos = 0;
            scanNumber();
            return;
        }

        if (ch == '-') {
            char next = charAt(pos + 1);
            if (next >= '0' && next <= '9') {
                bufPos = 0;
                scanNumber();
                return;
            }
        }

        if (ch == '?') {
            scanChar();
            token = Token.QUES;
            return;
        }

        if (ch == 'n' || ch == 'N') {
            char c1 = 0, c2, c3, c4;
            if (pos + 4 < text.length()
                && ((c1 = text.charAt(pos + 1)) == 'u' || c1 == 'U')
                && ((c2 = text.charAt(pos + 2)) == 'l' || c2 == 'L')
                && ((c3 = text.charAt(pos + 3)) == 'l' || c3 == 'L')
                && (isWhitespace(c4 = text.charAt(pos + 4), pos + 4) || c4 == ',' || isRightParen(c4, pos + 4))) {
                pos += 4;
                ch = c4;
                token = Token.NULL;
                stringVal = "NULL";
                return;
            }

            if (c1 == '\'') {
                bufPos = 0;
                ++pos;
                ch = '\'';
                scanString();
                token = Token.LITERAL_NCHARS;
                return;
            }
        }

        if (isRightParen(ch)) {
            pos += nchar - 1;
            scanChar();
            token = Token.RPAREN;
            return;
        }

        if (ch == '$' && isVaraintChar(charAt(pos + 1))) {
            scanVariable();
            return;
        }

        if (isFirstIdentifierChar(ch)) {
            scanIdentifier();
            return;
        }

        nextToken();
    }

    static boolean isVaraintChar(char ch) {
        return ch == '{' || (ch >= '0' && ch <= '9');
    }

    public final void nextTokenBy() {
        while (ch == ' ') {
            scanChar();
        }

        if (ch == 'b' || ch == 'B') {
            char ch_next = charAt(pos + 1);
            if (ch_next == 'y' || ch_next == 'Y') {
                char ch_next_2 = charAt(pos + 2);
                if (ch_next_2 == ' ') {
                    pos += 2;
                    ch = ' ';
                    token = Token.BY;
                    stringVal = "BY";
                    return;
                }
            }
        }

        nextToken();
    }

    public final void nextTokenNotOrNull() {
        while (ch == ' ') {
            scanChar();
        }

        if ((ch == 'n' || ch == 'N') && pos + 3 < text.length()) {
            char c1 = text.charAt(pos + 1);
            char c2 = text.charAt(pos + 2);
            char c3 = text.charAt(pos + 3);

            if ((c1 == 'o' || c1 == 'O')
                && (c2 == 't' || c2 == 'T')
                && isWhitespace(c3, pos + 3)) {
                pos += 3;
                ch = c3;
                token = Token.NOT;
                stringVal = "NOT";
                return;
            }

            char c4;
            if (pos + 4 < text.length()
                && (c1 == 'u' || c1 == 'U')
                && (c2 == 'l' || c2 == 'L')
                && (c3 == 'l' || c3 == 'L')
                && isWhitespace(c4 = text.charAt(pos + 4), pos + 4)) {
                pos += 4;
                ch = c4;
                token = Token.NULL;
                stringVal = "NULL";
                return;
            }
        }

        nextToken();
    }

    public final void nextTokenIdent() {
        while (ch == ' ') {
            scanChar();
        }

        if (ch == '$' && isVaraintChar(charAt(pos + 1))) {
            scanVariable();
            return;
        }

        if (isFirstIdentifierChar(ch) && !isLeftParen(ch)) {
            scanIdentifier();
            return;
        }

        if (ch == ')') {
            scanChar();
            token = RPAREN;
            return;
        }

        nextToken();
    }

    public final SQLType scanSQLType() {
        for (; ; ) {
            while (isWhitespace(ch)) {
                ch = charAt(pos = pos + nchar);
            }

            if (ch == '/' && pos + 1 < text.length() && text.charAt(pos + 1) == '*') {
                int index = text.indexOf("*/", pos + 2);
                if (index == -1) {
                    return SQLType.UNKNOWN;
                }

                pos = index + 2;
                ch = charAt(pos);
                continue;
            }

            if (ch == '-' && pos + 1 < text.length() && text.charAt(pos + 1) == '-') {
                int index = text.indexOf('\n', pos + 2);
                if (index == -1) {
                    return SQLType.UNKNOWN;
                }

                pos = index + 1;
                ch = charAt(pos);
                continue;
            }

            break;
        }

        while (ch == '(') {
            ch = charAt(++pos);

            while (isWhitespace(ch)) {
                ch = charAt(pos = pos + nchar);
            }
        }

        long hashCode = FnvHash.BASIC;
        for (; ; ) {
            char c;
            if (ch >= 'a' && ch <= 'z') {
                c = ch;
            } else if (ch >= 'A' && ch <= 'Z') {
                c = (char) (ch + 32);
            } else {
                break;
            }
            hashCode ^= c;
            hashCode *= FnvHash.PRIME;

            ch = charAt(++pos);
        }

        if (ch == '_' || (ch >= '0' && ch <= '9')) {
            return SQLType.UNKNOWN;
        }

        if (hashCode == FnvHash.Constants.SELECT) {
            return SQLType.SELECT;
        } else if (hashCode == FnvHash.Constants.INSERT) {
            return SQLType.INSERT;
        } else if (hashCode == FnvHash.Constants.DELETE) {
            return SQLType.DELETE;
        } else if (hashCode == FnvHash.Constants.UPDATE) {
            return SQLType.UPDATE;
        } else if (hashCode == FnvHash.Constants.REPLACE) {
            return SQLType.REPLACE;
        } else if (hashCode == FnvHash.Constants.TRUNCATE) {
            return SQLType.TRUNCATE;
        } else if (hashCode == FnvHash.Constants.MERGE) {
            return SQLType.MERGE;
        } else if (hashCode == FnvHash.Constants.CREATE) {
            return SQLType.CREATE;
        } else if (hashCode == FnvHash.Constants.ALTER) {
            return SQLType.ALTER;
        } else if (hashCode == FnvHash.Constants.SHOW) {
            return SQLType.SHOW;
        } else if (hashCode == FnvHash.Constants.DESC) {
            return SQLType.DESC;
        } else if (hashCode == FnvHash.Constants.DESCRIBE) {
            return SQLType.DESC;
        } else if (hashCode == FnvHash.Constants.SET) {
            return SQLType.SET;
        } else if (hashCode == FnvHash.Constants.KILL) {
            return SQLType.KILL;
        } else if (hashCode == FnvHash.Constants.MSCK) {
            return SQLType.MSCK;
        } else if (hashCode == FnvHash.Constants.USE) {
            return SQLType.USE;
        } else if (hashCode == FnvHash.Constants.DROP) {
            return SQLType.DROP;
        } else if (hashCode == FnvHash.Constants.LIST) {
            return SQLType.LIST;
        } else if (hashCode == FnvHash.Constants.ROLLBACK) {
            return SQLType.ROLLBACK;
        } else if (hashCode == FnvHash.Constants.COMMIT) {
            return SQLType.COMMIT;
        } else if (hashCode == FnvHash.Constants.WHO) {
            return SQLType.WHO;
        } else if (hashCode == FnvHash.Constants.GRANT) {
            return SQLType.GRANT;
        } else if (hashCode == FnvHash.Constants.REVOKE) {
            return SQLType.REVOKE;
        } else if (hashCode == FnvHash.Constants.ANALYZE) {
            return SQLType.ANALYZE;
        } else if (hashCode == FnvHash.Constants.EXPLAIN) {
            return SQLType.EXPLAIN;
        } else if (hashCode == FnvHash.Constants.WITH) {
            if (dbType == DbType.mysql) {
                return SQLType.SELECT;
            }
            return SQLType.UNKNOWN;
        } else if (hashCode == FnvHash.Constants.DUMP) {
            nextToken();
            if (identifierEquals(FnvHash.Constants.DATA)) {
                return SQLType.DUMP_DATA;
            }
        } else if (hashCode == FnvHash.Constants.ADD) {
            nextToken();
            if (token == Token.USER || identifierEquals(FnvHash.Constants.USER)) {
                return SQLType.ADD_USER;
            }
        } else if (hashCode == FnvHash.Constants.REMOVE) {
            nextToken();
            if (token == Token.USER || identifierEquals(FnvHash.Constants.USER)) {
                return SQLType.REMOVE_USER;
            }
        }

        return SQLType.UNKNOWN;
    }

    public final SQLType scanSQLTypeV2() {

        SQLType sqlType = scanSQLType();

        if (sqlType == SQLType.CREATE) {
            nextToken();
            if (token == Token.USER || identifierEquals(FnvHash.Constants.USER)) {
                return SQLType.CREATE_USER;
            }
        } else if (sqlType == SQLType.DROP) {
            nextToken();
            if (token == Token.USER || identifierEquals(FnvHash.Constants.USER)) {
                return SQLType.DROP_USER;
            }
        } else if (sqlType == SQLType.ALTER) {
            nextToken();
            if (token == Token.USER || identifierEquals(FnvHash.Constants.USER)) {
                return SQLType.ALTER_USER;
            }
        }

        return sqlType;
    }

    public final void nextTokenAlias() {
        startPos = pos;
        bufPos = 0;
        while (isWhitespace(ch)) {
            if (ch == '\n') {
                line++;
            }

            ch = charAt(pos = pos + nchar);
            startPos = pos;
            continue;
        }

        if (ch == '"') {
            scanAlias();
        } else if (ch == '\'') {
            scanAlias();

            int p;
            if (stringVal.length() > 1
                && stringVal.indexOf('"') == -1
                && ((p = stringVal.indexOf('\'', 1)) == -1 || p == stringVal.length() - 1)
            ) {
                char[] chars = stringVal.toCharArray();
                chars[0] = '"';
                chars[chars.length - 1] = '"';
                stringVal = new String(chars);
            }
            token = LITERAL_ALIAS;
        } else {
            nextToken();
        }
    }

    public final void nextPath() {
        while (isWhitespace(ch)) {
            ch = charAt(pos = pos + nchar);
        }
        stringVal = null;
        mark = pos;
        while (!isWhitespace(ch)) {
            ch = charAt(pos = pos + nchar);
        }
        bufPos = pos - mark;

        ch = charAt(++pos);

        token = LITERAL_PATH;
    }

    public final void nextToken() {
        startPos = pos;
        bufPos = 0;
        if (comments != null && comments.size() > 0) {
            comments = null;
        }

        this.lines = 0;
        int startLine = line;

        for (; ; ) {
            if (isWhitespace(ch)) {
                if (ch == '\n') {
                    line++;

                    lines = line - startLine;
                }

                ch = charAt(pos = pos + nchar);
                startPos = pos;
                continue;
            }

            if (ch == '$' && isVaraintChar(charAt(pos + 1))) {
                scanVariable();
                return;
            }

            if (isFirstIdentifierChar(ch)) {
                if (isLeftParen(ch)) {
                    pos += nchar - 1;
                    scanChar();
                    token = LPAREN;
                    return;
                } else if (isRightParen(ch)) {
                    pos += nchar - 1;
                    scanChar();
                    token = RPAREN;
                    return;
                }

                if (ch == 'N' || ch == 'n') {
                    if (charAt(pos + 1) == '\'') {
                        ++pos;
                        ch = '\'';
                        scanString();
                        token = Token.LITERAL_NCHARS;
                        return;
                    }
                }

                scanIdentifier();
                return;
            }

            switch (ch) {
            case '0':
                if (charAt(pos + 1) == 'x') {
                    scanChar();
                    scanChar();
                    scanHexaDecimal();
                } else {
                    scanNumber();
                }
                return;
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                scanNumber();
                return;
            case ',':
                scanChar();
                token = COMMA;
                return;
            case '(':
                scanChar();
                token = LPAREN;
                return;
            case ')':
                scanChar();
                token = RPAREN;
                return;
            case '[':
                scanLBracket();
                return;
            case ']':
                scanChar();
                token = RBRACKET;
                return;
            case '{':
                scanChar();
                token = LBRACE;
                return;
            case '}':
                scanChar();
                token = RBRACE;
                return;
            case ':':
                scanChar();
                if (ch == '=') {
                    scanChar();
                    token = COLONEQ;
                } else if (ch == ':') {
                    scanChar();
                    token = COLONCOLON;
                } else {
                    if (isEnabled(SQLParserFeature.TDDLHint)
                        || lexSingleColon) {
                        token = COLON;
                        return;
                    }
                    unscan();
                    scanVariable();
                }
                return;
            case '#':
                scanSharp();
                if ((token == Token.LINE_COMMENT || token == Token.MULTI_LINE_COMMENT) && skipComment) {
                    bufPos = 0;
                    continue;
                }
                return;
            case '.':
                scanChar();
                if (isDigit(ch)
                    && (pos == 1 || token != IDENTIFIER)
                ) {
                    unscan();
                    scanNumber();
                    return;
                } else if (ch == '.') {
                    scanChar();
                    if (ch == '.') {
                        scanChar();
                        token = Token.DOTDOTDOT;
                    } else {
                        token = Token.DOTDOT;
                    }
                } else {
                    token = Token.DOT;
                }
                return;
            case '\'':
                scanString();
                return;
            case '\"':
                scanAlias();
                return;
            case '*':
                scanChar();
                token = Token.STAR;
                return;
            case '?':
                scanChar();
                token = Token.QUES;
                return;
            case ';':
                scanChar();
                token = Token.SEMI;
                return;
            case '`':
                throw new ParserException("TODO. " + info()); // TODO
            case '@':
                scanVariable_at();
                return;
            case '-':
                char next = charAt(pos + 1);
                if (next == '-') {
                    scanComment();
                    if ((token == Token.LINE_COMMENT || token == Token.MULTI_LINE_COMMENT) && skipComment) {
                        bufPos = 0;
                        continue;
                    }
                } else if (next >= '0' && next <= '9') {
                    if (token == null) {
                        scanNumber();
                        return;
                    }
                    switch (token) {
                    case COMMA:
                    case LPAREN:
                    case WITH:
                    case BY:
                        scanNumber();
                        break;
                    default:
                        scanOperator();
                        break;
                    }
                } else {
                    scanOperator();
                }
                return;
            case '/':
                char nextChar = charAt(pos + 1);
                if (nextChar == '/'
                    || nextChar == '*'
                    || (nextChar == '!' && isEnabled(SQLParserFeature.TDDLHint))) {
                    scanComment();
                    if ((token == Token.LINE_COMMENT || token == Token.MULTI_LINE_COMMENT) && skipComment) {
                        bufPos = 0;
                        continue;
                    }
                } else if (nextChar == ' ' && charAt(pos + 2) == '*') {
                    scanComment();
                    if ((token == Token.LINE_COMMENT || token == Token.MULTI_LINE_COMMENT) && skipComment) {
                        bufPos = 0;
                        continue;
                    }
                } else {
                    token = Token.SLASH;
                    scanChar();
                }
                return;
            case 0xEF:
            case 0xA3:
                if (isLeftParen(ch)) {
                    pos += nchar - 1;
                    scanChar();
                    token = LPAREN;
                    return;
                } else if (isRightParen(ch)) {
                    pos += nchar - 1;
                    scanChar();
                    token = RPAREN;
                    return;
                } else if (isComma(ch)) {
                    pos += nchar - 1;
                    scanChar();
                    token = COMMA;
                    return;
                }
                // fallthrough
            default:
                if (Character.isLetter(ch)) {
                    scanIdentifier();
                    return;
                }

                if (isOperator(ch)) {
                    scanOperator();
                    return;
                }

                if (ch == '\\' && charAt(pos + 1) == 'N'
                    && DbType.mysql == dbType) {
                    scanChar();
                    scanChar();
                    token = Token.NULL;
                    return;
                }

                // QS_TODO ?
                if (isEOF()) { // JLS
                    token = EOF;
                } else {
                    lexError("illegal.char", String.valueOf((int) ch));
                    scanChar();
                }

                return;
            }
        }

    }

    protected void scanLBracket() {
        scanChar();
        token = LBRACKET;
    }

    private final void scanOperator() {
        switch (ch) {
        case '+':
            scanChar();
            token = Token.PLUS;
            break;
        case '-':
            scanChar();
            if (ch == '>') {
                scanChar();
                if (ch == '>') {
                    scanChar();
                    token = Token.SUBGTGT;
                } else {
                    token = Token.SUBGT;
                }
            } else {
                token = Token.SUB;
            }
            break;
        case '*':
            scanChar();
            token = Token.STAR;
            break;
        case '/':
            scanChar();
            token = Token.SLASH;
            break;
        case '&':
            scanChar();
            if (ch == '&') {
                scanChar();
                token = Token.AMPAMP;
            } else {
                token = Token.AMP;
            }
            break;
        case '|':
            scanChar();
            if (ch == '|') {
                scanChar();
                if (ch == '/') {
                    scanChar();
                    token = Token.BARBARSLASH;
                } else {
                    token = Token.BARBAR;
                }
            } else if (ch == '/') {
                scanChar();
                token = Token.BARSLASH;
            } else {
                token = Token.BAR;
            }
            break;
        case '^':
            scanChar();
            if (ch == '=') {
                scanChar();
                token = Token.CARETEQ;
            } else {
                token = Token.CARET;
            }
            break;
        case '%':
            scanChar();
            token = Token.PERCENT;
            break;
        case '=':
            scanChar();
            if (ch == ' ') {
                scanChar();
            }

            if (ch == '=') {
                scanChar();
                token = Token.EQEQ;
            } else if (ch == '>') {
                scanChar();
                token = Token.EQGT;
            } else {
                token = Token.EQ;
            }
            break;
        case '>':
            scanChar();
            if (ch == '=') {
                scanChar();
                token = Token.GTEQ;
            } else if (ch == '>') {
                scanChar();
                token = Token.GTGT;
            } else {
                token = Token.GT;
            }
            break;
        case '<':
            scanChar();
            if (ch == '=') {
                scanChar();
                if (ch == '>') {
                    token = Token.LTEQGT;
                    scanChar();
                } else {
                    token = Token.LTEQ;
                }
            } else if (ch == '>') {
                scanChar();
                token = Token.LTGT;
            } else if (ch == '<') {
                scanChar();
                token = Token.LTLT;
            } else if (ch == '@') {
                scanChar();
                token = Token.LT_MONKEYS_AT;
            } else if (ch == '-' && charAt(pos + 1) == '>') {
                scanChar();
                scanChar();
                token = Token.LT_SUB_GT;
            } else {
                if (ch == ' ') {
                    char c1 = charAt(pos + 1);
                    if (c1 == '=') {
                        scanChar();
                        scanChar();
                        if (ch == '>') {
                            token = Token.LTEQGT;
                            scanChar();
                        } else {
                            token = Token.LTEQ;
                        }
                    } else if (c1 == '>') {
                        scanChar();
                        scanChar();
                        token = Token.LTGT;
                    } else if (c1 == '<') {
                        scanChar();
                        scanChar();
                        token = Token.LTLT;
                    } else if (c1 == '@') {
                        scanChar();
                        scanChar();
                        token = Token.LT_MONKEYS_AT;
                    } else if (c1 == '-' && charAt(pos + 2) == '>') {
                        scanChar();
                        scanChar();
                        scanChar();
                        token = Token.LT_SUB_GT;
                    } else {
                        token = Token.LT;
                    }
                } else {
                    token = Token.LT;
                }
            }
            break;
        case '!':
            scanChar();
            while (isWhitespace(ch)) {
                pos += nchar - 1;
                scanChar();
            }
            if (ch == '=') {
                scanChar();
                token = Token.BANGEQ;
            } else if (ch == '>') {
                scanChar();
                token = Token.BANGGT;
            } else if (ch == '<') {
                scanChar();
                token = Token.BANGLT;
            } else if (ch == '!') {
                scanChar();
                token = Token.BANGBANG; // postsql
            } else if (ch == '~') {
                scanChar();
                if (ch == '*') {
                    scanChar();
                    token = Token.BANG_TILDE_STAR; // postsql
                } else {
                    token = Token.BANG_TILDE; // postsql
                }
            } else {
                token = Token.BANG;
            }
            break;
        case '?':
            scanChar();
            token = Token.QUES;
            break;
        case '~':
            scanChar();
            if (ch == '*') {
                scanChar();
                token = Token.TILDE_STAR;
            } else if (ch == '=') {
                scanChar();
                token = Token.TILDE_EQ; // postsql
            } else {
                token = Token.TILDE;
            }
            break;
        default:
            throw new ParserException("TODO. " + info());
        }
    }

    protected void scanString() {
        mark = pos;
        boolean hasSpecial = false;
        Token preToken = this.token;

        int skip = 0;
        for (; ; ) {
            if (isEOF()) {
                lexError("unclosed.str.lit");
                return;
            }

            if (skip > 0) {
                skip--;
                ch = charAt(++pos);

                if (!hasSpecial) {
                    bufPos++;
                } else {
                    if (bufPos == buf.length) {
                        putChar(ch);
                    } else {
                        buf[bufPos++] = (byte) ch;
                    }
                }
                continue;
            }

            ch = charAt(++pos);
            skip = lengthOf(ch, pos) - 1;

            if (ch == '\'') {
                scanChar();
                if (ch != '\'') {
                    token = LITERAL_CHARS;
                    break;
                } else {
                    if (!hasSpecial) {
                        initBuff(bufPos);
                        arraycopy(mark + 1, buf, 0, bufPos);
                        hasSpecial = true;
                    }
                    putChar('\'');
                    continue;
                }
            }

            if (!hasSpecial) {
                bufPos++;
                continue;
            }

            if (bufPos == buf.length) {
                putChar(ch);
            } else {
                buf[bufPos++] = (byte) ch;
            }
        }

        if (!hasSpecial) {
            if (preToken == Token.AS) {
                stringVal = subString(mark, bufPos + 2);
            } else {
                stringVal = subString(mark + 1, bufPos);
            }
        } else {
            stringVal = new String(buf, 0, bufPos, charset);
        }
    }

    protected final void scanString2() {
        {
            boolean hasSpecial = false;
            int startIndex = pos + 1;
            int endIndex = -1; // text.indexOf('\'', startIndex);
            for (int i = startIndex; i < text.length(); ++i) {
                final char ch = text.charAt(i);
                if (ch == '\\') {
                    hasSpecial = true;
                    continue;
                }
                if (ch == '\'') {
                    endIndex = i;
                    break;
                }
                i += lengthOf(ch, i) - 1;
            }

            if (endIndex == -1) {
                throw new ParserException("unclosed str. " + info());
            }

            String stringVal;
            if (token == Token.AS) {
                stringVal = text.substring(pos, endIndex + 1);
            } else {
                if (startIndex == endIndex) {
                    stringVal = "";
                } else {
                    stringVal = text.substring(startIndex, endIndex);
                }
            }
            // hasSpecial = stringVal.indexOf('\\') != -1;

            byte[] binaryVal;
            if (hash_lower == FnvHash.Constants._BINARY) {
                if (token == Token.AS) {
                    binaryVal = text.getBytes(pos, endIndex + 1);
                } else {
                    if (startIndex == endIndex) {
                        binaryVal = new byte[0];
                    } else {
                        binaryVal = text.getBytes(startIndex, endIndex);
                    }
                }
            } else {
                binaryVal = null;
            }

            if (!hasSpecial) {
                this.stringVal = stringVal;
                this.binaryVal = binaryVal;
                int pos = endIndex + 1;
                char ch = charAt(pos);
                if (ch != '\'') {
                    this.pos = pos;
                    this.ch = ch;
                    token = LITERAL_CHARS;
                    return;
                }
            }
        }

        int skip = 0;
        mark = pos;
        boolean hasSpecial = false;
        for (; ; ) {
            if (isEOF()) {
                lexError("unclosed.str.lit");
                return;
            }

            if (skip > 0) {
                skip--;
                ch = charAt(++pos);

                if (!hasSpecial) {
                    bufPos++;
                } else {
                    if (bufPos == buf.length) {
                        putChar(ch);
                    } else {
                        buf[bufPos++] = (byte) ch;
                    }
                }
                continue;
            }

            ch = charAt(++pos);
            skip = lengthOf(ch, pos) - 1;

            if (ch == '\\') {
                scanChar();
                skip = lengthOf(ch, pos) - 1;

                if (!hasSpecial) {
                    initBuff(bufPos);
                    arraycopy(mark + 1, buf, 0, bufPos);
                    hasSpecial = true;
                }

                switch (ch) {
                case '0':
                    putChar('\0');
                    break;
                case '\'':
                    putChar('\'');
                    break;
                case '"':
                    putChar('"');
                    break;
                case 'b':
                    putChar('\b');
                    break;
                case 'n':
                    putChar('\n');
                    break;
                case 'r':
                    putChar('\r');
                    break;
                case 't':
                    putChar('\t');
                    break;
                case '\\':
                    putChar('\\');
                    break;
                case 'Z':
                    putChar((char) 0x1A); // ctrl + Z
                    break;
                case '%':
                    if (dbType == DbType.mysql) {
                        putChar('\\');
                    }
                    putChar('%');
                    break;
                case '_':
                    if (dbType == DbType.mysql) {
                        putChar('\\');
                    }
                    putChar('_');
                    break;
                case 'u':
                    if ((features & SQLParserFeature.SupportUnicodeCodePoint.mask) != 0) {
                        char c1 = charAt(++pos);
                        char c2 = charAt(++pos);
                        char c3 = charAt(++pos);
                        char c4 = charAt(++pos);

                        int intVal = Integer.parseInt(new String(new char[] {c1, c2, c3, c4}), 16);

                        putUnicodeChar(intVal);
                    } else {
                        putChar(ch);
                    }
                    break;
                default:
                    putChar(ch);
                    break;
                }

                continue;
            }
            if (ch == '\'') {
                scanChar();
                skip = lengthOf(ch, pos) - 1;

                if (ch != '\'') {
                    token = LITERAL_CHARS;
                    break;
                } else {
                    if (!hasSpecial) {
                        initBuff(bufPos);
                        arraycopy(mark + 1, buf, 0, bufPos);
                        hasSpecial = true;
                    }
                    putChar('\'');
                    continue;
                }
            }

            if (!hasSpecial) {
                bufPos++;
                continue;
            }

            if (bufPos == buf.length) {
                putChar(ch);
            } else {
                buf[bufPos++] = (byte) ch;
            }
        }

        if (!hasSpecial) {
            stringVal = subString(mark + 1, bufPos);
        } else {
            stringVal = new String(buf, 0, bufPos, charset);
        }

        if (hash_lower == FnvHash.Constants._BINARY) {
            if (!hasSpecial) {
                binaryVal = text.getBytes(mark + 1, mark + 1 + bufPos);
            } else {
                binaryVal = Arrays.copyOf(buf, bufPos);
            }
        }
    }

    protected final void scanString2_d() {
        {
            boolean hasSpecial = false;
            int startIndex = pos + 1;
            int endIndex = -1; // text.indexOf('\'', startIndex);
            for (int i = startIndex; i < text.length(); ++i) {
                final char ch = text.charAt(i);
                if (ch == '\\') {
                    int i1 = i + 1;
                    if (i1 < text.length()) {
                        hasSpecial = true;
                        ++i;
                        continue;
                    }
                }

                if (ch == '"') {
                    int i1 = i + 1;
                    if (i1 < text.length() && text.charAt(i1) == '"') {
                        hasSpecial = true;
                        ++i;
                        continue;
                    }
                    endIndex = i;
                    break;
                }
            }

            if (endIndex == -1) {
                throw new ParserException("unclosed str. " + info());
            }

            String stringVal;
            if (token == Token.AS) {
                stringVal = subString(pos, endIndex + 1 - pos);
            } else {
                if (charAt(endIndex + 1) == '.') {
                    stringVal = subString(startIndex - 1, (endIndex - startIndex) + 2);
                } else {
                    stringVal = subString(startIndex, endIndex - startIndex);
                }
            }
            // hasSpecial = stringVal.indexOf('\\') != -1;

            if (!hasSpecial) {
                this.stringVal = stringVal;
                int pos = endIndex + 1;
                char ch = charAt(pos);
                if (ch == '.') {
                    this.pos = pos;
                    this.ch = ch;
                    token = Token.IDENTIFIER;
                    return;
                }

                if (ch != '\'') {
                    this.pos = pos;
                    this.ch = ch;
                    token = LITERAL_CHARS;
                    return;
                }
            }
        }

        int skip = 0;
        mark = pos;
        boolean hasSpecial = false;
        for (; ; ) {
            if (isEOF()) {
                lexError("unclosed.str.lit");
                return;
            }

            if (skip > 0) {
                skip--;
                ch = charAt(++pos);

                if (!hasSpecial) {
                    bufPos++;
                } else {
                    if (bufPos == buf.length) {
                        putChar(ch);
                    } else {
                        buf[bufPos++] = (byte) ch;
                    }
                }
                continue;
            }

            ch = charAt(++pos);
            skip = lengthOf(ch, pos) - 1;

            if (ch == '\\') {
                scanChar();
                skip = lengthOf(ch, pos) - 1;

                if (!hasSpecial) {
                    initBuff(bufPos);
                    arraycopy(mark + 1, buf, 0, bufPos);
                    hasSpecial = true;
                }

                switch (ch) {
                case '0':
                    putChar('\0');
                    break;
                case '\'':
                    putChar('\'');
                    break;
                case '"':
                    putChar('"');
                    break;
                case 'b':
                    putChar('\b');
                    break;
                case 'n':
                    putChar('\n');
                    break;
                case 'r':
                    putChar('\r');
                    break;
                case 't':
                    putChar('\t');
                    break;
                case '\\':
                    putChar('\\');
                    break;
                case 'Z':
                    putChar((char) 0x1A); // ctrl + Z
                    break;
                case '%':
                    if (dbType == DbType.mysql) {
                        putChar('\\');
                    }
                    putChar('%');
                    break;
                case '_':
                    if (dbType == DbType.mysql) {
                        putChar('\\');
                    }
                    putChar('_');
                    break;
                default:
                    putChar(ch);
                    break;
                }

                continue;
            }
            if (ch == '"') {
                scanChar();
                skip = lengthOf(ch, pos) - 1;

                if (ch != '"') {
                    if (buf != null && bufPos > 0) {
                        stringVal = new String(buf, 0, bufPos, charset);
                    }
                    token = LITERAL_CHARS;
                    break;
                } else {
                    if (!hasSpecial) {
                        initBuff(bufPos);
                        arraycopy(mark + 1, buf, 0, bufPos);
                        hasSpecial = true;
                    }
                    putChar('"');
                    continue;
                }
            }

            if (!hasSpecial) {
                bufPos++;
                continue;
            }

            if (bufPos == buf.length) {
                putChar(ch);
            } else {
                buf[bufPos++] = (byte) ch;
            }
        }

        if (!hasSpecial) {
            stringVal = subString(mark + 1, bufPos);
        } else {
            stringVal = new String(buf, 0, bufPos, charset);
        }
    }

    protected final void scanAlias() {
        final char quote = ch;
        {
            boolean hasSpecial = false;
            int startIndex = pos + 1;
            int endIndex = -1; // text.indexOf('\'', startIndex);
            for (int i = startIndex; i < text.length(); ++i) {
                final char ch = text.charAt(i);
                if (ch == '\\') {
                    hasSpecial = true;
                    i++;
                    continue;
                }
                if (ch == quote) {
                    if (i + 1 < text.length()) {
                        char ch_next = charAt(i + 1);
                        if (ch_next == quote) {
                            hasSpecial = true;
                            i++;
                            continue;
                        }
                    }

                    endIndex = i;
                    break;
                }
                i += lengthOf(ch, i) - 1;
            }

            if (endIndex == -1) {
                throw new ParserException("unclosed str. " + info());
            }

            String stringVal = subString(pos, endIndex + 1 - pos);
            // hasSpecial = stringVal.indexOf('\\') != -1;

            if (!hasSpecial) {
                this.stringVal = stringVal;
                int pos = endIndex + 1;
                char ch = charAt(pos);
                if (ch != '\'') {
                    this.pos = pos;
                    this.ch = ch;
                    token = LITERAL_ALIAS;
                    return;
                }
            }
        }

        mark = pos;
        initBuff(bufPos);
        //putChar(ch);

        putChar(ch);

        int skip = 0;
        for (; ; ) {
            if (isEOF()) {
                lexError("unclosed.str.lit");
                return;
            }

            if (skip > 0) {
                skip--;
                ch = charAt(++pos);
                if (bufPos == buf.length) {
                    putChar(ch);
                } else {
                    buf[bufPos++] = (byte) ch;
                }
                continue;
            }

            ch = charAt(++pos);
            skip = lengthOf(ch, pos) - 1;

            if (ch == '\\') {
                scanChar();

                switch (ch) {
                case '0':
                    putChar('\0');
                    break;
                case '\'':
                    if (ch == quote) {
                        putChar('\\');
                    }
                    putChar('\'');
                    break;
                case '"':
                    if (ch == quote) {
                        putChar('\\');
                    }
                    putChar('"');
                    break;
                case 'b':
                    putChar('\b');
                    break;
                case 'n':
                    putChar('\n');
                    break;
                case 'r':
                    putChar('\r');
                    break;
                case 't':
                    putChar('\t');
                    break;
                case '\\':
                    putChar('\\');
                    putChar('\\');
                    break;
                case 'Z':
                    putChar((char) 0x1A); // ctrl + Z
                    break;
                case 'u':
                    putChar(ch);
                    break;
                default:
                    putChar(ch);
                    break;
                }

                continue;
            }

            if (ch == quote) {
                char ch_next = charAt(pos + 1);

                if (ch_next == quote) {
                    putChar('\\');
                    putChar(ch);
                    scanChar();
                    continue;
                }

                putChar(ch);
                scanChar();
                token = LITERAL_ALIAS;
                break;
            }

            if (bufPos == buf.length) {
                putChar(ch);
            } else {
                buf[bufPos++] = (byte) ch;
            }
        }

        stringVal = new String(buf, 0, bufPos, charset);
    }

    public void scanSharp() {
        scanVariable();
    }

    public void scanVariable() {
        if (ch != ':' && ch != '#' && ch != '$' && !(ch == '@' && false)) {
            throw new ParserException("illegal variable. " + info());
        }

        mark = pos;
        bufPos = 1;
        char ch;

        final char c1 = charAt(pos + 1);
        if (c1 == '{') {
            pos++;
            bufPos++;

            for (; ; ) {
                ch = charAt(++pos);

                if (ch == '}') {
                    break;
                }

                bufPos++;
                continue;
            }

            if (ch != '}') {
                throw new ParserException("syntax error. " + info());
            }
            ++pos;
            bufPos++;

            this.ch = charAt(pos);

            stringVal = addSymbol();
            token = Token.VARIANT;
            return;
        }

        for (; ; ) {
            ch = charAt(++pos);

            if (!isIdentifierChar(ch)) {
                break;
            }

            pos += nchar - 1;
            bufPos += nchar;
            continue;
        }

        this.ch = charAt(pos);

        stringVal = addSymbol();
        token = Token.VARIANT;
    }

    protected void scanVariable_at() {
        if (ch != '@') {
            throw new ParserException("illegal variable. " + info());
        }

        mark = pos;
        bufPos = 1;
        char ch;

        final char c1 = charAt(pos + 1);
        if (c1 == '@') {
            ++pos;
            bufPos++;
        }

        for (; ; ) {
            ch = charAt(++pos);

            if (!isIdentifierChar(ch)) {
                break;
            }

            pos += nchar - 1;
            bufPos += nchar;
            continue;
        }

        this.ch = charAt(pos);

        stringVal = addSymbol();
        token = Token.VARIANT;
    }

    public void scanComment() {
        if (!allowComment) {
            throw new NotAllowCommentException();
        }

        if ((ch == '/' && charAt(pos + 1) == '/')
            || (ch == '-' && charAt(pos + 1) == '-')) {
            scanSingleLineComment();
        } else if (ch == '/' && charAt(pos + 1) == '*') {
            scanMultiLineComment();
        } else {
            throw new IllegalStateException();
        }
    }

    protected final void scanHiveComment() {
        if (ch != '/' && ch != '-') {
            throw new IllegalStateException();
        }

        Token lastToken = this.token;

        mark = pos;
        bufPos = 0;
        scanChar();

        if (ch == ' ') {
            mark = pos;
            bufPos = 0;
            scanChar();
        }

        // /*+ */
        if (ch == '*') {
            scanChar();
            bufPos++;

            while (ch == ' ') {
                scanChar();
                bufPos++;
            }

            boolean isHint = false;
            int startHintSp = bufPos + 1;
            if (ch == '+') {
                isHint = true;
                scanChar();
                bufPos++;
            }

            for (; ; ) {
                char c2;
                if (ch == '*'
                    && ((c2 = charAt(pos + 1)) == '/' || (c2 == ' ' && charAt(pos + 2) == '/'))
                ) {
                    if (c2 == ' ') {
                        bufPos++;
                        scanChar();
                    }

                    bufPos += 2;
                    scanChar();
                    scanChar();
                    break;
                }

                scanChar();
                bufPos++;
            }

            if (isHint) {
                stringVal = subString(mark + startHintSp, (bufPos - startHintSp) - 1);
                token = Token.HINT;
            } else {
                stringVal = subString(mark, bufPos + 1);
                token = Token.MULTI_LINE_COMMENT;
                commentCount++;
                if (keepComments) {
                    addComment(stringVal);
                }
            }

            if (commentHandler != null && commentHandler.handle(lastToken, stringVal)) {
                return;
            }

            if (token != Token.HINT && !isAllowComment()) {
                throw new NotAllowCommentException();
            }

            return;
        }

        if (!isAllowComment()) {
            throw new NotAllowCommentException();
        }

        if (ch == '/' || ch == '-') {
            scanChar();
            bufPos++;

            for (; ; ) {
                if (ch == '\r') {
                    if (charAt(pos + 1) == '\n') {
                        line++;
                        bufPos += 2;
                        scanChar();
                        break;
                    }
                    bufPos++;
                    break;
                } else if (ch == EOI) {
                    break;
                }

                if (ch == '\n') {
                    line++;
                    scanChar();
                    bufPos++;
                    break;
                }

                scanChar();
                bufPos++;
            }

            stringVal = subString(mark, ch != EOI ? bufPos : bufPos + 1);
            token = Token.LINE_COMMENT;
            commentCount++;
            if (keepComments) {
                addComment(stringVal);
            }
            endOfComment = isEOF();

            if (commentHandler != null && commentHandler.handle(lastToken, stringVal)) {
                return;
            }

            return;
        }
    }

    private void scanMultiLineComment() {
        Token lastToken = this.token;
        int depth = 1;

        scanChar();
        scanChar();
        mark = pos;
        bufPos = 0;

        scan:
        for (; ; ) {
            if (ch == '/' && charAt(pos + 1) == '*') {
                scanChar();
                scanChar();
                if (ch == '!') {
                    // Version hint can nested.
                    scanChar();
                    ++depth;
                } else {
                    // Normal comment or optimizer hint, just scan to end.
                    for (; ; ) {
                        if (ch == '*' && charAt(pos + 1) == '/') {
                            scanChar();
                            scanChar();
                            if (1 == depth) {
                                // Single normal comment.
                                break scan;
                            }
                            continue scan;
                        }

                        if (ch == EOI) {
                            throw new ParserException("unterminated /* comment. " + info());
                        }
                        scanChar();
                        bufPos++;
                    }
                }
            }

            if (ch == '*' && charAt(pos + 1) == '/') {
                scanChar();
                scanChar();
                if (0 == --depth) {
                    break;
                }
            }

            // multiline comment结束符错误
            if (ch == EOI) {
                throw new ParserException("unterminated /* comment. " + info());
            }
            scanChar();
            bufPos++;
        }

        stringVal = subString(mark, bufPos);
        token = Token.MULTI_LINE_COMMENT;
        commentCount++;
        if (keepComments) {
            addComment(stringVal);
        }

        if (commentHandler != null && commentHandler.handle(lastToken, stringVal)) {
            return;
        }

        if (!isAllowComment() && !isSafeComment(stringVal)) {
            throw new NotAllowCommentException();
        }
    }

    private void scanSingleLineComment() {
        Token lastToken = this.token;

        mark = pos;
        bufPos = 2;

        scanChar();
        scanChar();

        for (; ; ) {
            if (ch == '\r') {
                if (charAt(pos + 1) == '\n') {
                    line++;
                    scanChar();
                    break;
                }
                bufPos++;
                break;
            }

            if (ch == '\n') {
                line++;
                scanChar();
                break;
            }

            if (ch == EOI) {
                break;
            }

            scanChar();
            bufPos++;
        }

        stringVal = subString(mark, bufPos);
        token = Token.LINE_COMMENT;
        commentCount++;
        if (keepComments) {
            addComment(stringVal);
        }

        if (commentHandler != null && commentHandler.handle(lastToken, stringVal)) {
            return;
        }

        if (!isAllowComment() && !isSafeComment(stringVal)) {
            throw new NotAllowCommentException();
        }
    }

    public void scanIdentifier() {
        this.hash_lower = 0;
        this.hash = 0;

        final char first = ch;

        if (ch == '`') {
            mark = pos;
            bufPos = 1;
            char ch;

            int startPos = pos + 1;
            int quoteIndex = text.indexOf('`', startPos);
            if (quoteIndex == -1) {
                throw new ParserException("illegal identifier. " + info());
            }

            hash_lower = 0xcbf29ce484222325L;
            hash = 0xcbf29ce484222325L;

            for (int i = startPos; i < quoteIndex; ++i) {
                ch = text.charAt(i);

                hash_lower ^= ((ch >= 'A' && ch <= 'Z') ? (ch + 32) : ch);
                hash_lower *= 0x100000001b3L;

                hash ^= ch;
                hash *= 0x100000001b3L;
            }

            stringVal = text.substring(pos, quoteIndex + 1);

            pos = quoteIndex + 1;
            this.ch = charAt(pos);
            token = Token.IDENTIFIER;
            return;
        }

        final boolean firstFlag = isFirstIdentifierChar(first);
        if (!firstFlag) {
            throw new ParserException("illegal identifier. " + info());
        }

        hash_lower = 0xcbf29ce484222325L;
        hash = 0xcbf29ce484222325L;

        hash_lower ^= ((ch >= 'A' && ch <= 'Z') ? (ch + 32) : ch);
        hash_lower *= 0x100000001b3L;

        hash ^= ch;
        hash *= 0x100000001b3L;

        mark = pos;
        bufPos = 1;

        int skip = lengthOf(ch, pos) - 1;
        while (skip-- > 0) {
            ch = text.charAt(++pos);
            bufPos++;

            hash_lower ^= ch;
            hash_lower *= 0x100000001b3L;

            hash ^= ch;
            hash *= 0x100000001b3L;
        }

        char ch = 0;
        for (; ; ) {
            char c0 = ch;
            ch = charAt(++pos);

            if (!isIdentifierChar(ch)) {
                if ((isLeftParen(ch) && ch != '(' || isRightParen(ch) && ch != ')') && c0 >= 0x80) {
                    bufPos++;

                    skip = lengthOf(ch, pos) - 1;
                    while (skip-- > 0) {
                        ch = text.charAt(++pos);
                        bufPos++;

                        hash_lower ^= ch;
                        hash_lower *= 0x100000001b3L;

                        hash ^= ch;
                        hash *= 0x100000001b3L;
                    }
                    continue;
                }
                break;
            }

            bufPos++;

            hash_lower ^= ((ch >= 'A' && ch <= 'Z') ? (ch + 32) : ch);
            hash_lower *= 0x100000001b3L;

            hash ^= ch;
            hash *= 0x100000001b3L;

            skip = lengthOf(ch, pos) - 1;
            while (skip-- > 0) {
                ch = text.charAt(++pos);
                bufPos++;

                hash_lower ^= ch;
                hash_lower *= 0x100000001b3L;

                hash ^= ch;
                hash *= 0x100000001b3L;
            }
            continue;
        }

        this.ch = charAt(pos);

        if (bufPos == 1) {
            token = Token.IDENTIFIER;
            stringVal = CharTypes.valueOf(first);
            if (stringVal == null) {
                stringVal = Character.toString(first);
            }
            return;
        }

        Token tok = keywods.getKeyword(hash_lower);
        if (tok != null) {
            token = tok;
            if (token == Token.IDENTIFIER) {
                stringVal = text.substring(mark, mark + bufPos);
            } else {
                stringVal = null;
            }
        } else {
            token = Token.IDENTIFIER;
            stringVal = text.substring(mark, mark + bufPos);
        }
    }

    public void scanNumber() {
        mark = pos;
        numberSale = 0;
        numberExp = false;
        bufPos = 0;

        if (ch == '0' && charAt(pos + 1) == 'b') {
            int i = 2;
            int mark = pos + 2;
            for (; ; ++i) {
                char ch = charAt(pos + i);
                if (ch == '0' || ch == '1') {
                    continue;
                } else if (ch >= '2' && ch <= '9') {
                    break;
                } else {
                    bufPos += i;
                    pos += i;
                    stringVal = subString(mark, i - 2);
                    this.ch = charAt(pos);
                    token = Token.BITS;
                    return;
                }
            }
        }

        if (ch == '-') {
            bufPos++;
            ch = charAt(++pos);
        }

        for (; ; ) {
            if (ch >= '0' && ch <= '9') {
                bufPos++;
            } else {
                break;
            }
            ch = charAt(++pos);
        }

        if (ch == '.') {
            if (charAt(pos + 1) == '.') {
                token = Token.LITERAL_INT;
                return;
            }
            bufPos++;
            ch = charAt(++pos);

            for (numberSale = 0; ; numberSale++) {
                if (ch >= '0' && ch <= '9') {
                    bufPos++;
                } else {
                    break;
                }
                ch = charAt(++pos);
            }

            numberExp = true;
        }

        if ((ch == 'e' || ch == 'E')
            && (isDigit(charAt(pos + 1)) || (isDigit2(charAt(pos + 1)) && isDigit2(charAt(pos + 2))))) {
            numberExp = true;

            bufPos++;
            ch = charAt(++pos);

            if (ch == '+' || ch == '-') {
                bufPos++;
                ch = charAt(++pos);
            }

            for (; ; ) {
                if (ch >= '0' && ch <= '9') {
                    bufPos++;
                } else {
                    break;
                }
                ch = charAt(++pos);
            }

            if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
                numberExp = false;
            }
        }

        if (numberSale > 0 || numberExp) {
            token = Token.LITERAL_FLOAT;
        } else {
            if (isFirstIdentifierChar(ch)
                && !isRightParen(ch)
                && !(ch == 'b' && bufPos == 1 && charAt(pos - 1) == '0')
            ) {
                if (isFirstIdentifierChar(ch)) {
                    bufPos += nchar - 1;
                    pos += nchar - 1;
                }
                bufPos++;
                for (; ; ) {
                    char c0 = ch;
                    ch = charAt(++pos);

                    if (!isIdentifierChar(ch)) {
                        if ((isLeftParen(ch) && ch != '(' || isRightParen(ch) && ch != ')') && c0 >= 0x80) {
                            pos += nchar - 1;
                            bufPos += nchar;
                            continue;
                        }
                        break;
                    }

                    pos += nchar - 1;
                    bufPos += nchar;
                    continue;
                }

                stringVal = addSymbol();
                hash_lower = FnvHash.hashCode64(stringVal);
                token = Token.IDENTIFIER;
            } else {
                token = Token.LITERAL_INT;
            }
        }
    }

    public void scanHexaDecimal() {
        mark = pos;

        if (ch == '-') {
            bufPos++;
            ch = charAt(++pos);
        }

        for (; ; ) {
            if (CharTypes.isHex(ch)) {
                bufPos++;
            } else {
                break;
            }
            ch = charAt(++pos);
        }

        token = Token.LITERAL_HEX;
    }

    public String hexString() {
        return subString(mark, bufPos);
    }

    public final boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }

    protected static final boolean isDigit2(char ch) {
        return ch == '+' || ch == '-' || (ch >= '0' && ch <= '9');
    }

    /**
     * Append a character to sbuf.
     */
    protected final void putChar(char ch) {
        if (bufPos == buf.length) {
            byte[] newsbuf = new byte[buf.length * 2];
            System.arraycopy(buf, 0, newsbuf, 0, buf.length);
            buf = newsbuf;
        }
        buf[bufPos++] = (byte) ch;
    }

    protected final void putUnicodeChar(int codepoint) {
        char[] chars = Character.toChars(codepoint);
        byte[] bytes = new String(chars).getBytes(charset);
        for (byte b : bytes) {
            putChar((char) (b & 0xFF));
        }
    }

    /**
     * Return the current token's position: a 0-based offset from beginning of the raw input stream (before unicode
     * translation)
     */
    public final int pos() {
        return pos;
    }

    /**
     * The value of a literal token, recorded as a string. For integers, leading 0x and 'l' suffixes are suppressed.
     */
    public final String stringVal() {
        if (stringVal == null) {
            stringVal = subString(mark, bufPos);
        }
        return stringVal;
    }

    public final byte[] binaryVal() {
        if (binaryVal == null) {
            byte[] bytes = new byte[bufPos];
            arraycopy(mark, bytes, 0, bufPos);
            return bytes;
        }
        return binaryVal;
    }

    private final void stringVal(StringBuffer out) {
        if (stringVal != null) {
            out.append(stringVal);
            return;
        }

        out.append(text.substring(mark, mark + bufPos));
    }

    public final boolean identifierEquals(String text) {
        if (token != Token.IDENTIFIER) {
            return false;
        }

        if (stringVal == null) {
            stringVal = subString(mark, bufPos);
        }
        return text.equalsIgnoreCase(stringVal);
    }

    public final boolean identifierEquals(long hash_lower) {
        if (token != Token.IDENTIFIER) {
            return false;
        }

        if (this.hash_lower == 0) {
            if (stringVal == null) {
                stringVal = subString(mark, bufPos);
            }
            this.hash_lower = FnvHash.fnv1a_64_lower(stringVal);
        }
        return this.hash_lower == hash_lower;
    }

    public final long hash_lower() {
        if (this.hash_lower == 0) {
            if (stringVal == null) {
                stringVal = subString(mark, bufPos);
            }
            this.hash_lower = FnvHash.fnv1a_64_lower(stringVal);
        }
        return hash_lower;
    }

    public final List<String> readAndResetComments() {
        List<String> comments = this.comments;

        this.comments = null;

        return comments;
    }

    private boolean isOperator(char ch) {
        switch (ch) {
        case '!':
        case '%':
        case '&':
        case '*':
        case '+':
        case '-':
        case '<':
        case '=':
        case '>':
        case '^':
        case '|':
        case '~':
        case ';':
            return true;
        default:
            return false;
        }
    }

    private static final long MULTMIN_RADIX_TEN = Long.MIN_VALUE / 10;
    private static final long N_MULTMAX_RADIX_TEN = -Long.MAX_VALUE / 10;

    private final static int[] digits = new int[(int) '9' + 1];

    static {
        for (int i = '0'; i <= '9'; ++i) {
            digits[i] = i - '0';
        }
    }

    public final boolean isNegativeIntegerValue() {
        return charAt(mark) == '-';
    }

    // QS_TODO negative number is invisible for lexer
    public final Number integerValue() {
        long result = 0;
        boolean negative = false;
        int i = mark, max = mark + bufPos;
        long limit;
        long multmin;
        int digit;

        if (charAt(mark) == '-') {
            negative = true;
            limit = Long.MIN_VALUE;
            i++;
        } else {
            limit = -Long.MAX_VALUE;
        }
        multmin = negative ? MULTMIN_RADIX_TEN : N_MULTMAX_RADIX_TEN;
        if (i < max) {
            digit = charAt(i++) - '0';
            result = -digit;
        }
        while (i < max) {
            // Accumulating negatively avoids surprises near MAX_VALUE
            digit = charAt(i++) - '0';
            if (result < multmin) {
                return new BigInteger(
                    numberString());
            }
            result *= 10;
            if (result < limit + digit) {
                return new BigInteger(numberString());
            }
            result -= digit;
        }

        if (negative) {
            if (i > mark + 1) {
                if (result >= Integer.MIN_VALUE) {
                    return (int) result;
                }
                return result;
            } else { /* Only got "-" */
                throw new NumberFormatException(numberString());
            }
        } else {
            result = -result;
            if (result <= Integer.MAX_VALUE) {
                return (int) result;
            }
            return result;
        }
    }

    public int bp() {
        return this.pos;
    }

    public char current() {
        return this.ch;
    }

    public void reset(int mark, char markChar, Token token) {
        this.pos = mark;
        this.ch = markChar;
        this.token = token;
    }

    public final String numberString() {
        return text.substring(mark, mark + bufPos);
        // return subString(mark, bufPos);
    }

    public BigDecimal decimalValue() {
        if (numberSale > 0 && !numberExp) {
            int len = bufPos;

            if (len < 20) {
                long unscaleVal = 0;

                boolean negative = false;
                int i = 0;
                char first = text.charAt(mark);
                if (first == '+') {
                    i++;
                } else if (first == '-') {
                    i++;
                    negative = true;
                }
                for (; i < len; ++i) {
                    char ch = text.charAt(mark + i);
                    if (ch == '.') {
                        continue;
                    }
                    int digit = ch - '0';
                    unscaleVal = unscaleVal * 10 + digit;
                }
                return BigDecimal.valueOf(negative ? -unscaleVal : unscaleVal, numberSale);
            }
        }

        char[] value = sub_chars(mark, bufPos);
        if (!StringUtils.isNumber(value)) {
            throw new ParserException(value + " is not a number! " + info());
        }
        return new BigDecimal(value);
    }

    public SQLNumberExpr numberExpr() {
        char[] value = sub_chars(mark, bufPos);
        if (!StringUtils.isNumber(value)) {
            throw new ParserException(value + " is not a number! " + info());
        }

        return new SQLNumberExpr(value);
    }

    public SQLNumberExpr numberExpr(SQLObject parent) {
        char[] value = sub_chars(mark, bufPos);
        if (!StringUtils.isNumber(value)) {
            throw new ParserException(value + " is not a number! " + info());
        }

        return new SQLNumberExpr(value, parent);
    }

    public SQLNumberExpr numberExpr(boolean negate) {
        char[] value = sub_chars(mark, bufPos);
        if (!StringUtils.isNumber(value)) {
            throw new ParserException(value + " is not a number! " + info());
        }

        if (negate) {
            char[] chars = new char[value.length + 1];
            chars[0] = '-';
            System.arraycopy(value, 0, chars, 1, value.length);
            return new SQLNumberExpr(chars);
        } else {
            return new SQLNumberExpr(value);
        }
    }

    public static interface CommentHandler {
        boolean handle(Token lastToken, String comment);
    }

    public boolean hasComment() {
        return comments != null;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void skipToEOF() {
        pos = text.length();
        this.token = Token.EOF;
    }

    public boolean isEndOfComment() {
        return endOfComment;
    }

    protected boolean isSafeComment(String comment) {
        if (comment == null) {
            return true;
        }
        comment = comment.toLowerCase();
        if (comment.indexOf("select") != -1 //
            || comment.indexOf("delete") != -1 //
            || comment.indexOf("insert") != -1 //
            || comment.indexOf("update") != -1 //
            || comment.indexOf("into") != -1 //
            || comment.indexOf("where") != -1 //
            || comment.indexOf("or") != -1 //
            || comment.indexOf("and") != -1 //
            || comment.indexOf("union") != -1 //
            || comment.indexOf('\'') != -1 //
            || comment.indexOf('=') != -1 //
            || comment.indexOf('>') != -1 //
            || comment.indexOf('<') != -1 //
            || comment.indexOf('&') != -1 //
            || comment.indexOf('|') != -1 //
            || comment.indexOf('^') != -1 //
        ) {
            return false;
        }
        return true;
    }

    protected void addComment(String comment) {
        if (comments == null) {
            comments = new ArrayList<String>(2);
        }
        comments.add(comment);
    }

    public List<String> getComments() {
        return comments;
    }

    public int getLine() {
        return line;
    }

    public void computeRowAndColumn() {
        int line = 1;
        int column = 1;
        for (int i = 0; i < startPos; ++i) {
            char ch = text.charAt(i);
            if (ch == '\n') {
                column = 0;
                line++;
            } else {
                column++;
            }
        }

        this.posLine = line;
        this.posColumn = column;
    }

    public int getPosLine() {
        return posLine;
    }

    public int getPosColumn() {
        return posColumn;
    }

    public void config(SQLParserFeature feature, boolean state) {
        features = SQLParserFeature.config(features, feature, state);

        if (feature == OptimizedForParameterized) {
            optimizedForParameterized = state;
        } else if (feature == KeepComments) {
            this.keepComments = state;
        } else if (feature == KeepSourceLocation) {
            this.keepSourceLocaltion = state;
        } else if (feature == SkipComments) {
            this.skipComment = state;
        }
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public final boolean isEnabled(SQLParserFeature feature) {
        return SQLParserFeature.isEnabled(this.features, feature);
    }

    public static String parameterize(String sql, DbType dbType) {
        Lexer lexer = SQLParserUtils.createLexer(sql, dbType);
        lexer.optimizedForParameterized = true; // optimized

        lexer.nextToken();

        StringBuffer buf = new StringBuffer();

        for_:
        for (; ; ) {
            Token token = lexer.token;
            switch (token) {
            case LITERAL_ALIAS:
            case LITERAL_FLOAT:
            case LITERAL_CHARS:
            case LITERAL_INT:
            case LITERAL_NCHARS:
            case LITERAL_HEX:
            case VARIANT:
                if (buf.length() != 0) {
                    buf.append(' ');
                }
                buf.append('?');
                break;
            case COMMA:
                buf.append(',');
                break;
            case EQ:
                buf.append('=');
                break;
            case EOF:
                break for_;
            case ERROR:
                return sql;
            case SELECT:
                buf.append("SELECT");
                break;
            case UPDATE:
                buf.append("UPDATE");
                break;
            default:
                if (buf.length() != 0) {
                    buf.append(' ');
                }
                lexer.stringVal(buf);
                break;
            }

            lexer.nextToken();
        }

        return buf.toString();
    }

    public String getSource() {
        return text.toString();
    }

    public boolean isLexSingleColon() {
        return lexSingleColon;
    }

    public void setLexSingleColon(boolean lexSingleColon) {
        this.lexSingleColon = lexSingleColon;
    }

    /**
     * Length of special character, e.g. full-width whitespace or identifier character
     * <p/>
     * Must be set if <code>isIdentifierChar<code/>, <code>isFirstIdentifierChar<code/> or <code>isWhitespace</code>
     * returns true
     */
    protected int nchar = 1;

    /**
     * @see CharTypes#isWhitespace(char)
     */
    protected boolean isWhitespace(char c) {
        assert c == charAt(pos);
        return isWhitespace(c, pos);
    }

    protected boolean isWhitespace(char c, int pos) {
        if (c < 0x80) {
            nchar = 1;
            return CharTypes.isWhitespace(c);
        }
        if (charset == UTF8) {
            return isUtf8Whitespace(c, pos);
        } else if (charset == GBK) {
            return isGbkWhitespace(c, pos);
        } else if (charset == LATIN1) {
            return CharTypes.isWhitespace(c);
        }
        return false;
    }

    private boolean isUtf8Whitespace(char c1, int pos) {
        assert c1 == text.charAt(pos);
        if (c1 == 0xC2) {
            // control characters (\u0080 ~ \u00A0)
            if (pos + 1 < text.length()) {
                char c2 = text.charAt(pos + 1);
                if (c2 >= 0x80 && c2 <= 0xA0) {
                    nchar = 2;
                    return true;
                }
            }
        } else if (c1 == 0xE3) {
            // ideographic space '　' (E3 80 80)
            if (pos + 2 < text.length()) {
                if (text.charAt(pos + 1) == 0x80 && text.charAt(pos + 2) == 0x80) {
                    nchar = 3;
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isGbkWhitespace(char c1, int pos) {
        assert c1 == text.charAt(pos);
        if (pos + 1 >= text.length()) {
            throw new IllegalArgumentException("illegal gbk string");
        }
        char c2 = text.charAt(pos + 1);
        if (c1 == 0xA1 && c2 == 0xA1) {
            nchar = 2;
            return true; // ideographic space '　' (A1 A1)
        }
        return false;
    }

    /**
     * @see CharTypes#isIdentifierChar(char)
     */
    protected boolean isIdentifierChar(char c) {
        if (c < 0x80) {
            nchar = 1;
            return CharTypes.isIdentifierChar(c);
        }
        if (charset == UTF8) {
            return isUtf8IdentifierChar(c, pos);
        } else if (charset == GBK) {
            return isGbkIdentifierChar(c, pos);
        } else if (charset == LATIN1) {
            return CharTypes.isIdentifierChar(c);
        }
        return false;
    }

    private boolean isUtf8IdentifierChar(char c1, int pos) {
        assert c1 == text.charAt(pos);
        if (c1 == 0xC2) {
            // \u00A0 ~ \u00FF -> false
            return false;
        } else if (c1 == 0xC3) {
            // \u00A0 ~ \u00FF -> false
            if (pos + 1 < text.length()) {
                char c2 = text.charAt(pos + 1);
                if (c2 <= 0xBF) {
                    return false;
                }
            }
        } else if (c1 == 0xE3) {
            // ideographic space '　' (E3 80 80)
            if (pos + 2 < text.length()) {
                if (text.charAt(pos + 1) == 0x80 && text.charAt(pos + 2) == 0x80) {
                    return false;
                }
            }
        } else if (c1 == 0xEF) {
            if (pos + 2 < text.length() && text.charAt(pos + 1) == 0xBC) {
                char c3 = text.charAt(pos + 2);
                if (this instanceof MySqlLexer) {
                    if (c3 == 0x8C) {
                        // full-width comma or paren '，' (EF BC 8C)
                        return false;
                    }
                } else {
                    if (c3 == 0x8C || c3 == 0x89 || c3 == 0x88) {
                        // full-width comma or paren '，' '）' '（'
                        return false;
                    }
                }
            }
        }
        nchar = lengthOf(c1, pos);
        return true;
    }

    private boolean isGbkIdentifierChar(char c1, int pos) {
        assert c1 == text.charAt(pos);
        if (pos + 1 >= text.length()) {
            throw new IllegalArgumentException("illegal gbk string");
        }
        char c2 = text.charAt(pos + 1);
        if (c1 == 0xA1 && c2 == 0xA1) {
            return false; // ideographic space '　' (A1 A1)
        } else if (c1 == 0xA3 && c2 == 0xAC) {
            return false; // full-width comma '，' (A3 AC)
        } else if (!(this instanceof MySqlLexer)) {
            if (c1 == 0xA3 && c2 == 0xA8) {
                return false; // full-width left paren (A3 A8)
            } else if (c1 == 0xA3 && c2 == 0xA9) {
                return false; // full-width right paren (A3 A9)
            }
        }
        nchar = 2;
        return true;
    }

    /**
     * @see CharTypes#isFirstIdentifierChar(char)
     */
    protected boolean isFirstIdentifierChar(char c) {
        if (c < 0x80) {
            nchar = 1;
            return CharTypes.isFirstIdentifierChar(c);
        }
        if (charset == UTF8) {
            return isUtf8FirstIdentifierChar(c, pos);
        } else if (charset == GBK) {
            return isGbkFirstIdentifierChar(c, pos);
        } else if (charset == LATIN1) {
            return CharTypes.isFirstIdentifierChar(c);
        }
        return false;
    }

    private boolean isUtf8FirstIdentifierChar(char c1, int pos) {
        assert c1 == text.charAt(pos);
        if (c1 == 0xC2) {
            // \u00A0 ~ \u00FF -> false
            return false;
        } else if (c1 == 0xC3) {
            // \u00A0 ~ \u00FF -> false
            if (pos + 1 < text.length()) {
                char c2 = text.charAt(pos + 1);
                if (c2 <= 0xBF) {
                    return false;
                }
            }
        } else if (c1 == 0xE3) {
            // ideographic space '　' (E3 80 80)
            if (pos + 2 < text.length()) {
                if (text.charAt(pos + 1) == 0x80 && text.charAt(pos + 2) == 0x80) {
                    return false;
                }
            }
        } else if (c1 == 0xEF) {
            if (pos + 2 < text.length()) {
                if (text.charAt(pos + 1) == 0xBC && text.charAt(pos + 2) == 0x8C) {
                    return false; // full-width comma '，'
                }
            }
        }
        nchar = lengthOf(c1, pos);
        return true;
    }

    private boolean isGbkFirstIdentifierChar(char c1, int pos) {
        assert c1 == text.charAt(pos);
        if (pos + 1 >= text.length()) {
            throw new IllegalArgumentException("illegal gbk string");
        }
        char c2 = text.charAt(pos + 1);
        if (c1 == 0xA1 && c2 == 0xA1) {
            return false; // ideographic space '　' (A1 A1)
        } else if (c1 == 0xA3 && c2 == 0xAC) {
            return false; // full-width comma '，' (A3 AC)
        }
        nchar = 2;
        return true;
    }

    private boolean isLeftParen(char c) {
        if (c < 0x80) {
            nchar = 1;
            return c == '(';
        }
        assert c == text.charAt(pos);
        if (charset == UTF8) {
            if (pos + 2 < text.length()
                && text.charAt(pos) == 0xEF && text.charAt(pos + 1) == 0xBC && text.charAt(pos + 2) == 0x88) {
                nchar = 3;
                return true;
            }
        } else if (charset == GBK) {
            if (pos + 1 < text.length()
                && text.charAt(pos) == 0xA3 && text.charAt(pos + 1) == 0xA8) {
                nchar = 2;
                return true;
            }
        }
        return false;
    }

    private boolean isRightParen(char c) {
        return isRightParen(c, pos);
    }

    private boolean isRightParen(char c, int pos) {
        if (c < 0x80) {
            nchar = 1;
            return c == ')';
        }
        assert c == text.charAt(pos);
        if (charset == UTF8) {
            if (pos + 2 < text.length() &&
                text.charAt(pos) == 0xEF && text.charAt(pos + 1) == 0xBC && text.charAt(pos + 2) == 0x89) {
                nchar = 3;
                return true;
            }
        } else if (charset == GBK) {
            if (pos + 1 < text.length() &&
                text.charAt(pos) == 0xA3 && text.charAt(pos + 1) == 0xA9) {
                nchar = 2;
                return true;
            }
        }
        return false;
    }

    private boolean isComma(char c) {
        if (c < 0x80) {
            nchar = 1;
            return c == ',';
        }
        assert c == text.charAt(pos);
        if (charset == UTF8) {
            if (pos + 2 < text.length() &&
                text.charAt(pos) == 0xEF && text.charAt(pos + 1) == 0xBC && text.charAt(pos + 2) == 0x8C) {
                nchar = 3;
                return true;
            }
        } else if (charset == GBK) {
            if (pos + 1 < text.length() &&
                text.charAt(pos) == 0xA3 && text.charAt(pos + 1) == 0xAC) {
                nchar = 2;
                return true;
            }
        }
        return false;
    }

    private boolean isZeroWidthSpace(char c) {
        if (c == EOI) {
            return false;
        }
        assert c == text.charAt(pos);
        if (charset == UTF8) {
            if (pos + 2 < text.length() &&
                text.charAt(pos) == 0xE2 && text.charAt(pos + 1) == 0x80 && text.charAt(pos + 2) == 0x8B) {
                nchar = 3;
                return true;
            }
        }
        return false;
    }

    protected int lengthOf(char ch, int pos) {
        if (hash_lower == FnvHash.Constants._BINARY) {
            return 1;
        }
        if (charset == UTF8) {
            if ((ch & 0x80) == 0) {
                return 1;
            }
            if ((ch & 0xE0) == 0xC0) {
                return 2;
            }
            if ((ch & 0xF0) == 0xE0) {
                return 3;
            }
            if ((ch & 0xF8) == 0xF0) {
                return 4;
            }
            if ((ch & 0xFC) == 0xF8) {
                return 5;
            }
            if ((ch & 0xFE) == 0xFC) {
                return 6;
            }
            return 1; // illegal utf-8 bytes, just skip it
        } else if (charset == GBK) {
            if (ch < 0x80) {
                return 1;
            } else {
                return 2;
            }
        }
        return 1;
    }

    public int getStartPos() {
        return startPos;
    }
}
