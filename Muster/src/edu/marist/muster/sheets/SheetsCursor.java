package edu.marist.muster.sheets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
public class SheetsCursor<T> {

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
	 * @return the value from cell, {@code null} if failed in some way.
	 */
	@SuppressWarnings("unchecked")
	public T cellValue(String cell) {
		List<List<Object>> values;
		try {
			values = service.spreadsheets().values().get(cell, cell).execute().getValues();
			if(values != null && values.get(0) != null && values.get(0).get(0) != null) {
				return (T) values.get(0).get(0);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return null;
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
