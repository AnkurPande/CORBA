package client;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.omg.CORBA.ORB;

import common.ILibrary;
import common.ILibraryHelper;

public abstract class BaseClient extends Thread
{
	ILibrary concordiaServer, mcgillServer, montrealServer;
	static final String CONCORDIA="Concordia", MCGILL="Mcgill", MONTREAL="Montreal";
	protected String instituteName;
	
	public abstract void showMenu();
	
	//Logger
	protected Logger logger;
	
	public String[] args;
	
	public void setLogger(String username, String fileName) {
		try{
			this.logger = Logger.getLogger(username);
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
	
	private ILibrary initServer(String[] args, String name) throws Exception {
		
		ORB orb = ORB.init(args, null);
		BufferedReader br = new BufferedReader(new FileReader("logs/"+name+".txt"));
		String ior = br.readLine();
		br.close();
		
		org.omg.CORBA.Object o = orb.string_to_object(ior);
		return ILibraryHelper.narrow(o);
	}
	
	
	public void initializeServers(String[] args) throws Exception {
		concordiaServer = initServer(args, CONCORDIA);
		montrealServer = initServer(args, MONTREAL);
		mcgillServer = initServer(args, MCGILL);	
	}
	
	public ILibrary getServer(String inst) {
		if(inst.equals(MONTREAL)) {
			return montrealServer;
		}
		else if(inst.equals(CONCORDIA)) {
			return concordiaServer;
		}
		else if(inst.equals(MCGILL)) {
			return mcgillServer;
		}
		return null;
	}
	
	public String getValidString(Scanner keyboard) {
		Boolean valid = false;
		String userInput = "";
		while(!valid)
		{
			try{
				userInput=keyboard.nextLine();
				valid=true;
			}
			catch(Exception e)
			{
				System.out.println("Invalid Input, please enter an String");
				valid=false;
				//keyboard.nextLine();
			}
		}
		//keyboard.nextLine();
		return userInput;
	}
	
	public int getValidInt(Scanner keyboard) {
		// Enforces a valid integer input.
		Boolean valid = false;
		int userChoice = 0;
		while(!valid)
		{
			try{
				userChoice=keyboard.nextInt();
				valid=true;
			}
			catch(Exception e)
			{
				System.out.println("Invalid Input, please enter an Integer");
				valid=false;
				//keyboard.nextLine();
			}
		}
		keyboard.nextLine();
		return userChoice;
	}
	
	public ILibrary getValidServer(Scanner keyboard) {
		// Enforces a valid integer input.
		Boolean valid = false;
		ILibrary server = null;
		System.out.println("Enter Institute Name");
		System.out.println("'Concordia' For Concordia University");
		System.out.println("'Mcgill' For Mcgill University");
		System.out.println("'Montreal' For Montreal University");
		while(!valid)
		{
			try{
				instituteName = keyboard.nextLine();
				server = getServer(instituteName);
				if(server != null) {
					valid=true;
				}
				else {
					System.out.println("Invalid Institute Name, please enter valid institute name");
					//keyboard.nextLine();
				}
			}
			catch(Exception e)
			{
				System.out.println("Invalid Institute Name, please enter valid institute name");
				valid=false;
				//keyboard.nextLine();
			}
		}
		//keyboard.nextLine();
		return server;
	}
}
