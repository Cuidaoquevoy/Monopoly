package controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dao.DAOManager;
import dao.ProfileDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import models.Profile;

public class ListProfilesController {

	@FXML
	private ListView<Profile> listProfiles;

	@FXML
	private Button confirmButton;

	@FXML
	private Button selectImageButton;

	@FXML
	private Button goBackButton;

	@FXML
	private TextField nicknameField;

	@FXML
	private Button startButton;

	@FXML
	private Button deleteProfileButton;

	private String selectedImagePath;

	private static DAOManager daoManager = new DAOManager();
	private static ProfileDAO profileDAO = daoManager.getProfileDAO();
	private boolean isNewGame;
	private List<Profile> selectedProfiles = new ArrayList<Profile>();

	public void setNewGame(boolean isNewGame) {
		this.isNewGame = isNewGame;
	}

	public void configureView() {
		System.out.println("New game: " + isNewGame);

		if (isNewGame) {
			listProfiles.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		} else {
			listProfiles.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		}

		List<Profile> profiles = profileDAO.getAll();
		listProfiles.getItems().clear();
		listProfiles.setCellFactory(list -> new ListCell<Profile>() {
			private final ImageView imageView = new ImageView();
			private final Label label = new Label();
			private final HBox content = new HBox(10);

			{
				imageView.setFitWidth(80);
				imageView.setFitHeight(80);
				label.setFont(Font.font("Comic Sans MS", 20));
				content.setStyle("-fx-padding: 10px;");
				content.getChildren().addAll(imageView, label);
			}

			@Override
			protected void updateItem(Profile item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setGraphic(null);
				} else {
					imageView.setImage(new Image(item.image, true));
					label.setText(item.nickname);
					setGraphic(content);
				}
			}
		});

		listProfiles.getItems().addAll(profiles);
		listProfiles.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
			if (newValue != null && isNewGame) {
				if (selectedProfiles.contains(newValue)) {
					selectedProfiles.remove(newValue);
				} else {
					selectedProfiles.add(newValue);
				}
			}
		});

		// Solo abre el perfil con doble clic — así el simple clic solo selecciona
		// y el botón "Borrar" puede actuar sobre el perfil seleccionado
		listProfiles.setOnMouseClicked(event -> {
			if (event.getClickCount() == 2 && !isNewGame) {
				openProfile();
			}
		});
	}

	public void openProfile() {
		Profile selectedProfile = listProfiles.getSelectionModel().getSelectedItem();

		if (selectedProfile != null) {
			try {
				FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ProfileView.fxml"));
				Parent profileViewRoot = loader.load();

				ProfileController profileController = loader.getController();
				profileController.setProfile(selectedProfile);

				Scene profileViewScene = new Scene(profileViewRoot);
				Stage stage = (Stage) listProfiles.getScene().getWindow();

				stage.setScene(profileViewScene);
				stage.show();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@FXML
	public void addProfile(ActionEvent event) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ProfileView.fxml"));
			Parent addProfileRoot = loader.load();

			Scene addProfileScene = new Scene(addProfileRoot);
			Stage stage = (Stage) listProfiles.getScene().getWindow();

			stage.setScene(addProfileScene);
			stage.show();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	public void deleteProfile(ActionEvent event) {
		Profile selectedProfile = listProfiles.getSelectionModel().getSelectedItem();

		if (selectedProfile == null) {
			Alert alert = new Alert(Alert.AlertType.WARNING);
			alert.setTitle("Sin selección");
			alert.setHeaderText("No hay perfil seleccionado");
			alert.setContentText("Selecciona un perfil para borrar.");
			alert.showAndWait();
			return;
		}

		Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
		confirm.setTitle("Confirmar borrado");
		confirm.setHeaderText("¿Seguro que quieres borrar el perfil \"" + selectedProfile.getNickname() + "\"?");
		confirm.setContentText("Esta acción no se puede deshacer.");

		confirm.showAndWait().ifPresent(response -> {
			if (response == ButtonType.OK) {
				profileDAO.deleteProfile(selectedProfile.getIdProfile());
				listProfiles.getItems().remove(selectedProfile);
				selectedProfiles.remove(selectedProfile);
				System.out.println("[DEBUG] Perfil borrado: " + selectedProfile.getNickname());
			}
		});
	}

	public List<Profile> getSelectedProfiles() {
		return new ArrayList<>(listProfiles.getSelectionModel().getSelectedItems());
	}

	@FXML
	private void goBack(ActionEvent event) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MainView.fxml"));
			Parent mainViewRoot = loader.load();

			Scene mainViewScene = new Scene(mainViewRoot);
			Stage stage = (Stage) goBackButton.getScene().getWindow();

			stage.setScene(mainViewScene);
			stage.show();

		} catch (IOException e) {
			e.printStackTrace();
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setTitle("Load Error");
			alert.setHeaderText("Could not load Main View");
			alert.setContentText("An error occurred while trying to load the main view.");
			alert.showAndWait();
		}
	}

	@FXML
	public void onStartButtonClicked(ActionEvent event) {
		if (selectedProfiles.size() < 2) {
			Alert alert = new Alert(Alert.AlertType.WARNING);
			alert.setTitle("Jugadores insuficientes");
			alert.setHeaderText("Se necesitan al menos 2 jugadores");
			alert.setContentText("Selecciona al menos 2 perfiles para empezar la partida.");
			alert.showAndWait();
			return;
		}

		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/GameView.fxml"));
			Parent root = loader.load();

			GameController gameController = loader.getController();
			gameController.setProfiles(selectedProfiles);

			Scene gameViewScene = new Scene(root);
			Stage stage = (Stage) listProfiles.getScene().getWindow();

			stage.setScene(gameViewScene);
			stage.show();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
