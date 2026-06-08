package table.queries;

public class TableQueries {

	public static final String SQL_ACTION = "CREATE TABLE IF NOT EXISTS Action (" + "id_action INT PRIMARY KEY, "
			+ "action_type TEXT, " + "times INT);";

	public static final String SQL_CARD = "CREATE TABLE IF NOT EXISTS Card (" + "id_card INT PRIMARY KEY, "
			+ "type TEXT, " + "action_id INT, " + "FOREIGN KEY (action_id) REFERENCES Action(id_action));";

	public static final String SQL_GAME = "CREATE TABLE IF NOT EXISTS Game ("
			+ "id_game INTEGER PRIMARY KEY AUTOINCREMENT, " + "state TEXT, " + "duration TEXT, " + "name TEXT);";

	public static final String SQL_PROFILE = "CREATE TABLE IF NOT EXISTS Profile ("
			+ "id_profile INTEGER PRIMARY KEY AUTOINCREMENT, " + "nickname TEXT, " + "image TEXT);";

	public static final String SQL_BOARD = "CREATE TABLE IF NOT EXISTS Board (" + "id_board INT PRIMARY KEY, "
			+ "size INT);";

	public static final String SQL_CELL = "CREATE TABLE IF NOT EXISTS Cell (" + "id_cell INT PRIMARY KEY, "
			+ "type TEXT, " + "board_id INT, " + "card_id INT, " + "property_id INT, "
			+ "FOREIGN KEY (board_id) REFERENCES Board(id_board), " + "FOREIGN KEY (card_id) REFERENCES Card(id_card), "
			+ "FOREIGN KEY (property_id) REFERENCES Property(id_property));";

	public static final String SQL_PLAYER = "CREATE TABLE IF NOT EXISTS Player ("
			+ "id_player INTEGER PRIMARY KEY AUTOINCREMENT, " + "profile_id INT, " + "cell_id INT, " + "money INT, "
			+ "game_id INT, " + "is_bankrupt INT, " + "jail_turns_left INT, "
			+ "FOREIGN KEY (profile_id) REFERENCES Profile(id_profile), "
			+ "FOREIGN KEY (cell_id) REFERENCES Cell(id_cell), " + "FOREIGN KEY (game_id) REFERENCES Game(id_game));";

	// ← columna name añadida
	public static final String SQL_PROPERTY = "CREATE TABLE IF NOT EXISTS Property (" + "id_property INT PRIMARY KEY, "
			+ "name TEXT, " + "cell_id INT, " + "sell_value INT, " + "buy_value INT, " + "house_buy_value INT, "
			+ "hotel_buy_value INT, " + "rent_hotel_value INT, " + "rent_base_value INT, "
			+ "FOREIGN KEY (cell_id) REFERENCES Cell(id_cell));";

	public static final String SQL_PLAYER_CARD = "CREATE TABLE IF NOT EXISTS Player_Card ("
			+ "player_id INT, card_id INT, game_id INT, " + "PRIMARY KEY (player_id, card_id, game_id), "
			+ "FOREIGN KEY (player_id) REFERENCES Player(id_player), "
			+ "FOREIGN KEY (card_id) REFERENCES Card(id_card), " + "FOREIGN KEY (game_id) REFERENCES Game(id_game));";

	public static final String SQL_PLAYER_PROPERTY = "CREATE TABLE IF NOT EXISTS Player_Property ("
			+ "player_id INT, property_id INT, game_id INT, " + "PRIMARY KEY (player_id, property_id, game_id), "
			+ "FOREIGN KEY (player_id) REFERENCES Player(id_player), "
			+ "FOREIGN KEY (property_id) REFERENCES Property(id_property), "
			+ "FOREIGN KEY (game_id) REFERENCES Game(id_game));";

	public static final String SQL_RENT_HOUSE_VALUE = "CREATE TABLE IF NOT EXISTS Rent_House_Value ("
			+ "property_id INT, " + "house_count INT, " + "rent_value INT, "
			+ "PRIMARY KEY (property_id, house_count), "
			+ "FOREIGN KEY (property_id) REFERENCES Property(id_property));";

	public static final String SQL_TURN_ORDER = "CREATE TABLE IF NOT EXISTS Turn_Order (" + "game_id INTEGER NOT NULL, "
			+ "player_id INTEGER NOT NULL, " + "turn_order INTEGER NOT NULL, " + "PRIMARY KEY (game_id, player_id), "
			+ "FOREIGN KEY (game_id) REFERENCES Game(id_game), "
			+ "FOREIGN KEY (player_id) REFERENCES Player(id_player));";
}
