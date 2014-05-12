package croft.james.amulet.helpers;

public class UnitConverter {
	
	public static float ToUnit(float measurement, float percentage) {
		return (percentage * measurement) / 1000;
	}
	
	public static float ToMeasurement(float unit, float percentage) {
		return (unit * 1000) / percentage;
	}
}