package croft.james.amulet;

import android.graphics.Paint;
import android.graphics.Rect;

public class PaintHelper {
	public static Paint setTextSizeForWidth(Paint paint, float width, float fontSize, String text) {

	    // Get the bounds of the text.
	    paint.setTextSize(fontSize);
	    Rect bounds = new Rect();
	    paint.getTextBounds(text, 0, text.length(), bounds);

	    // Calculate the desired size.
	    float desiredTextSize = fontSize * width / bounds.width();

	    // Set the paint for that size.
	    paint.setTextSize(desiredTextSize);
		
		return paint;
	}
}
