// SVM server
import java.net.*; 
import java.io.*; 
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.*;
import java.util.Date;
import java.util.Arrays;
import java.sql.Timestamp;

public class Server
{ 
	private static final int SERVER_NUM = 10; // MAIN + BACKUPS
	private Socket		 socket = null; 
	private ServerSocket server = null; 
	private DataInputStream in	 = null; 
	private DataOutputStream out	 = null; 
	private static int numReplicas = 0;
	private DataInputStream[] ServerSocketsIn = null;
	private DataOutputStream[] ServerSocketsOut = null;
	private int storageCheckpoint = 0;
	private static Process[] serverProcesses = new Process[SERVER_NUM];
	private Socket[] socketConn = new Socket[SERVER_NUM];
	private DataOutputStream[] outConn = new DataOutputStream[SERVER_NUM];
	private DataInputStream[] inConn = new DataInputStream[SERVER_NUM];
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
				outConn[storageCheckpoint+1].flush();
				outConn[storageCheckpoint+1].writeUTF("commit");
				outConn[storageCheckpoint+1].writeUTF(filename + ts);
				outConn[storageCheckpoint+1].write(bytearray,0,bytesRead);
        		outConn[storageCheckpoint+1].flush();
        		System.out.println("Sending file " + filename + ts + " (" + bytesRead + " bytes) to storage server " + Integer.toString(storageCheckpoint+1));
        		storageCheckpoint = (storageCheckpoint+1)%9;
			}
		} catch(IOException i) { 
            System.out.println(i); 
        } 
        System.out.println("Done.");
    }

	public void storageCommit() {
    	try {
    		String filename = in.readUTF();
		    byte [] bytearray  = new byte [2048];		    
		    int bytesRead = in.read(bytearray,0,bytearray.length);
		    //store file
			FileOutputStream outFile = new FileOutputStream("storageServer"+serverID+"/"+filename);
			outFile.write(bytearray,0,bytesRead);
			outFile.close();
		} catch(IOException i) { 
            System.out.println(i); 
        } 
    }

    public void checkout() {
    	try {
    		String filename = in.readUTF();
    		String timestamp = in.readUTF();
			System.out.println("Searching file " + filename + " with timestamp " + timestamp + "...");
    		byte [] bytearray = searchFile(filename, timestamp);
    		if(bytearray == null) {
    			out.writeUTF("false");
    			out.flush();
    			System.out.println("NOT FOUND " + filename + " with timestamp " + timestamp + ".");
    		} else {
    			out.flush();
    			out.writeUTF("true");
    			out.write(bytearray,0,bytearray.length);
    			out.flush();
    			System.out.println("Sending file " + filename + " (" + bytearray.length + " bytes)...");
    		}
		} catch(IOException i) { 
            System.out.println(i); 
        } 
        System.out.println("Done.");
    }

    private byte[] searchFile(String filename, String timestamp) {
    	for(int i = 1; i < SERVER_NUM; i++) {
			try {
				System.out.println("Asking server with ID " + Integer.toString(i) + "...");
				outConn[i].writeUTF("get");
				outConn[i].writeUTF(filename + timestamp);
				outConn[i].flush();
				// get response
				String found = inConn[i].readUTF();
				if(found.equals("true")) {
					byte [] bytearray  = new byte [2048];		    
			    	int bytesRead = inConn[i].read(bytearray,0,bytearray.length);
			    	System.out.println("Found in server with ID " + Integer.toString(i) + ". Size " + bytesRead + " bytes.");
					return Arrays.copyOfRange(bytearray, 0, bytesRead);
				}
			} catch(UnknownHostException u) { 
	            System.out.println(u); 
	        } catch(IOException io) { 
	            System.out.println(io); 
	        }
		}
    	return null;
    }

    public void getFile() {
    	try {
			String filename = in.readUTF();
			System.out.println("Get file " + filename);
			File file = new File("storageServer" + Integer.toString(serverID) + "/" + filename);
			if(file.exists()) { 
			    byte [] bytearray  = new byte [(int)file.length()];
			    FileInputStream inFile = new FileInputStream("storageServer" + Integer.toString(serverID) + "/" + filename);
	        	inFile.read(bytearray,0,bytearray.length);
	        	out.writeUTF("true");
	        	out.write(bytearray,0,bytearray.length);
			} else {
				out.writeUTF("false");
			}
			out.flush();
			System.out.println("Done.");
       	} catch(IOException io) { 
	      	System.out.println(io); 
	  	}
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

				// Connect to servers
				for(int i = 1; i < SERVER_NUM; i++) {
					socketConn[i] = new Socket("127.0.0.1", 6000+i);
					outConn[i] = new DataOutputStream(new BufferedOutputStream(socketConn[i].getOutputStream()));
					inConn[i] = new DataInputStream(new BufferedInputStream(socketConn[i].getInputStream()));
				}

				String line = ""; 

				// reads message from client until "Exit" is sent 
				while (!line.equals("exit")) 
				{ 
					try
					{ 
						System.out.println("Waiting command...");
						line = in.readUTF(); 
						System.out.print("Command: ");
						System.out.println(line);

						String arg[] = line.split(" ");
						if(arg[0].equals("commit")) {
							String filename = in.readUTF();
							commit(filename);
						} else if(arg[0].equals("checkout")) {
							checkout();
						} else if(arg[0].equals("update")) {
							System.out.println("update");
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
				for(int i=1; i<SERVER_NUM; i++) {
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

				// takes input from the main server socket 
				in = new DataInputStream( 
					new BufferedInputStream(socket.getInputStream()));

				out = new DataOutputStream(
					new BufferedOutputStream(socket.getOutputStream())); 

				String line = ""; 

				// reads message from main until "Exit" is sent 
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
						} else if(arg[0].equals("get")) {
							System.out.println("Get request.");
							getFile();
						} else if(arg[0].equals("update")) {
							
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
			for(int i = 1; i < SERVER_NUM; i++) {
				serverProcesses[i] = Runtime.getRuntime().exec("java Server " + Integer.toString(numReplicas) + " " + Integer.toString(i));
			}
			Server server = new Server(6000, numReplicas, "main", 0); 
	    } else {
	    	int serverNum = Integer.parseInt(args[1]);
			Server server = new Server(6000+serverNum, numReplicas, "backup", serverNum); 
	    }
	} 
} 
