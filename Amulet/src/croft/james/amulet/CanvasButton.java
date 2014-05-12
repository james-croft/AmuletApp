package croft.james.amulet;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;

public class CanvasButton {
	public Bitmap image;
	public Rect imageRect;
	public Rect destRect;

	public CanvasButton(Bitmap imageSrc) {
		image = imageSrc;
		imageRect = new Rect(0,0,image.getWidth(), image.getHeight());
		destRect = new Rect(0,0,0,0);
	}
	
	public CanvasButton(Bitmap imageSrc, Rect destination) {
		image = imageSrc;
		imageRect = new Rect(0,0,image.getWidth(), image.getHeight());
		destRect = destination;
	}
	
	public CanvasButton(Bitmap imageSrc, Rect destination, Rect imageSize) {
		image = imageSrc;
		imageRect = imageSize;
		destRect = destination;
	}
	
	public boolean contains(CanvasButton button) {
		return destRect.intersect(button.destRect);
	}
	
	public boolean contains(Point point) { 
		return destRect.contains(point.x, point.y);
	}
	
	public boolean contains(int x, int y) {
		return destRect.contains(x, y);
	}
	
	public boolean contains(float x, float y) {
		return contains((int)x, (int)y);
	}
	
	public void setCenter(int x, int y) {
		destRect = new Rect(x - (imageRect.width() / 2), y - (imageRect.height() / 2), x + (imageRect.width() / 2), y + (imageRect.height() / 2));
	}
	
	public Point destCenter() {
		return new Point(destRect.centerX(), destRect.centerY());
	}
	
	public Point center() {
		return new Point(imageRect.centerX(), imageRect.centerY());
	}
}
