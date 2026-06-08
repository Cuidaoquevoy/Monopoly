
package controllers;

import java.io.IOException;

import dao.ManagerConnection;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class MainController {

	@FXML
	private Button btnContinueGame;

	@FXML
	private Button btnExit;

	@FXML
	private Button btnNewGame;

	@FXML
	private Button btnProfiles;

	@FXML
	private ImageView imgLogo;

	@FXML
	private AnchorPane mainLayout;
	private boolean isNewGame = false;

	public void setNewGame(boolean isNewGame) {
		this.isNewGame = isNewGame;
	}

	/**
	 * 
	 * @author Ana
	 */
	@FXML
	public void initialize() {
		// Cargamos primero de todo la base de datos
		ManagerConnection.connectar();

		Image img = new Image(getClass().getResource("/images/logos/home_logo.png").toExternalForm());
		imgLogo.setImage(img);
		imgLogo.setFitWidth(600);
		imgLogo.setFitHeight(400);
		imgLogo.setPreserveRatio(true);
		if (isNewGame) {
			System.out.println("Iniciando nueva partida...");

		} else {
			System.out.println("Cargando partida guardada...");

		}

	}

	@FXML
	public void exitMenu() {
		System.exit(0);
	}

	@FXML
	void goToProfile(ActionEvent event) {
		try {
			Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ListProfilesView.fxml"));
			Parent root = loader.load();
			ListProfilesController listController = loader.getController();

			listController.configureView();
			stage.setScene(new Scene(root));
			stage.show();
		} catch (IOException e) {

			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @author Ana
	 */
	@FXML
	public void startNewGame(ActionEvent event) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ListProfilesView.fxml"));
			Parent root = loader.load();
			ListProfilesController listController = loader.getController();

			listController.setNewGame(true);
			listController.configureView();

			// Mostrar la vista para seleccionar perfiles
			Scene listViewScene = new Scene(root);
			Stage stage = (Stage) mainLayout.getScene().getWindow();

			stage.setScene(listViewScene);
			stage.show();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	@FXML
	private void continueGame(ActionEvent event) {
	    try {
	        // ✅ Carga correctamente el FXML desde el paquete /views
	        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/SavedGamesView.fxml"));
	        Parent root = loader.load();

	        // Opcional: obtener el controlador del nuevo FXML
	       ListGamesSavedController controller = loader.getController();

	        // Mostrar la nueva escena
	        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
	        stage.setScene(new Scene(root));
	        stage.show();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}




}
