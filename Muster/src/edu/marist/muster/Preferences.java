package edu.marist.muster;

import javafx.scene.paint.Paint;
import javafx.util.Duration;

/**
 * Static class containing simple preferences, such as:
 * 	
 * <li> Google sheet id
 * <li> Success and failure colors for text
 * 
 * @author Tom Magnusson
 *
 */
public class Preferences {

	/// Not meant to be instantiated
	private Preferences() {}
	
	// Prints the names and majors of students in a sample spreadsheet:
	// https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit
	public static String getSheetID() {
		return "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms";
	}
	
	// Test sheet entitled "RHC Test"
	public static String getTestSheetID() {
		return "1wLqJrMyMIcwigWzaWiVj64xTcZBKfZ6-VOs1qpmqHZA";
	}
	
	/**
	 * @return a duration for the opacity fade for confirmation text.
	 */
	public static Duration opacityFadeDuration() {
		return Duration.seconds(4.0);
	}
	
	/**
	 * @return a nice green color
	 */
	public static Paint successColor() {
		return Paint.valueOf("#08790a7a");
	}
	
	/**
	 * @return a nasty red color
	 */
	public static Paint failureColor() {
		return Paint.valueOf("#981b1a");
	}
}
