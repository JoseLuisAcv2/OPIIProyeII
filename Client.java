// SVM client
import java.net.*; 
import java.io.*; 

public class Client 
{ 
    private Socket socket        = null; 
    private DataInputStream input = null; 
    private DataOutputStream out     = null; 
    private BufferedReader in = null; 
    private String serverResponse = null;

    public void commit(String filename) {
        // int count;
        // byte[] buffer = new byte[8192]; // or 4096, or more
        // while ((count = in.read(buffer)) > 0)
        // {
        //   out.write(buffer, 0, count);
        // }
        byte [] bytearray = null;
        try {
            File file = new File ("workingDir/" + filename);
            bytearray  = new byte [(int)file.length()];
            //FileInputStream fis = new FileInputStream(file);
            //BufferedInputStream bis = new BufferedInputStream(fis);
            FileInputStream in = new FileInputStream("workingDir/" + filename);
            in.read(bytearray,0,bytearray.length);
            out.write(bytearray,0,bytearray.length);
            System.out.println("Sending " + filename + " (" + bytearray.length + " bytes) to server...");
        } catch(IOException i) { 
            System.out.println(i); 
        } 
        System.out.println("Done.");
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
            //in = new DataInputStream( 
            //    new BufferedInputStream(socket.getInputStream()));
            in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));

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
                    out.writeUTF("commit");
                    System.out.print("Filename: "); 
                    line = input.readLine(); 
                    out.writeUTF(line);
                    commit(line);
                } else if(arg[0].equals("checkout")) {
                    //out.writeUTF(checkout(arg[1], arg[2]));
                } else if(arg[0].equals("update")) {
                    //out.writeUTF(update(arg[1]));
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
            //in.close();
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
