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

/**
 * A position in a file/document.
 */

public interface ViFPOS extends Comparable {
  public int getLine();
  public int getColumn();
  public int getOffset();
  /**
   * Set the position. This will set the postition on the new line.
   * If the column is less than zero, or past the new line, then it will
   * be restricted.
   * This is optional, may throw an UnsupportedOperationException
   */
  public void set(int line, int column);
  /**
   * This is a convenience for set(fpos.getLine(), fpos.getColumn());
   */
  public void set(ViFPOS fpos);
  /**
   * Set the column, leave the line unchanged.
   * <br/>
   * This is optional, may throw an UnsupportedOperationException
   */
  public void setColumn(int col);
  /**
   * Set the line, leave the column unchanged.
   * <br/>
   * This is optional, may throw an UnsupportedOperationException
   */
  public void setLine(int line);

  /** Make a copy */
  public ViFPOS copy();
}
