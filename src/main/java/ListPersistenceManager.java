
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ListPersistenceManager {
    private final List<String> itemList;
    private final String filename;

    public ListPersistenceManager(String filename) {
        this.filename = filename;
        this.itemList = loadListFromFile();
    }

    public void addItemToList(String item) {
        itemList.add(item);
        saveListToFile();
    }

    public boolean containsItem(String item) {
        return itemList.contains(item);
    }

    private final static Logger _logger = Logger.named("ListPersistenceManager");

    private List<String> loadListFromFile() {
        List<String> list = new ArrayList<>();
        File file = new File(filename);
        if (!file.exists()) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("");
            } catch (IOException e) {
                _logger.debug("Error writing file: " + e.getMessage());
            }
        } else {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    list.add(line);
                }
            } catch (FileNotFoundException e) {
                _logger.debug("Impossible: " + e.getMessage());
            } catch (IOException e) {
                _logger.debug("Couldn't read dict: " + e.getMessage());
            } catch (Exception e) {
                _logger.debug("Error reading " + filename + ": " + e.getMessage());
            }
        }
        return list;
    }

    private void saveListToFile() {
        try (OutputStream outputStream = new FileOutputStream(getFile())) {
            for (String item : itemList) {
                String line = item + System.lineSeparator();
                outputStream.write(line.getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File getFile() {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(filename).getFile());
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }
}
