import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public class service2 extends Thread {
    private Socket clientSocket;
    private PrintWriter to_client;
    private BufferedReader from_client;
    private long lastSystemInfoRequest;
    private static final Object systemInfoLock = new Object();
    private static List<ClientInfo> connectedClients = Collections.synchronizedList(new ArrayList<>());
    private String clientId;
    
    static class ClientInfo {
        String clientId;
        String ipAddress;
        long lastRequestTime;
        int requestCount;

        public ClientInfo(String clientId, String ipAddress) {
            this.clientId = clientId;
            this.ipAddress = ipAddress;
            this.lastRequestTime = System.currentTimeMillis();
            this.requestCount = 0;
        }
    }

    public service2(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.lastSystemInfoRequest = System.currentTimeMillis();
    }

    @Override
    public void run() {
        try {
            to_client = new PrintWriter(clientSocket.getOutputStream(), true);
            from_client = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            OutputStream outStream = clientSocket.getOutputStream();

            clientId = from_client.readLine();
            System.out.println("New client connected: " + clientId);
            ClientInfo client = new ClientInfo(clientId, clientSocket.getInetAddress().getHostAddress());
            connectedClients.add(client);
            displayConnectedClients();

            runNetworkScript();

            String request;
            while ((request = from_client.readLine()) != null) {
                if (request.equals("SYSTEM_INFO")) {
                    System.out.println("Received system info request from " + clientId);
                    handleSystemInfoRequest(client, outStream);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private void runNetworkScript() {
        try {
            System.out.println("Running network script for client: " + clientId);
            ProcessBuilder pb = new ProcessBuilder("./network.sh", 
                clientSocket.getInetAddress().getHostAddress());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Network script output for " + clientId + ": " + line);
            }
            process.waitFor();
            System.out.println("Network script completed for client: " + clientId);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // private void handleSystemInfoRequest(ClientInfo client, OutputStream outStream) {
    //     long currentTime = System.currentTimeMillis();
    //     if (currentTime - lastSystemInfoRequest < 300000) {
    //         System.out.println("Rejecting request from " + clientId + ": 5-minute cooldown not elapsed");
    //         to_client.println("ERROR: Please wait 5 minutes between system info requests");
    //         return;
    //     }

    //     synchronized (systemInfoLock) {
    //         try {
    //             System.out.println("Preparing system info for client: " + clientId);
    //             ProcessBuilder pb = new ProcessBuilder("./system.sh");
    //             pb.redirectErrorStream(true);
    //             Process process = pb.start();
    //             process.waitFor();

    //             String fileName = "system_info_" + client.clientId + ".txt";
    //             System.out.println("Creating file: " + fileName);

    //             FileOutputStream fileOut = new FileOutputStream(fileName);
    //             Files.copy(Paths.get("disk_info.log"), fileOut);
    //             fileOut.write("\n".getBytes());
    //             Files.copy(Paths.get("mem_cpu_info.log"), fileOut);
    //             fileOut.close();

    //             File file = new File(fileName);
    //             System.out.println("Sending file size (" + file.length() + " bytes) to client: " + clientId);
    //             to_client.println("FILE_SIZE:" + file.length());
                
    //             byte[] buffer = new byte[4096];
    //             FileInputStream fileIn = new FileInputStream(file);
    //             int bytesRead;
    //             long totalBytesSent = 0;
                
    //             while ((bytesRead = fileIn.read(buffer)) != -1) {
    //                 outStream.write(buffer, 0, bytesRead);
    //                 totalBytesSent += bytesRead;
    //             }
    //             outStream.flush();
    //             fileIn.close();

    //             System.out.println("File sent successfully to " + clientId + ". Total bytes sent: " + totalBytesSent);

    //             client.lastRequestTime = currentTime;
    //             client.requestCount++;
    //             lastSystemInfoRequest = currentTime;
                
    //             displayConnectedClients();

    //         } catch (IOException | InterruptedException e) {
    //             System.out.println("Error sending file to " + clientId + ": " + e.getMessage());
    //             e.printStackTrace();
    //             to_client.println("ERROR: Failed to get system information");
    //         }
    //     }
    // }
    private void handleSystemInfoRequest(ClientInfo client, OutputStream outStream) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSystemInfoRequest < 300000) {
            System.out.println("Rejecting request from " + clientId + ": 5-minute cooldown not elapsed");
            to_client.println("ERROR: Please wait 5 minutes between system info requests");
            return;
        }
    
        synchronized (systemInfoLock) {
            try {
                System.out.println("Preparing system info for client: " + clientId);
                
                // Run system.sh and capture output directly
                ProcessBuilder pb = new ProcessBuilder("./system.sh");
                pb.redirectErrorStream(true);
                System.out.println("Starting system.sh execution...");
                Process process = pb.start();
                
                // Create the output file directly from process output
                String fileName = "system_info_" + client.clientId + ".txt";
                System.out.println("Creating file: " + fileName);
                
                StringBuilder output = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    System.out.println("system.sh output: " + line);
                }
                
                // Write captured output to file
                FileOutputStream fileOut = new FileOutputStream(fileName);
                fileOut.write(output.toString().getBytes());
                fileOut.close();
    
                File file = new File(fileName);
                if (file.length() == 0) {
                    System.out.println("Warning: Generated file is empty!");
                    to_client.println("ERROR: No system information generated");
                    return;
                }
    
                System.out.println("Sending file size (" + file.length() + " bytes) to client: " + clientId);
                to_client.println("FILE_SIZE:" + file.length());
                
                // Send file content
                FileInputStream fileIn = new FileInputStream(file);
                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytesSent = 0;
                
                while ((bytesRead = fileIn.read(buffer)) != -1) {
                    outStream.write(buffer, 0, bytesRead);
                    totalBytesSent += bytesRead;
                    System.out.println("Sent " + totalBytesSent + " bytes...");
                }
                outStream.flush();
                fileIn.close();
    
                System.out.println("File sent successfully to " + clientId);
    
                client.lastRequestTime = currentTime;
                client.requestCount++;
                lastSystemInfoRequest = currentTime;
                
                displayConnectedClients();
    
            } catch (IOException e) {
                System.out.println("Error in handleSystemInfoRequest: " + e.getMessage());
                e.printStackTrace();
                to_client.println("ERROR: Failed to get system information");
            }
        }
    }
    

    private void displayConnectedClients() {
        System.out.println("\n=== Connected Clients ===");
        synchronized(connectedClients) {
            for (ClientInfo client : connectedClients) {
                System.out.println("Client ID: " + client.clientId);
                System.out.println("IP Address: " + client.ipAddress);
                System.out.println("Last Request: " + new Date(client.lastRequestTime));
                System.out.println("Total Requests: " + client.requestCount);
                System.out.println("------------------------");
            }
        }
    }

    private void cleanup() {
        System.out.println("Client disconnecting: " + clientId);
        connectedClients.removeIf(client -> client.clientId.equals(clientId));
        displayConnectedClients();
        try {
            if (to_client != null) to_client.close();
            if (from_client != null) from_client.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
