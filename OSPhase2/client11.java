import java.io.*;
import java.net.*;
import java.nio.file.*;


public class client11 {

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
            InputStream input_stream = client.getInputStream();

            //connected :)
            System.out.println("Connected with server " + client.getInetAddress() + ":" + client.getPort());
            //identify client to server
            to_server.println("Client1");

            //login.sh
            System.out.println("running login script");
            run_script("./login.sh", "Login Shell Script Output");

            System.out.println("running check script");
            run_script("./check.sh", "Check Shell Script Output");

            while (true) {
                System.out.println("\nrequesting system info from server");
                to_server.println("SYSTEM_INFO");

                //reads response
                String response = from_server.readLine();
                if (response.startsWith("ERROR:")) {
                    System.out.println("response: " + response);
                } else if (response.startsWith("FILE_SIZE:")) {
                    //see file size from server response
                    long file_size = Long.parseLong(response.substring(10));
                    System.out.println("server file size: " + file_size + " bytes");
                    
                    String file_name = "received_system_info_client1.txt";
                    System.out.println("got file :) " + file_name);
                    
                    FileOutputStream file_out = new FileOutputStream(file_name);
                    byte[] buffer = new byte[4096];
                    long total_bytes_read = 0;
                    int bytes_read;
                    
                    
                    //read file until complete
                    while (total_bytes_read < file_size && 
                           (bytes_read = input_stream.read(buffer, 0, (int)Math.min(buffer.length, file_size - total_bytes_read))) != -1) {
                        file_out.write(buffer, 0, bytes_read);
                        total_bytes_read += bytes_read;
                        System.out.println("received bytes:  " + totalBytesRead + " of file total:  " + file_size + " bytes");
                    }
                    
                    file_out.close();
                    
                    //just verification to see if the file size matches tbe bytes we read (compelte or not)
                    if (total_bytes_read == file_size) {
                        System.out.println("file received :)");
                        System.out.println("saved as: " + file_name);
                        
                        System.out.println("\ncontent:");
                        Files.readAllLines(Paths.get(file_name)).forEach(System.out::println);
                    } else {
                        System.out.println("file not received fully ):");
                        System.out.println("received " + total_bytes_read + " out of total file bytes " + file_size);
                    }

                    File received_file = new File(file_name);
                    if (received_file.exists()) {
                        System.out.println("file exists: " + receivedFile.length() + " bytes");
                    } else {
                        System.out.println("file not found ):");
                    }
                }

                System.out.println("\nI am waiting 5 minutes before next request...");
                Thread.sleep(300000);
            }
        } catch (IOException | InterruptedException e) {
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


    private static void run_script(String script_path, String output_header) {
        try {
            ProcessBuilder pb = new ProcessBuilder(script_path);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            String line;
            System.out.println("\n" + output_header + ":");
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            
            int exit_code = process.waitFor();
            if (exit_code == 0) {
                System.out.println("script ran successfully :)");
            } else {
                System.out.println("script failed  :( with exit code: " + exit_code);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}