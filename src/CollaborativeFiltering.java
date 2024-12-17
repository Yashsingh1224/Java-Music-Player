import java.io.*;
import java.util.*;

public class CollaborativeFiltering {

    // Map to store user-song ratings: {user -> {song -> rating}}
    private static Map<String, Map<Integer, Integer>> userRatings = new HashMap<>();

    public static void main(String[] args) throws IOException {
        String filePath = "src/assets/music_users_dataset.csv"; // Path to the dataset
        loadDataset(filePath);

        String targetUser = "user1"; // Target user for recommendations
        List<Integer> recommendations = recommendSongs(targetUser, 5); // Get 5 recommendations

        System.out.println("Recommendations for " + targetUser + ": " + recommendations);
    }

    // Load dataset from CSV into userRatings map
    private static void loadDataset(String filePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line = reader.readLine(); // Skip header
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(",");
            String user = parts[0];
            int songId = Integer.parseInt(parts[1]);
            int rating = Integer.parseInt(parts[2]);

            userRatings.computeIfAbsent(user, k -> new HashMap<>()).put(songId, rating);
        }
        reader.close();
    }

    // Recommend songs for the target user
    private static List<Integer> recommendSongs(String targetUser, int numRecommendations) {
        Map<Integer, Integer> targetRatings = userRatings.get(targetUser);
        if (targetRatings == null) {
            throw new IllegalArgumentException("User not found: " + targetUser);
        }

        // Compute similarities between the target user and all other users
        Map<String, Double> similarities = new HashMap<>();
        for (String otherUser : userRatings.keySet()) {
            if (!otherUser.equals(targetUser)) {
                double similarity = cosineSimilarity(targetRatings, userRatings.get(otherUser));
                similarities.put(otherUser, similarity);
            }
        }

        // Find top N similar users
        List<String> topUsers = similarities.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue())) // Sort by similarity descending
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();

        // Aggregate recommendations from top users
        Map<Integer, Double> songScores = new HashMap<>();
        for (String similarUser : topUsers) {
            Map<Integer, Integer> similarRatings = userRatings.get(similarUser);
            for (Map.Entry<Integer, Integer> entry : similarRatings.entrySet()) {
                int songId = entry.getKey();
                int rating = entry.getValue();
                if (!targetRatings.containsKey(songId)) { // Only recommend songs the target user hasn't rated
                    songScores.put(songId, songScores.getOrDefault(songId, 0.0) + rating * similarities.get(similarUser));
                }
            }
        }

        // Sort recommended songs by score and return the top N
        return songScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue())) // Sort by score descending
                .limit(numRecommendations)
                .map(Map.Entry::getKey)
                .toList();
    }

    // Calculate cosine similarity between two users' ratings
    private static double cosineSimilarity(Map<Integer, Integer> ratings1, Map<Integer, Integer> ratings2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int songId : ratings1.keySet()) {
            int rating1 = ratings1.get(songId);
            norm1 += rating1 * rating1;

            if (ratings2.containsKey(songId)) {
                int rating2 = ratings2.get(songId);
                dotProduct += rating1 * rating2;
            }
        }

        for (int rating : ratings2.values()) {
            norm2 += rating * rating;
        }

        norm1 = Math.sqrt(norm1);
        norm2 = Math.sqrt(norm2);

        return (norm1 > 0 && norm2 > 0) ? dotProduct / (norm1 * norm2) : 0.0;
    }
}
