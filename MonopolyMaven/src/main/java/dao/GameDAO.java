package dao;

import java.util.List;

import models.Game;
import models.Player;

/**
 * @author Ana
 */
public interface GameDAO {

	/* CRUD operations */
	/* Create */
	public int addGame(Game game);

	/* Read */
	public Game findGameById(int id);

	/* Update */
	public void updateGame(Game game);

	/* Delete */
	public void deleteGame(int id);

	/* Read All */
	public List<Game> getAll();

	public List<Player> loadPlayers(int gameId);

	List<Player> loadPlayers();

}
