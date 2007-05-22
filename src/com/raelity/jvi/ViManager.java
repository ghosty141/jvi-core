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

import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JEditorPane;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;

import java.awt.datatransfer.FlavorMap;
import java.awt.datatransfer.SystemFlavorMap;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import com.raelity.jvi.swing.KeyBinding;
import com.raelity.text.TextUtil.MySegment;

/**
 * This class coordinates things.
 * <b>NEEDSWORK:</b><ul>
 * </ul>
 */
public class ViManager {
    
  public static final String PREFS_ROOT = "com/raelity/jvi";
  public static final String PREFS_KEYS = "KeyBindings";
  
  public static final String VIM_CLIPBOARD = "VimClipboard";
  public static final String VIM_CLIPBOARD2 = "VimClipboard2";
  public static final String VIM_CLIPBOARD_RAW = "VimRawBytes";

  static private JEditorPane currentEditorPane;
  static private ViFactory factory;

  static private Keymap editModeKeymap;
  static private Keymap normalModeKeymap;

  // HACK: to workaround JDK bug dealing with focus and JWindows
  public static ViCmdEntry activeCommandEntry;

  public static final jViVersion version = new jViVersion("0.9.5.beta1.4");
  
  private static boolean enabled;

  public static void setViFactory(ViFactory factory) {
    if(ViManager.factory != null) {
      throw new RuntimeException("ViFactory already set");
    }
    
    enabled = true;
    ViManager.factory = factory;

    Options.init();
    KeyBinding.init();
    
    // Add the vim clipboards

    // Spawn to get current release info
    new GetMotd().start();

    /*jViVersion v1 = new jViVersion("1.2.3.x4");
    jViVersion v2 = new jViVersion("1.2.3.alpha4");
    jViVersion v3 = new jViVersion("1.2.3.beta4");
    jViVersion v4 = new jViVersion("1.2.3.rc4");
    jViVersion v5 = new jViVersion("1.2.3.rc");
    jViVersion v6 = new jViVersion("1.2.3.beta5");
    jViVersion v7 = new jViVersion("1.2.3");
    jViVersion v8 = new jViVersion("1.2.4");
    jViVersion v9 = new jViVersion("1.3.0");*/
  }
  
  public static final DataFlavor VimClipboard = addVimClipboard(VIM_CLIPBOARD);
  public static final DataFlavor VimClipboard2 = addVimClipboard(VIM_CLIPBOARD2);
  public static final DataFlavor VimRawBytes = addVimClipboard(VIM_CLIPBOARD_RAW);
  // public static final DataFlavor VimClipboard;
  // public static final DataFlavor VimClipboard2;
  // public static final DataFlavor VimRawBytes;
  
  private static DataFlavor addVimClipboard(String cbName) {
    DataFlavor df = null;
    FlavorMap fm = SystemFlavorMap.getDefaultFlavorMap();
    if(fm instanceof SystemFlavorMap) {
      SystemFlavorMap sfm = (SystemFlavorMap) fm;
      try {
        df = new DataFlavor("application/" + cbName + "; class=java.nio.ByteBuffer");
      } catch (ClassNotFoundException ex) {
        ex.printStackTrace();
      }
      System.err.println(cbName + " " + df.getMimeType());
      
      sfm.addFlavorForUnencodedNative(cbName, df);
      sfm.addUnencodedNativeForFlavor(df, cbName);
    }
    return df;
  }

  public static String getReleaseString() {
    return "jVi " + version;
  }

  public static ViFactory getViFactory() {
    return factory;
  }

  public static ViFS getFS() {
    return factory.getFS();
  }

  public static JEditorPane getCurrentEditorPaneXXX() {
    return currentEditorPane;
  }

  public static ViTextView getViTextView(JEditorPane editorPane) {
    return factory.getViTextView(editorPane);
  }
  
  /** get any text view, other than tv, which has buf KLUDGE HACK */
  public static ViTextView getAlternateTextView(ViTextView tv, Buffer buf) {
    ViTextView tv01 = null;
    Set<ViTextView> tvSet = factory.getViTextViewSet();
    for (ViTextView tv02 : tvSet) {
      if(tv == tv02)
        continue;
      JEditorPane ep = tv02.getEditorComponent();
      if(ep != null) {
        if(factory.getBuffer(ep) == buf) {
          tv01 = tv02;
        }
      }
    }
    return tv01;
  }

  public static Buffer getBuffer(JEditorPane editorPane) {
    return factory.getBuffer(editorPane);
  }
  
  public static ViOutputStream createOutputStream(ViTextView tv,
                                           Object type, Object info) {
    return factory.createOutputStream(tv, type, info);
  }

  static public void installKeymap(JEditorPane editorPane) {
    editorPane.setKeymap(KeyBinding.getKeymap());
  }

  /**
   * Pass control to indicated ViCmdEntry widget. If there are
   * readahead or typeahead characters available, then collect
   * them up to a &lt;CR&gt; and append them to initialString.
   * If there was a CR, then signal the widget to immeadiately
   * fire its actionPerformed without displaying any UI element.
   */
  public static void startCommandEntry(ViCmdEntry commandEntry,
                                       String mode,
                                       ViTextView tv,
                                       StringBuffer initialString)
  {
    Msg.clearMsg();
    if(initialString == null) {
      initialString = new StringBuffer();
    }
    if(activeCommandEntry != null) {
      throw new RuntimeException("activeCommandEntry not null");
    }

    activeCommandEntry = commandEntry;
    boolean passThru;
    if (initialString.indexOf("\n") >= 0) {
      passThru = true;
    }
    else {
      passThru = GetChar.getRecordedLine(initialString);
    }
    commandEntry.activate(mode, tv, new String(initialString), passThru);
  }

  public static void stopCommandEntry() {
    activeCommandEntry = null;
  }
  
  /** update visible textviews */
  public static void updateHighlightSearchState() {
    Set<ViTextView> s = factory.getViTextViewSet();
    for (ViTextView tv : s) {
      if(factory.isVisible(tv)) {
        tv.updateHighlightSearchState();
      }
    }
  }
  
  //
  // jVi maintains two lists of opened files: the order they opened, and a
  // MostRecentlyUsed list.
  //
  // Even if jVi is disabled, these methods can be used. They only maintain
  // the lists.
  //

  // NEEDSWORK: textMRU: use a weak reference to fileObject?
  private static List textBuffers = new ArrayList();
  private static LinkedList textMRU = new LinkedList();
  private static Object currentlyActive;
  private static Object ignoreActivation;

  /**
   * Fetch the text buffer indicated by the argument. If the argument is
   * positive, then fetch the Nth buffer, numbered 1 to N, according to
   * the order they were activated. If the argument is negative then use
   * the MRU list to get the buffer, where -1
   * means the previous buffer. An argument of 0 will return null.
   * Usage for n < 0 is deprecated, consider -0 is not the top of the
   * MRU list, see {@link #getMruBuffer}.
   */
  public static Object getTextBuffer(int i) {
    if(i == 0) {
      return null;
    }
    if(i < 0)
      return getMruBuffer(-i);
      
    i = i - 1;
    if(i >= textBuffers.size()) {
      return null;
    }
    return textBuffers.get(i);
  }
  
  /**
   * Fetch the Nth buffer, 0 to N-1, from the Mru list.
   * @return the buffer, else null if i is out of bounds.
   */
  public static Object getMruBuffer(int i) {
    if(i < 0 || i >= textMRU.size())
        return null;
    return textMRU.get(i);
  }
  
  /**
   * Return the Ith next/previous fileObject relative to the argument 
   * fileObject. If i < 0 then look in previously used direction.
   */
  public static Object relativeMruBuffer(Object fileObject, int i) {
      if(factory != null && G.dbgEditorActivation.getBoolean()) {
        System.err.println("Activation: ViManager.relativeMruBuffer: "
                + factory.getDisplayFilename(fileObject));
      }
      if(textMRU.size() == 0)
          return null;
      int idx = textMRU.indexOf(fileObject);
      if(idx < 0)
          return null;
      // the most recent is at index 0, so bigger numbers are backwwards in time
      idx += -i;
      if(idx < 0)
          idx = 0;
      else if(idx >= textMRU.size())
          idx = textMRU.size() -1;
      return textMRU.get(idx);
  }
  
  public static Object relativeMruBuffer(int i) {
      return relativeMruBuffer(currentlyActive, i);
  }
  
  /**
   * Request that the next activation does not re-order the mru list if the
   * activated object is the argment.
   */
  public static void ignoreActivation(Object fileObject) {
      if(!textBuffers.contains(fileObject)) {
          return; // can't ignore if its not in the list
      }
      ignoreActivation = fileObject;
  }

  /**
   * The application invokes this whenever a file becomes selected
   * in the specified container. This also serves as an open.
   * @param ep May be null, otherwise the associated editor pane
   * @param parent Usually, but not necessarily, a container that hold the
   *               editor.
   */
  public static void activateFile(JEditorPane ep, Object fileObject, String tag) {
    if(factory != null && G.dbgEditorActivation.getBoolean()) {
      System.err.println("Activation: ViManager.activateFile: "
              + tag + ": " + factory.getDisplayFilename(fileObject));
    }
    if(ep != null && enabled)
        registerEditorPane(ep);
    assert(fileObject != null);
    if(fileObject == null)
        return;
    
    Object ign = ignoreActivation;
    ignoreActivation = null;
    currentlyActive = fileObject;
    if(textBuffers.contains(ign) && fileObject == ign) {
        return;
    }
    
    textMRU.remove(fileObject);
    textMRU.add(0, fileObject);
    if( ! textBuffers.contains(fileObject)) {
      textBuffers.add(fileObject);
    }
  }
  
  public static void deactivateCurrentFile(Object parent) {
    if(factory != null && G.dbgEditorActivation.getBoolean()) {
      System.err.println("Activation: ViManager.deactivateCurentFile: "
                         + factory.getDisplayFilename(parent));
    }
    // For several reasons, eg. don't want to hold begin/endUndo
    if(enabled)
        exitInputMode();
    
    currentlyActive = null;
    // assert(parent == currentlyActive || parent == null || currentlyActive == null);
  }
  
  public static boolean isBuffer(Object fileObject) {
      return textBuffers.contains(fileObject);
  }

  /**
   * The applications invokes this method when a file is completely
   * removed from a container or should be forgotten by jVi.
   */
  public static void closeFile(JEditorPane ep, Object fileObject) {
    if(factory != null && G.dbgEditorActivation.getBoolean()) {
      String fname = factory.getDisplayFilename(fileObject);
      System.err.println("Activation: ViManager.closeFile: "
              + (ep == null ? "(no shutdown) " : "") + fname);
    }
    
    assert(factory != null);
    if(factory != null && ep != null && enabled) {
        factory.shutdown(ep);
    }
    if(fileObject == currentlyActive)
        currentlyActive = null;
    textMRU.remove(fileObject);
    textBuffers.remove(fileObject);
  }
  
  //
  // END of OpenEditors list handling
  //

  /**
   * Set up an editor pane for use with vi.
   */
  public static void registerEditorPane(JEditorPane editorPane) {
    factory.registerEditorPane(editorPane);
  }
  
  public static void log(Object... a) {
      StringBuilder s = new StringBuilder();
      for (int i = 0; i < a.length; i++) {
          s.append(a[i]);
      }
      System.err.println(s);
  }

  /**
   * A key was typed. Handle the event.
   * <br>NEEDSWORK: catch all exceptions comming out of here?
   */
  static public void keyStroke(JEditorPane target, int key, int modifier) {
    switchTo(target);
    if(rerouteChar(key, modifier)) {
      return;
    }
    factory.finishTagPush(G.curwin);
    GetChar.gotc(key, modifier);
    
    if(G.curwin != null)
        G.curwin.getStatusDisplay().refresh();
  }

  /** If chars came in between the time a dialog was initiated and
   * the time the dialog starts taking the characters, we feed the
   * chars to the dialog.
   * <p>Special characters are discarded.
   * </p>
   */
  static boolean rerouteChar(int c, int modifiers) {
    if(activeCommandEntry == null) {
      return false;
    }
    if((c & 0xF000) != KeyDefs.VIRT
              && modifiers == 0) {
      if(c >= 0x20 && c < 0x7f) {
        String content = new String(new char[] {(char)c});
        activeCommandEntry.append(content);
      }
    }
    // System.err.println("rerouteChar " + (char)c);
    return true;
  }
  
  public static void requestSwitch(JEditorPane ep) {
      switchTo(ep);
  }

  private static boolean started = false;
  static final void switchTo(JEditorPane editorPane) {
    if(editorPane == currentEditorPane) {
        return;
    }
    if( ! started) {
      started = true;
      startup();
    }
    motd.outputOnce();
    
    exitInputMode(); // if switching, make sure prev out of input mode
    
    ViTextView textView = getViTextView(editorPane);
    registerEditorPane(editorPane); // make sure it has the right caret
    textView.attach();
    if(G.dbgEditorActivation.getBoolean()) {
      System.err.println("Activation: ViManager.SWITCHTO: "
              + textView.getDisplayFileName());
    }
    
    if(currentEditorPane != null) {
      Normal.resetCommand(); // NEEDSWORK: dont think this is needed
      Normal.abortVisualMode();
      ViTextView currentTv = getViTextView(currentEditorPane);
      // Freeze and/or detach listeners from previous active view
      currentTv.detach();
    }

    currentEditorPane = editorPane;
    Buffer buf = getBuffer(editorPane);
    G.switchTo(textView, buf);
    textView.activateOptions(textView);
    buf.activateOptions(textView);
    Normal.resetCommand(); // Means something first time window is switched to
    buf.checkModeline();
  }

  private static boolean inStartup;
  /** invoked once when vi is first used */
  private static void startup() {
    setupStartupList();
    inStartup = true;
    Iterator iter = startupList.iterator();
    while(iter.hasNext()) {
      ((ActionListener)iter.next()).actionPerformed(null);
    }
    Misc.javaKeyMap = KeyBinding.initJavaKeyMap();
    inStartup = false;
    startupList = null;
  }

  static List startupList;
  static void setupStartupList() {
    if(startupList == null) {
      startupList = new ArrayList();
    }
  }

  /**
   * Add listener to invoke when editor is starting up.
   * A null argument can be used to test if startup has
   * already occured.
   * @return true if listener add, otherwise false indicates
   * that startup has already occured.
   */
  public static boolean addStartupListener(ActionListener l) {
    if(started) {
      return false;
    }
    if(l != null) {
      setupStartupList();
      startupList.add(l);
    }
    return true;
  }

  public static void removeStartupListener(ActionListener l) {
    if(inStartup) {
      return;
    }
    startupList.remove(l);
  }

  /**
   * The arg JEditorPane is detached from its text view,
   * forget about it.
   */
  public static void detached(JEditorPane ep) {
    if(currentEditorPane == ep) {
      if(G.dbgEditorActivation.getBoolean()) {
        System.err.println("Activation: ViManager.detached");
      }
      currentEditorPane = null;
    }
  }

  public static void exitInputMode() {
    if(currentEditorPane != null) {
      Normal.resetCommand();
    }
  }

  /**
   * A mouse click; switch to the activated editor.
   * Pass the click on to the window and give it
   * a chance to adjust the position and whatever.
   */
  public static int mouseSetDot(int pos, JTextComponent c) {
    if( ! (c instanceof JEditorPane)) {
      return pos;
    }

    JEditorPane editorPane = (JEditorPane)c;

    // NEEDSWORK: mouse click: if( ! isRegistered(editorPane)) {}

    GetChar.flush_buffers(true);
    exitInputMode();
    switchTo(editorPane);
    
    //System.err.println("mouseSetDot(" + pos + ")");
    Window window = factory.lookupWindow(editorPane);
    pos = window.mouseClickedPosition(pos);
    Normal.abortVisualMode();
    return pos;
  }
  
  /** not mouse involved, keep caret off of new line;
   * see window.mouseClickedPosition(pos) */
  public static int setDot(int pos, JTextComponent c) {
    ViTextView tv = factory.getExistingViTextView(c);
    if(tv != null) {
      MySegment seg = new MySegment();
      tv.getSegment(pos, 1, seg);
      if (seg.count > 0
          && seg.array[seg.offset] == '\n'
          && (G.State & Constants.INSERT) == 0) {
        if(pos > 0) {
          tv.getSegment(pos -1, 1, seg);
          if(seg.count > 0 && seg.array[seg.offset] != '\n')
            --pos;
        }
      }
    }
    return pos;
  }
  
  public static int mouseMoveDot(int pos, JTextComponent c) {
    if(c != G.curwin.getEditorComponent()) {
      return pos;
    }
    //System.err.println("mouseMoveDot(" + pos + ")");
    if(pos != G.curwin.getCaretPosition()) {
    G.VIsual_mode ='v';
    G.VIsual_active = true;
    G.VIsual = (FPOS) G.curwin.getWCursor().copy();
    Misc.showmode();
    }
    return pos;
  }

  /** A mouse click may have moved the caret. */
  public static void unexpectedCaretChange(int dot) {
    // XXX verify mouse is at an acceptable location
  }

  /** The viewport has changed, so number of screen lines have changed */
  public static void viewSizeChange(ViTextView textView) {
    try {
      Window window = factory.lookupWindow(textView.getEditorComponent());
      window.viewSizeChange();
    }
    catch (NonExistentWindowException ex) {
    }
  }

  /** The viewport has changed or scrolled, clear messages*/
  public static void viewMoveChange(ViTextView textView) {
    if(G.curwin == null) {
      // this case is because switchto, does attach, does viewport init
      // but G.curwin is not set yet. See switchTo(JEditorPane editorPane)
      return;
    }
    Msg.clearMsg();
  }

  /** set the previous context to the indicated offset */
  /*
  public static void previousContextHack(ViTextView textView, ViMark mark) {
    Window window = factory.lookupWindow(textView.getEditorComponent());
    window.previousContextHack(mark);
  }
  */

  /**
   * Listen to carets events for newly registered JEditorPanes.
   * If an event comes in, assign the editorPane to a TextView
   * and give it the oportunity to re-adjust the cursor.
   * <br><b>NEEDSWORK:</b><ul>
   * <li> work this out with textview, where this should be......
   * <ul>
   */
  static void fixupCaret() {
  }

  static public void dumpStack(String msg) {
    try {
      throw new IllegalStateException(msg);
    } catch(IllegalStateException ex) {
      ex.printStackTrace();
    }
  }

  static public void dumpStack() {
    try {
      throw new IllegalStateException();
    } catch(IllegalStateException ex) {
      ex.printStackTrace();
    }
  }

  static public void setInsertModeKeymap(Keymap newInsertModeKeymap) {
    editModeKeymap = newInsertModeKeymap;
  }

  static public Keymap getInsertModeKeymap() {
    return editModeKeymap;
  }

  static public void setNormalModeKeymap(Keymap newNormalModeKeymap) {
    normalModeKeymap = newNormalModeKeymap;
  }

  static public Keymap getNormalModeKeymap() {
    return normalModeKeymap;
  }

  static public ActionListener xlateKeymapAction(ActionListener act) {
    return factory.xlateKeymapAction(act);
  }
  
  static public void dump(PrintStream ps) {
    ps.println("-----------------------------------");
    /*
    ps.println("currentEditorPane = " + currentEditorPane );
    ps.println("factory = " + factory );
    
    ps.println("" + textBuffers.size() + " active");
    ps.println("textBuffers = " + textBuffers );
    ps.println("textMRU = " + textMRU );
    ps.println("currentlyActive = " + currentlyActive );
    ps.println("ignoreActivation = " + ignoreActivation );
    */
    ps.println("currentEditorPane = " + G.curwin.getDisplayFileName());
    ps.println("factory = " + factory );
    
    ps.println("textBuffers: " + textBuffers.size());
    for (Object o : textBuffers) {
        ps.println("\t" + factory.getDisplayFilename(o)
                   + ", " + o.getClass().getSimpleName());
    }
    ps.println("textMRU: " + textMRU.size());
    for (Object o : textMRU) {
        ps.println("\t" + factory.getDisplayFilename(o)
                   + ", " + o.getClass().getSimpleName());
    }
    ps.println("currentlyActive: " + (currentlyActive == null ? "none"
               : "" + factory.getDisplayFilename(currentlyActive)
                 + ", " + currentlyActive.getClass().getSimpleName()));
    ps.println("ignoreActivation: " + (ignoreActivation == null ? "none"
               : "" + factory.getDisplayFilename(ignoreActivation)
                 + ", " + ignoreActivation.getClass().getSimpleName()));
    
    Set<ViTextView> tvSet = factory.getViTextViewSet();
    ps.println("TextViewSet: " + tvSet.size());
    for (ViTextView tv : tvSet) {
        ps.println("\t" + tv.getDisplayFileName());
    }
    
    Set<Buffer> bufSet = factory.getBufferSet();
    ps.println("BufferSet: " + bufSet.size());
    for (Buffer buf : bufSet) {
        ps.println("\t" + factory.getDisplayFilename(buf.getDoc())
                   + ", share: " + buf.getShare());
    }
  }

  /** version is of the form #.#.# or #.#.#.[x|alpha|beta|rc]#,
   * examples 0.9.1, 0.9.1.beta1
   * also, 0.9.1.beta1.3 for tweaking between exposed releases
   */
  public static final class jViVersion implements Comparable<jViVersion> {
    // in order
    public static final String X = "x";
    public static final String ALPHA = "alpha";
    public static final String BETA = "beta";
    public static final String RC = "rc";
    // following is map in order of suspected quality, these map to values
    // 0, 1, 2, 3
    // a release has none of these tags and compares greater than any of them
    // since it is set to value qualityMap.length, value == 4
    String[] qualityMap = new String[] { X, ALPHA, BETA, RC };

    // Each component of the version is an element of the array.
    // major.minor.micro
    // major.minor.micro.<quality>which
    // If no a|alpha.... then this is the 
    private int[] version = new int[6];

    private boolean valid;

    public jViVersion(String s) {
      String rev[] = s.split("\\.");
      if(rev.length < 3 || rev.length > 5) {
        init(0, 0, 0, 0, 0);
        return;
      }
      for(int i = 0; i < 3; i++)  {
        try {
          version[i] = Integer.parseInt(rev[i]);
        } catch (NumberFormatException ex) {
          ex.printStackTrace();
          init(0, 0, 0, 0, 0);
          return;
        }
      }
      valid = true;
      if(rev.length == 3) {
        // A release version, no quality tag or tweak, it is "better" than those
        version[3] = qualityMap.length;
      } else {
        // so this is something between releases
        // version[0:2] has 1.2.3 stored in it
        // rev[3] has string like beta3, rev[4] may have a tweak; beta3.7
        // into rev[0:2] put strings for beta,3,7
        Pattern p = Pattern.compile("(x|alpha|beta|rc)(\\d+)");
        Matcher m = p.matcher(rev[3]);
        // Note, if doesn't match, then version number looks like 1.2.3.x0
        // since version[3:5] left at zero
        if(m.matches()) {
          String q = m.group(1);
          for(int i = 0; i < qualityMap.length; i++) {
            if(q.equals(qualityMap[i])) {
              rev[0] = "" + i;
              break;
            }
          }
          rev[1] = m.group(2);
          // if there's a tweak on beta1, then copy it, else set to zero
          rev[2] = rev.length == 5 ? rev[4] : "0";
          try {
            for(int i = 0; i <= 2; i++)
              version[i+3] = Integer.parseInt(rev[i]);
          } catch (NumberFormatException ex) {
            ex.printStackTrace();
          }
        }
      }
      //System.err.println("input: " + s + ", version: " + this);
    }

    private void init(int major, int minor, int micro, int qTag, int qVer) {
      version[0] = major;
      version[1] = minor;
      version[2] = micro;
      version[3] = qTag;
      version[4] = qVer;
    }

    public boolean isValid() {
      return valid;
    }

    public boolean isRelease() {
      return version[3] == qualityMap.length;
    }

    public boolean isDevelopment() {
      // development releases are x, alpha or any tweaks
      // beta and rc are not (unless tweaked)
      int qTag = version[3];
      return qTag == 0 || qTag == 1 || getTweak() != 0;
    }

    public String toString() {
      String s =   "" + version[0]
		+ "." + version[1]
                + "." + version[2];
      if(version[3] != qualityMap.length)
        s += "." + qualityMap[version[3]] + version[4];
      if(version[5] != 0)
        s += "." + version[5];
      return s;
    }
    
    public int getMajor() {
      return version[0];
    }
    
    public int getMinor() {
      return version[1];
    }
    
    public int getMicro() {
      return version[2];
    }

    public int getTweak() {
      return version[5];
    }
    
    public String getTag() {
      if(isRelease())
        return "";
      return qualityMap[version[3]] + version[4]
              + (getTweak() == 0 ? "" : getTweak());
    }

    public int compareTo(ViManager.jViVersion v2) {
      for(int i = 0; i < version.length; i++) {
        if(version[i] != v2.version[i])
          return version[i] - v2.version[i];
      }
      return 0;
    }
  }
  
  static Motd motd = new Motd();

  static class Motd {
    private jViVersion latestRelease;
    private jViVersion latestBeta;
    private int messageNumber;
    private String message;
    private boolean valid;
    private boolean output;

    Motd() {
      // not valid
    }

    boolean getValid() {
      return valid;
    }

    Motd(String s) {
      //String lines[] = motd.split("\n");
      Pattern p = Pattern.compile("^jVi-release: (\\S+)", Pattern.MULTILINE);
      Matcher m = p.matcher(s);
      if(m.find()) {
        latestRelease = new jViVersion(m.group(1));
      }
      p = Pattern.compile("^jVi-beta: (\\S+)", Pattern.MULTILINE);
      m = p.matcher(s);
      if(m.find()) {
        latestBeta = new jViVersion(m.group(1));
      }
      p = Pattern.compile("^jVi-message: (\\d+).*$", Pattern.MULTILINE);
      m = p.matcher(s);
      int loc = 0;
      if(m.find()) {
        try {
          messageNumber = Integer.parseInt(m.group(1));
          message = s.substring(m.end(0)+1); // +1 to skip the newline
        } catch (NumberFormatException ex) {
          ex.printStackTrace();
        }
      }
      valid = true;
    }

    void outputOnce() {
      if(output)
        return;
      output();
    }

    void output() {
      if(!valid)
        return;
      output = true;

      ViOutputStream vios = ViManager.createOutputStream(
              null, ViOutputStream.OUTPUT, "jVi Version Information");

      String tagCurrent = "";
      String hasNewer = null;
      if(latestRelease != null && latestRelease.isValid()) {
        if(latestRelease.compareTo(version) > 0) {
          hasNewer = "Newer release available: " + latestRelease;
        } else if(latestRelease.compareTo(version) == 0)
          tagCurrent = " (This is the latest release)";
        else {
          // In this else, should be able to assert that !isRelease()
          if(version.isDevelopment())
            tagCurrent = " (development release)";
        }
      }
      vios.println("Running: " + getReleaseString() + tagCurrent);
      if(hasNewer != null)
        vios.println(hasNewer);
      if(latestBeta != null && latestBeta.isValid()) {
        if(latestBeta.compareTo(version) > 0) {
          vios.println("Beta or release candidate available: " + latestBeta);
        }
      }
      if(message != null)
        vios.println(message);
      vios.close();
    }
  }

  private static class GetMotd extends Thread {
    private static final int BUF_LEN = 1024;
    private static final int MAX_MSG = 8 * 1024;
    public void run() {
      URL url = null;
      try {
        URI uri = new URI("http://jvi.sourceforge.net/motd");
        url = uri.toURL();
      } catch (MalformedURLException ex) {
        ex.printStackTrace();
      } catch (URISyntaxException ex) {
        ex.printStackTrace();
      }
      if(url == null)
        return;
      
      // Read the remote file into a string
      // We *know* that the decoder will never have unprocessed
      // bytes in it, US-ASCII ==> 1 byte per char.
      // So use a simple algorithm.
      try {
        URLConnection c = url.openConnection();
        InputStream in = c.getInputStream();
        byte b[] = new byte[BUF_LEN];
        ByteBuffer bb = ByteBuffer.wrap(b);
        StringBuilder sb = new StringBuilder();
        Charset cset = Charset.forName("US-ASCII");
        int n;
        int total = 0;
        while((n = in.read(b)) > 0 && total < MAX_MSG) {
          bb.position(0);
          bb.limit(n);
          CharBuffer cb = cset.decode(bb);
          sb.append(cb.toString());
          total += n;
        }
        in.close();
        
        motd = new Motd(sb.toString());
        System.err.print(motd);
      } catch (IOException ex) {
        //ex.printStackTrace();
      }
    }
  }
}

// vi:set sw=2 ts=8:
