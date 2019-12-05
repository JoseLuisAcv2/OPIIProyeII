// SVM server
import java.net.*; 
import java.io.*; 
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.*;
import java.util.Date;
import java.sql.Timestamp;

public class Server
{ 
	private Socket		 socket = null; 
	private ServerSocket server = null; 
	private DataInputStream in	 = null; 
	private DataOutputStream out	 = null; 
	private static int numReplicas = 0;
	private DataInputStream[] ServerSocketsIn = null;
	private DataOutputStream[] ServerSocketsOut = null;
	private int storageCheckpoint = 0;
	private static Process[] serverProcesses = new Process[10];
	private int serverID = 0;
	private int port = 0;
	private String serverType = null;

    public void commit(String filename) {
    	try {
	    	System.out.println("Downloading file " + filename+ "...");
		    byte [] bytearray  = new byte [2048];		    
		    int bytesRead = in.read(bytearray,0,bytearray.length);
		    System.out.println("Downloaded file " + filename + " (" + bytesRead + " bytes read)");

		    // send file to backup servers k-replicas
		    Timestamp ts = new Timestamp((new Date()).getTime());
		    for(int i = 0; i < numReplicas; i++) {
				socket = new Socket("127.0.0.1", 6000+(storageCheckpoint+1));
				DataOutputStream outConn = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
				outConn.writeUTF("commit");
				outConn.writeUTF(filename + ts);
				outConn.write(bytearray,0,bytesRead);
        		outConn.flush();
        		System.out.println("Sending file " + filename + ts + " (" + bytesRead + " bytes) to storage server " + Integer.toString(storageCheckpoint+1));
        		storageCheckpoint = (storageCheckpoint+1)%9;
				outConn.close();
				socket.close();
			}
		} catch(IOException i) { 
            System.out.println(i); 
        } 
    }

	public void storageCommit() {
    	try {
    		String filename = in.readUTF();
		    byte [] bytearray  = new byte [2048];		    
		    int bytesRead = in.read(bytearray,0,bytearray.length);
		    //store file
			FileOutputStream out = new FileOutputStream("storageServer"+serverID+"/"+filename);
			out.write(bytearray,0,bytesRead);
			out.close();
		} catch(IOException i) { 
            System.out.println(i); 
        } 
    }

    public String checkout(String filename, String date) {
        return "Done checkout";
    }

    public String update(String filename) {
        return "Done update";
    }

	public Server(int portParam, int numReplicasParam, String serverTypeParam, int serverIDparam) 

	{ 
		serverID = serverIDparam;
		numReplicas = numReplicasParam;
		port = portParam;
		serverType = serverTypeParam;
		//starts server and wait for client MAIN
		if(serverType.equals("main")) {
			try
			{ 
				server = new ServerSocket(port); 
				System.out.println(String.format("Main SVM Server started")); 

				System.out.println("Waiting for client ..."); 

				socket = server.accept(); 
				System.out.println("Client accepted"); 

				// takes input from the client socket 
				in = new DataInputStream( 
					new BufferedInputStream(socket.getInputStream()));

				out = new DataOutputStream(
					new BufferedOutputStream(socket.getOutputStream())); 

				String line = ""; 

				// reads message from client until "Exit" is sent 
				while (!line.equals("exit")) 
				{ 
					try
					{ 
						line = in.readUTF(); 
						System.out.print("Command: ");
						System.out.println(line);

						String arg[] = line.split(" ");
						if(arg[0].equals("commit")) {
							String filename = in.readUTF();
							commit(filename);
						} else if(arg[0].equals("checkout")) {
							out.writeUTF(checkout(arg[1], arg[2]));
						} else if(arg[0].equals("update")) {
							out.writeUTF(update(arg[1]));
						} else if(arg[0].equals("exit")) {
							break;
						} else {
							out.writeUTF("ERROR: Command not recognized");
						}

					} 
					catch(IOException i) 
					{ 
						System.out.println(i); 
					} 
				} 
				System.out.println("Closing connection..."); 

				// close connection
				for(int i=1; i<10; i++) {
					serverProcesses[i].destroy();
				} 
				socket.close(); 
				in.close(); 
				out.close();

				System.out.println("Goodbye SVM"); 
			} 
			catch(IOException i) 
			{ 
				System.out.println(i); 
			} 
		} else { // SECONDARY STORAGE SERVER FUNCTIONS
			try
			{ 
				server = new ServerSocket(port); 
				System.out.println(String.format("Storage SVM Server started")); 

				System.out.println("Waiting for main ..."); 

				socket = server.accept(); 
				System.out.println("Main accepted"); 

				// takes input from the client socket 
				in = new DataInputStream( 
					new BufferedInputStream(socket.getInputStream()));

				out = new DataOutputStream(
					new BufferedOutputStream(socket.getOutputStream())); 

				String line = ""; 

				// reads message from client until "Exit" is sent 
				while (!line.equals("Exit")) 
				{ 
					try
					{ 
						line = in.readUTF(); 
						System.out.print("Command: ");
						System.out.println(line);

						String arg[] = line.split(" ");
						if(arg[0].equals("commit")) {							
							storageCommit();
						} else if(arg[0].equals("checkout")) {
							out.writeUTF(checkout(arg[1], arg[2]));
						} else if(arg[0].equals("update")) {
							out.writeUTF(update(arg[1]));
						}

					} 
					catch(IOException i) 
					{ 
						System.out.println(i); 
					} 
				} 
				System.out.println("Closing connection"); 

				// close connection 
				socket.close(); 
				in.close(); 
			} 
			catch(IOException i) 
			{ 
				System.out.println(i); 
			} 
		}
	} 

	public static void main(String args[]) throws Exception
	{ 
		int numReplicas = Integer.parseInt(args[0]);
		if(args.length == 1) {
			for(int i = 1; i < 10; i++) {
				serverProcesses[i] = Runtime.getRuntime().exec("java Server " + Integer.toString(numReplicas) + " " + Integer.toString(i));
			}
			Server server = new Server(6000, numReplicas, "main", 0); 
	    } else {
	    	int serverNum = Integer.parseInt(args[1]);
			Server server = new Server(6000+serverNum, numReplicas, "backup", serverNum); 
	    }
	} 
} 
