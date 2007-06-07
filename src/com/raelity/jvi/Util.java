/**
 * Title:        jVi<p>
 * Description:  A VI-VIM clone.
 * Use VIM as a model where applicable.<p>
 * Copyright:    Copyright (c) Ernie Rael<p>
 * Company:      Raelity Engineering<p>
 * @author Ernie Rael
 * @version 1.0
 */

/*
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 * 
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 * 
 * The Original Code is jvi - vi editor clone.
 * 
 * The Initial Developer of the Original Code is Ernie Rael.
 * Portions created by Ernie Rael are
 * Copyright (C) 2000 Ernie Rael.  All Rights Reserved.
 * 
 * Contributor(s): Ernie Rael <err@raelity.com>
 */
package com.raelity.jvi;

import java.awt.Toolkit;
import java.text.CharacterIterator;
import javax.swing.text.BadLocationException;

import com.raelity.text.TextUtil.MySegment;

public class Util {
  // static final int TERMCAP2KEY(int a, int b) { return a + (b << 8); }
  // NEEDSWORK: CHAR
  static final int ctrl(int x) { return x & 0x1f; }
  // static final int shift(int c) { return c | (0x1 << 24); }
  // static void stuffcharReadbuff(int c) {}

  /** position to end of line. */
  static void endLine() {
    ViFPOS fpos = G.curwin.getWCursor();
    int offset = G.curwin
	      		.getLineEndOffsetFromOffset(fpos.getOffset());
    // assumes there is at least one char in line, could be a '\n'
    offset--;	// point at last char of line
    if(Util.getCharAt(offset) != '\n') {
      offset++; // unlikely
    }
    G.curwin.setCaretPosition(offset);
  }

  public static void vim_beep() {
    Toolkit.getDefaultToolkit().beep();
  }

  /** 
   * Returns the substring of c in s or null if c not part of s.
   * @param s the string to search in
   * @param c the character to search for
   * @return the substring of c in s or null if c not part of s.
   */
  public static String vim_strchr(String s, int c) {
    int index = s.indexOf(c);
    if(index < 0) {
      return null;
    }
    return s.substring(index);
  }

  public static final boolean isalnum(int regname) {
    return	regname >= '0' && regname <= '9'
    		|| regname >= 'a' && regname <= 'z'
    		|| regname >= 'A' && regname <= 'Z';
  }

  public static final boolean isalpha(int c) {
    return	   c >= 'a' && c <= 'z'
    		|| c >= 'A' && c <= 'Z';
  }

  public static boolean islower(int c) {
    return 'a' <= c && c <= 'z';
  }

 public static int tolower(int c) {
   if(isupper(c)) {
     c |= 0x20;
   }
   return c;
 }

  static boolean isupper(int c) {
    return 'A' <= c && c <= 'Z';
  }

 static int toupper(int c) {
   if(islower(c)) {
     c &= ~0x20;
   }
   return c;
 }

  public static boolean isdigit(int c) {
    return '0' <= c && c <= '9';
  }

  static boolean vim_isprintc(int c) { return false; }

  /**
   * get a pointer to a (read-only copy of a) line.
   *
   * On failure an error message is given and IObuff is returned (to avoid
   * having to check for error everywhere).
   */
  static MySegment ml_get(int lnum) {
    // return ml_get_buf(curbuf, lnum, FALSE);
    return new MySegment(G.curwin.getLineSegment(lnum));
  }
  
  static MySegment ml_get_curline() {
    //return ml_get_buf(curbuf, curwin->w_cursor.lnum, FALSE);
    return ml_get(G.curwin.getWCursor().getLine());
  }
  
  /** get pointer to positin 'pos', the returned MySegment's CharacterIterator
   * is initialized to the character at pos.
   * <p>
   * NEEDSWORK: this and following could alternately return a Segment
   *            whose first character is at the fpos/cursor.
   *
   * @return MySegment for the line.
   */
  static CharacterIterator ml_get_pos(ViFPOS pos) {
    //return (ml_get_buf(curbuf, pos->lnum, FALSE) + pos->col);
    MySegment seg = new MySegment(G.curwin.getLineSegment(pos.getLine()));
    seg.setIndex(seg.offset + pos.getColumn());
    return seg;
  }

  static CharacterIterator ml_get_cursor() {
    return ml_get_pos(G.curwin.getWCursor());
    //MySegment segment = ml_get(G.curwin.getWCursor().getLine());
    //return (MySegment) segment.subSequence(G.curwin.getWCursor().getColumn(), segment.length());
  }
  static void ml_replace(int lnum, CharSequence line) {
    G.curwin.replaceString(G.curwin.getLineStartOffset(lnum),
            G.curwin.getLineEndOffset(lnum) -1,
            line.toString());
  }

  public static MySegment truncateNewline(MySegment seg) {
      assert(seg.array[seg.offset + seg.count - 1] == '\n');
      return new MySegment(seg.array, seg.offset, seg.count - 1);
  }

  /**
   * Get the length of a line, not incuding the newline
   */
  static int lineLength(int line) {
    MySegment seg = G.curwin.getLineSegment(line);
    return seg.count < 1 ? 0 : seg.count - 1;
  }

  /** is the indicated line empty? */
  static boolean lineempty(int lnum) {
    MySegment seg = G.curwin.getLineSegment(lnum);
    return seg.count == 0 || seg.array[seg.offset] == '\n';
  }
  
  static boolean bufempty() {
      return G.curwin.getLineCount() == 1
             && lineempty(1);
  }

  static char getChar() {
    return getCharAt(G.curwin.getCaretPosition());
  }

  static char getCharAt(int offset) {
    MySegment seg = new MySegment();
    G.curwin.getSegment(offset, 1, seg);
    return seg.count > 0 ? seg.array[seg.offset] : 0;
  }

  /** flush map and typeahead buffers and vige a warning for an error */
  static void beep_flush() {
    GetChar.flush_buffers(false);
    vim_beep();
  }
}

// vi:set sw=2 ts=8:
