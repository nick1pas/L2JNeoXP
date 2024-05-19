package net.sf.l2j.TeleportInterface;

import net.sf.l2j.commons.data.StatSet;

import net.sf.l2j.gameserver.model.location.Location;


/**
 * A datatype extending {@link Location}, used to retain a single Gatekeeper teleport location.
 */
public class TeleLocation extends Location
{
	public TeleLocation(StatSet set)
	{
	    super(set.getInteger("x"), set.getInteger("y"), set.getInteger("z"));

	
	    _price = set.getInteger("price");
	    
	    _isNoble = set.getBool("isNoble");
	}

	
	private final int _price;
	private final boolean _isNoble;
	
	public int getPrice()
	{
		return _price;
	}
	
	public boolean isNoble()
	{
		return _isNoble;
	}
}