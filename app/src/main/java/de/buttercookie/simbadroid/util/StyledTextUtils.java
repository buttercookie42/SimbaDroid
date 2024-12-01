/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

// Adapted from https://gist.github.com/sergio-sastre/371191e5067c73af747f3d0939e0db29

package de.buttercookie.simbadroid.util;

import android.content.Context;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextUtils;

import androidx.annotation.PluralsRes;
import androidx.annotation.StringRes;
import androidx.core.text.HtmlCompat;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public class StyledTextUtils {

    private StyledTextUtils() {}

    public static CharSequence getStyledText(Context context, @StringRes int id,
                                             Object... formatArgs) {
        return getStyledCharSequence(context.getText(id), formatArgs);
    }

    public static CharSequence getStyledQuantityText(Context context, @PluralsRes int id,
                                                     int quantity, Object... formatArgs) {
        return getStyledCharSequence(
                context.getResources().getQuantityText(id, quantity), formatArgs);
    }

    private static CharSequence getStyledCharSequence(CharSequence spannedStringFromRes,
                                                      Object... formatArgs) {
        Object[] escapedArgs = Arrays.stream(formatArgs).map(
                obj -> obj instanceof String str ? TextUtils.htmlEncode(str) : obj).toArray();

        SpannedString spannedString = new SpannedString(spannedStringFromRes);
        StringBuilder sb = new StringBuilder(HtmlCompat.
                toHtml(spannedString, HtmlCompat.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL));
        sb.setLength(sb.lastIndexOf(">"));
        sb.append(">");
        String htmlString = sb.toString();

        String dynamicStyledString = String.format(htmlString, escapedArgs);
        Spanned result = HtmlCompat.fromHtml(dynamicStyledString, HtmlCompat.FROM_HTML_MODE_COMPACT);
        return snipTrailingNewline(result);
    }

    private static CharSequence snipTrailingNewline(CharSequence sequence) {
        if (StringUtils.endsWith(sequence, StringUtils.LF)) {
            return sequence.subSequence(0, sequence.length() - 1);
        } else {
            return sequence;
        }
    }
}
