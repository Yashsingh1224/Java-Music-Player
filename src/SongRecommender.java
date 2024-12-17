import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

class Song {
    String name;
    String genre;

    public Song(String name, String genre) {
        this.name = name;
        this.genre = genre;
    }
}

public class SongRecommender {
    private static Map<String, Song> songMap = new HashMap<>();
    private static List<Song> songs = new ArrayList<>();

    public static void main(String[] args) {
        loadSongs("C:\\Users\\mrsta\\Documents\\ex.csv");

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the song name for recommendations: ");
        String songName = scanner.nextLine().trim();
        scanner.close();

        List<Song> recommendations = getRecommendations(songName);
        System.out.println("Top 5 similar songs:");
        for (Song song : recommendations) {
            System.out.println(song.name);
        }
    }

    public static void loadSongs(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            br.readLine();  // Skip the header
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");

                if (values.length < 4) {  // Assuming genre is the 4th column (index 3)
                    System.out.println("Skipping line due to incorrect format: " + line);
                    continue;
                }

                String name = values[1].trim();
                String genre = values[3].trim();

                Song song = new Song(name, genre);
                songs.add(song);
                songMap.put(name.toLowerCase(), song);  // Store in lowercase for case-insensitive matching
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Unable to read file at: " + filePath);
        }
    }

    public static List<Song> getRecommendations(String songName) {
        Song targetSong = songMap.get(songName.toLowerCase());
        if (targetSong == null) {
            System.out.println("Song not found!");
            return Collections.emptyList();
        }

        System.out.println("Successfully found!");
        List<Song> recommendations = new ArrayList<>(songs);

        // Remove the target song from the list of recommendations to avoid including it
        recommendations.removeIf(song -> song.name.equalsIgnoreCase(songName));

        recommendations.sort((a, b) ->
                Double.compare(cosineSimilarity(targetSong, b), cosineSimilarity(targetSong, a))
        );

        // Return the top 5 recommendations
        return recommendations.subList(0, Math.min(5, recommendations.size()));
    }


    private static double cosineSimilarity(Song s1, Song s2) {
        // Convert genres into sets of words (bag of words approach)
        Set<String> genreWords1 = new HashSet<>(Arrays.asList(s1.genre.toLowerCase().split(" ")));
        Set<String> genreWords2 = new HashSet<>(Arrays.asList(s2.genre.toLowerCase().split(" ")));

        // Calculate the intersection of words (common words between genres)
        Set<String> intersection = new HashSet<>(genreWords1);
        intersection.retainAll(genreWords2);

        // Calculate cosine similarity based on word overlap
        double dotProduct = intersection.size();
        double magnitude1 = Math.sqrt(genreWords1.size());
        double magnitude2 = Math.sqrt(genreWords2.size());

        // Handle division by zero if either genre is empty
        if (magnitude1 == 0 || magnitude2 == 0) return 0.0;

        return dotProduct / (magnitude1 * magnitude2);
    }
}
