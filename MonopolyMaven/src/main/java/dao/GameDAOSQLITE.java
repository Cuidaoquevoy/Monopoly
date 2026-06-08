package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import models.Game;
import models.Game.State;
import models.Player;

/**
 * @author Ana
 */
public class GameDAOSQLITE implements GameDAO {

	@Override
	public int addGame(Game game) {
		String sql = "INSERT INTO Game(state, duration, name) VALUES (?, ?, ?)";
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;

		try {
			conn = ManagerConnection.obtenirConnexio();
			statement = conn.prepareStatement(sql, statement.RETURN_GENERATED_KEYS);
			statement.setString(1, game.getState().name());
			statement.setString(2, game.getDuration());
			statement.setString(3, game.getName());
			statement.executeUpdate();

			resultSet = statement.getGeneratedKeys();
			if (resultSet.next()) {
				int id = resultSet.getInt(1);
				return id;
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (resultSet != null) {
					resultSet.close();
				}
				if (statement != null) {
					statement.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return 0;
	}

	@Override
	public Game findGameById(int id) {
		String sql = "SELECT * FROM Game WHERE id_game = ?";
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;

		try {
			conn = ManagerConnection.obtenirConnexio();
			statement = conn.prepareStatement(sql);
			statement.setInt(1, id);
			resultSet = statement.executeQuery();
			if (resultSet.next()) {
				int gameId = resultSet.getInt("id_game");
				String typeString = resultSet.getString("state");
				String duration = resultSet.getString("duration");
				String name = resultSet.getString("name");
				Game game = new Game(gameId, State.valueOf(typeString), duration);
				game.setName(name);
				return game;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (resultSet != null) {
					resultSet.close();
				}
				if (statement != null) {
					statement.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@Override
	public void updateGame(Game game) {
		String sql = "UPDATE Game SET state = ?, duration = ?, name = ? WHERE id_game = ?";
		Connection conn = null;
		PreparedStatement statement = null;

		try {
			conn = ManagerConnection.obtenirConnexio();
			statement = conn.prepareStatement(sql);
			statement.setString(1, game.getState().name());
			statement.setString(2, game.getDuration());
			statement.setString(3, game.getName());
			statement.setInt(4, game.getIdGame());
			statement.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (statement != null) {
					statement.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void deleteGame(int id) {
		String sql = "DELETE FROM Game WHERE id_game = ?";
		Connection conn = null;
		PreparedStatement statement = null;

		try {
			conn = ManagerConnection.obtenirConnexio();
			statement = conn.prepareStatement(sql);
			statement.setInt(1, id);
			statement.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (statement != null) {
					statement.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public List<Game> getAll() {
		List<Game> games = new ArrayList<>();
		String sql = "SELECT * FROM Game"; // Cambié Card por Game
		Connection conn = null;
		Statement statement = null;
		ResultSet resultSet = null;

		try {
			conn = ManagerConnection.obtenirConnexio();
			statement = conn.createStatement();
			resultSet = statement.executeQuery(sql);

			while (resultSet.next()) {
				int gameId = resultSet.getInt("id_game");
				String stateString = resultSet.getString("state");
				String duration = resultSet.getString("duration");
				String name = resultSet.getString("name");

				Game game = new Game(gameId, State.valueOf(stateString), duration);
				game.setName(name);
				games.add(game);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (resultSet != null) {
					resultSet.close();
				}
				if (statement != null) {
					statement.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return games;
	}

	@Override
	public List<Player> loadPlayers(int gameId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Player> loadPlayers() {
		// TODO Auto-generated method stub
		return null;
	}

}