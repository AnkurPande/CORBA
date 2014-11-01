package common;

/** 
 * Helper class for : ILibrary
 *  
 * @author OpenORB Compiler
 */ 
public class ILibraryHelper
{
    /**
     * Insert ILibrary into an any
     * @param a an any
     * @param t ILibrary value
     */
    public static void insert(org.omg.CORBA.Any a, common.ILibrary t)
    {
        a.insert_Object(t , type());
    }

    /**
     * Extract ILibrary from an any
     *
     * @param a an any
     * @return the extracted ILibrary value
     */
    public static common.ILibrary extract( org.omg.CORBA.Any a )
    {
        if ( !a.type().equivalent( type() ) )
        {
            throw new org.omg.CORBA.MARSHAL();
        }
        try
        {
            return common.ILibraryHelper.narrow( a.extract_Object() );
        }
        catch ( final org.omg.CORBA.BAD_PARAM e )
        {
            throw new org.omg.CORBA.MARSHAL(e.getMessage());
        }
    }

    //
    // Internal TypeCode value
    //
    private static org.omg.CORBA.TypeCode _tc = null;

    /**
     * Return the ILibrary TypeCode
     * @return a TypeCode
     */
    public static org.omg.CORBA.TypeCode type()
    {
        if (_tc == null) {
            org.omg.CORBA.ORB orb = org.omg.CORBA.ORB.init();
            _tc = orb.create_interface_tc( id(), "ILibrary" );
        }
        return _tc;
    }

    /**
     * Return the ILibrary IDL ID
     * @return an ID
     */
    public static String id()
    {
        return _id;
    }

    private final static String _id = "IDL:common/ILibrary:1.0";

    /**
     * Read ILibrary from a marshalled stream
     * @param istream the input stream
     * @return the readed ILibrary value
     */
    public static common.ILibrary read(org.omg.CORBA.portable.InputStream istream)
    {
        return(common.ILibrary)istream.read_Object(common._ILibraryStub.class);
    }

    /**
     * Write ILibrary into a marshalled stream
     * @param ostream the output stream
     * @param value ILibrary value
     */
    public static void write(org.omg.CORBA.portable.OutputStream ostream, common.ILibrary value)
    {
        ostream.write_Object((org.omg.CORBA.portable.ObjectImpl)value);
    }

    /**
     * Narrow CORBA::Object to ILibrary
     * @param obj the CORBA Object
     * @return ILibrary Object
     */
    public static ILibrary narrow(org.omg.CORBA.Object obj)
    {
        if (obj == null)
            return null;
        if (obj instanceof ILibrary)
            return (ILibrary)obj;

        if (obj._is_a(id()))
        {
            _ILibraryStub stub = new _ILibraryStub();
            stub._set_delegate(((org.omg.CORBA.portable.ObjectImpl)obj)._get_delegate());
            return stub;
        }

        throw new org.omg.CORBA.BAD_PARAM();
    }

    /**
     * Unchecked Narrow CORBA::Object to ILibrary
     * @param obj the CORBA Object
     * @return ILibrary Object
     */
    public static ILibrary unchecked_narrow(org.omg.CORBA.Object obj)
    {
        if (obj == null)
            return null;
        if (obj instanceof ILibrary)
            return (ILibrary)obj;

        _ILibraryStub stub = new _ILibraryStub();
        stub._set_delegate(((org.omg.CORBA.portable.ObjectImpl)obj)._get_delegate());
        return stub;

    }

}
