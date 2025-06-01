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

	/* Setters */

	/**
	 * @author Ana
	 */
	public void setSelectedToken(String path) {
		this.selectedTokens.add(path);
	}

	/**
	 * @author Ana
	 */
	public void setProfiles(List<Profile> selectedProfiles) {
		this.selectedProfiles = selectedProfiles;
		if (selectedProfiles == null || selectedProfiles.isEmpty()) {
			System.out.println("Error: Los perfiles no se han cargado en el GameController.");
			return;
		} else {
			initGame();
		}
	}

	/* Inits */
	/**
	 * @author Ana
	 */
	private void initGame() {
		if (selectedProfiles == null || selectedProfiles.isEmpty()) {
			System.out.println("Error: Los perfiles no se han cargado en el GameController.");
		} else {
			System.out.println("Perfiles cargados en el GameController: " + selectedProfiles.size());
			for (Profile profile : selectedProfiles) {
				System.out.println("Perfil: " + profile.getNickname());
			}
		}

		// Creamos el juego
		actualGame = new Game();
		actualGame.setDuration("60");
		actualGame.setState(State.IN_GAME);

		// Creamos el juego en la base de datos
		int idGame = gameDAO.addGame(actualGame);
		actualGame.setIdGame(idGame);

		// Creamos las celdas
		cells = cellDAO.getAll();

		// Cremos el tablero
		initBoard();

		// Iniciar selección de tokens uno a uno
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

	/**
	 * @author Ana
	 */
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

	/**
	 * @author Ana
	 */
	private void initPlayers() {
		for (int i = 0; i < selectedProfiles.size(); i++) {
			Player player = new Player();

			player.setProfile(selectedProfiles.get(i));

			for (Cell cell : cells) {
				if (cell.getType() == Cell.CellType.START) {
					player.setCell(cell);
					break;
				}
			}

			Cell startCell = cellDAO.findCellById(1);

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

			// Creamos el jugador en la base de datos
			int idPlayer = playerDAO.addPlayer(player);
			player.setIdPlayer(idPlayer);

			allPlayers.add(player);
			System.out.println("Jugador " + player.getProfile().getNickname() + " creado con éxito.");
		}
	}

	/**
	 * @author Ana
	 */
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
					// todos seleccionaron, ir al juego
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

	/**
	 * @author Ana
	 */
	private void iniciarFichasJugadores() {
		Cell celdaSalida = cellDAO.findCellById(1);
		System.out.println("Celda de salida: " + celdaSalida.getIdCell());
		Point2D coordenadas = null;

		for (Map.Entry<Point2D, Cell> entry : mapaCeldaPorCoordenada.entrySet()) {
			if (entry.getValue().getIdCell() == celdaSalida.getIdCell()) {
				coordenadas = entry.getKey();
				System.out.println("Coordenadas de la celda de salida: " + coordenadas);
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

	/* Juego */
	/**
	 * @author Ana
	 */
	public void startGame() {
		turnIndex = 0;
		iniciarFichasJugadores();
		startTurn();
	}

	/**
	 * @author Ana
	 */
	private void startTurn() {
		Player currentPlayer = orderTurn.get(turnIndex);
		mostrarPanelTurnoJugador(currentPlayer.getProfile());
	}

	/**
	 * @author Ana
	 */
	private void avanzarTurnoLanzamiento() {
		if (currentProfileIndex >= selectedProfiles.size()) {
			if (faseInicial) {
				System.out.println("Todos los jugadores han lanzado los dados para determinar el orden.");
				determinarOrdenTurno();
			} else {
				// Pasar al siguiente jugador
				currentProfileIndex = 0;
			}
			return;
		}

		Profile jugador = selectedProfiles.get(currentProfileIndex);
		lblPlayerName.setText(jugador.getNickname());
		imgPlayerPhoto.setImage(new Image(jugador.getImage()));
	}

	/**
	 * @author Ana
	 */
	private void determinarOrdenTurno() {
		List<Profile> ordenFinal = selectedProfiles.stream().filter(p -> tiradasPorJugador.containsKey(p))
				.sorted(Comparator.comparingInt(p -> tiradasPorJugador.get(p)).reversed()).collect(Collectors.toList());

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
		startGame();
	}

	/**
	 * @author Ana
	 */
	// TODO TOCAR ESTE MÉTODO CREO
	public void rollDice() {
		Random random = new Random();
		Thread thread = new Thread() {
			@Override
			public void run() {
				try {
					for (int i = 0; i < 15; i++) {
						// Generamos los números de forma aleatoria del 1 al 6
						int randomNum1 = random.nextInt(6) + 1;
						int randomNum2 = random.nextInt(6) + 1;

						// Generamos las rutas de los recursos (ya corregidas)
						String resourcePathFirst = MessageFormat.format(DICE_IMAGE, randomNum1);
						String resourcePathSecond = MessageFormat.format(DICE_IMAGE, randomNum2);

						// Cargar las imágenes desde el classpath
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

								// Llamamos al callback con el resultado final de los dados
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
	 * @author Ana
	 */
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
					Stage stage = (Stage) this.exitButton.getScene().getWindow();
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
				System.exit(0);
			}
		});
	}

	/* Turnos */
	/**
	 * @author Ana
	 */
	private void mostrarPanelOrdenDeTurno() {
		lblAction.setText("Lanza los dados para determinar el orden de turno. Jugador actual: "
				+ selectedProfiles.get(0).getNickname());
		lblAction.setPrefWidth(169);
		lblAction.setPrefHeight(Region.USE_COMPUTED_SIZE);
		lblAction.setWrapText(true);

		VBox vbox = new VBox(20);
		vbox.setAlignment(Pos.CENTER);

		// Jugador actual
		lblPlayerName.setText(selectedProfiles.get(0).getNickname());
		imgPlayerPhoto.setImage(new Image(selectedProfiles.get(0).getImage()));
		imgPlayerPhoto.setFitWidth(55);
		imgPlayerPhoto.setFitHeight(55);

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
		currentProfileIndex = 0;
		lanzarButton.setOnAction(e -> {
			lanzarButton.setDisable(true);

			// Obtener jugador actual
			Profile jugador = selectedProfiles.get(currentProfileIndex);

			// Definir el callback para cuando termine de lanzar
			resultadoCallback = resultado -> {
				int total = resultado.getDado1() + resultado.getDado2();

				if (faseInicial) {
					tiradasPorJugador.put(jugador, total);
					currentProfileIndex++;
					Platform.runLater(() -> {
						avanzarTurnoLanzamiento();
						lanzarButton.setDisable(false);
					});
				}
			};

			// Iniciar tirada
			rollDice();
		});

		centerPane.getChildren().clear();
		centerPane.getChildren().addAll(imageDiceFirst, imageDiceSecond, lanzarButton);

		avanzarTurnoLanzamiento();
	}

	/**
	 * @author Ana
	 */
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

			// Actualiza el texto por si el dinero cambió
			String labelText = String.format("Turno %d: %s - %d€", i + 1, player.getProfile().getNickname(),
					player.getMoney());
			label.setText(labelText);
		}
	}

	/**
	 * @author Ana
	 */
	private void mostrarPanelTurnoJugador(Profile perfilJugador) {
		lblPlayerName.setText(perfilJugador.getNickname());
		imgPlayerPhoto.setImage(new Image(perfilJugador.getImage()));
		imgPlayerPhoto.setFitWidth(55);
		imgPlayerPhoto.setFitHeight(55);

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

			resultadoCallback = resultado -> {
				int dado1 = resultado.getDado1();
				int dado2 = resultado.getDado2();
				int total = dado1 + dado2;

				Player actualPlayer = orderTurn.get(turnIndex);

				if (actualPlayer.getIsBankrupt()) {
					System.out
							.println("El jugador " + actualPlayer.getProfile().getNickname() + " está en bancarrota.");
					turnIndex++;
					if (turnIndex >= orderTurn.size()) {
						turnIndex = 0;
					}
					resultadoCallback = null;
					return;
				}

				moverFichaJugador(actualPlayer, total);
				ejecutarMovimiento(actualPlayer, dado1, dado2);
				resultadoCallback = null;
			};

			rollDice();
		});
		centerPane.getChildren().clear();
		centerPane.getChildren().addAll(imageDiceFirst, imageDiceSecond, lanzarButton);
	}

	/**
	 * @author Ana
	 */
	private void mostrarMensajeJuegoTerminado() {
		Label lblInfo = new Label();
		lblInfo.setText("El juego ha terminado");
		lblInfo.setLayoutX(28);
		lblInfo.setLayoutY(102);
		lblInfo.setMaxWidth(273);
		lblInfo.setWrapText(true);
		lblInfo.setFont(Font.font("Comic Sans MS", 14));
	}

	/* Movimiento */
	/**
	 * @author Ana
	 */
	public void moverFichaJugador(Player actualPlayer, int total) {
		ImageView imgFicha = actualPlayer.getImgToken();
		Cell actualCell = actualPlayer.getCell();
		int actualCellNumber = actualCell.getIdCell();
		int nextCellNumber = (actualCellNumber + total) % TOTAL_NUM_CELLS;
		Cell newCell = cellDAO.findCellById(nextCellNumber);
		actualPlayer.setCell(newCell); // MUY IMPORTANTE: actualizar la celda del jugador

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

	/**
	 * @author Ana
	 */
	public void ejecutarMovimiento(Player actualPlayer, int dado1, int dado2) {
		if (isGameFinished()) {
			System.out.println("El juego ha terminado. No se puede ejecutar el movimiento.");
			mostrarMensajeJuegoTerminado();
		}

		actualizarResaltadoJugador(actualPlayer);

		System.out.println("Turno del jugador " + actualPlayer.getProfile().getNickname());
		// Verificamos si el jugador está en la cárcel
		if (actualPlayer.getJailTurnsLeft() != 0) {
			lblAction.setText(
					"El jugador " + actualPlayer.getProfile().getNickname() + " está en la cárcel y no puede moverse.");
			lblAction.setPrefWidth(169);
			lblAction.setPrefHeight(Region.USE_COMPUTED_SIZE);
			lblAction.setWrapText(true);
			if (dado1 == dado2) {
				// Si el jugador ha sacado dobles, lanza el dado de nuevo
				actualPlayer.setJailTurnsLeft(0);
				System.out.println("El jugador " + actualPlayer.getProfile().getNickname()
						+ " ha sacado dobles y sale de la cárcel.");

			} else {
				System.out.println("El jugador " + actualPlayer.getProfile().getNickname()
						+ " no ha sacado dobles y sigue en la cárcel.");
				actualPlayer.setJailTurnsLeft(actualPlayer.getJailTurnsLeft() - 1);
				turnIndex++;
				if (turnIndex >= orderTurn.size()) {
					turnIndex = 0;
				}
				startTurn();
			}
			return;
		} else {
			// Si el jugador no está en la cárcel, lanza el dado
			System.out.println(
					"El jugador " + actualPlayer.getProfile().getNickname() + " ha sacado " + dado1 + " y " + dado2);
			if (dado1 == dado2) {
				// Si el jugador ha sacado dobles, lanza el dado de nuevo
				System.out.println("El jugador " + actualPlayer.getProfile().getNickname()
						+ " ha sacado dobles y lanza de nuevo.");
				mostrarPanelTurnoJugador(actualPlayer.getProfile());
			}
			// Avanzamos la ficha
			Cell actualCell = actualPlayer.getCell();
			int actualCellNumber = actualCell.getIdCell();
			System.out.println("Celda actual: " + actualCellNumber + ", dados: " + dado1 + " + " + dado2);
			int nextCellNumber = (actualCellNumber + dado1 + dado2) % TOTAL_NUM_CELLS;
			System.out.println("Moviendo a la celda con ID: " + nextCellNumber);
			Cell newCell = cellDAO.findCellById(nextCellNumber);
			actualPlayer.setCell(newCell);
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
				System.out.println("El jugador ha caído en el parking. Turno sin acción.");
				break;
			default:
				break;
			}

			// mirar si el jugador ha ganado o no
			if (isGameFinished()) {
				System.out.println("El juego ha terminado. No se puede continuar el turno.");
				mostrarMensajeJuegoTerminado();
				return;
			}

			// Actualizamos el estado del jugador
			playerDAO.updatePlayer(actualPlayer);

			actualizarResaltadoJugador(actualPlayer);
		}
	}

	/* Propiedades */
	/**
	 * @author Ana
	 */
	public void handlePropertyCell(Cell cell, Player player) {
		lblAction.setText("Has caído en una celda de propiedad.");
		lblAction.setPrefWidth(169);
		lblAction.setPrefHeight(Region.USE_COMPUTED_SIZE);
		lblAction.setWrapText(true);

		System.out.println("Manejando celda de propiedad...");
		Property property = cell.getProperty();
		if (property != null) {
			System.out.println("Partida encontrada: " + actualGame.getIdGame());
			boolean hasOwner = playerPropertyDAO.isPropertyOwned(property.getIdProperty(), actualGame.getIdGame());
			int ownerId = playerPropertyDAO.getPropertyOwner(property.getIdProperty(), actualGame.getIdGame());
			Player owner = playerDAO.findPlayerById(ownerId);
			System.out.println("Propiedad ID: " + property.getIdProperty() + ", Tiene dueño: " + hasOwner
					+ ", Dueño ID: " + ownerId);
			if (!hasOwner) {
				handleComprarPropiedad(property, player);
			} else if ((hasOwner && owner != null) && owner.getIdPlayer() != player.getIdPlayer()) {
				handleCobrarAlquiler(property, owner, player);
			} else {
				handleUpdateProperty(property, player);
			}

		} else {
			System.out.println("No hay propiedad en esta celda.");
		}
	}

	/**
	 * @author Ana
	 */
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

		Button btnBuy = new Button();
		btnBuy.setText("Comprar propiedad");
		btnBuy.setFont(Font.font("Comic Sans MS", 14));
		btnBuy.setLayoutX(34);
		btnBuy.setLayoutY(320);
		btnBuy.setOnAction(e -> {
			comprarPropiedad(property, player);
		});

		Button btnCancel = new Button();
		btnCancel.setText("Terminar turno");
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

	/**
	 * @author Ana
	 */
	public void handleCobrarAlquiler(Property property, Player owner, Player player) {
		centerPane.getChildren().clear();

		int alquiler = cobrarAlquiler(property, owner, player);

		Label lblInfo = new Label();
		lblInfo.setText("Tienes que pagarle " + alquiler + " al propietario " + owner.getProfile().getNickname());
		lblInfo.setLayoutX(35);
		lblInfo.setLayoutY(63);
		lblInfo.setMaxWidth(317);
		lblInfo.setWrapText(true);
		lblInfo.setFont(Font.font("Comic Sans MS", 14));

		Button btnTerminarTurno = new Button();
		btnTerminarTurno.setText("Terminar turno");
		btnTerminarTurno.setFont(Font.font("Comic Sans MS", 14));
		btnTerminarTurno.setLayoutX(129);
		btnTerminarTurno.setLayoutY(256);
		btnTerminarTurno.layoutBoundsProperty().addListener((obs, oldVal, newVal) -> {
			double anchoBoton = newVal.getWidth();
			double altoBoton = newVal.getHeight();

			btnTerminarTurno.setLayoutX((centerPane.getWidth() - anchoBoton) / 2);
			btnTerminarTurno.setLayoutY((centerPane.getHeight() - altoBoton) / 2);
		});
		btnTerminarTurno.setOnAction(e -> {
			turnIndex++;
			if (turnIndex >= orderTurn.size()) {
				turnIndex = 0;
			}
			startTurn();
		});
		centerPane.getChildren().addAll(lblInfo, btnTerminarTurno);
	}

	/**
	 * @author Ana
	 */
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

		Label lblEpisodios = new Label();
		lblEpisodios.setText("Número de episodios:");
		lblEpisodios.setPrefWidth(117);
		lblEpisodios.setPrefHeight(17);
		lblEpisodios.setLayoutX(216);
		lblEpisodios.setLayoutY(6);
		lblEpisodios.setFont(Font.font("Comic Sans MS", 14));

		HBox hbEpisodios = new HBox();
		hbEpisodios.setSpacing(5);
		hbEpisodios.setLayoutX(216);
		hbEpisodios.setLayoutY(29);
		hbEpisodios.setPrefWidth(122);
		hbEpisodios.setPrefHeight(31);
		hbEpisodios.getChildren().clear();
		for (int i = 0; i < property.getHouseNumber(); i++) {
			ImageView houseView = new ImageView(new Image("images/episode.png"));
			houseView.setFitWidth(30);
			houseView.setFitHeight(30);
			hbEpisodios.getChildren().add(houseView);
		}

		Label lblTemporadas = new Label();
		lblTemporadas.setText("Número de episodios:");
		lblTemporadas.setPrefWidth(125);
		lblTemporadas.setPrefHeight(17);
		lblTemporadas.setLayoutX(216);
		lblTemporadas.setLayoutY(65);
		lblTemporadas.setFont(Font.font("Comic Sans MS", 14));

		HBox hbTemporada = new HBox();
		hbTemporada.setSpacing(5);
		hbTemporada.setLayoutX(216);
		hbTemporada.setLayoutY(90);
		hbTemporada.setPrefWidth(122);
		hbTemporada.setPrefHeight(31);
		hbTemporada.getChildren().clear();
		for (int i = 0; i < property.getHotelNumber(); i++) {
			ImageView hotelView = new ImageView(new Image("images/season.png"));
			hotelView.setFitWidth(24);
			hotelView.setFitHeight(24);
			hbTemporada.getChildren().add(hotelView);
		}

		Button btnComprarCasa = new Button();
		btnComprarCasa.setText("Comprar episodio");
		btnComprarCasa.setFont(Font.font("Comic Sans MS", 14));
		btnComprarCasa.setLayoutX(216);
		btnComprarCasa.setLayoutY(129);
		btnComprarCasa.setOnAction(e -> {
			comprarCasa(property, actualPlayer);
		});

		Button btnComprarHotel = new Button();
		btnComprarHotel.setText("Comprar temporada");
		btnComprarCasa.setFont(Font.font("Comic Sans MS", 14));
		btnComprarHotel.setLayoutX(216);
		btnComprarHotel.setLayoutY(215);
		btnComprarHotel.setOnAction(e -> {
			comprarHotel(property, actualPlayer);
		});

		Button btnVenderCasa = new Button();
		btnVenderCasa.setText("Vender episodio");
		btnComprarCasa.setFont(Font.font("Comic Sans MS", 14));
		btnVenderCasa.setLayoutX(216);
		btnVenderCasa.setLayoutY(171);
		btnVenderCasa.setOnAction(e -> {
			venderCasa(property, actualPlayer);
		});

		Button btnVenderHotel = new Button();
		btnVenderHotel.setText("Vender temporada");
		btnComprarCasa.setFont(Font.font("Comic Sans MS", 14));
		btnVenderHotel.setLayoutX(216);
		btnVenderHotel.setLayoutY(262);
		btnVenderHotel.setOnAction(e -> {
			venderHotel(property, actualPlayer);
		});

		Button btnTerminarTurno = new Button();
		btnTerminarTurno.setText("Terminar turno");
		btnComprarCasa.setFont(Font.font("Comic Sans MS", 14));
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
				btnComprarCasa, btnComprarHotel, btnVenderCasa, btnVenderHotel, btnTerminarTurno);
	}

	/**
	 * @author Ana
	 */
	public void comprarPropiedad(Property property, Player player) {
		System.out.println("Comprando propiedad...");
		boolean hasOwner = playerPropertyDAO.isPropertyOwned(property.getIdProperty(), actualGame.getIdGame());
		if (!hasOwner) {
			int propertyValue = property.getBuyValue();
			int actualMoney = player.getMoney();
			if (checkIfPlayerCanPurchase(actualMoney, propertyValue)) {
				int substractedMoney = actualMoney - propertyValue;
				player.setMoney(substractedMoney);
				List<Property> properties = player.getProperties();
				properties.add(property);
				player.setProperties(properties);
				System.out.println(
						"Propiedad comprada: " + property.getIdProperty() + " por el jugador: " + player.getIdPlayer());
				playerPropertyDAO.addPlayerProperty(
						new PlayerProperty(player.getIdPlayer(), property.getIdProperty(), actualGame.getIdGame()));
			} else {
				player.setBankrupt(true);
			}
		} else {
			System.out.println("La propiedad ya tiene dueño");
		}
	}

	/**
	 * @author Ana
	 */
	public void venderPropiedad(Property property, Player seller, Player buyer) {
		System.out.println("Vendiendo propiedad...");

		// Verificar que el vendedor es el dueño
		boolean esDueno = playerPropertyDAO.getPropertyOwner(property.getIdProperty(), actualGame.getIdGame()) == seller
				.getIdPlayer();
		if (!esDueno) {
			System.out.println("Error: el vendedor no es dueño de esta propiedad.");
			return;
		}

		int valorVenta = property.getSellValue();
		int dineroComprador = buyer.getMoney();

		if (dineroComprador < valorVenta) {
			System.out.println("El comprador no tiene suficiente dinero para la compra.");
			buyer.setBankrupt(true);
			return;
		}

		// Realizar la transacción económica
		buyer.setMoney(dineroComprador - valorVenta);
		seller.setMoney(seller.getMoney() + valorVenta);

		// Actualizar propiedad del comprador y eliminarla del vendedor
		playerPropertyDAO.deletePlayerProperty(property.getIdProperty(), seller.getIdPlayer(), actualGame.getIdGame());
		playerPropertyDAO.addPlayerProperty(
				new PlayerProperty(buyer.getIdPlayer(), property.getIdProperty(), actualGame.getIdGame()));

		// Actualizar listas en memoria
		seller.getProperties().remove(property);
		buyer.getProperties().add(property);

		System.out.println("Propiedad vendida correctamente de " + seller.getProfile().getNickname() + " a "
				+ buyer.getProfile().getNickname() + " por " + valorVenta + "€.");
	}

	/**
	 * @author Ana
	 */
	public void comprarCasa(Property property, Player player) {
		lblAction.setText("Has caído en una celda de propiedad. Puedes comprarla");
		lblAction.setPrefWidth(169);
		lblAction.setPrefHeight(Region.USE_COMPUTED_SIZE);
		lblAction.setWrapText(true);

		if (property.getHouseNumber() >= 3 && property.getHotelNumber() <= 1) {
			System.out.println("Comprando casa...");
			int houseValue = property.getHouseBuyValue();
			int actualMoney = player.getMoney();
			if (checkIfPlayerCanPurchase(actualMoney, houseValue)) {
				int substractedMoney = actualMoney - houseValue;
				player.setMoney(substractedMoney);
				int houseNumber = property.getHouseNumber();
				property.setHouseNumber(houseNumber + 1);
			} else {
				player.setBankrupt(true);
			}
		} else {
			System.out.println("Error, no tiene suficientes casas o ya tiene un hotel comprado");
		}
	}

	/**
	 * @author Ana
	 */
	public void comprarHotel(Property property, Player player) {
		lblAction.setText("Has caído en una celda de tu propiedad. Puedes comprar un hotel si tienes 3 casas.");
		lblAction.setPrefWidth(169);
		lblAction.setPrefHeight(Region.USE_COMPUTED_SIZE);
		lblAction.setWrapText(true);

		if (property.getHouseNumber() == 2 || property.getHotelNumber() < 1) {
			System.out.println("Comprando hotel...");
			int hotelValue = property.getHotelBuyValue();
			int actualMoney = player.getMoney();
			if (checkIfPlayerCanPurchase(actualMoney, hotelValue)) {
				int substractedMoney = actualMoney - hotelValue;
				player.setMoney(substractedMoney);
				property.setHotelNumber(1);
			} else {
				player.setBankrupt(true);
			}
		} else {
			System.out.println("Error, no tiene suficientes casas o ya tiene un hotel comprado");
		}
	}

	/**
	 * 
	 * @author Ana
	 */
	public void venderCasa(Property property, Player player) {
		lblAction.setText("Has caído en una celda de tu propiedad. Puedes vender una casa si tienes al menos 1 casa.");
		lblAction.setPrefWidth(169);
		lblAction.setPrefHeight(Region.USE_COMPUTED_SIZE);
		lblAction.setWrapText(true);

		if ((property.getHouseNumber() >= 1 && property.getHouseNumber() <= 3) && property.getHotelNumber() == 0) {
			System.out.println("Vendiendo casa...");
			int houseValue = property.getHouseBuyValue();
			int actualMoney = player.getMoney();
			if (!player.getIsBankrupt()) {
				int addedMoney = actualMoney + houseValue;
				player.setMoney(addedMoney);
				int houseNumber = property.getHouseNumber();
				property.setHouseNumber(houseNumber - 1);
			} else {
				System.out.println("Estás en bancarrota, ya no puedes jugar");
			}
		} else {
			System.out.println(
					"Error, no puedes vender la casa porque no tienes al menos una casa o tienes un hotel comprado");
		}
	}

	/**
	 * 
	 * @author Ana
	 */
	public void venderHotel(Property property, Player player) {
		lblAction.setText("Has caído en una celda de tu propiedad. Puedes vender un hotel si tienes 1 hotel.");
		lblAction.setPrefWidth(169);
		lblAction.setPrefHeight(Region.USE_COMPUTED_SIZE);
		lblAction.setWrapText(true);

		if (property.getHotelNumber() == 1) {
			System.out.println("Vendiendo hotel...");
			int hotelValue = property.getHotelBuyValue();
			int actualMoney = player.getMoney();
			if (!player.getIsBankrupt()) {
				int addedMoney = actualMoney + hotelValue;
				player.setMoney(addedMoney);
				property.setHotelNumber(0);
			} else {
				System.out.println("Estás en bancarrota, ya no puedes jugar");
			}
		} else {
			System.out.println("Error, no puedes vender el hotel porque no tienes uno");
		}
	}

	/**
	 * @author Ana
	 */
	public int cobrarAlquiler(Property property, Player owner, Player renter) {
	    if (owner == null || renter == null || property == null) {
	        System.err.println("Invalid parameters: owner=" + owner + ", renter=" + renter + ", property=" + property);
	        lblAction.setText("Error: Datos no válidos.");
	        return 0;
	    }

	    String ownerNickname = (owner.getProfile() != null) ? owner.getProfile().getNickname() : "Desconocido";
	    
	    lblAction.setText("Has caído en una celda de propiedad. El dueño es: " + ownerNickname
	            + ". Se cobrará el alquiler correspondiente.");
	    lblAction.setPrefWidth(169);
	    lblAction.setPrefHeight(Region.USE_COMPUTED_SIZE);
	    lblAction.setWrapText(true);

	    System.out.println("Cobrando alquiler...");
	    int rent = 0;
	    if (property.getHouseNumber() == 0 && property.getHotelNumber() == 0) {
	        rent = property.getRentBaseValue();
	    } else if (property.getHouseNumber() > 0) {
	        List<RentHouseValue> rents = property.getRentHouseValue();
	        if (rents != null && property.getHouseNumber() <= rents.size()) {
	            rent = rents.get(property.getHouseNumber() - 1).getRentValue();
	        } else {
	            System.err.println("Invalid rent data for property: " + property);
	        }
	    } else if (property.getHotelNumber() == 1) {
	        rent = property.getRentHotelValue();
	    }

	    if (checkIfPlayerCanPurchase(renter.getMoney(), rent)) {
	        int substractedMoney = renter.getMoney() - rent;
	        renter.setMoney(substractedMoney);
	        int addedMoney = owner.getMoney() + rent;
	        owner.setMoney(addedMoney);
	    } else {
	        renter.setBankrupt(true);
	        String renterNickname = (renter.getProfile() != null) ? renter.getProfile().getNickname() : "Desconocido";
	        System.out.println("El jugador " + renterNickname + " está en bancarrota.");
	    }
	    return rent;
	}

	/**
	 * @author Ana
	 */
	public Boolean checkIfPlayerCanPurchase(int actualMoney, int quantity) {
		int substractedMoney = actualMoney - quantity;
		if (actualMoney == 0) {
			return false;
		} else if (substractedMoney < 0) {
			return false;
		} else {
			return true;
		}
	}

	/* Acciones en celdas */
	/**
	 * @author Ana
	 */
	public void executeAction(Action action, Player player) {
		String type = action.getActionType().name();
		int value = action.getTimes();

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
			if (actualPlayer == null) {
		        System.err.println("Error: actualPlayer is null in executeAction");
		        lblAction.setText("Error: No hay jugador actual.");
		        return;
		    }
			Cell actualCell = actualPlayer.getCell();
			int actualCellNumber = actualCell.getIdCell();
			int nextCellNumber = (actualCellNumber - value) % TOTAL_NUM_CELLS;
			Cell newCell = cellDAO.findCellById(nextCellNumber);
			player.setCell(newCell);
			break;
		case "MOVE_CELLS":
			if (actualPlayer == null) {
		        System.err.println("Error: actualPlayer is null in executeAction");
		        lblAction.setText("Error: No hay jugador actual.");
		        return;
		    }
			actualCell = actualPlayer.getCell();
			actualCellNumber = actualCell.getIdCell();
			nextCellNumber = (actualCellNumber + value) % TOTAL_NUM_CELLS;
			newCell = cellDAO.findCellById(nextCellNumber);
			player.setCell(newCell);
			break;
		case "SUM_CELL":
			if (actualPlayer == null) {
		        System.err.println("Error: actualPlayer is null in executeAction");
		        lblAction.setText("Error: No hay jugador actual.");
		        return;
		    }
			actualCell = actualPlayer.getCell();
			actualCellNumber = actualCell.getIdCell();
			nextCellNumber = (actualCellNumber + value) % TOTAL_NUM_CELLS;
			newCell = cellDAO.findCellById(nextCellNumber);
			player.setCell(newCell);
			break;
		case "GO_EXIT":
			newCell = cellDAO.findCellById(1);
			player.setCell(newCell);
			break;
		default:
			System.out.println("Acción desconocida: " + type);
			break;
		}
	}

	/**
	 * @author Ana
	 */
	public void handleJailCell(Player player) {
		lblAction.setText("Has caído en una celda de cárcel.");
		lblAction.setPrefWidth(169);
		lblAction.setPrefHeight(Region.USE_COMPUTED_SIZE);
		lblAction.setWrapText(true);

		System.out.println("Manejando celda de cárcel...");
		player.setJailTurnsLeft(3);

		centerPane.getChildren().clear();

		Label lblInfo = new Label();
		lblInfo.setText("¡Oh no, has caído en la cárcel!");
		lblInfo.setLayoutX(85);
		lblInfo.setLayoutY(49);
		lblInfo.setMaxWidth(215);
		lblInfo.setFont(Font.font("Comic Sans MS", 14));

		ImageView imgCarcel = new ImageView();
		imgCarcel.setImage(new Image("/images/cells/jail.jpg"));
		imgCarcel.setFitWidth(110);
		imgCarcel.setFitHeight(110);
		imgCarcel.setLayoutX(133);
		imgCarcel.setLayoutY(107);

		Button btnTerminarTurno = new Button();
		btnTerminarTurno.setText("Terminar turno");
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

	/**
	 * @author Ana
	 */
	public void handleStartCell(Cell cell, Player player) {
		lblAction.setText("Has caído en la celda de salida.");
		lblAction.setPrefWidth(169);
		lblAction.setPrefHeight(Region.USE_COMPUTED_SIZE);
		lblAction.setWrapText(true);

		System.out.println("Manejando celda de salida...");
		int startMoney = 200;
		int actualMoney = player.getMoney();
		int addedMoney = actualMoney + startMoney;
		player.setMoney(addedMoney);

		centerPane.getChildren().clear();

		Label lblInfo = new Label();
		lblInfo.setText("Has pasado por la salida y has recibido " + startMoney + " dólares.");
		lblInfo.setLayoutX(85);
		lblInfo.setLayoutY(49);
		lblInfo.setMaxWidth(215);
		lblInfo.setWrapText(true);
		lblInfo.setFont(Font.font("Comic Sans MS", 14));

		Button btnTerminarTurno = new Button();
		btnTerminarTurno.setText("Terminar turno");
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

	/**
	 * @author Ana
	 */
	public void handleTaxCell(Cell cell, Player player) {
		lblAction.setText("Has caído en una celda de impuestos.");
		lblAction.setPrefWidth(169);
		lblAction.setPrefHeight(Double.MAX_VALUE / 5);
		lblAction.setWrapText(true);

		System.out.println("Manejando celda de impuestos...");
		int taxAmount = 200;
		int actualMoney = player.getMoney();
		if (checkIfPlayerCanPurchase(actualMoney, taxAmount)) {
			int substractedMoney = actualMoney - taxAmount;
			player.setMoney(substractedMoney);
		} else {
			player.setBankrupt(true);
		}

		centerPane.getChildren().clear();

		Label lblInfo = new Label();
		lblInfo.setText("Has caído en la celda de impuestos y has pagado " + taxAmount
				+ " dólares para poder hacer el rodaje de tu serie favorita.");
		lblInfo.setLayoutX(35);
		lblInfo.setLayoutY(63);
		lblInfo.setMaxWidth(317);
		lblInfo.setWrapText(true);
		lblInfo.setFont(Font.font("Comic Sans MS", 12));

		Button btnTerminarTurno = new Button();
		btnTerminarTurno.setText("Terminar turno");
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

	/**
	 * @author Ana
	 */
	public void handleLuckCell(Cell cell, Player player) {
		lblAction.setText("Has caído en una celda de carta de suerte.");
		lblAction.setPrefWidth(169);
		lblAction.setPrefHeight(Region.USE_COMPUTED_SIZE);
		lblAction.setWrapText(true);

		System.out.println("Manejando celda de suerte...");
		Card luckyCard = getRandomCard(CardType.LUCK);
		System.out.println("Carta de suerte obtenida: " + luckyCard.getIdCard());
		List<Card> cards = player.getCards();
		cards.add(luckyCard);
		player.setCards(cards);
		playerCardDAO
				.addPlayerCard(new PlayerCard(player.getIdPlayer(), luckyCard.getIdCard(), actualGame.getIdGame()));

		centerPane.getChildren().clear();

		ImageView imgLuck = new ImageView();
		String resourcePath = MessageFormat.format(LUCK_IMAGE, luckyCard.getIdCard());
		URL resourceUrl = getClass().getClassLoader().getResource(resourcePath);
		System.out.println("Buscando recurso en: " + resourcePath);
		System.out.println("URL recurso: " + resourceUrl);
		if (resourceUrl != null) {
			imgLuck.setImage(new Image(resourceUrl.toString()));
			imgLuck.setFitWidth(311);
			imgLuck.setFitHeight(207);
			imgLuck.setLayoutX(33);
			imgLuck.setLayoutY(29);
		}

		Button btnTerminarTurno = new Button();
		btnTerminarTurno.setText("Ejecutar acción y terminar turno");
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

	/**
	 * @author Ana
	 */
	public void handleCommunityChestCell(Cell cell, Player player) {
		lblAction.setText("Has caído en una celda de cofre de comunidad.");
		lblAction.setPrefWidth(169);
		lblAction.setPrefHeight(Region.USE_COMPUTED_SIZE);
		lblAction.setWrapText(true);

		System.out.println("Manejando celda de cofre comunitario...");
		Card chestCard = getRandomCard(CardType.COMMUNITY_CHEST);
		System.out.println("Carta de cofre obtenida: " + chestCard.getIdCard());
		List<Card> cards = player.getCards();
		cards.add(chestCard);
		player.setCards(cards);
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

		Button btnTerminarTurno = new Button();
		btnTerminarTurno.setText("Ejecutar acción y terminar turno");
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

	/**
	 * @author Ana
	 */
	public Card getRandomCard(CardType cardType) {
		Card card = new Card();
		if (cardType == CardType.LUCK) {
			int randomId = (int) (Math.random() * (NUM_CARD_FINISH_LUCK - NUM_CARD_INIT_LUCK + 1)) + NUM_CARD_INIT_LUCK;
			System.out.println("Random ID for luck card: " + randomId);
			card = cardDAO.findCardById(randomId);
		} else if (cardType == CardType.COMMUNITY_CHEST) {
			int randomId = (int) (Math.random() * (NUM_CARD_FINISH_CHEST - NUM_CARD_INIT_CHEST + 1))
					+ NUM_CARD_INIT_CHEST;
			System.out.println("Random ID for chest card: " + randomId);
			card = cardDAO.findCardById(randomId);
		}
		return card;
	}

}
