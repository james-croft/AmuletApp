package croft.james.amulet.helpers;

public class ImageResizer {
	public static int calculateHeight(int destinationWidth, int imageWidth, int imageHeight) {
		int ratio = imageWidth / imageHeight;
		return destinationWidth / ratio;
	}
	
	public static int calculateWidth(int destinationHeight, int imageWidth, int imageHeight) {
		int ratio = imageWidth / imageHeight;
		return ratio * destinationHeight;
	}
}
