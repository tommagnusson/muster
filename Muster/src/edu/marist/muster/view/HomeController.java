package edu.marist.muster.view;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import edu.marist.muster.Preferences;
import edu.marist.muster.SheetsHelper;
import edu.marist.muster.SheetsHelperService;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

/**
 * To "hook this up" to the fxml representation (BaseView.fxml) you need to look
 * at the bottom left of the FXSceneBuilder and type in this controller's class
 * name.
 * 
 * @author Tom Magnusson
 *
 */
public class HomeController {

	/**
	 * Changes the view over to the settings.
	 */
	@FXML
	private Button settingsButton;

	/**
	 * Place where students enter "First.Lastname#"
	 */
	@FXML
	private TextField emailTextField;

	/**
	 * Confirms the student with the email in the emailTextField is here.
	 * 
	 * Connects with Google Sheets API to record that the student is here.
	 */
	@FXML
	private Button imHereButton;

	/**
	 * Confirmation text that the student who pressed I'm Here! actually is
	 * here.
	 */
	@FXML
	private Label imHereConfirmationLabel;

	/**
	 * 0% when not in use.
	 * 33% when started the task.
	 * 100% when completed a sign in.
	 */
	@FXML
	private ProgressBar progressBar;
	
	/**
	 * Enables network requests to take place
	 * off the main thread.
	 */
	private SheetsHelperService service;

	/**
	 * Called after the HomeController is all set up from FXML. Useful for
	 * initial configuration and whatnot.
	 */
	@FXML
	private void initialize() {
		try {
			// helps us with the sheets logic (encapsulation or something)
			// TODO: still blocking UI thread
			SheetsHelper sheetsHelper = new SheetsHelper();
			
			// the service allows us to run all the network requests off the main thread
			service = new SheetsHelperService();
			service.setSheetsHelper(sheetsHelper);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
			// TODO: include warning that sheets is not cooperating
		}
		
		// the service operates in callbacks, async style
		service.setOnSucceeded((workerStateEvent) -> {
			// the result of the Task.call() method
			String email = (String) workerStateEvent.getSource().getValue();
			imHereSuccess(email);
			progressBar.setProgress(1.0);
			
			// fade like the success text
			FadeTransition fade = new FadeTransition(Preferences.opacityFadeDuration(), progressBar);
			fade.setToValue(0.0);
			fade.play();
		});
		
		service.setOnFailed((workerStateEvent) -> {
			String email = (String) workerStateEvent.getSource().getValue();
			imHereFailure("Something's wrong, " + email + " could not be signed in, might be Google Sheets acting up.");
			progressBar.setProgress(0.0);
			progressBar.setOpacity(0.0);
		});
		
		
		// hide the confirmation label and progress bar
		imHereConfirmationLabel.setOpacity(0.0);
		progressBar.setOpacity(0.0);

		// wiring up events using lambda notation
		imHereButton.setOnMouseClicked(this::imHere);
		settingsButton.setOnMouseClicked(this::onSetttingsClicked);
		emailTextField.setOnKeyPressed(this::onEnterEmailTextField);
	}

	/**
	 * Simple boolean replacement for validation, leaving room for more
	 * validation options later.
	 * 
	 * @see {@code HomeController.validateEmail()}
	 * 
	 * @author Tom Magnusson
	 *
	 */
	private enum Validation {
		VALID, BAD_EMAIL_FORMATTING;
	}

	/**
	 * Validates a Marist email:<br>
	 * <br>
	 * 
	 * {@code Firstname.Lastname#}
	 * 
	 * @param email
	 * @return validation
	 */
	private Validation validateEmail(String email) {
		// should create something with "stringanylength.stringanylength1" where
		// 1 can be any number
		Pattern regex = Pattern.compile("^[a-zA-Z]+\\.[a-zA-Z]+[0-9]+$");
		Matcher matcher = regex.matcher(email);

		return matcher.find() ? Validation.VALID : Validation.BAD_EMAIL_FORMATTING;
	}

	/**
	 * The method triggered when student presses "I'm Here!" or presses enter.
	 * 
	 * @param m
	 */
	private void imHere(MouseEvent m) {
		String email = emailTextField.getText();
		switch (validateEmail(email)) {
		case VALID:
			// starts the mark http requests off the UI Thread
			service.setEmail(email);
			service.restart();
			progressBar.setOpacity(1.0);
			progressBar.setProgress(0.33);
			break;
		case BAD_EMAIL_FORMATTING:
			imHereFailure("Something's wrong, looks like \"" + email + "\" isn't formatted correctly.");
			break;
		default:
			imHereFailure("Something's wrong. Perhaps try again?");
			break;
		}
	}

	private String uppercaseFirstLetter(String s) {
		if (s.length() == 0)
			return s;
		if (s.length() == 1) {
			return s.toUpperCase();
		}
		return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
	}

	/**
	 * Called when the user is successfully "here." Sets the confirmation text
	 * green, then fades that text's opacity over a few seconds so that the next
	 * person can enter themselves.
	 * 
	 * @param email
	 */
	private void imHereSuccess(String email) {
		imHereConfirmationLabel.setTextFill(Preferences.successColor());

		// Stream.of() map using uppercaseFirst String.join with " "
		// "thomas.magnusson1" 
		// -> ["thomas" , "magnusson"] 
		// -> ["Thomas", "Magnusson1"] 
		// -> "Thomas Magnusson1"
		String whosHere = String.join(" ",
				Stream.of(email.split("\\.")).map(this::uppercaseFirstLetter).toArray(String[]::new));
		whosHere = whosHere.replaceAll("[0-9]+", ""); // remove numbers at the end, "Thomas Magnusson"

		System.out.println(whosHere);
		imHereConfirmationLabel.setText(whosHere + " is here!");

		// fade the opacity over three seconds, noice
		imHereConfirmationLabel.setOpacity(1.0);
		FadeTransition fade = new FadeTransition(Preferences.opacityFadeDuration(), imHereConfirmationLabel);
		fade.setToValue(0.0);
		fade.play();

		// clear the email field
		emailTextField.setText("");
	}

	/**
	 * Called when the user's attempt failed. Sets the confirmation text red.
	 * Persists that text until the person fixes the error or someone else tries
	 * to sign in.
	 * 
	 * @param
	 */
	private void imHereFailure(String message) {
		imHereConfirmationLabel.setOpacity(1.0);
		imHereConfirmationLabel.setTextFill(Preferences.failureColor());
		imHereConfirmationLabel.setText(message);
	}
	
	private void onSetttingsClicked(MouseEvent m) {
		System.out.println("Transitioning to settings view.");
	}

	/**
	 * When the user presses enter, it should be the equivalent of clicking "I'm
	 * Here" button.
	 * 
	 * @param k
	 */
	private void onEnterEmailTextField(KeyEvent k) {
		if (k.getCode() == KeyCode.ENTER) {
			imHere(null);
		}
	}

}
