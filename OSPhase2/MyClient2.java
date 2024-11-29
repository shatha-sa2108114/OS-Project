import java.io.*;
import java.net.*;

public class MyClient2 {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 1300;
    private static volatile boolean running = true;

    public static void main(String[] args) {
        Socket client = null;
        BufferedReader fromServer = null;
        BufferedReader fromUser = null;
        PrintWriter toServer = null;

        try {
            client = new Socket(SERVER_ADDRESS, SERVER_PORT);
            fromServer = new BufferedReader(new InputStreamReader(client.getInputStream()));
            fromUser = new BufferedReader(new InputStreamReader(System.in));
            toServer = new PrintWriter(client.getOutputStream(), true);

            System.out.println("Connected with server " + client.getInetAddress() + ":" + client.getPort());

            // Identify to server
            toServer.println("Client2");

            // Run search.sh script
            runScript("./search.sh", "Search Script Output");

            // Run clientinfo.sh script with timeout
            Thread clientInfoThread = new Thread(() -> {
                try {
                    ProcessBuilder pb = new ProcessBuilder("./clientinfo.sh");
                    pb.redirectErrorStream(true);
                    Process process = pb.start();
                    
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                    
                    System.out.println("\nClient Info Script Output:");
                    String line;
                    while (running && (line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                    
                    process.destroy();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            clientInfoThread.start();

            // Handle system info requests
            while (running) {
                toServer.println("SYSTEM_INFO");

                String line;
                boolean reading = false;
                while ((line = fromServer.readLine()) != null) {
                    if (line.equals("BEGIN_SYSTEM_INFO")) {
                        reading = true;
                        continue;
                    }
                    if (line.equals("END_SYSTEM_INFO")) {
                        break;
                    }
                    if (reading) {
                        System.out.println(line);
                    }
                    if (line.startsWith("ERROR:")) {
                        System.out.println(line);
                        break;
                    }
                }

                // Wait 5 minutes before next request
                Thread.sleep(300000);
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Error: " + e);
        } finally {
            running = false;
            try {
                if (fromServer != null) fromServer.close();
                if (fromUser != null) fromUser.close();
                if (toServer != null) toServer.close();
                if (client != null) client.close();
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }

    private static void runScript(String scriptPath, String outputHeader) {
        try {
            ProcessBuilder pb = new ProcessBuilder(scriptPath);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            String line;
            System.out.println("\n" + outputHeader + ":");
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            
            int exitCode = process.waitFor();
            System.out.println(scriptPath + " completed with exit code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}