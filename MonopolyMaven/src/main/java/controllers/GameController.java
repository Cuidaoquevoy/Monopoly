package controllers;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import dao.CardDAO;
import dao.CellDAO;
import dao.DAOManager;
import dao.GameDAO;
import dao.PlayerCardDAO;
import dao.PlayerDAO;
import dao.PlayerPropertyDAO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import models.Action;
import models.Card;
import models.Card.CardType;
import models.Cell;
import models.Game;
import models.Game.State;
import models.Player;
import models.PlayerCard;
import models.PlayerProperty;
import models.Profile;
import models.Property;
import models.RentHouseValue;
import models.TiradaResultado;

public class GameController {

	public static final String[] TOKENS_IMAGES = { "/images/tokens/gema.png", "/images/tokens/mascara.png",
			"/images/tokens/pankake.png", "/images/tokens/probeta.png", "/images/tokens/rinyonera.png",
			"/images/tokens/tabla.png" };
	public static final String DICE_IMAGE = "images/dice/dice0{0}.png";
	public static final String PROPERTY_IMAGE = "images/properties/{0}.png";
	private static final String LUCK_IMAGE = "images/lucky/{0}.png";
	private static final String CHEST_IMAGE = "images/chest/{0}.png";
	private static final int TOTAL_NUM_CELLS = 40;
	private static final int NUM_CARD_INIT_CHEST = 33;
	private static final int NUM_CARD_FINISH_CHEST = 64;
	private static final int NUM_CARD_INIT_LUCK = 1;
	private static final int NUM_CARD_FINISH_LUCK = 32;

	@FXML
	private Button exitButton;
	@FXML
	private GridPane board_game;
	@FXML
	private Text lblPlayerName;
	@FXML
	private ImageView imgPlayerPhoto;
	@FXML
	private VBox playerOrderContainer;
	@FXML
	private Label lblAction;
	@FXML
	private ImageView imageDiceFirst;
	@FXML
	private ImageView imageDiceSecond;
	@FXML
	private Button rollDice;
	@FXML
	private AnchorPane centerPane;
	@FXML
	private AnchorPane mainPane;

	// Game
	private Game actualGame;
	private Boolean isFinished = false;
	private int dice1;
	private int dice2;
	private Player actualPlayer;
	private int currentProfileIndex = 0;
	private int turnIndex = 0;
	private boolean faseInicial = true;
	private List<Player> orderTurn = new ArrayList<Player>();
	private List<Player> allPlayers = new ArrayList<Player>();
	private List<Profile> selectedProfiles = new ArrayList<Profile>();
	private List<String> selectedTokens = new ArrayList<String>();
	private List<Cell> cells = new ArrayList<Cell>();
	private Map<Profile, Integer> tiradasPorJugador = new HashMap<>();
	private Consumer<TiradaResultado> resultadoCallback;
	private List<Label> playerLabels = new ArrayList<>();
	private List<Point2D> borde = new ArrayList<>();
	private Map<Point2D, Cell> mapaCeldaPorCoordenada = new HashMap<>();
	private Map<Point2D, StackPane> stackPanePorCoordenada = new HashMap<>();
	private Map<Point2D, HBox> hboxPorCoordenada = new HashMap<>();

	// DAOs
	private DAOManager daoManager = new DAOManager();
	private GameDAO gameDAO = daoManager.getGameDAO();
	private CellDAO cellDAO = daoManager.getCellDAO();
	private CardDAO cardDAO = daoManager.getCardDAO();
	private PlayerDAO playerDAO = daoManager.getPlayerDAO();
	private PlayerCardDAO playerCardDAO = daoManager.getPlayerCardDAO();
	private PlayerPropertyDAO playerPropertyDAO = daoManager.getPlayerPropertyDAO();

	/* ── Setters ────────────────────────────────────────────────────────────── */

	@FXML
	public void initialize() {
		Platform.runLater(() -> {
			try {
				Stage stage = (Stage) exitButton.getScene().getWindow();
				stage.setOnCloseRequest(event -> {
					if (actualGame != null) {
						guardarPartida();
					}
				});
			} catch (Exception e) {
				// Stage aún no disponible, se ignorará
			}
		});
	}

	/** @author Ana */
	public void setSelectedToken(String path) {
		this.selectedTokens.add(path);
	}

	/** @author Ana */
	public void setProfiles(List<Profile> selectedProfiles) {
		this.selectedProfiles = selectedProfiles;
		if (selectedProfiles == null || selectedProfiles.isEmpty()) {
			System.out.println("Error: Los perfiles no se han cargado en el GameController.");
			return;
		}
		initGame();
	}

	/* ── Inits ──────────────────────────────────────────────────────────────── */

	/** @author Ana */
	private void initGame() {
		if (selectedProfiles == null || selectedProfiles.isEmpty()) {
			System.out.println("Error: Los perfiles no se han cargado en el GameController.");
		} else {
			System.out.println("Perfiles cargados en el GameController: " + selectedProfiles.size());
			for (Profile profile : selectedProfiles) {
				System.out.println("Perfil: " + profile.getNickname());
			}
		}

		actualGame = new Game();
		actualGame.setDuration("60");
		actualGame.setState(State.IN_GAME);

		int idGame = gameDAO.addGame(actualGame);
		actualGame.setIdGame(idGame);

		cells = cellDAO.getAll();
		initBoard();

		selectedTokens.clear();
		for (int i = 0; i < selectedProfiles.size(); i++) {
			seleccionarFichaJugador();
		}

		Image image = new Image(getClass().getResource("/images/board/tablero.png").toExternalForm());
		BackgroundImage backgroundImage = new BackgroundImage(image, BackgroundRepeat.NO_REPEAT,
				BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER,
				new BackgroundSize(100, 100, true, true, true, false));
		board_game.setBackground(new Background(backgroundImage));
	}

	/** @author Ana */
	private void initBoard() {
		// Fila inferior (derecha a izquierda)
		for (int col = 10; col >= 0; col--) {
			borde.add(new Point2D(10, col));
		}
		// Columna izquierda (de abajo hacia arriba, sin esquinas)
		for (int row = 9; row >= 1; row--) {
			borde.add(new Point2D(row, 0));
		}
		// Fila superior (izquierda a derecha)
		for (int col = 0; col <= 10; col++) {
			borde.add(new Point2D(0, col));
		}
		// Columna derecha (de arriba hacia abajo, sin esquinas)
		for (int row = 1; row <= 9; row++) {
			borde.add(new Point2D(row, 10));
		}

		mapaCeldaPorCoordenada = new HashMap<>();
		for (int i = 0; i < borde.size(); i++) {
			mapaCeldaPorCoordenada.put(borde.get(i), cells.get(i));
		}

		stackPanePorCoordenada = new HashMap<>();
		hboxPorCoordenada = new HashMap<>();

		for (Point2D coordenada : borde) {
			int fila = (int) coordenada.getX();
			int columna = (int) coordenada.getY();

			StackPane contenedor = new StackPane();
			contenedor.setPrefSize(60, 60);

			HBox hboxFichas = new HBox(2);
			hboxFichas.setAlignment(Pos.CENTER);
			contenedor.getChildren().add(hboxFichas);

			board_game.add(contenedor, columna, fila);

			stackPanePorCoordenada.put(coordenada, contenedor);
			hboxPorCoordenada.put(coordenada, hboxFichas);
		}
	}

	/** @author Ana */
	private void initPlayers() {
		for (int i = 0; i < selectedProfiles.size(); i++) {
			Player player = new Player();
			player.setProfile(selectedProfiles.get(i));

			Cell startCell = cellDAO.findCellById(0);
			player.setMoney(1500);
			player.setCards(new ArrayList<Card>());
			player.setProperties(new ArrayList<Property>());
			player.setGame(actualGame);
			player.setBankrupt(false);
			player.setJailTurnsLeft(0);
			player.setCell(startCell);
			player.setToken(selectedTokens.get(i));

			ImageView ficha = new ImageView(new Image(player.getToken()));
			ficha.setFitWidth(20);
			ficha.setFitHeight(20);
			player.setImgToken(ficha);

			int idPlayer = playerDAO.addPlayer(player);
			player.setIdPlayer(idPlayer);

			allPlayers.add(player);
			System.out.println("Jugador " + player.getProfile().getNickname() + " creado con éxito.");
		}
	}

	/** @author Ana */
	private void seleccionarFichaJugador() {
		lblAction.setText(
				"Selecciona una ficha para el jugador: " + selectedProfiles.get(currentProfileIndex).getNickname());
		lblAction.setPrefWidth(169);
		lblAction.setPrefHeight(Region.USE_COMPUTED_SIZE);
		lblAction.setWrapText(true);

		ScrollPane scrollPane = new ScrollPane();
		scrollPane.setFitToWidth(true);
		scrollPane.setFitToHeight(true);
		scrollPane.setLayoutX(51);
		scrollPane.setLayoutY(51);
		scrollPane.setPrefWidth(275);
		scrollPane.setPrefHeight(275);

		TilePane tilePane = new TilePane();
		tilePane.setPrefColumns(4);
		tilePane.setPrefRows(2);

		for (String imagePath : TOKENS_IMAGES) {
			Image image = new Image(imagePath, true);
			ImageView imageView = new ImageView(image);
			imageView.setFitWidth(90);
			imageView.setFitHeight(90);
			imageView.setPreserveRatio(true);
			imageView.setCursor(Cursor.HAND);

			imageView.setOnMouseClicked(e -> {
				selectedTokens.add(imagePath);
				System.out.println("Ficha seleccionada: " + imagePath);

				currentProfileIndex++;
				if (currentProfileIndex < selectedProfiles.size()) {
					seleccionarFichaJugador();
				} else {
					initPlayers();
					mostrarPanelOrdenDeTurno();
				}
			});

			tilePane.getChildren().add(imageView);
		}

		centerPane.getChildren().clear();
		centerPane.getChildren().add(scrollPane);
		scrollPane.setContent(tilePane);
	}

	/** @author Ana */
	private void iniciarFichasJugadores() {
		Cell celdaSalida = cellDAO.findCellById(0);
		Point2D coordenadas = null;

		for (Map.Entry<Point2D, Cell> entry : mapaCeldaPorCoordenada.entrySet()) {
			if (entry.getValue().getIdCell() == celdaSalida.getIdCell()) {
				coordenadas = entry.getKey();
				break;
			}
		}

		if (coordenadas != null) {
			HBox hboxFichas = hboxPorCoordenada.get(coordenadas);
			for (Player player : orderTurn) {
				ImageView ficha = player.getImgToken();
				Parent anterior = ficha.getParent();
				if (anterior instanceof Pane) {
					((Pane) anterior).getChildren().remove(ficha);
				}
				hboxFichas.getChildren().add(ficha);
			}
		}
	}

	/* ── Juego ──────────────────────────────────────────────────────────────── */

	/** @author Ana */
	public void startGame() {
		turnIndex = 0;
		iniciarFichasJugadores();
		startTurn();
	}

	/** @author Ana */
	private void startTurn() {
		Player currentPlayer = orderTurn.get(turnIndex);
		mostrarPanelTurnoJugador(currentPlayer.getProfile());
	}

	/** @author Ana */
	private void avanzarTurnoLanzamiento() {
		if (currentProfileIndex >= selectedProfiles.size()) {
			if (faseInicial) {
				System.out.println("Todos los jugadores han lanzado los dados para determinar el orden.");
				determinarOrdenTurno();
			} else {
				currentProfileIndex = 0;
			}
			return;
		}

		Profile jugador = selectedProfiles.get(currentProfileIndex);
		lblPlayerName.setText(jugador.getNickname());
		imgPlayerPhoto.setImage(new Image(jugador.getImage()));
	}

	/** @author Ana */
	private void determinarOrdenTurno() {
		List<Profile> ordenFinal = selectedProfiles.stream().filter(p -> tiradasPorJugador.containsKey(p))
				.sorted(Comparator.comparingInt((Profile p) -> tiradasPorJugador.get(p)).reversed())
				.collect(Collectors.toList());

		// Detectar empates entre los jugadores con la puntuación más alta
		int maxPuntuacion = tiradasPorJugador.get(ordenFinal.get(0));
		List<Profile> empatados = ordenFinal.stream().filter(p -> tiradasPorJugador.get(p) == maxPuntuacion)
				.collect(Collectors.toList());

		if (empatados.size() > 1) {
			// Hay empate — avisar y repetir solo para los empatados
			String nombresEmpatados = empatados.stream().map(Profile::getNickname).collect(Collectors.joining(" y "));
			lblAction.setText("\u00a1Empate entre " + nombresEmpatados + " con " + maxPuntuacion
					+ " puntos!\nVolver\u00e1n a lanzar los dados.");
			lblAction.setPrefWidth(169);
			lblAction.setPrefHeight(Region.USE_COMPUTED_SIZE);
			lblAction.setWrapText(true);

			new Thread(() -> {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				}
				Platform.runLater(() -> {
					// Reiniciar solo con los jugadores empatados
					List<Profile> anteriores = new java.util.ArrayList<>(selectedProfiles);
					selectedProfiles.clear();
					selectedProfiles.addAll(empatados);
					tiradasPorJugador.clear();
					currentProfileIndex = 0;
					faseInicial = true;
					mostrarPanelOrdenDeTurno();
					// Restaurar lista completa para cuando acabe el desempate
					selectedProfiles.addAll(
							anteriores.stream().filter(p -> !empatados.contains(p)).collect(Collectors.toList()));
				});
			}).start();
			return;
		}

		orderTurn.clear();
		for (Profile p : ordenFinal) {
			for (Player jugador : allPlayers) {
				if (jugador.profile.equals(p)) {
					orderTurn.add(jugador);
					break;
				}
			}
		}

		playerOrderContainer.getChildren().clear();
		playerLabels.clear();
		for (int i = 0; i < orderTurn.size(); i++) {
			Player player = orderTurn.get(i);
			String labelText = String.format("Turno %d: %s - %d$", i + 1, player.getProfile().getNickname(),
					player.getMoney());
			Label label = new Label(labelText);
			label.setStyle("-fx-font-size: 14px; -fx-padding: 5 0 5 0;");
			label.setFont(Font.font("Comic Sans MS", 14));
			playerOrderContainer.getChildren().add(label);
			playerLabels.add(label);
		}

		faseInicial = false;
		currentProfileIndex = 0;

		// Mostrar quién empieza durante 3 segundos antes de arrancar
		String primerJugador = orderTurn.get(0).getProfile().getNickname();
		lblAction.setText(
				"\u00a1" + primerJugador + " empieza la partida!\n\nOrden de juego:\n" + buildResultadosTexto());
		lblAction.setPrefWidth(169);
		lblAction.setPrefHeight(Region.USE_COMPUTED_SIZE);
		lblAction.setWrapText(true);

		new Thread(() -> {
			try {
				Thread.sleep(3000);
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
			Platform.runLater(() -> startGame());
		}).start();
	}

	/* ── Dados ──────────────────────────────────────────────────────────────── */

	/**
	 * Lanza los dados con animación y llama al callback con el resultado final.
	 * 
	 * @author Víctor
	 */
	public void rollDice() {
		Random random = new Random();
		Thread thread = new Thread() {
			@Override
			public void run() {
				try {
					for (int i = 0; i < 15; i++) {
						int randomNum1 = random.nextInt(6) + 1;
						int randomNum2 = random.nextInt(6) + 1;

						String resourcePathFirst = MessageFormat.format(DICE_IMAGE, randomNum1);
						String resourcePathSecond = MessageFormat.format(DICE_IMAGE, randomNum2);

						URL resourceUrlFirst = getClass().getClassLoader().getResource(resourcePathFirst);
						URL resourceUrlSecond = getClass().getClassLoader().getResource(resourcePathSecond);

						if (resourceUrlFirst != null && resourceUrlSecond != null) {
							Image diceFace1 = new Image(resourceUrlFirst.toString());
							Image diceFace2 = new Image(resourceUrlSecond.toString());

							int finalNum1 = randomNum1;
							int finalNum2 = randomNum2;

							Platform.runLater(() -> {
								imageDiceFirst.setImage(diceFace1);
								imageDiceSecond.setImage(diceFace2);
							});

							if (i == 14) {
								dice1 = finalNum1;
								dice2 = finalNum2;

								TiradaResultado resultado = new TiradaResultado(dice1, dice2);
								Platform.runLater(() -> {
									if (resultadoCallback != null) {
										resultadoCallback.accept(resultado);
									}
								});
							}
						} else {
							System.out.println("No se encontró la imagen:");
							System.out.println("   " + resourcePathFirst + " -> " + resourceUrlFirst);
							System.out.println("   " + resourcePathSecond + " -> " + resourceUrlSecond);
						}

						Thread.sleep(50);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		thread.start();
	}

	/**
	 * Sobrecarga con callback puntual. Úsala desde mostrarPanelTurnoJugador().
	 */
	public void rollDice(Consumer<TiradaResultado> callback) {
		this.resultadoCallback = callback;
		rollDice();
	}

	/* ── Estado del juego ───────────────────────────────────────────────────── */

	/** @author Ana */
	public boolean isGameFinished() {
		if (isFinished) {
			return true;
		}

		int activePlayers = 0;
		for (Player player : orderTurn) {
			if (!player.getIsBankrupt()) {
				activePlayers++;
			}
		}
		return activePlayers <= 1;
	}

	/* ── Salir / Guardar ────────────────────────────────────────────────────── */

	@FXML
	public void exitGame() {
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setTitle("Salir del Juego");
		alert.setHeaderText("¿Estás seguro que quieres salir?");
		alert.setContentText("Elige una opción:");

		ButtonType mainMenuButton = new ButtonType("Menu Principal");
		ButtonType exitAppButton = new ButtonType("Salir", ButtonBar.ButtonData.OK_DONE);
		ButtonType cancelButton = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);

		alert.getButtonTypes().setAll(mainMenuButton, exitAppButton, cancelButton);

		alert.showAndWait().ifPresent(response -> {
			if (response == mainMenuButton) {
				try {
					guardarPartida();
					Stage stage = (Stage) exitButton.getScene().getWindow();
					double currentWidth = stage.getWidth();
					double currentHeight = stage.getHeight();

					FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MainView.fxml"));
					Parent root = loader.load();

					Scene scene = new Scene(root, currentWidth, currentHeight);
					stage.setScene(scene);
					stage.show();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else if (response == exitAppButton) {
				guardarPartida();
				System.exit(0);
			}
		});
	}

	/** @author Víctor */
	private void guardarPartida() {
		javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog("Mi partida");
		dialog.setTitle("Guardar partida");
		dialog.setHeaderText("¿Con qué nombre quieres guardar la partida?");
		dialog.setContentText("Nombre:");

		dialog.showAndWait().ifPresent(nombre -> {
			try {
				if (actualGame != null) {
					actualGame.setState(Game.State.SAVED);
					actualGame.setName(nombre.trim().isEmpty() ? "Partida #" + actualGame.getIdGame() : nombre.trim());
					gameDAO.updateGame(actualGame);
					System.out.println("[DEBUG] Juego guardado: " + actualGame.getName());
				}
				for (Player player : allPlayers) {
					playerDAO.updatePlayer(player);
				}
				System.out.println("[DEBUG] Guardado finalizado.");
			} catch (Exception e) {
				System.err.println("[ERROR] Error al guardar la partida:");
				e.printStackTrace();
			}
		});
	}

	/**
	 * Carga una partida guardada desde la BD y restaura su estado. Llamado desde
	 * ListGamesSavedController.
	 */
	public void setGame(Game savedGame) {
		System.out.println("[DEBUG] Cargando partida guardada: ID = " + savedGame.getIdGame());
		this.actualGame = savedGame;

		allPlayers = playerDAO.getPlayersByGame(savedGame.getIdGame());

		if (allPlayers == null || allPlayers.isEmpty()) {
			System.out.println("[ERROR] No se encontraron jugadores para la partida " + savedGame.getIdGame());
			return;
		}

		selectedProfiles.clear();
		selectedTokens.clear();
		for (Player player : allPlayers) {
			selectedProfiles.add(player.getProfile());

			String token = player.getToken() != null ? player.getToken() : TOKENS_IMAGES[0];
			player.setToken(token);
			selectedTokens.add(token);

			ImageView ficha = new ImageView(new Image(token));
			ficha.setFitWidth(20);
			ficha.setFitHeight(20);
			player.setImgToken(ficha);
		}

		orderTurn = new ArrayList<>(allPlayers);

		cells = cellDAO.getAll();
		initBoard();

		Image image = new Image(getClass().getResource("/images/board/tablero.png").toExternalForm());
		BackgroundImage backgroundImage = new BackgroundImage(image, BackgroundRepeat.NO_REPEAT,
				BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER,
				new BackgroundSize(100, 100, true, true, true, false));
		board_game.setBackground(new Background(backgroundImage));

		playerOrderContainer.getChildren().clear();
		playerLabels.clear();
		for (int i = 0; i < orderTurn.size(); i++) {
			Player player = orderTurn.get(i);
			String labelText = String.format("Turno %d: %s - %d$", i + 1, player.getProfile().getNickname(),
					player.getMoney());
			Label label = new Label(labelText);
			label.setStyle("-fx-font-size: 14px; -fx-padding: 5 0 5 0;");
			label.setFont(Font.font("Comic Sans MS", 14));
			playerOrderContainer.getChildren().add(label);
			playerLabels.add(label);
		}

		faseInicial = false;
		turnIndex = 0;
		startGame();
	}

	/* ── Turnos ─────────────────────────────────────────────────────────────── */

	/** @author Ana */
	private void mostrarPanelOrdenDeTurno() {
		currentProfileIndex = 0;
		actualizarUIJugadorActual();

		imageDiceFirst = new ImageView();
		imageDiceFirst.setFitWidth(120);
		imageDiceFirst.setFitHeight(120);
		imageDiceFirst.setLayoutX(36);
		imageDiceFirst.setLayoutY(85);

		imageDiceSecond = new ImageView();
		imageDiceSecond.setFitWidth(120);
		imageDiceSecond.setFitHeight(120);
		imageDiceSecond.setLayoutX(212);
		imageDiceSecond.setLayoutY(85);

		Button lanzarButton = new Button("Lanzar dados");
		lanzarButton.setFont(Font.font("Comic Sans MS", 14));
		lanzarButton.setLayoutX(129);
		lanzarButton.setLayoutY(256);

		lanzarButton.setOnAction(e -> {
			lanzarButton.setDisable(true);
			// Capturamos el indice actual ANTES de lanzar para evitar race conditions
			int indiceActual = currentProfileIndex;
			Profile jugador = selectedProfiles.get(indiceActual);

			resultadoCallback = resultado -> {
				int dado1 = resultado.getDado1();
				int dado2 = resultado.getDado2();
				int total = dado1 + dado2;
				System.out.println(
						"[ORDEN] " + jugador.getNickname() + " ha sacado " + dado1 + " + " + dado2 + " = " + total);
				tiradasPorJugador.put(jugador, total);
				currentProfileIndex++;

				// Mostrar resultado y esperar 3 segundos antes de continuar
				Platform.runLater(() -> {
					lblAction.setText(jugador.getNickname() + " ha sacado " + dado1 + " + " + dado2 + " = " + total
							+ "\nResultados hasta ahora:\n" + buildResultadosTexto());
					lblAction.setPrefWidth(169);
					lblAction.setPrefHeight(Region.USE_COMPUTED_SIZE);
					lblAction.setWrapText(true);
				});

				// Esperar 3.5 segundos en hilo secundario antes de continuar
				new Thread(() -> {
					try {
						Thread.sleep(3500);
					} catch (InterruptedException ex) {
						ex.printStackTrace();
					}
					Platform.runLater(() -> {
						if (currentProfileIndex < selectedProfiles.size()) {
							actualizarUIJugadorActual();
							lanzarButton.setDisable(false);
						} else {
							System.out.println("[ORDEN] Todos han lanzado. Determinando orden...");
							determinarOrdenTurno();
						}
					});
				}).start();
			};

			rollDice();
		});

		centerPane.getChildren().clear();
		centerPane.getChildren().addAll(imageDiceFirst, imageDiceSecond, lanzarButton);
	}

	/**
	 * Construye el texto con los resultados de todos los jugadores que ya han
	 * lanzado
	 */
	private String buildResultadosTexto() {
		StringBuilder sb = new StringBuilder();
		for (Profile p : selectedProfiles) {
			if (tiradasPorJugador.containsKey(p)) {
				sb.append(p.getNickname()).append(": ").append(tiradasPorJugador.get(p)).append("\n");
			}
		}
		return sb.toString().trim();
	}

	/**
	 * Actualiza lblAction, lblPlayerName e imgPlayerPhoto con el jugador actual de
	 * la fase inicial
	 */
	private void actualizarUIJugadorActual() {
		if (currentProfileIndex >= selectedProfiles.size()) {
			return;
		}
		Profile jugador = selectedProfiles.get(currentProfileIndex);
		lblAction.setText("Lanza los dados para determinar el orden. Jugador actual: " + jugador.getNickname());
		lblAction.setPrefWidth(169);
		lblAction.setPrefHeight(Region.USE_COMPUTED_SIZE);
		lblAction.setWrapText(true);
		lblPlayerName.setText(jugador.getNickname());
		imgPlayerPhoto.setImage(new Image(jugador.getImage()));
		imgPlayerPhoto.setFitWidth(55);
		imgPlayerPhoto.setFitHeight(55);
	}

	/**
	 * Muestra el panel del turno del jugador: dados + propiedades y cartas.
	 * 
	 * @author Ana
	 */
	private void mostrarPanelTurnoJugador(Profile profile) {
		// Buscar el Player correspondiente al Profile
		Player currentPlayer = null;
		for (Player p : orderTurn) {
			if (p.getProfile().equals(profile)) {
				currentPlayer = p;
				break;
			}
		}

		lblPlayerName.setText(profile.getNickname());
		imgPlayerPhoto.setImage(new Image(profile.getImage()));
		imgPlayerPhoto.setFitWidth(55);
		imgPlayerPhoto.setFitHeight(55);

		actualizarResaltadoJugador(currentPlayer);

		centerPane.getChildren().clear();

		// ── Dados ──────────────────────────────────────────────────────────────
		imageDiceFirst = new ImageView();
		imageDiceFirst.setFitWidth(120);
		imageDiceFirst.setFitHeight(120);
		imageDiceFirst.setLayoutX(36);
		imageDiceFirst.setLayoutY(85);

		imageDiceSecond = new ImageView();
		imageDiceSecond.setFitWidth(120);
		imageDiceSecond.setFitHeight(120);
		imageDiceSecond.setLayoutX(212);
		imageDiceSecond.setLayoutY(85);

		// ── Botón lanzar dados ─────────────────────────────────────────────────
		Button lanzarButton = new Button("Lanzar dados");
		lanzarButton.setFont(Font.font("Comic Sans MS", 14));
		lanzarButton.setLayoutX(129);
		lanzarButton.setLayoutY(256);

		final Player finalPlayer = currentPlayer;
		lanzarButton.setOnAction(e -> {
			lanzarButton.setDisable(true);

			rollDice(resultado -> {
				int dado1 = resultado.getDado1();
				int dado2 = resultado.getDado2();
				int total = dado1 + dado2;
				System.out.println("Dados: " + dado1 + " + " + dado2 + " = " + total);

				Platform.runLater(() -> {
					lblAction.setText(finalPlayer.getProfile().getNickname() + " ha sacado " + dado1 + " y " + dado2
							+ " (total: " + total + ")");
					lblAction.setPrefWidth(169);
					lblAction.setPrefHeight(Region.USE_COMPUTED_SIZE);
					lblAction.setWrapText(true);
				});

				if (finalPlayer != null && finalPlayer.getIsBankrupt()) {
					System.out.println("El jugador " + finalPlayer.getProfile().getNickname() + " está en bancarrota.");
					turnIndex++;
					if (turnIndex >= orderTurn.size()) {
						turnIndex = 0;
					}
					startTurn();
					return;
				}

				moverFichaJugador(finalPlayer, total);
				ejecutarMovimiento(finalPlayer, dado1, dado2);
				lanzarButton.setDisable(false);
			});
		});

		// ── Panel de propiedades y cartas del jugador actual ───────────────────
		VBox propiedadesBox = new VBox(4);
		propiedadesBox.setPrefWidth(335);

		if (currentPlayer != null) {
			Label titleProps = new Label("🏠 Propiedades:");
			titleProps.setFont(Font.font("Comic Sans MS", 13));
			titleProps.setStyle("-fx-font-weight: bold;");
			propiedadesBox.getChildren().add(titleProps);

			List<Property> props = currentPlayer.getProperties();
			if (props == null || props.isEmpty()) {
				Label noProps = new Label("  (ninguna)");
				noProps.setFont(Font.font("Comic Sans MS", 11));
				propiedadesBox.getChildren().add(noProps);
			} else {
				for (Property prop : props) {
					String nombre = (prop.getName() != null && !prop.getName().isEmpty()) ? prop.getName()
							: "Propiedad #" + prop.getIdProperty();
					Label propLabel = new Label("  · " + nombre + "  [" + prop.getBuyValue() + "$]");
					propLabel.setFont(Font.font("Comic Sans MS", 11));
					propiedadesBox.getChildren().add(propLabel);
				}
			}

			propiedadesBox.getChildren().add(new Label(""));

			Label titleCards = new Label("🃏 Cartas:");
			titleCards.setFont(Font.font("Comic Sans MS", 13));
			titleCards.setStyle("-fx-font-weight: bold;");
			propiedadesBox.getChildren().add(titleCards);

			List<Card> cards = currentPlayer.getCards();
			if (cards == null || cards.isEmpty()) {
				Label noCards = new Label("  (ninguna)");
				noCards.setFont(Font.font("Comic Sans MS", 11));
				propiedadesBox.getChildren().add(noCards);
			} else {
				for (Card card : cards) {
					String tipo = card.getType() == Card.CardType.LUCK ? "Suerte" : "Cofre";
					String accion = card.getAction() != null ? card.getAction().getActionType().name() : "?";
					int veces = card.getAction() != null ? card.getAction().getTimes() : 0;
					Label cardLabel = new Label("  · [" + tipo + "] " + accion + " x" + veces);
					cardLabel.setFont(Font.font("Comic Sans MS", 11));
					propiedadesBox.getChildren().add(cardLabel);
				}
			}
		}

		// El ScrollPane de propiedades va al panel izquierdo (mainPane),
		// debajo del playerOrderContainer (que termina en Y≈170).
		// Eliminamos primero cualquier scroll anterior que hubiera.
		mainPane.getChildren().removeIf(n -> n instanceof ScrollPane);

		ScrollPane scrollProps = new ScrollPane(propiedadesBox);
		scrollProps.setLayoutX(10);
		scrollProps.setLayoutY(175);
		scrollProps.setPrefWidth(226);
		scrollProps.setPrefHeight(430);
		scrollProps.setFitToWidth(true);
		scrollProps.setStyle("-fx-background-color: transparent;");

		mainPane.getChildren().add(scrollProps);

		centerPane.getChildren().addAll(imageDiceFirst, imageDiceSecond, lanzarButton);
	}

	/** @author Ana */
	private void actualizarResaltadoJugador(Player actualPlayer) {
		for (int i = 0; i < orderTurn.size(); i++) {
			Player player = orderTurn.get(i);
			Label label = playerLabels.get(i);
			label.setFont(Font.font("Comic Sans MS", 14));
			if (player.equals(actualPlayer)) {
				label.setStyle(
						"-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-color: #d0f0c0; -fx-padding: 5 0 5 0;");
			} else {
				label.setStyle("-fx-font-size: 14px; -fx-padding: 5 0 5 0;");
			}
			String labelText = String.format("Turno %d: %s - %d$", i + 1, player.getProfile().getNickname(),
					player.getMoney());
			label.setText(labelText);
		}
	}

	/** @author Ana */
	private void mostrarMensajeJuegoTerminado() {
		centerPane.getChildren().clear();
		Label lblInfo = new Label("El juego ha terminado");
		lblInfo.setLayoutX(28);
		lblInfo.setLayoutY(102);
		lblInfo.setMaxWidth(273);
		lblInfo.setWrapText(true);
		lblInfo.setFont(Font.font("Comic Sans MS", 14));
		centerPane.getChildren().add(lblInfo);
	}

	/* ── Movimiento ─────────────────────────────────────────────────────────── */

	/** @author Víctor */
	public void moverFichaJugador(Player actualPlayer, int total) {
		ImageView imgFicha = actualPlayer.getImgToken();
		Cell actualCell = actualPlayer.getCell();
		int actualCellNumber = actualCell.getIdCell();

		// Celdas numeradas del 0 al 39, salida = celda 0
		int nextCellNumber = (actualCellNumber + total) % TOTAL_NUM_CELLS;

		Cell newCell = cellDAO.findCellById(nextCellNumber);
		actualPlayer.setCell(newCell);

		Point2D coordenadas = null;
		for (Map.Entry<Point2D, Cell> entry : mapaCeldaPorCoordenada.entrySet()) {
			if (entry.getValue().getIdCell() == newCell.getIdCell()) {
				coordenadas = entry.getKey();
				break;
			}
		}

		if (coordenadas != null) {
			HBox hboxDestino = hboxPorCoordenada.get(coordenadas);
			Parent padre = imgFicha.getParent();
			if (padre instanceof Pane) {
				((Pane) padre).getChildren().remove(imgFicha);
			}
			hboxDestino.getChildren().add(imgFicha);
		}
	}

	/** @author Víctor */
	public void ejecutarMovimiento(Player actualPlayer, int dado1, int dado2) {
		if (isGameFinished()) {
			mostrarMensajeJuegoTerminado();
			return;
		}

		actualizarResaltadoJugador(actualPlayer);

		System.out.println("Turno del jugador " + actualPlayer.getProfile().getNickname());

		if (actualPlayer.getJailTurnsLeft() != 0) {
			lblAction.setText(
					"El jugador " + actualPlayer.getProfile().getNickname() + " está en la cárcel y no puede moverse.");
			lblAction.setPrefWidth(169);
			lblAction.setPrefHeight(Region.USE_COMPUTED_SIZE);
			lblAction.setWrapText(true);

			if (dado1 == dado2) {
				actualPlayer.setJailTurnsLeft(0);
				System.out.println(actualPlayer.getProfile().getNickname() + " ha sacado dobles y sale de la cárcel.");
			} else {
				actualPlayer.setJailTurnsLeft(actualPlayer.getJailTurnsLeft() - 1);
				turnIndex++;
				if (turnIndex >= orderTurn.size()) {
					turnIndex = 0;
				}
				startTurn();
			}
			return;
		}

		if (dado1 == dado2) {
			System.out.println(actualPlayer.getProfile().getNickname() + " ha sacado dobles y lanza de nuevo.");
			mostrarPanelTurnoJugador(actualPlayer.getProfile());
			return;
		}

		// ← USA la celda ya actualizada por moverFichaJugador(), no recalcules
		Cell newCell = actualPlayer.getCell();
		System.out.println("Jugador en celda: " + newCell.getIdCell() + " tipo: " + newCell.getType());

		switch (newCell.getType()) {
		case PROPERTY:
			handlePropertyCell(newCell, actualPlayer);
			break;
		case JAIL:
			handleJailCell(actualPlayer);
			break;
		case LUCK:
			handleLuckCell(newCell, actualPlayer);
			break;
		case COMMUNITY_CHEST:
			handleCommunityChestCell(newCell, actualPlayer);
			break;
		case START:
			handleStartCell(newCell, actualPlayer);
			break;
		case TAX:
			handleTaxCell(newCell, actualPlayer);
			break;
		case PARKING:
			System.out.println("Parking. Sin acción.");
			break;
		default:
			break;
		}

		if (isGameFinished()) {
			mostrarMensajeJuegoTerminado();
			return;
		}

		playerDAO.updatePlayer(actualPlayer);
		actualizarResaltadoJugador(actualPlayer);
	}

	/* ── Propiedades ────────────────────────────────────────────────────────── */

	/** @author Ana */
	public void handlePropertyCell(Cell cell, Player player) {
		lblAction.setText("Has caído en una celda de propiedad.");
		lblAction.setPrefWidth(169);
		lblAction.setPrefHeight(Region.USE_COMPUTED_SIZE);
		lblAction.setWrapText(true);

		Property property = cell.getProperty();
		if (property != null) {
			boolean hasOwner = playerPropertyDAO.isPropertyOwned(property.getIdProperty(), actualGame.getIdGame());
			int ownerId = playerPropertyDAO.getPropertyOwner(property.getIdProperty(), actualGame.getIdGame());
			Player owner = playerDAO.findPlayerById(ownerId);

			if (!hasOwner) {
				handleComprarPropiedad(property, player);
			} else if (owner != null && owner.getIdPlayer() != player.getIdPlayer()) {
				handleCobrarAlquiler(property, owner, player);
			} else {
				handleUpdateProperty(property, player);
			}
		} else {
			System.out.println("No hay propiedad en esta celda.");
		}
	}

	/** @author Ana */
	public void handleComprarPropiedad(Property property, Player player) {
		centerPane.getChildren().clear();

		ImageView imgProperty = new ImageView();
		String resourcePath = MessageFormat.format(PROPERTY_IMAGE, property.getIdProperty());
		URL resourceUrl = getClass().getClassLoader().getResource(resourcePath);
		if (resourceUrl != null) {
			imgProperty.setImage(new Image(resourceUrl.toString()));
			imgProperty.setFitWidth(221);
			imgProperty.setFitHeight(269);
			imgProperty.setLayoutX(78);
			imgProperty.setLayoutY(27);
		}

		Button btnBuy = new Button("Comprar propiedad");
		btnBuy.setFont(Font.font("Comic Sans MS", 14));
		btnBuy.setLayoutX(34);
		btnBuy.setLayoutY(320);
		btnBuy.setOnAction(e -> comprarPropiedad(property, player));

		Button btnCancel = new Button("Terminar turno");
		btnCancel.setFont(Font.font("Comic Sans MS", 14));
		btnCancel.setLayoutX(210);
		btnCancel.setLayoutY(320);
		btnCancel.setOnAction(e -> {
			turnIndex++;
			if (turnIndex >= orderTurn.size()) {
				turnIndex = 0;
			}
			startTurn();
		});

		centerPane.getChildren().addAll(imgProperty, btnBuy, btnCancel);
	}

	/** @author Ana */
	public void handleCobrarAlquiler(Property property, Player owner, Player player) {
		centerPane.getChildren().clear();

		int alquiler = cobrarAlquiler(property, owner, player);

		Label lblInfo = new Label(
				"Tienes que pagarle " + alquiler + " al propietario " + owner.getProfile().getNickname());
		lblInfo.setLayoutX(35);
		lblInfo.setLayoutY(63);
		lblInfo.setMaxWidth(317);
		lblInfo.setWrapText(true);
		lblInfo.setFont(Font.font("Comic Sans MS", 14));

		Button btnTerminarTurno = new Button("Terminar turno");
		btnTerminarTurno.setFont(Font.font("Comic Sans MS", 14));
		btnTerminarTurno.setLayoutX(129);
		btnTerminarTurno.setLayoutY(256);
		btnTerminarTurno.setOnAction(e -> {
			turnIndex++;
			if (turnIndex >= orderTurn.size()) {
				turnIndex = 0;
			}
			startTurn();
		});

		centerPane.getChildren().addAll(lblInfo, btnTerminarTurno);
	}

	/** @author Ana */
	public void handleUpdateProperty(Property property, Player actualPlayer) {
		centerPane.getChildren().clear();

		ImageView imgProperty = new ImageView();
		String resourcePath = MessageFormat.format(PROPERTY_IMAGE, property.getIdProperty());
		URL resourceUrl = getClass().getClassLoader().getResource(resourcePath);
		if (resourceUrl != null) {
			imgProperty.setImage(new Image(resourceUrl.toString()));
			imgProperty.setFitWidth(190);
			imgProperty.setFitHeight(270);
			imgProperty.setLayoutX(14);
			imgProperty.setLayoutY(18);
		}

		Label lblEpisodios = new Label("Número de episodios:");
		lblEpisodios.setPrefWidth(117);
		lblEpisodios.setPrefHeight(17);
		lblEpisodios.setLayoutX(216);
		lblEpisodios.setLayoutY(6);
		lblEpisodios.setFont(Font.font("Comic Sans MS", 14));

		HBox hbEpisodios = new HBox(5);
		hbEpisodios.setLayoutX(216);
		hbEpisodios.setLayoutY(29);
		hbEpisodios.setPrefWidth(122);
		hbEpisodios.setPrefHeight(31);
		for (int i = 0; i < property.getHouseNumber(); i++) {
			ImageView houseView = new ImageView(new Image("images/episode.png"));
			houseView.setFitWidth(30);
			houseView.setFitHeight(30);
			hbEpisodios.getChildren().add(houseView);
		}

		Label lblTemporadas = new Label("Número de temporadas:");
		lblTemporadas.setPrefWidth(125);
		lblTemporadas.setPrefHeight(17);
		lblTemporadas.setLayoutX(216);
		lblTemporadas.setLayoutY(65);
		lblTemporadas.setFont(Font.font("Comic Sans MS", 14));

		HBox hbTemporada = new HBox(5);
		hbTemporada.setLayoutX(216);
		hbTemporada.setLayoutY(90);
		hbTemporada.setPrefWidth(122);
		hbTemporada.setPrefHeight(31);
		for (int i = 0; i < property.getHotelNumber(); i++) {
			ImageView hotelView = new ImageView(new Image("images/season.png"));
			hotelView.setFitWidth(24);
			hotelView.setFitHeight(24);
			hbTemporada.getChildren().add(hotelView);
		}

		Button btnComprarCasa = new Button("Comprar episodio");
		btnComprarCasa.setFont(Font.font("Comic Sans MS", 14));
		btnComprarCasa.setLayoutX(216);
		btnComprarCasa.setLayoutY(129);
		btnComprarCasa.setOnAction(e -> comprarCasa(property, actualPlayer));

		Button btnVenderCasa = new Button("Vender episodio");
		btnVenderCasa.setFont(Font.font("Comic Sans MS", 14));
		btnVenderCasa.setLayoutX(216);
		btnVenderCasa.setLayoutY(171);
		btnVenderCasa.setOnAction(e -> venderCasa(property, actualPlayer));

		Button btnComprarHotel = new Button("Comprar temporada");
		btnComprarHotel.setFont(Font.font("Comic Sans MS", 14));
		btnComprarHotel.setLayoutX(216);
		btnComprarHotel.setLayoutY(215);
		btnComprarHotel.setOnAction(e -> comprarHotel(property, actualPlayer));

		Button btnVenderHotel = new Button("Vender temporada");
		btnVenderHotel.setFont(Font.font("Comic Sans MS", 14));
		btnVenderHotel.setLayoutX(216);
		btnVenderHotel.setLayoutY(262);
		btnVenderHotel.setOnAction(e -> venderHotel(property, actualPlayer));

		Button btnTerminarTurno = new Button("Terminar turno");
		btnTerminarTurno.setFont(Font.font("Comic Sans MS", 14));
		btnTerminarTurno.setLayoutX(129);
		btnTerminarTurno.setLayoutY(315);
		btnTerminarTurno.setOnAction(e -> {
			turnIndex++;
			if (turnIndex >= orderTurn.size()) {
				turnIndex = 0;
			}
			startTurn();
		});

		centerPane.getChildren().addAll(imgProperty, lblEpisodios, hbEpisodios, lblTemporadas, hbTemporada,
				btnComprarCasa, btnVenderCasa, btnComprarHotel, btnVenderHotel, btnTerminarTurno);
	}

	/** @author Ana */
	public void comprarPropiedad(Property property, Player player) {
		boolean hasOwner = playerPropertyDAO.isPropertyOwned(property.getIdProperty(), actualGame.getIdGame());
		if (!hasOwner) {
			int propertyValue = property.getBuyValue();
			if (checkIfPlayerCanPurchase(player.getMoney(), propertyValue)) {
				player.setMoney(player.getMoney() - propertyValue);
				player.getProperties().add(property);
				playerPropertyDAO.addPlayerProperty(
						new PlayerProperty(player.getIdPlayer(), property.getIdProperty(), actualGame.getIdGame()));
				System.out.println(
						"Propiedad " + property.getIdProperty() + " comprada por jugador " + player.getIdPlayer());
			} else {
				player.setBankrupt(true);
			}
		} else {
			System.out.println("La propiedad ya tiene dueño.");
		}
	}

	/** @author Ana */
	public void venderPropiedad(Property property, Player seller, Player buyer) {
		boolean esDueno = playerPropertyDAO.getPropertyOwner(property.getIdProperty(), actualGame.getIdGame()) == seller
				.getIdPlayer();
		if (!esDueno) {
			System.out.println("Error: el vendedor no es dueño de esta propiedad.");
			return;
		}

		int valorVenta = property.getSellValue();
		if (buyer.getMoney() < valorVenta) {
			System.out.println("El comprador no tiene suficiente dinero.");
			buyer.setBankrupt(true);
			return;
		}

		buyer.setMoney(buyer.getMoney() - valorVenta);
		seller.setMoney(seller.getMoney() + valorVenta);

		playerPropertyDAO.deletePlayerProperty(property.getIdProperty(), seller.getIdPlayer(), actualGame.getIdGame());
		playerPropertyDAO.addPlayerProperty(
				new PlayerProperty(buyer.getIdPlayer(), property.getIdProperty(), actualGame.getIdGame()));

		seller.getProperties().remove(property);
		buyer.getProperties().add(property);

		System.out.println("Propiedad vendida de " + seller.getProfile().getNickname() + " a "
				+ buyer.getProfile().getNickname() + " por " + valorVenta + "$.");
	}

	/** @author Ana */
	public void comprarCasa(Property property, Player player) {
		lblAction.setText("Has caído en una celda de propiedad. Puedes comprarla.");
		lblAction.setPrefWidth(169);
		lblAction.setPrefHeight(Region.USE_COMPUTED_SIZE);
		lblAction.setWrapText(true);

		if (property.getHouseNumber() < 3 && property.getHotelNumber() == 0) {
			if (checkIfPlayerCanPurchase(player.getMoney(), property.getHouseBuyValue())) {
				player.setMoney(player.getMoney() - property.getHouseBuyValue());
				property.setHouseNumber(property.getHouseNumber() + 1);
			} else {
				player.setBankrupt(true);
			}
		} else {
			System.out.println("No se puede comprar episodio: ya tiene 3 o tiene temporada.");
		}
	}

	/** @author Ana */
	public void comprarHotel(Property property, Player player) {
		lblAction.setText("Puedes comprar una temporada si tienes 3 episodios.");
		lblAction.setPrefWidth(169);
		lblAction.setPrefHeight(Region.USE_COMPUTED_SIZE);
		lblAction.setWrapText(true);

		if (property.getHouseNumber() == 3 && property.getHotelNumber() == 0) {
			if (checkIfPlayerCanPurchase(player.getMoney(), property.getHotelBuyValue())) {
				player.setMoney(player.getMoney() - property.getHotelBuyValue());
				property.setHotelNumber(1);
				property.setHouseNumber(0);
			} else {
				player.setBankrupt(true);
			}
		} else {
			System.out.println("No se puede comprar temporada: necesitas 3 episodios y no tener temporada.");
		}
	}

	/** @author Ana */
	public void venderCasa(Property property, Player player) {
		lblAction.setText("Puedes vender un episodio si tienes al menos 1.");
		lblAction.setPrefWidth(169);
		lblAction.setPrefHeight(Region.USE_COMPUTED_SIZE);
		lblAction.setWrapText(true);

		if (property.getHouseNumber() >= 1 && property.getHotelNumber() == 0) {
			player.setMoney(player.getMoney() + property.getHouseBuyValue());
			property.setHouseNumber(property.getHouseNumber() - 1);
		} else {
			System.out.println("No se puede vender episodio.");
		}
	}

	/** @author Ana */
	public void venderHotel(Property property, Player player) {
		lblAction.setText("Puedes vender una temporada si tienes 1.");
		lblAction.setPrefWidth(169);
		lblAction.setPrefHeight(Region.USE_COMPUTED_SIZE);
		lblAction.setWrapText(true);

		if (property.getHotelNumber() == 1) {
			player.setMoney(player.getMoney() + property.getHotelBuyValue());
			property.setHotelNumber(0);
		} else {
			System.out.println("No se puede vender temporada: no tienes ninguna.");
		}
	}

	/** @author Ana */
	public int cobrarAlquiler(Property property, Player owner, Player renter) {
		if (owner == null || renter == null || property == null) {
			System.err.println("Parámetros inválidos en cobrarAlquiler.");
			return 0;
		}

		lblAction.setText(
				"Has caído en una propiedad de " + owner.getProfile().getNickname() + ". Se cobrará el alquiler.");
		lblAction.setPrefWidth(169);
		lblAction.setPrefHeight(Region.USE_COMPUTED_SIZE);
		lblAction.setWrapText(true);

		int rent = 0;
		if (property.getHouseNumber() == 0 && property.getHotelNumber() == 0) {
			rent = property.getRentBaseValue();
		} else if (property.getHouseNumber() > 0) {
			List<RentHouseValue> rents = property.getRentHouseValue();
			if (rents != null && property.getHouseNumber() <= rents.size()) {
				rent = rents.get(property.getHouseNumber() - 1).getRentValue();
			}
		} else if (property.getHotelNumber() == 1) {
			rent = property.getRentHotelValue();
		}

		if (checkIfPlayerCanPurchase(renter.getMoney(), rent)) {
			renter.setMoney(renter.getMoney() - rent);
			owner.setMoney(owner.getMoney() + rent);
		} else {
			renter.setBankrupt(true);
			System.out.println("El jugador " + renter.getProfile().getNickname() + " está en bancarrota.");
		}
		return rent;
	}

	/** @author Ana */
	public Boolean checkIfPlayerCanPurchase(int actualMoney, int quantity) {
		return actualMoney > 0 && (actualMoney - quantity) >= 0;
	}

	/* ── Acciones en celdas ─────────────────────────────────────────────────── */

	/** @author Ana */
	public void executeAction(Action action, Player player) {
		String type = action.getActionType().name();
		int value = action.getTimes();
		Cell actualCell;
		int actualCellNumber, nextCellNumber;
		Cell newCell;

		switch (type) {
		case "PAY":
			player.setMoney(player.getMoney() - value);
			break;
		case "RECIEVE":
			player.setMoney(player.getMoney() + value);
			break;
		case "PAY_PLAYERS":
			for (Player other : orderTurn) {
				if (!other.equals(player) && !other.getIsBankrupt()) {
					player.setMoney(player.getMoney() - value);
					other.setMoney(other.getMoney() + value);
				}
			}
			break;
		case "RECIEVE_PLAYERS":
			for (Player other : orderTurn) {
				if (!other.equals(player) && !other.getIsBankrupt()) {
					other.setMoney(other.getMoney() - value);
					player.setMoney(player.getMoney() + value);
				}
			}
			break;
		case "EXIT_JAIL":
			player.setJailTurnsLeft(0);
			break;
		case "ROLL_DICE":
			mostrarPanelTurnoJugador(player.getProfile());
			break;
		case "GO_BACK_CELLS":
			actualCell = player.getCell();
			actualCellNumber = actualCell.getIdCell();
			nextCellNumber = (actualCellNumber - value % TOTAL_NUM_CELLS + TOTAL_NUM_CELLS) % TOTAL_NUM_CELLS;
			newCell = cellDAO.findCellById(nextCellNumber);
			player.setCell(newCell);
			break;
		case "MOVE_CELLS":
		case "SUM_CELL":
			actualCell = player.getCell();
			actualCellNumber = actualCell.getIdCell();
			nextCellNumber = (actualCellNumber + value) % TOTAL_NUM_CELLS;
			newCell = cellDAO.findCellById(nextCellNumber);
			player.setCell(newCell);
			break;
		case "GO_EXIT":
			newCell = cellDAO.findCellById(0);
			player.setCell(newCell);
			break;
		default:
			System.out.println("Acción desconocida: " + type);
			break;
		}
	}

	/** @author Ana */
	public void handleJailCell(Player player) {
		lblAction.setText("Has caído en una celda de cárcel.");
		lblAction.setPrefWidth(169);
		lblAction.setPrefHeight(Region.USE_COMPUTED_SIZE);
		lblAction.setWrapText(true);

		player.setJailTurnsLeft(3);
		centerPane.getChildren().clear();

		Label lblInfo = new Label("¡Oh no, has caído en la cárcel!");
		lblInfo.setLayoutX(85);
		lblInfo.setLayoutY(49);
		lblInfo.setMaxWidth(215);
		lblInfo.setFont(Font.font("Comic Sans MS", 14));

		ImageView imgCarcel = new ImageView(new Image("/images/cells/jail.jpg"));
		imgCarcel.setFitWidth(110);
		imgCarcel.setFitHeight(110);
		imgCarcel.setLayoutX(133);
		imgCarcel.setLayoutY(107);

		Button btnTerminarTurno = new Button("Terminar turno");
		btnTerminarTurno.setFont(Font.font("Comic Sans MS", 14));
		btnTerminarTurno.setLayoutX(129);
		btnTerminarTurno.setLayoutY(256);
		btnTerminarTurno.setOnAction(e -> {
			turnIndex++;
			if (turnIndex >= orderTurn.size()) {
				turnIndex = 0;
			}
			startTurn();
		});

		centerPane.getChildren().addAll(lblInfo, imgCarcel, btnTerminarTurno);
	}

	/** @author Ana */
	public void handleStartCell(Cell cell, Player player) {
		lblAction.setText("Has caído en la celda de salida.");
		lblAction.setPrefWidth(169);
		lblAction.setPrefHeight(Region.USE_COMPUTED_SIZE);
		lblAction.setWrapText(true);

		int startMoney = 200;
		player.setMoney(player.getMoney() + startMoney);
		centerPane.getChildren().clear();

		Label lblInfo = new Label("Has pasado por la salida y has recibido " + startMoney + " dólares.");
		lblInfo.setLayoutX(85);
		lblInfo.setLayoutY(49);
		lblInfo.setMaxWidth(215);
		lblInfo.setWrapText(true);
		lblInfo.setFont(Font.font("Comic Sans MS", 14));

		Button btnTerminarTurno = new Button("Terminar turno");
		btnTerminarTurno.setFont(Font.font("Comic Sans MS", 14));
		btnTerminarTurno.setLayoutX(129);
		btnTerminarTurno.setLayoutY(256);
		btnTerminarTurno.setOnAction(e -> {
			turnIndex++;
			if (turnIndex >= orderTurn.size()) {
				turnIndex = 0;
			}
			startTurn();
		});

		centerPane.getChildren().addAll(lblInfo, btnTerminarTurno);
	}

	/** @author Ana */
	public void handleTaxCell(Cell cell, Player player) {
		lblAction.setText("Has caído en una celda de impuestos.");
		lblAction.setPrefWidth(169);
		lblAction.setPrefHeight(Double.MAX_VALUE / 5);
		lblAction.setWrapText(true);

		int taxAmount = 200;
		if (checkIfPlayerCanPurchase(player.getMoney(), taxAmount)) {
			player.setMoney(player.getMoney() - taxAmount);
		} else {
			player.setBankrupt(true);
		}

		centerPane.getChildren().clear();

		Label lblInfo = new Label("Has caído en la celda de impuestos y has pagado " + taxAmount
				+ " dólares para poder hacer el rodaje de tu serie favorita.");
		lblInfo.setLayoutX(35);
		lblInfo.setLayoutY(63);
		lblInfo.setMaxWidth(317);
		lblInfo.setWrapText(true);
		lblInfo.setFont(Font.font("Comic Sans MS", 12));

		Button btnTerminarTurno = new Button("Terminar turno");
		btnTerminarTurno.setFont(Font.font("Comic Sans MS", 14));
		btnTerminarTurno.setLayoutX(129);
		btnTerminarTurno.setLayoutY(256);
		btnTerminarTurno.setOnAction(e -> {
			turnIndex++;
			if (turnIndex >= orderTurn.size()) {
				turnIndex = 0;
			}
			startTurn();
		});

		centerPane.getChildren().addAll(lblInfo, btnTerminarTurno);
	}

	/** @author Ana */
	public void handleLuckCell(Cell cell, Player player) {
		lblAction.setText("Has caído en una celda de carta de suerte.");
		lblAction.setPrefWidth(169);
		lblAction.setPrefHeight(Region.USE_COMPUTED_SIZE);
		lblAction.setWrapText(true);

		Card luckyCard = getRandomCard(CardType.LUCK);
		if (luckyCard == null) {
			System.out.println("[ERROR] No se pudo obtener carta de suerte.");
			centerPane.getChildren().clear();
			Label lblErr = new Label("No se pudo obtener carta. Turno sin acción.");
			lblErr.setLayoutX(35);
			lblErr.setLayoutY(63);
			lblErr.setMaxWidth(317);
			lblErr.setWrapText(true);
			lblErr.setFont(Font.font("Comic Sans MS", 14));
			Button btnSkip = new Button("Terminar turno");
			btnSkip.setFont(Font.font("Comic Sans MS", 14));
			btnSkip.setLayoutX(129);
			btnSkip.setLayoutY(256);
			btnSkip.setOnAction(e -> {
				turnIndex++;
				if (turnIndex >= orderTurn.size()) {
					turnIndex = 0;
				}
				startTurn();
			});
			centerPane.getChildren().addAll(lblErr, btnSkip);
			return;
		}
		player.getCards().add(luckyCard);
		playerCardDAO
				.addPlayerCard(new PlayerCard(player.getIdPlayer(), luckyCard.getIdCard(), actualGame.getIdGame()));

		centerPane.getChildren().clear();

		ImageView imgLuck = new ImageView();
		String resourcePath = MessageFormat.format(LUCK_IMAGE, luckyCard.getIdCard());
		URL resourceUrl = getClass().getClassLoader().getResource(resourcePath);
		if (resourceUrl != null) {
			imgLuck.setImage(new Image(resourceUrl.toString()));
			imgLuck.setFitWidth(311);
			imgLuck.setFitHeight(207);
			imgLuck.setLayoutX(33);
			imgLuck.setLayoutY(29);
		}

		Button btnTerminarTurno = new Button("Ejecutar acción y terminar turno");
		btnTerminarTurno.setFont(Font.font("Comic Sans MS", 14));
		btnTerminarTurno.setLayoutX(129);
		btnTerminarTurno.setLayoutY(256);
		btnTerminarTurno.setOnAction(e -> {
			executeAction(luckyCard.getAction(), player);
			turnIndex++;
			if (turnIndex >= orderTurn.size()) {
				turnIndex = 0;
			}
			startTurn();
		});

		centerPane.getChildren().addAll(imgLuck, btnTerminarTurno);
	}

	/** @author Ana */
	public void handleCommunityChestCell(Cell cell, Player player) {
		lblAction.setText("Has caído en una celda de cofre de comunidad.");
		lblAction.setPrefWidth(169);
		lblAction.setPrefHeight(Region.USE_COMPUTED_SIZE);
		lblAction.setWrapText(true);

		Card chestCard = getRandomCard(CardType.COMMUNITY_CHEST);
		if (chestCard == null) {
			System.out.println("[ERROR] No se pudo obtener carta de cofre.");
			centerPane.getChildren().clear();
			Label lblErr = new Label("No se pudo obtener carta. Turno sin acción.");
			lblErr.setLayoutX(35);
			lblErr.setLayoutY(63);
			lblErr.setMaxWidth(317);
			lblErr.setWrapText(true);
			lblErr.setFont(Font.font("Comic Sans MS", 14));
			Button btnSkip = new Button("Terminar turno");
			btnSkip.setFont(Font.font("Comic Sans MS", 14));
			btnSkip.setLayoutX(129);
			btnSkip.setLayoutY(256);
			btnSkip.setOnAction(e -> {
				turnIndex++;
				if (turnIndex >= orderTurn.size()) {
					turnIndex = 0;
				}
				startTurn();
			});
			centerPane.getChildren().addAll(lblErr, btnSkip);
			return;
		}
		player.getCards().add(chestCard);
		playerCardDAO
				.addPlayerCard(new PlayerCard(player.getIdPlayer(), chestCard.getIdCard(), actualGame.getIdGame()));

		centerPane.getChildren().clear();

		ImageView imgChest = new ImageView();
		String resourcePath = MessageFormat.format(CHEST_IMAGE, chestCard.getIdCard());
		URL resourceUrl = getClass().getClassLoader().getResource(resourcePath);
		if (resourceUrl != null) {
			imgChest.setImage(new Image(resourceUrl.toString()));
			imgChest.setFitWidth(311);
			imgChest.setFitHeight(207);
			imgChest.setLayoutX(33);
			imgChest.setLayoutY(29);
		}

		Button btnTerminarTurno = new Button("Ejecutar acción y terminar turno");
		btnTerminarTurno.setFont(Font.font("Comic Sans MS", 14));
		btnTerminarTurno.setLayoutX(129);
		btnTerminarTurno.setLayoutY(256);
		btnTerminarTurno.setOnAction(e -> {
			executeAction(chestCard.getAction(), player);
			turnIndex++;
			if (turnIndex >= orderTurn.size()) {
				turnIndex = 0;
			}
			startTurn();
		});

		centerPane.getChildren().addAll(imgChest, btnTerminarTurno);
	}

	/** @author Ana */
	public Card getRandomCard(CardType cardType) {
		Card card = null;
		int attempts = 0;
		while (card == null && attempts < 10) {
			int randomId;
			if (cardType == CardType.LUCK) {
				randomId = (int) (Math.random() * (NUM_CARD_FINISH_LUCK - NUM_CARD_INIT_LUCK + 1)) + NUM_CARD_INIT_LUCK;
			} else {
				randomId = (int) (Math.random() * (NUM_CARD_FINISH_CHEST - NUM_CARD_INIT_CHEST + 1))
						+ NUM_CARD_INIT_CHEST;
			}
			System.out.println("Buscando carta ID: " + randomId + " tipo: " + cardType);
			card = cardDAO.findCardById(randomId);
			if (card == null) {
				System.out.println("[WARN] Carta " + randomId + " no encontrada, reintentando...");
			}
			attempts++;
		}
		return card;
	}

}