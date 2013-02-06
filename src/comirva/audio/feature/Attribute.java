package comirva.audio.feature;

import java.io.IOException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import comirva.audio.XMLSerializable;

/**
 * <b>Attribute</b>
 *
 * <p>Description: </p>
 * An <code>Attribute</code> is an abstract concept strongly realted to another
 * object, which one can attributed with an <code>Attribute</code> object.
 * One can think about the attributed object to be some kinde of a container,
 * which represents a real world object, that one can describe closer simply by
 * putting <code>Attribute</code> objects in the container. In latter the
 * attributed object is simpliy called <i>container</>.<br>
 * <br>
 * For example one could think of a container <i>House</i> being closer
 * described by an attribute <i>Color</i>, whose state could be <i>red</i> for
 * example. Another attribute could be <i>Cat</i>, telling us that there lives a
 * cat in the house.<br>
 * <br>
 * The obvious advantage is that there is no need to change the container's
 * class structure, if one has to add a new attibute, but simply can subclass
 * <code>Atrribute</code>. This is extremely usefull, if possible attributes of
 * a container are unknown at the current development stage.<br>
 * <br>
 * In combination with an <code>AttributeExtractor</code> an
 * <code>Attribute</code> object gets a further semantics. An
 * <code>Attribute</code> describes a container, and the describtion can be
 * obtained automatically by an <i>extraction process</i>. The type of
 * information needed to obtain an attribute depends on the attribute extraction
 * process and therfore can variy from attribute to attribute.
 * So if one has a large number of containers, a batch job for extracting
 * attributes is a good idea. This takes us to the concept of a
 * <code>AttributeExtractionThread</code>.<br>
 * Last but not least <code>Attributes</code> may implement the
 * <code>XMLSerializable</code> interface, such that it is easy for a container
 * to make attributes persistent.
 *
 * @see comirva.audio.extraction.AttributeExtractor
 * @see comirva.audio.XMLSerializable
 * @see comirva.audio.extraction.AudioFeatureExtractionThread
 * @author Klaus Seyerlehner
 * @version 1.0
 */
public abstract class Attribute
{
  /**
   * Constructor of an atrribute.
   */
  protected Attribute()
  {
    super();
  }


  /**
   * Returns an unique integer value for each subclass of attribute.
   * This can be used to distinguish different subclasses.  By
   * definition this is the hash code of the attribute's class name.
   *
   * @return int typevalue
   */
  public final int getType()
  {
    return this.getClassName().hashCode();
  }


  /**
   * Returns the full qualified class name of an attribute.
   *
   * @return String the full qualified class name
   */
  public final String getClassName()
  {
    return this.getClass().getName();
  }


  /**
   * Supports reding <code>Attribute</code> objects from a xml input stream.
   *
   * @param parser XMLStreamReader the xml input stream
   * @return Attribute the attibute read from the stream
   *
   * @throws IOException raised, if there are any io troubles
   * @throws XMLStreamException raised, if there are any parsing errors
   */
  public static Attribute readAttribute(XMLStreamReader parser) throws IOException, XMLStreamException
  {
    Attribute attribute = null;
    String attributeType = null;

    parser.require(XMLStreamReader.START_ELEMENT, null, "feature");
    attributeType = parser.getAttributeValue(null, "type");

    //find class
    try
    {
      Class attributeClass = Class.forName(attributeType);
      attribute = (Attribute) attributeClass.newInstance();
      if(XMLSerializable.class.isInstance(attribute))
        ((XMLSerializable)attribute).readXML(parser);
      else
        throw new IOException("the class specified in the xml stream doesn't support XMLSerializable;");
    }
    catch (ClassNotFoundException ex)
    {
      boolean skip = true;
      while(parser.hasNext() && skip)
      {
        parser.next();
        if(parser.hasName() && parser.getName().toString().equals("feature"))
        {
          skip = false;
        }
      }
      parser.require(XMLStreamReader.END_ELEMENT, null, "feature");
      throw new IOException("couldn't find the class file for this attribute;");
    }
    catch (ClassCastException cce)
    {
      throw new IOException("the class is not in the attribute hierarchie;");
    }
    catch(Exception e)
    {
      throw new IOException("some class instantiation error occured;");
    }

    parser.nextTag();
    parser.require(XMLStreamReader.END_ELEMENT, null, "feature");

    return attribute;
  }
}
