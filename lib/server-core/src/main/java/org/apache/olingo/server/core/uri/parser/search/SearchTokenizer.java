/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.olingo.server.core.uri.parser.search;

import java.util.ArrayList;
import java.util.List;

/**
 * <code>
 * searchExpr = ( OPEN BWS searchExpr BWS CLOSE
 *  / searchTerm
 *  ) [ searchOrExpr
 *  / searchAndExpr
 *  ]
 *
 *  searchOrExpr  = RWS 'OR'  RWS searchExpr
 *  searchAndExpr = RWS [ 'AND' RWS ] searchExpr
 *
 *  searchTerm   = [ 'NOT' RWS ] ( searchPhrase / searchWord )
 *  searchPhrase = quotation-mark 1*qchar-no-AMP-DQUOTE quotation-mark
 *  searchWord   = 1*ALPHA ; Actually: any character from the Unicode categories L or Nl,
 *  ; but not the words AND, OR, and NOT
 * </code>
 */
public class SearchTokenizer {

  private static abstract class State implements SearchQueryToken {
    private Token token = null;
    private boolean finished = false;

    protected static final char QUOTATION_MARK = '\"';
    protected static final char CHAR_N = 'N';
    protected static final char CHAR_O = 'O';
    protected static final char CHAR_T = 'T';
    protected static final char CHAR_A = 'A';
    protected static final char CHAR_D = 'D';
    protected static final char CHAR_R = 'R';
    protected static final char CHAR_CLOSE = ')';
    protected static final char CHAR_OPEN = '(';

    public State(Token t) {
      token = t;
    }

    protected abstract State nextChar(char c);

    public State allowed(char c) {
      return this;
    }

    public State forbidden(char c) {
      throw new IllegalStateException(this.getClass().getName() + "->" + c);
    }

    public State finish() {
      this.finished = true;
      return this;
    }

    public boolean isFinished() {
      return finished;
    }

    public Token getToken() {
      return token;
    }

    public State close() {
      return this;
    }

    static boolean isAllowedChar(final char character) {
      // TODO mibo: add missing allowed characters
      return CHAR_A <= character && character <= 'Z' // case A..Z
          || 'a' <= character && character <= 'z' // case a..z
          || '0' <= character && character <= '9'; // case 0..9
    }

    /**
     * qchar-no-AMP-DQUOTE   = qchar-unescaped / escape ( escape / quotation-mark )
     * qchar-unescaped  = unreserved / pct-encoded-unescaped / other-delims / ":" / "@" / "/" / "?" / "$" / "'" / "="
     * unreserved    = ALPHA / DIGIT / "-" / "." / "_" / "~"
     * @param character which is checked
     * @return true if character is allowed for a phrase
     */
    static boolean isAllowedPhrase(final char character) {
      // FIXME mibo: check missing and '\''
      return isAllowedChar(character)
          || character == '-'
          || character == '.'
          || character == '_'
          || character == '~'
          || character == ':'
          || character == '@'
          || character == '/'
          || character == '$'
          || character == '=';
    }

    //BWS =  *( SP / HTAB / "%20" / "%09" )  ; "bad" whitespace
    //RWS = 1*( SP / HTAB / "%20" / "%09" )  ; "required" whitespace
    static boolean isWhitespace(final char character) {
      //( SP / HTAB / "%20" / "%09" )
      // TODO mibo: add missing whitespaces
      return character == ' ' || character == '\t';
    }

    @Override
    public String getLiteral() {
      return token.toString();
    }

    @Override
    public String toString() {
      return this.getToken().toString() + "=>{" + getLiteral() + "}";
    }
  }

  private static abstract class LiteralState extends State {
    protected final StringBuilder literal = new StringBuilder();
    public LiteralState(Token t) {
      super(t);
    }
    public LiteralState(Token t, char c) {
      super(t);
      init(c);
    }
    public LiteralState(Token t, String initLiteral) {
      super(t);
      literal.append(initLiteral);
    }
    public State allowed(char c) {
      literal.append(c);
      return this;
    }
    @Override
    public String getLiteral() {
      return literal.toString();
    }

    public State init(char c) {
      if(isFinished()) {
        throw new IllegalStateException(toString() + " is already finished.");
      }
      literal.append(c);
      return this;
    }
  }

  private class SearchExpressionState extends LiteralState {
    public SearchExpressionState() {
      super(null);
    }
    @Override
    public State nextChar(char c) {
      if (c == CHAR_OPEN) {
        return new OpenState();
      } else if (isWhitespace(c)) {
        return new RwsImplicitAndState();
      } else if(c == CHAR_CLOSE) {
        return new CloseState();
      } else {
        return new SearchTermState().init(c);
      }
    }

    @Override
    public State init(char c) {
      return nextChar(c);
    }
  }

  private class SearchTermState extends LiteralState {
    public SearchTermState() {
      super(Token.TERM);
    }
    @Override
    public State nextChar(char c) {
      if(c == CHAR_N) {
        return new NotState(c);
      } else if (c == QUOTATION_MARK) {
        return new SearchPhraseState(c);
      } else if (isAllowedChar(c)) {
        return new SearchWordState(c);
      }
      throw new IllegalStateException(this.getClass().getName() + "->" + c);
    }
    @Override
    public State init(char c) {
      return nextChar(c);
    }
  }

  private class SearchWordState extends LiteralState {
    public SearchWordState(char c) {
      super(Token.WORD, c);
    }
    public SearchWordState(State toConsume) {
      super(Token.WORD, toConsume.getLiteral());
    }

    @Override
    public State nextChar(char c) {
      if (isAllowedChar(c)) {
        return allowed(c);
      } else if (c == CHAR_CLOSE) {
        finish();
        return new CloseState();
      } else if (isWhitespace(c)) {
        finish();
        return new RwsImplicitAndState();
      }
      return forbidden(c);
    }

    @Override
    public State close() {
      return finish();
    }
  }

  private class SearchPhraseState extends LiteralState {
    public SearchPhraseState(char c) {
      super(Token.PHRASE, c);
      if(c != QUOTATION_MARK) {
        forbidden(c);
      }
    }

    @Override
    public State nextChar(char c) {
      if(isFinished()) {
        return new SearchExpressionState().init(c);
      } else if (isAllowedPhrase(c)) {
        return allowed(c);
      } else if (c == QUOTATION_MARK) {
        finish();
        allowed(c);
        return new SearchExpressionState();
      } else if (isWhitespace(c)) {
        if(isFinished()) {
          return new RwsImplicitAndState();
        }
        return allowed(c);
      } else if (c == CHAR_CLOSE) {
        if(isFinished()) {
          return new CloseState();
        }
        return allowed(c);
      }
      return forbidden(c);
    }
  }

  private class OpenState extends State {
    public OpenState() {
      super(Token.OPEN);
      finish();
    }
    @Override
    public State nextChar(char c) {
      finish();
      if (isWhitespace(c)) {
        return forbidden(c);
      }
      return new SearchExpressionState().init(c);
    }
  }

  private class CloseState extends State {
    public CloseState() {
      super(Token.CLOSE);
      finish();
    }

    @Override
    public State nextChar(char c) {
      return new SearchExpressionState().init(c);
    }
  }

  private class NotState extends LiteralState {
    public NotState(char c) {
      super(Token.NOT, c);
      if(c != CHAR_N) {
        forbidden(c);
      }
    }
    @Override
    public State nextChar(char c) {
      if (getLiteral().length() == 1 && c == CHAR_O) {
        return allowed(c);
      } else if (getLiteral().length() == 2 && c == CHAR_T) {
        return allowed(c);
      } else if(getLiteral().length() == 3 && isWhitespace(c)) {
        finish();
        return new BeforeSearchExpressionRwsState();
      } else {
        return new SearchWordState(this);
      }
    }
  }

  private class AndState extends LiteralState {
    public AndState(char c) {
      super(Token.AND, c);
      if(c != CHAR_A) {
        forbidden(c);
      }
    }
    @Override
    public State nextChar(char c) {
      if (literal.length() == 1 && c == CHAR_N) {
        return allowed(c);
      } else if (literal.length() == 2 && c == CHAR_D) {
        return allowed(c);
      } else if(literal.length() == 3 && isWhitespace(c)) {
        finish();
        return new BeforeSearchExpressionRwsState();
      } else {
        return new SearchWordState(this);
      }
    }
  }

  private class OrState extends LiteralState {
    public OrState(char c) {
      super(Token.OR, c);
      if(c != CHAR_O) {
        forbidden(c);
      }
    }
    @Override
    public State nextChar(char c) {
      if (literal.length() == 1 && (c == CHAR_R)) {
        return allowed(c);
      } else if(literal.length() == 2 && isWhitespace(c)) {
        finish();
        return new BeforeSearchExpressionRwsState();
      } else {
        return new SearchWordState(this);
      }
    }
  }

  // RWS 'OR'  RWS searchExpr
  // RWS [ 'AND' RWS ] searchExpr
  private class BeforeSearchExpressionRwsState extends State {
    public BeforeSearchExpressionRwsState() {
      super(null);
    }
    @Override
    public State nextChar(char c) {
      if (isWhitespace(c)) {
        return allowed(c);
      } else {
        return new SearchExpressionState().init(c);
      }
    }
  }

  // implicit and
  private class RwsImplicitAndState extends State {
    public RwsImplicitAndState() {
      super(Token.AND);
    }
    @Override
    public State nextChar(char c) {
      if (isWhitespace(c)) {
        return allowed(c);
      } else if (c == CHAR_O) {
        return new OrState(c);
      } else if (c == CHAR_A) {
        return new AndState(c);
      } else {
        finish();
        return new SearchExpressionState().init(c);
      }
    }
  }

  // TODO (mibo): add (new) parse exception
  public List<SearchQueryToken> tokenize(String searchQuery) {
    char[] chars = searchQuery.toCharArray();

    State state = new SearchExpressionState();
    List<SearchQueryToken> states = new ArrayList<SearchQueryToken>();
    for (char aChar : chars) {
      State next = state.nextChar(aChar);
      if (state.isFinished()) {
        states.add(state);
      }
      state = next;
    }

    if(state.close().isFinished()) {
      states.add(state);
    }

    return states;
  }
}