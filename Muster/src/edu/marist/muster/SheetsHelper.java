package edu.marist.muster;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

/**
 * Static wrapper for all the sheets HTTP calls, hiding the request logic (boy
 * is there a lot of that).
 * 
 * TODO: cache network request response answers like todayColumnExists TODO:
 * have a stack with the current email request in it TODO: check which network
 * calls are being called more than once within mark TODO: organize and clean up
 * methods
 * 
 * @author Tom Magnusson
 *
 */
public class SheetsHelper {

	// 3/17/17
	final public static String DATE_FORMAT = "M/d/uu";

	// 9:30:00 PM
	final public static String TIME_FORMAT = "hh:mm:ss a";

	/**
	 * Use {@code toString()} to get the string representation.
	 * 
	 * Represents the two types of dimensions for Google Sheets.
	 * 
	 * @author Tom Magnusson
	 *
	 */
	private enum Dimension {
		ROWS, COLUMNS;
	}

	private enum ValueInputOption {
		RAW, USER_ENTERED;
	}

	private Sheets service;
	private String spreadsheetId;

	private String sheetTitle;

	private boolean sheetIsCreated = false;
	private List<String> emailsAlreadyHere = new ArrayList<>();

	private void createSheet() throws IOException {
		updateValueInCell("Email", "A1");
		sheetIsCreated = true;
	}

	public SheetsHelper() throws Exception {
		// Build a new authorized API client service.
		// grab the http services helper from the API Boilerplate setup
		// class
		service = GoogleAPIHelper.getSheetsService();

		// grab the ID of the sheet, within the url
		spreadsheetId = Preferences.getTestSheetID();
	}

	public boolean mark(String email) throws IOException {

		if (emailsAlreadyHere.contains(email)) {
			return false;
		}

		System.out.println("Sheet created: " + sheetIsCreated);
		if (!sheetIsCreated)
			createSheet();

		boolean emailExists = emailRowExists(email);
		System.out.println("Email row exists: " + emailExists);
		if (!emailExists)
			insertEmailRow(email);

		boolean todayExists = todayColumnExists();
		System.out.println("Today column exists: " + todayExists);
		if (!todayExists)
			appendTodayColumn();

		insertTimeMarkForEmail(email);
		return true;
	}

	private void insertTimeMarkForEmail(String email) throws IOException {
		int emailRowNumber = findRowForEmail(email);
		String todayColLetter = findColForDate(LocalDate.now());
		String timeString = LocalTime.now().format(DateTimeFormatter.ofPattern(TIME_FORMAT));
		updateValueInCell(timeString, todayColLetter + emailRowNumber);
		emailsAlreadyHere.add(email);
	}

	private String findColForDate(LocalDate date) throws IOException {
		List<String> dates = getAllDates();
		System.out.println("All dates: " + dates);
		int dateColIndex = getAllDates().indexOf(date.format(DateTimeFormatter.ofPattern(DATE_FORMAT)));
		System.out.println("Date column index: " + dateColIndex);
		return colLetter(dateColIndex);
	}

	private int findRowForEmail(String email) throws IOException {
		int index = getAllEmails().indexOf(email);

		// +2 because it's 1 based indexing,
		// and "Email" is always in A1.
		return index + 2;
	}

	/**
	 * TODO: bug where the formatting on the actual sheet is M-d-uu, not M/d/uu
	 * 
	 * @throws IOException
	 */
	private void appendTodayColumn() throws IOException {
		String todayString = LocalDate.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT));
		int indexOfLastCol = service.spreadsheets().values().get(spreadsheetId, "1:1").execute().getValues().get(0)
				.size() - 1;
		String letterOfCol = colLetter(indexOfLastCol);
		System.out.println("Today string: " + todayString);
		service.spreadsheets().values()
				.update(spreadsheetId, letterOfCol + "1:" + letterOfCol + "1", packValueIntoValueRange(todayString))
				.setValueInputOption(ValueInputOption.USER_ENTERED.toString()).execute();
	}

	/**
	 * Takes the index in a list of columns starting with the A'th column (0
	 * would be A). "A" -> "Z" 65 -> 90
	 * 
	 * @param index
	 * @return the column letter corresponding to the index
	 */
	private String colLetter(int i) {
		int unicode = i + 1 + 65;
		String c = Character.toString((char) unicode);
		System.out.println(i + " -> " + c);
		return c;
	}

	private boolean todayColumnExists() throws IOException {
		String todayString = getLastHeader();
		return LocalDate.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT)).equals(todayString);
	}

	private String getLastHeader() throws IOException {
		// should only be one index because we're querying header row only
		List<Object> values = service.spreadsheets().values().get(spreadsheetId, "1:1").execute().getValues().get(0);
		int size = values.size();

		// should return "M/d/uu"
		return (String) values.get(size - 1);
	}

	private List<List<Object>> getValuesFromRangeByColumn(String range) throws IOException {
		return service.spreadsheets().values().get(spreadsheetId, range).setMajorDimension(Dimension.COLUMNS.toString())
				.execute().getValues();
	}

	private List<List<Object>> getValuesFromRangeByRow(String range) throws IOException {
		return service.spreadsheets().values().get(spreadsheetId, range).setMajorDimension(Dimension.ROWS.toString())
				.execute().getValues();
	}

	private boolean emailRowExists(String email) throws IOException {
		return getAllEmails().contains(email);
	}

	private void insertEmailRow(String email) throws IOException {
		int rowIndex = getLastEmailRowSheetIndex();
		appendEmailRowAfterIndex(email, rowIndex);
	}

	private void appendEmailRowAfterIndex(String email, int rowIndex) throws IOException {
		service.spreadsheets().values().append(spreadsheetId, "A" + rowIndex + ":A", packValueIntoValueRange(email))
				.setValueInputOption(ValueInputOption.USER_ENTERED.toString()).execute();
	}

	private ValueRange packValueIntoValueRange(String value) {
		List<List<Object>> outer = new ArrayList<List<Object>>(1);
		List<Object> inner = new ArrayList<Object>(1);
		inner.add(value);
		outer.add(inner);
		return new ValueRange().setValues(outer);
	}

	private int getLastEmailRowSheetIndex() throws IOException {
		List<String> emails = getAllEmails();

		// this is not size - 1 because we're starting at A2
		// the "first" email would start at A2, so we'd need
		// to return 2.
		return emails.size() + 1;
	}

	private List<String> getAllEmails() throws IOException {
		List<List<Object>> valueValue = getValuesFromRangeByColumn("A2:A1000");
		List<Object> values = valueValue != null ? valueValue.get(0) : new ArrayList<Object>(0);
		List<String> emails = values.stream().map(o -> o.toString()).collect(Collectors.toList());
		System.out.println("Just finished getting emails: " + emails);
		return emails;
	}

	private List<String> getAllDates() throws IOException {
		// skip "A" because we know it's "Emails"
		List<Object> values = getValuesFromRangeByRow("B1:Z1").get(0);
		List<String> dates = values.stream().map(o -> o.toString()).collect(Collectors.toList());
		return dates;
	}

	private UpdateValuesResponse updateValueInCell(String value, String cell) throws IOException {
		System.out.println("Cell: " + cell);
		return service.spreadsheets().values().update(spreadsheetId, cell, packValueIntoValueRange(value))
				.setValueInputOption(ValueInputOption.USER_ENTERED.toString()).execute();
	}

	private SheetProperties getSheetProperties() throws IOException {
		return service.spreadsheets().get(spreadsheetId).execute().getSheets().get(0).getProperties();
	}

	/**
	 * Grabs the sheetTitle cached in memory, or if that's null, make the http
	 * request to get the title.
	 * 
	 * @return title of the sheet, usually "Sheet1"
	 * @throws IOException
	 */
	public String getSheetTitle() throws IOException {
		if (sheetTitle != null)
			return sheetTitle;

		return forceGetSheetTitle();
	}

	/**
	 * Forces the class to go over the network to get the sheet title. Caches
	 * the title to the {@code sheetTitle} instance variable.
	 * 
	 * Try to use {@code getSheetTitle()} instead, unless you think the sheet
	 * was updated.
	 * 
	 * @return title of the sheet, usually "Sheet1"
	 * @throws IOException
	 */
	public String forceGetSheetTitle() throws IOException {
		sheetTitle = getSheetProperties().getTitle();
		return sheetTitle;
	}
}
