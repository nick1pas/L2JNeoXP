package net.sf.l2j.gameserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.logging.LogManager;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.mmocore.SelectorConfig;
import net.sf.l2j.commons.mmocore.SelectorThread;
import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.util.SysUtil;

import net.sf.l2j.Config;
import net.sf.l2j.TeleportInterface.TeleportLocationData;
import net.sf.l2j.gameserver.communitybbs.CommunityBoard;
import net.sf.l2j.gameserver.communitybbs.CustomCommunityBoard;
import net.sf.l2j.gameserver.data.HTMLData;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.cache.CrestCache;
import net.sf.l2j.gameserver.data.manager.AntiFeedManager;
import net.sf.l2j.gameserver.data.manager.BufferManager;
import net.sf.l2j.gameserver.data.manager.BuyListManager;
import net.sf.l2j.gameserver.data.manager.CastleManager;
import net.sf.l2j.gameserver.data.manager.CastleManorManager;
import net.sf.l2j.gameserver.data.manager.ClanHallManager;
import net.sf.l2j.gameserver.data.manager.CoupleManager;
import net.sf.l2j.gameserver.data.manager.CursedWeaponManager;
import net.sf.l2j.gameserver.data.manager.DerbyTrackManager;
import net.sf.l2j.gameserver.data.manager.EventsDropManager;
import net.sf.l2j.gameserver.data.manager.FestivalOfDarknessManager;
import net.sf.l2j.gameserver.data.manager.FishingChampionshipManager;
import net.sf.l2j.gameserver.data.manager.HeroManager;
import net.sf.l2j.gameserver.data.manager.LotteryManager;
import net.sf.l2j.gameserver.data.manager.PartyMatchRoomManager;
import net.sf.l2j.gameserver.data.manager.PcCafeManager;
import net.sf.l2j.gameserver.data.manager.PetitionManager;
import net.sf.l2j.gameserver.data.manager.RaidPointManager;
import net.sf.l2j.gameserver.data.manager.SevenSignsManager;
import net.sf.l2j.gameserver.data.manager.SpawnManager;
import net.sf.l2j.gameserver.data.manager.ZoneManager;
import net.sf.l2j.gameserver.data.sql.BookmarkTable;
import net.sf.l2j.gameserver.data.sql.ClanTable;
import net.sf.l2j.gameserver.data.sql.OfflineTradersTable;
import net.sf.l2j.gameserver.data.sql.PlayerInfoTable;
import net.sf.l2j.gameserver.data.sql.ServerMemoTable;
import net.sf.l2j.gameserver.data.xml.AdminData;
import net.sf.l2j.gameserver.data.xml.AnnouncementData;
import net.sf.l2j.gameserver.data.xml.ArmorSetData;
import net.sf.l2j.gameserver.data.xml.AugmentationData;
import net.sf.l2j.gameserver.data.xml.BoatData;
import net.sf.l2j.gameserver.data.xml.CapsuleBoxData;
import net.sf.l2j.gameserver.data.xml.DoorData;
import net.sf.l2j.gameserver.data.xml.EnchantData;
import net.sf.l2j.gameserver.data.xml.EventsData;
import net.sf.l2j.gameserver.data.xml.FishData;
import net.sf.l2j.gameserver.data.xml.HealSpsData;
import net.sf.l2j.gameserver.data.xml.HennaData;
import net.sf.l2j.gameserver.data.xml.InstantTeleportData;
import net.sf.l2j.gameserver.data.xml.ItemData;
import net.sf.l2j.gameserver.data.xml.ManorAreaData;
import net.sf.l2j.gameserver.data.xml.MultisellData;
import net.sf.l2j.gameserver.data.xml.NewbieBuffData;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.data.xml.ObserverGroupData;
import net.sf.l2j.gameserver.data.xml.PlayerData;
import net.sf.l2j.gameserver.data.xml.PlayerLevelData;
import net.sf.l2j.gameserver.data.xml.RecipeData;
import net.sf.l2j.gameserver.data.xml.RestartPointData;
import net.sf.l2j.gameserver.data.xml.ScriptData;
import net.sf.l2j.gameserver.data.xml.SkillTreeData;
import net.sf.l2j.gameserver.data.xml.SkipData;
import net.sf.l2j.gameserver.data.xml.SoulCrystalData;
import net.sf.l2j.gameserver.data.xml.SpellbookData;
import net.sf.l2j.gameserver.data.xml.StaticObjectData;
import net.sf.l2j.gameserver.data.xml.StaticSpawnData;
import net.sf.l2j.gameserver.data.xml.SummonItemData;
import net.sf.l2j.gameserver.data.xml.SysString;
import net.sf.l2j.gameserver.data.xml.TeleportData;
import net.sf.l2j.gameserver.data.xml.WalkerRouteData;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.handler.AdminCommandHandler;
import net.sf.l2j.gameserver.handler.ChatHandler;
import net.sf.l2j.gameserver.handler.ItemHandler;
import net.sf.l2j.gameserver.handler.SkillHandler;
import net.sf.l2j.gameserver.handler.TargetHandler;
import net.sf.l2j.gameserver.handler.UserCommandHandler;
import net.sf.l2j.gameserver.handler.VoicedCommandHandler;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.entity.events.capturetheflag.CTFManager;
import net.sf.l2j.gameserver.model.entity.events.deathmatch.DMManager;
import net.sf.l2j.gameserver.model.entity.events.lastman.LMManager;
import net.sf.l2j.gameserver.model.entity.events.teamvsteam.TvTManager;
import net.sf.l2j.gameserver.model.memo.GlobalMemo;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.olympiad.OlympiadGameManager;
import net.sf.l2j.gameserver.network.GameClient;
import net.sf.l2j.gameserver.network.GamePacketHandler;
import net.sf.l2j.gameserver.taskmanager.AiTaskManager;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;
import net.sf.l2j.gameserver.taskmanager.BoatTaskManager;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;
import net.sf.l2j.gameserver.taskmanager.DelayedItemsManager;
import net.sf.l2j.gameserver.taskmanager.GameTimeTaskManager;
import net.sf.l2j.gameserver.taskmanager.InventoryUpdateTaskManager;
import net.sf.l2j.gameserver.taskmanager.ItemInstanceTaskManager;
import net.sf.l2j.gameserver.taskmanager.ItemsOnGroundTaskManager;
import net.sf.l2j.gameserver.taskmanager.PvpFlagTaskManager;
import net.sf.l2j.gameserver.taskmanager.ShadowItemTaskManager;
import net.sf.l2j.gameserver.taskmanager.WaterTaskManager;
import net.sf.l2j.util.DeadLockDetector;
import net.sf.l2j.util.IPv4Filter;

public class GameServer
{
	private static final CLogger LOGGER = new CLogger(GameServer.class.getName());
	
	private final SelectorThread<GameClient> _selectorThread;
	private final long _serverStartTimeMillis;
	
	private static GameServer _gameServer;
	public long serverLoadStart = System.currentTimeMillis();
	
	public static void main(String[] args) throws Exception
	{
		_gameServer = new GameServer();
	}
	
	public GameServer() throws Exception
	{
		// Create log folder
		new File("./log").mkdir();
		new File("./log/chat").mkdir();
		new File("./log/console").mkdir();
		new File("./log/error").mkdir();
		new File("./log/gmaudit").mkdir();
		new File("./log/item").mkdir();
		new File("./data/crests").mkdirs();
		
		// Create input stream for log file -- or store file data into memory
		try (InputStream is = new FileInputStream(new File("config/logging.properties")))
		{
			LogManager.getLogManager().readConfiguration(is);
		}
		
		StringUtil.printSection("Config");
		Config.loadGameServer();
		
		StringUtil.printSection("Poolers");
		ConnectionPool.init();
		ThreadPool.init();
		
		StringUtil.printSection("IdFactory");
		IdFactory.getInstance();
		
		StringUtil.printSection("Cache");
		HTMLData.getInstance().load();
		SysString.getInstance().load();
		CrestCache.getInstance();
		
		StringUtil.printSection("World");
		World.getInstance();
		AnnouncementData.getInstance();
		ServerMemoTable.getInstance();
		GlobalMemo.getInstance();
		
		StringUtil.printSection("Skills");
		SkillTable.getInstance();
		SkillTreeData.getInstance();
		
		StringUtil.printSection("Items");
		ItemData.getInstance();
		SummonItemData.getInstance();
		HennaData.getInstance();
		BuyListManager.getInstance();
		MultisellData.getInstance();
		RecipeData.getInstance();
		ArmorSetData.getInstance();
		FishData.getInstance();
		SpellbookData.getInstance();
		SoulCrystalData.getInstance();
		AugmentationData.getInstance();
		CursedWeaponManager.getInstance();
		SkipData.getInstance();
		
		StringUtil.printSection("Admins");
		AdminData.getInstance();
		BookmarkTable.getInstance();
		PetitionManager.getInstance();
		
		StringUtil.printSection("Characters");
		PlayerData.getInstance();
		PlayerInfoTable.getInstance();
		PlayerLevelData.getInstance();
		PartyMatchRoomManager.getInstance();
		RaidPointManager.getInstance();
		HealSpsData.getInstance();
		RestartPointData.getInstance();
		
		StringUtil.printSection("Community server");
		if (Config.ENABLE_CUSTOM_BBS)
			CustomCommunityBoard.getInstance();
		
		if (Config.ENABLE_COMMUNITY_BOARD)
			CommunityBoard.getInstance();
		
		StringUtil.printSection("Clans");
		ClanTable.getInstance();
		
		StringUtil.printSection("Geodata & Pathfinding");
		GeoEngine.getInstance();
		
		StringUtil.printSection("Zones");
		ZoneManager.getInstance();
		
		StringUtil.printSection("Doors");
		DoorData.getInstance().spawn();
		
		StringUtil.printSection("Castles & Clan Halls");
		CastleManager.getInstance();
		ClanHallManager.getInstance();
		
		StringUtil.printSection("Task Managers");
		AiTaskManager.getInstance();
		AttackStanceTaskManager.getInstance();
		BoatTaskManager.getInstance();
		DecayTaskManager.getInstance();
		DelayedItemsManager.getInstance();
		GameTimeTaskManager.getInstance();
		ItemsOnGroundTaskManager.getInstance();
		PvpFlagTaskManager.getInstance();
		ShadowItemTaskManager.getInstance();
		WaterTaskManager.getInstance();
		InventoryUpdateTaskManager.getInstance();
		ItemInstanceTaskManager.getInstance();
		
		StringUtil.printSection("Seven Signs");
		SevenSignsManager.getInstance();
		FestivalOfDarknessManager.getInstance();
		
		StringUtil.printSection("Manor Manager");
		ManorAreaData.getInstance();
		CastleManorManager.getInstance();
		
		StringUtil.printSection("NPCs");
		BufferManager.getInstance();
		NpcData.getInstance();
		WalkerRouteData.getInstance();
		StaticObjectData.getInstance();
		SpawnManager.getInstance();
		NewbieBuffData.getInstance();
		InstantTeleportData.getInstance();
		TeleportData.getInstance();
		ObserverGroupData.getInstance();
		
		CastleManager.getInstance().spawnEntities();
		
		StringUtil.printSection("Olympiads & Heroes");
		OlympiadGameManager.getInstance();
		Olympiad.getInstance();
		HeroManager.getInstance();
		
		StringUtil.printSection("Quests & Scripts");
		ScriptData.getInstance();
		
		if (Config.ALLOW_BOAT)
			BoatData.getInstance().load();
		
		StringUtil.printSection("Events");
		DerbyTrackManager.getInstance();
		LotteryManager.getInstance();
		CoupleManager.getInstance();
		EventsData.getInstance();
		
		if (Config.ALLOW_FISH_CHAMPIONSHIP)
			FishingChampionshipManager.getInstance();
		
		StringUtil.printSection("L2JNeox");
		if ((Config.OFFLINE_TRADE_ENABLE || Config.OFFLINE_CRAFT_ENABLE) && Config.RESTORE_OFFLINERS)
			OfflineTradersTable.getInstance().restore();
		
		EventsDropManager.getInstance();
		
		CTFManager.getInstance();
		DMManager.getInstance();
		LMManager.getInstance();
		TvTManager.getInstance();
		
		EnchantData.getInstance();
		
		AntiFeedManager.getInstance().registerEvent(AntiFeedManager.GAME_ID);
		
		PcCafeManager.getInstance();
		
		CapsuleBoxData.getInstance();
		
		StaticSpawnData.getInstance();
		
		StringUtil.printSection("Spawns");
		SpawnManager.getInstance().spawn();
		
		StringUtil.printSection("Handlers");
		LOGGER.info("Loaded {} admin command handlers.", AdminCommandHandler.getInstance().size());
		LOGGER.info("Loaded {} chat handlers.", ChatHandler.getInstance().size());
		LOGGER.info("Loaded {} item handlers.", ItemHandler.getInstance().size());
		LOGGER.info("Loaded {} skill handlers.", SkillHandler.getInstance().size());
		LOGGER.info("Loaded {} target handlers.", TargetHandler.getInstance().size());
		LOGGER.info("Loaded {} user command handlers.", UserCommandHandler.getInstance().size());
		LOGGER.info("Loaded {} voiced command handlers.", VoicedCommandHandler.getInstance().size());
		
		StringUtil.printSection("Gk Interface");
		TeleportLocationData.getInstance();
		
		StringUtil.printSection("System");
		Runtime.getRuntime().addShutdownHook(Shutdown.getInstance());
		
		if (Config.DEADLOCK_DETECTOR)
		{
			LOGGER.info("Deadlock detector is enabled. Timer: {}s.", Config.DEADLOCK_CHECK_INTERVAL);
			
			final DeadLockDetector deadDetectThread = new DeadLockDetector();
			deadDetectThread.setDaemon(true);
			deadDetectThread.start();
		}
		else
			LOGGER.info("Deadlock detector is disabled.");
		
		LOGGER.info("Gameserver has started, used memory: {} / {} Mo.", SysUtil.getUsedMemory(), SysUtil.getMaxMemory());
		LOGGER.info("Maximum allowed players: {}.", Config.MAXIMUM_ONLINE_USERS);
		LOGGER.info("Server loaded in " + (System.currentTimeMillis() - serverLoadStart) / 1000 + " seconds");
		
		StringUtil.printSection("Login");
		LoginServerThread.getInstance().start();
		
		final SelectorConfig sc = new SelectorConfig();
		sc.MAX_READ_PER_PASS = Config.MMO_MAX_READ_PER_PASS;
		sc.MAX_SEND_PER_PASS = Config.MMO_MAX_SEND_PER_PASS;
		sc.SLEEP_TIME = Config.MMO_SELECTOR_SLEEP_TIME;
		sc.HELPER_BUFFER_COUNT = Config.MMO_HELPER_BUFFER_COUNT;
		
		final GamePacketHandler handler = new GamePacketHandler();
		_selectorThread = new SelectorThread<>(sc, handler, handler, handler, new IPv4Filter());
		
		_serverStartTimeMillis = System.currentTimeMillis();
		
		InetAddress bindAddress = null;
		if (!Config.GAMESERVER_HOSTNAME.equals("*"))
		{
			try
			{
				bindAddress = InetAddress.getByName(Config.GAMESERVER_HOSTNAME);
			}
			catch (Exception e)
			{
				LOGGER.error("The GameServer bind address is invalid, using all available IPs.", e);
			}
		}
		
		try
		{
			_selectorThread.openServerSocket(bindAddress, Config.GAMESERVER_PORT);
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to open server socket.", e);
			System.exit(1);
		}
		_selectorThread.start();
	}
	
	public static GameServer getInstance()
	{
		return _gameServer;
	}
	
	public SelectorThread<GameClient> getSelectorThread()
	{
		return _selectorThread;
	}
	
	public long getServerStartTime()
	{
		return _serverStartTimeMillis;
	}
}