
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class CSVReader {
    public static void main(String[] args) {
        String pathToCsv = "service-names-port-numbers.csv";
        String line;
        Map<Integer, String> portMap = new HashMap<>();
        TreeSet<Integer> sortedKeys = new TreeSet<>();
        Jedis jedis = null;

        try (BufferedReader br = new BufferedReader(new FileReader(pathToCsv))) {
            while ((line = br.readLine()) != null) {
                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] columns = line.split(",");
                if (columns.length >= 4) {
                    String keyString = columns[1];
                    String value = columns[3];

                    try {
                        int key = Integer.parseInt(keyString);
                        if (!portMap.containsKey(key)) {
                            portMap.put(key, value);
                            sortedKeys.add(key);
                        }
                    } catch (NumberFormatException e) {
                        // Handle the case where key is not a valid integer
                    }
                } else {
                    // Handle the case where the line does not have enough columns
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            // Connect to Redis
            jedis = new Jedis("localhost");

            // Populate Redis with portMap data
            for (Map.Entry<Integer, String> entry : portMap.entrySet()) {
                jedis.set(entry.getKey().toString(), entry.getValue());
            }

            // Retrieve and print data from Redis using sorted keys
            for (Integer key : sortedKeys) {
                String value = jedis.get(key.toString());
                System.out.println("Port: " + key + " " + value);
            }
        } catch (JedisConnectionException e) {
            System.out.println("Could not connect to Redis: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Issue: " + e.getMessage());
        } finally {
            // Close the Redis connection in the finally block
            if (jedis != null) {
                jedis.close();
            }
        }
    }
}