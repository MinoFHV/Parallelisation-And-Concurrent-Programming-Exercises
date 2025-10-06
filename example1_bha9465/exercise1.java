import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class exercise1
{

    // Default number of chunks if the user doesn't provide one
    private static final int DEFAULT_CHUNKS = 4;

    // Regex pattern to split words (non-letter, non-digit, non-apostrophe characters)
    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^\\p{L}\\p{Nd}']+");

    public static void main(String[] args) 
    {

        // Validate command-line arguments
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: java exercise1 <path-to-text-file> [numChunks]");
            System.exit(2);
        }

        // Validate input file
        Path input_path = Paths.get(args[0]);
        if (!Files.exists(input_path) || !Files.isReadable(input_path)) {
            System.err.println("Error: File does not exist or not readable: " + input_path.toAbsolutePath());
            System.exit(2);
        }

        // Validate numChunks argument
        int num_chunks = DEFAULT_CHUNKS;
        if (args.length == 2)
        {
            try
            {
                num_chunks = Integer.parseInt(args[1]);
                if (num_chunks <= 0) throw new NumberFormatException();
            }
            catch (NumberFormatException ex) 
            {
                System.err.println("Error: numChunks must be a positive integer.");
                System.exit(2);
            }

        }

        // Read all lines from the input file
        List<String> lines = new ArrayList<>();
        try
        {
            lines = Files.readAllLines(input_path, StandardCharsets.UTF_8);
        }
        catch (IOException ioe)
        {
            System.err.println("Error reading file: " + ioe.getMessage());
            System.exit(1);
        }

        // Handle empty file case
        if (lines.isEmpty())
        {
            System.out.println("The file is empty. No words to count.");
            return;
        }

        // Split lines into chunks for parallel processing
        num_chunks = Math.min(num_chunks, lines.size());
        List<List<String>> chunks = split_into_chunks(lines, num_chunks);

        // Create a thread pool and process each chunk in parallel
        ExecutorService executor = Executors.newFixedThreadPool(num_chunks);
        List<Future<ConcurrentHashMap<String, Integer>>> futures = new ArrayList<>();

        // Submit tasks for each chunk
        for (List<String> chunk : chunks) 
            futures.add(executor.submit(() -> count_words_in_chunk(chunk)));

        executor.shutdown();

        // Aggregate results from all chunks
        ConcurrentHashMap<String, Integer> total_counts = new ConcurrentHashMap<>();
        for (Future<ConcurrentHashMap<String, Integer>> f : futures)
        {

            // Wait for the task to complete and merge results
            try
            {
                ConcurrentHashMap<String, Integer> chunk_map = f.get();
                for (Map.Entry<String, Integer> e : chunk_map.entrySet())
                    total_counts.merge(e.getKey(), e.getValue(), Integer::sum);
            }
            catch (Exception e) 
            {
                System.err.println("Error during word count aggregation: " + e.getMessage());
                System.exit(1);
            }

        }

        // Print the final word counts in alphabetical order
        List<String> words = new ArrayList<>(total_counts.keySet());
        Collections.sort(words);
        for (String w : words)
            System.out.println(w + " : " + total_counts.get(w));

    }

    private static List<List<String>> split_into_chunks(List<String> lines, int n)
    {

        int total = lines.size();
        int base_size = total / n;
        int remainder = total % n;

        List<List<String>> result = new ArrayList<>();
        int start = 0;

        // Distribute lines into n chunks as evenly as possible
        for (int i = 0; i < n; i++)
        {

            int size = base_size + (i < remainder ? 1 : 0);
            int end = start + size;

            result.add(lines.subList(start, end));
            start = end;

        }

        return result;

    }

    private static ConcurrentHashMap<String, Integer> count_words_in_chunk(List<String> chunk)
    {

        // Map to hold word counts for this chunk
        ConcurrentHashMap<String, Integer> counts = new ConcurrentHashMap<>();
        for (String line : chunk)
        {

            // Skip empty lines
            if (line == null || line.isEmpty()) continue;

            // Count occurrences of each word
            String[] tokens = TOKEN_SPLIT.split(line);
            for (String token : tokens)
            {
                if (token.isEmpty()) continue;
                counts.merge(token, 1, Integer::sum);
            }
        }

        return counts;

    }

}
