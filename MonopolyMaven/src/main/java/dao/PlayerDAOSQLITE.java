package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import models.Card;
import models.Cell;
import models.Game;
import models.Player;
import models.PlayerCard;
import models.PlayerProperty;
import models.Profile;
import models.Property;

/**
 * @author Ana
 */
public class PlayerDAOSQLITE implements PlayerDAO {

	@Override
	public int addPlayer(Player player) {
		String sql = "INSERT INTO Player(profile_id, cell_id, money, game_id, is_bankrupt, jail_turns_left) VALUES (?, ?, ?, ?, ?, ?)";
		Connection conn = null;
		PreparedStatement statement = null;

		try {
			conn = ManagerConnection.obtenirConnexio();
			statement = conn.prepareStatement(sql, statement.RETURN_GENERATED_KEYS);
			statement.setInt(1, player.getProfile().getIdProfile());
			statement.setInt(2, player.getCell().getIdCell());
			statement.setInt(3, player.getMoney());
			statement.setInt(4, player.getGame().getIdGame());
			statement.setInt(5, player.getIsBankrupt() ? 1 : 0);
			statement.setInt(6, player.getJailTurnsLeft());
			statement.executeUpdate();

			ResultSet resultSet = statement.getGeneratedKeys();
			if (resultSet.next()) {
				int id = resultSet.getInt(1);
				player.setIdPlayer(id); // 🧠 IMPORTANTE: actualizas el ID en el objeto

				// 🔁 Ahora sí puedes añadir cartas y propiedades
				DAOManager daoManager = new DAOManager();
				PlayerCardDAO cardDAO = daoManager.getPlayerCardDAO();
				PlayerPropertyDAO propertyDAO = daoManager.getPlayerPropertyDAO();

				for (Card card : player.getCards()) {
					cardDAO.addPlayerCard(new PlayerCard(id, card.getIdCard(), player.getGame().getIdGame()));
				}

				for (Property property : player.getProperties()) {
					propertyDAO.addPlayerProperty(
							new PlayerProperty(id, property.getIdProperty(), player.getGame().getIdGame()));
				}

				return id;
			}

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
		return 0;
	}

	@Override
	public Player findPlayerById(int id) {
		String sql = "SELECT * FROM Player WHERE id_player = ?";
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;

		try {
			conn = ManagerConnection.obtenirConnexio();
			statement = conn.prepareStatement(sql);
			statement.setInt(1, id);
			resultSet = statement.executeQuery();

			if (resultSet.next()) {
				int profileId = resultSet.getInt("profile_id");
				int cellId = resultSet.getInt("cell_id");
				int money = resultSet.getInt("money");
				int gameId = resultSet.getInt("game_id");
				boolean isBankrupt = resultSet.getInt("is_bankrupt") == 1;
				int jailTurns = resultSet.getInt("jail_turns_left");

				DAOManager daoManager = new DAOManager();
				ProfileDAO profileDAO = daoManager.getProfileDAO();
				CellDAO cellDAO = daoManager.getCellDAO();
				GameDAO gameDAO = daoManager.getGameDAO();
				CardDAO cardDAO = daoManager.getCardDAO();
				PlayerCardDAO playerCardDAO = daoManager.getPlayerCardDAO();
				PropertyDAO propertyDAO = daoManager.getPropertyDAO();
				PlayerPropertyDAO playerPropertyDAO = daoManager.getPlayerPropertyDAO();

				Profile profile = profileDAO.findProfileById(profileId);
				Cell cell = cellDAO.findCellById(cellId);
				Game game = gameDAO.findGameById(gameId);

				// Obtener cartas
				List<Card> cards = new ArrayList<>();
				for (PlayerCard pc : playerCardDAO.findPlayerCardsByPlayerAndGame(id, gameId)) {
					cards.add(cardDAO.findCardById(pc.getCardId()));
				}

				// Obtener propiedades
				List<Property> properties = new ArrayList<>();
				for (PlayerProperty pp : playerPropertyDAO.findPlayerPropertiesByPlayerAndGame(id, gameId)) {
					properties.add(propertyDAO.findPropertyById(pp.getPropertyId()));
				}

				Player player = new Player();
				player.setIdPlayer(id);
				player.setProfile(profile);
				player.setCell(cell);
				player.setMoney(money);
				player.setGame(game);
				player.setBankrupt(isBankrupt);
				player.setJailTurnsLeft(jailTurns);
				player.setCards(cards);
				player.setProperties(properties);
				return player;
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
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@Override
	public void updatePlayer(Player player) {
		String sql = "UPDATE Player SET profile_id = ?, cell_id = ?, money = ?, game_id = ?, is_bankrupt = ?, jail_turns_left = ? WHERE id_player = ?";
		Connection conn = null;
		PreparedStatement statement = null;

		try {
			conn = ManagerConnection.obtenirConnexio();
			statement = conn.prepareStatement(sql);
			statement.setInt(1, player.getProfile().getIdProfile());
			statement.setInt(2, player.getCell().getIdCell());
			statement.setInt(3, player.getMoney());
			statement.setInt(4, player.getGame().getIdGame());
			statement.setInt(5, player.getIsBankrupt() ? 1 : 0);
			statement.setInt(6, player.getJailTurnsLeft());
			statement.setInt(7, player.getIdPlayer());
			statement.executeUpdate();

			DAOManager daoManager = new DAOManager();
			PlayerCardDAO cardDAO = daoManager.getPlayerCardDAO();
			PlayerPropertyDAO propertyDAO = daoManager.getPlayerPropertyDAO();

			if (!player.getCards().isEmpty()) {
				cardDAO.deletePlayerCard(player.getIdPlayer(), player.getGame().getIdGame(),
						player.getCards().get(0).getIdCard());
				for (Card card : player.getCards()) {
					cardDAO.addPlayerCard(
							new PlayerCard(player.getIdPlayer(), card.getIdCard(), player.getGame().getIdGame()));
				}
			}

			// Solo borrar si hay propiedades
			if (!player.getProperties().isEmpty()) {
				propertyDAO.deletePlayerProperty(player.getIdPlayer(), player.getGame().getIdGame(),
						player.getProperties().get(0).getIdProperty());
				for (Property property : player.getProperties()) {
					propertyDAO.addPlayerProperty(new PlayerProperty(player.getIdPlayer(), property.getIdProperty(),
							player.getGame().getIdGame()));
				}
			}

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
	public void deletePlayer(int id) {
		String sql = "DELETE FROM Player WHERE id_player = ?";
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
	public List<Player> getAll() {
		List<Player> players = new ArrayList<>();
		String sql = "SELECT * FROM Player";
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;

		try {
			conn = ManagerConnection.obtenirConnexio();
			statement = conn.prepareStatement(sql);
			resultSet = statement.executeQuery(sql);
			while (resultSet.next()) {
				Player player = findPlayerById(resultSet.getInt("id_player"));
				if (player != null) {
					players.add(player);
				}
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
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		return players;
	}

}