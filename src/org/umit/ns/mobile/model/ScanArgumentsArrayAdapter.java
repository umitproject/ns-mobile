package org.umit.ns.mobile.model;

import android.R;
import android.content.Context;
import android.content.res.ColorStateList;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

public class ScanArgumentsArrayAdapter<T> extends ArrayAdapter<T> implements ScanArgsConst{
	private List<T> mObjects;
	private int mResource;
	private int mDropDownResource;
	private int mFieldId = 0;
	private Context mContext;
	private LayoutInflater mInflater;

	public static final int RED_COLOR = 0xFFAA0000;
	public static final int BLACK_COLOR = 0xFF000000;

	private boolean mRootAccess;

	public ScanArgumentsArrayAdapter(Context context, int textViewResourceId, List<T> objects, boolean rootAccess) {
		super(context, textViewResourceId, objects);
		init(context, textViewResourceId, 0, objects);
		mRootAccess = rootAccess;
	}

	private void init(Context context, int resource, int textViewResourceId, List<T> objects) {
		mContext = context;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mResource = mDropDownResource = resource;
		mObjects = objects;
		mFieldId = textViewResourceId;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return createViewFromResource(position, convertView, parent, mResource);
	}

	private View createViewFromResource(int position, View convertView, ViewGroup parent,
	                                    int resource) {
		View view;
		TextView text;

		if (convertView == null) {
			view = mInflater.inflate(resource, parent, false);
		} else {
			view = convertView;
		}

		try {
			if (mFieldId == 0) {
				//  If no custom field is assigned, assume the whole resource is a TextView
				text = (TextView) view;
			} else {
				//  Otherwise, find the TextView field within the layout
				text = (TextView) view.findViewById(mFieldId);
			}
		} catch (ClassCastException e) {
			Log.e("ArrayAdapter", "You must supply a resource ID for a TextView");
			throw new IllegalStateException(
					"ArrayAdapter requires the resource ID to be a TextView", e);
		}

		String plainArg;
		T item = getItem(position);
		if (item instanceof CharSequence) {
			text.setText((CharSequence) item);
			plainArg = ARGS_MAP.get(((CharSequence) item).toString());
		} else {
			text.setText(item.toString());
			plainArg = ARGS_MAP.get(item.toString());
		}


		if(ROOT_ARGS.contains( plainArg )){
			text.setTextColor(RED_COLOR);
		} else {
			text.setTextColor(BLACK_COLOR);
		}

		return view;
	}


}
