package net.sf.l2j.gameserver.scripting.script.ai.group;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.NpcStringId;
import net.sf.l2j.gameserver.scripting.Quest;

/**
 * Handles teleporting NPCs, such as Toma, Merchant of Mammon, Blacksmith of Mammon and Rooney.<br>
 * When these {@link Npc}s are spawned, they will randomly change their location.
 */
public class RandomTeleport extends Quest
{
	// NPCs
	private static final int MASTER_TOMA = 30556;
	private static final int MERCHANT_OF_MAMMON = 31113;
	private static final int BLACKSMITH_OF_MAMMON = 31126;
	private static final int BLACKSMITH_OF_WIND_ROONEY = 32049;
	
	// Teleport locations
	private static final Location TOMA[] =
	{
		new Location(151572, -174829, -1781),
		new Location(154132, -220070, -3404),
		new Location(178849, -184342, -342),
	};
	
	private static final Location ROONEY_LOC[] = // Pos_1 - Pos_39
	{
		new Location(175937, -112167, -5550),
		new Location(178896, -112425, -5860),
		new Location(180628, -115992, -6135),
		new Location(183010, -114753, -6135),
		new Location(184496, -116773, -6135),
		new Location(181857, -109491, -5865),
		new Location(178917, -107633, -5853),
		new Location(178804, -110080, -5853),
		new Location(182221, -106806, -6025),
		new Location(186488, -109715, -5915),
		new Location(183847, -119231, -3113),
		new Location(185193, -120342, -3113),
		new Location(188047, -120867, -3113),
		new Location(189734, -120471, -3113),
		new Location(188754, -118940, -3313),
		new Location(190022, -116803, -3313),
		new Location(188443, -115814, -3313),
		new Location(186421, -114614, -3313),
		new Location(185188, -113307, -3313),
		new Location(187378, -112946, -3313),
		new Location(189815, -113425, -3313),
		new Location(189301, -111327, -3313),
		new Location(190289, -109176, -3313),
		new Location(187783, -110478, -3313),
		new Location(185889, -109990, -3313),
		new Location(181881, -109060, -3695),
		new Location(183570, -111344, -3675),
		new Location(182077, -112567, -3695),
		new Location(180127, -112776, -3698),
		new Location(179155, -108629, -3695),
		new Location(176282, -109510, -3698),
		new Location(176071, -113163, -3515),
		new Location(179376, -117056, -3640),
		new Location(179760, -115385, -3640),
		new Location(177950, -119691, -4140),
		new Location(177037, -120820, -4340),
		new Location(181125, -120148, -3702),
		new Location(182212, -117969, -3352),
		new Location(186074, -118154, -3312),
	};
	
	public RandomTeleport()
	{
		super(-1, "ai/group");
		
		addCreated(MASTER_TOMA, MERCHANT_OF_MAMMON, BLACKSMITH_OF_MAMMON, BLACKSMITH_OF_WIND_ROONEY);
		addDecayed(MERCHANT_OF_MAMMON, BLACKSMITH_OF_MAMMON);
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("toma"))
			npc.teleportTo(Rnd.get(TOMA), 0);
		else if (name.equalsIgnoreCase("mom"))
		{
			int i0 = Rnd.get(80);
			String loc = null;
			
			if (i0 <= 10 && npc.getSpawn().getMemo().getInteger("PosX1", 0) != 0)
			{
				loc = npc.getSpawn().getMemo().get("Name1");
				npc.teleportTo(npc.getSpawn().getMemo().getInteger("PosX1", 0), npc.getSpawn().getMemo().getInteger("PosY1", 0), npc.getSpawn().getMemo().getInteger("PosZ1", 0), 0);
			}
			else if (i0 > 10 && i0 <= 20 && npc.getSpawn().getMemo().getInteger("PosX2", 0) != 0)
			{
				loc = npc.getSpawn().getMemo().get("Name2");
				npc.teleportTo(npc.getSpawn().getMemo().getInteger("PosX2", 0), npc.getSpawn().getMemo().getInteger("PosY2", 0), npc.getSpawn().getMemo().getInteger("PosZ2", 0), 0);
			}
			else if (i0 > 20 && i0 <= 30 && npc.getSpawn().getMemo().getInteger("PosX3", 0) != 0)
			{
				loc = npc.getSpawn().getMemo().get("Name3");
				npc.teleportTo(npc.getSpawn().getMemo().getInteger("PosX3", 0), npc.getSpawn().getMemo().getInteger("PosY3", 0), npc.getSpawn().getMemo().getInteger("PosZ3", 0), 0);
			}
			else if (i0 > 30 && i0 <= 40 && npc.getSpawn().getMemo().getInteger("PosX4", 0) != 0)
			{
				loc = npc.getSpawn().getMemo().get("Name4");
				npc.teleportTo(npc.getSpawn().getMemo().getInteger("PosX4", 0), npc.getSpawn().getMemo().getInteger("PosY4", 0), npc.getSpawn().getMemo().getInteger("PosZ4", 0), 0);
			}
			else if (i0 > 40 && i0 <= 50 && npc.getSpawn().getMemo().getInteger("PosX5", 0) != 0)
			{
				loc = npc.getSpawn().getMemo().get("Name5");
				npc.teleportTo(npc.getSpawn().getMemo().getInteger("PosX5", 0), npc.getSpawn().getMemo().getInteger("PosY5", 0), npc.getSpawn().getMemo().getInteger("PosZ5", 0), 0);
			}
			else if (i0 > 50 && i0 <= 60 && npc.getSpawn().getMemo().getInteger("PosX6", 0) != 0)
			{
				loc = npc.getSpawn().getMemo().get("Name6");
				npc.teleportTo(npc.getSpawn().getMemo().getInteger("PosX6", 0), npc.getSpawn().getMemo().getInteger("PosY6", 0), npc.getSpawn().getMemo().getInteger("PosZ6", 0), 0);
			}
			else if (i0 > 60 && i0 <= 70 && npc.getSpawn().getMemo().getInteger("PosX7", 0) != 0)
			{
				loc = npc.getSpawn().getMemo().get("Name7");
				npc.teleportTo(npc.getSpawn().getMemo().getInteger("PosX7", 0), npc.getSpawn().getMemo().getInteger("PosY7", 0), npc.getSpawn().getMemo().getInteger("PosZ7", 0), 0);
			}
			else if (npc.getSpawn().getMemo().getInteger("PosX8", 0) != 0)
			{
				loc = npc.getSpawn().getMemo().get("Name8");
				npc.teleportTo(npc.getSpawn().getMemo().getInteger("PosX8", 0), npc.getSpawn().getMemo().getInteger("PosY8", 0), npc.getSpawn().getMemo().getInteger("PosZ8", 0), 0);
			}
			
			// Announce the location change
			if (loc != null && Config.ANNOUNCE_MAMMON_SPAWN)
				World.announceToOnlinePlayers("Merchant of Mammon in: " + loc);
		}
		else if (name.equalsIgnoreCase("bom"))
		{
			int i0 = Rnd.get(70);
			String loc = null;
			
			if (i0 <= 10 && npc.getSpawn().getMemo().getInteger("PosX1", 0) != 0)
			{
				loc = npc.getSpawn().getMemo().get("Name1");
				npc.teleportTo(npc.getSpawn().getMemo().getInteger("PosX1", 0), npc.getSpawn().getMemo().getInteger("PosY1", 0), npc.getSpawn().getMemo().getInteger("PosZ1", 0), 0);
			}
			else if (i0 > 10 && i0 <= 20 && npc.getSpawn().getMemo().getInteger("PosX2", 0) != 0)
			{
				loc = npc.getSpawn().getMemo().get("Name2");
				npc.teleportTo(npc.getSpawn().getMemo().getInteger("PosX2", 0), npc.getSpawn().getMemo().getInteger("PosY2", 0), npc.getSpawn().getMemo().getInteger("PosZ2", 0), 0);
			}
			else if (i0 > 20 && i0 <= 30 && npc.getSpawn().getMemo().getInteger("PosX3", 0) != 0)
			{
				loc = npc.getSpawn().getMemo().get("Name3");
				npc.teleportTo(npc.getSpawn().getMemo().getInteger("PosX3", 0), npc.getSpawn().getMemo().getInteger("PosY3", 0), npc.getSpawn().getMemo().getInteger("PosZ3", 0), 0);
			}
			else if (i0 > 30 && i0 <= 40 && npc.getSpawn().getMemo().getInteger("PosX4", 0) != 0)
			{
				loc = npc.getSpawn().getMemo().get("Name4");
				npc.teleportTo(npc.getSpawn().getMemo().getInteger("PosX4", 0), npc.getSpawn().getMemo().getInteger("PosY4", 0), npc.getSpawn().getMemo().getInteger("PosZ4", 0), 0);
			}
			else if (i0 > 40 && i0 <= 50 && npc.getSpawn().getMemo().getInteger("PosX5", 0) != 0)
			{
				loc = npc.getSpawn().getMemo().get("Name5");
				npc.teleportTo(npc.getSpawn().getMemo().getInteger("PosX5", 0), npc.getSpawn().getMemo().getInteger("PosY5", 0), npc.getSpawn().getMemo().getInteger("PosZ5", 0), 0);
			}
			else if (i0 > 50 && i0 < 60 && npc.getSpawn().getMemo().getInteger("PosX6", 0) != 0)
			{
				loc = npc.getSpawn().getMemo().get("Name6");
				npc.teleportTo(npc.getSpawn().getMemo().getInteger("PosX6", 0), npc.getSpawn().getMemo().getInteger("PosY6", 0), npc.getSpawn().getMemo().getInteger("PosZ6", 0), 0);
			}
			
			int i1 = Rnd.get(30);
			if (i1 < 10)
				npc.broadcastNpcShout(NpcStringId.ID_1000431);
			else if (i1 >= 10 && i1 < 20)
				npc.broadcastNpcShout(NpcStringId.ID_1000432);
			else
				npc.broadcastNpcShout(NpcStringId.ID_1000433);
			
			if (loc != null && Config.ANNOUNCE_MAMMON_SPAWN)
				World.announceToOnlinePlayers("Blacksmith of Mammon in: " + loc);
		}
		else if (name.equalsIgnoreCase("rooney"))
			npc.teleportTo(Rnd.get(ROONEY_LOC), 0);
		
		return null;
	}
	
	@Override
	public void onCreated(Npc npc)
	{
		switch (npc.getNpcId())
		{
			case MASTER_TOMA:
				startQuestTimerAtFixedRate("toma", npc, null, 1800000, 1800000);
				break;
			
			case MERCHANT_OF_MAMMON:
				startQuestTimerAtFixedRate("mom", npc, null, 1800000, 1800000);
				break;
			
			case BLACKSMITH_OF_MAMMON:
				startQuestTimerAtFixedRate("bom", npc, null, 1800000, 1800000);
				break;
			
			case BLACKSMITH_OF_WIND_ROONEY:
				startQuestTimerAtFixedRate("rooney", npc, null, 1800000, 1800000);
				break;
		}
		super.onCreated(npc);
	}
	
	@Override
	public void onDecayed(Npc npc)
	{
		cancelQuestTimers("mom", npc);
		cancelQuestTimers("bom", npc);
		
		super.onDecayed(npc);
	}
}