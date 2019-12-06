// SVM client
import java.net.*; 
import java.io.*; 
import java.sql.Timestamp;

public class Client 
{ 
    private Socket socket        = null; 
    private DataInputStream input = null; 
    private DataOutputStream out     = null; 
    private DataInputStream in = null; 
    private String serverResponse = null;

    public void commit() {
        try {
            System.out.print("Filename: "); 
            String filename = input.readLine(); 
            byte [] bytearray = null;
            File filecheck = new File("workingDir/" + filename);
            if(filecheck.exists()) { 
                out.writeUTF("commit");
                out.writeUTF(filename);
                File file = new File ("workingDir/" + filename);
                bytearray  = new byte [(int)file.length()];
                //FileInputStream fis = new FileInputStream(file);
                //BufferedInputStream bis = new BufferedInputStream(fis);
                FileInputStream in = new FileInputStream("workingDir/" + filename);
                in.read(bytearray,0,bytearray.length);
                out.write(bytearray,0,bytearray.length);
                System.out.println("Sending " + filename + " (" + bytearray.length + " bytes) to server...");
                System.out.println("Done.");
            } else {
                System.out.println("NOT FOUND file " + filename + " in working directory");
            }
        } catch(IOException io) { 
            System.out.println(io); 
        }
    }

    public void checkout(String filename, String timestamp) {
        try { 
            out.writeUTF(filename);
            out.writeUTF(timestamp);
            out.flush();
            String found = in.readUTF();
            //checkout file
            if(found.equals("false")) {
                System.out.println("File " + filename + " with timestamp " + timestamp + " not found...");
            } else {
                byte [] bytearray  = new byte [2048];   
                int bytesRead = in.read(bytearray,0,bytearray.length);
                System.out.println("Checking out " + filename + " (" + bytesRead + " bytes) from server...");
                FileOutputStream outFile = new FileOutputStream("workingDir"+"/"+filename);
                outFile.write(bytearray,0,bytesRead);
                outFile.close();
                System.out.println("File " + filename + " with timestamp " + timestamp + " checked out.");
            }
            System.out.println("Done.");
        } catch(IOException i) { 
            System.out.println(i); 
        } 
    }

    public void update() {
        try { 
            System.out.print("Filename: "); 
            String filename = input.readLine();
            out.writeUTF("update");
            out.writeUTF(filename);
            out.flush();
            String found = in.readUTF();
            //checkout file
            if(found.equals("false")) {
                System.out.println("File " + filename + " not found...");
            } else {
                System.out.println("Updating file " + filename + "...");
                byte [] bytearray  = new byte [2048];   
                int bytesRead = in.read(bytearray,0,bytearray.length);
                System.out.println("Updating to last revision of file " + filename + " (" + bytesRead + " bytes) from server...");
                FileOutputStream outFile = new FileOutputStream("workingDir"+"/"+filename);
                outFile.write(bytearray,0,bytesRead);
                outFile.close();
                System.out.println("File " + filename + " updated.");
            }
            System.out.println("Done.");
        } catch(IOException i) { 
            System.out.println(i); 
        } 
    }

    public Client(String address, int port) 
    { 
        // establish a connection 
        try
        { 
            socket = new Socket(address, port); 
            System.out.println("Connected"); 
            System.out.println("Welcome to SVM");
            System.out.println("Enter your command: [commit | update | checkout | exit]"); 

            // takes input from terminal 
            input = new DataInputStream(System.in); 
            in = new DataInputStream(socket.getInputStream());
            // sends output to the socket 
            out = new DataOutputStream(socket.getOutputStream()); 
        } 
        catch(UnknownHostException u) 
        { 
            System.out.println(u); 
        } 
        catch(IOException i) 
        { 
            System.out.println(i); 
        } 

        // string to read message from input 
        String line = ""; 

        // keep reading until "Exit" is input 
        while (!line.equals("exit")) 
        { 
            try
            { 
                System.out.print("Command: "); 
                line = input.readLine(); 

                String arg[] = line.split(" ");
                if(arg[0].equals("commit")) {                    
                    commit();
                } else if(arg[0].equals("checkout")) {
                    out.writeUTF("checkout");
                    System.out.print("Filename: ");
                    String filename = input.readLine();
                    System.out.print("Timestamp [yyy-mm-dd hh:mm:ss.sss]: ");
                    String timestamp = input.readLine();
                    checkout(filename, timestamp);
                } else if(arg[0].equals("update")) {
                    update();
                } else if(arg[0].equals("exit")) {
                    break;
                } else {
                    System.out.println("ERROR: Command not recognized");
                }

            } 
            catch(IOException i) 
            { 
                System.out.println("Exception"); 
                System.out.println(i); 
            } 
        } 

        // close the connection 
        try
        { 
            out.writeUTF("exit");
            in.close();
            input.close(); 
            out.close(); 
            socket.close(); 
        } 
        catch(IOException i) 
        { 
            System.out.println(i); 
        } 
    } 

    public static void main(String args[]) 
    { 
        Client client = new Client("127.0.0.1", 6000); 
    } 
} 
