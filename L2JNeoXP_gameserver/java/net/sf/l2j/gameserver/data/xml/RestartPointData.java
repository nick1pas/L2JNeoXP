package net.sf.l2j.gameserver.data.xml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.data.xml.IXmlReader;

import net.sf.l2j.gameserver.data.manager.CastleManager;
import net.sf.l2j.gameserver.data.manager.ClanHallManager;
import net.sf.l2j.gameserver.data.manager.SevenSignsManager;
import net.sf.l2j.gameserver.data.manager.ZoneManager;
import net.sf.l2j.gameserver.enums.CabalType;
import net.sf.l2j.gameserver.enums.RestartType;
import net.sf.l2j.gameserver.enums.SealType;
import net.sf.l2j.gameserver.enums.SiegeSide;
import net.sf.l2j.gameserver.enums.SpawnType;
import net.sf.l2j.gameserver.enums.actors.ClassRace;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.residence.castle.Castle;
import net.sf.l2j.gameserver.model.residence.castle.Siege;
import net.sf.l2j.gameserver.model.residence.clanhall.ClanHall;
import net.sf.l2j.gameserver.model.residence.clanhall.ClanHallSiege;
import net.sf.l2j.gameserver.model.restart.RestartArea;
import net.sf.l2j.gameserver.model.restart.RestartPoint;
import net.sf.l2j.gameserver.model.zone.form.ZoneNPoly;
import net.sf.l2j.gameserver.model.zone.type.ArenaZone;
import net.sf.l2j.gameserver.scripting.script.siegablehall.FlagWar;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

/**
 * This class loads and handles following types of zones used when under the process of revive, or scrolling out :
 * <ul>
 * <li>{@link RestartArea}s retain delimited areas bound to {@link Player}'s {@link ClassRace}. It priors and overrides initial behavior.</li>
 * <li>{@link RestartPoint}s define region-scale restart points.</li>
 * </ul>
 */
public final class RestartPointData implements IXmlReader
{
	private final List<RestartArea> _restartAreas = new ArrayList<>();
	private final List<RestartPoint> _restartPoints = new ArrayList<>();
	
	protected RestartPointData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseFile("./data/xml/restartPointAreas.xml");
		LOGGER.info("Loaded {} restart areas and {} restart points.", _restartAreas.size(), _restartPoints.size());
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode ->
		{
			forEach(listNode, "area", areaNode ->
			{
				final NamedNodeMap attrs = areaNode.getAttributes();
				
				final int minZ = parseInteger(attrs, "minZ");
				final int maxZ = parseInteger(attrs, "maxZ");
				
				final List<IntIntHolder> nodes = new ArrayList<>();
				forEach(areaNode, "node", nodeNode ->
				{
					final NamedNodeMap nodeAttrs = nodeNode.getAttributes();
					nodes.add(new IntIntHolder(parseInteger(nodeAttrs, "x"), parseInteger(nodeAttrs, "y")));
				});
				
				final IntIntHolder[] coords = nodes.toArray(new IntIntHolder[nodes.size()]);
				final int[] aX = new int[coords.length];
				final int[] aY = new int[coords.length];
				
				for (int i = 0; i < coords.length; i++)
				{
					aX[i] = coords[i].getId();
					aY[i] = coords[i].getValue();
				}
				
				final EnumMap<ClassRace, String> classRestrictions = new EnumMap<>(ClassRace.class);
				forEach(areaNode, "restart", restartNode ->
				{
					final NamedNodeMap nodeAttrs = restartNode.getAttributes();
					classRestrictions.put(parseEnum(nodeAttrs, ClassRace.class, "race"), parseString(nodeAttrs, "zone"));
				});
				
				_restartAreas.add(new RestartArea(new ZoneNPoly(aX, aY, minZ, maxZ), classRestrictions));
			});
			
			forEach(listNode, "point", pointNode ->
			{
				final StatSet set = new StatSet();
				final List<Location> points = new ArrayList<>();
				final List<Location> chaoPoints = new ArrayList<>();
				final List<IntIntHolder> mapRegions = new ArrayList<>();
				
				forEach(pointNode, "set", setNode ->
				{
					final NamedNodeMap setAttrs = setNode.getAttributes();
					final String name = parseString(setAttrs, "name");
					
					switch (name)
					{
						case "point":
							points.add(parseLocation(setAttrs, "val"));
							break;
						
						case "chaoPoint":
							chaoPoints.add(parseLocation(setAttrs, "val"));
							break;
						
						case "map":
							mapRegions.add(parseIntIntHolder(setAttrs, "val"));
							break;
						
						default:
							set.set(name, parseString(setAttrs, "val"));
							break;
					}
				});
				
				set.set("points", (points.isEmpty()) ? Collections.emptyList() : points);
				set.set("chaoPoints", (chaoPoints.isEmpty()) ? Collections.emptyList() : chaoPoints);
				set.set("mapRegions", (mapRegions.isEmpty()) ? Collections.emptyList() : mapRegions);
				
				_restartPoints.add(new RestartPoint(set));
			});
		});
	}
	
	public void reload()
	{
		_restartAreas.clear();
		_restartPoints.clear();
		
		load();
	}
	
	public List<RestartArea> getRestartAreas()
	{
		return _restartAreas;
	}
	
	public List<RestartPoint> getRestartPoints()
	{
		return _restartPoints;
	}
	
	/**
	 * @param player : The {@link Player} to test.
	 * @return The {@link RestartArea} associated to the X/Y/Z position of the {@link Player} set as parameter, or null if none is found.
	 */
	public RestartArea getRestartArea(Player player)
	{
		return _restartAreas.stream().filter(ra -> ra.getZone().isInsideZone(player.getX(), player.getY(), player.getZ())).findFirst().orElse(null);
	}
	
	/**
	 * @param creature : The {@link Creature} to test.
	 * @return The {@link RestartPoint} associated to the geomap position of the {@link Player} set as parameter, or null if none is found.
	 */
	public RestartPoint getRestartPoint(Creature creature)
	{
		return getRestartPoint(creature.getPosition());
	}
	
	/**
	 * @param loc : The {@link Location} to test.
	 * @return The {@link RestartPoint} associated to the geomap position of the {@link Location} set as parameter, or null if none is found.
	 */
	public RestartPoint getRestartPoint(Location loc)
	{
		final int rx = (loc.getX() - World.WORLD_X_MIN) / World.TILE_SIZE + World.TILE_X_MIN;
		final int ry = (loc.getY() - World.WORLD_Y_MIN) / World.TILE_SIZE + World.TILE_Y_MIN;
		
		for (RestartPoint ra : _restartPoints)
		{
			for (IntIntHolder iih : ra.getMapRegions())
				if (iih.equals(rx, ry))
					return ra;
		}
		return null;
	}
	
	/**
	 * @param name : The {@link String} to test.
	 * @return The {@link RestartPoint} associated to a name.
	 */
	public RestartPoint getRestartPointByName(String name)
	{
		return _restartPoints.stream().filter(rp -> rp.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
	}
	
	/**
	 * Search over both {@link RestartArea}s and {@link RestartPoint}s to define best teleport {@link RestartPoint}, based on {@link Player}'s {@link ClassRace} and karma.
	 * @param player : The {@link Player} to test.
	 * @return The nearest {@link RestartPoint} took over either {@link RestartArea}s or {@link RestartPoint}s.
	 */
	public RestartPoint getCalculatedRestartPoint(Player player)
	{
		// Retrieve a RestartArea, if any.
		final RestartArea area = getRestartArea(player);
		if (area != null)
			return getRestartPointByName(area.getClassRestriction(player));
		
		RestartPoint restartPoint = getRestartPoint(player);
		if (restartPoint.getBannedRace() == player.getRace())
			restartPoint = getRestartPointByName(restartPoint.getBannedPoint());
		
		return restartPoint;
	}
	
	/**
	 * Search over both {@link RestartArea}s and {@link RestartPoint}s to define best teleport {@link Location}, based on {@link Player}'s {@link ClassRace} and karma.
	 * @param player : The {@link Player} to test.
	 * @return A {@link Location} took randomly over either {@link RestartArea}s or {@link RestartPoint}s.
	 */
	public Location getNearestRestartLocation(Player player)
	{
		// Retrieve a RestartArea, if any.
		final RestartArea area = getRestartArea(player);
		if (area != null)
		{
			final RestartPoint restartPoint = getRestartPointByName(area.getClassRestriction(player));
			return (player.getKarma() > 0) ? restartPoint.getRandomChaoPoint() : restartPoint.getRandomPoint();
		}
		
		RestartPoint restartPoint = getRestartPoint(player);
		if (restartPoint.getBannedRace() == player.getRace())
			restartPoint = getRestartPointByName(restartPoint.getBannedPoint());
		
		return (player.getKarma() > 0) ? restartPoint.getRandomChaoPoint() : restartPoint.getRandomPoint();
	}
	
	/**
	 * @param player : The {@link Player} to check.
	 * @param teleportType : The {@link RestartType} to check.
	 * @return a {@link Location} based on {@link Player} own position.
	 */
	public Location getLocationToTeleport(Player player, RestartType teleportType)
	{
		if (teleportType != RestartType.TOWN && player.getClan() != null)
		{
			if (teleportType == RestartType.CLAN_HALL)
			{
				final ClanHall ch = ClanHallManager.getInstance().getClanHallByOwner(player.getClan());
				if (ch != null)
					return ch.getRndSpawn(SpawnType.OWNER);
			}
			else if (teleportType == RestartType.CASTLE)
			{
				// Check if the player is part of a castle owning clan.
				final Castle castle = CastleManager.getInstance().getCastleByOwner(player.getClan());
				if (castle != null)
					return castle.getRndSpawn(SpawnType.OWNER);
				
				// If not, check if he is in defending side.
				final Siege siege = CastleManager.getInstance().getActiveSiege(player);
				if (siege != null && siege.checkSides(player.getClan(), SiegeSide.DEFENDER, SiegeSide.OWNER))
					return siege.getCastle().getRndSpawn(SpawnType.OWNER);
			}
			else if (teleportType == RestartType.SIEGE_FLAG)
			{
				final Siege siege = CastleManager.getInstance().getActiveSiege(player);
				if (siege != null)
				{
					final Npc flag = siege.getFlag(player.getClan());
					if (flag != null)
						return flag.getPosition();
				}
				
				final ClanHallSiege chs = ClanHallManager.getInstance().getActiveSiege(player);
				if (chs != null)
				{
					final Npc flag = chs.getFlag(player.getClan());
					if (flag != null)
						return flag.getPosition();
				}
			}
		}
		
		// Check if player is in arena.
		final ArenaZone arena = ZoneManager.getInstance().getZone(player, ArenaZone.class);
		if (arena != null)
			return arena.getRndSpawn((player.getKarma() > 0) ? SpawnType.CHAOTIC : SpawnType.NORMAL);
			
		// Returning to Town in a Siege - Seal of Strife.
		// When owned by Dawn: Player restarts in the second nearest village.
		// When owned by Dusk / not owned: A clan that has participated in a siege restarts in the first town at the time of escape or death.
		final Castle castle = CastleManager.getInstance().getCastle(player);
		if (castle != null && castle.getSiege().isInProgress() && SevenSignsManager.getInstance().isSealValidationPeriod() && SevenSignsManager.getInstance().getSealOwner(SealType.STRIFE) == CabalType.DAWN)
			return castle.getRndSpawn((player.getKarma() > 0) ? SpawnType.CHAOTIC : SpawnType.OTHER);
		
		final ClanHallSiege chs = ClanHallManager.getInstance().getActiveSiege(player);
		if (chs != null && chs instanceof FlagWar)
		{
			final FlagWar fw = (FlagWar) chs;
			final Location fwRestartPoint = fw.getClanRestartPoint(player.getClan());
			if (fwRestartPoint != null)
				return fwRestartPoint;
		}
		
		// Karma player lands out of city, otherwise retrieve a random spawn location of the nearest town.
		return getNearestRestartLocation(player);
	}
	
	/**
	 * @param player : The {@link Player} to check.
	 * @return the {@link String} used for Auctioneer HTM.
	 */
	public final String getAgitMap(Player player)
	{
		switch (getRestartPoint(player).getLocName())
		{
			case 912:
				return "gludio";
			
			case 911:
				return "gludin";
			
			case 916:
				return "dion";
			
			case 918:
				return "giran";
			
			case 1537:
				return "rune";
			
			case 1538:
				return "godard";
			
			case 1714:
				return "schuttgart";
			
			default:
				return "aden";
		}
	}
	
	public static RestartPointData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final RestartPointData INSTANCE = new RestartPointData();
	}
}