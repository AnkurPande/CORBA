package client;
import common.*;

import java.util.Scanner;
import java.util.logging.Logger;

public class StudentClient extends BaseClient
{
	
	public void showMenu() 
	{
		System.out.println("\n****Welcome to DRMS Student Client System****\n");
		System.out.println("Please select an option (1-3)");
		System.out.println("1. Create An Account.");
		System.out.println("2. Reserve a Book");
		System.out.println("3. Multi Thread Test");
		System.out.println("4. Exit");
	}
	
	public Logger getLogger(String userName)
	{
		this.setLogger(userName, "logs/students/"+userName+".txt");
		return this.logger;
	}
	
	public void run()
	{
		try
		{
			createAccount();
		}
		catch(Exception err) {
			err.printStackTrace();
		}
	}
	
	public void createAccount()
	{
		
		try {
			ILibrary server = concordiaServer;
			boolean test = server.createAccount("test1", "test", "test@test", "123123123", "test", "abcd", "concordia");
			System.out.println(test);
		}
		catch(Exception err){
			err.printStackTrace();
		}
	}
	
	
	public static void main(String args[]) {
		try {
			
			//createAccount();
			StudentClient client = new StudentClient();
			client.args = args;
			client.initializeServers(args);
			
			ILibrary server;
			
			int userChoice=0;
			Scanner keyboard = new Scanner(System.in);
			
			server = client.getValidServer(keyboard);
			
			client.showMenu();
			
			String userName, password, inst;
			boolean isSuccess;
			while(true)
			{
				
				userChoice = client.getValidInt(keyboard);
				// Manage user selection.
				switch(userChoice)
				{
				case 1: 
					System.out.println("First Name: ");
					String firstName = client.getValidString(keyboard);
					System.out.println("Last Name: ");
					String lastName = client.getValidString(keyboard);
					System.out.println("Email: ");
					String emailAddress = client.getValidString(keyboard);
					System.out.println("Phone No: ");
					String phoneNumber = client.getValidString(keyboard);
					System.out.println("User Name: ");
					userName = client.getValidString(keyboard);
					System.out.println("Pass: ");
					password = client.getValidString(keyboard);
					
					//TODO what to do with institute name
					client.getLogger(userName).info(""+server.createAccount(firstName, lastName, emailAddress, phoneNumber, userName, password, client.instituteName));

					client.showMenu();
					break;
				case 2:
					System.out.println("User Name: ");
					userName = client.getValidString(keyboard);
					System.out.println("Pass: ");
					password = client.getValidString(keyboard);
					System.out.println("Book Name: ");
					String bookName = client.getValidString(keyboard);
					System.out.println("Author: ");
					String authorName = client.getValidString(keyboard);
					if(server.reserveBook(userName, password, bookName, authorName)) {
						client.getLogger(userName).info("Reserve Success");
					}
					else {
						//Try to reserve on other libraries
						if(server.reserveInterLibrary(userName, password, bookName, authorName)) {
							client.getLogger(userName).info("Reserve InterLibrary Success");
						}
					}

					client.showMenu();
					break;
				case 3:
					System.out.println("Number Of Thread");
					int numThread = client.getValidInt(keyboard);
					for(int i=0; i <numThread;i++)
					{
						StudentClient st = new StudentClient();
						st.start();
					}
					break;
				case 4:
					System.out.println("Have a nice day!");
					keyboard.close();
					System.exit(0);
				default:
					System.out.println("Invalid Input, please try again.");
				}
			}
			
		}
		catch(Exception err) {
			err.printStackTrace();
		}
		
	}
}
