package net.sf.l2j.gameserver.model.residence.castle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.manager.CastleManager;
import net.sf.l2j.gameserver.data.manager.CastleManorManager;
import net.sf.l2j.gameserver.data.manager.SpawnManager;
import net.sf.l2j.gameserver.data.manager.ZoneManager;
import net.sf.l2j.gameserver.data.sql.ClanTable;
import net.sf.l2j.gameserver.enums.SiegeSide;
import net.sf.l2j.gameserver.enums.actors.TowerType;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.instance.HolyThing;
import net.sf.l2j.gameserver.model.item.MercenaryTicket;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.itemcontainer.PcInventory;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.model.location.TowerSpawnLocation;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.model.pledge.ClanMember;
import net.sf.l2j.gameserver.model.pledge.ItemInfo;
import net.sf.l2j.gameserver.model.records.custom.SiegeInfo;
import net.sf.l2j.gameserver.model.residence.Residence;
import net.sf.l2j.gameserver.model.spawn.Spawn;
import net.sf.l2j.gameserver.model.zone.type.CastleTeleportZone;
import net.sf.l2j.gameserver.model.zone.type.CastleZone;
import net.sf.l2j.gameserver.model.zone.type.SiegeZone;
import net.sf.l2j.gameserver.network.NpcStringId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class Castle extends Residence
{
	protected static final CLogger LOGGER = new CLogger(Castle.class.getName());
	
	private static final String UPDATE_TAXES = "UPDATE castle SET treasury=?, taxRevenue=?, seedIncome=?, currentTaxPercent=?, nextTaxPercent=? WHERE id=?";
	private static final String UPDATE_TREASURY = "UPDATE castle SET treasury=? WHERE id=?";
	private static final String UPDATE_TAX_REVENUE = "UPDATE castle SET taxRevenue=? WHERE id=?";
	private static final String UPDATE_SEED_INCOME = "UPDATE castle SET seedIncome=? WHERE id=?";
	private static final String UPDATE_CERTIFICATES = "UPDATE castle SET certificates=? WHERE id=?";
	private static final String UPDATE_CURRENT_TAX = "UPDATE castle SET currentTaxPercent=? WHERE id=?";
	private static final String UPDATE_NEXT_TAX = "UPDATE castle SET nextTaxPercent=? WHERE id=?";
	
	private static final String UPDATE_DOORS = "REPLACE INTO castle_doorupgrade (doorId,hp,castleId) VALUES (?,?,?)";
	private static final String DELETE_DOOR = "DELETE FROM castle_doorupgrade WHERE castleId=?";
	
	private static final String DELETE_OLD_OWNER = "UPDATE clan_data SET hasCastle=0 WHERE hasCastle=?";
	private static final String UPDATE_NEW_OWNER = "UPDATE clan_data SET hasCastle=? WHERE clan_id=?";
	
	private static final String UPDATE_TRAP = "REPLACE INTO castle_trapupgrade (castleId, towerIndex, level) VALUES (?,?,?)";
	private static final String DELETE_TRAP = "DELETE FROM castle_trapupgrade WHERE castleId=?";
	
	private static final String LOAD_FUNCTIONS = "SELECT * FROM castle_functions WHERE castle_id = ?";
	private static final String UPDATE_FUNCTIONS = "REPLACE INTO castle_functions (castle_id, type, lvl, lease, rate, endTime) VALUES (?,?,?,?,?,?)";
	private static final String DELETE_FUNCTIONS = "DELETE FROM castle_functions WHERE castle_id=? AND type=?";
	
	private static final String UPDATE_ITEMS_LOC = "UPDATE items SET loc='INVENTORY' WHERE item_id IN (?,6841) AND owner_id=? AND loc='PAPERDOLL'";
	
	private int _circletId;
	
	private final Map<Integer, CastleFunction> _function = new HashMap<>();
	
	private final List<MercenaryTicket> _tickets = new ArrayList<>(60);
	
	private final Set<ItemInstance> _droppedTickets = new ConcurrentSkipListSet<>();
	private final List<Npc> _siegeGuards = new ArrayList<>();
	
	private final List<TowerSpawnLocation> _controlTowers = new ArrayList<>();
	
	private final Map<Integer, SpawnLocation> _artifacts = new HashMap<>(1);
	
	private Siege _siege;
	private Calendar _siegeDate;
	private boolean _isTimeRegistrationOver = true;
	
	private int _currentTaxPercent;
	private int _nextTaxPercent;
	private double _taxRate;
	
	private long _treasury;
	private long _taxRevenue;
	private long _seedIncome;
	
	private SiegeZone _siegeZone;
	private CastleZone _castleZone;
	private CastleTeleportZone _teleZone;
	
	private int _leftCertificates;
	
	private final Map<Integer, SiegeInfo> _sieges = new HashMap<>();
	
	private final Map<Integer, Integer> _skillMember = new HashMap<>();
	private final Map<Integer, Integer> _skillLeader = new HashMap<>();
	private final Map<Integer, ItemInfo> _itemsMember = new HashMap<>();
	private final Map<Integer, ItemInfo> _itemsLeader = new HashMap<>();
	
	public static final int FUNC_TELEPORT = 1;
	public static final int FUNC_RESTORE_HP = 2;
	public static final int FUNC_RESTORE_MP = 3;
	public static final int FUNC_RESTORE_EXP = 4;
	public static final int FUNC_SUPPORT = 5;
	
	public Castle(StatSet set)
	{
		super(set);
		
		_circletId = set.getInteger("circletId");
		_townName = NpcStringId.getNpcMessage(1001000 + _id);
		
		// Feed _siegeZone.
		for (SiegeZone zone : ZoneManager.getInstance().getAllZones(SiegeZone.class))
		{
			if (zone.getSiegableId() == _id)
			{
				_siegeZone = zone;
				break;
			}
		}
		
		// Feed _castleZone.
		for (CastleZone zone : ZoneManager.getInstance().getAllZones(CastleZone.class))
		{
			if (zone.getResidenceId() == _id)
			{
				_castleZone = zone;
				break;
			}
		}
		
		// Feed _teleZone.
		for (CastleTeleportZone zone : ZoneManager.getInstance().getAllZones(CastleTeleportZone.class))
		{
			if (zone.getCastleId() == _id)
			{
				_teleZone = zone;
				break;
			}
		}
		
		loadFunctions();
	}
	
	public CastleFunction getFunction(int type)
	{
		if (_function.containsKey(type))
			return _function.get(type);
		
		return null;
	}
	
	public synchronized void engrave(Clan clan, WorldObject target)
	{
		if (!isGoodArtifact(target))
			return;
		
		// "Clan X engraved the ruler" message.
		getSiege().announce(SystemMessage.getSystemMessage(SystemMessageId.CLAN_S1_ENGRAVED_RULER).addString(clan.getName()), SiegeSide.ATTACKER, SiegeSide.DEFENDER);
		
		setOwner(clan);
	}
	
	/**
	 * Tax the positive value set as parameter.
	 * @param amount : The Adena amount to add to treasury.
	 * @return The leftover value after taxes being applied.
	 */
	private long tax(long amount)
	{
		if (_ownerId <= 0 || amount <= 0)
			return 0;
		
		// Cut the tax amount using the system rate.
		if (_taxSysgetRate > 0)
			amount -= (_taxSysgetRate / 100.0) * amount;
		
		// Test if parent exists and a tribute rate is set.
		if (_parentId > 0 && _tributeRate > 0)
		{
			final Castle parentCastle = CastleManager.getInstance().getCastleById(_parentId);
			if (parentCastle != null)
			{
				// Calculate the tribute tax.
				final int tax = (int) ((_tributeRate / 100.0) * amount);
				if (tax > 0)
				{
					// Add the tax to the parent castle's tax revenue. Bypass the tax calculation.
					parentCastle.riseTaxRevenue(tax, true);
					
					// Subtract parent tax from income in all cases.
					amount -= tax;
				}
			}
		}
		
		return amount;
	}
	
	/**
	 * Add or remove amount to {@link Castle}'s treasury. Also refresh tax revenue and seed income.
	 * @param amount : The Adena amount to add to treasury.
	 * @param save : If true, we save into db.
	 * @return True if successful or false otherwise.
	 */
	public boolean editTreasury(long amount, boolean save)
	{
		// Do nothing if no owner, or amount is equals to 0.
		if (_ownerId <= 0 || amount == 0)
			return false;
		
		if (amount < 0)
		{
			// Abort if we try to pick too much from treasury.
			if (_treasury < Math.abs(amount))
				return false;
			
			_treasury += amount;
		}
		// Can't grow higher than Integer.MAX_VALUE.
		else if (_treasury + amount > Integer.MAX_VALUE)
			_treasury = Integer.MAX_VALUE;
		else
			_treasury += amount;
		
		if (save)
		{
			try (Connection con = ConnectionPool.getConnection();
				PreparedStatement ps = con.prepareStatement(UPDATE_TREASURY))
			{
				ps.setLong(1, _treasury);
				ps.setInt(2, _id);
				ps.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.error("Couldn't update treasury.", e);
			}
		}
		return true;
	}
	
	public void riseTaxRevenue(long amount)
	{
		riseTaxRevenue(amount, false);
	}
	
	public void riseTaxRevenue(long amount, boolean bypassTax)
	{
		// Do nothing if no owner or amount is equals to 0.
		if (_ownerId <= 0 || amount <= 0)
			return;
		
		// Calculate and cut down taxes.
		if (!bypassTax)
			amount = tax(amount);
		
		if (_taxRevenue < Integer.MAX_VALUE)
		{
			// Can't grow higher than Integer.MAX_VALUE.
			if (_taxRevenue + amount > Integer.MAX_VALUE)
				_taxRevenue = Integer.MAX_VALUE;
			else
				_taxRevenue += amount;
			
			try (Connection con = ConnectionPool.getConnection();
				PreparedStatement ps = con.prepareStatement(UPDATE_TAX_REVENUE))
			{
				ps.setLong(1, _taxRevenue);
				ps.setInt(2, _id);
				ps.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.error("Couldn't update tax revenue.", e);
			}
		}
		
	}
	
	public void riseSeedIncome(long amount)
	{
		// Do nothing if no owner or amount is equals to 0.
		if (_ownerId <= 0 || amount <= 0)
			return;
		
		// Calculate and cut down taxes.
		amount = tax(amount);
		
		if (_seedIncome < Integer.MAX_VALUE)
		{
			// Can't grow higher than Integer.MAX_VALUE.
			if (_seedIncome + amount > Integer.MAX_VALUE)
				_seedIncome = Integer.MAX_VALUE;
			else
				_seedIncome += amount;
			
			try (Connection con = ConnectionPool.getConnection();
				PreparedStatement ps = con.prepareStatement(UPDATE_SEED_INCOME))
			{
				ps.setLong(1, _seedIncome);
				ps.setInt(2, _id);
				ps.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.error("Couldn't update seed income.", e);
			}
		}
	}
	
	/**
	 * Update taxes for this {@link Castle}.<br>
	 * <br>
	 * For none owned castle :
	 * <ul>
	 * <li>Reset all vars as default.</li>
	 * <li>Use default tax rate for both current and next vars.</li>
	 * </ul>
	 * For owned castle :
	 * <ul>
	 * <li>Increase treasury based on tax revenue and seed income.</li>
	 * <li>Reset tax revenue and seed income vars.</li>
	 * <li>Set current tax using next tax rate.</li>
	 * </ul>
	 */
	public void updateTaxes()
	{
		// Update all castles with a single query.
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(UPDATE_TAXES))
		{
			// If not owned, reset all vars as default.
			if (isFree())
			{
				// Reset variables.
				setTreasury(0);
				setTaxRevenue(0);
				setSeedIncome(0);
				
				// Batch the query.
				ps.setLong(1, 0);
				ps.setLong(2, 0);
				ps.setLong(3, 0);
				ps.setInt(4, getDefaultTaxRate());
				ps.setInt(5, getDefaultTaxRate());
			}
			// Increase treasury based on tax revenue and seed income. Reset those 2 vars. Set the current tax rate as next tax rate. SQL query everything.
			else
			{
				// Set treasury, don't call the SQL query. Reset tax revenue and seed income vars.
				editTreasury(getTaxRevenue() + getSeedIncome(), false);
				setTaxRevenue(0);
				setSeedIncome(0);
				
				// Set current tax as next tax. Don't edit next tax rate, it will be saved as it.
				setCurrentTaxPercent(getNextTaxPercent(), false);
				
				// Batch the query.
				ps.setLong(1, getTreasury());
				ps.setLong(2, 0);
				ps.setLong(3, 0);
				ps.setInt(4, getCurrentTaxPercent());
				ps.setInt(5, getNextTaxPercent());
			}
			ps.setInt(6, getId());
			ps.addBatch();
			ps.executeBatch();
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to update taxes for {}.", e, getName());
		}
	}
	
	/**
	 * Move non clan members off castle area and to nearest town.
	 */
	public void banishForeigners()
	{
		getCastleZone().banishForeigners(_ownerId);
	}
	
	public SiegeZone getSiegeZone()
	{
		return _siegeZone;
	}
	
	public CastleZone getCastleZone()
	{
		return _castleZone;
	}
	
	public CastleTeleportZone getTeleZone()
	{
		return _teleZone;
	}
	
	public void oustAllPlayers()
	{
		getTeleZone().oustAllPlayers();
	}
	
	public int getLeftCertificates()
	{
		return _leftCertificates;
	}
	
	/**
	 * Set (and optionally save on database) left certificates amount.
	 * @param leftCertificates : The amount to save.
	 * @param storeInDb : If true, we store it on database. Basically set to false on server startup.
	 */
	public void setLeftCertificates(int leftCertificates, boolean storeInDb)
	{
		_leftCertificates = leftCertificates;
		
		if (storeInDb)
		{
			try (Connection con = ConnectionPool.getConnection();
				PreparedStatement ps = con.prepareStatement(UPDATE_CERTIFICATES))
			{
				ps.setInt(1, leftCertificates);
				ps.setInt(2, _id);
				ps.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.error("Couldn't update certificates amount.", e);
			}
		}
	}
	
	/**
	 * This method setup the castle owner.
	 * @param clan The clan who will own the castle.
	 */
	public void setOwner(Clan clan)
	{
		// Act only if castle owner is different of NPC, or if old owner is different of new owner.
		if (_ownerId > 0 && (clan == null || clan.getClanId() != _ownerId))
		{
			// Try to find clan instance of the old owner.
			Clan oldOwner = ClanTable.getInstance().getClan(_ownerId);
			if (oldOwner != null)
			{
				// Dismount the old leader if he was riding a wyvern.
				Player oldLeader = oldOwner.getLeader().getPlayerInstance();
				if (oldLeader != null && oldLeader.getMountType() == 2)
					oldLeader.dismount();
				
				// Unset castle flag for old owner clan.
				oldOwner.setCastle(0);
			}
		}
		
		// Update database.
		updateOwnerInDB(clan);
		
		// If siege is in progress, mid victory phase of siege.
		if (getSiege().isInProgress())
			getSiege().midVictory();
	}
	
	/**
	 * Remove the castle owner. This method is only used by admin command.
	 **/
	public void removeOwner()
	{
		if (_ownerId <= 0)
			return;
		
		final Clan clan = ClanTable.getInstance().getClan(_ownerId);
		if (clan == null)
			return;
		
		clan.setCastle(0);
		clan.broadcastToMembers(new PledgeShowInfoUpdate(clan));
		
		// Remove clan from siege registered clans (as owners are automatically added).
		getSiege().getRegisteredClans().remove(clan);
		
		// Delete all spawned tickets.
		for (ItemInstance item : _droppedTickets)
			item.decayMe();
		
		// Clear the List.
		_droppedTickets.clear();
		
		for (Player player : clan.getOnlineMembers())
		{
			final Map<Integer, Integer> skill = player.isClanLeader() ? getSkillsLeader() : getSkillsMember();
			if (getId() == player.getClan().getCastleId())
			{
				skill.forEach((skillId, skillLvl) ->
				{
					player.addSkill(SkillTable.getInstance().getInfo(skillId, skillLvl), true);
				});
			}
			else
			{
				skill.forEach((skillId, skillLvl) ->
				{
					player.removeSkill(skillId, true);
				});
			}
		}
		
		// Unspawn Mercenaries, clear the List.
		_siegeGuards.forEach(npc -> npc.doDie(npc));
		_siegeGuards.clear();
		
		updateOwnerInDB(null);
		
		if (getSiege().isInProgress())
			getSiege().midVictory();
		else
			checkItemsForClan(clan);
	}
	
	public void setCurrentTaxPercent(int value, boolean save)
	{
		// Don't update if already set to same value.
		if (_currentTaxPercent == value)
			return;
		
		_currentTaxPercent = value;
		_taxRate = _currentTaxPercent / 100.0;
		
		if (save)
		{
			try (Connection con = ConnectionPool.getConnection();
				PreparedStatement ps = con.prepareStatement(UPDATE_CURRENT_TAX))
			{
				ps.setInt(1, value);
				ps.setInt(2, _id);
				ps.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.error("Couldn't update tax amount.", e);
			}
		}
	}
	
	public void setNextTaxPercent(int value, boolean save)
	{
		// Don't update if already set to same value.
		if (_nextTaxPercent == value)
			return;
		
		_nextTaxPercent = value;
		
		if (save)
		{
			try (Connection con = ConnectionPool.getConnection();
				PreparedStatement ps = con.prepareStatement(UPDATE_NEXT_TAX))
			{
				ps.setInt(1, value);
				ps.setInt(2, _id);
				ps.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.error("Couldn't update tax amount.", e);
			}
		}
	}
	
	/**
	 * Respawn doors associated to that castle.
	 * @param isDoorWeak if true, spawn doors with 50% max HPs.
	 */
	public void spawnDoors(boolean isDoorWeak)
	{
		for (Door door : _doors)
		{
			if (door.isDead())
				door.doRevive();
			
			door.closeMe();
			door.getStatus().setHp((isDoorWeak) ? door.getStatus().getMaxHp() / 2 : door.getStatus().getMaxHp());
		}
	}
	
	/**
	 * Upgrade door.
	 * @param doorId The doorId to affect.
	 * @param hp The hp ratio.
	 * @param db If set to true, save changes on database.
	 */
	public void upgradeDoor(int doorId, int hp, boolean db)
	{
		final Door door = getDoor(doorId);
		if (door == null)
			return;
		
		door.getStatus().setUpgradeHpRatio(hp);
		door.getStatus().setMaxHp();
		
		if (db)
		{
			try (Connection con = ConnectionPool.getConnection();
				PreparedStatement ps = con.prepareStatement(UPDATE_DOORS))
			{
				ps.setInt(1, doorId);
				ps.setInt(2, hp);
				ps.setInt(3, _id);
				ps.execute();
			}
			catch (Exception e)
			{
				LOGGER.error("Couldn't upgrade castle doors.", e);
			}
		}
	}
	
	/**
	 * This method is only used on siege midVictory.
	 */
	public void removeDoorUpgrade()
	{
		for (Door door : _doors)
			door.getStatus().setUpgradeHpRatio(1);
		
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_DOOR))
		{
			ps.setInt(1, _id);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't delete door upgrade.", e);
		}
	}
	
	private void updateOwnerInDB(Clan clan)
	{
		if (clan != null)
		{
			// Update owner id property.
			_ownerId = clan.getClanId();
			
			// Set castle for new owner.
			clan.setCastle(_id);
			
			// Announce to clan members.
			clan.broadcastToMembers(new PledgeShowInfoUpdate(clan), new PlaySound(1, "Siege_Victory"));
		}
		else
		{
			// Remove owner.
			_ownerId = 0;
			
			// Reset manor data.
			CastleManorManager.getInstance().resetManorData(_id);
			
			// Reset financial variables.
			_treasury = 0;
			_taxRevenue = 0;
			_seedIncome = 0;
			
			setCurrentTaxPercent(getDefaultTaxRate(), false);
			setNextTaxPercent(getDefaultTaxRate(), false);
			
			try (Connection con = ConnectionPool.getConnection();
				PreparedStatement ps = con.prepareStatement(UPDATE_TAXES))
			{
				ps.setLong(1, _treasury);
				ps.setLong(2, _taxRevenue);
				ps.setLong(3, _seedIncome);
				ps.setInt(4, getDefaultTaxRate());
				ps.setInt(5, getDefaultTaxRate());
				ps.setInt(6, _id);
				ps.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.error("Couldn't reset financial vars.", e);
			}
		}
		
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_OLD_OWNER);
			PreparedStatement ps2 = con.prepareStatement(UPDATE_NEW_OWNER))
		{
			ps.setInt(1, _id);
			ps.executeUpdate();
			
			ps2.setInt(1, _id);
			ps2.setInt(2, _ownerId);
			ps2.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't update castle owner.", e);
		}
	}
	
	public List<MercenaryTicket> getTickets()
	{
		return _tickets;
	}
	
	public MercenaryTicket getTicket(int itemId)
	{
		return _tickets.stream().filter(t -> t.getItemId() == itemId).findFirst().orElse(null);
	}
	
	public void addTicket(StatSet set)
	{
		_tickets.add(new MercenaryTicket(set));
	}
	
	public Set<ItemInstance> getDroppedTickets()
	{
		return _droppedTickets;
	}
	
	public void addDroppedTicket(ItemInstance item)
	{
		_droppedTickets.add(item);
	}
	
	public void removeDroppedTicket(ItemInstance item)
	{
		_droppedTickets.remove(item);
	}
	
	public int getDroppedTicketsCount(int itemId)
	{
		return (int) _droppedTickets.stream().filter(t -> t.getItemId() == itemId).count();
	}
	
	public boolean isTooCloseFromDroppedTicket(int x, int y, int z)
	{
		return _droppedTickets.stream().anyMatch(i -> i.isIn3DRadius(x, y, z, 25));
	}
	
	/**
	 * That method is used to spawn NPCs, being neutral guards or player-based mercenaries.
	 * <ul>
	 * <li>If castle got an owner, it spawns mercenaries following tickets. Otherwise it uses SpawnManager territory.</li>
	 * <li>It feeds the nearest Control Tower with the spawn. If tower is broken, associated spawns are removed.</li>
	 * </ul>
	 */
	public void spawnSiegeGuardsOrMercenaries()
	{
		if (_ownerId > 0)
		{
			// Spawn Guards.
			SpawnManager.getInstance().startSpawnTime("pc_siege_warfare_start", String.valueOf(_id), null, null, true);
			
			for (ItemInstance item : _droppedTickets)
			{
				// Retrieve MercenaryTicket information.
				final MercenaryTicket ticket = getTicket(item.getItemId());
				if (ticket == null)
					continue;
				
				try
				{
					final Spawn spawn = new Spawn(ticket.getNpcId());
					spawn.setLoc(item.getPosition());
					
					// Spawn the Npc.
					final Npc guard = spawn.doSpawn(false);
					guard.setResidence(this);
					
					_siegeGuards.add(guard);
				}
				catch (Exception e)
				{
					LOGGER.error("Couldn't spawn npc ticket {}. ", e, ticket.getNpcId());
					continue;
				}
				
				// Delete the ticket item.
				item.decayMe();
			}
			
			_droppedTickets.clear();
		}
		// Spawn Guards.
		else
			SpawnManager.getInstance().startSpawnTime("siege_warfare_start", String.valueOf(_id), null, null, true);
	}
	
	/**
	 * Despawn neutral guards or player-based mercenaries.
	 */
	public void despawnSiegeGuardsOrMercenaries()
	{
		_siegeGuards.forEach(Npc::deleteMe);
		_siegeGuards.clear();
		
		// Despawn Guards.
		SpawnManager.getInstance().stopSpawnTime("pc_siege_warfare_start", String.valueOf(_id), null, null, true);
		SpawnManager.getInstance().stopSpawnTime("siege_warfare_start", String.valueOf(_id), null, null, true);
	}
	
	public List<TowerSpawnLocation> getControlTowers()
	{
		return _controlTowers;
	}
	
	public int getAliveLifeTowerCount()
	{
		return (int) _controlTowers.stream().filter(ct -> ct.getType() == TowerType.LIFE_CONTROL && ct.getNpc() != null && (!getSiege().isInProgress() || ct.getNpc().getPolymorphTemplate() != null)).count();
	}
	
	public int getCircletId()
	{
		return _circletId;
	}
	
	public void setCircletId(int circletId)
	{
		_circletId = circletId;
	}
	
	public Siege getSiege()
	{
		return _siege;
	}
	
	public void launchSiege()
	{
		_siege = new Siege(this);
	}
	
	public Calendar getSiegeDate()
	{
		return _siegeDate;
	}
	
	public void setSiegeDate(Calendar siegeDate)
	{
		_siegeDate = siegeDate;
	}
	
	public boolean isTimeRegistrationOver()
	{
		return _isTimeRegistrationOver;
	}
	
	public void setTimeRegistrationOver(boolean val)
	{
		_isTimeRegistrationOver = val;
	}
	
	public int getCurrentTaxPercent()
	{
		return _currentTaxPercent;
	}
	
	public int getNextTaxPercent()
	{
		return _nextTaxPercent;
	}
	
	public double getTaxRate()
	{
		return _taxRate;
	}
	
	public long getTreasury()
	{
		return _treasury;
	}
	
	public void setTreasury(long treasury)
	{
		_treasury = treasury;
	}
	
	public long getTaxRevenue()
	{
		return _taxRevenue;
	}
	
	public void setTaxRevenue(long taxRevenue)
	{
		_taxRevenue = taxRevenue;
	}
	
	public long getSeedIncome()
	{
		return _seedIncome;
	}
	
	public void setSeedIncome(long seedIncome)
	{
		_seedIncome = seedIncome;
	}
	
	public Map<Integer, SpawnLocation> getArtifacts()
	{
		return _artifacts;
	}
	
	public Map<Integer, SiegeInfo> getSieges()
	{
		return _sieges;
	}
	
	public Map<Integer, Integer> getSkillsMember()
	{
		return _skillMember;
	}
	
	public Map<Integer, Integer> getSkillsLeader()
	{
		return _skillLeader;
	}
	
	public Map<Integer, ItemInfo> getItemsMember()
	{
		return _itemsMember;
	}
	
	public Map<Integer, ItemInfo> getItemsLeader()
	{
		return _itemsLeader;
	}
	
	public boolean isGoodArtifact(WorldObject object)
	{
		return object instanceof HolyThing holyThing && _artifacts.containsKey(holyThing.getNpcId());
	}
	
	/**
	 * @param towerIndex : The index to check on.
	 * @return the trap upgrade level for a dedicated tower index.
	 */
	public int getTrapUpgradeLevel(int towerIndex)
	{
		final TowerSpawnLocation spawn = _controlTowers.get(towerIndex);
		return (spawn != null) ? spawn.getUpgradeLevel() : 0;
	}
	
	/**
	 * Save properties of a Flame Tower.
	 * @param towerIndex : The tower to affect.
	 * @param level : The new level of update.
	 * @param save : Should it be saved on database or not.
	 */
	public void setTrapUpgrade(int towerIndex, int level, boolean save)
	{
		if (save)
		{
			try (Connection con = ConnectionPool.getConnection();
				PreparedStatement ps = con.prepareStatement(UPDATE_TRAP))
			{
				ps.setInt(1, _id);
				ps.setInt(2, towerIndex);
				ps.setInt(3, level);
				ps.execute();
			}
			catch (Exception e)
			{
				LOGGER.error("Couldn't replace trap upgrade.", e);
			}
		}
		
		final TowerSpawnLocation spawn = _controlTowers.get(towerIndex);
		if (spawn != null)
			spawn.setUpgradeLevel(level);
	}
	
	/**
	 * Delete all traps informations for a single castle.
	 */
	public void removeTrapUpgrade()
	{
		for (TowerSpawnLocation ts : _controlTowers)
			ts.setUpgradeLevel(0);
		
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_TRAP))
		{
			ps.setInt(1, _id);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't delete trap upgrade.", e);
		}
	}
	
	public void checkItemsForMember(ClanMember member)
	{
		final Player player = member.getPlayerInstance();
		if (player != null)
			player.checkItemRestriction();
		else
		{
			try (Connection con = ConnectionPool.getConnection();
				PreparedStatement ps = con.prepareStatement(UPDATE_ITEMS_LOC))
			{
				ps.setInt(1, _circletId);
				ps.setInt(2, member.getObjectId());
				ps.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.error("Couldn't update items for member.", e);
			}
		}
	}
	
	public void checkItemsForClan(Clan clan)
	{
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(UPDATE_ITEMS_LOC))
		{
			ps.setInt(1, _circletId);
			
			for (ClanMember member : clan.getMembers())
			{
				final Player player = member.getPlayerInstance();
				if (player != null)
					player.checkItemRestriction();
				else
				{
					ps.setInt(2, member.getObjectId());
					ps.addBatch();
				}
			}
			ps.executeBatch();
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't update items for clan.", e);
		}
	}
	
	private void loadFunctions()
	{
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement statement = con.prepareStatement(LOAD_FUNCTIONS))
		{
			statement.setInt(1, _id);
			try (ResultSet rs = statement.executeQuery())
			{
				while (rs.next())
				{
					_function.put(rs.getInt("type"), new CastleFunction(rs.getInt("type"), rs.getInt("lvl"), rs.getInt("lease"), 0, rs.getLong("rate"), rs.getLong("endTime"), true));
				}
			}
			statement.clearParameters();
		}
		catch (Exception e)
		{
			LOGGER.error("Exception: Castle.loadFunctions(): " + e.getMessage(), e);
		}
	}
	
	public void removeFunction(int functionType)
	{
		_function.remove(functionType);
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement statement = con.prepareStatement(DELETE_FUNCTIONS))
		{
			statement.setInt(1, getOwnerId());
			statement.setInt(2, functionType);
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER.error("Exception: Castle.removeFunctions(int functionType): " + e.getMessage(), e);
		}
	}
	
	public boolean updateFunctions(Player player, int type, int lvl, int lease, long rate, boolean addNew)
	{
		if (player == null)
			return false;
		
		if (lease > 0)
		{
			if (!player.destroyItemByItemId(PcInventory.ADENA_ID, lease, true))
				return false;
		}
		
		if (addNew)
			_function.put(type, new CastleFunction(type, lvl, lease, 0, rate, 0, false));
		else
		{
			if ((lvl == 0) && (lease == 0))
				removeFunction(type);
			else
			{
				int diffLease = lease - _function.get(type).getLease();
				if (diffLease > 0)
				{
					_function.remove(type);
					_function.put(type, new CastleFunction(type, lvl, lease, 0, rate, -1, false));
				}
				else
				{
					_function.get(type).setLease(lease);
					_function.get(type).setLvl(lvl);
					_function.get(type).dbSave();
				}
			}
		}
		return true;
	}
	
	public class CastleFunction
	{
		private final int _type;
		private int _lvl;
		protected int _fee;
		protected int _tempFee;
		private final long _rate;
		private long _endDate;
		protected boolean _inDebt;
		public boolean _cwh;
		
		public CastleFunction(int type, int lvl, int lease, int tempLease, long rate, long time, boolean cwh)
		{
			_type = type;
			_lvl = lvl;
			_fee = lease;
			_tempFee = tempLease;
			_rate = rate;
			_endDate = time;
			initializeTask(cwh);
		}
		
		public int getType()
		{
			return _type;
		}
		
		public int getLvl()
		{
			return _lvl;
		}
		
		public int getLease()
		{
			return _fee;
		}
		
		public long getRate()
		{
			return _rate;
		}
		
		public long getEndTime()
		{
			return _endDate;
		}
		
		public void setLvl(int lvl)
		{
			_lvl = lvl;
		}
		
		public void setLease(int lease)
		{
			_fee = lease;
		}
		
		public void setEndTime(long time)
		{
			_endDate = time;
		}
		
		private void initializeTask(boolean cwh)
		{
			if (getOwnerId() <= 0)
				return;
			
			long currentTime = System.currentTimeMillis();
			if (_endDate > currentTime)
				ThreadPool.schedule(new FunctionTask(cwh), _endDate - currentTime);
			else
				ThreadPool.schedule(new FunctionTask(cwh), 0);
		}
		
		private class FunctionTask implements Runnable
		{
			public FunctionTask(boolean cwh)
			{
				_cwh = cwh;
			}
			
			@Override
			public void run()
			{
				try
				{
					if (getOwnerId() <= 0)
						return;
					
					if ((ClanTable.getInstance().getClan(getOwnerId()).getWarehouse().getAdena() >= _fee) || !_cwh)
					{
						int fee = _fee;
						if (getEndTime() == -1)
							fee = _tempFee;
						
						setEndTime(System.currentTimeMillis() + getRate());
						dbSave();
						if (_cwh)
							ClanTable.getInstance().getClan(getOwnerId()).getWarehouse().destroyItemByItemId(PcInventory.ADENA_ID, fee);
						
						ThreadPool.schedule(new FunctionTask(true), getRate());
					}
					else
						removeFunction(getType());
				}
				catch (Exception e)
				{
					LOGGER.error("", e);
				}
			}
		}
		
		public void dbSave()
		{
			try (Connection con = ConnectionPool.getConnection();
				PreparedStatement statement = con.prepareStatement(UPDATE_FUNCTIONS))
			{
				statement.setInt(1, getId());
				statement.setInt(2, getType());
				statement.setInt(3, getLvl());
				statement.setInt(4, getLease());
				statement.setLong(5, getRate());
				statement.setLong(6, getEndTime());
				statement.execute();
			}
			catch (Exception e)
			{
				LOGGER.error("Exception: Castle.updateFunctions(int type, int lvl, int lease, long rate, long time, boolean addNew): " + e.getMessage(), e);
			}
		}
	}
}