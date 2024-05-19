package net.sf.l2j.gameserver.data.xml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.commons.data.xml.IXmlReader;

import net.sf.l2j.gameserver.model.records.custom.EventsInfo;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

public class EventsData implements IXmlReader
{
	private final List<EventsInfo> _events = new ArrayList<>();
	
	protected EventsData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseFile("./data/xml/events.xml");
		LOGGER.info("Loaded {} events.", _events.size());
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode -> forEach(listNode, "event", event ->
		{
			String eventName = event.getAttributes().getNamedItem("name").getNodeValue();
			
			final List<Integer> itemIds = new ArrayList<>();
			final List<Integer> itemCounts = new ArrayList<>();
			final List<Integer> itemChances = new ArrayList<>();
			final List<Integer> minLvls = new ArrayList<>();
			
			forEach(event, "item", itemNode ->
			{
				final NamedNodeMap attrs = itemNode.getAttributes();
				int itemId = parseInteger(attrs, "itemId");
				int itemCount = parseInteger(attrs, "itemCount");
				int itemChance = parseInteger(attrs, "itemChance");
				int minLvl = parseInteger(attrs, "minLvl", 1);
				
				// Add elements to the lists
				itemIds.add(itemId);
				itemCounts.add(itemCount);
				itemChances.add(itemChance);
				minLvls.add(minLvl);
			});
			
			// Convert lists to arrays
			int[] itemIdArray = itemIds.stream().mapToInt(Integer::intValue).toArray();
			int[] itemCountArray = itemCounts.stream().mapToInt(Integer::intValue).toArray();
			int[] itemChanceArray = itemChances.stream().mapToInt(Integer::intValue).toArray();
			int[] minLvlArray = minLvls.stream().mapToInt(Integer::intValue).toArray();
			
			// Create an instance of EventsInfo with arrays
			_events.add(new EventsInfo(itemIdArray, itemCountArray, itemChanceArray, eventName, minLvlArray));
		}));
	}
	
	public EventsInfo getEventsData(String eventName)
	{
		return _events.stream().filter(event -> event.eventName().equals(eventName)).findFirst().orElse(null);
	}
	
	public static EventsData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final EventsData INSTANCE = new EventsData();
	}
}