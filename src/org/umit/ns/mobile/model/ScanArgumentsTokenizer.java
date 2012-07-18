package org.umit.ns.mobile.model;

import android.widget.MultiAutoCompleteTextView;

public class ScanArgumentsTokenizer implements MultiAutoCompleteTextView.Tokenizer, ScanArgsConst {
	public int findTokenStart(CharSequence text, int cursor) {
		int i = cursor;

		while (i > 0 && text.charAt(i - 1) != ' ') {
			i--;
		}

		return i;
	}

	public int findTokenEnd(CharSequence text, int cursor) {
		int i = cursor;
		int len = text.length();

		while (i < len) {
			if (text.charAt(i) == ' ') {
				return i;
			} else {
				i++;
			}
		}

		return len;
	}

	public CharSequence terminateToken(CharSequence text) {
		int i = text.length();

		while (i > 0 && text.charAt(i - 1) == ' ') {
			i--;
		}

		StringBuilder token = new StringBuilder();
		while(i>0 && text.charAt(i-1)!=' '){
			token.append(text.charAt(i-1));
			i--;
		}

		if(NO_SPACE_ARGS.contains(token))
			return text;
		else
			return text + " ";

	}
}
