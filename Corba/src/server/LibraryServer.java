package server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.omg.CORBA.Context;
import org.omg.CORBA.ContextList;
import org.omg.CORBA.DomainManager;
import org.omg.CORBA.ExceptionList;
import org.omg.CORBA.NVList;
import org.omg.CORBA.NamedValue;
import org.omg.CORBA.Object;
import org.omg.CORBA.Policy;
import org.omg.CORBA.Request;
import org.omg.CORBA.SetOverrideType;

import common.*;

public class LibraryServer extends ILibraryPOA implements Runnable
{	
	private static HashMap<Character, ArrayList<Student>> index = new HashMap<Character, ArrayList<Student>>();
	
	private String instituteName;
	private HashMap<String, Book> books   = new HashMap<String, Book>();
	private int udpPort = 0;
	private static ArrayList<LibraryServer> servers = new ArrayList<LibraryServer>();

	public int getUdpPort() {
		return udpPort;
	}

	//Logger
	private Logger logger;
	
	/**
	 * Initialize logger for library server
	 * @param fileName
	 */
	private void setLogger(String fileName) {
		try{
			this.logger = Logger.getLogger(this.instituteName);
			FileHandler fileTxt 	 = new FileHandler(fileName);
			SimpleFormatter formatterTxt = new SimpleFormatter();
		    fileTxt.setFormatter(formatterTxt);
		    logger.addHandler(fileTxt);
		}
		catch(Exception err) {
			System.out.println("Couldn't Initiate Logger. Please check file permission");
		}
	}
	
	//End Logger
	
	public Student searchStudent(String userName)
	{
		Student st = null;
		ArrayList<Student> list = null;
		try{
			if(index != null) {
				synchronized(index) {
					if(index.containsKey(userName.charAt(0))) {
						
						list = index.get(userName.charAt(0));
					
						if(list != null) {
							for(Student s: list) {
								if(s.getUserName().equals(userName)){
									return s;
								}
							}
						}
						else {
							System.out.println("Null List");
						}
					}
				}
			}
		}
		catch(Exception err) {
			err.printStackTrace();
		}
		return st;
	}

	
	@Override
	/**
	 * Create new Account for student
	 * @param firstName
	 * @param lastName
	 * @param emailAddress
	 * @param phoneNumber
	 * @param userName
	 * @param password
	 * @param inst
	 * @return
	 */
	public boolean createAccount(String firstName, String lastName,
			String emailAddress, String phoneNumber, String userName,
			String password, String inst) {
		Student st;

		//System.out.println("Account creation request recieved by "+this.instituteName+ " Library Server");
		st = searchStudent(userName);
		
		if(st != null) {
			logger.info("User exists in the system");
			return false;
		}
		
		st = new Student(userName, password, instituteName);
		
		st.setFirstName(firstName);
		st.setLastName(lastName);
		st.setEmailAddress(emailAddress);
		st.setPhoneNumber(phoneNumber);
		
		ArrayList<Student> students = index.get(userName.charAt(0));
		if(students == null) {
			students = new ArrayList<Student>();
			synchronized(index) {
				index.put(userName.charAt(0), students);
			}
		}
		students.add(st);
		
		//System.out.println("total student in the list: "+this.index.get(userName.charAt(0)).size());	
		logger.info("Account creation success for user: "+st.getUserName());
		
		return false;
	}

	//TODO where to put sync???
	@Override
	/**
	 * Reserve a book for user
	 * @param username
	 * @param password
	 * @param bookName
	 * @param authorName
	 * @return
	 */
	public boolean reserveBook(String username, String password,
			String bookName, String authorName) {
		
		Student st = searchStudent(username);
		
		if(st == null) {
			logger.info("Student not found");
			return false;
		}
		
		if(!st.getInst().equals(this.instituteName)) {
			logger.info("Wrong instutute");
			return false;
		}
		Book book = null;
		synchronized(st) {
			if(!st.getPassword().equals(password)) {
				logger.info("Password mismatch");
				return false;
			}
			book = (Book)this.books.get(bookName);
			if(book == null) {
				logger.info("Book not found");
				return false;
			}
			if(!book.getAuthor().equals(authorName)) {
				logger.info("Author mismatch "+authorName+" : "+book.getAuthor());
				return false;
			}
			if(book.getNumOfCopy() <=0) {
				//System.out.println("No copy left");
				return false;
			}
			
			book.setNumOfCopy(book.getNumOfCopy()-1);
			st.borrowBook(book);
			logger.info("Reserve successfull for user "+username+" with book "+bookName+". Now "+book.getNumOfCopy()+" left");
		}	
		return true;
	}
	

	@Override
	/**
	 * Get non returners for all libraries
	 * @param username
	 * @param password
	 * @param inst
	 * @param numOfDays
	 * @return
	 * @throws RemoteException
	 */
	public String getNonRetuners(String username, String password,
			String inst, short numOfDays) {
		String response = "";
		
		if(!username.equals("Admin") || !password.equals("Admin")) {
			return "Invalid Credential";
		}
		
	    response += calculateNonReturners(numOfDays);
	    for(LibraryServer s : servers) { 
			synchronized(s) {
		    	if(s.instituteName != this.instituteName) {
		    		DatagramSocket aSocket = null;
		    		try{
		    			aSocket = new DatagramSocket();
		    			byte [] m = (""+numOfDays).getBytes();
		    			InetAddress aHost = InetAddress.getByName("localhost");
		    			int serverPort = s.getUdpPort();
		    			DatagramPacket request = new DatagramPacket(m, (""+numOfDays).length(), aHost, serverPort);
		    	        aSocket.send(request);
		    	        byte [] buffer = new byte[1000];
		    	        DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
		    	        aSocket.receive(reply);
		    	        response += new String(reply.getData());
		    		}catch(Exception e){
		    			e.printStackTrace();
		    		}finally{
		    			aSocket.close();
		    		}
		    	}
			}
        }
	    
	    return response;
	}
	
	/**
	 * iterate through own students and create non-returner response string
	 * @param numOfDays
	 * @return
	 */
	private String calculateNonReturners(int numOfDays)
	{
		String response = this.instituteName+":\n";
		Iterator it = this.index.entrySet().iterator();
	    boolean foundStudent = false;
		while (it.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it.next();
	        
	        ArrayList<Student> studentList = (ArrayList<Student>)pairs.getValue();
	        
	        for(Student st: studentList) {
		        if(st.getInst() == this.instituteName && st.getBorrows().size() > 0) {
		        	//System.out.println("Student "+st.getUserName()+" has borrowed item, checking due date");
		        	ArrayList<Borrow> list = st.getBorrows();
		        	for(Borrow b : list) {
		        		if(b.getDueDays() >= numOfDays) {
		        			response += st.getFirstName()+" "+st.getLastName()+" "+st.getPhoneNumber()+"\n";
		        			foundStudent = true;
		        			break;
		        		}
		        	}
		        }
	        }
	        
	        //it.remove(); // avoids a ConcurrentModificationException
	    }
		
	    if(!foundStudent) {
	    	response += "No student found\n";
	    }
	    
	    response += "------*-------\n\n";
	   
	    
	    return response;
	}
	
	/**
	 * Run thread + UDP server 
	 */
	public void run()
	{
		DatagramSocket aSocket = null;
		try {
			aSocket = new DatagramSocket(this.udpPort);
			byte [] buffer = new byte[10000];
			this.logger.info("UPD server for "+this.instituteName+" is running on port: "+udpPort);
			while(true) {
				DatagramPacket request = new DatagramPacket(buffer, buffer.length);
				aSocket.receive(request);
				String data = new String(request.getData());
				String response = calculateNonReturners(Integer.parseInt(data.trim()));
				DatagramPacket reply = new DatagramPacket(response.getBytes(), response.length(), request.getAddress(), request.getPort());
				aSocket.send(reply);
			}
			//this.logger.info("Server for "+this.instituteName+" library is running now");
		}
		catch(Exception err) {
			err.printStackTrace();
		}
		finally{
			aSocket.close();
		}
	}
	
	/**
	 * Constructor
	 * @param instituteName
	 * @param udpPort
	 */
	public LibraryServer(String instituteName, int udpPort)
	{
		this.instituteName = instituteName;
		this.udpPort	   = udpPort;
		this.setLogger("logs/libraries/"+instituteName+".txt");
	}
	
	/**
	 * Debug Tool to set custom numOfDays
	 * @param username
	 * @param bookName
	 * @param numDays
	 */
	public static void setDuration(String username, String bookName, int numDays)
	{
		
		for(LibraryServer s: servers) {
			Student st = s.searchStudent(username);
			if(st != null && st.getInst().equals(s.instituteName)) {
				
				Book book = (Book)s.books.get(bookName);
				if(book == null) {
					System.out.println("Book not found");
				}
				
				if(book.getNumOfCopy() <= 0) {
					System.out.println("No copy left");
				}
				
				book.setNumOfCopy(book.getNumOfCopy()-1);
				Borrow b = st.borrowBook(book);
				b.setDueDate(numDays);
			}
			else {
				//System.out.println("Student not found");
			}
		}
	}
	
	private static int i=1;
	/**
	 * Dummy data creation for server
	 * @param server
	 */
	public static void addData(LibraryServer server)
	{
		Book book = new Book("Cuda","Nicholas",2);
		server.books.put(book.getName(), book);
		book = new Book("Opencl","Munshi", 3);
		server.books.put(book.getName(), book);
		book = new Book("3DMath","Fletcher", 1);
		server.books.put(book.getName(), book);
		
		//books
		/*for(int j=1; j<10; j++) { 
			Book book = new Book("Book"+j, "Author"+j, 10);
			server.books.put(book.getName(), book);
		}
		
		server.createAccount("Test"+i, "Test"+i, "Test"+i+"@test.com", "123456", "rana"+i, "abcd", server.instituteName);
		server.reserveBook("rana"+i, "abcd", "Book"+i, "Author"+i);
		i++;*/
	}
	
	/**
	 * Main entry point for server
	 * @param args
	 */
	public static void main(String args[])
	{
		try{
			int port = 1099;
			LibraryServer server1 = new LibraryServer("Concordia University", 6780);
			LibraryServer server2 = new LibraryServer("Mcgill University", 6781);
			LibraryServer server3 = new LibraryServer("Montreal University", 6782);
			
			/*Registry r;
			r = LocateRegistry.createRegistry(port);
			
			Remote obj = UnicastRemoteObject.exportObject(server1, port);
			r.bind("concordia", obj);
			
			Remote obj2 = UnicastRemoteObject.exportObject(server2, port);
			r.bind("mcgill", obj2);
			
			Remote obj3 = UnicastRemoteObject.exportObject(server3, port);
			r.bind("montreal", obj3);*/
			
			//server1.start();
			//server2.start();
			//server3.start();
			
			addData(server1);
			addData(server2);
			addData(server3);
			
			servers.add(server1);
			servers.add(server2);
			servers.add(server3);
			
			//server1.createAccount("Frankenstein", "Test", "frankenstein@test.com", "123456", "Frankenstein", "abcd", server1.instituteName);
			//server2.createAccount("drwho900", "Test", "drwho900@test.com", "123456", "drwho900", "abcd", server2.instituteName);
			//server3.createAccount("patrickstar", "Test", "patrickstar@test.com", "123456", "patrickstar", "abcd", server3.instituteName);
			
			/*server3.reserveBook("patrickstar", "abcd", "Opencl", "Munshi");
			server2.reserveBook("drwho900", "abcd", "3DMath", "Fletcher");
			server2.reserveBook("drwho900", "abcd", "3DMath", "Fletcher");
			server1.reserveBook("Frankenstein", "abcd", "Cuda", "Nicholas");*/
			
			runDebugTool();
		}
		catch(Exception err){
			err.printStackTrace();
		}
		
	}
	
	/**
	 * Debug tool menu
	 */
	public static void runDebugTool()
	{
		Scanner keyboard = new Scanner(System.in);
		while(true)
		{
			try {
				keyboard.nextLine();
				System.out.println("\n****Welcome to DRMS Debug Tool System****\n");
				System.out.println("User Name: ");
				String userName = keyboard.nextLine();
				System.out.println("Book: ");
				String bookName = keyboard.nextLine();
				System.out.println("No Of Days: ");
				int numOfDays = keyboard.nextInt();
				//keyboard.nextLine();
				setDuration(userName, bookName, numOfDays);
			}
			catch(Exception er) {
				//er.printStackTrace();
			}
		}
	}

	@Override
	public boolean reserveInterLibrary(String username, String password,
			String bookName, String authorName) {
		// TODO Auto-generated method stub
		return false;
	}

}
