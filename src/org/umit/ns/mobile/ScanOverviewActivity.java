package org.umit.ns.mobile;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.umit.ns.mobile.provider.Scanner;
import org.umit.ns.mobile.provider.Scanner.Scans;

import android.database.Cursor;
import android.os.Bundle;

public class ScanOverviewActivity extends Activity {

	ListView scansListView;
	ScansAdapter scansAdapter;

	static int scanArgumentsColumn;
	static int taskNameColumn;
	static int taskProgressColumn;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.scan_overview_activity);

		scansListView = (ListView)findViewById(R.id.Scans);

		Cursor cursor = getContentResolver().query(Scanner.SCANS_URI, null, null, null, null);
		startManagingCursor(cursor);

		scanArgumentsColumn = cursor.getColumnIndex(Scans.SCAN_ARGUMENTS);
		taskNameColumn = cursor.getColumnIndex(Scans.TASK);
		taskProgressColumn = cursor.getColumnIndex(Scans.TASK_PROGRESS);

		scansAdapter = new ScansAdapter(this,cursor);
		scansListView.setAdapter(scansAdapter);
		int count = scansListView.getCount();
	}

	public static class ScansAdapter extends CursorAdapter {
		public ScansAdapter(Context context, Cursor c) {
			super(context, c);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			final LayoutInflater inflater = LayoutInflater.from(context);
			final RelativeLayout view =  (RelativeLayout)inflater.inflate(
					R.layout.scan_overview_item, parent, false);

			TextView scanArguments = (TextView) view.findViewById(R.id.ScanArguments);
		  ProgressBar progress = (ProgressBar) view.findViewById(R.id.Progress);
			TextView taskName = (TextView) view.findViewById(R.id.TaskName);
			Button stopButton = (Button) view.findViewById(R.id.StopButton);

			scanArguments.setText(cursor.getString(scanArgumentsColumn));
			progress.setProgress(cursor.getInt(taskProgressColumn));
			taskName.setText(cursor.getString(taskNameColumn));
			stopButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {

				}
			});

			return view;
		}

		@Override
		public void bindView(View v, Context context, Cursor cursor) {
			ViewGroup view = (ViewGroup) v;

			TextView scanArguments = (TextView) view.findViewById(R.id.ScanArguments);
			ProgressBar progress = (ProgressBar) view.findViewById(R.id.Progress);
			TextView taskName = (TextView) view.findViewById(R.id.TaskName);
			Button stopButton = (Button) view.findViewById(R.id.StopButton);

			scanArguments.setText(cursor.getString(scanArgumentsColumn));
			progress.setProgress(cursor.getInt(taskProgressColumn));
			taskName.setText(cursor.getString(taskNameColumn));
			stopButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {

				}
			});
		}
	}
}