
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MyServer {
	
	public static void main(String args[])
	{
		ServerSocket server = null;
		Socket nextClient = null;
		
		try{
			//Bind to service port, so clients access to daytime service
			server=new ServerSocket(1300);
			System.out.println("Server waiting for client on port "+
					server.getLocalPort());
			System.out.println("Service Started");
			for(;;){
				//Get the next TCP Client
				nextClient=server.accept();
				//Display connection details
				System.out.println("Receiving Request From "+nextClient.getInetAddress()+ ":" +
						nextClient.getPort());
				//serving each client on a different socket after the it accepts client request
				Service servThread = new Service(nextClient);
				servThread.start();
				
			}
		}
		catch(IOException ioe){
			System.out.println("Error" + ioe);
		}
		finally {
			if (server != null)
				try{
					server.close();
				}
				catch (Exception e){System.err.println(e);}
		}
	} //End main

}

