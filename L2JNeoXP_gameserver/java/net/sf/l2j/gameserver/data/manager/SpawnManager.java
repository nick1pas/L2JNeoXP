package net.sf.l2j.gameserver.data.manager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.data.xml.IXmlReader;
import net.sf.l2j.commons.geometry.Triangle;
import net.sf.l2j.commons.geometry.algorithm.Kong;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.pool.ConnectionPool;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.enums.CabalType;
import net.sf.l2j.gameserver.enums.SealType;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.location.Point2D;
import net.sf.l2j.gameserver.model.memo.SpawnMemo;
import net.sf.l2j.gameserver.model.records.PrivateData;
import net.sf.l2j.gameserver.model.spawn.ASpawn;
import net.sf.l2j.gameserver.model.spawn.MultiSpawn;
import net.sf.l2j.gameserver.model.spawn.NpcMaker;
import net.sf.l2j.gameserver.model.spawn.Spawn;
import net.sf.l2j.gameserver.model.spawn.SpawnData;
import net.sf.l2j.gameserver.model.spawn.Territory;
import net.sf.l2j.gameserver.scripting.Quest;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

/**
 * Loads spawn list based on {@link Territory}s and {@link NpcMaker}s.<br>
 * Handles spawn/respawn/despawn of various {@link Npc} in the game using events.<br>
 * Locally stores individual {@link Spawn}s (e.g. quests, temporary spawned {@link Npc}s).<br>
 * Loads/stores {@link Npc}s' {@link SpawnData} to/from database.
 */
public class SpawnManager implements IXmlReader
{
	private static final String OTHER_XML_FOLDER = "./data/xml/spawnlist/custom";
	
	private static final String LOAD_SPAWN_DATAS = "SELECT * FROM spawn_data ORDER BY name";
	private static final String TRUNCATE_SPAWN_DATAS = "TRUNCATE spawn_data";
	private static final String SAVE_SPAWN_DATAS = "INSERT INTO spawn_data (name, status, current_hp, current_mp, loc_x, loc_y, loc_z, heading, db_value, respawn_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	private static final String UPDATE_SPAWN_DATAS = "UPDATE spawn_data SET name = ?, status = ?, current_hp = ?, current_mp = ?, loc_x = ?, loc_y = ?, loc_z = ?, heading = ?, db_value = ?, respawn_time =? WHERE name=?";
	
	private final Map<String, SpawnData> _spawnData = new ConcurrentHashMap<>();
	
	private final Set<Territory> _territories = ConcurrentHashMap.newKeySet();
	private final Set<NpcMaker> _makers = ConcurrentHashMap.newKeySet();
	private final Set<Spawn> _spawns = ConcurrentHashMap.newKeySet();
	
	private int _dynamicGroupId = 0;
	
	public SpawnManager()
	{
		load();
	}
	
	@Override
	public void load()
	{
		loadSpawnData();
		LOGGER.info("Loaded {} spawn data.", _spawnData.size());
		
		parseFile("./data/xml/spawnlist/");
		LOGGER.info("Loaded {} territories.", _territories.size());
		LOGGER.info("Loaded {} NPC makers.", _makers.size());
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		final List<Point2D> coords = new ArrayList<>();
		forEach(doc, "list", listNode ->
		{
			forEach(listNode, "territory", territoryNode ->
			{
				final NamedNodeMap terr = territoryNode.getAttributes();
				
				// Get Territory name and Z limits.
				final String name = parseString(terr, "name");
				int minZ = parseInteger(terr, "minZ");
				int maxZ = parseInteger(terr, "maxZ");
				
				// Get Territory coordinates.
				forEach(territoryNode, "node", locationNode ->
				{
					// load X, Y, min Z, max Z and add them to coordinate and limits to lists
					final NamedNodeMap loc = locationNode.getAttributes();
					coords.add(new Point2D(parseInteger(loc, "x"), parseInteger(loc, "y")));
				});
				
				// Create Territory and store it in the List.
				try
				{
					_territories.add(new Territory(name, Kong.doTriangulation(coords), minZ, maxZ));
				}
				catch (Exception e)
				{
					LOGGER.warn("Cannot load territory \"{}\", {}", name, e.getMessage());
				}
				
				// Clear coordinates.
				coords.clear();
			});
			
			// Parse and feed NpcMakers.
			forEach(listNode, "npcmaker", npcmakerNode ->
			{
				final StatSet set = parseAttributes(npcmakerNode);
				
				// Retrieve the Territory.
				Territory territory = findTerritory(set.getString("territory"));
				if (territory != null)
					set.put("t", territory);
				
				// Retrieve the banned Territory, if any.
				final String banName = set.getString("ban", null);
				if (banName != null)
				{
					territory = findTerritory(banName);
					if (territory != null)
						set.put("bt", territory);
				}
				
				// Build the Maker AI parameters.
				final Map<String, String> makerAIParams = new HashMap<>();
				
				forEach(npcmakerNode, "ai", aiNode ->
				{
					// Set this Maker type.
					set.put("maker", parseString(aiNode.getAttributes(), "type"));
					
					forEach(aiNode, "set", paramNode ->
					{
						final NamedNodeMap paramAttrs = paramNode.getAttributes();
						makerAIParams.put(parseString(paramAttrs, "name"), parseString(paramAttrs, "val").replace("@", ""));
					});
				});
				
				set.put("aiParams", makerAIParams);
				
				final NpcMaker maker = new NpcMaker(set);
				
				// Feed MultiSpawn List.
				final List<MultiSpawn> spawns = new ArrayList<>();
				forEach(npcmakerNode, "npc", npcNode ->
				{
					final NamedNodeMap npc = npcNode.getAttributes();
					
					// Get related NpcTemplate.
					final int npcId = parseInteger(npc, "id");
					final NpcTemplate template = NpcData.getInstance().getTemplate(npcId);
					if (template == null)
					{
						LOGGER.warn("NpcTemplate was not found for NPC id {} in NpcMaker name {}.", npcId, maker.getName());
						return;
					}
					
					// Get the total amount of npcs.
					final int total = parseInteger(npc, "total");
					
					// Get the respawn data.
					final int respawnDelay = StringUtil.getTimeStamp(parseString(npc, "respawn", null));
					final int respawnRandom = StringUtil.getTimeStamp(parseString(npc, "respawnRand", null));
					
					// Build NpcMaker privates.
					final List<PrivateData> privateData = new ArrayList<>();
					forEach(npcNode, "privates", privatesNode -> forEach(privatesNode, "private", privateNode -> privateData.add(new PrivateData(parseAttributes(privateNode)))));
					
					// Build SpawnMemo.
					final SpawnMemo spawnMemo = new SpawnMemo();
					forEach(npcNode, "ai", aiNode -> forEach(aiNode, "set", paramNode ->
					{
						final NamedNodeMap paramAttrs = paramNode.getAttributes();
						spawnMemo.put(parseString(paramAttrs, "name"), parseString(paramAttrs, "val"));
					}));
					
					// Get the position coordinates.
					int[][] coords2 = null;
					final String pos = parseString(npc, "pos", null);
					if (pos != null)
					{
						String[] loc = pos.split(";");
						if (loc.length < 5)
						{
							// Fixed position (X, Y, Z, heading).
							coords2 = new int[1][4];
							coords2[0][0] = Integer.parseInt(loc[0]);
							coords2[0][1] = Integer.parseInt(loc[1]);
							coords2[0][2] = Integer.parseInt(loc[2]);
							coords2[0][3] = Integer.parseInt(loc[3]);
						}
						else
						{
							// Random position with chance (N x [X, Y, Z, heading, chance]).
							coords2 = new int[loc.length / 5][5];
							for (int i = 0; i < loc.length / 5; i++)
							{
								coords2[i][0] = Integer.parseInt(loc[i * 5]);
								coords2[i][1] = Integer.parseInt(loc[i * 5 + 1]);
								coords2[i][2] = Integer.parseInt(loc[i * 5 + 2]);
								coords2[i][3] = Integer.parseInt(loc[i * 5 + 3]);
								coords2[i][4] = Integer.parseInt(loc[i * 5 + 4].split("%")[0]);
							}
						}
					}
					
					// Get the SpawnData name.
					final String dbName = parseString(npc, "dbName", null);
					
					// Get the SpawnData or create a new one, if it doesn't exist.
					SpawnData spawnData = null;
					if (dbName != null)
						spawnData = _spawnData.computeIfAbsent(dbName, sd -> new SpawnData(dbName));
					
					// Create a new MultiSpawn and add it to the List.
					try
					{
						spawns.add(new MultiSpawn(maker, template, total, respawnDelay, respawnRandom, privateData, spawnMemo, coords2, spawnData));
					}
					catch (Exception e)
					{
						LOGGER.error("Can't create MultiSpawn for maker {}, npc id {}", e, maker.getName(), npcId);
					}
				});
				
				// Set spawns on the NpcMaker.
				maker.setSpawns(spawns);
				
				// Create a new NpcMaker and add it to the List.
				_makers.add(maker);
			});
		});
	}
	
	public SpawnData getSpawnData(String name)
	{
		return _spawnData.get(name);
	}
	
	/**
	 * Reload {@link Territory}s and {@link NpcMaker}s and spawn NPCs.
	 */
	public void reload()
	{
		// Save dynamic data.
		save();
		
		// Clear entries.
		_spawnData.clear();
		_territories.clear();
		_makers.clear();
		_spawns.clear();
		
		// Load and spawn.
		load();
		spawn();
	}
	
	/**
	 * Save NPC data.
	 */
	public void save()
	{
		// Update NPCs' spawn data.
		_makers.stream().map(NpcMaker::getSpawns).flatMap(List::stream).forEach(MultiSpawn::updateSpawnData);
		_spawns.forEach(Spawn::updateSpawnData);
		
		// Save spawn data.
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement delete = con.prepareStatement(TRUNCATE_SPAWN_DATAS);
			PreparedStatement ps = con.prepareStatement(SAVE_SPAWN_DATAS))
		{
			// Delete all previous entries.
			delete.execute();
			
			// Save SpawnDatas.
			for (SpawnData data : _spawnData.values())
			{
				// Skip spawn data, which NPC did not spawn at all.
				byte status = data.getStatus();
				if (status < 0)
					continue;
				
				try
				{
					data.save(ps);
					
					ps.addBatch();
				}
				catch (Exception e)
				{
					LOGGER.warn("Couldn't save spawn data for name \"{}\".", e, data.getName());
				}
			}
			
			ps.executeBatch();
		}
		catch (Exception e)
		{
			LOGGER.warn("Couldn't save spawn data.", e);
		}
	}
	
	public void save(SpawnData data)
	{
		// Save spawn data.
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(UPDATE_SPAWN_DATAS))
		{
			// Skip spawn data, which NPC did not spawn at all.
			byte status = data.getStatus();
			if (status < 0)
				return;
			
			try
			{
				data.save(ps);
				ps.setString(11, data.getName());
				ps.addBatch();
			}
			catch (Exception e)
			{
				LOGGER.warn("Couldn't save spawn data for name \"{}\".", e, data.getName());
			}
			
			ps.executeBatch();
		}
		catch (Exception e)
		{
			LOGGER.warn("Couldn't save spawn data.", e);
		}
	}
	
	/**
	 * Load all {@link SpawnData}s from database.
	 */
	private final void loadSpawnData()
	{
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(LOAD_SPAWN_DATAS);
			ResultSet rs = ps.executeQuery();)
		{
			while (rs.next())
			{
				final String name = rs.getString("name");
				_spawnData.put(name, new SpawnData(name, rs));
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("Couldn't load spawn data.", e);
		}
	}
	
	/**
	 * Spawn all possible {@link Npc} to the world at server start.<br>
	 * Native, day/night, events allowed on start, Seven Signs, etc.
	 */
	public void spawn()
	{
		if (Config.NO_SPAWNS)
			return;
			
		// Spawn native NPCs (where on-start condition is met):
		// 1) without "event"
		// 2) with "event" + "onStart=true"
		long total = _makers.stream().filter(NpcMaker::isOnStart).mapToInt(NpcMaker::spawnAll).sum();
		LOGGER.info("Spawned {} NPCs.", total);
		
		// Spawn event NPCs.
		for (String event : Config.SPAWN_EVENTS)
			spawnEventNpcs(event, true);
		
		// Spawn Seven Signs NPCs.
		notifySevenSignsChange();
	}
	
	/**
	 * Spawn Seven Signs NPCs depending on period and status.
	 */
	public void notifySevenSignsChange()
	{
		// Despawn all SevenSigns NPCs.
		
		// Seal of Avarice NPCs.
		despawnEventNpcs("ssq_seal1_none", false);
		despawnEventNpcs("ssq_seal1_dawn", false);
		despawnEventNpcs("ssq_seal1_twilight", false);
		
		// Seal of Gnosis NPCs.
		despawnEventNpcs("ssq_seal2_none", false);
		despawnEventNpcs("ssq_seal2_dawn", false);
		despawnEventNpcs("ssq_seal2_twilight", false);
		
		// Event NPCs.
		despawnEventNpcs("ssq_event", false);
		
		// Spawn required Seven Signs NPCs.
		switch (SevenSignsManager.getInstance().getCurrentPeriod())
		{
			case RECRUITING, COMPETITION:
				// Spawn Seven Signs event NPCs.
				long spawn = spawnEventNpcs("ssq_event", false);
				LOGGER.info("Spawned {} Seven Signs - Event NPCs.", spawn);
				break;
			
			case RESULTS, SEAL_VALIDATION:
				// Get this period Seven Signs winner.
				final CabalType cabalWon = SevenSignsManager.getInstance().getWinningCabal();
				
				// Check Seal of Avarice winner.
				switch (SevenSignsManager.getInstance().getSealOwner(SealType.AVARICE))
				{
					case NORMAL:
						spawn = spawnEventNpcs("ssq_seal1_none", false);
						LOGGER.info("Spawned {} Seven Signs - Seal of Avarice NPCs, winning cabal none.", spawn);
						break;
					
					case DUSK:
						if (cabalWon == CabalType.DUSK)
						{
							spawn = spawnEventNpcs("ssq_seal1_twilight", false);
							LOGGER.info("Spawned {} Seven Signs - Seal of Avarice NPCs, winning cabal Dusk.", spawn);
						}
						else
						{
							spawn = spawnEventNpcs("ssq_seal1_none", false);
							LOGGER.info("Spawned {} Seven Signs - Seal of Avarice NPCs, winning cabal Dawn, seal cabal Dusk.", spawn);
						}
						break;
					
					case DAWN:
						if (cabalWon == CabalType.DAWN)
						{
							spawn = spawnEventNpcs("ssq_seal1_dawn", false);
							LOGGER.info("Spawned {} Seven Signs - Seal of Avarice NPCs, winning cabal Dawn.", spawn);
						}
						else
						{
							spawn = spawnEventNpcs("ssq_seal1_none", false);
							LOGGER.info("Spawned {} Seven Signs - Seal of Avarice NPCs, winning cabal Dusk, seal cabal Dawn.", spawn);
						}
						break;
				}
				
				// Check Seal of Gnosis winner.
				switch (SevenSignsManager.getInstance().getSealOwner(SealType.GNOSIS))
				{
					case NORMAL:
						spawn = spawnEventNpcs("ssq_seal2_none", false);
						LOGGER.info("Spawned {} Seven Signs - Seal of Gnosis NPCs, winning cabal none.", spawn);
						break;
					
					case DUSK:
						if (cabalWon == CabalType.DUSK)
						{
							spawn = spawnEventNpcs("ssq_seal2_twilight", false);
							LOGGER.info("Spawned {} Seven Signs - Seal of Gnosis NPCs, winning cabal Dusk.", spawn);
						}
						else
						{
							spawn = spawnEventNpcs("ssq_seal2_none", false);
							LOGGER.info("Spawned {} Seven Signs - Seal of Gnosis NPCs, winning cabal Dawn, seal cabal Dusk.", spawn);
						}
						break;
					
					case DAWN:
						if (cabalWon == CabalType.DAWN)
						{
							spawn = spawnEventNpcs("ssq_seal2_dawn", false);
							LOGGER.info("Spawned {} Seven Signs - Seal of Gnosis NPCs, winning cabal Dawn.", spawn);
						}
						else
						{
							spawn = spawnEventNpcs("ssq_seal2_none", false);
							LOGGER.info("Spawned {} Seven Signs - Seal of Gnosis NPCs, winning cabal Dusk, seal cabal Dawn.", spawn);
						}
						break;
				}
				break;
		}
	}
	
	/**
	 * Despawn all NPCs from {@link NpcMaker} and individual spawns.
	 */
	public final void despawn()
	{
		// Despawn all NPCs from NpcMakers.
		long total = _makers.stream().mapToInt(NpcMaker::deleteAll).sum();
		LOGGER.info("Despawned {} NPCs.", total);
		
		// Despawn all NPCs from individual spawns.
		_spawns.forEach(Spawn::doDelete);
	}
	
	/**
	 * @param name : The name.
	 * @return the {@link Territory} of given ID, null when none.
	 */
	public final Territory getTerritory(String name)
	{
		return _territories.stream().filter(t -> t.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
	}
	
	/**
	 * @param names : The name(s) of the {@link Territory}(s).
	 * @return the {@link Territory} of given name(s).
	 */
	private final Territory findTerritory(String names)
	{
		final String[] list = names.split(";");
		if (list.length == 0)
			return null;
		
		// A single territory is defined.
		if (list.length == 1)
			return getTerritory(list[0]);
		
		// Collect territories informations.
		final String groupedName = "grouped_" + String.format("%03d", _dynamicGroupId++);
		final List<Triangle> shapes = new ArrayList<>();
		
		int minZ = Integer.MAX_VALUE;
		int maxZ = Integer.MIN_VALUE;
		
		for (String name : list)
		{
			final Territory territory = getTerritory(name);
			if (territory == null)
			{
				LOGGER.warn("Territory {} does not exist.", name);
				return null;
			}
			
			minZ = Math.min(minZ, territory.getMinZ());
			maxZ = Math.max(maxZ, territory.getMaxZ());
			
			shapes.addAll(territory.getShapes());
		}
		
		// Create a new Territory.
		final Territory t = new Territory(groupedName, shapes, minZ, maxZ);
		
		_territories.add(t);
		return t;
	}
	
	/**
	 * @param loc : The {@link Location} to test.
	 * @return the {@link List} of all {@link NpcMaker}s at a given {@link Location}.
	 */
	public final List<NpcMaker> getNpcMakers(Location loc)
	{
		return _makers.stream().filter(m -> m.getTerritory().isInside(loc)).toList();
	}
	
	/**
	 * @param name : The {@link String} used as name.
	 * @return the {@link NpcMaker} of given name, null when none.
	 */
	public final NpcMaker getNpcMaker(String name)
	{
		return _makers.stream().filter(nm -> nm.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
	}
	
	/**
	 * Add {@link Quest} to {@link NpcMaker} of given name, to handle all NPCs being dead event.
	 * @param name : The name.
	 * @param quest : The {@link Quest} to be added.
	 */
	public final void addQuestEventByName(String name, Quest quest)
	{
		_makers.stream().filter(nm -> nm.getName().equalsIgnoreCase(name)).forEach(nm -> nm.addQuestEvent(quest));
	}
	
	/**
	 * Add {@link Quest} to {@link NpcMaker} of given event name, to handle all NPCs being dead event.
	 * @param event : The event name.
	 * @param quest : The {@link Quest} to be added.
	 */
	public final void addQuestEventByEvent(String event, Quest quest)
	{
		_makers.stream().filter(nm -> event.equals(nm.getEvent())).forEach(nm -> nm.addQuestEvent(quest));
	}
	
	/**
	 * Spawn NPCs with given event name.
	 * @param event : Type of spawn.
	 * @param message : When true, display LOGGER message about spawn.
	 * @return the amount of spawned NPCs.
	 */
	public final long spawnEventNpcs(String event, boolean message)
	{
		if (event == null || event.length() == 0)
			return 0;
		
		long total = _makers.stream().filter(nm -> event.equals(nm.getEvent())).mapToInt(NpcMaker::spawnAll).sum();
		
		if (message)
			LOGGER.info("Spawned {} \"{}\" NPCs.", total, event);
		
		return total;
	}
	
	/**
	 * Immediately respawn all dead NPCs in {@link NpcMaker}s with given event name.<br>
	 * Currently running respawn tasks are canceled.
	 * @param event : Type of spawn.
	 * @param message : When true, display LOGGER message about spawn.
	 * @return the amount of spawned NPCs.
	 */
	public final long respawnEventNpcs(String event, boolean message)
	{
		if (event == null || event.length() == 0)
			return 0;
		
		long total = _makers.stream().filter(nm -> event.equals(nm.getEvent())).mapToInt(NpcMaker::respawnAll).sum();
		
		if (message)
			LOGGER.info("Respawned {} \"{}\" NPCs.", total, event);
		
		return total;
	}
	
	/**
	 * Despawn NPCs in {@link NpcMaker}s with given event name.
	 * @param event : Type of spawn.
	 * @param message : When true, display LOGGER message about despawn.
	 * @return the mount of despawned NPCs.
	 */
	public final long despawnEventNpcs(String event, boolean message)
	{
		if (event == null || event.length() == 0)
			return 0;
		
		long total = _makers.stream().filter(nm -> event.equals(nm.getEvent())).mapToInt(NpcMaker::deleteAll).sum();
		
		if (message)
			LOGGER.info("Despawned {} \"{}\" NPCs.", total, event);
		
		return total;
	}
	
	/**
	 * Spawn NPCs with given event name.
	 * @param time : time to spawn.
	 * @param param1 : time to spawn parameter 1.
	 * @param param2 : time to spawn parameter 2.
	 * @param param3 : time to spawn parameter 3.
	 * @param message : When true, display LOGGER message about spawn.
	 * @return the amount of spawned NPCs.
	 */
	public final long startSpawnTime(String time, String param1, String param2, String param3, boolean message)
	{
		if (time == null || time.isEmpty())
			return 0;
		
		long total = _makers.stream().filter(nm ->
		{
			if (nm.getMakerSpawnTime() == null)
				return false;
			
			if (!time.equalsIgnoreCase(nm.getMakerSpawnTime().getName()))
				return false;
			
			final String[] spawnTimeParams = nm.getMakerSpawnTimeParams();
			if (spawnTimeParams == null)
				return false;
			
			if (spawnTimeParams.length > 0)
			{
				if (param1 == null)
					return false;
				
				if (!param1.equalsIgnoreCase(spawnTimeParams[0]))
					return false;
				
				if (spawnTimeParams.length > 1)
				{
					if (param2 == null)
						return false;
					
					if (!param2.equalsIgnoreCase(spawnTimeParams[1]))
						return false;
				}
				
				if (spawnTimeParams.length > 2)
				{
					if (param3 == null)
						return false;
					
					if (!param3.equalsIgnoreCase(spawnTimeParams[2]))
						return false;
				}
			}
			
			return true;
		}).mapToInt(NpcMaker::spawnAll).sum();
		
		if (message)
			LOGGER.info("Spawned {} \"{}\" NPCs.", total, time);
		
		return total;
	}
	
	/**
	 * Despawn NPCs in {@link NpcMaker}s with given event name.
	 * @param time : time to despawn.
	 * @param param1 : time to despawn parameter 1.
	 * @param param2 : time to despawn parameter 2.
	 * @param param3 : time to despawn parameter 3.
	 * @param message : When true, display LOGGER message about despawn.
	 * @return the mount of despawned NPCs.
	 */
	public final long stopSpawnTime(String time, String param1, String param2, String param3, boolean message)
	{
		if (time == null || time.isEmpty())
			return 0;
		
		long total = _makers.stream().filter(nm ->
		{
			if (nm.getMakerSpawnTime() == null)
				return false;
			
			if (!time.equalsIgnoreCase(nm.getMakerSpawnTime().getName()))
				return false;
			
			final String[] spawnTimeParams = nm.getMakerSpawnTimeParams();
			if (spawnTimeParams == null)
				return false;
			
			if (spawnTimeParams.length > 0)
			{
				if (param1 == null)
					return false;
				
				if (!param1.equalsIgnoreCase(spawnTimeParams[0]))
					return false;
				
				if (spawnTimeParams.length > 1)
				{
					if (param2 == null)
						return false;
					
					if (!param2.equalsIgnoreCase(spawnTimeParams[1]))
						return false;
				}
				
				if (spawnTimeParams.length > 2)
				{
					if (param3 == null)
						return false;
					
					if (!param3.equalsIgnoreCase(spawnTimeParams[2]))
						return false;
				}
			}
			
			return true;
		}).mapToInt(NpcMaker::deleteAll).sum();
		
		if (message)
			LOGGER.info("Despawned {} \"{}\" NPCs.", total, time);
		
		return total;
	}
	
	/**
	 * Add an individual {@link Spawn}.
	 * @param spawn : {@link Spawn} to be added.
	 */
	public void addSpawn(Spawn spawn)
	{
		_spawns.add(spawn);
	}
	
	/**
	 * Adds a new spawn to the spawn table.
	 * @param spawn the spawn to add
	 * @param store if {@code true} it'll be saved in the spawn XML files
	 */
	public void addSpawn(Spawn spawn, boolean store)
	{
		addSpawn(spawn);
		
		if (store)
		{
			// Create output directory if it doesn't exist
			final File outputDirectory = new File(OTHER_XML_FOLDER);
			if (!outputDirectory.exists())
			{
				try
				{
					outputDirectory.mkdir();
				}
				catch (SecurityException se)
				{
					// empty
				}
			}
			
			// XML file for spawn
			final String name = spawn.getNpc().getName().replaceAll("(\\s|')+", "").toLowerCase() + "_" + spawn.getNpc().getObjectId();
			final String npcMakerName = spawn.getNpc().getName().replaceAll("(\\s|')+", "").toLowerCase() + "_" + spawn.getNpc().getObjectId() + "1";
			final String fileName = spawn.getNpc().getName().replaceAll("(\\s|')+", "").toLowerCase();
			
			final int x = ((spawn.getLocX() - World.WORLD_X_MIN) >> 15) + World.TILE_X_MIN;
			final int y = ((spawn.getLocY() - World.WORLD_Y_MIN) >> 15) + World.TILE_Y_MIN;
			final File spawnFile = new File(OTHER_XML_FOLDER + "/" + fileName + "_" + x + "_" + y + ".xml");
			
			// Write info to XML
			final String spawnId = String.valueOf(spawn.getNpcId());
			final String spawnLoc = String.valueOf(spawn.getLocX() + ";" + spawn.getLocY() + ";" + spawn.getLocZ() + ";" + spawn.getHeading());
			
			final var respawn = getSpawn(spawn.getNpc().getNpcId()).getRespawnDelay();
			final var respawnRnd = getSpawn(spawn.getNpc().getNpcId()).getRespawnRandom();
			final var respawnRndString = (respawnRnd == 0 ? "" : "\" respawnRand=\"" + respawnRnd + "sec");
			
			if (spawnFile.exists()) // update
			{
				final File tempFile = new File(OTHER_XML_FOLDER + "/" + name + "_" + x + "_" + y + ".tmp");
				try (BufferedReader reader = new BufferedReader(new FileReader(spawnFile));
					BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile)))
				{
					String currentLine;
					while ((currentLine = reader.readLine()) != null)
					{
						if (currentLine.contains("</list>"))
						{
							writer.write("	<territory name=\"" + name + "\" minZ=\"" + (spawn.getLocZ()) + "\" maxZ=\"" + (spawn.getLocZ() + 16) + "\">\n");
							writer.write("		<node x=\"" + (spawn.getLocX() + 50) + "\" y=\"" + (spawn.getLocY() + 50) + "\" />\n");
							writer.write("		<node x=\"" + (spawn.getLocX() - 50) + "\" y=\"" + (spawn.getLocY() + 50) + "\" />\n");
							writer.write("		<node x=\"" + (spawn.getLocX() - 50) + "\" y=\"" + (spawn.getLocY() - 50) + "\" />\n");
							writer.write("		<node x=\"" + (spawn.getLocX() + 50) + "\" y=\"" + (spawn.getLocY() - 50) + "\" />\n");
							writer.write("	</territory>\n");
							writer.write("	<npcmaker name=\"" + npcMakerName + "\" territory=\"" + name + "\" maximumNpcs=\"" + 1 + "\">\n");
							writer.write("		<ai type=\"default_maker\"/>\n");
							writer.write("		<npc id=\"" + spawnId + "\" pos=\"" + spawnLoc + "\" total=\"" + 1 + "\" respawn=\"" + respawn + "sec" + respawnRndString + "\" /> <!-- " + NpcData.getInstance().getTemplate(spawn.getNpcId()).getName() + " -->\n");
							writer.write("	</npcmaker>\n");
							writer.write(currentLine + "\n");
							continue;
						}
						writer.write(currentLine + "\n");
					}
					writer.close();
					reader.close();
					spawnFile.delete();
					tempFile.renameTo(spawnFile);
				}
				catch (Exception e)
				{
					LOGGER.warn("Could not store spawn in the spawn XML files: " + e);
				}
			}
			else // new file
			{
				try (BufferedWriter writer = new BufferedWriter(new FileWriter(spawnFile)))
				{
					writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
					writer.write("<list>\n");
					writer.write("	<territory name=\"" + name + "\" minZ=\"" + (spawn.getLocZ()) + "\" maxZ=\"" + (spawn.getLocZ() + 16) + "\">\n");
					writer.write("		<node x=\"" + (spawn.getLocX() + 50) + "\" y=\"" + (spawn.getLocY() + 50) + "\" />\n");
					writer.write("		<node x=\"" + (spawn.getLocX() - 50) + "\" y=\"" + (spawn.getLocY() + 50) + "\" />\n");
					writer.write("		<node x=\"" + (spawn.getLocX() - 50) + "\" y=\"" + (spawn.getLocY() - 50) + "\" />\n");
					writer.write("		<node x=\"" + (spawn.getLocX() + 50) + "\" y=\"" + (spawn.getLocY() - 50) + "\" />\n");
					writer.write("	</territory>\n");
					writer.write("	<npcmaker name=\"" + npcMakerName + "\" territory=\"" + name + "\" maximumNpcs=\"" + 1 + "\">\n");
					writer.write("		<ai type=\"default_maker\"/>\n");
					writer.write("		<npc id=\"" + spawnId + "\" pos=\"" + spawnLoc + "\" total=\"" + 1 + "\" respawn=\"" + respawn + "sec" + respawnRndString + "\" /> <!-- " + NpcData.getInstance().getTemplate(spawn.getNpcId()).getName() + " -->\n");
					writer.write("	</npcmaker>\n");
					writer.write("</list>\n");
					writer.close();
				}
				catch (Exception e)
				{
					LOGGER.warn("Spawn " + spawn + " could not be added to the spawn XML files: " + e);
				}
			}
		}
	}
	
	/**
	 * Remove an individual {@link Spawn}.
	 * @param spawn : {@link Spawn} to be removed.
	 */
	public void deleteSpawn(Spawn spawn)
	{
		_spawns.remove(spawn);
	}
	
	public void deleteSpawn(Spawn spawn, boolean store)
	{
		final String name = spawn.getNpc().getName().replaceAll("(\\s|')+", "").toLowerCase() + "_" + spawn.getNpc().getObjectId();
		final String npcMakerName = spawn.getNpc().getName().replaceAll("(\\s|')+", "").toLowerCase() + "_" + spawn.getNpc().getObjectId() + "1";
		
		final String fileName = spawn.getNpc().getName().replaceAll("(\\s|')+", "").toLowerCase();
		final int x = ((spawn.getLocX() - World.WORLD_X_MIN) >> 15) + World.TILE_X_MIN;
		final int y = ((spawn.getLocY() - World.WORLD_Y_MIN) >> 15) + World.TILE_Y_MIN;
		final File spawnFile = new File(OTHER_XML_FOLDER + "/" + fileName + "_" + x + "_" + y + ".xml");
		
		if (!spawnFile.exists())
		{
			return; // The spawn doesn't exist in the file
		}
		
		try
		{
			final File tempFile = new File(OTHER_XML_FOLDER + "/" + fileName + "_" + x + "_" + y + ".tmp");
			try (BufferedReader reader = new BufferedReader(new FileReader(spawnFile));
				BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile)))
			{
				String currentLine;
				boolean inTerritory = false;
				boolean inNpcMaker = false;
				boolean needToDelete = false;
				while ((currentLine = reader.readLine()) != null)
				{
					if (currentLine.contains("<territory name=\"" + name))
						inTerritory = true;
					
					if (currentLine.contains("<npcmaker name=\"" + npcMakerName))
						inNpcMaker = true;
					
					if (inTerritory || inNpcMaker)
					{
						if (currentLine.contains("</territory>") || currentLine.contains("</npcmaker>"))
						{
							inTerritory = false;
							inNpcMaker = false;
							if (!needToDelete)
								writer.write(currentLine + "\n"); // Write the end tag if it wasn't removed
						}
						needToDelete = true;
					}
					else
						writer.write(currentLine + "\n");
				}
				
				writer.close();
				reader.close();
				
				if (needToDelete)
				{
					spawnFile.delete();
					tempFile.renameTo(spawnFile);
				}
				else
					tempFile.delete(); // Nothing was removed
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("Could not remove spawn from the spawn XML files: " + e);
		}
	}
	
	/**
	 * @param npcId : The {@link Npc} ID.
	 * @return The first found {@link ASpawn} of given {@link Npc}.
	 */
	public final ASpawn getSpawn(int npcId)
	{
		ASpawn result = _makers.stream().flatMap(nm -> nm.getSpawns().stream()).filter(ms -> ms.getNpcId() == npcId).findFirst().orElse(null);
		if (result == null)
			result = _spawns.stream().filter(s -> s.getNpcId() == npcId).findFirst().orElse(null);
		
		return result;
	}
	
	/**
	 * @param npcAlias : The {@link Npc} ID.
	 * @return The first found {@link ASpawn} of given {@link Npc}.
	 */
	public final ASpawn getSpawn(String npcAlias)
	{
		ASpawn result = _makers.stream().flatMap(nm -> nm.getSpawns().stream()).filter(ms -> ms.getTemplate().getAlias().equalsIgnoreCase(npcAlias)).findFirst().orElse(null);
		if (result == null)
			result = _spawns.stream().filter(s -> s.getTemplate().getAlias().equalsIgnoreCase(npcAlias)).findFirst().orElse(null);
		
		return result;
	}
	
	/**
	 * @param npcId : The {@link Npc} ID.
	 * @return The first found {@link Npc} of given npcId.
	 */
	public final Npc getNpc(int npcId)
	{
		final ASpawn spawn = getSpawn(npcId);
		if (spawn == null)
			return null;
		
		return spawn.getNpc();
	}
	
	public static final SpawnManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SpawnManager INSTANCE = new SpawnManager();
	}
}