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
package com.raelity.jvi.swing;

import java.awt.Event;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.util.List;
import java.util.ArrayList;
import javax.swing.KeyStroke;
import javax.swing.Action;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import javax.swing.KeyStroke;

import com.raelity.jvi.KeyDefs;
import com.raelity.jvi.BooleanOption;
import com.raelity.jvi.Option;
import com.raelity.jvi.Options;
import com.raelity.jvi.*;

public class KeyBinding implements KeyDefs {

  public static BooleanOption keyDebug
                = (BooleanOption)Options.getOption(Options.dbgKeyStrokes);
  public static boolean notImpDebug = false;

  public static final int MOD_MASK = InputEvent.SHIFT_MASK
	    				| InputEvent.CTRL_MASK
					| InputEvent.META_MASK
					| InputEvent.ALT_MASK;

  static final String enqueKeyAction = "enque-key";

  public static Keymap getKeymap() {
    Keymap keymap = JTextComponent.addKeymap(null, null);
    keymap.setDefaultAction(ViManager.getViFactory()
			    	.createCharAction(enqueKeyAction));
    JTextComponent.loadKeymap(keymap, getBindings(), getActions());
    return keymap;
  }

  public static Action getDefaultAction() {
    return ViManager.getViFactory().createCharAction(enqueKeyAction);
  }

  /** Modify the keymap <code>km00</code> by removing any keystrokes
   * specified in the keymap <code>km01</code>. This is typically used
   * to remove keys that vi would normally be looking at, e.g. function
   * keys, but are otherwise bound to the environment in which vi is
   * operating.
   */
  public static void removeBindings(Keymap km00, Keymap km01) {
    int i;
    KeyStroke[] k01 = km01.getBoundKeyStrokes();
    for(i = 0; i < k01.length; i++) {
      km00.removeKeyStrokeBinding(k01[i]);
    }
  }

  /**
   * Bind the keys to actions of their own name. This is simpley a
   * way to grab all the keys. Probably want to use key events
   * directly at some point.
   * <br>
   * Only do regular and shift versiions for now.
   */
  public static JTextComponent.KeyBinding[] getBindings() {
    List l = getBindingsList();
    return (JTextComponent.KeyBinding[]) l.toArray(
                              new JTextComponent.KeyBinding[l.size()]);
  }

  public static List getBindingsList() {

    List bindingList = new ArrayList();

    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_UP, 0),
                   "ViUpKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_DOWN, 0),
                   "ViDownKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_LEFT, 0),
                   "ViLeftKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_RIGHT, 0),
                   "ViRightKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_INSERT, 0),
                   "ViInsertKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_DELETE, 0),
                   "ViDeleteKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_HOME, 0),
                   "ViHomeKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_END, 0),
                   "ViEndKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_HELP, 0),
                   "ViHelpKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_UNDO, 0),
                   "ViUndoKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_PAGE_UP, 0),
                   "ViPage_upKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_PAGE_DOWN, 0),
                   "ViPage_downKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_PLUS, 0),
                   "ViPlusKey"));
    // bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
      // 	     KeyEvent.VK_MINUS, 0),
      // 	     "ViMinusKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_DIVIDE, 0),
                   "ViDivideKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_MULTIPLY, 0),
                   "ViMultiplyKey"));

    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F1, 0),
                   "ViF1Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F2, 0),
                   "ViF2Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F3, 0),
                   "ViF3Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F4, 0),
                   "ViF4Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F5, 0),
                   "ViF5Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F6, 0),
                   "ViF6Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F7, 0),
                   "ViF7Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F8, 0),
                   "ViF8Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F9, 0),
                   "ViF9Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F10, 0),
                   "ViF10Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F11, 0),
                   "ViF11Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F12, 0),
                   "ViF12Key"));
    //
    //
    // SHIFTED KEYS
    //
    //

    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_UP, InputEvent.SHIFT_MASK),
                   "ViUpKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_DOWN, InputEvent.SHIFT_MASK),
                   "ViDownKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_LEFT, InputEvent.SHIFT_MASK),
                   "ViLeftKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_RIGHT, InputEvent.SHIFT_MASK),
                   "ViRightKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_INSERT, InputEvent.SHIFT_MASK),
                   "ViInsertKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_DELETE, InputEvent.SHIFT_MASK),
                   "ViDeleteKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_HOME, InputEvent.SHIFT_MASK),
                   "ViHomeKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_END, InputEvent.SHIFT_MASK),
                   "ViEndKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_HELP, InputEvent.SHIFT_MASK),
                   "ViHelpKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_UNDO, InputEvent.SHIFT_MASK),
                   "ViUndoKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_PAGE_UP, InputEvent.SHIFT_MASK),
                   "ViPage_upKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_PAGE_DOWN, InputEvent.SHIFT_MASK),
                   "ViPage_downKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_PLUS, InputEvent.SHIFT_MASK),
                   "ViPlusKey"));
    // bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   // KeyEvent.VK_MINUS, InputEvent.SHIFT_MASK),
                   // "ViMinusKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_DIVIDE, InputEvent.SHIFT_MASK),
                   "ViDivideKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_MULTIPLY, InputEvent.SHIFT_MASK),
                   "ViMultiplyKey"));

    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F1, InputEvent.SHIFT_MASK),
                   "ViF1Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F2, InputEvent.SHIFT_MASK),
                   "ViF2Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F3, InputEvent.SHIFT_MASK),
                   "ViF3Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F4, InputEvent.SHIFT_MASK),
                   "ViF4Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F5, InputEvent.SHIFT_MASK),
                   "ViF5Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F6, InputEvent.SHIFT_MASK),
                   "ViF6Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F7, InputEvent.SHIFT_MASK),
                   "ViF7Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F8, InputEvent.SHIFT_MASK),
                   "ViF8Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F9, InputEvent.SHIFT_MASK),
                   "ViF9Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F10, InputEvent.SHIFT_MASK),
                   "ViF10Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F11, InputEvent.SHIFT_MASK),
                   "ViF11Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F12, InputEvent.SHIFT_MASK),
                   "ViF12Key"));
     
    //
    //
    // CTRL KEYS
    //
    //

    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_UP, InputEvent.CTRL_MASK),
                   "ViUpKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_DOWN, InputEvent.CTRL_MASK),
                   "ViDownKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_LEFT, InputEvent.CTRL_MASK),
                   "ViLeftKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_RIGHT, InputEvent.CTRL_MASK),
                   "ViRightKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_INSERT, InputEvent.CTRL_MASK),
                   "ViInsertKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_DELETE, InputEvent.CTRL_MASK),
                   "ViDeleteKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_HOME, InputEvent.CTRL_MASK),
                   "ViHomeKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_END, InputEvent.CTRL_MASK),
                   "ViEndKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_HELP, InputEvent.CTRL_MASK),
                   "ViHelpKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_UNDO, InputEvent.CTRL_MASK),
                   "ViUndoKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_PAGE_UP, InputEvent.CTRL_MASK),
                   "ViPage_upKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_PAGE_DOWN, InputEvent.CTRL_MASK),
                   "ViPage_downKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_PLUS, InputEvent.CTRL_MASK),
                   "ViPlusKey"));
    // bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   // KeyEvent.VK_MINUS, InputEvent.CTRL_MASK),
                   // "ViMinusKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_DIVIDE, InputEvent.CTRL_MASK),
                   "ViDivideKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_MULTIPLY, InputEvent.CTRL_MASK),
                   "ViMultiplyKey"));

    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F1, InputEvent.CTRL_MASK),
                   "ViF1Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F2, InputEvent.CTRL_MASK),
                   "ViF2Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F3, InputEvent.CTRL_MASK),
                   "ViF3Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F4, InputEvent.CTRL_MASK),
                   "ViF4Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F5, InputEvent.CTRL_MASK),
                   "ViF5Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F6, InputEvent.CTRL_MASK),
                   "ViF6Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F7, InputEvent.CTRL_MASK),
                   "ViF7Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F8, InputEvent.CTRL_MASK),
                   "ViF8Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F9, InputEvent.CTRL_MASK),
                   "ViF9Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F10, InputEvent.CTRL_MASK),
                   "ViF10Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F11, InputEvent.CTRL_MASK),
                   "ViF11Key"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_F12, InputEvent.CTRL_MASK),
                   "ViF12Key"));

    //
    //
    // other bindings
    //
    //

    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_SPACE, 0),
                 "ViSpaceKey"));

    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_F, Event.CTRL_MASK),
                 "ViCtrl-F"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_G, Event.CTRL_MASK),
                 "ViCtrl-G"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_BACK_SPACE, 0),
                 "ViBack_spaceKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_TAB, 0),
                   "ViTabKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_ENTER, 0),
                 "ViEnterKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_N, Event.CTRL_MASK),
                 "ViCtrl-N"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_P, Event.CTRL_MASK),
                 "ViCtrl-P"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_ESCAPE, 0),
                 "ViEscapeKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_OPEN_BRACKET, Event.CTRL_MASK),
                 "ViEscapeKey"));      // alternate 

    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_SPACE, InputEvent.SHIFT_MASK),
                 "ViSpaceKey"));

    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_BACK_SPACE, InputEvent.SHIFT_MASK),
                 "ViBack_spaceKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                   KeyEvent.VK_TAB, InputEvent.SHIFT_MASK),
                   "ViTabKey"));
    bindingList.add(new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
                 KeyEvent.VK_ENTER, InputEvent.SHIFT_MASK),
                 "ViEnterKey"));
    return bindingList;
  }

  /**
   * initialize keymap.
   */
  public static Action[] getActions() {
    Action[] localActions = null;
    try {
      ViFactory factory = ViManager.getViFactory();
      localActions = new Action[] {
	  factory.createKeyAction("ViUpKey", K_UP),
	  factory.createKeyAction("ViDownKey", K_DOWN),
	  factory.createKeyAction("ViLeftKey", K_LEFT),
	  factory.createKeyAction("ViRightKey", K_RIGHT),
	  factory.createKeyAction("ViInsertKey", K_INS),
	  factory.createKeyAction("ViDeleteKey", K_DEL),
	  factory.createKeyAction("ViTabKey", K_TAB),
	  factory.createKeyAction("ViHomeKey", K_HOME),
	  factory.createKeyAction("ViEndKey", K_END),
	  factory.createKeyAction("ViHelpKey", K_HELP),
	  factory.createKeyAction("ViUndoKey", K_UNDO),
	  factory.createKeyAction("ViBack_spaceKey", KeyEvent.VK_BACK_SPACE),

	  factory.createKeyAction("ViPage_upKey", K_PAGEUP),
	  factory.createKeyAction("ViPage_downKey", K_PAGEDOWN),
	  factory.createKeyAction("ViPlusKey", K_KPLUS),
	  factory.createKeyAction("ViMinusKey", K_KMINUS),
	  factory.createKeyAction("ViDivideKey", K_KDIVIDE),
	  factory.createKeyAction("ViMultiplyKey", K_KMULTIPLY),
	  // factory.createKeyAction("ViEnterKey", K_ENTER),

	  factory.createKeyAction("ViCtrl-F", 6),
	  factory.createKeyAction("ViCtrl-G", 7),
	  factory.createKeyAction("ViEnterKey", KeyEvent.VK_ENTER), // 13
	  factory.createKeyAction("ViCtrl-N", 14),
	  factory.createKeyAction("ViCtrl-P", 16),
	  factory.createKeyAction("ViEscapeKey", KeyEvent.VK_ESCAPE), // 27
	  factory.createKeyAction("ViSpaceKey", KeyEvent.VK_SPACE),

	  factory.createKeyAction("ViF1Key", K_F1),
	  factory.createKeyAction("ViF2Key", K_F2),
	  factory.createKeyAction("ViF3Key", K_F3),
	  factory.createKeyAction("ViF4Key", K_F4),
	  factory.createKeyAction("ViF5Key", K_F5),
	  factory.createKeyAction("ViF6Key", K_F6),
	  factory.createKeyAction("ViF7Key", K_F7),
	  factory.createKeyAction("ViF8Key", K_F8),
	  factory.createKeyAction("ViF9Key", K_F9),
	  factory.createKeyAction("ViF10Key", K_F10),
	  factory.createKeyAction("ViF11Key", K_F11),
	  factory.createKeyAction("ViF12Key", K_F12)
      };
    } catch(Throwable e) {
      e.printStackTrace();
    }
    return localActions;
  }

  /** Read these as keys, not chars. */
  final private static boolean ignoreCtrlChars[] = {
    	false,		// 0
	false,
	false,
	false,
	false,		// 4
    	false,		// 5
	true,		// 6	Ctrl-F
	true,		// 7	Ctrl-G
	true,		// 8	backspace
	true,		// 9	tab
    	true,		// 10	return/enter
	false,
	false,
	false,
	true,		// 14	Ctrl-N
    	false,		// 15
	true,		// 16	Ctrl-P
	false,
	false,
	false,		// 19
    	false,
	false,
	false,
	false,
	false,		// 24
    	false,
	false,
	true,		// 27	escape
	false,
	false,		// 29
	false,		// 30
	false,		// 31
	true		// 32	space is special case
  };

  /** Test if the argument char should be ignored. Note that when
   * ignored as a char, it is generally is queued up as a key.
   */
  final static public boolean ignoreChar(int c) {
    return	c < KeyBinding.ignoreCtrlChars.length
	  	&& KeyBinding.ignoreCtrlChars[c];
  }
}
