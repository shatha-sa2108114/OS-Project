// import java.io.*;
// import java.net.Socket;
// import java.util.Random;

// public class Service extends Thread{
	
// 	Socket nextClient = null;
// 	PrintWriter to_client = null;
// 	BufferedReader from_client = null;
	
// 	public Service(Socket nextClient) {
// 		super();
// 		this.nextClient = nextClient;
// 	}
	
// 	public void run()
// 	{
// 		try {
// 			from_client = new  BufferedReader(new InputStreamReader (nextClient.getInputStream()));
// 			to_client = new PrintWriter (nextClient.getOutputStream(),true);
// 			long start_time = System.currentTimeMillis() ,current_time ;
// 			int countCorrectAnsers = 0;
// 			long difference = 0;
			
// 			while(true){
// 				current_time = System.currentTimeMillis();
// 				if(difference>60000){
					
// 					to_client.println("You are done, total correct answers: "+countCorrectAnsers);
// 					to_client.println("done");
			
// 					break;
// 				}
			
// 				Random randGenerator = new Random();
// 				int number1 = randGenerator.nextInt(10);
// 				int number2 = randGenerator.nextInt(10);
// 				int number3 = randGenerator.nextInt(10);
				
// 				int correctAnswer = number1+number2+number3;
// 				//System.out.println("Please give the sum of "+number1 + " "+number2+" "+number3);
// 				to_client.println("Please give the sum of "+number1 + " "+number2+" "+number3);
// 				to_client.flush();
// 				String input = from_client.readLine();
// 				int clientResult = Integer.parseInt(input);
// 				difference = current_time - start_time;
// 				if(clientResult == correctAnswer && difference<=60000)
// 					countCorrectAnsers++;
// 			}//end of while loop
			
			
// 		} catch (IOException e) {
// 			// TODO Auto-generated catch block
// 			e.printStackTrace();
// 		}finally{
// 				try {
// 					if(from_client !=null)
// 					from_client.close();
// 					if(to_client !=null)
// 						to_client.close();
// 					if(nextClient !=null)
// 						nextClient.close();
// 				} catch (IOException e) {
// 					// TODO Auto-generated catch block
// 					e.printStackTrace();
// 				}
			
// 		}//end of finally
		
// 	}//end of run
	

// } 



import java.io.*;
import java.net.*;
import java.nio.file.Files;  
import java.nio.file.Paths;

public class Service extends Thread {
    private Socket clientSocket;
    private PrintWriter to_client;
    private BufferedReader from_client;
    private long lastSystemInfoRequest;
    private static final Object systemInfoLock = new Object();

    public Service(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.lastSystemInfoRequest = System.currentTimeMillis();
    }

    @Override
    public void run() {
        try {
            to_client = new PrintWriter(clientSocket.getOutputStream(), true);
            from_client = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // Get client identification
            String clientId = from_client.readLine();
            System.out.println("New client connected: " + clientId);

            // Run network.sh script
            runNetworkScript();

            String request;
            while ((request = from_client.readLine()) != null) {
                if (request.equals("SYSTEM_INFO")) {
                    handleSystemInfoRequest();
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
            ProcessBuilder pb = new ProcessBuilder("./network.sh", 
                clientSocket.getInetAddress().getHostAddress());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Network script: " + line);
            }
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void handleSystemInfoRequest() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSystemInfoRequest < 300000) { // 5 minutes
            to_client.println("ERROR: Please wait 5 minutes between system info requests");
            return;
        }

        synchronized (systemInfoLock) {
            try {
                ProcessBuilder pb = new ProcessBuilder("./system.sh");
                pb.redirectErrorStream(true);
                Process process = pb.start();
                process.waitFor();

                String diskInfo = new String(Files.readAllBytes(Paths.get("disk_info.log")));
                String memCpuInfo = new String(Files.readAllBytes(Paths.get("mem_cpu_info.log")));

                to_client.println("BEGIN_SYSTEM_INFO");
                to_client.println(diskInfo);
                to_client.println(memCpuInfo);
                to_client.println("END_SYSTEM_INFO");

                lastSystemInfoRequest = currentTime;
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                to_client.println("ERROR: Failed to get system information");
            }
        }
    }

    private void cleanup() {
        try {
            if (to_client != null) to_client.close();
            if (from_client != null) from_client.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
