package dao;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import table.queries.TableInserts;
import table.queries.TableQueries;

/**
 * @author Víctor
 */
public class ManagerConnection {

	// Creamos la ruta donde se va a guardar la base de datos
	// en este caso la guardamos en carpeta de usuario/db
	private static String dbFolder = System.getProperty("user.dir") + "/db/MonopolyCartoonNetwork.db";

	// Crea la base de datos si NO existe
	private static Connection connexio = null;

	public static int connectar() {
		try {
			// Antes de crear la conexión, miramos si la carpeta existe
			File carpeta = new File(System.getProperty("user.dir") + "/db");

			// Si no existe, entonces creamos la carpeta
			if (!carpeta.exists()) {
				carpeta.mkdirs();
			}

			connexio = DriverManager.getConnection("jdbc:sqlite:" + dbFolder);
			createTables(connexio);
			migrateExistingDB(connexio);
			insertDefaultData(connexio);
			return 0;
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;
		}
	}

	public static boolean isConnected() throws SQLException {
		if (connexio == null || connexio.isClosed()) {
			return false;
		} else {
			return true;
		}
	}

	public static synchronized Connection obtenirConnexio() throws SQLException {
		if (connexio == null || connexio.isClosed()) {
			connexio = DriverManager.getConnection("jdbc:sqlite:" + dbFolder);
			try (Statement stmt = connexio.createStatement()) {
				stmt.execute("PRAGMA journal_mode=WAL;");
			}
		}
		return connexio;
	}

	public static void tancarConnexio() {
		try {
			if (connexio != null && !connexio.isClosed()) {
				connexio.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// Aquí irán los sql de la creación de las tablas, luego lo llamaremos en la
	// conexión y creará las tablas
	private static void createTables(Connection conn) {
		try {
			java.sql.Statement smt = conn.createStatement();
			smt.execute(TableQueries.SQL_ACTION);
			smt.execute(TableQueries.SQL_BOARD);
			smt.execute(TableQueries.SQL_CARD);
			smt.execute(TableQueries.SQL_CELL);
			smt.execute(TableQueries.SQL_GAME);
			smt.execute(TableQueries.SQL_PLAYER);
			smt.execute(TableQueries.SQL_PROFILE);
			smt.execute(TableQueries.SQL_PROPERTY);
			smt.execute(TableQueries.SQL_PLAYER_CARD);
			smt.execute(TableQueries.SQL_PLAYER_PROPERTY);
			smt.execute(TableQueries.SQL_RENT_HOUSE_VALUE);
			smt.execute(TableQueries.SQL_TURN_ORDER); // ← línea añadida
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void migrateExistingDB(Connection conn) {
		// Migración: añadir columna name a Game si no existe
		try (java.sql.Statement smt = conn.createStatement()) {
			smt.execute("ALTER TABLE Game ADD COLUMN name TEXT;");
			System.out.println("[MIGRATION] Columna 'name' añadida a Game.");
		} catch (Exception ignored) {
		}

		// Migración: añadir columna name a Property si no existe
		try (java.sql.Statement smt = conn.createStatement()) {
			smt.execute("ALTER TABLE Property ADD COLUMN name TEXT;");
			System.out.println("[MIGRATION] Columna 'name' añadida a Property.");
		} catch (Exception ignored) {
		}

		// Migración: insertar cartas de cofre si faltan (bug original: se comprobaba
		// COUNT dos veces)
		try (java.sql.Statement smt = conn.createStatement()) {
			java.sql.ResultSet rs = smt
					.executeQuery("SELECT COUNT(*) AS total FROM Card WHERE type = 'COMMUNITY_CHEST';");
			rs.next();
			if (rs.getInt("total") == 0) {
				java.sql.Statement ins = conn.createStatement();
				for (String sql : table.queries.TableInserts.INSERT_CARDS_COMMUNITY_CHEST) {
					ins.executeUpdate(sql);
				}
				System.out.println("[MIGRATION] Cartas de cofre insertadas.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Migración: rellenar nombres de propiedades si están vacíos
		try (java.sql.Statement smt = conn.createStatement()) {
			java.sql.ResultSet rs = smt
					.executeQuery("SELECT COUNT(*) AS total FROM Property WHERE name IS NOT NULL AND name != '';");
			rs.next();
			if (rs.getInt("total") == 0) {
				java.sql.Statement upd = conn.createStatement();
				for (String sql : table.queries.TableInserts.MIGRATE_PROPERTY_NAMES) {
					upd.executeUpdate(sql);
				}
				System.out.println("[MIGRATION] Nombres de propiedades actualizados.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void insertDefaultData(Connection conn) {
		try {
			Statement stmt = conn.createStatement();

			// Verificamos si la tabla ya tiene datos (para no insertar duplicados)
			ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM Action;");
			rs.next();
			if (rs.getInt("total") == 0) {
				for (String sql : TableInserts.INSERT_ACTIONS) {
					stmt.executeUpdate(sql);
				}
			}

			rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM Board;");
			rs.next();
			if (rs.getInt("total") == 0) {
				for (String sql : TableInserts.INSERT_BOARD) {
					stmt.executeUpdate(sql);
				}
			}

			rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM Card;");
			rs.next();
			if (rs.getInt("total") == 0) {
				for (String sql : TableInserts.INSERT_CARDS_LUCK) {
					stmt.executeUpdate(sql);
				}
			}

			rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM Card;");
			rs.next();
			if (rs.getInt("total") == 0) {
				for (String sql : TableInserts.INSERT_CARDS_COMMUNITY_CHEST) {
					stmt.executeUpdate(sql);
				}
			}

			rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM Cell;");
			rs.next();
			if (rs.getInt("total") == 0) {
				for (String sql : TableInserts.INSERT_CELLS) {
					stmt.executeUpdate(sql);
				}
			}

			rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM Profile;");
			rs.next();
			if (rs.getInt("total") == 0) {
				for (String sql : TableInserts.INSERT_PROFILES) {
					stmt.executeUpdate(sql);
				}
			}

			rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM Property;");
			rs.next();
			if (rs.getInt("total") == 0) {
				for (String sql : TableInserts.INSERT_PROPERTIES) {
					stmt.executeUpdate(sql);
				}
			}

			rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM Rent_House_Value;");
			rs.next();
			if (rs.getInt("total") == 0) {
				for (String sql : TableInserts.INSERT_RENT_HOUSE_VALUES) {
					stmt.executeUpdate(sql);
				}
			}

			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}