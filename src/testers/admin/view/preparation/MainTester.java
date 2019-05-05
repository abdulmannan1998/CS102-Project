package testers.admin.view.preparation;

import java.io.IOException;
import java.net.MalformedURLException;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class MainTester extends Application
{

	@Override
	public void start( Stage stage) throws MalformedURLException, IOException
	{
		Pane root;
		String resource;

		resource = "/admin/view/fxml/Main.fxml";
		root = FXMLLoader.load( getClass().getResource( resource));
		Scene scene = new Scene( root, 1200, 675);
		stage.setScene( scene);

		stage.setOnCloseRequest( new EventHandler<WindowEvent>() {
			@Override
			public void handle( WindowEvent event)
			{
				System.exit( 0);
			}
		});

		stage.setTitle( "Sample frame");
		stage.show();
	}

	public static void main( String[] args)
	{
		launch( args);
	}
}
