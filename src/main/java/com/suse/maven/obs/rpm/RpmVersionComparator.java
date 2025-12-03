/*
 * Copyright (c) 2025 SUSE LLC
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 */
package com.suse.maven.obs.rpm;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Optional;

/**
 * A comparator for RPM version strings that implements the standard RPM version comparison
 * algorithm.
 * <p>
 * This implementation aims to match the behavior of the native {@code rpmvercmp} function.
 * @see <a href="https://github.com/rpm-software-management/rpm/blob/rpm-4.20.x/rpmio/rpmvercmp.c">rpmvercmp.c</a>
 */
public class RpmVersionComparator implements Comparator<String> {

    /**
     * Compares two RPM version strings.
     * @param v1 the first version string.
     * @param v2 the second version string.
     * @return a negative integer, zero, or a positive integer as the first argument
     *     is less than, equal to, or greater than the second.
     */
    @Override
    public int compare(@Nullable String v1, @Nullable String v2) {
        if (v1 == null || v2 == null || v1.equals(v2)) {
            return Strings.CS.compare(v1, v2);
        }

        StringCursor c1 = new StringCursor(v1);
        StringCursor c2 = new StringCursor(v2);

        while (c1.hasRemaining() || c2.hasRemaining()) {
            c1.skipSeparators();
            c2.skipSeparators();

            // Check for Tilde (~) and Caret (^) logic
            Optional<Integer> specialResult = compareSpecialSeparators(c1, c2);
            if (specialResult.isPresent()) {
                int result = specialResult.get();
                // 0 means we found matching separators, so we just continue the loop
                if (result == 0) {
                    continue;
                }

                // Non-zero means we found a definitive difference
                return result;
            }

            // End-of-String checks
            Optional<Integer> terminationResult = checkTermination(c1, c2);
            if (terminationResult.isPresent()) {
                return terminationResult.get();
            }

            // Segment Comparison
            String seg1 = c1.readNextSegment();
            String seg2 = c2.readNextSegment();

            int result = compareSegments(seg1, seg2);
            if (result != 0) {
                return result;
            }
        }

        return 0;
    }

    private Optional<Integer> compareSpecialSeparators(StringCursor c1, StringCursor c2) {
        char ch1 = c1.peek();
        char ch2 = c2.peek();

        // Tilde (~): Sorts BEFORE everything
        if (ch1 == '~' || ch2 == '~') {
            return Optional.of(compareTilde(c1, c2, ch1, ch2));
        }

        // Caret (^): Sorts AFTER End-of-String, BEFORE other chars
        if (ch1 == '^' || ch2 == '^') {
            return Optional.of(compareCaret(c1, c2, ch1, ch2));
        }

        return Optional.empty();
    }

    // Logic for the Caret (^) operator: End-of-String < Caret < Any other character.
    // Caret is used for snapshots that should sort higher than the base version.
    private int compareCaret(StringCursor c1, StringCursor c2, char ch1, char ch2) {
        if (!c1.hasRemaining()) {
            // End < Caret
            return -1;
        }
        if (!c2.hasRemaining()) {
            // Caret > End
            return 1;
        }

        if (ch1 != '^') {
            // Other char > Caret
            return 1;
        }
        if (ch2 != '^') {
            // Caret < Other char
            return -1;
        }

        c1.advance();
        c2.advance();

        return 0;
    }

    // Logic for the Tilde (~) operator: Tilde < End-of-String < Any other character.
    // Tilde is used for pre-releases (RCs, betas) that should sort lower than the base version.
    private int compareTilde(StringCursor c1, StringCursor c2, char ch1, char ch2) {
        if (ch1 != '~') {
            // Other > Tilde
            return 1;
        }
        if (ch2 != '~') {
            // Tilde < Other
            return -1;
        }

        c1.advance();
        c2.advance();

        return 0;
    }

    private Optional<Integer> checkTermination(StringCursor c1, StringCursor c2) {
        if (!c1.hasRemaining() && !c2.hasRemaining()) {
            return Optional.of(0);
        }

        if (!c1.hasRemaining()) {
            return Optional.of(-1);
        }

        if (!c2.hasRemaining()) {
            return Optional.of(1);
        }

        return Optional.empty();
    }

    private int compareSegments(String seg1, String seg2) {
        boolean isNum1 = CharUtils.isAsciiNumeric(seg1.charAt(0));
        boolean isNum2 = CharUtils.isAsciiNumeric(seg2.charAt(0));

        // Numeric segments are always considered "newer" (greater) than alpha segments.
        if (isNum1 != isNum2) {
            return isNum1 ? 1 : -1;
        }

        // Numeric segments are compared by value.
        if (isNum1) {
            String s1Clean = StringUtils.stripStart(seg1, "0");
            String s2Clean = StringUtils.stripStart(seg2, "0");

            int lenDiff = s1Clean.length() - s2Clean.length();
            if (lenDiff != 0) {
                return lenDiff;
            }

            return s1Clean.compareTo(s2Clean);
        }

        // Alpha segments are compared lexicographically.
        return seg1.compareTo(seg2);
    }

    /**
     * Helper class to traverse a version string, handling character lookahead and segmentation logic.
     */
    private static class StringCursor {
        private final String text;
        private final int length;
        private int pos;

        StringCursor(String text) {
            this.text = text;
            this.length = text.length();

            this.pos = 0;
        }

        boolean hasRemaining() {
            return pos < length;
        }

        char peek() {
            return hasRemaining() ? text.charAt(pos) : 0;
        }

        void advance() {
            if (hasRemaining()) {
                pos++;
            }
        }

        void skipSeparators() {
            while (hasRemaining()) {
                char c = text.charAt(pos);
                if (CharUtils.isAsciiAlphanumeric(c) || c == '~' || c == '^') {
                    break;
                }

                pos++;
            }
        }

        String readNextSegment() {
            if (!hasRemaining()) {
                return "";
            }

            int start = pos;
            boolean parsingDigit = CharUtils.isAsciiNumeric(text.charAt(pos));

            while (hasRemaining()) {
                char c = text.charAt(pos);
                if ((parsingDigit && !CharUtils.isAsciiNumeric(c)) || (!parsingDigit && !CharUtils.isAsciiAlpha(c))) {
                    break;
                }

                pos++;
            }
            return text.substring(start, pos);
        }
    }
}
