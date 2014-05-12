package croft.james.amulet;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;

public class MoveableCanvasButton extends CanvasButton {
	int X, Y;
	boolean isReversingX, isReversingY;

	public MoveableCanvasButton(Bitmap imageSrc) {
		super(imageSrc);
	}

	public MoveableCanvasButton(Bitmap imageSrc, Rect destination) {
		super(imageSrc, destination);
	}

	public MoveableCanvasButton(Bitmap imageSrc, Rect destination,
			Rect imageSize) {
		super(imageSrc, destination, imageSize);
	}

	public void reverseX() {
		if (!isReversingX) {
			X = -X;
			isReversingX = true;
		}
	}
	
	public void reverseY() {
		if(!isReversingY) {
			Y = -Y;
			isReversingY = true;
		}
	}

	public void move() {
		Point temp = this.destCenter();
		temp.x = temp.x + X;
		temp.y = temp.y + Y;

		this.setCenter(temp.x, temp.y);
	}
}
