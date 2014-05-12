package croft.james.amulet;

import java.text.SimpleDateFormat;
import java.util.Random;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

import croft.james.amulet.helpers.ImageResizer;
import croft.james.amulet.helpers.LocalStorer;
import croft.james.amulet.helpers.PaintHelper;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
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

public class PilotFragment extends Fragment {

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		inflater.inflate(R.layout.fragment_sequence, container, false);
		super.onCreateView(inflater, container, savedInstanceState);

		getActivity().setTitle("Pilot");

		return new PilotDrawView(getActivity());
	}

	private class PilotDrawView extends View implements
			OnRetrieveDataCompleted, ITaskActivity {
		Context _context;
		Task _pilotTask;
		CountDownTimer _startTimer;
		Drink _drinkRecord;

		String _timerText = "";
		int _canvasHeight, _canvasWidth;
		boolean _hasStarted, _isFinished, _isCalibration, _isHandlingObjects,
				_changeDirection;

		Rect _overlay;
		Paint _strokePaint, _textPaint, _coverPaint;
		PilotDrawView _view;

		CanvasButton _startGridBtn, _calibBtn;
		Vector<MoveableCanvasButton> _pilotAvoidanceObjects;
		int _pilotAvoidanceObjectNumber;
		CanvasButton _pilotUserObject;

		Handler _timeHandler = new Handler();
		Runnable _timeUpdater;
		long _init, _now, _time, _pilotTime, _basePilotTime;

		Handler _objectHandler = new Handler();
		Runnable _objectUpdater;
		long _lastChange, _current, _changeDirectionTime;
		int _valIncrease = 0;

		TaskState _state;

		public PilotDrawView(Context context) {
			super(context);

			_view = this;
			_context = context;

			_strokePaint = _textPaint = _coverPaint = new Paint(
					Paint.ANTI_ALIAS_FLAG);
			_state = TaskState.selection;
			_hasStarted = _isFinished = _isCalibration = _isHandlingObjects = _changeDirection = false;

			String pilotBase = SharedPreferencesWrapper.getPref(_context,
					"PilotBase", null);

			if (pilotBase != null) {
				_basePilotTime = Long.valueOf(pilotBase);
			} else {
				_basePilotTime = -1;
			}

			setupPaint();
			setupTaskTimer();

			_lastChange = -1;

			_pilotAvoidanceObjectNumber = 3;
		}

		@Override
		public void setupTaskTimer() {
			_pilotTime = 0;
			_init = -1;

			_timeUpdater = new Runnable() {

				@Override
				public void run() {
					if (_hasStarted) {
						_now = System.currentTimeMillis();
						_pilotTime = _now - _init;
						_timeHandler.postDelayed(this, 1);
					}
				}

			};
		}

		public void runObjectHandler() {
			_objectUpdater = new Runnable() {

				@Override
				public void run() {

					_current = System.currentTimeMillis();

					if (_hasStarted) {
						for (int i = 0; i < _pilotAvoidanceObjects.size(); i++) {
							if (_lastChange == -1
									|| _current > _changeDirectionTime) {

								Random r = new Random();

								int xVal = r.nextInt(50) - 15;
								int yVal = r.nextInt(50) - 15;

								_pilotAvoidanceObjects.elementAt(i).X = xVal
										+ _valIncrease;
								_pilotAvoidanceObjects.elementAt(i).Y = yVal
										+ _valIncrease;

								if (_valIncrease < 30) {
									_valIncrease = _valIncrease + 3; // increase
																		// speed
																		// each
																		// direction
																		// change
								}
								_changeDirection = true;
							}

							if (_pilotAvoidanceObjects.elementAt(i).destRect.right > _canvasWidth
									|| _pilotAvoidanceObjects.elementAt(i).destRect.left < 0) {
								_pilotAvoidanceObjects.elementAt(i).reverseX();
							} else {
								_pilotAvoidanceObjects.elementAt(i).isReversingX = false;
							}

							if (_pilotAvoidanceObjects.elementAt(i).destRect.bottom > _canvasHeight
									|| _pilotAvoidanceObjects.elementAt(i).destRect.top < 0) {
								_pilotAvoidanceObjects.elementAt(i).reverseY();
							} else {
								_pilotAvoidanceObjects.elementAt(i).isReversingY = false;
							}

							_pilotAvoidanceObjects.elementAt(i).move();

							if (_pilotUserObject
									.contains(_pilotAvoidanceObjects
											.elementAt(i))) {
								_hasStarted = false;
								_isFinished = true;
								break;

							}
						}

						if (_isFinished) {
							if (_isCalibration) {
								Toast.makeText(
										_context,
										_context.getResources().getString(
												R.string.saving_task_baseline),
										Toast.LENGTH_LONG).show();
								SharedPreferencesWrapper
										.savePref(_context, "PilotBase",
												String.valueOf(_pilotTime));
							} else {
								showDiaryInput();
							}
						} else {

							invalidate();

							if (_changeDirection) {
								_changeDirectionTime = _current + 10000;
								_lastChange = _current;
								_changeDirection = false;
							}

							_objectHandler.postDelayed(this, 10);
						}
					}
				}
			};
		}

		@Override
		public void restartTask() {
			_strokePaint = _textPaint = _coverPaint = new Paint(
					Paint.ANTI_ALIAS_FLAG);
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
			_startGridBtn = new CanvasButton(BitmapFactory.decodeResource(
					getResources(), R.drawable.button_task));
			_startGridBtn.destRect = new Rect(50, 50, _canvasWidth - 50,
					ImageResizer.calculateHeight(_canvasWidth - 100,
							_startGridBtn.image.getWidth(),
							_startGridBtn.image.getHeight()) + 50);
			_calibBtn = new CanvasButton(BitmapFactory.decodeResource(
					getResources(), R.drawable.button_calibration));
			_calibBtn.destRect = new Rect(50,
					_startGridBtn.destRect.bottom + 10, _canvasWidth - 50,
					ImageResizer.calculateHeight(_canvasWidth - 100,
							_calibBtn.image.getWidth(),
							_calibBtn.image.getHeight())
							+ (_startGridBtn.destRect.bottom + 10));

			canvas.drawRect(_overlay, _strokePaint);
			canvas.drawBitmap(_startGridBtn.image, _startGridBtn.imageRect,
					_startGridBtn.destRect, _strokePaint);
			canvas.drawBitmap(_calibBtn.image, _calibBtn.imageRect,
					_calibBtn.destRect, _strokePaint);
		}

		@Override
		public void drawCountdown(Canvas canvas) {
			_textPaint = PaintHelper.setTextSizeForWidth(_textPaint,
					_canvasWidth - 100, 12f, _timerText);
			canvas.drawText(_timerText, 50, _canvasHeight - 50, _textPaint);
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);

			_canvasWidth = MeasureSpec.getSize(widthMeasureSpec);
			_canvasHeight = MeasureSpec.getSize(heightMeasureSpec);
			this.setMeasuredDimension(_canvasWidth, _canvasHeight);

			_overlay = new Rect(0, 0, _canvasWidth, _canvasHeight);
		}

		@Override
		public void drawTask(Canvas canvas) {
			for (int i = 0; i < _pilotAvoidanceObjects.size(); i++) {
				canvas.drawBitmap(_pilotAvoidanceObjects.elementAt(i).image,
						_pilotAvoidanceObjects.elementAt(i).imageRect,
						_pilotAvoidanceObjects.elementAt(i).destRect,
						_strokePaint);
			}

			canvas.drawBitmap(_pilotUserObject.image,
					_pilotUserObject.imageRect, _pilotUserObject.destRect,
					_strokePaint);

			if (_isFinished) {
				drawResult(canvas);
			}

			startTimeHandler();
			startObjectHandler();
		}

		@Override
		public void drawResult(Canvas canvas) {
			if (_isCalibration) {
				_textPaint = PaintHelper.setTextSizeForWidth(_textPaint,
						_canvasWidth - 100, 12f,
						String.format("%1$s ms", String.valueOf(_pilotTime)));
				canvas.drawText(
						String.format("%1$s ms", String.valueOf(_pilotTime)),
						50, _canvasHeight - 150, _textPaint);
			} else {
				_textPaint = PaintHelper.setTextSizeForWidth(
						_textPaint,
						_canvasWidth - 100,
						12f,
						String.format("%1$s ms, difference of %2$s",
								String.valueOf(_pilotTime),
								String.valueOf(_pilotTime - _basePilotTime)));
				canvas.drawText(
						String.format("%1$s ms, difference of %2$s",
								String.valueOf(_pilotTime),
								String.valueOf(_pilotTime - _basePilotTime)),
						50, _canvasHeight - 150, _textPaint);
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
					runObjectHandler();
				}

				@Override
				public void onTick(long arg0) {
					_timerText = String.format("Starting in %1$s...",
							(int) arg0 / 1000); // countdown until task start
					_textPaint = PaintHelper.setTextSizeForWidth(_textPaint,
							_canvasWidth - 100, 18f, _timerText); // modify text
																	// size
					invalidate(); // refresh draw
				}

			}.start();
		}

		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);

			switch (_state) {
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
			switch (e.getAction()) {
			case MotionEvent.ACTION_DOWN:
				int x = (int) e.getX();
				int y = (int) e.getY();

				switch (_state) {
				case selection:
					if (_startGridBtn.contains(x, y)) {
						if (_basePilotTime != -1) {
							_state = TaskState.timerStart;
							getActivity().setRequestedOrientation(
									ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

							invalidate(); // refresh draw

							startCountdown();
						} else {
							Toast.makeText(
									_context,
									_context.getResources().getString(
											R.string.task_calibration_error),
									Toast.LENGTH_LONG).show();
						}
					} else if (_calibBtn.contains(x, y)) {
						_state = TaskState.timerStart;
						_isCalibration = true;
						getActivity().setRequestedOrientation(
								ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

						invalidate();

						startCountdown();
					}
					break;
				case timerStart:
					break;
				case timerFinish:
					break;
				}
				break;
			case MotionEvent.ACTION_MOVE:
				final int count = e.getPointerCount();
				switch (_state) {
				case selection:
					break;
				case timerStart:
					break;
				case timerFinish:

					for (int idx = 0; idx < count; idx++) {
						int xIdx = (int) e.getX(idx);
						int yIdx = (int) e.getY(idx);

						_pilotUserObject.setCenter(xIdx, yIdx);
						invalidate();
					}

					break;
				}
				break;
			}

			return true;
		}

		@Override
		public boolean sendResultsToServer(Task task) {
			task.TaskType = "pilot";
			task.Result = String.valueOf(_pilotTask);

			SimpleDateFormat formatter = new SimpleDateFormat(
					"yyyy-MM-dd HH:mm:ss.SSS");
			String dateString = formatter.format(new java.util.Date());

			task.TimeStamp = dateString;
			task.Units = _drinkRecord.Quantity;

			JSONObject sendObject = new JSONObject();
			JSONArray taskArray = LocalStorer.getLocalJSONArray(_context,
					"pilot");
			taskArray.put(task.toJsonObject());

			String username = SharedPreferencesWrapper.getPref(_context,
					"Username", "");
			String password = SharedPreferencesWrapper.getPref(_context,
					"Password", "");

			try {
				sendObject.put("username", username);
				sendObject.put("password", password);
				sendObject.put("tasks", taskArray);
			} catch (Exception ex) {
				Log.e("log_tag",
						"Error in JSONObject generation " + ex.toString());
			}

			// Send or store
			SendHTTPDataAsync loginData = new SendHTTPDataAsync(getActivity(),
					_view);
			loginData.execute(getString(R.string.web_service_url)
					+ getString(R.string.task), sendObject.toString());

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

			final View drinkDialog = View.inflate(_context,
					R.layout.dialog_drink, null);

			final MainActivity activity = (MainActivity) _context;

			alert.setView(drinkDialog);

			alert.setPositiveButton("Save",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							EditText drinkNameText = (EditText) drinkDialog
									.findViewById(R.id.drink_name);
							String drinkName = drinkNameText.getText()
									.toString();

							EditText drinkQuantityText = (EditText) drinkDialog
									.findViewById(R.id.drink_quantity);
							String drinkQuantity = drinkQuantityText.getText()
									.toString();

							_drinkRecord = new Drink();
							_drinkRecord.Name = drinkName;
							_drinkRecord.Quantity = drinkQuantity;

							SimpleDateFormat formatter = new SimpleDateFormat(
									"yyyy-MM-dd HH:mm:ss.SSS");
							String dateString = formatter
									.format(new java.util.Date());

							_drinkRecord.TimeStamp = dateString;

							JSONObject sendObject = new JSONObject();
							JSONArray drinkArray = LocalStorer
									.getLocalJSONArray(activity, "drink");
							drinkArray.put(_drinkRecord.toJsonObject());

							String username = SharedPreferencesWrapper.getPref(
									activity, "Username", "");
							String password = SharedPreferencesWrapper.getPref(
									activity, "Password", "");

							try {
								sendObject.put("username", username);
								sendObject.put("password", password);
								sendObject.put("entries", drinkArray);
							} catch (Exception ex) {
								Log.e("log_tag",
										"Error in JSONObject generation "
												+ ex.toString());
							}

							// Send or store
							SendHTTPDataAsync loginData = new SendHTTPDataAsync(
									activity, activity);
							loginData.execute(
									getString(R.string.web_service_url)
											+ getString(R.string.drink),
									sendObject.toString());

							_pilotTask = new Task();
							sendResultsToServer(_pilotTask);
						}
					});

			alert.show();
		}

		@Override
		public void generateNewTask() {
			_pilotAvoidanceObjects = new Vector<MoveableCanvasButton>();
			_pilotUserObject = new CanvasButton(BitmapFactory.decodeResource(
					getResources(), R.drawable.pilot_user));
			_pilotUserObject.destRect = new Rect(0, 0, 75, 75);

			Random r = new Random();

			for (int i = 0; i < _pilotAvoidanceObjectNumber; i++) {
				MoveableCanvasButton temp = new MoveableCanvasButton(
						BitmapFactory.decodeResource(getResources(),
								R.drawable.pilot_object));

				int top = 0;
				int left = 0;
				int bottom = 0;
				int right = 0;

				try {
					top = r.nextInt(_canvasHeight / 2) + 50;
					left = r.nextInt(_canvasWidth / 2) + 50;

					bottom = top + (r.nextInt(250) + 50);
					right = left + (r.nextInt(200) + 100);
				} catch (Exception e) {
					Log.e("log_tag",
							"Error in random number generation " + e.toString());
				}

				temp.destRect = new Rect(left, top, right, bottom);
				temp.imageRect = new Rect(0, 0, temp.destRect.width(),
						temp.destRect.height());

				_pilotAvoidanceObjects.add(temp);
			}

			invalidate();
		}

		@Override
		public void onTaskCompleted(String response) {
			if (response == "") {
				LocalStorer.saveLocalJSONObject(_context,
						_pilotTask.toJsonObject(), "pilot");
				Toast.makeText(_context, "Couldn't send to server",
						Toast.LENGTH_LONG).show();
				Toast.makeText(_context, "Stored locally", Toast.LENGTH_LONG)
						.show();
			} else if (response.toLowerCase().contains("drink")) {
				LocalStorer.clearLocalJSON(_context, "drink");
				Toast.makeText(_context, response, Toast.LENGTH_LONG).show();
			} else if (response.toLowerCase().contains("task")) {
				LocalStorer.clearLocalJSON(_context, "pilot");
				Toast.makeText(_context, response, Toast.LENGTH_LONG).show();

				getFragmentManager().popBackStackImmediate();
			}
		}

		public void startObjectHandler() {
			if (!_isHandlingObjects) {
				_isHandlingObjects = true;
				_objectHandler.post(_objectUpdater);
			}
		}

		public void startTimeHandler() {
			if (!_hasStarted) {
				if (_init == -1) {
					_init = System.currentTimeMillis();
				}

				_hasStarted = true;
				_timeHandler.post(_timeUpdater);
			}
		}
	}
}
