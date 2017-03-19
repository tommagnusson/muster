package edu.marist.muster.sheets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

/**
 * Quick access to single values in a Google Sheet.
 * All methods are synchronous and go over the network.
 * @author Tom Magnusson
 *
 * @param <T>
 * 		The expected value of the cells (usually String is applicable).
 */
public final class SheetsCursor<T> {

	/**
	 * Access to Google Sheets API
	 */
	private Sheets service;
	
	/**
	 * The spreadsheet's id, found in the url of the sheet.
	 */
	private String spreadsheetId;
	
	public SheetsCursor(Sheets service, String spreadsheetId) {
		this.service = service;
		this.spreadsheetId = spreadsheetId;
	}
	
	/**
	 * Retrieves a value from a given cell.
	 * @param cell
	 * @return Optional value of type T
	 * 		if the call failed in some way, the value will be empty
	 */
	@SuppressWarnings("unchecked")
	public Optional<T> cellValue(String cell) {
		List<List<Object>> values;
		try {
			values = service.spreadsheets().values().get(spreadsheetId, cell).execute().getValues();
			if(values != null && values.get(0) != null && values.get(0).get(0) != null) {
				return Optional.of((T) values.get(0).get(0));
			}
		} catch (IOException e) {
			e.printStackTrace();
			return Optional.empty();
		}
		return Optional.empty();
	}
	
	/**
	 * Sets a cell with the given values
	 * @param cell
	 * @param value
	 * @return {@code true} if successful, {@code false} if failed
	 */
	public boolean setCellValue(String cell, T value) {
		try {
			service.spreadsheets().values().update(spreadsheetId, cell, packValue(value))
					.setValueInputOption("USER_ENTERED").execute();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * Packs a single value into a ValueRange.
	 * @param value
	 * @return ValueRange containing the single value.
	 */
	public ValueRange packValue(T value) {
		List<List<Object>> outer = new ArrayList<List<Object>>(1);
		List<Object> inner = new ArrayList<Object>(1);
		
		inner.add(value);
		outer.add(inner);
		return new ValueRange().setValues(outer);
	}
	
}
