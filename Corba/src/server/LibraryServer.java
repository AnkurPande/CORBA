package server;

import java.io.PrintWriter;
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
import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;
import org.omg.CORBA.Policy;
import org.omg.CORBA.Request;
import org.omg.CORBA.SetOverrideType;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import common.*;

public class LibraryServer extends ILibraryPOA implements Runnable
{	
	private static HashMap<Character, ArrayList<Student>> index = new HashMap<Character, ArrayList<Student>>();
	
	private String instituteName;
	private HashMap<String, Book> books   = new HashMap<String, Book>();
	private int udpPort = 0;
	private static ArrayList<LibraryServer> servers = new ArrayList<LibraryServer>();

	//TODO change later
	public String[] args;
	
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
		synchronized(index) {
			students.add(st);
		}
		
		//System.out.println("total student in the list: "+this.index.get(userName.charAt(0)).size());	
		logger.info("Account creation success for user: "+st.getUserName());
		
		return true;
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
		synchronized(book) {
			book.setNumOfCopy(book.getNumOfCopy()-1);
			synchronized(st) {
				st.borrowBook(book);
			}
		}
		logger.info("Reserve successfull for user "+username+" with book "+bookName+". Now "+book.getNumOfCopy()+" left");
			
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
		    			String requestData = "nonReturn:"+numOfDays;
		    			byte [] m = requestData.getBytes();
		    			InetAddress aHost = InetAddress.getByName("localhost");
		    			int serverPort = s.getUdpPort();
		    			DatagramPacket request = new DatagramPacket(m, requestData.length(), aHost, serverPort);
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
			//ORB Part
			
			ORB orb = ORB.init(args,null);
			POA rootPoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
			
			
			byte[] id = rootPoa.activate_object(this); 
			org.omg.CORBA.Object ref = rootPoa.id_to_reference(id);
			
			String ior = orb.object_to_string(ref);
			System.out.println(ior);
			
			PrintWriter file = new PrintWriter("logs/"+this.instituteName+".txt");
			file.println(ior);
			file.close();
			
			rootPoa.the_POAManager().activate();
			orb.run();
			
			
			//UDP part
			aSocket = new DatagramSocket(this.udpPort);
			byte [] buffer = new byte[10000];
			this.logger.info("UPD server for "+this.instituteName+" is running on port: "+udpPort);
			while(true) {
				DatagramPacket request = new DatagramPacket(buffer, buffer.length);
				aSocket.receive(request);
				String data = new String(request.getData());
				String[] requestParts = data.split(":");
				String response = "";
				if(requestParts.length == 2 ) {
					//non return request
					response = calculateNonReturners(Integer.parseInt(data.trim()));
				}
				else {
					//reserve request
					response = reserveBook("", "", requestParts[1], requestParts[2])?"true":"false";
				}
				
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
	}
	
	/**
	 * Main entry point for server
	 * @param args
	 */
	public static void main(String args[])
	{
		try{
			//int port = 1099;
			LibraryServer library1 = new LibraryServer("Concordia", 6780);
			Thread server1 =  new Thread(library1);
			server1.start();
			LibraryServer library2 = new LibraryServer("Mcgill", 6781);
			Thread server2 =  new Thread(library2);
			server2.start();
			LibraryServer library3 = new LibraryServer("Montreal", 6781);
			Thread server3 =  new Thread(library3);
			server3.start();
			
			addData(library1);
			addData(library2);
			addData(library3);

			//runDebugTool();
		}
		catch(Exception err){
			err.printStackTrace();
		}
		
	}
	
	public static void createAndRunServer(String args[], String name) throws Exception {
		
		ORB orb = ORB.init(args,null);
		POA rootPoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
		
		LibraryServer server = new LibraryServer(name, 6780);
		byte[] id = rootPoa.activate_object(server); 
		org.omg.CORBA.Object ref = rootPoa.id_to_reference(id);
		
		String ior = orb.object_to_string(ref);
		System.out.println(ior);
		
		PrintWriter file = new PrintWriter("logs/"+server.instituteName+".txt");
		file.println(ior);
		file.close();
		
		rootPoa.the_POAManager().activate();
		orb.run();
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

		for(LibraryServer s : servers) { 
	    	if(s.instituteName != this.instituteName) {
	    		DatagramSocket aSocket = null;
	    		try{
	    			aSocket = new DatagramSocket();
	    			String requestString = "reserve:"+bookName+":"+authorName;
	    			byte [] m = requestString.getBytes();
	    			InetAddress aHost = InetAddress.getByName("localhost");
	    			int serverPort = s.getUdpPort();
	    			DatagramPacket request = new DatagramPacket(m, requestString.length(), aHost, serverPort);
	    	        aSocket.send(request);
	    	        byte [] buffer = new byte[1000];
	    	        DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
	    	        aSocket.receive(reply);
	    	        String response = new String(reply.getData());
	    	        Boolean booked = new Boolean(response);
	    	        if(booked) {
	    	        	return true;
	    	        }
	    	        
	    		}catch(Exception e){
	    			e.printStackTrace();
	    		}finally{
	    			aSocket.close();
	    		}
	    	}
        }

		return false;
	}

}
