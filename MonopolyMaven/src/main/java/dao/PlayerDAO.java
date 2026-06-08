package dao;

import java.util.List;

import models.Player;

/**
 * @author Víctor
 */
public interface PlayerDAO {

	/* CRUD operations */
	/* Create */
	public int addPlayer(Player player);

	/* Read */
	public Player findPlayerById(int id);

	/* Update */
	public void updatePlayer(Player player);

	/* Delete */
	public void deletePlayer(int id);

	/* Read All */
	public List<Player> getAll();

	/* Read by game — necesario para cargar partida guardada */
	public List<Player> getPlayersByGame(int gameId);

}