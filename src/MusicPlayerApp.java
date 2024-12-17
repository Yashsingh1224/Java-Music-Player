import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.animation.FadeTransition;


import java.sql.*;
import java.io.*;

import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

public class MusicPlayerApp extends Application {

    // Set to track already played songs
    private static Set<String> alreadyPlayedSongs = new HashSet<>();
    private SongRecommender songRecommender = new SongRecommender(); // Instance of your recommendation logic

    private List<String> recommendedSongsQueue = new ArrayList<>(); // To hold recommended songs
    private MediaPlayer mediaPlayer;
    private Slider timeSlider;
    private Label songLabel;
    private Label currentTimeLabel; // To show current time
    private Label totalTimeLabel; // To show total duration
    private Button playButton;
    private Button likeButton, dislikeButton;
    private Image playImage;
    private Image pauseImage;
    private Image nextImage;
    private Image prevImage;
    private ListView<String> songListView;
    private ObservableList<String> currentPlaylist; // To hold the current playlist
    private int currentSongIndex = -1; // To track the current song index
    private boolean playlistLoaded = false;
    Label notificationLabel = new Label();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Music Player");
        primaryStage.getIcons().add(new Image("assets/cd.png"));

        songRecommender.loadSongs("C:\\Users\\mrsta\\Documents\\ex.csv");

        // Load play, pause, next, and previous images
        playImage = new Image("assets/play.png");
        pauseImage = new Image("assets/pause.png");
        nextImage = new Image("assets/next.png");
        prevImage = new Image("assets/previous.png");

        // Search bar
        TextField searchField = new TextField();
        searchField.setPromptText("Search songs by title or artist...");
        searchField.setStyle("-fx-background-color: #4D4D4D; -fx-text-fill: white;");

        // Song list
        songListView = new ListView<>();
        songListView.setStyle("-fx-background-color: #2C2F33; -fx-text-fill: white;");
        songListView.setOnMouseClicked(event -> {
            String selectedSong = songListView.getSelectionModel().getSelectedItem();
            if (selectedSong != null) {
                playSongFromDB(selectedSong);
            }
        });

        // Play, pause, next, previous buttons
        playButton = createButton(playImage);
        playButton.setOnAction(event -> playPauseSong());

        Button nextButton = createButton(nextImage);
        nextButton.setOnAction(event -> playNextSong());

        Button prevButton = createButton(prevImage);
        prevButton.setOnAction(event -> playPreviousSong());

        // Slider
        timeSlider = new Slider();
        timeSlider.setMin(0);
        timeSlider.setMax(100);
        timeSlider.setStyle("-fx-background-color: #4D4D4D;");
        timeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (timeSlider.isValueChanging() && mediaPlayer != null) {
                mediaPlayer.seek(Duration.seconds(newValue.doubleValue()));
            }
        });
        // Notification Label setup

        notificationLabel.setStyle("-fx-text-fill: white; -fx-background-color: #3D3D3D; -fx-padding: 10; -fx-font-size: 14px;");
        notificationLabel.setVisible(false);  // Hide initially


        Image likeImage = new Image("assets/like.png");
        Image dislikeImage = new Image("assets/dislike.png");

        // Like and Dislike buttons
        likeButton = new Button();
        likeButton.setOnAction(event -> likeSong());
        likeButton.setGraphic(new ImageView(likeImage));
        likeButton.setStyle("-fx-background-color: transparent; -fx-padding: 10;");

        dislikeButton = new Button();
        dislikeButton.setStyle("-fx-background-color: transparent; -fx-padding: 10;");
        dislikeButton.setOnAction(event -> dislikeSong());
        dislikeButton.setGraphic(new ImageView(dislikeImage));
        // Song Label
        songLabel = new Label("No song playing...");
        songLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px;");

        // Time Labels
        currentTimeLabel = new Label("0:00");
        currentTimeLabel.setStyle("-fx-text-fill: white;");
        totalTimeLabel = new Label("0:00");
        totalTimeLabel.setStyle("-fx-text-fill: white;");

        // HBox for current and total time
        HBox timeLabelsBox = new HBox(350, currentTimeLabel, totalTimeLabel);
        timeLabelsBox.setAlignment(Pos.CENTER); // Align both labels horizontally
        timeLabelsBox.setStyle("-fx-padding: 5; -fx-hgap: 10;");

        // Search functionality
        Button searchButton = new Button("Search");
        searchButton.setStyle("-fx-background-color: #7289DA; -fx-text-fill: white;");
        searchButton.setOnAction(event -> searchSongs(searchField.getText(), songListView));

        // Create Playlist button
        Button createPlaylistButton = new Button("Create Playlist");
        createPlaylistButton.setOnAction(event -> openCreatePlaylistWindow());

        // Load Playlist button
        Button loadPlaylistButton = new Button("Load Playlist");
        loadPlaylistButton.setOnAction(event -> openLoadPlaylistWindow());

        // Layout for buttons in equal-sized boxes
        HBox buttonLayout = new HBox(10, prevButton, playButton, nextButton);
        buttonLayout.setAlignment(Pos.CENTER);
        buttonLayout.setStyle("-fx-spacing: 10px;");

        // Layout for Like and Dislike buttons
        HBox likeDislikeLayout = new HBox();
        likeDislikeLayout.getChildren().addAll( dislikeButton, likeButton);
        likeDislikeLayout.setAlignment(Pos.CENTER);
        likeDislikeLayout.setSpacing(300); // Adjust this value to control the gap
        likeDislikeLayout.setPadding(new Insets(10)); // Adds padding around the buttons

        HBox songDisplay = new HBox();
        songDisplay.getChildren().add(songLabel);
        songDisplay.setAlignment(Pos.CENTER);
        songDisplay.setPadding(new Insets(20));

        VBox layout = new VBox(10, searchField, searchButton, createPlaylistButton, loadPlaylistButton, songListView, timeLabelsBox, timeSlider, songDisplay,  buttonLayout, likeDislikeLayout, notificationLabel);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #2C2F33; -fx-padding: 20;");

        Scene scene = new Scene(layout, 500, 750);
        primaryStage.setResizable(false);
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Method to create a button with a fixed-size box and padding, but no background
    private Button createButton(Image image) {
        Button button = new Button();
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(40);
        imageView.setFitHeight(40);
        button.setGraphic(imageView);
        button.setStyle("-fx-background-color: transparent; -fx-padding: 10;");
        button.setPrefSize(50, 50);
        return button;
    }

    // Method to play or pause a song
    private void playPauseSong() {
        if (mediaPlayer != null) {
            MediaPlayer.Status status = mediaPlayer.getStatus();
            if (status == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();
                playButton.setGraphic(new ImageView(playImage));
            } else {
                mediaPlayer.play();
                playButton.setGraphic(new ImageView(pauseImage));
            }
        }
    }

    // Method to play the next song in the playlist
    private void playNextSong() {
        if (playlistLoaded) {
            // Handle playlist scenario
            if (currentPlaylist != null && !currentPlaylist.isEmpty()) {
                currentSongIndex = (currentSongIndex + 1) % currentPlaylist.size();
                String nextSong = currentPlaylist.get(currentSongIndex);
                playSongFromDB(nextSong);
            }
        } else {
            // Check if there are no recommended songs left
            if (recommendedSongsQueue.isEmpty()) {
                System.out.println("No more recommended songs in the queue.");
                return;
            }

            // Loop until a valid song is found
            while (!recommendedSongsQueue.isEmpty()) {
                String nextSongTitle = recommendedSongsQueue.get(0); // Get the first song in the queue

                // Check if the song has already been played
                if (alreadyPlayedSongs.contains(nextSongTitle)) {
                    System.out.println("Song already played: " + nextSongTitle + ". Skipping...");
                    recommendedSongsQueue.remove(0); // Remove this song from the queue
                    continue; // Check the next song in the queue
                }

                // Song hasn't been played, so we can play it
                System.out.println("Playing song: " + nextSongTitle);

                // Your logic for playing the song
                try (Connection connection = connectToDatabase();
                     PreparedStatement ps = connection.prepareStatement("SELECT title, artist FROM songs WHERE title = ?")) {

                    ps.setString(1, nextSongTitle); // Set the song title in the query
                    System.out.println("Querying database for song: " + nextSongTitle); // Debug

                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        // Fetch song info from the database
                        String title = rs.getString("title");
                        String artist = rs.getString("artist");
                        String songInfo = title + " - " + artist;

                        System.out.println("Fetched song from DB: " + songInfo); // Debug
                        playSongFromDB(songInfo); // Play the song from DB

                        // Mark this song as already played
                        alreadyPlayedSongs.add(nextSongTitle);

                        // Remove the song from the list after playing it
                        recommendedSongsQueue.remove(0); // Remove the first song after playing it
                        break; // Exit after playing one song
                    } else {
                        System.out.println("Song not found in database: " + nextSongTitle);
                        recommendedSongsQueue.remove(0); // Remove invalid song and continue
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    // Method to play the previous song in the playlist
    private void playPreviousSong() {
        if (currentPlaylist != null && !currentPlaylist.isEmpty()) {
            currentSongIndex = (currentSongIndex - 1 + currentPlaylist.size()) % currentPlaylist.size();
            String prevSong = currentPlaylist.get(currentSongIndex);
            playSongFromDB(prevSong);
        }
    }

    // Method to show notification
    private void showNotification(String message) {
        notificationLabel.setText(message);
        notificationLabel.setVisible(true);

        // Create a fade transition to make it disappear after 3 seconds
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(3), notificationLabel);
        fadeOut.setFromValue(1.0);  // Fully visible
        fadeOut.setToValue(0.0);    // Fully transparent
        fadeOut.setOnFinished(event -> notificationLabel.setVisible(false)); // Hide after fade
        fadeOut.play();
    }

    // Action for the Like button
    private void likeSong() {
        if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            String currentSongTitle = songLabel.getText().split(" - ")[0];
            System.out.println("Song Liked: " + currentSongTitle);
            showNotification("You liked this song!");
            // Logic to prioritize liked songs or generate additional recommendations
            generateRecommendations(currentSongTitle);
        } else {
            showNotification("No song is currently playing.");
        }
    }

    // Action for the Dislike button
    private void dislikeSong() {
        if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            String currentSongTitle = songLabel.getText().split(" - ")[0];
            System.out.println("Song Disliked: " + currentSongTitle);
            showNotification("You disliked this song!");

            // Skip the current song and play the next recommended one
            playNextSong();
        } else {
            showNotification("No song is currently playing.");
        }
    }

    // Method to search for songs in the database
    private void searchSongs(String query, ListView<String> songListView) {
        songListView.getItems().clear();
        try (Connection connection = connectToDatabase();
             PreparedStatement ps = connection.prepareStatement("SELECT title, artist FROM songs WHERE title LIKE ? OR artist LIKE ?")) {

            ps.setString(1, "%" + query + "%");
            ps.setString(2, "%" + query + "%");

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String title = rs.getString("title");
                String artist = rs.getString("artist");
                songListView.getItems().add(title + " - " + artist);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to play a song from the database
    private void playSongFromDB(String songInfo) {
        String[] parts = songInfo.split(" - ");
        String title = parts[0];
        String artist = parts[1];

        try (Connection connection = connectToDatabase();
             PreparedStatement ps = connection.prepareStatement("SELECT song_file_path FROM songs WHERE title = ? AND artist = ?")) {

            ps.setString(1, title);
            ps.setString(2, artist);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String songFilePath = rs.getString("song_file_path");

                // Check if the file exists at the path
                File songFile = new File(songFilePath);
                if (songFile.exists() && songFile.isFile()) {
                    // Play the song using the file path
                    Media media = new Media(songFile.toURI().toString());
                    if (mediaPlayer != null) {
                        mediaPlayer.stop();
                    }
                    mediaPlayer = new MediaPlayer(media);
                    mediaPlayer.setOnReady(() -> {
                        timeSlider.setMax(mediaPlayer.getTotalDuration().toSeconds());
                        mediaPlayer.play();
                        songLabel.setText(title + " - " + artist);
                        playButton.setGraphic(new ImageView(pauseImage));
                        totalTimeLabel.setText(formatTime(mediaPlayer.getTotalDuration()));

                        // Automatically play next song when current song ends
                        mediaPlayer.setOnEndOfMedia(() -> playNextSong());
                    });

                    mediaPlayer.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
                        timeSlider.setValue(newValue.toSeconds());
                        currentTimeLabel.setText(formatTime(newValue));
                    });
                }

                // Generate recommendations for the song
                System.out.println("Generating recommendations for song: " + title); // Debug
                System.out.println(title);
                generateRecommendations(title);

                // Update current playlist and index
                updateCurrentPlaylist(songInfo);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // gete recommended songs
    private void generateRecommendations(String songName) {
        recommendedSongsQueue.clear(); // Clear previous recommendations

        // Get the recommendations for the song
        List<Song> recommendations = songRecommender.getRecommendations(songName);
        if (recommendations.isEmpty()) {
            System.out.println("No recommendations found for the song: " + songName);
        }

        // Add recommended songs to the list
        for (Song song : recommendations) {
            // Only add the song if it hasn't been played before
            if (!alreadyPlayedSongs.contains(song.name)) {
                System.out.println("Adding recommended song: " + song.name); // Debug
                recommendedSongsQueue.add(song.name); // Add recommended song names to the list
            }
        }

        System.out.println("Recommended Songs Queue: " + recommendedSongsQueue); // Debug
    }


    // Update the current playlist and set the current song index
    private void updateCurrentPlaylist(String songInfo) {
        if (currentPlaylist == null) {
            currentPlaylist = songListView.getItems(); // Load songs from the ListView
        }
        currentSongIndex = currentPlaylist.indexOf(songInfo); // Set the index of the current song
    }

    // Format time in "MM:SS" format
    private String formatTime(Duration duration) {
        int minutes = (int) duration.toMinutes();
        int seconds = (int) duration.toSeconds() % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // Connect to the database
    private Connection connectToDatabase() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/music"; // Adjust your database URL, username, and password
        String user = "root"; // Adjust according to your setup
        String password = ""; // Adjust according to your setup
        return DriverManager.getConnection(url, user, password);
    }
    // Method to open the Create Playlist window
    private void openCreatePlaylistWindow() {
        Stage createPlaylistStage = new Stage();
        createPlaylistStage.setTitle("Create Playlist");

        // Create a modern, spacious layout
        VBox layout = new VBox(15);  // Increased spacing
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #2F3136; -fx-padding: 20; -fx-border-radius: 10; -fx-background-radius: 10;");

        TextField playlistNameField = new TextField();
        playlistNameField.setPromptText("Enter playlist name");
        playlistNameField.setStyle("-fx-background-color: #4D4D4D; -fx-text-fill: white; -fx-padding: 10; -fx-border-radius: 5;");

        ListView<String> availableSongsView = new ListView<>();
        availableSongsView.setStyle("-fx-background-color: #2C2F33; -fx-text-fill: white; -fx-border-radius: 10;");
        searchSongs("", availableSongsView);

        ListView<String> selectedSongsView = new ListView<>();
        selectedSongsView.setStyle("-fx-background-color: #2C2F33; -fx-text-fill: white; -fx-border-radius: 10;");

        Button addSongButton = new Button("Add Song");
        addSongButton.setStyle("-fx-background-color: #7289DA; -fx-text-fill: white; -fx-padding: 10 20; -fx-border-radius: 5;");
        addSongButton.setOnAction(event -> {
            String selectedSong = availableSongsView.getSelectionModel().getSelectedItem();
            if (selectedSong != null) {
                selectedSongsView.getItems().add(selectedSong);
            }
        });

        Button savePlaylistButton = new Button("Save Playlist");
        savePlaylistButton.setStyle("-fx-background-color: #7289DA; -fx-text-fill: white; -fx-padding: 10 20; -fx-border-radius: 5;");
        savePlaylistButton.setOnAction(event -> {
            String playlistName = playlistNameField.getText();
            ObservableList<String> selectedSongs = selectedSongsView.getItems();
            savePlaylistToDB(playlistName, selectedSongs);
            createPlaylistStage.close();
        });

        layout.getChildren().addAll(playlistNameField, availableSongsView, addSongButton, selectedSongsView, savePlaylistButton);

        Scene scene = new Scene(layout, 350, 500);
        scene.getStylesheets().add("styles.css");  // Assuming the stylesheet is already linked.
        createPlaylistStage.setScene(scene);
        createPlaylistStage.show();
    }


    // Method to save the created playlist to the database
    private void savePlaylistToDB(String playlistName, ObservableList<String> selectedSongs) {
        // Add your logic here to save the playlist to the database
        try (Connection connection = connectToDatabase()) {
            String insertPlaylistSQL = "INSERT INTO playlists (name) VALUES (?)";
            PreparedStatement playlistStmt = connection.prepareStatement(insertPlaylistSQL, Statement.RETURN_GENERATED_KEYS);
            playlistStmt.setString(1, playlistName);
            playlistStmt.executeUpdate();

            ResultSet generatedKeys = playlistStmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                int playlistId = generatedKeys.getInt(1);
                String insertSongSQL = "INSERT INTO playlist_songs (playlist_id, song_id) VALUES (?, ?)";
                PreparedStatement songStmt = connection.prepareStatement(insertSongSQL);

                for (String songInfo : selectedSongs) {
                    String[] parts = songInfo.split(" - ");
                    String title = parts[0];
                    String artist = parts[1];
                    // Get the song_id from the database
                    try (PreparedStatement songIdStmt = connection.prepareStatement("SELECT id FROM songs WHERE title = ? AND artist = ?")) {
                        songIdStmt.setString(1, title);
                        songIdStmt.setString(2, artist);
                        ResultSet rs = songIdStmt.executeQuery();
                        if (rs.next()) {
                            int songId = rs.getInt("id");
                            songStmt.setInt(1, playlistId);
                            songStmt.setInt(2, songId);
                            songStmt.executeUpdate();
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to open the Load Playlist window
    private void openLoadPlaylistWindow() {
        Stage loadPlaylistStage = new Stage();
        loadPlaylistStage.setTitle("Load Playlist");

        // Create a modern layout with padding
        VBox layout = new VBox(15);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #2F3136; -fx-padding: 20; -fx-border-radius: 10; -fx-background-radius: 10;");

        ListView<String> playlistsView = new ListView<>();
        playlistsView.setStyle("-fx-background-color: #2C2F33; -fx-text-fill: white; -fx-border-radius: 10;");
        loadPlaylists(playlistsView);

        Button loadButton = new Button("Load Playlist");
        loadButton.setStyle("-fx-background-color: #7289DA; -fx-text-fill: white; -fx-padding: 10 20; -fx-border-radius: 5;");
        loadButton.setOnAction(event -> {
            String selectedPlaylist = playlistsView.getSelectionModel().getSelectedItem();
            if (selectedPlaylist != null) {
                loadPlaylist(selectedPlaylist);
                loadPlaylistStage.close();
            }
        });

        layout.getChildren().addAll(playlistsView, loadButton);

        Scene scene = new Scene(layout, 350, 400);
        scene.getStylesheets().add("styles.css");
        loadPlaylistStage.setScene(scene);
        loadPlaylistStage.show();
    }


    // Method to load playlists from the database
    private void loadPlaylists(ListView<String> playlistsView) {
        try (Connection connection = connectToDatabase();
             PreparedStatement ps = connection.prepareStatement("SELECT name FROM playlists")) {

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String name = rs.getString("name");
                playlistsView.getItems().add(name);
            }
            playlistLoaded = true;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to load songs from a selected playlist
    private void loadPlaylist(String playlistName) {
        try (Connection connection = connectToDatabase();
             PreparedStatement ps = connection.prepareStatement("SELECT s.title, s.artist FROM playlist_songs ps JOIN songs s ON ps.song_id = s.id JOIN playlists p ON ps.playlist_id = p.id WHERE p.name = ?")) {

            ps.setString(1, playlistName);
            ResultSet rs = ps.executeQuery();
            songListView.getItems().clear();
            while (rs.next()) {
                String title = rs.getString("title");
                String artist = rs.getString("artist");
                songListView.getItems().add(title + " - " + artist);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}