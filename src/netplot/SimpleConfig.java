package netplot;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

/**
 * A Class that contains methods that save its own the state (the values of its
 * own attributes). This class may be subclassed. Only the following attribute
 * types will be saved.
 * 
 * byte short int long float double boolean String Vector (May only contain
 * Strings) Hashtable (May only contain Strings)
 * 
 */
public class SimpleConfig
{
  public String configurationFileHeader = "";

  /**
   * Save this objects state to a file.
   * 
   * @param filename
   *          The filename to save to. The filename will have the java.home path
   *          pre appended to it. The filename will also be pre-appended with
   *          the . (period) char.
   * 
   * @return The file that the object was saved to.
   * 
   * @throws IllegalAccessException
   * @throws FileNotFoundException
   * @throws IOException
   */
  public File save(String filename) throws IllegalAccessException, FileNotFoundException, IOException
  {
    return save(filename, true);
  }

  /**
   * Save this objects state to a file.
   * 
   * @param filename
   *          The filename to save to.
   * @param useJavaHomePath
   *          If true then the java.home path is pre appended to the filename
   *          and the filename is preappended with the . (period) char. If false
   *          then the filename is used as is.
   * 
   * @return The file that the object was saved to.
   * 
   * @throws IllegalAccessException
   * @throws FileNotFoundException
   * @throws IOException
   */
  public File save(String filename, boolean useJavaHomePath) throws IllegalAccessException, FileNotFoundException, IOException
  {
    String  _string;
    Properties properties = new Properties();
    Field fields[] = this.getClass().getFields();
    for (int i = 0; i < fields.length; i++)
    {
      Object o = fields[i].get(this);
      //If the object is a null string then set to an empty string
      if( o == null )
      {
        _string="";
      }
      else
      {
        _string = fields[i].get(this).toString();
      }
      //Save un encrypted data
      properties.put(fields[i].getName(), _string);
    }
    File file = SimpleConfig.GetConfigFile(filename, useJavaHomePath);
    FileOutputStream fos = new FileOutputStream(file);
    properties.store(fos, configurationFileHeader);
    fos.close();
    return file;
  }
  
  /**
   * Set the attributes of the objectInstance to the key values defined in the properties object.
   *  
   * @param objectInstance
   * @param properties
   *        
   * @throws IllegalAccessException
   */
  public static void LoadFields(Object objectInstance, Properties properties) throws IllegalAccessException, IOException
  {
    String key, value;

    //Get a list of all the fields of this class
    Field fields[] = objectInstance.getClass().getFields();

    Enumeration<Object> enum_ = properties.keys();
    // load each parameter/field
    while (enum_.hasMoreElements())
    {
      key = enum_.nextElement().toString();
      value = properties.getProperty(key);
      // Look for a match between the key and the name of a field of this class
      for (int i = 0; i < fields.length; i++)
      {
        // If this is the correct field
        if (fields[i].getName().compareTo(key) == 0)
        {
          if (fields[i].getType().getName().equals("java.util.Vector"))
          {
            fields[i].set(objectInstance, SimpleConfig.ParseVector(value));
          }
          if (fields[i].getType().getName().equals("java.util.Hashtable"))
          {
            fields[i].set(objectInstance, SimpleConfig.ParseHashtable(value));
          }
          if (fields[i].getType().getName().equals("byte") && value != null)
          {
            fields[i].setByte(objectInstance, Byte.parseByte(value));
          }
          if (fields[i].getType().getName().equals("char") && value != null)
          {
            fields[i].setChar(objectInstance, value.charAt(0));
          }
          if (fields[i].getType().getName().equals("short") && value != null)
          {
            fields[i].setShort(objectInstance, Short.parseShort(value));
          }
          if (fields[i].getType().getName().equals("int") && value != null)
          {
            fields[i].setInt(objectInstance, Integer.parseInt(value));
          }
          if (fields[i].getType().getName().equals("long") && value != null)
          {
            fields[i].setLong(objectInstance, Long.parseLong(value));
          }
          if (fields[i].getType().getName().equals("float") && value != null)
          {
            fields[i].setFloat(objectInstance, Float.parseFloat(value));
          }
          if (fields[i].getType().getName().equals("double") && value != null)
          {
            fields[i].setDouble(objectInstance, Double.parseDouble(value));
          }
          if (fields[i].getType().getName().equals("boolean") && value != null)
          {
            fields[i].setBoolean(objectInstance, Boolean.parseBoolean(value));
          }
          if (fields[i].getType().getName().equals("java.lang.String"))
          {
            fields[i].set(objectInstance, value);
          }
        }
      }
    }
  }

  /**
   * Load this objects state from the file.
   * 
   * @param filename
   *          The filename to save to.
   * @param useJavaHomePath
   *          If true then the java.home path is pre appended to the filename
   *          and the filename is pre appended with the . (period) char. If false
   *          then the filename is used as is.
   *        
   * @throws FileNotFoundException
   * @throws IOException
   * @throws IllegalAccessException
   */
  public void load(String filename, boolean useJavaHomePath) throws FileNotFoundException, IOException, IllegalAccessException
  {
    File file = SimpleConfig.GetConfigFile(filename, useJavaHomePath);

    FileInputStream fis = new FileInputStream(file);
    Properties properties = new Properties();

    properties.load(fis);

    LoadFields(this,properties);
  }
  public void load(String filename) throws FileNotFoundException, IOException, IllegalAccessException
  {
    this.load(filename, true);
  }

  
  /**
   * Parse a Vector. All elements of the Vector will be Strings.
   * 
   * @param value
   *          The value string (e.g [A,B,1])
   * @return The Vector object.
   */
  private static Vector<String> ParseVector(String value) throws IOException
  {
    if( value == null )
    {
      return null;
    }
    Vector <String>vector = new Vector<String>(0, 1);
    // If we have enough chars to read elements from
    if (value.length() > 2)
    {
      String values = value.substring(1, value.length() - 1);
      StringTokenizer strTok = new StringTokenizer(values, ",");
      int tokCount = strTok.countTokens();
      // Add each string to the Vector
      for (int i = 0; i < tokCount; i++)
      {
        vector.add(strTok.nextToken().trim());
      }
    }
    return vector;
  }

  /**
   * Parse a Hashtable. All elements of the Hashtable will be Strings.
   * 
   * @param value
   *          The value string (e.g {1,2,3} )
   * @param crypt
   *          The encryption/decryption object. If null no decryption occurs.
   * @return The Hashtable object.
   */
  private static Hashtable<String, String> ParseHashtable(String value) throws IOException
  {
    if( value == null )
    {
      return null;
    }
    String element, k, v;
    Hashtable<String, String> hashtable = new Hashtable<String, String>();
    // If we have enough chars to read elements from
    if (value.length() > 2)
    {
      String values = value.substring(1, value.length() - 1);
      StringTokenizer strTok = new StringTokenizer(values, ",");
      int tokCount = strTok.countTokens();
      // Add each string to the Vector
      for (int i = 0; i < tokCount; i++)
      {
        element = strTok.nextToken().trim();
        StringTokenizer strTok1 = new StringTokenizer(element, "=");
        if (strTok1.countTokens() == 2)
        {
          k = strTok1.nextToken();
          v = strTok1.nextToken();
          hashtable.put(k, v);
        }
      }
    }
    return hashtable;
  }

  /**
   * Set the line of text that will be saved at the start of the config file.
   * 
   * @param configurationFileHeader
   */
  public void setConfigFileHeaderLine(String configurationFileHeader)
  {
    this.configurationFileHeader = configurationFileHeader;
  }

  /**
   * Get the line of text that will be saved at the start of the config file.
   * 
   * @return The config file header text.
   */
  public String getConfigFileHeaderLine()
  {
    return configurationFileHeader;
  }

  /**
   * Get the config file object
   * 
   * @param filename
   * @param useJavaHomePath
   * @return The config file object
   */
  public static File GetConfigFile(String filename, boolean useJavaHomePath)
  {
    File file;
    if (useJavaHomePath)
    {
      file = new File(System.getProperty("user.home"), "." + filename);
    } else
    {
      file = new File(filename);
    }
    return file;
  }

}
