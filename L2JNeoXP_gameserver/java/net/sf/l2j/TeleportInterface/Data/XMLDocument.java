package net.sf.l2j.TeleportInterface.Data;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.l2j.commons.data.StatSet;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * An XML document, relying on a static and single DocumentBuilderFactory.
 */
public abstract class XMLDocument
{
	protected static final Logger LOG = Logger.getLogger(XMLDocument.class.getName());
	
	protected Document document;
	
	private static final DocumentBuilderFactory BUILDER;
	static
	{
		BUILDER = DocumentBuilderFactory.newInstance();
		BUILDER.setValidating(false);
		BUILDER.setIgnoringComments(true);
	}
	
	abstract protected void load();
	
	abstract protected void parseDocument(Document doc, File f);
	
	public void loadDocument(String filePath)
	{
		loadDocument(new File(filePath));
	}
	
	public void writeDocument(Document doc, String fileName)
	{
		try
		{
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(fileName));
			
			transformer.transform(source, result);
			LOG.info("XML file saved to " + fileName);
		}
		catch (TransformerException e)
		{
			LOG.warning("Error saving XML file: " + e.getMessage());
		}
	}
	
	/**
	 * Parse an entire directory or file if found.
	 * @param file
	 */
	public void loadDocument(File file)
	{
		if (!file.exists())
		{
			LOG.severe("The following file or directory doesn't exist: " + file.getName());
			return;
		}
		
		if (file.isDirectory())
		{
			for (File f : file.listFiles())
			{
				loadDocument(f);
			}
		}
		else if (file.isFile())
		{
			try
			{
				parseDocument(BUILDER.newDocumentBuilder().parse(file), file);
			}
			catch (Exception e)
			{
				LOG.log(Level.SEVERE, "Error loading XML file " + file.getName(), e);
			}
		}
	}
	
	public Document getDocument()
	{
		return document;
	}
	
	/**
	 * This method parses the content of a NamedNodeMap and feed the given StatsSet.
	 * @param attrs : The NamedNodeMap to parse.
	 * @param set : The StatsSet to feed.
	 */
	public static void parseAndFeed(NamedNodeMap attrs, StatSet set)
	{
		for (int i = 0; i < attrs.getLength(); i++)
		{
			final Node attr = attrs.item(i);
			set.set(attr.getNodeName(), attr.getNodeValue());
		}
	}
}