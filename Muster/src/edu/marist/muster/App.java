package edu.marist.muster;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class App extends Application {

	/**
	 * Holds all the content.
	 */
	private Stage primaryStage;

	/**
	 * Contains the border for all of the other fx components. FXML loader
	 * instantiates it.
	 */
	private BorderPane base;

	@Override
	public void start(Stage primaryStage) {

		// entry point into the program, pass off references
		// to maintain context.
		this.primaryStage = primaryStage;

		// TODO: set icon

		// Grabs the view (fxml, specialized xml) to get an
		// in-memory representation to manipulate with controllers
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(App.class.getResource("view/BaseView.fxml"));
		try {
			// load() returns an Object, cast to the right top-level component
			this.base = (BorderPane) loader.load();
		} catch (Exception e) {
			System.err.println("Base failed to load from FXML File.\n");
			e.printStackTrace();
		}
		Scene scene = new Scene(base);
		
		
		this.primaryStage.setScene(scene);
		this.primaryStage.show();
	}

	public static void main(String[] args) {
		launch(App.class, args);
	}
}
