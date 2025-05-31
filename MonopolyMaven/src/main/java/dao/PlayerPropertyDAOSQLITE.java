package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import models.PlayerProperty;

/**
 * @author Ana
 */
public class PlayerPropertyDAOSQLITE implements PlayerPropertyDAO {

	@Override
	public void addPlayerProperty(PlayerProperty playerProperty) {
		String sql = "INSERT OR IGNORE INTO Player_Property(player_id, property_id, game_id) VALUES (?, ?, ?)";
		Connection conn = null;
		PreparedStatement statement = null;

		try {
			conn = ManagerConnection.obtenirConnexio();
			statement = conn.prepareStatement(sql);
			statement.setInt(1, playerProperty.getPlayerId());
			statement.setInt(2, playerProperty.getPropertyId());
			statement.setInt(3, playerProperty.getGameId());
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
	public void updatePlayerProperty(PlayerProperty playerProperty) {
		String sql = "UPDATE Player_Property SET property_id = ? WHERE player_id = ? AND game_id = ?";
		try (Connection conn = ManagerConnection.obtenirConnexio();
				PreparedStatement statement = conn.prepareStatement(sql)) {

			statement.setInt(1, playerProperty.getPropertyId());
			statement.setInt(2, playerProperty.getPlayerId());
			statement.setInt(3, playerProperty.getGameId());
			statement.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void deletePlayerProperty(int playerId, int propertyId, int gameId) {
		String sql = "DELETE FROM Player_Property WHERE player_id = ? AND property_id = ? AND game_id = ?";
		try (Connection conn = ManagerConnection.obtenirConnexio();
				PreparedStatement statement = conn.prepareStatement(sql)) {

			statement.setInt(1, playerId);
			statement.setInt(2, propertyId);
			statement.setInt(3, gameId);
			statement.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public List<PlayerProperty> findPlayerPropertiesByPlayerAndGame(int playerId, int gameId) {
		List<PlayerProperty> properties = new ArrayList<>();
		String sql = "SELECT * FROM Player_Property WHERE player_id = ? AND game_id = ?";
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;

		try {
			conn = ManagerConnection.obtenirConnexio();
			statement = conn.prepareStatement(sql);
			statement.setInt(1, playerId);
			statement.setInt(2, gameId);
			resultSet = statement.executeQuery();

			while (resultSet.next()) {
				int propertyId = resultSet.getInt("property_id");
				properties.add(new PlayerProperty(playerId, propertyId, gameId));
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

		return properties;
	}

	@Override
	public boolean isPropertyOwned(int propertyId, int gameId) {
		String sql = "SELECT COUNT(*) FROM Player_Property WHERE property_id = ? AND game_id = ?";
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;

		try {
			conn = ManagerConnection.obtenirConnexio();
			statement = conn.prepareStatement(sql);
			statement.setInt(1, propertyId);
			statement.setInt(2, gameId);
			resultSet = statement.executeQuery();

			if (resultSet.next()) {
				return resultSet.getInt(1) > 0;
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
		return false;
	}

	@Override
	public int getPropertyOwner(int propertyId, int gameId) {
		String sql = "SELECT player_id FROM Player_Property WHERE property_id = ? AND game_id = ?";
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;

		try {
			conn = ManagerConnection.obtenirConnexio();
			statement = conn.prepareStatement(sql);
			statement.setInt(1, propertyId);
			statement.setInt(2, gameId);
			resultSet = statement.executeQuery();

			if (resultSet.next()) {
				return resultSet.getInt("player_id");
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
		return -1;
	}

}
