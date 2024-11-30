import java.io.*;
import java.net.*;
import java.nio.file.*;

public class client11 {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 1300;

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
            InputStream inStream = client.getInputStream();

            System.out.println("Connected with server " + client.getInetAddress() + ":" + client.getPort());

            toServer.println("Client1");

            System.out.println("Starting login script...");
            runScript("./login.sh", "Login Script Output");

            System.out.println("Starting check script...");
            runScript("./check.sh", "Check Script Output");

            while (true) {
                System.out.println("\nRequesting system information from server...");
                toServer.println("SYSTEM_INFO");

                String response = fromServer.readLine();
                if (response.startsWith("ERROR:")) {
                    System.out.println("Server response: " + response);
                } else if (response.startsWith("FILE_SIZE:")) {
                    long fileSize = Long.parseLong(response.substring(10));
                    System.out.println("Server is sending file of size: " + fileSize + " bytes");
                    
                    String fileName = "received_system_info_client1.txt";
                    System.out.println("Creating file: " + fileName);
                    
                    FileOutputStream fileOut = new FileOutputStream(fileName);
                    byte[] buffer = new byte[4096];
                    long totalBytesRead = 0;
                    int bytesRead;
                    
                    System.out.println("Receiving file data...");
                    
                    while (totalBytesRead < fileSize && 
                           (bytesRead = inStream.read(buffer, 0, (int)Math.min(buffer.length, fileSize - totalBytesRead))) != -1) {
                        fileOut.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                        System.out.println("Received " + totalBytesRead + " of " + fileSize + " bytes");
                    }
                    
                    fileOut.close();
                    
                    if (totalBytesRead == fileSize) {
                        System.out.println("File received successfully!");
                        System.out.println("Saved as: " + fileName);
                        
                        System.out.println("\nFile contents:");
                        Files.readAllLines(Paths.get(fileName)).forEach(System.out::println);
                    } else {
                        System.out.println("Warning: File transfer incomplete!");
                        System.out.println("Received " + totalBytesRead + " bytes out of " + fileSize);
                    }

                    File receivedFile = new File(fileName);
                    if (receivedFile.exists()) {
                        System.out.println("Verified file on disk: " + receivedFile.length() + " bytes");
                    } else {
                        System.out.println("Error: File not found on disk!");
                    }
                }

                System.out.println("\nWaiting 5 minutes before next request...");
                Thread.sleep(300000);
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Error: " + e);
        } finally {
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
            if (exitCode == 0) {
                System.out.println("Script executed successfully");
            } else {
                System.out.println("Script failed with exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}

