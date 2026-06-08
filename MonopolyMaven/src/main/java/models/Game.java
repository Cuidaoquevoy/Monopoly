package models;

/**
 * @author Ana
 */
public class Game {

	public enum State {
		IN_GAME, FINISHED, PAUSED, SAVED
	}

	public int idGame;
	public State state;
	public String duration;
	public String name;

	public Game(int idGame, State state, String duration) {
		super();
		this.idGame = idGame;
		this.state = state;
		this.duration = duration;
	}

	public Game() {
		// TODO Auto-generated constructor stub
	}

	public int getIdGame() {
		return idGame;
	}

	public void setIdGame(int idGame) {
		this.idGame = idGame;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public String getDuration() {
		return duration;
	}

	public void setDuration(String duration) {
		this.duration = duration;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isGameOver() {
		// Implementar lógica para finalizar el juego.
		return true;
	}
}