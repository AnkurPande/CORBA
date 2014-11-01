package common;

/**
 * Holder class for : ILibrary
 * 
 * @author OpenORB Compiler
 */
final public class ILibraryHolder
        implements org.omg.CORBA.portable.Streamable
{
    /**
     * Internal ILibrary value
     */
    public common.ILibrary value;

    /**
     * Default constructor
     */
    public ILibraryHolder()
    { }

    /**
     * Constructor with value initialisation
     * @param initial the initial value
     */
    public ILibraryHolder(common.ILibrary initial)
    {
        value = initial;
    }

    /**
     * Read ILibrary from a marshalled stream
     * @param istream the input stream
     */
    public void _read(org.omg.CORBA.portable.InputStream istream)
    {
        value = ILibraryHelper.read(istream);
    }

    /**
     * Write ILibrary into a marshalled stream
     * @param ostream the output stream
     */
    public void _write(org.omg.CORBA.portable.OutputStream ostream)
    {
        ILibraryHelper.write(ostream,value);
    }

    /**
     * Return the ILibrary TypeCode
     * @return a TypeCode
     */
    public org.omg.CORBA.TypeCode _type()
    {
        return ILibraryHelper.type();
    }

}
