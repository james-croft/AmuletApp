package croft.james.amulet;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import croft.james.amulet.helpers.ImageResizer;
import croft.james.amulet.helpers.LocalStorer;
import croft.james.amulet.helpers.PaintHelper;
import croft.james.amulet.helpers.UnitConverter;

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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class InspectionFragment extends Fragment {

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		inflater.inflate(R.layout.fragment_inspection, container, false);
		super.onCreateView(inflater, container, savedInstanceState);

		getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		getActivity().setTitle("Inspection");

		return new InspectionDrawView(this.getActivity());
	}

	private class InspectionDrawView extends View implements OnRetrieveDataCompleted, ITaskActivity {
		Context _context;
		Task _inspectionTask;
		CountDownTimer _startTimer, _coverTimer;
		Drink _drinkRecord;

		String _timerText = "";
		int _canvasWidth, _canvasHeight;
		float _length1, _length2;
		boolean _isCalibration, _hasLengths, _isCovered, _selectionMade, _isFinished, _incorrectAnswer, _correctAnswer;

		Rect _overlay;
		Paint _strokePaint, _textPaint, _coverPaint;
		InspectionDrawView _view;

		CanvasButton _startBtn, _calibBtn, _coverBtn1, _coverBtn2;

		Handler _timeHandler = new Handler();
		Runnable _timeUpdater;
		long _init, _now, _time, _inspectionTime, _baseInspectionTime;

		TaskState _state;

		public InspectionDrawView(Context context) {
			super(context);

			_view = this;
			_context = context;

			_strokePaint = _textPaint = _coverPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			_state = TaskState.selection;
			_isCalibration = _isCovered = _selectionMade = _isFinished = _incorrectAnswer = _correctAnswer = false;
			_inspectionTime = 2000; // sets the base inspection time

			String inspectionBase = SharedPreferencesWrapper.getPref(_context, "InspectionBase", null);

			if(inspectionBase != null) {
				_baseInspectionTime = Long.valueOf(inspectionBase);
				_inspectionTime = _baseInspectionTime;
			} else {
				_baseInspectionTime = -1;
			}

			setupPaint();
			setupTaskTimer();
			
			loadPresetDrinks();
		}

		@Override
		public void setupTaskTimer() {
			_time = 0;
			_init = -1;

			_timeUpdater = new Runnable() {

				@Override
				public void run() {
					if(_isCovered) {
						_now = System.currentTimeMillis();
						_time = _now - _init;
						_timeHandler.postDelayed(this, 1);
					}
				}
			};
		}

		@Override
		public void restartTask() {
			_strokePaint = _textPaint = _coverPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			_state = TaskState.timerStart;
			_isCovered = _selectionMade = false;

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
			_startBtn = new CanvasButton(BitmapFactory.decodeResource(getResources(), R.drawable.button_task));
			_startBtn.destRect = new Rect(50, 50, _canvasWidth - 50, ImageResizer.calculateHeight(_canvasWidth - 100, _startBtn.image.getWidth(), _startBtn.image.getHeight()) + 50);
			_calibBtn = new CanvasButton(BitmapFactory.decodeResource(getResources(), R.drawable.button_calibration));
			_calibBtn.destRect = new Rect(50, _startBtn.destRect.bottom + 10, _canvasWidth - 50, ImageResizer.calculateHeight(_canvasWidth - 100, _calibBtn.image.getWidth(), _calibBtn.image.getHeight()) + (_startBtn.destRect.bottom + 10));

			canvas.drawRect(_overlay, _strokePaint);
			canvas.drawBitmap(_startBtn.image, _startBtn.imageRect, _startBtn.destRect, _strokePaint);
			canvas.drawBitmap(_calibBtn.image, _calibBtn.imageRect, _calibBtn.destRect, _strokePaint);
		}

		@Override
		public void drawCountdown(Canvas canvas) {
			_textPaint = PaintHelper.setTextSizeForWidth(_textPaint, _canvasWidth - 100, 12f, _timerText);
			canvas.drawText(_timerText, 50, _canvasHeight - 50, _textPaint);
		}

		@Override
		public void drawTask(Canvas canvas) {
			canvas.drawLine(50, 50, _canvasWidth - 50, 50, _strokePaint); // draw line across screen
			canvas.drawLine(_canvasWidth / 3, 50, _canvasWidth / 3, _length1, _strokePaint);
			canvas.drawLine((_canvasWidth / 3) * 2, 50, (_canvasWidth / 3) * 2, _length2, _strokePaint);

			_textPaint = PaintHelper.setTextSizeForWidth(_textPaint, 100, 12f, _timerText);
			canvas.drawText(_timerText, 50, _canvasHeight - 15, _textPaint);

			if(_isCovered) {
				drawCover(canvas);
			} else if (_selectionMade) {
				drawResult(canvas);
			}

			startTimeHandler();
		}

		private void startTimeHandler() {
			if(_init == -1) {
				_init = System.currentTimeMillis();
			}

			_timeHandler.post(_timeUpdater);
		}

		private void drawCover(Canvas canvas) {
			float coverLength, coverTop;

			if(_length1 > _length2) {
				coverTop = _length2 - 10; // Top of the bar
				coverLength = _length1 + 10; // Bottom of the bar
			} else {
				coverTop = _length1 - 10;
				coverLength = _length2 + 10;
			}

			_coverBtn1 = new CanvasButton(BitmapFactory.decodeResource(getResources(), R.drawable.button_task));
			_coverBtn1.destRect = new Rect((_canvasWidth / 3) - 20, (int)coverTop, (_canvasWidth / 3) + 20, (int)coverLength);

			_coverBtn2 = new CanvasButton(BitmapFactory.decodeResource(getResources(), R.drawable.button_task));
			_coverBtn2.destRect = new Rect(((_canvasWidth / 3) *2 ) - 20, (int)coverTop, ((_canvasWidth / 3) * 2) + 20, (int)coverLength);

			canvas.drawRect(_coverBtn1.destRect, _coverPaint);
			canvas.drawRect(_coverBtn2.destRect, _coverPaint);

			String task = _context.getString(R.string.inspection_task);
			_textPaint = PaintHelper.setTextSizeForWidth(_textPaint, _canvasWidth / 2, 12f, task);
			canvas.drawText(task, 50, _canvasHeight - 25, _textPaint);

			startTimeHandler();				
		}

		@Override
		public void drawResult(Canvas canvas) {
			if(_isCalibration) {
				_textPaint = PaintHelper.setTextSizeForWidth(_textPaint, _canvasWidth - 100, 12f, String.format("%1$s ms", String.valueOf(_inspectionTime)));
				canvas.drawText(String.format("%1$s ms", String.valueOf(_inspectionTime)), 50, _canvasHeight - 150, _textPaint);
			} else {
				_textPaint = PaintHelper.setTextSizeForWidth(_textPaint, _canvasWidth - 100, 12f, String.format("%1$s ms, difference of %2$s", String.valueOf(_inspectionTime), String.valueOf(_inspectionTime - _baseInspectionTime)));
				canvas.drawText(String.format("%1$s ms, difference of %2$s", String.valueOf(_inspectionTime), String.valueOf(_inspectionTime - _baseInspectionTime)), 50, _canvasHeight - 150, _textPaint);
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

					// spin off another timer for the wait until cover
					_coverTimer = new CountDownTimer(_inspectionTime, 1) {

						@Override
						public void onFinish() {
							_isCovered = true;
							_timerText = "";
							invalidate();
						}

						@Override
						public void onTick(long millisUntilFinished) {
							_timerText = "";
							invalidate();
						}

					}.start();
				}

				@Override
				public void onTick(long millisUntilFinished) {
					_timerText = String.format("Starting in %1$s...", (int)millisUntilFinished / 1000);
					_textPaint = PaintHelper.setTextSizeForWidth(_textPaint, _canvasWidth - 100, 18f, _timerText);
					invalidate();
				}
			}.start();
		}

		@Override
		public boolean sendResultsToServer(Task task) {
			task.TaskType = "inspection";
			task.Result = String.valueOf(_inspectionTime);

			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
			String dateString = formatter.format(new Date());

			task.TimeStamp = dateString;
			task.Units = _drinkRecord.Quantity;

			JSONObject sendObject = new JSONObject();
			JSONArray taskArray = LocalStorer.getLocalJSONArray(_context, "inspection");
			taskArray.put(task.toJsonObject());

			String username = SharedPreferencesWrapper.getPref(_context, "Username", "");
			String password = SharedPreferencesWrapper.getPref(_context, "Password", "");

			try {
				sendObject.put("username", username);
				sendObject.put("password", password);
				sendObject.put("tasks", taskArray);
			} catch (Exception e) {
				Log.e("log_tag", "Error in JSONObject generation " + e.toString());
			}

			SendHTTPDataAsync taskData = new SendHTTPDataAsync(getActivity(), _view);
			taskData.execute(getString(R.string.web_service_url) + getString(R.string.task), sendObject.toString());

			return true;
		}
		
		Vector<InfoDrink> _infoDrinks = new Vector<InfoDrink>();
		
		JSONArray infoDrinks;

		/**
		 * Loads in the preset drinks from assets into local storage
		 */
		private void loadPresetDrinks() {
			infoDrinks = LocalStorer.getLocalJSONArray(_context, "info_drinks");

			if (infoDrinks.length() == 0) { // loads in the drinks if they aren't
											// already stored in the local storage
				// load preset drinks
				AssetManager mgr = _context.getAssets();
				InputStream drinksFile;
				byte[] array = new byte[0];
				try {
					drinksFile = mgr.open("Files/drinks.json");
					array = new byte[drinksFile.available()];
					drinksFile.read(array);
					drinksFile.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

				JSONObject drinksObj = new JSONObject();
				JSONArray drinksArray = new JSONArray();

				try {
					drinksObj = new JSONObject(new String(array));
					drinksArray = drinksObj.getJSONArray("drinks");
				} catch (Exception e) {
					e.printStackTrace();
				}

				if (drinksArray.length() > 0) {
					LocalStorer
							.saveLocalJSONArray(_context, drinksArray, "info_drinks");
					infoDrinks = drinksArray;
				}
			}
		}

		@Override
		public void saveToLocalStorage() {
		}
		
		private void addInfoDrinks(View v) {
			Spinner spinner = (Spinner) v.findViewById(R.id.drink_list_box);
			List<String> list = new ArrayList<String>();

			for (int i = 0; i < infoDrinks.length(); i++) {
				try {
					JSONObject obj = infoDrinks.getJSONObject(i);
					InfoDrink drink = new InfoDrink(obj.getString("name"),
							obj.getString("description"), obj.getInt("quantity"),
							obj.getLong("percent"));
					_infoDrinks.add(drink);
					list.add(drink.toString());
					Log.i("log", obj.toString());
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(_context,
					android.R.layout.simple_spinner_item, list);
			dataAdapter
					.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spinner.setAdapter(dataAdapter);

		}

		@Override
		public void showDiaryInput() {
			AlertDialog.Builder alert = new AlertDialog.Builder(_context);
			alert.setTitle(R.string.drink_prompt_title);
			alert.setMessage(R.string.drink_prompt_message);

			final View drinkDialog = View.inflate(_context, R.layout.dialog_drink, null);

			final MainActivity activity = (MainActivity) _context;
			
			addInfoDrinks(drinkDialog);

			alert.setView(drinkDialog);

			alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					String drinkName = "";
					try{
						drinkName = ((EditText)drinkDialog.findViewById(R.id.drink_name)).getText().toString();
					}catch (Exception ex){
						Log.e("log_error", ex.getMessage());
					}
					
					if(drinkName.isEmpty()) {
						drinkName = ((Spinner)drinkDialog.findViewById(R.id.drink_list_box)).getSelectedItem().toString();
					}

					float drinkPercentage = -1;
					
					try{
						drinkPercentage = Float.parseFloat(((EditText)drinkDialog.findViewById(R.id.drink_percentage)).getText().toString());
					}catch (Exception ex){
						Log.e("log_error", ex.getMessage());
					}
					
					if(drinkPercentage == -1) {
						drinkPercentage = _infoDrinks.get((int) ((Spinner)drinkDialog.findViewById(R.id.drink_list_box)).getSelectedItemId()).Percent;
					}
					
					String units = "";
					
					float drinkUnits = 0;
					try{
						drinkUnits = Float.parseFloat(((EditText)drinkDialog.findViewById(R.id.drink_unit)).getText().toString());
					} catch (Exception ex) {
						Log.e("error", ex.getMessage());
					}
					
					if(drinkUnits == 0) {
						float drinkQuantity = 0;
						try{
							drinkQuantity = Float.parseFloat(((EditText)drinkDialog.findViewById(R.id.drink_quantity)).getText().toString());
						} catch (Exception ex) {
							Log.e("error", ex.getMessage());
						}
						
						units = String.valueOf(UnitConverter.ToUnit(drinkQuantity, drinkPercentage));
					}
					
					_drinkRecord = new Drink();
					_drinkRecord.Name = drinkName;
					_drinkRecord.Quantity = units;

					SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
					String dateString = formatter.format(new java.util.Date());

					_drinkRecord.TimeStamp = dateString;

					JSONObject sendObject = new JSONObject();
					JSONArray drinkArray = LocalStorer.getLocalJSONArray(activity, "drink");			
					drinkArray.put(_drinkRecord.toJsonObject());

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

					_inspectionTask = new Task();
					sendResultsToServer(_inspectionTask);
				}
			});

			alert.show();
		}

		@Override
		public void generateNewTask() {
			Random r = new Random();

			try {
				_length1 = r.nextInt(_canvasHeight - 300) + 200;
				_length2 = r.nextInt(_canvasHeight - 300) + 200;
			} catch (Exception e) {
				Log.e("log_tag", "Error in random number generation " + e.toString());
			}

			_hasLengths = true;
		}

		@Override
		protected void onMeasure(int width, int height) {
			super.onMeasure(width, height);

			_canvasWidth = MeasureSpec.getSize(width);
			_canvasHeight = MeasureSpec.getSize(height);
			this.setMeasuredDimension(_canvasWidth, _canvasHeight);

			_overlay = new Rect(0, 0, _canvasWidth, _canvasHeight);
		}

		@Override
		public void onTaskCompleted(String response) {
			if(response == "") {
				LocalStorer.saveLocalJSONObject(_context, _inspectionTask.toJsonObject(), "inspection");
				Toast.makeText(_context, "Couldn't send to server", Toast.LENGTH_LONG).show();
				Toast.makeText(_context, "Stored locally", Toast.LENGTH_LONG).show();
			} else if (response.toLowerCase().contains("drink")) {
				LocalStorer.clearLocalJSON(_context, "drink");
				Toast.makeText(_context, response, Toast.LENGTH_LONG).show();
			} else if (response.toLowerCase().contains("task")) {
				LocalStorer.clearLocalJSON(_context, "inspection");
				Toast.makeText(_context, response, Toast.LENGTH_LONG).show();

				getFragmentManager().popBackStackImmediate();
			}
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
					if(_startBtn.contains(x, y)) {
						if(_baseInspectionTime != -1) {
							_state = TaskState.timerStart;
							invalidate();
							
							startCountdown();
						} else {
							Toast.makeText(_context, _context.getResources().getString(R.string.task_calibration_error), Toast.LENGTH_LONG).show();
						}
					} else if(_calibBtn.contains(x, y )) {
						_state = TaskState.timerStart;
						_isCalibration = true;
						invalidate();
						
						startCountdown();
					}
					break;
				case timerStart:
					break;
				case timerFinish:
					if(_isCovered) {
						if(_coverBtn1.contains(x, y) || _coverBtn2.contains(x, y)) {
							_isCovered = false;
							
							float longest;
							
							if(_length1 > _length2) {
								longest = _length1;
							} else {
								longest = _length2;
							}
							
							if(_isCalibration) {
								if((_coverBtn1.contains(x, y) && longest == _length1) || (_coverBtn2.contains(x, y) && longest == _length2)) {
									_timerText = "correct";
									
									if(_inspectionTime > 100) {
										_inspectionTime = _inspectionTime - (_inspectionTime / 2);
									} else if (_inspectionTime > 10) {
										_inspectionTime = _inspectionTime - 10;
									} else {
										_isFinished = true;
									}
								} else {
									_timerText = "incorrect";
									_isFinished = true;
								}
							} else {
								if((_coverBtn1.contains(x, y) && longest == _length1) || (_coverBtn2.contains(x, y) && longest == _length2)) {
									_timerText = "correct";
									
									if(_incorrectAnswer) {
										_timerText = "finished";
										_isFinished = true;
									} else if(_inspectionTime > 100) {
										_inspectionTime = _inspectionTime - (_inspectionTime / 2);
									} else if (_inspectionTime > 10) {
										_inspectionTime = _inspectionTime - 10;
									} else {
										_isFinished = true;
									}
								} else {
									if(_correctAnswer) {
										_timerText = "finished";
										_isFinished = true;
									} else {
										_timerText = "";
										_incorrectAnswer = true;
										
										Random r = new Random();
										int increment = r.nextInt(20 - 10) + 10;
										_inspectionTime = _inspectionTime + increment;
									}
								}
							}
							
							_selectionMade = true;
							invalidate();
							
							if(!_isFinished) {
								restartTask();
							} else {
								if(_isCalibration) {
									Toast.makeText(_context, _context.getResources().getString(R.string.saving_task_baseline), Toast.LENGTH_LONG).show();
									SharedPreferencesWrapper.savePref(_context, "InspectionBase", String.valueOf(_inspectionTime));
								} else {
									showDiaryInput();
								}
							}
						}
					}
					break;
				}
				break;
			}
			
			return true;
		}
	}

}

