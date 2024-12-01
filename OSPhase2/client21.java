import java.io.*;
import java.net.*;
import java.nio.file.*;

public class client21 {
    public static void main(String[] args) {
        Socket client = null;
        BufferedReader from_server = null;
        BufferedReader from_user = null;
        PrintWriter to_server = null;

        try {
            client = new Socket("localhost", 1300);
            from_server = new BufferedReader(new InputStreamReader(client.getInputStream()));
            from_user = new BufferedReader(new InputStreamReader(System.in));
            to_server = new PrintWriter(client.getOutputStream(), true);
            InputStream in_stream = client.getInputStream(); //byte stream for file transfer

            //connected
            System.out.println("Connected to server " + client.getInetAddress() + ":" + client.getPort());
            to_server.println("Client2");

            //running search shell script
            runScript("./search.sh", "Search Script Output");

            //running clientinfo.sh but in seperate thread
            Thread clientInfoThread = new Thread(() -> {
                try {
                    ProcessBuilder pb = new ProcessBuilder("./clientinfo.sh");
                    pb.redirectErrorStream(true); //merge stdrr with stdout
                    Process process = pb.start(); //start thread
                    
                    //read script output 
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                    
                    System.out.println("\nClient info shell script output:");
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            clientInfoThread.setDaemon(true); //to make thread a background thread
            clientInfoThread.start();

            while (true) {
                try {
                    //client requests system info
                    System.out.println("\nrequesting system info from server");
                    to_server.println("SYSTEM_INFO");

                    //reads response
                    String response = from_server.readLine();
                    // if not response, server closed conn
                    if (response == null) {
                        System.out.println("connection closed");
                        break;
                    }
                    // if response, print it 
                    if (response.startsWith("ERROR:")) {
                        System.out.println("response: " + response);
                    } else if (response.startsWith("FILE_SIZE:")) {
                        //get file size from server
                        long file_size = Long.parseLong(response.substring(10));
                        System.out.println("file size: " + file_size + " bytes");
                        String file_name = "received_system_info_client2.txt";
                        System.out.println("got file :)  " + file_name);
                        
                        FileOutputStream file_out = new FileOutputStream(file_name);
                        byte[] buffer = new byte[4096];
                        long totalBytesRead = 0;
                        int bytesRead;

                        //read file data until complete      
                        while (totalBytesRead < file_size && 
                               (bytesRead = in_stream.read(buffer, 0, (int)Math.min(buffer.length, file_size - totalBytesRead))) != -1) {
                            file_out.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;
                            System.out.println("received bytes:  " + totalBytesRead + " of file total:  " + file_size + " bytes");
                        }
                        
                        file_out.close();
                        
                        //just verification to see if the file size matches tbe bytes we read (compelte or not)
                        if (totalBytesRead == file_size) {
                            System.out.println("file received :)");
                            System.out.println("saved as " + file_name);
                            
                            System.out.println("\ncontent:");
                            Files.readAllLines(Paths.get(file_name)).forEach(System.out::println);
                        } else {
                            System.out.println("file not received fully ):");
                            System.out.println("received " + totalBytesRead + " bytes out of file total bytes: " + file_size);
                        }

                        File receivedFile = new File(file_name);
                        if (receivedFile.exists()) {
                            System.out.println("file exists: " + receivedFile.length() + " bytes");
                        } else {
                            System.out.println("file not found ):");
                        }
                    }

                    System.out.println("\nI am waiting 5 minutes before next request...");
                    Thread.sleep(300000);
                } catch (IOException | InterruptedException e) {
                    System.out.println("Error: " + e);
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Error: " + e);
        } finally {
            try {
                if (from_server != null) from_server.close();
                if (from_user != null) from_user.close();
                if (to_server != null) to_server.close();
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
                System.out.println("script ran successfully :)");
            } else {
                System.out.println("script failed :( with exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
