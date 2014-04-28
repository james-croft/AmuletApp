package croft.james.amulet;

import android.graphics.Canvas;

public interface ITaskActivity {
	void setupTaskTimer();
	void restartTask(); 
	void setupPaint();
	void drawSelectionButtons(Canvas canvas);
	void drawCountdown(Canvas canvas);
	void drawTask(Canvas canvas);
	void drawResult(Canvas canvas);
	void startCountdown();
	boolean sendResultsToServer(Task task);
	void saveToLocalStorage();
	void showDiaryInput();
	void generateNewTask();
}
