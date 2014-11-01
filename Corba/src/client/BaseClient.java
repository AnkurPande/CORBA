package client;

import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import common.ILibrary;

public abstract class BaseClient extends Thread
{
	ILibrary concordiaServer, mcgillServer, montrealServer;
	static final String CONCORDIA="concordia", MCGILL="mcgill", MONTREAL="montreal";
	protected String instituteName;
	
	public abstract void showMenu();
	
	//Logger
	protected Logger logger;
	
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
	
	public void initializeServers() throws Exception {
		System.setSecurityManager(new RMISecurityManager());
		
		concordiaServer = (ILibrary)Naming.lookup("rmi://127.0.0.1:1099/"+CONCORDIA);		
		montrealServer = (ILibrary)Naming.lookup("rmi://127.0.0.1:1099/"+MONTREAL);
		mcgillServer = (ILibrary)Naming.lookup("rmi://127.0.0.1:1099/"+MCGILL);	
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
		System.out.println("'concordia' For Concordia University");
		System.out.println("'mcgill' For Mcgill University");
		System.out.println("'montreal' For Montreal University");
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