package common;

/**
 * Interface definition: ILibrary.
 * 
 * @author OpenORB Compiler
 */
public interface ILibraryOperations
{
    /**
     * Operation createAccount
     */
    public boolean createAccount(String firstName, String lastName, String emailAddress, String phoneNumber, String userName, String password, String inst);

    /**
     * Operation reserveBook
     */
    public boolean reserveBook(String username, String password, String bookName, String authorName);

    /**
     * Operation getNonRetuners
     */
    public String getNonRetuners(String username, String password, String inst, short numOfDays);

}
