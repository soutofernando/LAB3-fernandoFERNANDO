import java.io.*;
import java.util.*;
import java.util.concurrent.Semaphore;

public class FileSimilarity {
    private static final Semaphore mutex = new Semaphore(1);
    private static final Map<String, List<Long>> fileFingerprints = new HashMap<>();

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java Sum filepath1 filepath2 filepathN");
            System.exit(1);
        }

        // Create a map to store the fingerprint for each file

        // Calculate the fingerprint for each file
        List<Thread> threads = new ArrayList<>();
        for (String path : args) {
            Thread thread = new Thread(() -> {
                try{
                    List<Long> fingerprint = fileSum(path);
                    mutex.acquire();
                    try {
                        fileFingerprints.put(path, fingerprint);
                    } finally {
                        mutex.release();
                    }
                }
                
                catch(IOException | InterruptedException e){
                    e.printStackTrace();
                    
                }
            });
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        // Compare each pair of files
        List<Thread> similarityThreads = new ArrayList<>();
        List<String> filePaths = new ArrayList<>(fileFingerprints.keySet());
        for (int i = 0; i < filePaths.size(); i++) {
            for (int j = i + 1; j < filePaths.size(); j++) {
                String file1 = filePaths.get(i);
                String file2 = filePaths.get(j);

                Thread simThread = new Thread(() -> {
                    try {
                        List<Long> fingerprint1, fingerprint2;
                        // List<Long> fingerprint1 = fileFingerprints.get(file1);
                        // List<Long> fingerprint2 = fileFingerprints.get(file2);

                        mutex.acquire();
                        try{
                            fingerprint1 = fileFingerprints.get(file1);
                            fingerprint2 = fileFingerprints.get(file2);
                        }finally {
                            mutex.release();
                        }
                        if ( fingerprint1 != null && fingerprint2 != null) {
                            float similarityScore = similarity(fingerprint1, fingerprint2);
                            System.out.println("Similarity between " + file1 + " and " + file2 + ": " + (similarityScore * 100) + "%");
                            
                        }
                    } catch (Exception e) {
                        // TODO: handle exception
                        e.printStackTrace();
                    }
                });
                
                similarityThreads.add(simThread);
                simThread.start();
            }
        }
    }

    private static List<Long> fileSum(String filePath) throws IOException {
        File file = new File(filePath);
        List<Long> chunks = new ArrayList<>();
        try (FileInputStream inputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[100];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                long sum = sum(buffer, bytesRead);
                chunks.add(sum);
            }
        }
        return chunks;
    }

    private static long sum(byte[] buffer, int length) {
        long sum = 0;
        for (int i = 0; i < length; i++) {
            sum += Byte.toUnsignedInt(buffer[i]);
        }
        return sum;
    }

    private static float similarity(List<Long> base, List<Long> target) {
        int counter = 0;
        List<Long> targetCopy = new ArrayList<>(target);

        for (Long value : base) {
            if (targetCopy.contains(value)) {
                counter++;
                targetCopy.remove(value);
            }
        }

        return (float) counter / base.size();
    }
}
