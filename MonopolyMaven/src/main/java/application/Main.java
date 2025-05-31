package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Main extends Application {
	@Override
	public void start(Stage primaryStage) {
		try {
			BorderPane root = (BorderPane) FXMLLoader.load(getClass().getResource("/views/MainView.fxml"));
			Scene scene = new Scene(root, 800, 600);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
}

//Arreglar porqué peta
//Mostrar por pantalla las propiedades y cartas del jugador que tiene el turno
//Guardar y continuar las partidas.

//No permitir jugar con menos de 2 personas.
//Hacer que en la tirada para escoger turno los dados se mantengan en el centro de la pantalla
// Al comprar una propiedad no se resta el dinero total. Poner talvez algun random entre 50 y 200
