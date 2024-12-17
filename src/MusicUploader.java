import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.FieldKey;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class MusicUploader {

    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/music"; // Replace with your database URL
        String user = "root"; // Replace with your database username
        String password = ""; // Replace with your database password

        // Directory containing all MP3 files
        File folder = new File("src/assets/songs1"); // Replace with the path to your folder

        // Check if folder exists and is a directory
        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("Directory does not exist or is not a directory.");
            return;
        }

        // Get all files in the folder
        File[] songFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp3"));

        if (songFiles == null || songFiles.length == 0) {
            System.out.println("No MP3 files found in the folder.");
            return;
        }

        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement ps = connection.prepareStatement("INSERT INTO songs (id, title, artist, song_file_path) VALUES (?, ?, ?, ?)")) {

            // Loop through each MP3 file
            for (File songFile : songFiles) {
                try {
                    // Extract the song ID from the file name (without the .mp3 extension)
                    String fileName = songFile.getName();
                    int songId = Integer.parseInt(fileName.substring(0, fileName.indexOf('.')));

                    // Use JAudiotagger to extract metadata
                    AudioFile audioFile = AudioFileIO.read(songFile);
                    Tag tag = audioFile.getTag();
                    String title = tag.getFirst(FieldKey.TITLE);
                    String artist = tag.getFirst(FieldKey.ARTIST);

                    // If title or artist is missing, set default values
                    if (title == null || title.isEmpty()) {
                        title = "Unknown Title";
                    }
                    if (artist == null || artist.isEmpty()) {
                        artist = "Unknown Artist";
                    }

                    // Set the prepared statement parameters
                    ps.setInt(1, songId);
                    ps.setString(2, title);
                    ps.setString(3, artist);
                    ps.setString(4, songFile.getAbsolutePath()); // Store the absolute file path

                    // Execute the update for the current song
                    ps.executeUpdate();
                    System.out.println("Uploaded: " + fileName + " (Title: " + title + ", Artist: " + artist + ", Path: " + songFile.getAbsolutePath() + ")");

                } catch (Exception e) {
                    System.err.println("Error uploading file: " + songFile.getName());
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
   }
}
}
