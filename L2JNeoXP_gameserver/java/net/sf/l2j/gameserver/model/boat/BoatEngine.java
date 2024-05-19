package net.sf.l2j.gameserver.model.boat;

import java.util.Iterator;
import java.util.List;

import net.sf.l2j.gameserver.data.xml.BoatData;
import net.sf.l2j.gameserver.enums.boats.BoatDock;
import net.sf.l2j.gameserver.enums.boats.BoatSound;
import net.sf.l2j.gameserver.enums.boats.BoatState;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Boat;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.BoatLocation;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.taskmanager.BoatTaskManager;

public class BoatEngine implements Runnable
{
	private static final int BOAT_BROADCAST_RADIUS = 20000;
	
	private Iterator<ScheduledBoatMessages> _messages;
	
	private final Boat _boat;
	private final int _waitDelay;
	private final BoatRouteInfo[] _info;
	private final PlaySound[] _sounds = new PlaySound[3];
	
	private int _shoutCount = 0;
	private int _delay = 0;
	
	private BoatDock _currentDock;
	private BoatDock _destinationDock;
	
	private BoatState _state = BoatState.PREPARING;
	
	public BoatEngine(BoatItinerary itinerary)
	{
		_boat = BoatData.getInstance().getNewBoat(itinerary);
		_boat.setEngine(this);
		
		_currentDock = itinerary.getInfo()[0].getDock();
		_destinationDock = (itinerary.isOneWay()) ? _currentDock : itinerary.getInfo()[1].getDock();
		
		_info = itinerary.getInfo();
		
		_waitDelay = (_currentDock == BoatDock.RUNE && _destinationDock == BoatDock.PRIMEVAL) ? 0 : 300;
		_delay = _waitDelay;
		
		_sounds[0] = BoatSound.LEAVE_5_MIN.get(_boat);
		_sounds[1] = BoatSound.LEAVE_1_MIN.get(_boat);
		_sounds[2] = BoatSound.ARRIVAL_DEPARTURE.get(_boat);
		
		// Prepare messages.
		_messages = getRouteInfoByDock(_currentDock).getScheduledMessages().iterator();
		
		// Register this BoatEngine in the scheduler.
		BoatTaskManager.getInstance().add(this);
	}
	
	/**
	 * @return the {@link BoatDock} which represent the entrance from the dock to the Boat.
	 */
	public BoatDock getDock()
	{
		return _currentDock;
	}
	
	public BoatState getState()
	{
		return _state;
	}
	
	public void setState(BoatState state)
	{
		_state = state;
	}
	
	/**
	 * Broadcast one or several {@link L2GameServerPacket}(s) in both path points.
	 * @param packets : The {@link L2GameServerPacket}(s) to broadcast.
	 */
	public void broadcast(List<L2GameServerPacket> packets)
	{
		if (packets == null || packets.isEmpty())
			return;
		
		for (Player player : World.getInstance().getPlayers())
		{
			if (player.isIn2DRadius(_info[0].getDock().getDockLoc(), BOAT_BROADCAST_RADIUS) || (_info[1] != null && player.isIn2DRadius(_info[1].getDock().getDockLoc(), BOAT_BROADCAST_RADIUS)))
			{
				for (L2GameServerPacket packet : packets)
					player.sendPacket(packet);
			}
		}
	}
	
	/**
	 * Broadcast one or several {@link L2GameServerPacket}(s) in both path points.
	 * @param packets : The {@link L2GameServerPacket}(s) to broadcast.
	 */
	public void broadcast(L2GameServerPacket... packets)
	{
		if (packets == null || packets.length == 0)
			return;
		
		for (Player player : World.getInstance().getPlayers())
		{
			if (player.isIn2DRadius(_info[0].getDock().getDockLoc(), BOAT_BROADCAST_RADIUS) || (_info[1] != null && player.isIn2DRadius(_info[1].getDock().getDockLoc(), BOAT_BROADCAST_RADIUS)))
			{
				for (L2GameServerPacket packet : packets)
					player.sendPacket(packet);
			}
		}
	}
	
	public boolean canRun()
	{
		if (_delay > 0)
			_delay--;
		
		return _delay == 0;
	}
	
	/**
	 * @param dock : The {@link BoatDock} used as parameter.
	 * @return The {@link BoatRouteInfo} associated to the {@link BoatDock} set as parameter.
	 */
	public BoatRouteInfo getRouteInfoByDock(BoatDock dock)
	{
		for (BoatRouteInfo routeInfo : _info)
		{
			if (routeInfo.getDock() == dock)
				return routeInfo;
		}
		return null;
	}
	
	@Override
	public void run()
	{
		switch (_state)
		{
			case PREPARING:
				if (_messages.hasNext())
				{
					final ScheduledBoatMessages sm = _messages.next();
					
					// Broadcast messages.
					broadcast(sm.getMessages());
					
					// Set the delay.
					_delay = sm.getDelay();
					
					switch (_delay)
					{
						case 240 -> broadcast(_sounds[0]);
						case 40, 20 -> broadcast(_sounds[1]);
					}
				}
				else
					_state = BoatState.EXECUTE_ROUTE;
				
				break;
			
			case EXECUTE_ROUTE:
				final BoatRouteInfo info = getRouteInfoByDock(_currentDock);
				
				_boat.payForRide(info.getItemId(), info.getDock().getOustLoc());
				_boat.getMove().executePath(info.getPaths());
				
				info.getDock().setBusy(false);
				
				// Broadcast ship_arrival_departure sound.
				broadcast(_sounds[2]);
				
				_state = BoatState.SEALING;
				break;
			
			case READY_TO_MOVE_TO_DOCK:
				if (_destinationDock.isBusy())
				{
					_delay = 5;
					
					if (_shoutCount == 0)
						broadcast(getRouteInfoByDock(_destinationDock).getBusyMessage());
					
					_shoutCount++;
					if (_shoutCount > 35)
						_shoutCount = 0;
					
					_shoutCount = (_shoutCount + 1) % 36;
					return;
				}
				
				_destinationDock.setBusy(true);
				
				final BoatLocation[] path = getRouteInfoByDock(_currentDock).getPaths();
				_boat.getMove().moveToBoatLocation(path[path.length - 1]);
				
				_state = BoatState.SEALING;
				break;
			
			case DOCKED:
				_delay = _waitDelay;
				
				// Broadcast ship_arrival_departure sound.
				broadcast(_sounds[2]);
				
				// Swap the docks.
				final BoatDock temp = _currentDock;
				
				_currentDock = _destinationDock;
				_destinationDock = temp;
				
				// Prepare messages.
				_messages = getRouteInfoByDock(_currentDock).getScheduledMessages().iterator();
				
				_boat.getKnownType(Player.class).forEach(_boat::sendInfo);
				
				_state = BoatState.PREPARING;
				break;
		}
	}
}