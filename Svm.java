// SVM server
public class Svm
{ 
	public static void main(String args[]) throws Exception
	{ 
		int servers = Integer.parseInt(args[0]);
		Process exec = Runtime.getRuntime().exec(String.format("java Server 1"));
		exec.waitFor();
	} 
} 
