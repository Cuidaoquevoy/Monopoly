package controllers;

import java.io.IOException;

import dao.DAOManager;
import dao.ProfileDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.stage.Stage;
import models.Profile;

public class ProfileController {

	private static final String[] IMAGE_PATHS = { "/images/profile_photos/finn.jpg", "/images/profile_photos/jake.jpg",
			"/images/profile_photos/bmo.jpg", "/images/profile_photos/finn.jpg", "/images/profile_photos/jake.jpg" };

	@FXML
	private Button btnEditNickname;

	@FXML
	private Button btnEditProfilePhoto;

	@FXML
	private Button btnGoBack;

	@FXML
	private Button btnSaveProfile;

	@FXML
	private ImageView imgProfilePhoto;

	@FXML
	private TextField tfNickname;

	@FXML
	private TilePane profilePhotosPane;

	@FXML
	private ScrollPane scrollPane;

	private Profile selectedProfile;
	private String selectedImagePath;
	private DAOManager daoManager = new DAOManager();
	private ProfileDAO profileDAO = daoManager.getProfileDAO();

	@FXML
	private void initialize() {
		// Al crear un perfil nuevo: todo editable desde el principio
		tfNickname.setEditable(true);
		btnEditNickname.setVisible(false);
		btnEditProfilePhoto.setVisible(false);
		scrollPane.setVisible(true);
		profilePhotosPane.setVisible(true);
		setImagePane();
	}

	/**
	 * Llamado desde ListProfilesController cuando se abre un perfil existente.
	 */
	public void setProfile(Profile profile) {
		this.selectedProfile = profile;

		if (profile != null) {
			// Modo edición: nickname no editable hasta pulsar "Editar"
			tfNickname.setEditable(false);
			tfNickname.setText(profile.getNickname());

			btnEditNickname.setVisible(true);
			btnEditProfilePhoto.setVisible(true);

			// Mostrar foto actual
			if (profile.getImage() != null && !profile.getImage().isEmpty()) {
				imgProfilePhoto.setImage(new Image(profile.getImage(), true));
				selectedImagePath = profile.getImage(); // importante: inicializar con la imagen actual
			}

			// Ocultar el panel de fotos hasta que pulse "Editar foto"
			scrollPane.setVisible(false);
			profilePhotosPane.setVisible(false);
		}
	}

	@FXML
	void editName(ActionEvent event) {
		tfNickname.setEditable(true);
		tfNickname.requestFocus();
		tfNickname.selectAll();
	}

	@FXML
	void editPhoto(ActionEvent event) {
		scrollPane.setVisible(true);
		profilePhotosPane.setVisible(true);
		// Limpiar el panel y recargar las imágenes disponibles
		profilePhotosPane.getChildren().clear();
		setImagePane();
	}

	void setImagePane() {
		for (String imagePath : IMAGE_PATHS) {
			Image image = new Image(imagePath, true);
			ImageView imageView = new ImageView(image);
			imageView.setFitWidth(100);
			imageView.setFitHeight(100);
			imageView.setPreserveRatio(true);
			imageView.setCursor(Cursor.HAND);

			imageView.setOnMouseClicked(e -> {
				imgProfilePhoto.setImage(image);
				selectedImagePath = imagePath;
				// Cerrar el panel de selección tras elegir foto
				scrollPane.setVisible(false);
				profilePhotosPane.setVisible(false);
			});

			profilePhotosPane.getChildren().add(imageView);
		}
	}

	@FXML
	void saveProfile(ActionEvent event) {
		String nickname = tfNickname.getText().trim();

		if (nickname.isEmpty()) {
			Alert alert = new Alert(Alert.AlertType.WARNING);
			alert.setTitle("Nickname vacío");
			alert.setHeaderText("El nickname no puede estar vacío");
			alert.setContentText("Por favor escribe un nombre para el perfil.");
			alert.showAndWait();
			return;
		}

		if (selectedImagePath == null || selectedImagePath.isEmpty()) {
			Alert alert = new Alert(Alert.AlertType.WARNING);
			alert.setTitle("Sin imagen");
			alert.setHeaderText("No has seleccionado ninguna imagen");
			alert.setContentText("Por favor selecciona una foto de perfil.");
			alert.showAndWait();
			return;
		}

		if (selectedProfile != null) {
			// EDITAR perfil existente: actualizamos los campos del objeto y llamamos update
			selectedProfile.setNickname(nickname);
			selectedProfile.setImage(selectedImagePath);
			profileDAO.updateProfile(selectedProfile);
			System.out.println("[DEBUG] Perfil actualizado: " + selectedProfile);
		} else {
			// CREAR perfil nuevo
			Profile newProfile = new Profile(nickname, selectedImagePath);
			profileDAO.addProfile(newProfile);
			System.out.println("[DEBUG] Perfil creado: " + newProfile.getNickname());
		}

		// Volver a la lista de perfiles
		goBackToList();
	}

	@FXML
	public void goBack(ActionEvent event) {
		goBackToList();
	}

	private void goBackToList() {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ListProfilesView.fxml"));
			Parent mainViewRoot = loader.load();

			ListProfilesController listController = loader.getController();
			listController.setNewGame(false);
			listController.configureView();

			Scene mainViewScene = new Scene(mainViewRoot);
			Stage stage = (Stage) btnGoBack.getScene().getWindow();

			stage.setScene(mainViewScene);
			stage.show();

		} catch (IOException e) {
			e.printStackTrace();
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setTitle("Load Error");
			alert.setHeaderText("Could not load Profiles View");
			alert.setContentText("An error occurred while trying to load the profiles view.");
			alert.showAndWait();
		}
	}
}
