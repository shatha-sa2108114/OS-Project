
// import java.io.BufferedReader;
// import java.io.IOException;
// import java.io.InputStreamReader;
// import java.io.PrintWriter;
// import java.net.Socket;

// public class Client1 {

// 	public static void main(String[] args) {

// 		Socket client = null;
// 		BufferedReader from_server = null;
// 		BufferedReader from_user = null;
// 		PrintWriter to_server = null;

// 		String serverInput, userInput;

// 		try{

// 			client=new Socket("localhost", 1300);
// 			from_server=new BufferedReader(new InputStreamReader(client.getInputStream()));
// 			from_user=new BufferedReader(new InputStreamReader(System.in));
// 			to_server = new PrintWriter(client.getOutputStream());

// 			System.out.println("Connected with server " + client.getInetAddress()+ ":"
// 					+client.getPort());


// 			while(true){
// 				serverInput = from_server.readLine();
// 				// if the server receives done just break from this loop
// 				if(serverInput.equals("done") || serverInput == null){
// 					System.out.println(serverInput);
// 					break;
// 				}
// 				//print server input
// 				System.out.println(serverInput);
// 				//get user input and send it to the server
// 				userInput = from_user.readLine();
// 				to_server.println(userInput);
// 				to_server.flush();

// 			}//end of the loop

// 		}
// 		catch(IOException ioe)
// 		{
// 			System.out.println("Error" + ioe);
// 		}
// 		finally{
// 			try {
// 				if (from_server != null)
// 					from_server.close();
// 				if (client != null)
// 					client.close();
// 				if(to_server != null)
// 					to_server.close();
// 			}
// 			catch (IOException ioee) {
// 				System.err.println(ioee);
// 			}
// 		}

// 	}

// }

// Client1.java
import java.io.*;
import java.net.*;

public class MyClient1 {
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

            System.out.println("Connected with server " + client.getInetAddress() + ":" + client.getPort());

            // Identify to server
            toServer.println("Client1");

            // Run login.sh script
            runScript("./login.sh", "Login Script Output");

            // Run check.sh script
            runScript("./check.sh", "Check Script Output");

            // Handle system info requests
            while (true) {
                // Request system info
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
                // If you see "please enter username" or "please enter password"
                // The script is now handling this internally
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

