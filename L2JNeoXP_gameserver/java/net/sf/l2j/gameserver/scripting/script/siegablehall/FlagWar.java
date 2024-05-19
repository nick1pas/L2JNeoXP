package net.sf.l2j.gameserver.scripting.script.siegablehall;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.manager.SpawnManager;
import net.sf.l2j.gameserver.data.sql.ClanTable;
import net.sf.l2j.gameserver.enums.RestartType;
import net.sf.l2j.gameserver.enums.SiegeSide;
import net.sf.l2j.gameserver.enums.SiegeStatus;
import net.sf.l2j.gameserver.enums.SpawnType;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.model.residence.clanhall.ClanHallSiege;
import net.sf.l2j.gameserver.model.spawn.NpcMaker;
import net.sf.l2j.gameserver.model.spawn.Spawn;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public abstract class FlagWar extends ClanHallSiege
{
	private static final String SELECT_ATTACKERS = "SELECT * FROM clanhall_flagwar_attackers WHERE clanhall_id = ?";
	private static final String INSERT_ATTACKERS = "INSERT INTO clanhall_flagwar_attackers VALUES(?,?,?,?)";
	private static final String DELETE_ATTACKERS = "DELETE FROM clanhall_flagwar_attackers WHERE clanhall_id = ?";
	private static final String UPDATE_ATTACKERS_NPC = "UPDATE clanhall_flagwar_attackers SET npc = ? WHERE clan_id = ?";
	
	private static final String SELECT_MEMBERS = "SELECT object_id FROM clanhall_flagwar_members WHERE clan_id = ?";
	private static final String INSERT_MEMBERS = "INSERT INTO clanhall_flagwar_members VALUES (?,?,?)";
	private static final String DELETE_MEMBERS = "DELETE FROM clanhall_flagwar_members WHERE clanhall_id = ?";
	
	protected String MAKER_NAME;
	
	protected int ROYAL_FLAG;
	protected int FLAG_RED;
	protected int FLAG_YELLOW;
	protected int FLAG_GREEN;
	protected int FLAG_BLUE;
	protected int FLAG_PURPLE;
	
	protected int ALLY_1;
	protected int ALLY_2;
	protected int ALLY_3;
	protected int ALLY_4;
	protected int ALLY_5;
	
	protected int TELEPORT_1;
	
	protected int MESSENGER;
	
	protected int[] OUTTER_DOORS;
	protected int[] INNER_DOORS;
	protected SpawnLocation[] FLAG_COORDS;
	
	protected int QUEST_REWARD;
	
	protected SpawnLocation CENTER;
	
	protected Map<Integer, ClanData> _data;
	protected Clan _winner;
	private boolean _firstPhase = true;
	private List<Npc> _npcSpawns = new ArrayList<>();
	private List<Npc> _tentSpawns = new ArrayList<>();
	private List<Npc> _clanlessNpcSpawns = new ArrayList<>();
	private List<Npc> _clanlessTentSpawns = new ArrayList<>();
	
	public FlagWar(String name, final int hallId)
	{
		super("siegablehall", hallId);
		
		// If siege ends w/ more than 1 flag alive, winner is old owner
		_winner = ClanTable.getInstance().getClan(_hall.getOwnerId());
	}
	
	public abstract String getFlagHtml(int flag);
	
	public abstract String getAllyHtml(int ally);
	
	public Location getClanRestartPoint(Clan clan)
	{
		if (clan == null)
			return null;
		
		final ClanData cd = _data.get(clan.getClanId());
		
		if (cd == null)
			return null;
		
		final int restartPoint = cd.flag - ROYAL_FLAG;
		final int restartPointNpcId = TELEPORT_1 + restartPoint;
		
		final Npc restartPointNpc = World.getInstance().getNpc(restartPointNpcId);
		if (restartPointNpc != null)
			return restartPointNpc.getSpawnLocation();
		
		return null;
	}
	
	protected void attachListeners()
	{
		addFirstTalkId(MESSENGER);
		addTalkId(MESSENGER);
		
		for (int i = 0; i < 6; i++)
			addFirstTalkId(TELEPORT_1 + i);
		
		addCreated(ALLY_1, ALLY_2, ALLY_3, ALLY_4, ALLY_5);
		addMyDying(ALLY_1, ALLY_2, ALLY_3, ALLY_4, ALLY_5);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String html = null;
		
		if (npc.getNpcId() == MESSENGER)
		{
			if (!checkSide(player.getClan(), SiegeSide.ATTACKER))
			{
				Clan clan = ClanTable.getInstance().getClan(_hall.getOwnerId());
				
				html = getHtmlText(player, "messenger_initial.htm");
				html = html.replaceAll("%clanName%", (clan == null) ? "no owner" : clan.getName());
				html = html.replaceAll("%objectId%", String.valueOf(npc.getObjectId()));
			}
			else
			{
				html = "messenger_initial.htm";
			}
		}
		else
		{
			int index = npc.getNpcId() - TELEPORT_1;
			if ((index == 0 && _firstPhase) || npc._i_ai1 != 0)
			{
				html = "teleporter_notyet.htm";
			}
			else
			{
				npc._i_ai1 = 1;
				teleportPlayers(npc);
				startQuestTimer("clear-tp-delay", npc, player, 60000);
			}
		}
		
		return html;
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String html = event;
		Clan clan = player.getClan();
		
		if (event.startsWith("register_clan")) // Register the clan for the siege
		{
			if (!_hall.isRegistering())
			{
				if (_hall.isInSiege())
				{
					html = "messenger_registrationpassed.htm";
				}
				else
				{
					html = getHtmlText(player, "siege_date.htm");
					html = html.replace("%nextSiege%", _hall.getSiegeDate().getTime().toString());
				}
			}
			else if (!player.isClanLeader())
			{
				html = "messenger_notclannotleader.htm";
			}
			else if (_hall.getOwnerId() == clan.getClanId())
			{
				html = "messenger_curownermessage.htm";
			}
			else if (checkSide(clan, SiegeSide.ATTACKER))
			{
				html = "messenger_clanalreadyregistered.htm";
			}
			else if (getAttackerClans().size() >= (5 + (_hall.getOwnerId() == 0 ? 1 : 0)))
			{
				html = "messenger_attackersqueuefull.htm";
			}
			else
			{
				String[] arg = event.split(" ");
				if (arg.length >= 2)
				{
					// Register passing the quest
					if (arg[1].equals("wQuest"))
					{
						if (player.destroyItemByItemId(QUEST_REWARD, 1, false)) // Quest passed
						{
							registerClan(clan);
							html = getFlagHtml(_data.get(clan.getClanId()).flag);
						}
						else
						{
							html = "messenger_noquest.htm";
						}
					}
					// Register paying the fee
					else if (arg[1].equals("wFee") && canPayRegistration())
					{
						if (player.reduceAdena(200000, false)) // Fee payed
						{
							registerClan(clan);
							html = getFlagHtml(_data.get(clan.getClanId()).flag);
						}
						else
						{
							html = "messenger_nomoney.htm";
						}
					}
				}
			}
		}
		// Select the flag to defend
		else if (event.startsWith("select_clan_npc"))
		{
			if (!player.isClanLeader())
			{
				html = "messenger_onlyleaderselectally.htm";
			}
			else if (!_data.containsKey(clan.getClanId()))
			{
				html = "messenger_clannotregistered.htm";
			}
			else
			{
				String[] var = event.split(" ");
				if (var.length >= 2)
				{
					int id = 0;
					try
					{
						id = Integer.parseInt(var[1]);
					}
					catch (Exception e)
					{
						LOGGER.error("Couldn't parse integer {} for {}.", e, var[1], getName());
					}
					
					if (id > 0 && (html = getAllyHtml(id)) != null)
					{
						_data.get(clan.getClanId()).npc = id;
						saveNpc(id, clan.getClanId());
					}
				}
			}
		}
		// View (and change ? ) the current selected mahum warrior
		else if (event.startsWith("view_clan_npc"))
		{
			ClanData cd = null;
			if (clan == null)
			{
				html = "messenger_clannotregistered.htm";
			}
			else if ((cd = _data.get(clan.getClanId())) == null)
			{
				html = "messenger_notclannotleader.htm";
			}
			else if (cd.npc == 0)
			{
				html = "messenger_leaderdidnotchooseyet.htm";
			}
			else
			{
				html = getAllyHtml(cd.npc);
			}
		}
		// Register a clan member for the fight
		else if (event.equals("register_member"))
		{
			if (clan == null)
			{
				html = "messenger_clannotregistered.htm";
			}
			else if (!_hall.isRegistering())
			{
				html = "messenger_registrationpassed.htm";
			}
			else
			{
				final ClanData cd = _data.get(clan.getClanId());
				if (cd == null)
				{
					html = "messenger_notclannotleader.htm";
				}
				else if (cd.players.size() >= 18)
				{
					html = "messenger_clanqueuefull.htm";
				}
				else
				{
					cd.players.add(player.getObjectId());
					
					saveMember(clan.getClanId(), player.getObjectId());
					
					if (cd.npc == 0)
					{
						html = "messenger_leaderdidnotchooseyet.htm";
					}
					else
					{
						html = "messenger_clanregistered.htm";
					}
				}
			}
		}
		// Show cur attacker list
		else if (event.equals("view_attacker_list"))
		{
			if (_hall.isRegistering())
			{
				html = getHtmlText(player, "siege_date.htm");
				html = html.replace("%nextSiege%", _hall.getSiegeDate().getTime().toString());
			}
			else
			{
				html = getHtmlText(player, "messenger_registeredclans.htm");
				
				int i = 0;
				for (Entry<Integer, ClanData> entry : _data.entrySet())
				{
					final Clan attacker = ClanTable.getInstance().getClan(entry.getKey());
					if (attacker == null)
						continue;
					
					html = html.replaceAll("%clan" + i + "%", clan.getName());
					html = html.replaceAll("%clanMem" + i + "%", String.valueOf(entry.getValue().players.size()));
					i++;
				}
				
				if (_data.size() < 5)
				{
					for (int c = _data.size(); c < 5; c++)
					{
						html = html.replaceAll("%clan" + c + "%", "Empty pos. ");
						html = html.replaceAll("%clanMem" + c + "%", "Empty pos. ");
					}
				}
			}
		}
		
		return html;
	}
	
	@Override
	public void onMyDying(Npc npc, Creature killer)
	{
		if (_hall.isInSiege())
		{
			if (_data.size() > (_hall.getOwnerId() == 0 ? 1 : 0))
				for (Entry<Integer, ClanData> entry : _data.entrySet())
				{
					if (entry.getKey() == npc.getClanId())
						removeParticipant(entry.getKey(), true);
				}
			
			if (_clanlessNpcSpawns.contains(npc))
				_clanlessNpcSpawns.remove(npc);
				
			// TODO: Zoey76: previous bad implementation.
			// Converting map.keySet() to List and map.values() to List doesn't ensure that
			// first element in the key's List correspond to the first element in the values' List
			// That's the reason that values aren't copied to a List, instead using _data.get(clanIds.get(0))
			final List<Integer> clanIds = new ArrayList<>(_data.keySet());
			if (_firstPhase)
			{
				// "Clanless"/"Unassosiated to any clan" npcs are not killed yet
				if (_clanlessNpcSpawns.size() > 0)
					return;
					
				// Siege ends if just 1 flag is alive
				// Hall was free before battle or owner didn't set the ally npc
				if (((clanIds.size() == 1) && (_hall.getOwnerId() <= 0)) || (_hall.getOwnerId() > 0 && _data.get(_hall.getOwnerId()).npc == 0))
				{
					doUnSpawns();
					
					_missionAccomplished = true;
					_winner = ClanTable.getInstance().getClan(clanIds.get(0));
					
					removeParticipant(clanIds.get(0), false);
					cancelSiegeTask();
					endSiege();
				}
				else if ((_data.size() == 2) && (_hall.getOwnerId() > 0)) // Hall has defender (owner)
				{
					// Second stage lasts 40 minutes
					cancelSiegeTask();
					_siegeTask = ThreadPool.schedule(this::endSiege, 40 * 60 * 1000);
					
					_firstPhase = false;
					_hall.getSiegeZone().setActive(false);
					
					for (int doorId : INNER_DOORS)
						_hall.openDoor(null, doorId);
					
					doUnSpawns();
					
					for (Entry<Integer, ClanData> entry : _data.entrySet())
						doSpawns(entry.getKey(), entry.getValue(), true, false);
					
					ThreadPool.schedule(() ->
					{
						for (int doorId : INNER_DOORS)
							_hall.closeDoor(null, doorId);
						
						for (Entry<Integer, ClanData> entry : _data.entrySet())
							doSpawns(entry.getKey(), entry.getValue(), false, true);
						
						// Send maker second stage start event
						sendMakerEvent("onFlagWarFinalEvent");
						
						_hall.getSiegeZone().setActive(true);
					}, 300000);
				}
			}
			else
			{
				doUnSpawns();
				
				_missionAccomplished = true;
				_winner = ClanTable.getInstance().getClan(clanIds.get(0));
				
				removeParticipant(clanIds.get(0), false);
				endSiege();
			}
		}
	}
	
	@Override
	public void onCreated(Npc npc)
	{
		ThreadPool.schedule(() ->
		{
			npc.getAI().addMoveToDesire(CENTER, 5);
		}, (Rnd.get(6) + 5) * 1000L);
		
		super.onCreated(npc);
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("clear-tp-delay"))
			npc._i_ai1 = 0;
		
		return null;
	}
	
	@Override
	public Clan getWinner()
	{
		return _winner;
	}
	
	@Override
	public void prepareSiege()
	{
		if (_hall.getOwnerId() > 0)
			registerClan(ClanTable.getInstance().getClan(_hall.getOwnerId()));
		
		_hall.banishForeigners();
		_hall.updateSiegeStatus(SiegeStatus.REGISTRATION_OVER);
		
		_siegeTask = ThreadPool.schedule(this::startSiege, 3600000);
		
		World.toAllOnlinePlayers(SystemMessage.getSystemMessage(SystemMessageId.REGISTRATION_TERM_FOR_S1_ENDED).addFortId(_hall.getId()));
	}
	
	@Override
	public void startSiege()
	{
		removeInvalidRegistrations();
		
		if (_attackers.size() < 2)
		{
			for (int clanId : _data.keySet())
				removeParticipant(clanId, _hall.getOwnerId() != clanId);
			
			clearTables();
			
			_attackers.clear();
			
			_hall.updateNextSiege();
			
			World.toAllOnlinePlayers(SystemMessage.getSystemMessage(SystemMessageId.SIEGE_OF_S1_HAS_BEEN_CANCELED_DUE_TO_LACK_OF_INTEREST).addFortId(_hall.getId()));
			return;
		}
		
		// Open doors for challengers
		for (int door : OUTTER_DOORS)
			_hall.openDoor(null, door);
		
		// Schedule open doors closement, banish non siege participants and siege start in 5 minutes
		ThreadPool.schedule(() ->
		{
			for (int door : OUTTER_DOORS)
				_hall.closeDoor(null, door);
			
			final List<Integer> allowedPlayers = _data.values().stream().flatMap(clanData -> clanData.players.stream()).collect(Collectors.toList());
			
			for (Player player : _hall.getZone().getKnownTypeInside(Player.class, p -> p.getClan() == null || !allowedPlayers.contains(p.getObjectId())))
				player.teleportTo(_hall.getRndSpawn(SpawnType.BANISH), 20);
			
			// Teleport owner inside
			if (_hall.getOwnerId() > 0)
			{
				final Clan owner = ClanTable.getInstance().getClan(_hall.getOwnerId());
				if (owner != null)
				{
					for (Player player : owner.getOnlineMembers())
						player.teleportTo(_hall.getRndSpawn(SpawnType.OWNER), 0);
				}
			}
			
			// Set as an active siege zone
			_hall.getSiegeZone().setActive(true);
			
			// Spawns challengers npcs
			for (Entry<Integer, ClanData> entry : _data.entrySet())
			{
				ClanData cd = entry.getValue();
				
				doSpawns(entry.getKey(), cd, false, true);
			}
			
			// Send maker siege start event
			sendMakerEvent("onSiegeStart");
			
			// Spawn "Defender NPC"/"Filler" Npcs
			doClanlessNpcSpawns(false, true);
			
			_hall.updateSiegeStatus(SiegeStatus.IN_PROGRESS);
			
			// First stage lasts 20 minutes
			cancelSiegeTask();
			_siegeTask = ThreadPool.schedule(this::endSiege, 20 * 60 * 1000);
		}, 5 * 60 * 1000);
		
		// Spawns challengers flags
		for (Entry<Integer, ClanData> entry : _data.entrySet())
		{
			ClanData cd = entry.getValue();
			
			doSpawns(entry.getKey(), cd, true, false);
			
			for (int objId : cd.players)
			{
				final Player player = World.getInstance().getPlayer(objId);
				if (player != null)
					cd.playersInstance.add(player);
			}
		}
		
		// Spawn "Defender NPC"/"Filler" flags
		doClanlessNpcSpawns(true, false);
	}
	
	@Override
	public void endSiege()
	{
		cancelSiegeTask();
		
		// Send maker siege end event
		sendMakerEvent("onSiegeEnd");
		
		doUnSpawns();
		
		if (_hall.getOwnerId() > 0)
		{
			final Clan clan = ClanTable.getInstance().getClan(_hall.getOwnerId());
			clan.setClanHallId(0);
			
			_hall.free();
		}
		
		super.endSiege();
		
		if (_data.size() > 0)
		{
			for (int clanId : _data.keySet())
				removeParticipant(clanId, _hall.getOwnerId() != clanId);
		}
		
		clearTables();
	}
	
	@Override
	public final boolean canPlantFlag()
	{
		return false;
	}
	
	@Override
	public final boolean doorIsAutoAttackable()
	{
		return false;
	}
	
	void doSpawns(int clanId, ClanData cd, boolean tents, boolean npcs)
	{
		try
		{
			int index = 0;
			if (_firstPhase)
				index = cd.flag - FLAG_RED;
			else
				index = clanId == _hall.getOwnerId() ? 5 : 6;
			
			final SpawnLocation loc = FLAG_COORDS[index];
			
			if (cd.flag != 0 && tents)
			{
				cd.flagInstance = new Spawn(cd.flag);
				cd.flagInstance.setLoc(loc);
				_tentSpawns.add(cd.flagInstance.doSpawn(false));
			}
			
			if (cd.npc != 0 && npcs)
			{
				cd.warrior = new Spawn(cd.npc, true);
				cd.warrior.setLoc(loc);
				_npcSpawns.add(cd.warrior.doSpawn(false, null, clanId));
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't generate spawns for {}.", e, getName());
		}
	}
	
	private void registerClan(Clan clan)
	{
		_attackers.add(clan);
		
		final ClanData cd = new ClanData();
		cd.flag = ROYAL_FLAG + _data.size() + 1;
		cd.players.add(clan.getLeaderId());
		
		_data.put(clan.getClanId(), cd);
		
		saveClan(clan.getClanId(), cd.flag);
		saveMember(clan.getClanId(), clan.getLeaderId());
	}
	
	private final void removeParticipant(int clanId, boolean teleport)
	{
		final ClanData cd = _data.remove(clanId);
		if (cd == null)
			return;
		
		cd.players.clear();
		
		// Teleport players outside
		if (teleport)
		{
			for (Player player : cd.playersInstance)
				player.teleportTo(RestartType.TOWN);
		}
		
		cd.playersInstance.clear();
	}
	
	public boolean canPayRegistration()
	{
		return true;
	}
	
	@Override
	public final void loadAttackers()
	{
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_ATTACKERS))
		{
			ps.setInt(1, _hall.getId());
			
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final int clanId = rs.getInt("clan_id");
					
					if (ClanTable.getInstance().getClan(clanId) == null)
						continue;
					
					final ClanData cd = new ClanData();
					cd.flag = rs.getInt("flag");
					cd.npc = rs.getInt("npc");
					
					_data.put(clanId, cd);
					
					try (PreparedStatement ps2 = con.prepareStatement(SELECT_MEMBERS))
					{
						ps2.setInt(1, clanId);
						
						try (ResultSet rs2 = ps2.executeQuery())
						{
							while (rs2.next())
								cd.players.add(rs2.getInt("object_id"));
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't load attackers for {}.", e, getName());
		}
	}
	
	private final void saveClan(int clanId, int flag)
	{
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(INSERT_ATTACKERS))
		{
			ps.setInt(1, _hall.getId());
			ps.setInt(2, flag);
			ps.setInt(3, 0);
			ps.setInt(4, clanId);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't save clans for {}.", e, getName());
		}
	}
	
	private final void saveNpc(int npc, int clanId)
	{
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(UPDATE_ATTACKERS_NPC))
		{
			ps.setInt(1, npc);
			ps.setInt(2, clanId);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't save NPCs for {}.", e, getName());
		}
	}
	
	private final void saveMember(int clanId, int objectId)
	{
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(INSERT_MEMBERS))
		{
			ps.setInt(1, _hall.getId());
			ps.setInt(2, clanId);
			ps.setInt(3, objectId);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't save members for {}.", e, getName());
		}
	}
	
	private void clearTables()
	{
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps1 = con.prepareStatement(DELETE_ATTACKERS);
			PreparedStatement ps2 = con.prepareStatement(DELETE_MEMBERS))
		{
			ps1.setInt(1, _hall.getId());
			ps1.execute();
			
			ps2.setInt(1, _hall.getId());
			ps2.execute();
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't clear data tables for {}.", e, getName());
		}
	}
	
	private void doClanlessNpcSpawns(boolean tents, boolean npcs)
	{
		try
		{
			final int[] tmpFlags =
			{
				0,
				1,
				2,
				3,
				4
			};
			
			final List<Integer> takenFlags = _data.values().stream().map(dt -> dt.flag - FLAG_RED).collect(Collectors.toList());
			
			for (int flg : tmpFlags)
			{
				if (!takenFlags.contains(flg))
				{
					int flagNpcId = FLAG_RED + flg;
					
					final SpawnLocation loc = FLAG_COORDS[flg];
					
					if (tents)
					{
						final Spawn flagInstance = new Spawn(flagNpcId);
						flagInstance.setLoc(loc);
						final Npc flagNpc = flagInstance.doSpawn(false);
						
						_clanlessTentSpawns.add(flagNpc);
					}
					
					final int[] allyList =
					{
						ALLY_1,
						ALLY_2,
						ALLY_3,
						ALLY_4,
						ALLY_5
					};
					
					if (npcs)
					{
						final Spawn warrior = new Spawn(Rnd.get(allyList), true);
						warrior.setLoc(loc);
						final Npc warriorNpc = warrior.doSpawn(false);
						_clanlessNpcSpawns.add(warriorNpc);
						
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't generate spawns for {}.", e, getName());
		}
	}
	
	private void despawnClanlessNpcsAndTents()
	{
		for (Npc tent : _clanlessTentSpawns)
			tent.deleteMe();
		
		for (Npc npc : _clanlessNpcSpawns)
			npc.deleteMe();
		
		_clanlessTentSpawns.clear();
		_clanlessNpcSpawns.clear();
	}
	
	private void doUnSpawns()
	{
		for (Npc tent : _tentSpawns)
			tent.deleteMe();
		
		for (Npc npc : _npcSpawns)
			npc.deleteMe();
		
		_tentSpawns.clear();
		_npcSpawns.clear();
		
		despawnClanlessNpcsAndTents();
	}
	
	private void sendMakerEvent(String eventName)
	{
		NpcMaker maker = SpawnManager.getInstance().getNpcMaker(MAKER_NAME);
		maker.getMaker().onMakerScriptEvent(eventName, maker, 0, 0);
	}
	
	private void removeInvalidRegistrations()
	{
		Iterator<Map.Entry<Integer, ClanData>> iterator = _data.entrySet().iterator();
		while (iterator.hasNext())
		{
			Map.Entry<Integer, ClanData> entry = iterator.next();
			ClanData clanData = entry.getValue();
			
			if (entry.getKey() != _hall.getOwnerId() && (clanData.npc == 0 || clanData.flag == 0))
			{
				// Remove invalid registration from the map
				iterator.remove();
				_attackers.remove(ClanTable.getInstance().getClan(entry.getKey()));
			}
		}
	}
	
	private static final void teleportPlayers(Npc npc)
	{
		List<Player> players = npc.getKnownTypeInRadius(Player.class, 250);
		
		final int posIndex = Rnd.get(1) + 1;
		final int posX = getNpcIntAIParamOrDefault(npc, "Pos_x" + posIndex, 0);
		final int posY = getNpcIntAIParamOrDefault(npc, "Pos_y" + posIndex, 0);
		final int posZ = getNpcIntAIParamOrDefault(npc, "Pos_z" + posIndex, 0);
		
		for (Player pl : players)
			if (Math.abs(pl.getZ() - npc.getZ()) <= 100)
				pl.teleportTo(posX, posY, posZ, 0);
	}
	
	private static final int getNpcIntAIParamOrDefault(Npc npc, String name, int defaultValue)
	{
		return npc.getSpawn().getMemo().getInteger(name, npc.getTemplate().getAiParams().getInteger(name, defaultValue));
	}
	
	class ClanData
	{
		int flag = 0;
		int npc = 0;
		
		List<Integer> players = new ArrayList<>(18);
		List<Player> playersInstance = new ArrayList<>(18);
		
		Spawn warrior = null;
		Spawn flagInstance = null;
	}
}