package net.sf.l2j.TeleportInterface;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.commons.data.StatSet;

import net.sf.l2j.TeleportInterface.Data.XMLDocument;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * This class loads and stores {@link TeleLocation}s.
 */
public class TeleportLocationData extends XMLDocument
{
	private final Map<Integer, TeleLocation> _teleports = new HashMap<>();
	
	protected TeleportLocationData()
	{
		load();
	}
	
	@Override
	protected void load()
	{
		loadDocument("./data/xml/teleportLocations.xml");
		LOG.info("Loaded {} teleport locations." + _teleports.size());
	}
	
	@Override
	protected void parseDocument(Document doc, File file)
	{
		// StatsSet used to feed informations. Cleaned on every entry.
		final StatSet set = new StatSet();
		
		// First element is never read.
		final Node n = doc.getFirstChild();
		
		for (Node o = n.getFirstChild(); o != null; o = o.getNextSibling())
		{
			if (!"teleport".equalsIgnoreCase(o.getNodeName()))
				continue;
			
			// Parse and feed content.
			parseAndFeed(o.getAttributes(), set);
			
			// Feed the map with new data.
			_teleports.put(set.getInteger("id"), new TeleLocation(set));
			
			// Clear the StatsSet.
			set.clear();
		}
	}
	
	public void reload()
	{
		_teleports.clear();
		
		load();
	}
	
	public TeleLocation getTeleportLocation(int id)
	{
		return _teleports.get(id);
	}
	
	public static TeleportLocationData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final TeleportLocationData INSTANCE = new TeleportLocationData();
	}
}