// import java.io.*;
// import java.net.*;
// import java.util.*;

// public class service extends Thread {
//     private Socket client_socket;
//     private PrintWriter to_client;
//     private BufferedReader from_client;
//     private long last_system_request;

//     //for synchronization
//     private static final Object system_info_lock = new Object();
//     private static List<ClientInfo> connected_clients = Collections.synchronizedList(new ArrayList<>());
//     private String client_id;
    
//     //------------------------------------------ For connection details ------------------------------------------
//     static class ClientInfo {
//         String client_id;
//         String ip_address;
//         long last_request_time;
//         int request_count;

//         public ClientInfo(String client_id, String ip_address) {
//             this.client_id = client_id;
//             this.ip_address = ip_address;
//             this.last_request_time = System.currentTimeMillis();
//             this.request_count = 0;
//         }
//     }
//     //-----------------------------------------------------------------------------------------------------------
//     public service(Socket client_socket) {
//         this.client_socket = client_socket;
//         this.last_system_request = System.currentTimeMillis();
//     }

//     @Override
//     public void run() {
//         try {
//             to_client = new PrintWriter(client_socket.getOutputStream(), true);
//             from_client = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
//             OutputStream out_stream = client_socket.getOutputStream();

//             //get client id and store some info
//             client_id = from_client.readLine();
//             System.out.println("a new client connected =): " + client_id);
//             ClientInfo client_info = new ClientInfo(client_id, client_socket.getInetAddress().getHostAddress());
//             connected_clients.add(client_info);
//             display_connected_clients();
//             run_network_script();

//             String client_request;
//             //if client requests server info. handdle it
//             while ((client_request = from_client.readLine()) != null) {
//                 if (client_request.equals("SYSTEM_INFO")) {
//                     System.out.println("received system info request from " + client_id);
//                     handle_system_info_request(client_info, out_stream);
//                 }
//             }
//         } catch (IOException e) {
//             e.printStackTrace();
//         } finally {
//             cleanup_connection();
//         }
//     }

//     private void run_network_script() {
//         try {
//             System.out.println("running the network script for client: " + client_id);
//             ProcessBuilder process_builder = new ProcessBuilder("./network.sh", 
//                 client_socket.getInetAddress().getHostAddress());
//             process_builder.redirectErrorStream(true);
//             Process script_process = process_builder.start();
            
//             //get the output of the script and log it
//             BufferedReader script_reader = new BufferedReader(
//                 new InputStreamReader(script_process.getInputStream()));
//             String output_line;
//             while ((output_line = script_reader.readLine()) != null) {
//                 System.out.println("script output for " + client_id + ": " + output_line);
//             }
//             script_process.waitFor();
//             System.out.println("script completed for client: " + client_id +" :)");
//         } catch (IOException | InterruptedException e) {
//             e.printStackTrace();
//         }
//     }

//     private void handle_system_info_request(ClientInfo client_info, OutputStream out_stream) {
//         long current_time = System.currentTimeMillis();
//         if (current_time - last_system_request < 300000) {
//             System.out.println("rejecting request from " + client_id + " bc of 5-minute cooldown :(");
//             to_client.println("ERROR: wait 5 minutes between system info requests");
//             return;
//         }
    
//         synchronized (system_info_lock) {
//             try {
                
//                 ProcessBuilder process_builder = new ProcessBuilder("./system.sh");
//                 process_builder.redirectErrorStream(true);
//                 System.out.println("will execute shell script system.sh");
//                 Process script_process = process_builder.start();
                
//                 //creating a file with client id in name
//                 String file_name = "system_info_" + client_info.client_id + ".txt";
//                 System.out.println("file: " + file_name);
//                 //take script output
//                 StringBuilder script_output = new StringBuilder();
//                 BufferedReader script_reader = new BufferedReader(
//                     new InputStreamReader(script_process.getInputStream()));
//                 String output_line;
//                 while ((output_line = script_reader.readLine()) != null) {
//                     script_output.append(output_line).append("\n");
//                     System.out.println("system.sh output: " + output_line);
//                 }
                
//                 //save the output in file
//                 FileOutputStream file_out = new FileOutputStream(file_name);
//                 file_out.write(script_output.toString().getBytes());
//                 file_out.close();
    
//                 File info_file = new File(file_name);
//                 if (info_file.length() == 0) {
//                     System.out.println("Warning: file was empty! :(");
//                     to_client.println("ERROR: there was no system info file was empty :(");
//                     return;
//                 }
    
//                 System.out.println("sending file of size: " + info_file.length() + " to client: " + client_id);
//                 to_client.println("FILE_SIZE:" + info_file.length());
                
//                 //file transferring
//                 FileInputStream file_in = new FileInputStream(info_file);
//                 byte[] transfer_buffer = new byte[4096];
//                 int bytes_read;
//                 long total_bytes_sent = 0;
                
//                 while ((bytes_read = file_in.read(transfer_buffer)) != -1) {
//                     out_stream.write(transfer_buffer, 0, bytes_read);
//                     total_bytes_sent += bytes_read;
//                     System.out.println("sent " + total_bytes_sent + " bytes");
//                 }
//                 out_stream.flush();
//                 file_in.close();
    
//                 System.out.println("file was successfully sent to " + client_id + " :)");
//                 //reupdate some client info 
//                 client_info.last_request_time = current_time;
//                 client_info.request_count++;
//                 last_system_request = current_time;
                
//                 display_connected_clients();
    
//             } catch (IOException e) {
//                 System.out.println("error in the handle_system_info_request: " + e.getMessage());
//                 e.printStackTrace();
//                 to_client.println("ERROR: fail to get system info :(");
//             }
//         }
//     }

//     private void display_connected_clients() {
//         System.out.println("\n----------- Connected Clients -----------");
//         synchronized(connected_clients) {
//             for (ClientInfo client : connected_clients) {
//                 System.out.println("Client ID: " + client.client_id);
//                 System.out.println("IP: " + client.ip_address);
//                 System.out.println("Last Request: " + new Date(client.last_request_time));
//                 System.out.println("Total Requests: " + client.request_count);
//                 System.out.println("------------------------");
//             }
//         }
//     }

//     private void cleanup_connection() {
//         System.out.println("disconnecting client: " + client_id);
//         connected_clients.removeIf(client -> client.client_id.equals(client_id));
//         display_connected_clients();
//         try {
//             if (to_client != null) to_client.close();
//             if (from_client != null) from_client.close();
//             if (client_socket != null) client_socket.close();
//         } catch (IOException e) {
//             e.printStackTrace();
//         }
//     }
// }

import java.io.*;
import java.net.*;
import java.util.*;

public class service extends Thread {
    private Socket client_socket;
    private PrintWriter to_client;
    private BufferedReader from_client;
    private long last_system_request;

    //for synchronization
    private static final Object system_info_lock = new Object();
    private static List<ClientInfo> connected_clients = Collections.synchronizedList(new ArrayList<>());
    private String client_id;
    
    //------------------------------------------ For connection details ------------------------------------------
    static class ClientInfo {
        String client_id;
        String ip_address;
        long last_request_time;
        int request_count;

        public ClientInfo(String client_id, String ip_address) {
            this.client_id = client_id;
            this.ip_address = ip_address;
            this.last_request_time = System.currentTimeMillis();
            this.request_count = 0;
        }
    }
    //-----------------------------------------------------------------------------------------------------------
    public service(Socket client_socket) {
        this.client_socket = client_socket;
        this.last_system_request = System.currentTimeMillis();
    }

    @Override
    public void run() {
        try {
            to_client = new PrintWriter(client_socket.getOutputStream(), true);
            from_client = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
            OutputStream out_stream = client_socket.getOutputStream();

            //get client id and store some info
            client_id = from_client.readLine();
            System.out.println("a new client connected =): " + client_id);
            ClientInfo client_info = new ClientInfo(client_id, client_socket.getInetAddress().getHostAddress());
            connected_clients.add(client_info);
            display_connected_clients();
            run_network_script();

            String client_request;
            //if client requests server info. handdle it
            while ((client_request = from_client.readLine()) != null) {
                if (client_request.equals("SYSTEM_INFO")) {
                    System.out.println("received system info request from " + client_id);
                    handle_system_info_request(client_info, out_stream);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            cleanup_connection();
        }
    }

    private void run_network_script() {
        try {
            System.out.println("running the network script for client: " + client_id);
            ProcessBuilder process_builder = new ProcessBuilder("./network.sh", 
                client_socket.getInetAddress().getHostAddress());
            process_builder.redirectErrorStream(true);
            Process script_process = process_builder.start();
            
            //get the output of the script and log it
            BufferedReader script_reader = new BufferedReader(
                new InputStreamReader(script_process.getInputStream()));
            String output_line;
            while ((output_line = script_reader.readLine()) != null) {
                System.out.println("script output for " + client_id + ": " + output_line);
            }
            script_process.waitFor();
            System.out.println("script completed for client: " + client_id +" :)");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void handle_system_info_request(ClientInfo client_info, OutputStream out_stream) {
        long current_time = System.currentTimeMillis();
        if (current_time - last_system_request < 300000) {
            System.out.println("rejecting request from " + client_id + " bc of 5-minute cooldown :(");
            to_client.println("ERROR: wait 5 minutes between system info requests");
            return;
        }
    
        String file_name = "system_info_" + client_info.client_id + ".txt";
        File info_file = null;
    
        synchronized (system_info_lock) {
            try {
                ProcessBuilder process_builder = new ProcessBuilder("./system.sh");
                process_builder.redirectErrorStream(true);
                System.out.println("will execute shell script system.sh");
                Process script_process = process_builder.start();
                
                //creating a file with client id in name
                System.out.println("file: " + file_name);
                //take script output
                StringBuilder script_output = new StringBuilder();
                BufferedReader script_reader = new BufferedReader(
                    new InputStreamReader(script_process.getInputStream()));
                String output_line;
                while ((output_line = script_reader.readLine()) != null) {
                    script_output.append(output_line).append("\n");
                    System.out.println("system.sh output: " + output_line);
                }
                
                //save the output in file
                FileOutputStream file_out = new FileOutputStream(file_name);
                file_out.write(script_output.toString().getBytes());
                file_out.close();
    
                info_file = new File(file_name);
                if (info_file.length() == 0) {
                    System.out.println("Warning: file was empty! :(");
                    to_client.println("ERROR: there was no system info file was empty :(");
                    return;
                }
    
                System.out.println("sending file of size: " + info_file.length() + " to client: " + client_id);
                to_client.println("FILE_SIZE:" + info_file.length());
            } catch (IOException e) {
                System.out.println("error in creating system info file: " + e.getMessage());
                e.printStackTrace();
                to_client.println("ERROR: fail to get system info :(");
                return;
            }
        }
    
        // File transfer outside synchronized block
        try {
            if (info_file != null && info_file.exists()) {
                FileInputStream file_in = new FileInputStream(info_file);
                BufferedOutputStream buffered_out = new BufferedOutputStream(out_stream);
                byte[] transfer_buffer = new byte[4096];
                int bytes_read;
                long total_bytes_sent = 0;
                
                while ((bytes_read = file_in.read(transfer_buffer)) != -1) {
                    buffered_out.write(transfer_buffer, 0, bytes_read);
                    total_bytes_sent += bytes_read;
                    System.out.println("sent " + total_bytes_sent + " bytes");
                    buffered_out.flush(); // Ensure data is sent immediately
                }
                
                file_in.close();
                System.out.println("file was successfully sent to " + client_id + " :)");
                
                // Update client info after successful transfer
                synchronized (system_info_lock) {
                    client_info.last_request_time = current_time;
                    client_info.request_count++;
                    last_system_request = current_time;
                    display_connected_clients();
                }
            }
        } catch (IOException e) {
            System.out.println("error in file transfer: " + e.getMessage());
            e.printStackTrace();
            to_client.println("ERROR: fail to transfer system info :(");
        }
    }

    private void display_connected_clients() {
        System.out.println("\n----------- Connected Clients -----------");
        synchronized(connected_clients) {
            for (ClientInfo client : connected_clients) {
                System.out.println("Client ID: " + client.client_id);
                System.out.println("IP: " + client.ip_address);
                System.out.println("Last Request: " + new Date(client.last_request_time));
                System.out.println("Total Requests: " + client.request_count);
                System.out.println("------------------------");
            }
        }
    }

    private void cleanup_connection() {
        System.out.println("disconnecting client: " + client_id);
        connected_clients.removeIf(client -> client.client_id.equals(client_id));
        display_connected_clients();
        try {
            if (to_client != null) to_client.close();
            if (from_client != null) from_client.close();
            if (client_socket != null) client_socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}