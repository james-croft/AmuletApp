package croft.james.amulet;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Random;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.widget.EditText;
import android.widget.Toast;

public class SequenceFragment extends Fragment {

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		inflater.inflate(R.layout.fragment_sequence, container, false);
		super.onCreateView(inflater, container, savedInstanceState);

		getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		return new SequenceDrawView(getActivity());
	}

	private class SequenceDrawView extends View implements OnRetrieveDataCompleted, ITaskActivity {
		Context _context;
		Task _sequenceTask;
		CountDownTimer _startTimer;
		AssetManager _assetMgr;
		Drink drinkRecord;

		String _timerText = "";
		int _canvasWidth, _canvasHeight;
		boolean _hasStarted, _isFinished, _is9GridView, _is16GridView, _isRandom, _isCalibration;

		Rect _overlay;
		Paint _strokePaint, _textPaint, _coverPaint;
		SequenceDrawView _view;

		CanvasButton _startGridBtn, _calibBtn;
		Vector<SequenceGridButton> _sequenceButtons;
		Vector<Integer> _sequenceNumbers;

		Handler _timeHandler = new Handler();
		Runnable _timeUpdater;
		long _init, _now, _time, _sequenceTime, _baseSequenceTime;

		TaskState _state;

		public SequenceDrawView(Context context) {
			super(context);

			_view = this;			
			_context = context;
			_assetMgr = _context.getAssets();

			_strokePaint = _textPaint = _coverPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			_state = TaskState.selection;
			_hasStarted = _isFinished = _isCalibration = _is16GridView = _isRandom = false;
			_is9GridView = true;

			String sequenceBase = SharedPreferencesWrapper.getPref(_context, "SequenceBase", null);

			if(sequenceBase != null) {
				_baseSequenceTime = Long.valueOf(sequenceBase);
			} else {
				_baseSequenceTime = -1;
			}

			setupPaint();
			setupTaskTimer();
		}

		@Override
		public void onTaskCompleted(String response) {
			if(response == "") {
				_sequenceTask.saveLocal(_context, "sequence");
				Toast.makeText(_context, "Couldn't send to server", Toast.LENGTH_LONG).show();
				Toast.makeText(_context, "Stored locally", Toast.LENGTH_LONG).show();
			} else if (response.toLowerCase().contains("drink")) {
				drinkRecord.clearLocal(_context, "drink");
				Toast.makeText(_context, response, Toast.LENGTH_LONG).show();
			} else if (response.toLowerCase().contains("task")) {
				_sequenceTask.clearLocal(_context, "sequence");
				Toast.makeText(_context, response, Toast.LENGTH_LONG).show();

				getFragmentManager().popBackStackImmediate();
			}
		}

		@Override
		public void setupTaskTimer() {
			_sequenceTime = 0;
			_init = -1;

			_timeUpdater = new Runnable() {

				@Override
				public void run() {
					if(_hasStarted) {
						_now = System.currentTimeMillis();
						_sequenceTime = _now - _init;
						_timeHandler.postDelayed(this, 1);
					}

				}

			};
		}

		@Override
		public void restartTask() {
			_strokePaint = _textPaint = _coverPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			_state = TaskState.timerStart;
			_hasStarted = false;

			setupPaint();
			setupTaskTimer();
			generateNewTask();
			startCountdown();
		}

		@Override
		public void setupPaint() {
			_strokePaint.setStyle(Paint.Style.STROKE);
			_strokePaint.setStrokeWidth(8);
			_strokePaint.setColor(Color.BLACK);

			_textPaint.setColor(Color.BLACK);

			_coverPaint.setStyle(Paint.Style.FILL);
			_coverPaint.setColor(Color.BLACK);
		}

		@Override
		public void drawSelectionButtons(Canvas canvas) {
			_startGridBtn = new CanvasButton(BitmapFactory.decodeResource(getResources(), R.drawable.button_task));
			_startGridBtn.destRect = new Rect(75, 50, _canvasWidth - 100, _startGridBtn.imageRect.bottom + 50);
			_calibBtn = new CanvasButton(BitmapFactory.decodeResource(getResources(), R.drawable.button_calibration));
			_calibBtn.destRect = new Rect(75, 150, _canvasWidth - 100, _calibBtn.imageRect.bottom + 150);

			canvas.drawRect(_overlay, _strokePaint);
			canvas.drawBitmap(_startGridBtn.image, _startGridBtn.imageRect, _startGridBtn.destRect, _strokePaint);
			canvas.drawBitmap(_calibBtn.image, _calibBtn.imageRect, _calibBtn.destRect, _strokePaint);
		}

		@Override
		public void drawCountdown(Canvas canvas) {
			_textPaint = PaintHelper.setTextSizeForWidth(_textPaint, _canvasWidth - 100, 12f, _timerText);
			canvas.drawText(_timerText, 50, _canvasHeight - 200, _textPaint);
		}

		@Override 
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);

			_canvasWidth = MeasureSpec.getSize(widthMeasureSpec);
			_canvasHeight = MeasureSpec.getSize(heightMeasureSpec);
			this.setMeasuredDimension(_canvasWidth, _canvasHeight);

			_overlay = new Rect(0, 0, _canvasWidth, _canvasHeight);
		}

		@Override
		public void drawTask(Canvas canvas) {
			for(int i = 0; i < _sequenceButtons.size(); i++) {
				canvas.drawBitmap(_sequenceButtons.elementAt(i).image, _sequenceButtons.elementAt(i).imageRect, _sequenceButtons.elementAt(i).destRect, _strokePaint);
			}

			if(_isFinished) {
				drawResult(canvas);
			}

			startTimeHandler();
		}

		@Override
		public void drawResult(Canvas canvas) {
			if(_isCalibration){
				_textPaint = PaintHelper.setTextSizeForWidth(_textPaint, _canvasWidth - 100, 12f, String.format("%1$s ms", String.valueOf(_sequenceTime)));
				canvas.drawText(String.format("%1$s ms", String.valueOf(_sequenceTime)), 50, _canvasHeight - 150, _textPaint);
			} else {
				_textPaint = PaintHelper.setTextSizeForWidth(_textPaint, _canvasWidth - 100, 12f, String.format("%1$s ms, difference of %2$s", String.valueOf(_sequenceTime), String.valueOf(_sequenceTime - _baseSequenceTime)));
				canvas.drawText(String.format("%1$s ms, difference of %2$s", String.valueOf(_sequenceTime), String.valueOf(_sequenceTime - _baseSequenceTime)), 50, _canvasHeight - 150, _textPaint);
			}
		}

		@Override
		public void startCountdown() {
			_startTimer = new CountDownTimer(5000, 1000) {

				@Override
				public void onFinish() {
					_state = TaskState.timerFinish;
					_timerText = "";
					generateNewTask();
				}

				@Override
				public void onTick(long arg0) {
					_timerText = String.format("Starting in %1$s...", (int)arg0 / 1000); // countdown until task start
					_textPaint = PaintHelper.setTextSizeForWidth(_textPaint, _canvasWidth - 100, 18f, _timerText); // modify text size
					invalidate(); // refresh draw
				}

			}.start();
		}

		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);

			switch(_state) {
			case selection:
				drawSelectionButtons(canvas);
				break;
			case timerStart:
				drawCountdown(canvas);
				break;
			case timerFinish:
				drawTask(canvas);
				break;
			}
		}

		@Override
		public boolean onTouchEvent(MotionEvent e) {
			float x = e.getX();
			float y = e.getY();

			switch(e.getAction()) {
			case MotionEvent.ACTION_DOWN:
				switch(_state) {
				case selection:
					if(_startGridBtn.contains(x, y)){
						if(_baseSequenceTime != -1) {
							_state = TaskState.timerStart;
							invalidate(); // refresh draw
							startCountdown();
						} else {
							Toast.makeText(_context, _context.getResources().getString(R.string.task_calibration_error), Toast.LENGTH_LONG).show();
						}
					} else if(_calibBtn.contains(x, y)) {						
						_state = TaskState.timerStart;
						_isCalibration = true;
						invalidate();

						startCountdown();
					}
					break;
				case timerStart:
					// No command during this state
					break;
				case timerFinish:


					for(int i = 0; i < _sequenceButtons.size(); i++) {
						SequenceGridButton temp = _sequenceButtons.elementAt(i);

						if(temp.contains(x, y)) { // if the button at the current index contains the touch position

							if(_sequenceNumbers.firstElement() == temp.value) { // if the first value in the sequence is the buttons value
								_sequenceNumbers.removeElement(temp.value); // remove the first value (next in sequence)

								try {
									_sequenceButtons.elementAt(i).image = BitmapFactory.decodeStream(_assetMgr.open(String.format("SequenceNumbers/seq_selected_%1$s.png", temp.value + 1)));
									invalidate();
								} catch (IOException ex) {
									Log.e("log_tag", "Error in changing button asset " + ex.toString());
								} catch (Exception ex) {
									Log.e("log_tag", "Error accessing sequence button " + ex.toString());
								}

								if(_sequenceNumbers.size() == 0){
									_hasStarted = false;

									if(_is9GridView) {
										_is9GridView = false;
										_is16GridView = true;

										restartTask();
									} else {
										_hasStarted = false;
										_isFinished = true;
										
										invalidate();

										// finished
										if(_isCalibration) {
											Toast.makeText(_context, _context.getResources().getString(R.string.saving_task_baseline), Toast.LENGTH_LONG).show();
											SharedPreferencesWrapper.savePref(_context, "SequenceBase", String.valueOf(_sequenceTime));
										} else {
											showDiaryInput();
										}

									}


								}

								break;
							}
						}
					}

					break;
				}
				break;
			}

			return true;
		}

		@Override
		public boolean sendResultsToServer(Task task) {
			task.TaskType = "sequence";
			task.Result = String.valueOf(_sequenceTime);

			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
			String dateString = formatter.format(new java.util.Date());

			task.TimeStamp = dateString;
			task.Units = drinkRecord.Quantity;
			
			JSONObject sendObject = new JSONObject();
			JSONArray taskArray = task.loadLocal(_context, "sequence");			
			taskArray.put(task.toJsonObject());

			String username = SharedPreferencesWrapper.getPref(_context, "Username", "");
			String password = SharedPreferencesWrapper.getPref(_context, "Password", "");

			try {
				sendObject.put("username", username);
				sendObject.put("password", password);
				sendObject.put("tasks", taskArray);
			} catch (Exception ex) {
				Log.e("log_tag", "Error in JSONObject generation " + ex.toString());
			}

			// Send or store									
			SendHTTPDataAsync loginData = new SendHTTPDataAsync(getActivity(), _view);
			loginData.execute(getString(R.string.web_service_url) + getString(R.string.task), sendObject.toString());

			return true;
		}

		@Override
		public void saveToLocalStorage() {
			// TODO Auto-generated method stub

		}

		@Override
		public void showDiaryInput() {
			AlertDialog.Builder alert = new AlertDialog.Builder(_context);
			alert.setTitle(R.string.drink_prompt_title);
			alert.setMessage(R.string.drink_prompt_message);

			final View drinkDialog = View.inflate(_context, R.layout.dialog_drink, null);

			final MainActivity activity = (MainActivity) _context;

			alert.setView(drinkDialog);

			alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					EditText drinkNameText = (EditText)drinkDialog.findViewById(R.id.drink_name);
					String drinkName = drinkNameText.getText().toString();

					EditText drinkQuantityText = (EditText)drinkDialog.findViewById(R.id.drink_quantity);
					String drinkQuantity = drinkQuantityText.getText().toString();

					drinkRecord = new Drink();
					drinkRecord.Name = drinkName;
					drinkRecord.Quantity = drinkQuantity;

					SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
					String dateString = formatter.format(new java.util.Date());

					drinkRecord.TimeStamp = dateString;

					JSONObject sendObject = new JSONObject();
					JSONArray drinkArray = drinkRecord.loadLocal(activity, "drink");			
					drinkArray.put(drinkRecord.toJsonObject());

					String username = SharedPreferencesWrapper.getPref(activity, "Username", "");
					String password = SharedPreferencesWrapper.getPref(activity, "Password", "");

					try {
						sendObject.put("username", username);
						sendObject.put("password", password);
						sendObject.put("entries", drinkArray);
					} catch (Exception ex) {
						Log.e("log_tag", "Error in JSONObject generation " + ex.toString());
					}

					// Send or store									
					SendHTTPDataAsync loginData = new SendHTTPDataAsync(activity, activity);
					loginData.execute(getString(R.string.web_service_url) + getString(R.string.drink), sendObject.toString());
				
					_sequenceTask = new Task();
					sendResultsToServer(_sequenceTask);
				}
			});

			alert.show();
		}

		@Override
		public void generateNewTask() {
			_sequenceButtons = new Vector<SequenceGridButton>();
			_sequenceNumbers = new Vector<Integer>();

			if(_is9GridView) {
				int buttonSize;
				_tops = null;

				if(_canvasWidth > _canvasHeight){
					buttonSize = _canvasHeight / 3;
				} else {
					buttonSize = _canvasWidth / 3;
				}

				for(int i = 0; i < 9; i++) {
					SequenceGridButton temp;

					try {
						temp = new SequenceGridButton(BitmapFactory.decodeStream(_assetMgr.open(String.format("SequenceNumbers/seq_%1$s.png", i + 1))));
						temp.value = i;
						int[] coords = getCoords(3);
						temp.destRect = new Rect(coords[0], coords[1] - buttonSize, coords[0] + buttonSize, coords[1]);

						_sequenceButtons.add(temp);

					} catch (IOException e) {
						Log.e("log_tag", "Error in loading sequence image " + e.toString());
					} catch (Exception e) { 
						Log.e("log_tag", "Error in creating sequence button " + e.toString());
					}

					_sequenceNumbers.add(i);
				}
			} else if (_is16GridView) {
				int buttonSize;
				_tops = null;

				if(_canvasWidth > _canvasHeight){
					buttonSize = _canvasHeight / 4;
				} else {
					buttonSize = _canvasWidth / 4;
				}

				for(int i = 0; i < 16; i++) {
					SequenceGridButton temp;

					try {
						temp = new SequenceGridButton(BitmapFactory.decodeStream(_assetMgr.open(String.format("SequenceNumbers/seq_%1$s.png", i + 1))));
						temp.value = i;
						int[] coords = getCoords(4);
						temp.destRect = new Rect(coords[0], coords[1] - buttonSize, coords[0] + buttonSize, coords[1]);

						_sequenceButtons.add(temp);

					} catch (IOException e) {
						Log.e("log_tag", "Error in loading sequence image " + e.toString());
					} catch (Exception e) { 
						Log.e("log_tag", "Error in creating sequence button " + e.toString());
					}

					_sequenceNumbers.add(i);
				}
			}

			invalidate();
		}

		Vector<int[]> _tops;

		public int[] getCoords(int columns) {
			if(_tops == null || _tops.size() == 0) {
				_tops = new Vector<int[]>();

				int gridbuttonSize;

				if(_canvasWidth > _canvasHeight){
					gridbuttonSize = _canvasHeight / columns;
				} else {
					gridbuttonSize = _canvasWidth / columns;
				}

				for(int i = 0; i < columns; i++) {
					for(int j = 0; j < columns; j++) {
						int top = gridbuttonSize * j;
						int bottom = (gridbuttonSize * i) + gridbuttonSize;
						_tops.add(new int[] {top, bottom});
					}
				}
			}

			Random r = new Random();

			int value = r.nextInt(_tops.size());
			int[] returnObj = _tops.elementAt(value);
			_tops.remove(value);

			return returnObj;
		}

		public void startTimeHandler() {			
			if(!_hasStarted) {
				if(_init == -1) {
					_init = System.currentTimeMillis();
				}

				_hasStarted = true;

				_timeHandler.post(_timeUpdater);
			}
		}
	}
}
