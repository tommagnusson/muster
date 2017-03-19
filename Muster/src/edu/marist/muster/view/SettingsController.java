package edu.marist.muster.view;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.marist.muster.Preferences;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

public class SettingsController {

	@FXML private TextField googleSheetUrlTextfield;
	
	@FXML
	private void initialize() {
		
		googleSheetUrlTextfield.requestFocus();
		
		// TODO: validate url better
		googleSheetUrlTextfield.setOnKeyPressed((keyEvent) -> {
			if(keyEvent.getCode() == KeyCode.ENTER) {
				String id = extractSheetId(googleSheetUrlTextfield.getText());
				Preferences.setSheetID(id);
			}
		});
	}

	private String extractSheetId(String url) {
		// regex for getting the id out of a url
		// via https://developers.google.com/sheets/api/guides/concepts#spreadsheet_id
		
		//Pattern regex = Pattern.compile(" .+/spreadsheets/d/([a-zA-Z0-9-_]+).+");
		//Matcher matcher = regex.matcher(url);
		String[] parts = url.split("/");
		
		// with full path it should be 6th part
		// TODO: fix regex
		return parts[5];
	}
	
}
