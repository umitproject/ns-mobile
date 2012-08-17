package org.umit.ns.mobile.model;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.method.QwertyKeyListener;
import android.util.AttributeSet;
import android.widget.MultiAutoCompleteTextView;

public class ScanMultiAutoCompleteTextView
		extends MultiAutoCompleteTextView
		implements ScanArgsConst{
	private Tokenizer mTokenizer;

	public ScanMultiAutoCompleteTextView(Context context) {
		super(context);
	}

	public ScanMultiAutoCompleteTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ScanMultiAutoCompleteTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public void setTokenizer(Tokenizer t) {
		super.setTokenizer(t);
		mTokenizer = t;
	}

	@Override
	protected void replaceText(CharSequence selectedText) {
		CharSequence text = ARGS_MAP.get(selectedText);

		if(ROOT_ARGS.contains(text))
			return;

		clearComposingText();

		int end = getSelectionEnd();
		int start = mTokenizer.findTokenStart(getText(), end);

		Editable editable = getText();
		String original = TextUtils.substring(editable,start,end);

		QwertyKeyListener.markAsReplaced(editable, start, end, original);
		editable.replace(start,end,mTokenizer.terminateToken(text));

	}
}
