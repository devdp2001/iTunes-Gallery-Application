package cs1302.gallery;

import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.layout.TilePane;
import javafx.scene.text.Text;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.util.Duration;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.google.gson.*;

/**
 * Represents an iTunes GalleryApp.
 */
public class GalleryApp extends Application {

    Scene scene; // Scene
    Stage stage; // Stage
    VBox vBox; // container for scene graph
    HBox hBox; // progress bar hBox
    MenuBar menuBar; // menu bar
    ToolBar toolBar; // search, play/pause, and update conbtainer
    String input; // String for the user input
    TextField textField; // Textfield box
    TilePane tilePane = new TilePane(); // Tile pane
    Button playPause; // Play or Pause button
    Button imageUpdating; // Image Updating button
    boolean play = true; // whether the random image replacement is playing
    double playCount = -1.0; // times the play button is used
    ProgressBar progressBar = new ProgressBar(); // Progress bar
    double progress = 0.0; // current progress for the progress bar
    JsonArray backup; // backup elements
    JsonArray current; // current elements
    JsonArray jsonScreen; // Json
    String[] stringImage; // String Array
    ImageView[] screen = new ImageView[20]; // Array for urls to images
    Timeline timeline; // Timeline for program
    
    /**
     * Start Method. 
     * {@inheritdoc}
     */
    @Override
    public void start(Stage stage) {
        vBox = new VBox();
        vBox.getChildren().addAll(menuBar(), toolBar(), progressBar());
        
        Thread task = new Thread(() -> {
            getImage(input);
            Platform.runLater(() -> vBox.getChildren().add(randomizeTilePane()));
        });
        
        task.setDaemon(true);
        task.start();
        scene = new Scene(vBox, 500, 480);
        stage.setTitle("GalleryApp!");
        stage.setScene(scene);
        stage.sizeToScene();
        stage.show();
    } // start

    /**
     * Creates the Menu Bar.
     *
     * @return the Menu Bar used in the application
     */
    public MenuBar menuBar() {
        Menu file = new Menu("File");
        menuBar = new MenuBar();
        menuBar.getMenus().add(file);
        MenuItem exit = new MenuItem("Exit");
        file.getItems().add(exit);
        
        // exits out of program
        exit.setOnAction(event -> System.exit(0));
        
        return menuBar;
    } // menuBar

    /**
     * Creates the fully functioning Tool Bar.
     *
     * @return the Tool Bar used in the application
     */
    public ToolBar toolBar() {
        // Creates tool bar and adds buttons and text field
        toolBar = new ToolBar();
        playPause = new Button("Play");
        Label searchQuery = new Label("Search Query:");
        textField = new TextField("rock");
        input = parseInput(textField);
        imageUpdating = new Button("Update Images");
        toolBar.getItems().addAll(playPause, searchQuery, textField, imageUpdating);

        // action for playOrPause button
        playPause.setOnAction(p -> updateImage());

        // action for updating images
        imageUpdating.setOnAction(e -> {
            imageUpdating.setStyle("-fx-background: #FF0000;");
            boolean updating = false;

            //pause timeline if it is running
            if (timeline != null) {
                if (timeline.getStatus() == Animation.Status.RUNNING) {
                    updating = true;
                    timeline.pause();
                }
            }
            String textFieldInput = parseInput(textField);
            progressBar.setProgress(0.0);

            Thread task = new Thread(() -> {
                getImage(textFieldInput);
                Platform.runLater(() -> randomizeTilePane());
            });
            task.setDaemon(true);
            task.start();
            
            //run timeline if it is paused                               
            if (updating) {
                timeline.play();
            }
        });
        return toolBar;
    } // toolBar

    /**
     * Creates the Progress Bar.
     * 
     * @return the Progress Bar for the application    
     */
    public HBox progressBar() {
        hBox = new HBox();

        // Label next to progress bar
        Label credits = new Label(" Images provided courtesy of iTunes");

        progressBar.setLayoutX(50.0);
        progressBar.setLayoutY(550.0);
        hBox.getChildren().addAll(progressBar, credits);

        return hBox;
    } // progressBar

    /**
     * Parses the user's input inside of the Text Field.
     *
     * @param textField the textfield in menubar with user input
     * @return the String inputed into the textField
     */
    public String parseInput(TextField textField) {
        input = textField.getText();

        //adds each word in userInput to an array
        String[] inputArray = input.split(" ");
        input = inputArray[0];
        
        //adds individual search words to string
        for (int i = 1; i < inputArray.length; i++) {
            input += "+" + inputArray[i];
        }
        
        //resetting the progress instance variable
        progress = 0.0;
        
        return input;
    } // parseInput

    /** 
     * Parses the JSON query results. 
     * 
     * @param input the users newly entered search
     */
    public void getImage(String input) {
        URL url = null;
        InputStreamReader inputStreamReader = null;

        tilePane.setPrefColumns(5); // 5 columns
        tilePane.setPrefRows(4); // 4 rows  

        // puts the input into itunes search url 
        String itunesUrl = "https://itunes.apple.com/search?term=" + input;

        try {
            URLEncoder.encode(itunesUrl, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException UEE) {
            throw new RuntimeException(UEE.getMessage());
        }
        getQueryResults(url, itunesUrl, inputStreamReader);
        
        if (stringImage.length >= 21) { // fills results array with string URLs
            for (int i = 0; i < 20; i++) {
                JsonObject object = jsonScreen.get(i).getAsJsonObject();
                JsonElement elementUrl = object.get("artworkUrl100");
                current.add(object);

                if (elementUrl != null) { // element not existing 
                    String imageUrl = elementUrl.getAsString();
                    Image image = new Image(imageUrl);

                    stringImage[i] = imageUrl;
                    screen[i] = new ImageView();
                    screen[i].setImage(new Image(stringImage[i]));
                    Platform.runLater(() -> {
                        progress += 0.05;
                        progressBar.setProgress(progress);
                    });
                }
            }
        } else if (stringImage.length < 21) { // outputs error message if < 21 available 
            Platform.runLater(() -> {
                Alert error = new Alert(Alert.AlertType.ERROR,
                                        "Not Enough Results, Not Displaying Results!",
                                        ButtonType.OK);
                error.showAndWait();
                playPause.setText("Play");
            });
        }
    } //getImage
    
    /**
     * Updates the images in the TilePane.
     */
    public void updateImage() {
        playCount++;
        
        //if the playcount is odd, play is false
        if (playCount % 2 != 0.0) {
            play = false;
        } else {
            play = true;
        }
        Platform.runLater(() -> {
            //if on play, change button and play timeline
            if (play) {
                playPause.setText("Pause");
                timeline.play();
            } else  {
                playPause.setText("Play");
                timeline.pause();
                return;
            }
        });
        if (play) {
            EventHandler<ActionEvent> object = (e -> {
                if (jsonScreen.size() > 21) {
                    
                    //pause button if not already paused
                    if (playPause.getText() == "Play") {
                        timeline.pause();
                        return;
                    }
                    //gets a backup image art
                    int unusedImage = (int) (Math.random() * backup.size());
                    
                    //gets a random image art being displayed
                    int currentImage = (int) (Math.random() * screen.length);
                    
                    JsonObject finalObject = backup.get(unusedImage).getAsJsonObject();
                    JsonElement imageElement = finalObject.get("artworkUrl100");
                    
                    //replaces the images
                    if (imageElement != null) {
                        replaceImage(imageElement, currentImage);
                    }
                    if (current.contains(backup.get(unusedImage))) {
                        backup.remove(backup.get(unusedImage));
                    }
                }
            });
            KeyFrame keyFrame = new KeyFrame(Duration.seconds(2), object);

            timeline = new Timeline();
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.getKeyFrames().add(keyFrame);
            timeline.play();
        }
    } // updateImage

    /**
     * This method chooses a random image being displayed to
     * replace with an unused one and sets it to the ImageView
     * object and adds all objects to TilePane.
     *
     * @param imageElement element of newly introduced member
     * @param newMember integer of new member in backup array
     */
    public void replaceImage(JsonElement imageElement, int newMember) {
        //adds new member to used
        current.add(imageElement);
        
        String imageUrl = imageElement.getAsString();
        Image image = new Image(imageUrl);

        screen[newMember] = new ImageView(); // creates new object in place of old 

        backup.add(jsonScreen.get(newMember)); //adds image displayed to unused
        screen[newMember].setImage(image);
        screen[newMember].setFitHeight(100.0);
        screen[newMember].setFitWidth(100.0);
        tilePane.getChildren().clear();

        //add ImageView object to tile pane object
        for (int i = 0; i < 20; i++) {
            tilePane.getChildren().add(screen[i]);
        }
    } // replaceImage

    /**
     * Gets the query results and parses it.
     *
     * @param url the url containing the image
     * @param urlString the string that will contain the parsed url link
     * @param streamReader reader which passes through the query results
     */
    public void getQueryResults(URL url, String urlString, InputStreamReader streamReader) {
        try { // catches errors
            url = new URL(urlString);
        } catch (MalformedURLException MURLE) {
            throw new RuntimeException(MURLE.getMessage());
        }
        
        try { // catches errors
            streamReader = new InputStreamReader(url.openStream());
        } catch (IOException IOE) {
            throw new RuntimeException(IOE.getMessage());
        }
        current = new JsonArray(); // holds all elements being displayed
        
        JsonParser jsonParser = new JsonParser();
        JsonElement jsonElement = jsonParser.parse(streamReader);
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        jsonScreen = jsonObject.getAsJsonArray("results");
        stringImage = new String[jsonScreen.size()];
        backup = jsonObject.getAsJsonArray("results");  
    } // getQueryResults

    /**
     * Updates the Tile Pane from the new search.
     *
     * @return the new Tile Pane
     */
    public TilePane randomizeTilePane() {
        if (stringImage.length < 21) {
            return tilePane;
        }
        
        tilePane.getChildren().clear(); // clear Tile Pane
        progress = 0.0; // resets progress instance variable
        
        for (int i = 0; i < 20; i++) {
            screen[i].setImage(new Image(stringImage[i]));
            screen[i].setFitHeight(100.0);
            screen[i].setFitWidth(100.0);
            tilePane.getChildren().add(screen[i]);
        }
        
        for (int j = 0; j < backup.size(); j++) {
            if (current.contains(backup.get(j))) {
                backup.remove(backup.get(j));
            }
        }
        return tilePane;
    } // randomizeTilePane
    
} // GalleryApp
