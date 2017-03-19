package edu.marist.muster.sheets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * <p>Quick access to single values in a Google Sheet.
 * 	<li>All methods are synchronous.
 * 	<li>Implements an in-memory cache to avoid repetitive
 * 		network calls.
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
	 * The spreadsheet's id, found in the url of the sheet
	 */
	private String spreadsheetId;
	
	/**
	 * Retrieves values from the cache if they exist in memory,
	 * otherwise return them from the network (no extra expensive network requests).
	 */
	private Cache<String, Optional<T>> cache = CacheBuilder.newBuilder().build();
	
	/**
	 * A function that converts an Object (from the network response)
	 * and converts it into the desired type T. Alternative to 
	 * unsafely casts. E.g.:
	 * 
	 * <pre>
	 * {@code
	 * Object value = ... // response from network
	 * T trueValue = (T) value; // unsafe casting
	 * }
	 * </pre>
	 * versus the safe way
	 * <pre>
	 * {@code
	 * Object value = ... // response from network
	 * T trueValue = transform.apply(value); // safe!
	 * }
	 * </pre>
	 */
	private Function<Object, T> transform;
	
	public SheetsCursor(Sheets service, String spreadsheetId, Function<Object, T> transform) {
		this.service = service;
		this.spreadsheetId = spreadsheetId;
		this.transform = transform;
	}
	
	/**
	 * Retrieves a value from a given cell.
	 * @param cell
	 * @return Optional T.
	 * 		If the call failed in some way: {@code Optional.empty()}
	 */
	public Optional<T> cellValue(String cell) {
		// lambda that gets the value from the network
		Callable<Optional<T>> retrieveValue = () -> {
			List<List<Object>> valuePackage;
			try {
				// network call
				valuePackage = service.spreadsheets().values()
						.get(spreadsheetId, cell)
						.execute()
						.getValues();
				
				// null checks
				if(valuePackage != null && valuePackage.get(0) != null && valuePackage.get(0).get(0) != null) {
					// unpack the value, then wrap it back up into an optional
					T transformedValue = transform.apply(valuePackage.get(0).get(0));
					return Optional.of(transformedValue);
				} else {
					// if the null checks fail, return empty
					return Optional.empty();
				}
			} catch (IOException e) {
				e.printStackTrace();
				return Optional.empty();
			}
		};
		try {
			// gets the cell's value if it's already in memory,
			// otherwise it goes to the network (because it has to).
			return cache.get(cell, retrieveValue);
		} catch (ExecutionException e) {
			e.printStackTrace();
			return Optional.empty();
		}
	}
	
	/**
	 * Sets a cell with the given values
	 * @param cell
	 * @param value
	 * @return {@code true} if successful, {@code false} if failed
	 */
	public boolean setCellValue(String cell, T value) {
		try {
			service.spreadsheets().values().update(spreadsheetId, cell + ":" + cell, packValue(value))
					.setValueInputOption("USER_ENTERED").execute();
			cache.put(cell, Optional.of(value));
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
