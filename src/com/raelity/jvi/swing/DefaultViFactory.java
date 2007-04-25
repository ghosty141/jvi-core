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

import java.awt.Component;
import java.awt.Container;
import java.awt.KeyboardFocusManager;
import java.awt.datatransfer.SystemFlavorMap;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.prefs.Preferences;
import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;
import javax.swing.text.Document;
import javax.swing.text.TextAction;
import javax.swing.text.Caret;

import com.raelity.jvi.*;
import com.raelity.jvi.ViTextView.TAGOP;

import static com.raelity.jvi.Constants.*;
import static com.raelity.jvi.KeyDefs.*;
import java.awt.datatransfer.FlavorMap;
import java.util.Map;
import javax.swing.JComponent;

/**
 * This provides the Vi items to interface with standard swing JEditorPane.
 * <b>NEEDSWORK:</b><ul>
 * <li> only one text view supported for now
 * </ul>
 */
public class DefaultViFactory implements ViFactory {
  protected static DefaultViFactory INSTANCE;
  public static final String PROP_VITV = "ViTextView";
  public static final String PROP_BUF = "ViBuffer";
  // Really a WeakSet, all doc's that have been seen, garbage collector trims it
  protected Map<Document, Object> docSet
          = new WeakHashMap<Document,Object>();
  
  // This is used only when dbgEditorActivation is turned on
  protected WeakHashMap<JEditorPane, Object> editorSet
          = new WeakHashMap<JEditorPane, Object>();

  Window window;
  CommandLine cmdLine;
  ViFS fs = new DefaultViFS();

  public DefaultViFactory(CommandLine cmdLine) {
    this.cmdLine = cmdLine;
    if(INSTANCE != null)
        throw new IllegalStateException("ViFactory already exists");
    INSTANCE = this;
    
    // Add VimClipboard DataFlavor if not already there
    //FlavorMap fm = SystemFlavorMap.getDefaultFlavorMap();
  }
  
  public ViTextView getExistingViTextView(Object editorPane) {
    if(!(editorPane instanceof JComponent))
        return null;
    return (ViTextView)((JComponent)editorPane).getClientProperty(PROP_VITV);
  }

  public ViTextView getViTextView(JEditorPane editorPane) {
    ViTextView tv01 = (ViTextView)editorPane.getClientProperty(PROP_VITV);
    if(tv01 == null) {
      if(G.dbgEditorActivation.getBoolean())
          System.err.println("Activation: getViTextView: create");
      tv01 = createViTextView(editorPane);
      
      Document doc = editorPane.getDocument();
      Buffer buf = null;
      if(doc != null) {
        buf = (Buffer)doc.getProperty(PROP_BUF);
        if(buf == null) {
          buf = createBuffer(editorPane);
          buf.b_visual_start = new Mark();
          buf.b_visual_end = new Mark();
          doc.putProperty(PROP_BUF, buf);
          docSet.put(doc, null);
        }
        buf.addShare();
      }
      
      tv01.startup(buf);
      tv01.setWindow(new Window(tv01));
      editorPane.putClientProperty(PROP_VITV, tv01);
      editorSet.put(editorPane, null);
    }
    return tv01;
  }
  
  /** subclass probably wants to override this */
  protected ViTextView createViTextView(JEditorPane editorPane) {
    return new TextView(editorPane);
  }

  public Set<ViTextView> getViTextViewSet() {
    Set<ViTextView> s = new HashSet<ViTextView>();
    for (JEditorPane ep : editorSet.keySet()) {
        ViTextView tv = (ViTextView) ep.getClientProperty(PROP_VITV);
        if(tv != null)
            s.add(tv);
    }
      
    return s;
  }
  
  public boolean isVisible(ViTextView tv) {
    // wonder if this works
    return tv.getEditorComponent().isVisible();
  }
  
  public Buffer getBuffer(JEditorPane editorPane) {
    Buffer buf = null;
    Document doc = editorPane.getDocument();
    if(doc != null) {
      buf = (Buffer)doc.getProperty(PROP_BUF);
    }
    return buf;
  }
  
  /** subclass probably wants to override this */
  protected Buffer createBuffer(JEditorPane editorPane) {
      return new Buffer(editorPane.getDocument());
  }

  public Set<Buffer> getBufferSet() {
    Set<Buffer> s = new HashSet<Buffer>();
    for (Document doc : docSet.keySet()) {
      Buffer buf = (Buffer) doc.getProperty(PROP_BUF);
      if(buf != null)
        s.add(buf);
    }
      
    return s;
  }
  
  public String getDisplayFilename(Object o) {
      return "";
  }
  
  public void shutdown(JEditorPane ep) {
    ViTextView tv = (ViTextView)ep.getClientProperty(PROP_VITV);
    if(tv != null) {
      if(G.dbgEditorActivation.getBoolean())
        System.err.println("Activation: shutdown TV");
      tv.shutdown();
      ep.putClientProperty(PROP_VITV, null);
      Buffer buf = getBuffer(ep);
      if(buf != null) {
        Document doc = buf.getDoc();
        buf.removeShare();
        if(buf.getShare() == 0) {
          if(doc != null)
            doc.putProperty(PROP_BUF, null);
          else
            ViManager.dumpStack("SHUTDOWN NULL DOC");
        }
      }
    }
  }
  

  public ViFS getFS() {
    return fs;
  }

  public ViOutputStream createOutputStream(ViTextView tv,
                                           Object type, Object info) {
    return new DefaultOutputStream(tv, type.toString(),
                                   info == null ? null : info.toString());
  }
  
  private MouseInputAdapter mouseAdapter;
  private KeyListener keyListener;
  
  /** Get the glass pane for the given component, if it doesn't have an
   * associated mouseAdapter create one and add it. */
  private Container getModalGlassPane(final Component c){
    Container glass = null;
    if(c != null) {
      JRootPane rp = SwingUtilities.getRootPane(c);
      if(rp != null) {
        glass = (Container) rp.getGlassPane();
        if(mouseAdapter == null) {
          mouseAdapter = new MouseInputAdapter() {
            public void mousePressed(MouseEvent evt) {
              c.getToolkit().beep();
            }
          };
          glass.addMouseListener(mouseAdapter);
          glass.addMouseMotionListener(mouseAdapter);
        }
      }
    }
    return glass;
  }
  
  public void startModalKeyCatch(KeyListener kl) {
    if(mouseAdapter != null)
      throw new IllegalStateException("Already in modal state");
    
    Container glass = getModalGlassPane(G.curwin.getEditorComponent());
    keyListener = kl;
    glass.addKeyListener(kl);
    glass.setVisible(true);
    
    // disable all focus traversal
    glass.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                                Collections.EMPTY_SET);
    glass.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                                Collections.EMPTY_SET);
    glass.setFocusTraversalKeys(KeyboardFocusManager.DOWN_CYCLE_TRAVERSAL_KEYS,
                                Collections.EMPTY_SET);
    glass.setFocusTraversalKeys(KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS,
                                Collections.EMPTY_SET);
    glass.setFocusCycleRoot(true);
    
    glass.requestFocusInWindow();
  }
  
  /*
  public void startModalButton(String text, Color color) {
    if(mouseAdapter != null)
      throw new IllegalStateException("Already in modal state");
    Container glass = getModalGlassPane(G.curwin.getEditorComponent());
  }
  */
  
  public void stopModal() {
    if(mouseAdapter == null)
      throw new IllegalStateException("Not in modal state");
    Container glass = getModalGlassPane(G.curwin.getEditorComponent());
    glass.setVisible(false);
    glass.removeMouseListener(mouseAdapter);
    glass.removeMouseMotionListener(mouseAdapter);
    glass.removeKeyListener(keyListener);
    mouseAdapter = null;
    keyListener = null;
    
    // Back to default bahavior
    glass.setFocusCycleRoot(false);
    glass.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                                null);
    glass.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                                null);
    glass.setFocusTraversalKeys(KeyboardFocusManager.DOWN_CYCLE_TRAVERSAL_KEYS,
                                null);
    glass.setFocusTraversalKeys(KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS,
                                null);
    
    G.curwin.getEditorComponent().requestFocusInWindow();
  }

  /**
   * Register editor pane for use with vi. Install a
   * vi cursor. This is a nop
   * if already registered.
   */
  public void registerEditorPane(JEditorPane editorPane) {
    // install cursor if neeeded
    Caret c = editorPane.getCaret();
    if( ! (c instanceof ViCaret)) {
      DefaultViCaret caret = new DefaultViCaret();
      editorPane.setCaret(caret);
      caret.setDot(c.getDot());
      caret.setBlinkRate(c.getBlinkRate());
    }
  }

  public Window lookupWindow(JEditorPane editorPane) {
      // NEEDSWORK: get rid of lookupWindow, should always be currentEdPane
      //            maybe can get rid of it entirely
    ViTextView tv = ViManager.getViTextView(editorPane);
    return tv.getWindow();
  }

  public ViCmdEntry createCmdEntry(int type) {
    // ViCmdEntry cmdEntry = new DefaultCmdEntry(cmdLine);
    // return cmdEntry;
    
    // use this instead so that input is grabbed. When we have a
    // cleaner and more understandable key input state machine revisit
    // this.
    // return new WindowCmdEntry(type);
    return new InlineCmdEntry(type);
  }
  
  public void updateKeymap() {
  }

  /**
   * @return action suitable for default key action
   */
  public Action createCharAction(String name) {
    return new EnqueCharAction(name);
  }

  /**
   * @return action for picking up specified key
   */
  public Action createKeyAction(String name, int key) {
    return new EnqueKeyAction(name, key);
  }
  
  /*
  public Keymap getInsertModeKeymap() {
    return KeyBinding.insertModeKeymap;
  }
  
  public Keymap getNormalModeKeymap() {
    return KeyBinding.normalModeKeymap;
  }
  */
  
  public Action createInsertModeKeyAction(String name, int vkey, String desc) {
    return new InsertModeAction(name, vkey, desc);
  }
  
  public Action createNormalModeKeyAction(String name, int vkey, String desc) {
    return null;
  }
  
  public ActionListener xlateKeymapAction(ActionListener act) {
    return act;
  }

  public Preferences getPreferences() {
      return Preferences.userRoot().node(ViManager.PREFS_ROOT);
      //return Preferences.userNodeForPackage(Options.class);
  }

  /**
   * This is the default key action.
   * Ignore all Ctrl characters (which includes that troublesome Ctrl-space).
   * Control characters of interest are picked up as key-press events
   * in {link #EnqKeyAction}.
   */
  public static class EnqueCharAction extends TextAction {
    public EnqueCharAction(String name) {
	super(name);
    }

    public void actionPerformed(ActionEvent e) {
      JEditorPane target = (JEditorPane)getTextComponent(e);
      if(target != null && e != null) {
	String content = e.getActionCommand();
	int mod = e.getModifiers();
	if(content != null && content.length() > 0) {
	  int c = content.charAt(0);
	  if((mod & (MOD_MASK_CTRL|MOD_MASK_ALT)) != 0
             || c < 0x20
             || c == 0x7f ) { //The delete key comes in as a virtual key.
	    // Wouldn't have thought that the 'c<0x20' was needed, but the
            // <RETURN>,<BS> come in less than 0x20 without the Control key
	    return;
	  }
          if(KeyBinding.isKeyDebug()) {
            System.err.println("CharAction: " + "'" + (char)c + "' "
                               + String.format("%x", c) + "(" + c + ") " + mod);
          }
	  ViManager.keyStroke(target, content.charAt(0), mod);
	}
	else {
          if(KeyBinding.isKeyDebug()) {
            System.err.println("CharAction: " + e);
          }
	}
      }
    }
  }

  /**
   * Catch non-printing keys with this class. The constructor
   * specifies which key. The modifiers are handled by examining
   * the event and added to the key. Recieved characters are placed
   * on the vi input Q.
   */
  public static class EnqueKeyAction extends TextAction {
    int basekey;

    public EnqueKeyAction(String name, int key) {
	super(name);
	this.basekey = key;
    }

    public void actionPerformed(ActionEvent e) {
      JEditorPane target = (JEditorPane)getTextComponent(e);
      int mod = e.getModifiers();
      int key = basekey;
      if(KeyBinding.isKeyDebug()) {
        String virt = ((key & 0xF000) == VIRT) ? "virt" : "";
        System.err.println("KeyAction: " + getValue(Action.NAME).toString()
                           + ": " + String.format("%x", key)
                           + "(" + (key&~VIRT) + ") " + mod + " " + virt);
      }
      ViManager.keyStroke(target, key, mod);
    }
  }
  
  private static class InsertModeAction extends TextAction
  				      implements ViXlateKey {
    int basekey;

    public InsertModeAction(String name, int vkey, String desc) {
      super(name); // ??????????????????????
      this.basekey = vkey;
      
      // if name starts with Vi and ends with Key, then put out a message
      // with the name of the key in it
      //this.putValue(Action.LONG_DESCRIPTION, desc);
      //this.putValue("ActionGroup", GROUP_VI_EDIT);
      //EditorActions.addBindableEditorAction(this, JBViKeymap.VI_EDIT_KEYMAP);
    }

    public void actionPerformed(ActionEvent e) {
      // NOT USED for the translation keymap
    }
    
    public int getXlateKey() {
      return basekey;
    }
  }

  public void startTagPush(ViTextView tv, String ident){}

  public void finishTagPush(ViTextView tv){}
  
  public void tagStack(TAGOP op, int count) {}
  
  public void displayTags(){}
  
  public void tagDialog(ColonCommands.ColonEvent e) {}
}
