package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import models.PlayerCard;

/**
 * @author Ana
 */
public class PlayerCardDAOSQLITE implements PlayerCardDAO {

	@Override
	public void addPlayerCard(PlayerCard playerCard) {
		String sql = "INSERT OR IGNORE INTO Player_Card(player_id, card_id, game_id) VALUES (?, ?, ?)";
		Connection conn = null;
		PreparedStatement statement = null;

		try {
			conn = ManagerConnection.obtenirConnexio();
			statement = conn.prepareStatement(sql);
			statement.setInt(1, playerCard.getPlayerId());
			statement.setInt(2, playerCard.getCardId());
			statement.setInt(3, playerCard.getGameId());
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
	public void updatePlayerCard(PlayerCard playerCard) {
		String sql = "UPDATE Player_Card SET card_id = ? WHERE player_id = ? AND game_id = ?";
		Connection conn = null;
		PreparedStatement statement = null;

		try {
			conn = ManagerConnection.obtenirConnexio();
			statement = conn.prepareStatement(sql);
			statement.setInt(1, playerCard.getCardId());
			statement.setInt(2, playerCard.getPlayerId());
			statement.setInt(3, playerCard.getGameId());
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
	public void deletePlayerCard(int playerId, int gameId, int cardId) {
		String sql = "DELETE FROM Player_Card WHERE player_id = ? and game_id = ? and card_id = ?";
		Connection conn = null;
		PreparedStatement statement = null;

		try {
			conn = ManagerConnection.obtenirConnexio();
			statement = conn.prepareStatement(sql);
			statement.setInt(1, playerId);
			statement.setInt(2, gameId);
			statement.setInt(3, cardId);
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
	public List<PlayerCard> findPlayerCardsByPlayerAndGame(int playerId, int gameId) {
		List<PlayerCard> cards = new ArrayList<>();
		String sql = "SELECT * FROM Player_Card WHERE player_id = ? AND game_id = ?";
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
				int cardId = resultSet.getInt("card_id");
				cards.add(new PlayerCard(playerId, cardId, gameId));
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

		return cards;
	}

}
