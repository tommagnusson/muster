package edu.marist.muster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

public class SheetsCursor<T> {

	private Sheets service;
	private String spreadsheetId;
	
	public SheetsCursor(Sheets service, String spreadsheetId) {
		this.service = service;
		this.spreadsheetId = spreadsheetId;
	}
	
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
	private ValueRange packValue(T value) {
		List<List<Object>> outer = new ArrayList<List<Object>>(1);
		List<Object> inner = new ArrayList<Object>(1);
		
		inner.add(value);
		outer.add(inner);
		return new ValueRange().setValues(outer);
	}
	
}
