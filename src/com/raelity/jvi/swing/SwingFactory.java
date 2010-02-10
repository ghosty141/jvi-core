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

import com.raelity.jvi.core.Buffer;
import com.raelity.jvi.core.ColonCommands;
import com.raelity.jvi.core.G;
import com.raelity.jvi.options.OptionsBeanBase;
import  static com.raelity.jvi.core.KeyDefs.*;
import  com.raelity.jvi.*;
import  com.raelity.jvi.ViTextView.TAGOP;
import com.raelity.jvi.core.Options;

import  javax.swing.Action;
import  javax.swing.JDialog;
import  javax.swing.JRootPane;
import  javax.swing.SwingUtilities;
import  javax.swing.event.MouseInputAdapter;
import  javax.swing.text.Document;
import  javax.swing.text.TextAction;
import  javax.swing.text.Caret;
import  java.awt.AWTKeyStroke;
import  java.awt.Component;
import  java.awt.Container;
import  java.awt.Dimension;
import  java.awt.Frame;
import  java.awt.KeyboardFocusManager;
import  java.awt.Point;
import  java.awt.event.ActionEvent;
import  java.awt.event.ActionListener;
import  java.awt.event.KeyListener;
import  java.awt.event.MouseEvent;
import  java.beans.IntrospectionException;
import  java.beans.PropertyDescriptor;
import  java.util.Collections;
import  java.util.HashSet;
import  java.util.Map;
import  java.util.Set;
import  java.util.WeakHashMap;
import  java.util.prefs.Preferences;
import javax.swing.text.EditorKit;
import javax.swing.text.JTextComponent;

/**
 * This provides the Vi items to interface with standard swing JTextComponent.
 * <b>NEEDSWORK:</b><ul>
 * <li> only one text view supported for now
 * </ul>
 */
abstract public class SwingFactory implements ViFactory
{
    public static final String PROP_TV = "ViTextView";
    public static final String PROP_BUF  = "ViBuffer";
    public static final String PROP_AV = "ViAppView";

    // Really a WeakSet, all doc's that have been seen. value always null
    protected Map<Document, Object> docSet
            = new WeakHashMap<Document,Object>();

    // This is used only when dbgEditorActivation is turned on
    protected WeakHashMap<JTextComponent, Object> editorSet
            = new WeakHashMap<JTextComponent, Object>();

    JDialog dialog;
    protected static SwingFactory INSTANCE;

    private static final boolean isMac = ViManager.getOsVersion().isMac();
    private MouseInputAdapter mouseAdapter;
    private KeyListener keyListener;

    // ............

    /**
     *  Default constructor.
     */
    public SwingFactory()
    {
        if ( INSTANCE != null ) {
            throw new IllegalStateException("ViFactory already exists");
        }
        INSTANCE = this;

        // Add VimClipboard DataFlavor if not already there
        // FlavorMap fm = SystemFlavorMap.getDefaultFlavorMap();
    }

    //////////////////////////////////////////////////////////////////////
    //
    // Some swing specific things that an implementation may want to override
    //

    /**
     * @return action suitable for default key action
     */
    public Action createCharAction( String name )
    {
        return new EnqueCharAction(name);
    }


    /**
     * @return action for picking up specified key
     */
    public Action createKeyAction( String name, char key )
    {
        return new EnqueKeyAction(name, key);
    }

    //////////////////////////////////////////////////////////////////////
    //
    // ViFactory for swing
    //

    public boolean isEnabled() {
        return true;
    }

    public ViAppView getAppView(Component e) {
        return (ViAppView)((JTextComponent)e).getClientProperty(PROP_AV);
    }

    public Class loadClass( String name ) throws ClassNotFoundException
    {
        // NEEDSWORK: should this be systemclassloader or this's class loader???
        Class c = ClassLoader.getSystemClassLoader().loadClass(name);
        return c;
    }

    public boolean isStandalone()
    {
        return true;
    }

    public final ViTextView getTextView(Component ed)
    {
        return (ViTextView)(((JTextComponent)ed).getClientProperty(PROP_TV));
    }

    public final ViTextView createTextView(Component editor)
    {
        JTextComponent ed = (JTextComponent)editor;
        ViTextView tv01 = (ViTextView)ed.getClientProperty(PROP_TV);
        if ( tv01 == null ) {
            if ( G.dbgEditorActivation.getBoolean() ) {
                System.err.println("Activation: getViTextView: create");
            }
            tv01 = newTextView(ed);
            attachBuffer(tv01);

            tv01.startup();
            ed.putClientProperty(PROP_TV, tv01);
            editorSet.put(ed, null);
        }
        return tv01;
    }

    abstract protected ViTextView newTextView( JTextComponent ed );

    public final Set<ViTextView> getViTextViewSet()
    {
        Set<ViTextView> s = new HashSet<ViTextView>();
        for (JTextComponent ed : editorSet.keySet()) {
            ViTextView tv = (ViTextView) ed.getClientProperty(PROP_TV);
            if(tv != null)
                s.add(tv);
        }

        return s;
    }

    public boolean isNomadic(Component ed, ViAppView av) {
        return false;
    }


    public boolean isShowing( ViTextView tv )
    {
        return tv.getEditorComponent().isShowing();
    }


    abstract protected Buffer createBuffer( ViTextView tv );


    public Set<Buffer> getBufferSet() // NEEDSWORK: collection, list MRU?
    {
        Set<Buffer> s = new HashSet<Buffer>();
        for (Document doc : docSet.keySet()) {
            Buffer buf = (Buffer) doc.getProperty(PROP_BUF);
            if ( buf != null ) {
                s.add(buf);
            }
        }

        return s;
    }


    public void shutdown( Component editor )
    {
        JTextComponent ed = (JTextComponent)editor;
        ViTextView tv = (ViTextView)ed.getClientProperty(PROP_TV);
        if ( tv == null ) {
            return;
        }

        if ( G.dbgEditorActivation.getBoolean() ) {
            System.err.println("Activation: shutdown TV");
        }
        Buffer buf = tv.getBuffer();
        tv.shutdown();
        ed.putClientProperty(PROP_TV, null);
        releaseBuffer(buf);
    }


    public void changeBuffer(ViTextView tv, Object _oldDoc)
    {
        Document oldDoc = (Document) _oldDoc;
        if ( G.dbgEditorActivation.getBoolean() ) {
            System.err.println("Activation: changeBuffer");
        }
        attachBuffer(tv);
        releaseBuffer((Buffer)oldDoc.getProperty(PROP_BUF));
    }


    private void attachBuffer(ViTextView tv)
    {
        Document doc = ((JTextComponent)tv.getEditorComponent()).getDocument();
        Buffer buf = null;
        if ( doc != null )
        {
            buf = (Buffer)doc.getProperty(PROP_BUF);
            if ( buf == null )
            {
                buf = createBuffer(tv);
                doc.putProperty(PROP_BUF, buf);
                docSet.put(doc, null);
            }
            buf.addShare();
        }
        tv.attachBuffer(buf);
    }


    private void releaseBuffer(Buffer buf)
    {
        if ( buf != null ) {
            Document doc = (Document)buf.getDocument();
            buf.removeShare();
            if(buf.getShare() == 0) {
                if ( doc != null) {
                    doc.putProperty(PROP_BUF, null);
                } else {
                    ViManager.dumpStack("SHUTDOWN NULL DOC");
                }
            }
        }
    }


    public String getPlatformSelectionDisplayName()
    {
        return "PLATFORM-SELECTION";
    }

    public void setShutdownHook(Runnable hook) {
        Runtime.getRuntime().addShutdownHook(new Thread(hook));
    }


    /**
     *  Get the glass pane for the given component, if it doesn't
     *  have an associated mouseAdapter create one and add it.
     */
    private Container getModalGlassPane( final Component c )
    {
        Container glass = null;
        if ( c != null ) {
            JRootPane rp = SwingUtilities.getRootPane(c);
            if ( rp != null ) {
                glass = (Container) rp.getGlassPane();
                if ( mouseAdapter == null ) {
                    mouseAdapter = new MouseInputAdapter() {
                        @Override
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


    /**
     *  Method to establish a glass pane with the param key listener and all
     *  mouse events are blocked.
     *  This is not modal, in particular the event thread is still running, but
     *  it blocks the current window.
     */
    public void startGlassKeyCatch(KeyListener kl)
    {
        if ( mouseAdapter != null ) {
            throw new IllegalStateException("Already in modal state");
        }

        Container glass = getModalGlassPane(G.curwin.getEditorComponent());
        keyListener = kl;
        glass.addKeyListener(kl);
        glass.setVisible(true);

        // disable all focus traversal
        Set<AWTKeyStroke>noKeyStroke = Collections.emptySet();
        glass.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                                    noKeyStroke);
        glass.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                                    noKeyStroke);
        glass.setFocusTraversalKeys(KeyboardFocusManager.DOWN_CYCLE_TRAVERSAL_KEYS,
                                    noKeyStroke);
        glass.setFocusTraversalKeys(KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS,
                                    noKeyStroke);
        glass.setFocusCycleRoot(true);

        glass.requestFocusInWindow();
    }


    public void stopGlassKeyCatch()
    {
        if ( mouseAdapter == null ) {
            throw new IllegalStateException("Not in modal state");
        }
        Container glass = getModalGlassPane(G.curwin.getEditorComponent());
        glass.setVisible(false);
        glass.removeMouseListener(mouseAdapter);
        glass.removeMouseMotionListener(mouseAdapter);
        glass.removeKeyListener(keyListener);
        mouseAdapter = null;
        keyListener = null;

        // Back to default bahavior
        glass.setFocusCycleRoot(false);
        glass.setFocusTraversalKeys(
                KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        glass.setFocusTraversalKeys(
                KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
        glass.setFocusTraversalKeys(
                KeyboardFocusManager.DOWN_CYCLE_TRAVERSAL_KEYS, null);
        glass.setFocusTraversalKeys(
                KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS, null);
        G.curwin.getEditorComponent().requestFocusInWindow();
    }


    public void startModalKeyCatch( KeyListener kl )
    {
        JTextComponent ed = (JTextComponent)G.curwin.getEditorComponent();
        java.awt.Window wep = SwingUtilities.getWindowAncestor(ed);
        dialog = new JDialog((Frame)wep, "jVi", true);
        dialog.setUndecorated(true); // on windows see nothing, perfect
        dialog.pack();
        // place dialog in lower left of editor
        Container jc = SwingUtilities.getAncestorOfClass(
                javax.swing.JScrollPane.class,
                G.curwin.getEditorComponent());
        if ( jc == null ) {
            jc = (JTextComponent)G.curwin.getEditorComponent();
        }
        // put the dialog just below the editor pane, on the right
        Dimension d00 = dialog.getSize();
        Point p00 = jc.getLocation();
        p00.translate(jc.getWidth() - (int)d00.getWidth(), jc.getHeight());
        SwingUtilities.convertPointToScreen(p00, jc.getParent());
        dialog.setLocation(p00);

        Component glass = dialog.getGlassPane();
        glass.addKeyListener(kl);
        glass.setVisible(true);
        glass.requestFocusInWindow();

        dialog.setVisible(true);
    }


    public void stopModalKeyCatch()
    {
        if ( dialog == null ) {
            throw new IllegalStateException("Not in modal state");
        }
        dialog.setVisible(false);
        dialog.dispose();
        dialog = null;
    }


    /**
     * Register editor pane for use with vi. Install a
     * vi cursor. This is a nop
     * if already registered.
     */
    public void setupCaret( Component editor)
    {
        JTextComponent ed = (JTextComponent)editor;
        // install cursor if neeeded
        Caret c = ed.getCaret();
        if ( !(c instanceof ViCaret) ) {
            SwingCaret caret = new SwingCaret();
            ed.setCaret(caret);
            caret.setDot(c.getDot());
            caret.setBlinkRate(c.getBlinkRate());
            caret.setVisible(c.isVisible());
        }
    }


    public PropertyDescriptor createPropertyDescriptor(
            String optName,
            String methodName,
            Class clazz )
            throws IntrospectionException
    {
        return OptionsBeanBase.createPropertyDescriptor(
                optName, methodName, clazz);
    }


    public ViCmdEntry createCmdEntry( int type )
    {
        // ViCmdEntry cmdEntry = new DefaultCmdEntry(cmdLine);
        // return cmdEntry;

        // use this instead so that input is grabbed. When we have a
        // cleaner and more understandable key input state machine revisit
        // this.

        if ( G.useFrame.getBoolean() ) {
            return new WindowCmdEntry(type);
        } else {
            return new InlineCmdEntry(type);
        }
    }


    /*
    public Keymap getInsertModeKeymap()
    {
        return KeyBinding.insertModeKeymap;
    }

    public Keymap getNormalModeKeymap()
    {
        return KeyBinding.normalModeKeymap;
    }
    */


    public Action createInsertModeKeyAction(String name, char vkey, String desc)
    {
        return new InsertModeAction(name, vkey, desc);
    }

    public Action createNormalModeKeyAction(String name, int vkey, String desc)
    {
         return null;
    }

    public ActionListener xlateKeymapAction( ActionListener act )
    {
        return act;
    }

    public Preferences getPreferences()
    {
        return Preferences.userRoot().node(ViManager.PREFS_ROOT);
      //return Preferences.userNodeForPackage(Options.class);
    }


    /**
     * This is the default key action.
     * Ignore all Ctrl characters (which includes that troublesome Ctrl-space).
     * Control characters of interest are picked up as key-press events
     * in {link #EnqKeyAction}.
     */
    public static class EnqueCharAction extends TextAction
    {
        public EnqueCharAction(String name) {
            super(name);
        }


        public void actionPerformed( ActionEvent e )
        {
            JTextComponent target = (JTextComponent)getTextComponent(e);
            if ( target != null && e != null ) {
                String content = e.getActionCommand();
                if ( content != null && content.length() > 0 ) {
                    // Check whether the modifiers are OK
                    int mod = e.getModifiers();
                    boolean ctrl = ((mod & ActionEvent.CTRL_MASK) != 0);
                    // On the mac, norwegian and french keyboards use Alt to do
                    // bracket characters. This replicates Apple's modification
                    // DefaultEditorKit.DefaultKeyTypedAction
                    boolean alt = isMac
                            ? ((mod & ActionEvent.META_MASK) != 0)
                            : ((mod & ActionEvent.ALT_MASK) != 0);

                    char c = content.charAt(0);
                    boolean keep = true;
                    if ( alt || ctrl
                            || content.length() != 1
                            || c < 0x20
                            || c == 0x7f ) {
                        // the delete key comes in as a virtual key.
                        // Wouldn't have thought that the 'c<0x20' was needed,
                        // <RETURN>,<BS> come in < 0x20 without the Control key
                        keep = false;
                    }

                    if ( Options.isKeyDebug() && c >= 0x20 ) {
                        System.err.println("CharAction: "
                                + (keep ? "" : "REJECT: ")
                                + "'" + content + "' "
                                + String.format("%x", (int)c)
                                + "(" + (int)c + ") " + mod);
                    }
                    if ( keep ) {
                        ViManager.keyStroke(target, c, mod);
                    }
                } else {
                    if  ( Options.isKeyDebug() ) {
                      System.err.println("CharAction: " + e);
                    }
                }
            }
        }

        @Override
        public String toString() {
            return "jVi[DKTA]";
        }

    } // end inner class EnqueCharAction


    /**
     * Catch non-printing keys with this class. The constructor
     * specifies which key. The modifiers are handled by examining
     * the event and added to the key. Recieved characters are placed
     * on the vi input Q.
     */
    public static class EnqueKeyAction extends TextAction
    {
        char basekey;

        public EnqueKeyAction(String name, char key) {
            super(name);
            this.basekey = key;
        }

        public void actionPerformed( ActionEvent e )
        {
            JTextComponent target = getTextComponent(e);
            int mod = e.getModifiers();
            char key = basekey;
            if ( Options.isKeyDebug() ) {
                String virt = ((key & 0xF000) == VIRT) ? "virt" : "";
                System.err.println("KeyAction: "
                        + getValue(Action.NAME).toString()
                        + ": " + String.format("%x", (int)key)
                        + "(" + ((int)key&~VIRT) + ") " + mod + " " + virt);
            }
            ViManager.keyStroke(target, key, mod);
        }

        @Override
        public String toString() {
            return getValue(Action.NAME).toString();
        }
    }


    private static class InsertModeAction extends TextAction
            implements ViXlateKey
    {
        char basekey;

        public InsertModeAction(String name, char vkey, String desc) {
            super(name); // ??????????????????????
            this.basekey = vkey;

            // if name starts with Vi and ends with Key, then put out a message
            // with the name of the key in it
            //this.putValue(Action.LONG_DESCRIPTION, desc);
            //this.putValue("ActionGroup", GROUP_VI_EDIT);
            //EditorActions.addBindableEditorAction(this,
            //                                      JBViKeymap.VI_EDIT_KEYMAP);
        }

        public void actionPerformed(ActionEvent e) {
            // NOT USED for the translation keymap
        }

        public char getXlateKey() {
            return basekey;
        }

    } // end inner class InsertModeAction


    public void startTagPush( ViTextView tv, String ident ) {}

    public void finishTagPush( ViTextView tv ) {}

    public void tagStack( TAGOP op, int count ) {}

    public void displayTags() {}

    public void tagDialog( ColonCommands.ColonEvent e ) {}

    public void commandEntryAssist(ViCmdEntry cmdEntry, boolean enable ) {}


} // end com.raelity.jvi.swing.DefaultViFactory