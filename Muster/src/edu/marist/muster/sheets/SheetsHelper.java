package edu.marist.muster.sheets;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.SheetProperties;

import edu.marist.muster.Preferences;

/**
 * Static wrapper for all the sheets HTTP calls, hiding the request logic (boy
 * is there a lot of that).<br>
 * 
 * The only interesting (or public) method is {@code mark()}, which marks
 * a given email here, configuring the sheet as necessary.
 * 
 * TODO: implement guava cache
 * TODO: refactor methods (condense, rename)
 * TODO: add SheetsVector class for dealing with rows and columns (then add cache to that)
 * 
 * @author Tom Magnusson
 *
 */
public final class SheetsHelper {

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

	/**
	 * Use {@code toString()} to get the string representation.
	 * 
	 * Represents the two types of value inputs for Google Sheets.
	 * 
	 *  <li>{@code RAW} puts the values verbatim into the cell.
	 *  <li>{@code USER_ENTERED} puts the values into a cell, then
	 *  	sheets might format it (like if it's a date or time).
	 * 
	 * @author Tom Magnusson
	 *
	 */
	private enum ValueInputOption {
		RAW, USER_ENTERED;
	}

	/**
	 * The way this class communicates over the network with sheets.
	 * All of its methods are synchronous.
	 */
	private Sheets service;
	
	/**
	 * Convenience class reading and writing
	 * single cells and values in those cells.
	 */
	private SheetsCursor<String> cursor;
	
	/**
	 * Holds the spreadsheet's id, found in the url like:
	 * 	<li><strong>{@code 1wLqJrMyMIcwigWzaWiVj64xTcZBKfZ6}</strong> from 
	 * 	<li>https://docs.google.com/spreadsheets/d/<strong>1wLqJrMyMIcwigWzaWiVj64xTcZBKfZ6-VOs1qpmqHZA</strong>/edit#gid=0
	 */
	private String spreadsheetId;

	private void createSheet() throws IOException {
		cursor.setCellValue("A1", "Email");
	}

	public SheetsHelper() {
		// Build a new authorized API client service.
		// grab the http services helper from the API Boilerplate setup
		// class
		try {
			service = GoogleAPIHelper.getSheetsService();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Google sheets is not available.");
			System.exit(-1);
		}

		// grab the ID of the sheet, within the url
		spreadsheetId = Preferences.getSheetID();
		
		// convenience class for reading and writing single values
		cursor = new SheetsCursor<>(service, spreadsheetId, (o) -> (String) o);
	}
	
	// TODO: fix this id mess
	public void setSpreadSheetId(String id) {
		this.spreadsheetId = id;
		this.cursor.setSpreadsheetId(id);
	}

	/**
	 * Marks an email here at the current time on the current date
	 * in the google sheet. This call is synchronous.
	 * @param email
	 * @return {@code true} if the email successfully updated, {@code false} otherwise.
	 * @throws IOException
	 */
	public boolean mark(String email) {
		email = email.toLowerCase(); // make sure the emails are consistent
		try {
			System.out.println(email);
			if (!emailHeaderIsPresent())
				createSheet();

			boolean emailExists = emailRowExists(email);
			System.out.println("Email row exists: " + emailExists);
			if (!emailExists)
				appendEmailRow(email);

			boolean todayExists = todayColumnExists();
			System.out.println("Today column exists: " + todayExists);
			if (!todayExists)
				appendTodayColumn();

			insertTimeMarkForEmail(email);
			return true;
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private void insertTimeMarkForEmail(String email) throws IOException {
		int emailRowNumber = findRowForEmail(email);
		String todayColLetter = findColForDate(LocalDate.now());
		String timeString = LocalTime.now().format(DateTimeFormatter.ofPattern(Preferences.TIME_FORMAT));
		cursor.setCellValue(todayColLetter + emailRowNumber, timeString);
	}

	private String findColForDate(LocalDate date) throws IOException {
		List<String> dates = getAllDates();
		System.out.println("All dates: " + dates);
		int dateColIndex = getAllDates().indexOf(date.format(DateTimeFormatter.ofPattern(Preferences.DATE_FORMAT)));
		System.out.println("Date column index: " + dateColIndex);
		return indexToLetter(dateColIndex + 1);
	}

	/**
	 * Finds the row for the given email in the google sheet.
	 * <p>This corresponds to the sheet row number (e.g. "A<strong>3</strong>"), <strong>not</strong>
	 * the index within the list of emails.
	 * @param email
	 * @return the row number of the given email in column "A".
	 * @throws IOException
	 */
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
		String todayString = LocalDate.now().format(DateTimeFormatter.ofPattern(Preferences.DATE_FORMAT));
		int indexOfLastCol = service.spreadsheets().values()
				.get(spreadsheetId, "1:1").execute()
				.getValues()
				.get(0)
				.size();
		String letterOfCol = indexToLetter(indexOfLastCol);
		System.out.println("Today string: " + todayString);
		cursor.setCellValue(letterOfCol + "1", todayString);
	}

	/**
	 * Takes the index in a list of columns starting with the A'th column (0
	 * would be A). "A" -> "Z" = 65 -> 90
	 * 
	 * @param index
	 * @return the column letter corresponding to the index
	 */
	private String indexToLetter(int i) {
		int unicode = i + 65;
		String c = Character.toString((char) unicode);
		System.out.println(i + " -> " + c);
		return c;
	}

	private boolean todayColumnExists() throws IOException {
		String todayString = getLastHeader();
		return LocalDate.now().format(DateTimeFormatter.ofPattern(Preferences.DATE_FORMAT)).equals(todayString);
	}

	private String getLastHeader() throws IOException {
		// should only be one index because we're querying header row only
		List<Object> values = service.spreadsheets().values()
				.get(spreadsheetId, "1:1")
				.execute()
				.getValues()
				.get(0);
		int size = values.size();

		// should return "M/d/uu"
		return (String) values.get(size - 1);
	}

	
	private List<List<Object>> getValuesFromRangeByDimension(String range, Dimension dimension) throws IOException {
		return service.spreadsheets().values()
				.get(spreadsheetId, range)
				.setMajorDimension(dimension.toString())
				.execute()
				.getValues();
	}

	private boolean emailRowExists(String email) throws IOException {
		return getAllEmails().contains(email);
	}

	private void appendEmailRow(String email) throws IOException {
		service.spreadsheets().values()
				.append(spreadsheetId, "A:A", cursor.packValue(email))
				.setValueInputOption(ValueInputOption.USER_ENTERED.toString())
				.execute();
	}

	private int getLastEmailRowSheetIndex() throws IOException {
		List<String> emails = getAllEmails();

		// this is not size - 1 because we're starting at A2
		// the "first" email would start at A2, so we'd need
		// to return 2.
		return emails.size() + 1;
	}

	private List<String> getAllEmails() throws IOException {
		List<List<Object>> valueValue = getValuesFromRangeByDimension("A2:A1000", Dimension.COLUMNS);
		List<Object> values = valueValue != null ? valueValue.get(0) : new ArrayList<Object>(0);
		List<String> emails = values.stream().map(o -> o.toString()).collect(Collectors.toList());
		System.out.println("Just finished getting emails: " + emails);
		return emails;
	}

	private List<String> getAllDates() throws IOException {
		// skip "A" because we know it's "Emails"
		List<Object> values = getValuesFromRangeByDimension("B1:Z1", Dimension.ROWS).get(0);
		List<String> dates = values.stream().map(o -> o.toString()).collect(Collectors.toList());
		return dates;
	}
	
	private boolean emailHeaderIsPresent() {
		Optional<String> header = cursor.cellValue("A1");
		if(header.isPresent()) {
			 return header.get().equals("Email");
		}
		return false;
	}
	
	private SheetProperties getSheetProperties() throws IOException {
		return service.spreadsheets().get(spreadsheetId).execute().getSheets().get(0).getProperties();
	}
}
