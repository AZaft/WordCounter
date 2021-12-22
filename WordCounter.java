import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class WordCounter {

    public static final Path FOLDER_OF_TEXT_FILES  = Paths.get("./textfiles"); // path to the folder where input text files are located
    public static final Path WORD_COUNT_TABLE_FILE = Paths.get("./output/output.txt"); // path to the output plain-text (.txt) file
    public static final int  NUMBER_OF_THREADS     = 2;                // max. number of threads to spawn
    private static TreeMap<String, TreeMap<String,Integer>> mapFiles = new TreeMap<>();
    private static int longestWordLength;
    private static int longestColumnLength;

    public static void main(String... args) throws ExecutionException, InterruptedException {
        File fileDirectory = FOLDER_OF_TEXT_FILES.toFile();
        File[] files = fileDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".txt");
            }
        });
        for (int i = 0; i < files.length; i++) {
            mapFiles.put(files[i].getName(), new TreeMap<>());
        }

        if (NUMBER_OF_THREADS == 0 || NUMBER_OF_THREADS == 1) {
            for (File f : files) {
                readFile file = new readFile(f);
                file.run();
            }
            writeFile f = new writeFile();
            f.run();
        } else {
            ArrayList<Future> futures = new ArrayList<Future>();
            ExecutorService fileReaders = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
            for (int i = 0; i < files.length; i++) {
                futures.add(fileReaders.submit(new readFile(files[i])));
            }
            for(Future f: futures){
                f.get();
            }
            fileReaders.execute(new writeFile());
            fileReaders.shutdown();
        }
    }

    public static class writeFile implements Runnable{
        @Override
        public void run() {
            StringBuilder output = new StringBuilder();
            TreeSet<String> setOfWords = new TreeSet<>();

            for(TreeMap words : mapFiles.values()){
                for(Object w : words.keySet()){
                    setOfWords.add((String)w);
                }
            }

            if(!setOfWords.isEmpty()){
                output.append(String.format("%-"+ (longestWordLength+1) + "s", ""));
                for(String n: mapFiles.keySet()){
                    output.append(String.format("%-"+ longestColumnLength + "s",n.replaceFirst("[.][^.]+$", "")));
                }
                output.append(String.format("total")).append(System.lineSeparator());
            }

            int count;
            int totalCount;
            for(String s: setOfWords){
                totalCount = 0;
                String line = String.format("%-"+ (longestWordLength+1) + "s",s);
                for(TreeMap words : mapFiles.values()){
                    if(words.containsKey(s)) {
                        count = (int) words.get(s);
                        totalCount += count;
                    }else
                        count = 0;
                    line += String.format("%-"+ longestColumnLength + "d",count);
                }
                line += totalCount;
                output.append(line);
                output.append(System.lineSeparator());
            }

            FileWriter writer = null;
            try {
                writer = new FileWriter(WORD_COUNT_TABLE_FILE.toFile());
                writer.write(output.toString());
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class readFile implements Runnable {
        public File fileToRead;
        readFile(File f){
            fileToRead = f;
        }
        @Override
        public void run() {
            if(fileToRead.getName().length() > longestColumnLength)
                longestColumnLength = fileToRead.getName().length();
            String fileContents = null;
            try {
                fileContents = new String(Files.readAllBytes(fileToRead.toPath()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            String[] words = fileContents.split("\\s");
            for(int j = 0; j < words.length;j++){
                words[j] = words[j].replaceAll("[^a-zA-Z]", "").toLowerCase();
                if(words[j].isEmpty() || words[j].contains(" "))
                    continue;
                if(words[j].length() > longestWordLength)
                    longestWordLength = words[j].length();
                TreeMap thisFile = mapFiles.get(fileToRead.getName());
                int wordCount = 0;
                thisFile.putIfAbsent(words[j], wordCount);
                int currentCount = (int)mapFiles.get(fileToRead.getName()).get(words[j]);
                thisFile.put(words[j], ++currentCount);
            }
        }
    }
}
