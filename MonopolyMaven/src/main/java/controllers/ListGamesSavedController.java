package controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import dao.DAOManager;
import dao.GameDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import models.Game;
import models.Game.State;

public class ListGamesSavedController {

	@FXML
	private ListView<Game> listGames;

	@FXML
	private Button loadGameButton;

	@FXML
	private Button deleteGameButton;

	@FXML
	private Button goBackButton;

	private static DAOManager daoManager = new DAOManager();
	private static GameDAO gameDAO = daoManager.getGameDAO();

	private List<Game> selectedGames = new ArrayList<>();

	@FXML
	public void initialize() {
		configureView();
	}

	public void configureView() {
		listGames.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

		// Solo mostramos partidas con estado SAVED
		List<Game> allGames = gameDAO.getAll();
		List<Game> savedGames = allGames.stream().filter(g -> g.getState() == State.SAVED).collect(Collectors.toList());

		listGames.getItems().clear();

		listGames.setCellFactory(list -> new ListCell<Game>() {
			private final Label label = new Label();
			private final HBox content = new HBox(10);

			{
				label.setFont(Font.font("Arial", 18));
				content.setStyle("-fx-padding: 10px;");
				content.getChildren().add(label);
			}

			@Override
			protected void updateItem(Game item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setGraphic(null);
				} else {
					String nombre = (item.getName() != null && !item.getName().isEmpty()) ? item.getName()
							: "Partida #" + item.getIdGame();
					String display = nombre + "  |  Duración: " + item.getDuration();
					label.setText(display);
					setGraphic(content);
				}
			}
		});

		listGames.getItems().addAll(savedGames);

		listGames.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal != null) {
				selectedGames.clear();
				selectedGames.add(newVal);
			}
		});
	}

	@FXML
	private void loadSelectedGame(ActionEvent event) {
		if (selectedGames.isEmpty()) {
			Alert alert = new Alert(Alert.AlertType.WARNING);
			alert.setTitle("Sin selección");
			alert.setHeaderText("No hay partida seleccionada");
			alert.setContentText("Selecciona una partida guardada para cargar.");
			alert.showAndWait();
			return;
		}

		Game selectedGame = selectedGames.get(0);

		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/GameView.fxml"));
			Parent root = loader.load();

			GameController gameController = loader.getController();
			gameController.setGame(selectedGame); // carga la partida guardada

			Scene scene = new Scene(root);
			Stage stage = (Stage) loadGameButton.getScene().getWindow();
			stage.setScene(scene);
			stage.show();

		} catch (IOException e) {
			e.printStackTrace();
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setTitle("Error");
			alert.setHeaderText("Error al cargar la partida");
			alert.setContentText("No se pudo cargar la vista del juego.");
			alert.showAndWait();
		}
	}

	@FXML
	private void deleteSelectedGame(ActionEvent event) {
		if (selectedGames.isEmpty()) {
			Alert alert = new Alert(Alert.AlertType.WARNING);
			alert.setTitle("Sin selección");
			alert.setHeaderText("No hay partida seleccionada");
			alert.setContentText("Selecciona una partida para borrar.");
			alert.showAndWait();
			return;
		}

		Game gameToDelete = selectedGames.get(0);

		Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
		confirm.setTitle("Confirmar borrado");
		confirm.setHeaderText("¿Seguro que quieres borrar esta partida?");
		confirm.setContentText("Partida #" + gameToDelete.getIdGame() + " — esta acción no se puede deshacer.");

		confirm.showAndWait().ifPresent(response -> {
			if (response == javafx.scene.control.ButtonType.OK) {
				gameDAO.deleteGame(gameToDelete.getIdGame());
				listGames.getItems().remove(gameToDelete);
				selectedGames.clear();
				System.out.println("[DEBUG] Partida borrada: ID = " + gameToDelete.getIdGame());
			}
		});
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
}