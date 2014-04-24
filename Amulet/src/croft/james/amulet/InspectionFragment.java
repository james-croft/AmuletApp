package croft.james.amulet;

import java.util.Random;

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
import android.widget.EditText;
import android.widget.Toast;

public class InspectionFragment extends Fragment {

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		inflater.inflate(R.layout.fragment_inspection, container, false);
		super.onCreateView(inflater, container, savedInstanceState);

		getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		return new InspectionDrawView(this.getActivity());
	}

	private class InspectionDrawView extends View implements OnRetrieveDataCompleted {
		Context _context;

		String timerText = "";
		int parentWidth, parentHeight; // width & height of fragment
		float length1, length2; // length of two bars
		boolean isCalibration, hasLengths, isCovered, selectionMade, isFinished, incorrectAnswer;
		Rect overlay;
		Paint strokePaint, textPaint, coverPaint;

		CanvasButton startBtn, calibBtn, cover1Btn, cover2Btn;

		long baseInspectionTime;

		CountDownTimer startTimer, coverTimer;
		Handler timeHandler = new Handler();
		Runnable timeUpdater;
		long init, now, time, inspectionTime;

		TaskState state;

		public InspectionDrawView(Context context) {
			super(context);

			_context = context;

			strokePaint = textPaint = coverPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			state = TaskState.selection;
			isCalibration = isCovered = selectionMade = isFinished = incorrectAnswer = false;
			inspectionTime = 2000;

			String InspectionBase = SharedPreferencesWrapper.getPref(_context, "InspectionBase", null);

			if(InspectionBase != null) {
				baseInspectionTime = Long.valueOf(InspectionBase);
				inspectionTime = baseInspectionTime;
			} else {
				baseInspectionTime = -1;
			}

			setupPaint();
			setupTimeHandler();
		}

		private void setupTimeHandler() {
			time = 0;

			timeUpdater = new Runnable() {

				@Override
				public void run() {
					if(isCovered) {
						now = System.currentTimeMillis();
						time = now - init;
						timeHandler.postDelayed(this, 1);
					}
				}
			};
		}

		private void restartTask() {
			strokePaint = textPaint = coverPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			state = TaskState.timerStart;
			isCovered = selectionMade = false;

			setupPaint();
			setupTimeHandler();

			getLineLength();
			startTimers();
		}

		private void setupPaint() {
			strokePaint.setStyle(Paint.Style.STROKE);
			strokePaint.setStrokeWidth(8);
			strokePaint.setColor(Color.BLACK);

			textPaint.setColor(Color.BLACK);

			coverPaint.setStyle(Paint.Style.FILL);
			coverPaint.setColor(Color.BLACK);
		}

		private void processSelection(Canvas canvas) {
			startBtn = new CanvasButton(BitmapFactory.decodeResource(getResources(), R.drawable.button_task));
			startBtn.destRect = new Rect(75, 50, parentWidth - 100, startBtn.imageRect.bottom + 50);
			calibBtn = new CanvasButton(BitmapFactory.decodeResource(getResources(), R.drawable.button_calibration));
			calibBtn.destRect = new Rect(75, 150, parentWidth - 100, calibBtn.imageRect.bottom + 150);

			canvas.drawRect(overlay, strokePaint);
			canvas.drawBitmap(startBtn.image, startBtn.imageRect, startBtn.destRect, strokePaint);
			canvas.drawBitmap(calibBtn.image, calibBtn.imageRect, calibBtn.destRect, strokePaint);
		}

		private void processTimerStart(Canvas canvas) {
			textPaint = PaintHelper.setTextSizeForWidth(textPaint, parentWidth - 100, 12f, timerText);
			canvas.drawText(timerText, 50, parentHeight - 200, textPaint);
		}

		private void processInspectionTask(Canvas canvas) {
			textPaint = PaintHelper.setTextSizeForWidth(textPaint, 100, 12f, timerText);

			canvas.drawLine(50, 50, parentWidth-50, 50, strokePaint); // Draw line across screen

			canvas.drawLine(parentWidth / 3, 50, parentWidth / 3, length1, strokePaint);
			canvas.drawLine((parentWidth / 3) * 2, 50, (parentWidth / 3) * 2, length2, strokePaint);

			canvas.drawText(timerText, 50, parentHeight - 75, textPaint);

			if(isCovered) {
				processCover(canvas);
			} else if(selectionMade) {
				processResult(canvas);
			}
		}

		private void processResult(Canvas canvas) {
			if(isCalibration){
				canvas.drawText(String.format("%1$s ms", String.valueOf(inspectionTime)), 50, parentHeight - 150, textPaint);
			} else {
				canvas.drawText(String.format("%1$s ms, difference of %2$s", String.valueOf(inspectionTime), String.valueOf(inspectionTime - baseInspectionTime)), 50, parentHeight - 150, textPaint);
			}
		}

		private void processCover(Canvas canvas) {
			float coverLength, coverTop;

			if(length1 > length2) {
				coverTop = length2 - 10; // top of the bar
				coverLength = length1 + 10; // bottom of the bar
			} else {
				coverTop = length1 - 10; // top of the bar
				coverLength = length2 + 10; // bottom of the bar
			}

			cover1Btn = new CanvasButton(BitmapFactory.decodeResource(getResources(), R.drawable.button_task));
			cover1Btn.destRect = new Rect((parentWidth / 3) - 20, (int)coverTop, (parentWidth / 3) + 20, (int)coverLength);

			cover2Btn = new CanvasButton(BitmapFactory.decodeResource(getResources(), R.drawable.button_task));
			cover2Btn.destRect = new Rect(((parentWidth / 3) * 2) - 20, (int)coverTop, ((parentWidth / 3) * 2) + 20, (int)coverLength);

			canvas.drawRect(cover1Btn.destRect, coverPaint);
			canvas.drawRect(cover2Btn.destRect, coverPaint);

			String task = _context.getString(R.string.inspection_task);
			textPaint = PaintHelper.setTextSizeForWidth(textPaint, parentWidth / 2, 18f, task);
			canvas.drawText(task, 50, parentHeight - 75, textPaint);

			startTimeHandler();
		}

		private void startTimeHandler() {
			init = System.currentTimeMillis();
			timeHandler.post(timeUpdater);
		}

		private void startTimers() {
			startTimer = new CountDownTimer(5000, 1000) {

				@Override
				public void onFinish() {
					state = TaskState.timerFinish;
					timerText = "";
					invalidate(); // refresh draw

					// spin off another timer for cover
					coverTimer = new CountDownTimer(inspectionTime, 1000) {

						@Override
						public void onFinish() {
							isCovered = true;
							timerText = "";
							invalidate(); // refresh draw
						}

						@Override
						public void onTick(long millisUntilFinished) {
							timerText = String.format("%1$s...", (int)millisUntilFinished / 1000); // countdown until covered
							invalidate(); // refresh every tick to show new countdown time
						}

					}.start();
				}

				@Override
				public void onTick(long arg0) {
					timerText = String.format("Starting in %1$s...", (int)arg0 / 1000); // countdown until task start
					textPaint = PaintHelper.setTextSizeForWidth(textPaint, parentWidth - 100, 18f, timerText); // modify text size
					invalidate(); // refresh draw
				}

			}.start();
		}

		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);

			switch(state){
			case selection:
				processSelection(canvas);
				break;
			case timerStart:
				processTimerStart(canvas);
				break;
			case timerFinish:
				processInspectionTask(canvas);
				break;
			}
		}

		@Override
		public boolean onTouchEvent(MotionEvent e) {
			float x = e.getX();
			float y = e.getY();

			switch(e.getAction()) {
			case MotionEvent.ACTION_DOWN:
				switch(state) {
				case selection:
					if(startBtn.contains(x, y)){
						if(baseInspectionTime != -1) {
							state = TaskState.timerStart;
							invalidate(); // refresh draw

							startTimers();
						} else {
							Toast.makeText(_context, _context.getResources().getString(R.string.task_calibration_error), Toast.LENGTH_LONG).show();
						}
					} else if(calibBtn.contains(x, y)) {						
						state = TaskState.timerStart;
						isCalibration = true;
						invalidate();

						startTimers();
					}
					break;
				case timerStart:
					// No command during this state
					break;
				case timerFinish:
					if(isCovered) {
						if(cover1Btn.contains(x, y) || cover2Btn.contains(x, y)) {
							isCovered = false; // end of task, uncover

							float longest;

							// assign the longest value
							if(length1 > length2) {
								longest = length1;
							} else {
								longest = length2;
							}

							if(isCalibration) {
								// if a button has been pressed and it is the right answer
								if((cover1Btn.contains(x, y) && longest == length1) || (cover2Btn.contains(x, y) && longest == length2)) {
									timerText = "correct";

									if(inspectionTime > 100){
										inspectionTime = inspectionTime - (inspectionTime / 2);
									} else if (inspectionTime > 10) {
										inspectionTime = inspectionTime - 10;
									} else {
										isFinished = true;
									}
								} else {
									timerText = "fail";
									isFinished = true;
								}
							} else {
								// if a button has been pressed and it is the right answer
								if((cover1Btn.contains(x, y) && longest == length1) || (cover2Btn.contains(x, y) && longest == length2)) {
									timerText = "correct";

									if(incorrectAnswer){
										timerText = "finished";
										isFinished = true;
									} else if(inspectionTime > 100){
										inspectionTime = inspectionTime - (inspectionTime / 2);
									} else if (inspectionTime > 10) {
										inspectionTime = inspectionTime - 10;
									} else {
										isFinished = true;
									}
								} else {
									timerText = "";
									incorrectAnswer = true;

									Random r = new Random();

									int increment = r.nextInt(20 - 10) + 10; 
									inspectionTime = inspectionTime + increment;
								}
							}

							selectionMade = true;
							invalidate(); // refresh draw

							if(!isFinished){
								restartTask();
							} else {
								if(isCalibration) {
									Toast.makeText(_context, _context.getResources().getString(R.string.saving_task_baseline), Toast.LENGTH_LONG).show();
									SharedPreferencesWrapper.savePref(_context, "InspectionBase", String.valueOf(inspectionTime));
								}  else {
									AlertDialog.Builder alert = new AlertDialog.Builder(_context);
									final String drinkQuantity = "";

									alert.setTitle(R.string.drink_prompt_title);
									alert.setMessage(R.string.drink_prompt_message);
									
									// Set an EditText view to get user input 
									final EditText input = new EditText(_context);
									alert.setView(input);

									alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int whichButton) {
											drinkQuantity = input.getText().toString();
											// Do something with value!
										}
									});

									alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int whichButton) {
											// Canceled.
										}
									});

									alert.show();
									// Ask for drink numbers
									// Send or store
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

		private void getLineLength() {
			Random r = new Random();

			try {
				length1 = r.nextInt(parentHeight - 300) + 200;
				length2 = r.nextInt(parentHeight - 300) + 200;
			} catch (Exception e) {
				Log.e("log_tag", "Error in random number generation " + e.toString());
			}

			hasLengths = true;
		}

		@Override 
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);

			parentWidth = MeasureSpec.getSize(widthMeasureSpec);
			parentHeight = MeasureSpec.getSize(heightMeasureSpec);
			this.setMeasuredDimension(parentWidth, parentHeight);

			if(!hasLengths) {
				getLineLength();
			}

			overlay = new Rect(0, 0, parentWidth, parentHeight);
		}

		@Override
		public void onTaskCompleted(String response) {
			// TODO Auto-generated method stub

		}
	}
}

