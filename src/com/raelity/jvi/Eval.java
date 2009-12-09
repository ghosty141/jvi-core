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

import com.raelity.text.TextUtil.MySegment;
import java.text.CharacterIterator;
import static com.raelity.jvi.Constants.*;

/**
 *
 * @author Ernie Rael <err at raelity.com>
 */
public class Eval {

    /** NEEDSWORK: make class SaveCpo part of G */
    public static class SaveCpo {
        boolean w;
        boolean search;
        boolean j;

        public SaveCpo() {
            w      = G.p_cpo_w.getBoolean();
            search = G.p_cpo_search.getBoolean();
            j      = G.p_cpo_j.getBoolean();
        }

        public void restore() {
            G.p_cpo_w.setBoolean(w);
            G.p_cpo_search.setBoolean(search);
            G.p_cpo_j.setBoolean(j);
        }

        static public void clearCpo() {
            G.p_cpo_w.setBoolean(false);
            G.p_cpo_search.setBoolean(false);
            G.p_cpo_j.setBoolean(false);
        }
    }

/*
 * Search for a start/middle/end thing.
 * Used by searchpair(), see its documentation for the details.
 * Returns 0 or -1 for no match,
 *
 * NOTE: use of searchit in V7 is different!
 */
public static int do_searchpair(
            //spat, mpat, epat, dir, skip, flags, match_pos, lnum_stop)
    String	spat,	    /* start pattern */
    String	mpat,	    /* middle pattern */
    String	epat,	    /* end pattern */
    int		dir,	    /* BACKWARD or FORWARD */
    String	skip,	    /* skip expression */
    int		flags,	    /* SP_SETPCMARK and other SP_ values */
    ViFPOS	match_pos,      // NOTE: was *match_pos to RETURN a value
    int	lnum_stop  /* stop at this line if not zero */
)
{
    SaveCpo	save_cpo;
    String	pat, pat2 = null, pat3 = null;
    int		retval = 0;
    ViFPOS	pos;
    ViFPOS	firstpos;
    ViFPOS	foundpos;
    ViFPOS	save_cursor;
    ViFPOS	save_pos;
    int		n;
    boolean	r;
    int		nest = 1;
    boolean	err;
    int		options = SEARCH_KEEP;

    /* Make 'cpoptions' empty, the 'l' flag should not be used here. */
    save_cpo = new SaveCpo();
    SaveCpo.clearCpo(); // p_cpo = (char_u *)"";

    /* Make two search patterns: start/end (pat2, for in nested pairs) and
     * start/middle/end (pat3, for the top pair). */

    //sprintf((char *)pat2, "\\(%s\\m\\)\\|\\(%s\\m\\)", spat, epat);
    pat2 = String.format("(%s)|(%s)", spat, epat);
    if (mpat.length() == 0)
        pat3 = pat2; //STRCPY(pat3, pat2);
    else {
        //sprintf((char *)pat3, "\\(%s\\m\\)\\|\\(%s\\m\\)\\|\\(%s\\m\\)",
        //                                                  spat, epat, mpat);
        pat3 = String.format("(%s)|(%s)|(%s)", spat, epat, mpat);
    }
    if ((flags & SP_START) != 0)
        options |= SEARCH_START;

    save_cursor = G.curwin.w_cursor.copy();
    pos = G.curwin.w_cursor.copy();
    firstpos = null; //clearpos(&firstpos);
    foundpos = null; //clearpos(&foundpos);
    pat = pat3;
    for (;;)
    {
        n = Search.searchit(G.curwin,
                            //curbuf,
                            pos, dir, pat, 1,
                            options,
                            0, // was: RE_SEARCH. NOTE: param is ignored
                            false); // was lnum_stop);
        if (n == FAIL || equalpos(pos, firstpos))
            /* didn't find it or found the first match again: FAIL */
            break;

        if (firstpos == null)
            firstpos = pos.copy();
        if (equalpos(pos, foundpos))
        {
            /* Found the same position again.  Can happen with a pattern that
             * has "\zs" at the end and searching backwards.  Advance one
             * character and try again. */
            if (dir == BACKWARD)
                decl(pos);
            else
                inclV7(pos);
        }
        foundpos = pos.copy();

        /* If the skip pattern matches, ignore this match. */
        if (skip.length() != 0)
        {
            save_pos = G.curwin.w_cursor.copy();
            G.curwin.w_cursor.set(pos);
            if(true)
                throw new RuntimeException("eval_to_bool not implemented");
            //r = eval_to_bool(skip, &err, null, false);
            G.curwin.w_cursor.set(save_pos);
            if (err)
            {
                /* Evaluating {skip} caused an error, break here. */
                G.curwin.w_cursor.set(save_cursor);
                retval = -1;
                break;
            }
            if (r)
                continue;
        }

        if ((dir == BACKWARD && n == 3) || (dir == FORWARD && n == 2))
        {
            /* Found end when searching backwards or start when searching
             * forward: nested pair. */
            ++nest;
            pat = pat2;		/* nested, don't search for middle */
        }
        else
        {
            /* Found end when searching forward or start when searching
             * backward: end of (nested) pair; or found middle in outer pair. */
            if (--nest == 1)
                pat = pat3;	/* outer level, search for middle */
        }

        if (nest == 0)
        {
            /* Found the match: return matchcount or line number. */
            if ((flags & SP_RETCOUNT) != 0)
                ++retval;
            else
                retval = pos.getLine();
            if ((flags & SP_SETPCMARK) != 0)
                setpcmark();
            G.curwin.w_cursor.set(pos);
            if ((flags & SP_REPEAT) == 0)
                break;
            nest = 1;	    /* search for next unmatched */
        }
    }

    if (match_pos != null)
    {
        /* Store the match cursor position */
        match_pos.set(G.curwin.w_cursor.getLine(),
                      G.curwin.w_cursor.getColumn() + 1);
    }

    /* If 'n' flag is used or search failed: restore cursor position. */
    if ((flags & SP_NOMOVE) != 0 || retval == 0)
        G.curwin.w_cursor.set(save_cursor);

    save_cpo.restore(); // p_cpo = save_cpo;

    return retval;
}

  // These bounce routines to make porting easier
//Misc
private static int dec_cursor() { return Misc.dec_cursor(); }
private static int decl(ViFPOS pos) { return Misc.decl(pos); }
private static int del_char(boolean f) { return Misc.del_char(f); }
private static int do_join(boolean insert_space, boolean redraw) { return Misc.do_join(insert_space, redraw); }
private static void do_put(int regname_, int dir, int count, int flags) { Misc.do_put(regname_, dir, count, flags);}
private static char gchar_pos(ViFPOS pos) { return Misc.gchar_pos(pos); }
private static char gchar_cursor() { return Misc.gchar_cursor(); }
private static void getvcol(ViTextView tv, ViFPOS fpos, MutableInt start,
                            MutableInt cursor, MutableInt end)
                    { Misc.getvcol(tv, fpos, start, cursor, end); }
private static int inc_cursor() { return Misc.inc_cursor(); }
private static int inc_cursorV7() { return Misc.inc_cursorV7(); }
private static int inclV7(ViFPOS pos) { return Misc.inclV7(pos); }
private static void ins_char(char c) { Misc.ins_char(c); }
private static int skipwhite(MySegment seg, int idx) { return Misc.skipwhite(seg, idx); }
private static boolean vim_iswhite(char c) { return Misc.vim_iswhite(c); }
private static boolean vim_iswordc(char c) { return Misc.vim_iswordc(c); }

// Util
private static boolean ascii_isalpha(char c) { return Util.ascii_isalpha(c); }
private static void beep_flush() { Util.beep_flush(); }
private static boolean bufempty() { return Util.bufempty(); }
private static int CharOrd(char c) { return Util.CharOrd(c); }
private static final char ctrl(char x) { return Util.ctrl(x); }
private static int hex2nr(char c) { return Util.hex2nr(c); }
private static boolean isalpha(char c) { return Util.isalpha(c); }
private static boolean isdigit(char c) {return Util.isdigit(c); }
private static boolean isupper(char c) { return Util.isupper(c); }
private static MySegment ml_get(int lnum) { return Util.ml_get(lnum); }
private static MySegment ml_get_curline() { return Util.ml_get_curline(); }
private static CharacterIterator ml_get_cursor() { return Util.ml_get_cursor();}
private static int strncmp(String s1, String s2, int n) { return Util.strncmp(s1, s2, n); }
private static int strncmp(MySegment seg, int i, String s2, int n) { return Util.strncmp(seg, i, s2, n); }
private static void vim_beep() { Util.vim_beep(); }
private static boolean vim_isdigit(char c) {return Util.isdigit(c); }
public static boolean vim_isspace(char x) { return Util.vim_isspace(x); }
private static boolean vim_isxdigit(char c) { return Util.isxdigit(c); }
private static String vim_strchr(String s, char c) { return Util.vim_strchr(s, c); }

private static void vim_str2nr(MySegment seg, int start,
                               MutableInt pHex, MutableInt pLength,
                               int dooct, int dohex,
                               MutableInt pN, MutableInt pUn)
{ Util.vim_str2nr(seg, start, pHex, pLength, dooct, dohex, pN, pUn); }

// GetChar
private static void AppendCharToRedobuff(char c) { GetChar.AppendCharToRedobuff(c); }
private static void stuffReadbuff(String s) { GetChar.stuffReadbuff(s); }
private static void stuffcharReadbuff(char c) { GetChar.stuffcharReadbuff(c); }
private static void vungetc(char c) { GetChar.vungetc(c); }

// Normal
private static boolean add_to_showcmd(char c) { return Normal.add_to_showcmd(c); }
private static void clear_showcmd() { Normal.clear_showcmd(); }
private static CharacterIterator find_ident_under_cursor(MutableInt mi, int find_type)
    {return Normal.find_ident_under_cursor(mi, find_type);}
private static int u_save_cursor() { return Normal.u_save_cursor(); }

// Options
private static boolean can_bs(int what) { return Options.can_bs(what); }

// MarkOps
private static void setpcmark() {MarkOps.setpcmark();}
private static void setpcmark(ViFPOS pos) {MarkOps.setpcmark(pos);}

// cursor compare
private static boolean equalpos(ViFPOS p1, ViFPOS p2) { return Util.equalpos(p1, p2); }
private static boolean lt(ViFPOS p1, ViFPOS p2) { return Util.lt(p1, p2); }

}
