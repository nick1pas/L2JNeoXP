package net.sf.l2j.AutoFarm;

import net.sf.l2j.Config;


import net.sf.l2j.commons.math.MathUtil;
import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.ShortcutType;
import net.sf.l2j.gameserver.enums.TeamType;
import net.sf.l2j.gameserver.enums.skills.SkillTargetType;
import net.sf.l2j.gameserver.enums.skills.SkillType;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.handler.ItemHandler;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.AutoFarm;
import net.sf.l2j.gameserver.model.Shortcut;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.WorldRegion;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;

import net.sf.l2j.gameserver.model.actor.instance.Chest;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage.SMPOS;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AutofarmPlayerRoutine
{
	private final Player player;
	private ScheduledFuture<?> _task;
	private Creature committedTarget = null;

	public AutofarmPlayerRoutine(Player player)
	{
		this.player = player;
	}

	public void start()
	{
		if (_task == null)
		{
			
            
            if (isIpAllowed(player.getIP())) {
                player.sendMessage("Solo puedes usar Auto Farm con una IP a la vez.");
                return;
            }else {
    			_task = ThreadPool.scheduleAtFixedRate(() -> executeRoutine(), 450, 450);
    			
    			player.sendPacket(new ExShowScreenMessage("Auto Farming Actived...", 5*1000, SMPOS.TOP_CENTER, false));
    			player.sendPacket(new SystemMessage(SystemMessageId.AUTO_FARM_ACTIVATED));
    		    player.setTeam(player.isMageClass() ? TeamType.BLUE : TeamType.RED);
    		    insertIpEntry(player.getObjectId(), player.getIP());
    		    player.broadcastUserInfo();
            }
            
            
            
            
			

		
		}
	}
	
	public void stop()
	{
		if (_task != null)
		{
			
			removeIpEntry(player.getObjectId());
			_task.cancel(false);
			_task = null;
			
			player.sendPacket(new ExShowScreenMessage("Auto Farming Deactivated...", 5*1000, SMPOS.TOP_CENTER, false));
			player.sendPacket(new SystemMessage(SystemMessageId.AUTO_FARM_DESACTIVATED));
			player.setTeam(TeamType.NONE);
			player.broadcastUserInfo();
		   
		}
	}
	
	
    // Método para insertar la IP del jugador en la tabla Auto_Farm_Ip
    private void insertIpEntry(int charId, String ip) {
        try (Connection con = ConnectionPool.getConnection()) {
            String insertSql = "INSERT INTO Auto_Farm_Ip (char_id, ip) VALUES (?, ?)";
            try (PreparedStatement insertStatement = con.prepareStatement(insertSql)) {
                insertStatement.setInt(1, charId);
                insertStatement.setString(2, ip);
                insertStatement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
   
    public static boolean isIpAllowed( String ip) {
        try (Connection con = ConnectionPool.getConnection()) {
            String selectSql = "SELECT * FROM Auto_Farm_Ip WHERE ip = ?";
            try (PreparedStatement selectStatement = con.prepareStatement(selectSql)) {
               
                selectStatement.setString(1, ip);
                try (ResultSet result = selectStatement.executeQuery()) {
                    return result.next(); 
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false; // En caso de error o si no hay coincidencia
    }

    // Método para eliminar la entrada del jugador en la tabla Auto_Farm_Ip
    public static void removeIpEntry(int charId) {
        try (Connection con = ConnectionPool.getConnection()) {
            String deleteSql = "DELETE FROM Auto_Farm_Ip WHERE char_id = ?";
            try (PreparedStatement deleteStatement = con.prepareStatement(deleteSql)) {
                deleteStatement.setInt(1, charId);
                deleteStatement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
	
	
	
	public void executeRoutine()
	{
		if (player.isNoBuffProtected() && player.getAllEffects().length <= 8)
		{
			player.sendMessage("You don't have buffs to use autofarm.");
			player.broadcastUserInfo();
			stop();	
			player.setAutoFarm(false);
			AutoFarm.showAutoFarm(player);
			return;
		}
		
		calculatePotions();
		checkSpoil();
		targetEligibleCreature();
	    if (player.isMageClass()) 
	    {
	        useAppropriateSpell(); // Prioriza el uso de hechizos para los magos
	    }
	    else if (shotcutsContainAttack())
	    {
	        attack(); // Si es una clase no maga y tiene la acción de ataque en los shortcuts
	    }
	    else
	    {
	        useAppropriateSpell(); // Si es una clase no maga y no tiene la acción de ataque, usa hechizos
	    }
		checkSpoil();
		useAppropriateSpell();
	}

	private void attack() 
	{
		Boolean shortcutsContainAttack = shotcutsContainAttack();
		
	    if (shortcutsContainAttack) 
	        physicalAttack();
	}

	private void useAppropriateSpell() 
	{
		L2Skill chanceSkill = nextAvailableSkill(getChanceSpells(), AutofarmSpellType.Chance);

		if (chanceSkill != null)
		{
			doSkill(chanceSkill, false);
			return;
		}

		L2Skill lowLifeSkill = nextAvailableSkill(getLowLifeSpells(), AutofarmSpellType.LowLife);

		if (lowLifeSkill != null) 
		{
			doSkill(lowLifeSkill, true);
			return;
		}

		L2Skill attackSkill = nextAvailableSkill(getAttackSpells(), AutofarmSpellType.Attack);

		if (attackSkill != null) 
		{
			doSkill(attackSkill, false);
			return;
		}
	}

	public L2Skill nextAvailableSkill(List<Integer> skillIds, AutofarmSpellType spellType) 
	{
		for (Integer skillId : skillIds) 
		{
			L2Skill skill = player.getSkill(skillId);

			if (skill == null) 
				continue;
			
			if (skill.getSkillType() == SkillType.SIGNET || skill.getSkillType() == SkillType.SIGNET_CASTTIME)
				continue;

			if (player.isSkillDisabled(skill)) 
				continue;

			if (isSpoil(skillId))
			{
				if (monsterIsAlreadySpoiled())
				{
					continue;
				}
				return skill;
			}
			
			if (spellType == AutofarmSpellType.Chance && getMonsterTarget() != null)
			{
				if (getMonsterTarget().getFirstEffect(skillId) == null) 
					return skill;
				continue;
			}

			if (spellType == AutofarmSpellType.LowLife && getHpPercentage() > player.getHealPercent()) 
				break;

			return skill;
		}

		return null;
	}
	
	private void checkSpoil() 
	{
		if (canBeSweepedByMe() && getMonsterTarget().isDead()) 
		{
			L2Skill sweeper = player.getSkill(42);
			if (sweeper == null) 
				return;
			
			doSkill(sweeper, false);
		}
	}

	private Double getHpPercentage() 
	{
		return player.getStatus().getHp() * 100.0f / player.getStatus().getMaxHp();
	}

	private Double percentageMpIsLessThan() 
	{
		return player.getStatus().getMp() * 100.0f / player.getStatus().getMaxMp();
	}

	private Double percentageHpIsLessThan()
	{
		return player.getStatus().getHp() * 100.0f / player.getStatus().getMaxHp();
	}

	private List<Integer> getAttackSpells()
	{
		return getSpellsInSlots(AutofarmConstants.attackSlots);
	}

	private List<Integer> getSpellsInSlots(List<Integer> attackSlots) 
	{
		return Arrays.stream(player.getShortcutList().getShortcuts()).filter(shortcut -> shortcut.getPage() == player.getPage() && shortcut.getType() == ShortcutType.SKILL && attackSlots.contains(shortcut.getSlot())).map(Shortcut::getId).collect(Collectors.toList());
	}

	private List<Integer> getChanceSpells()
	{
		return getSpellsInSlots(AutofarmConstants.chanceSlots);
	}

	private List<Integer> getLowLifeSpells()
	{
		return getSpellsInSlots(AutofarmConstants.lowLifeSlots);
	}

	private boolean shotcutsContainAttack() 
	{
		return Arrays.stream(player.getShortcutList().getShortcuts()).anyMatch(shortcut -> shortcut.getPage() == player.getPage() && shortcut.getType() == ShortcutType.ACTION && (shortcut.getId() == 2 || player.isSummonAttack() && shortcut.getId() == 22));
	}
	
	
	
	
	private boolean monsterIsAlreadySpoiled()
	{
		return getMonsterTarget() != null && getMonsterTarget().getSpoilState().isSpoiled();
	}
	
	private static boolean isSpoil(Integer skillId)
	{
		return skillId == 254 || skillId == 302;
	}
	
	private boolean canBeSweepedByMe() 
	{
	    return getMonsterTarget() != null && getMonsterTarget().isDead() && getMonsterTarget().getSpoilState().isSweepable();
	}
	

	
		private void doSkill(L2Skill skill, boolean isSelfSkill)
		{
			final WorldObject target = player.getTarget();
			if (skill == null || !(target instanceof Creature))
				return;
			
			if (skill.getSkillType() == SkillType.RECALL && !Config.KARMA_PLAYER_CAN_TELEPORT && player.getKarma() > 0)
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			if (skill.isToggle() && player.isMounted())
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			if (player.isOutOfControl())
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			
			
			
			
			if (isNecessarySkill(skill))
				player.getAI().tryToCast(isSelfSkill ? player : (Creature) target, skill);
		}
	
	
		private boolean isNecessarySkill(L2Skill skill)
		{
			if (skill == null)
				return false;
			
			final WorldObject target = player.getTarget();
			if (target instanceof Monster)
			{
				final Monster monster = (Monster) target;
				if (skill.getSkillType() == SkillType.SPOIL && monster.getSpoilState().isSpoiled())
					return false;
				
				List<AbstractEffect> effects = Arrays.stream(monster.getAllEffects()).filter(e -> e.getSkill().isDebuff()).collect(Collectors.toList());
				if (effects != null && !effects.isEmpty() && effects.stream().anyMatch(e -> e.getSkill().getId() == skill.getId()))
					return false;
				
				if (!monster.isDead() && skill.getTargetType() == SkillTargetType.CORPSE_MOB)
					return false;
				
				return true;
			}
			return false;
		}

	private void physicalAttack()
	{
	    if (!(player.getTarget() instanceof Monster)) 
	        return;

	    Monster target = (Monster) player.getTarget();

	    if (!player.isMageClass()) 
	    {
	        if (target.canAutoAttack(player) && GeoEngine.getInstance().canSeeTarget(player, target))
	        {
	            if (GeoEngine.getInstance().canSeeTarget(player, target))
	            {
	                player.getAI().tryToAttack(target);
	                player.onActionRequest();

	                if (player.isSummonAttack() && player.getSummon() != null)
	                {
	                    // Siege Golem's
	                    if (player.getSummon().getNpcId() >= 14702 && player.getSummon().getNpcId() <= 14798 || player.getSummon().getNpcId() >= 14839 && player.getSummon().getNpcId() <= 14869)
	                        return;

	                    Summon activeSummon = player.getSummon();
	                    activeSummon.setTarget(target);
	                    activeSummon.getAI().tryToAttack(target);

	                    int[] summonAttackSkills = {4261, 4068, 4137, 4260, 4708, 4709, 4710, 4712, 5135, 5138, 5141, 5442, 5444, 6095, 6096, 6041, 6044};
	                    if (Rnd.get(100) < player.getSummonSkillPercent())
	                    {
	                        for (int skillId : summonAttackSkills)
	                        {
	                            useMagicSkillBySummon(skillId, target);
	                        }
	                    }
	                }
	            }
	        }
	        else
	        {
	            if (target.canAutoAttack(player) && GeoEngine.getInstance().canSeeTarget(player, target))
	            if (GeoEngine.getInstance().canSeeTarget(player, target))
	                player.getAI().tryToFollow(target, false);
	        }
	    }
	    else
	    {
	        if (player.isSummonAttack() && player.getSummon() != null)
	        {
	            // Siege Golem's
	            if (player.getSummon().getNpcId() >= 14702 && player.getSummon().getNpcId() <= 14798 || player.getSummon().getNpcId() >= 14839 && player.getSummon().getNpcId() <= 14869)
	                return;

	            Summon activeSummon = player.getSummon();
	            activeSummon.setTarget(target);
	            activeSummon.getAI().tryToAttack(target);

	            int[] summonAttackSkills = {4261, 4068, 4137, 4260, 4708, 4709, 4710, 4712, 5135, 5138, 5141, 5442, 5444, 6095, 6096, 6041, 6044};
	            if (Rnd.get(100) < player.getSummonSkillPercent())
	            {
	                for (int skillId : summonAttackSkills)
	                {
	                    useMagicSkillBySummon(skillId, target);
	                }
	            }
	        }
	    }
	}

	private void useMagicSkillBySummon(int skillId, WorldObject target)
	{
	    // No owner, or owner in shop mode.
	    if (player == null || player.isInStoreMode())
	        return;
	    
	    final Summon activeSummon = player.getSummon();
	    if (activeSummon == null)
	        return;
	    
	    // Pet which is 20 levels higher than owner.
	    if (activeSummon instanceof Pet && activeSummon.getStatus().getLevel() - player.getStatus().getLevel() > 20)
	    {
	        player.sendPacket(SystemMessageId.PET_TOO_HIGH_TO_CONTROL);
	        return;
	    }
	    
	    // Out of control pet.
	    if (activeSummon.isOutOfControl())
	    {
	        player.sendPacket(SystemMessageId.PET_REFUSING_ORDER);
	        return;
	    }
	    
	    // Verify if the launched skill is mastered by the summon.
	    final L2Skill skill = activeSummon.getSkill(skillId);
	    if (skill == null)
	        return;
	    
	    // Can't launch offensive skills on owner.
	    if (skill.isOffensive() && player == target)
	        return;
	    
	    activeSummon.setTarget(target);
	    activeSummon.getAI().tryToCast(committedTarget, skill);
	}



	public void targetEligibleCreature() {
	    if (player.getTarget() == null) {
	        selectNewTarget(); // Llamada a selectNewTarget si el jugador no tiene un objetivo
	        return;
	    }

	    if (committedTarget != null) {
	        if (/*!isSameInstance(player, committedTarget) ||*/ (committedTarget.isDead() && GeoEngine.getInstance().canSeeTarget(player, committedTarget))) {
	            committedTarget = null;
	            selectNewTarget(); // Llamada a selectNewTarget después de que el objetivo actual muere o es de otra instancia
	            return;
	        } else if (!committedTarget.isDead() && GeoEngine.getInstance().canSeeTarget(player, committedTarget)) {
	            attack();
	            return;
	        } else if (!GeoEngine.getInstance().canSeeTarget(player, committedTarget)) {
	            committedTarget = null;
	            selectNewTarget(); // Buscar otro objetivo si el jugador no puede ver al objetivo actual
	            return;
	        }
	        player.getAI().tryToFollow(committedTarget, false);
	        committedTarget = null;
	        player.setTarget(null);
	    }
	    
	    if (committedTarget instanceof Summon) 
	        return;
	        
	    List<Monster> targets = getKnownMonstersInRadius(player, player.getRadius(), creature -> GeoEngine.getInstance().canMoveToTarget(player.getX(), player.getY(), player.getZ(), creature.getX(), creature.getY(), creature.getZ()) && !player.ignoredMonsterContain(creature.getNpcId()) && !creature.isRaidRelated() && !creature.isRaidBoss() && !creature.isDead() && !(creature instanceof Chest) && !(player.isAntiKsProtected() && creature.getTarget() != null && creature.getTarget() != player && creature.getTarget() != player.getSummon()) /*&& isSameInstance(player, creature)*/);

	    if (targets.isEmpty())
	        return;

	    Monster closestTarget = targets.stream().min((o1, o2) -> Integer.compare((int) Math.sqrt(player.distance2D(o1)), (int) Math.sqrt(player.distance2D(o2)))).get();

	    committedTarget = closestTarget;
	    player.setTarget(closestTarget);
	}


	// Función para verificar si dos objetos pertenecen a la misma instancia
/*	private static boolean isSameInstance(WorldObject obj1, WorldObject obj2) 
	{
	    return obj1.getInstanceId2() == obj2.getInstanceId2();
	}*/

	// Función para seleccionar un nuevo objetivo
	private void selectNewTarget() 
	{
	    List<Monster> targets = getKnownMonstersInRadius(player, player.getRadius(), creature -> GeoEngine.getInstance().canMoveToTarget(player.getX(), player.getY(), player.getZ(), creature.getX(), creature.getY(), creature.getZ()) && !player.ignoredMonsterContain(creature.getNpcId()) && !creature.isRaidRelated() && !creature.isRaidBoss() && !creature.isDead() && !(creature instanceof Chest) && !(player.isAntiKsProtected() && creature.getTarget() != null && creature.getTarget() != player && creature.getTarget() != player.getSummon()) /*&& isSameInstance(player, creature)*/);

	    if (targets.isEmpty())
	        return;

	    Monster closestTarget = targets.stream().min((o1, o2) -> Integer.compare((int) Math.sqrt(player.distance2D(o1)), (int) Math.sqrt(player.distance2D(o2)))).get();

	    committedTarget = closestTarget;
	    player.setTarget(closestTarget);
	}




	public final static List<Monster> getKnownMonstersInRadius(Player player, int radius, Function<Monster, Boolean> condition)
	{
		final WorldRegion region = player.getRegion();
		if (region == null)
			return Collections.emptyList();

		final List<Monster> result = new ArrayList<>();

		for (WorldRegion reg : region.getSurroundingRegions())
		{
			for (WorldObject obj : reg.getObjects())
			{
				if (!(obj instanceof Monster) || !MathUtil.checkIfInRange(radius, player, obj, true) || !condition.apply((Monster) obj))
					continue;

				result.add((Monster) obj);
			}
		}

		return result;
	}

	public Monster getMonsterTarget()
	{
		if(!(player.getTarget() instanceof Monster)) 
		{
			return null;
		}

		return (Monster)player.getTarget();
	}

	

	private void calculatePotions()
	{
		if (percentageHpIsLessThan() < player.getHpPotionPercentage())
			forceUseItem(1539); 

		if (percentageMpIsLessThan() < player.getMpPotionPercentage())
			forceUseItem(728); 
	}	

	private void forceUseItem(int itemId)
	{
		final ItemInstance potion = player.getInventory().getItemByItemId(itemId);
		if (potion == null)
			return; 

		final IItemHandler handler = ItemHandler.getInstance().getHandler(potion.getEtcItem());
		if (handler != null)
			handler.useItem(player, potion, false); 
	}	
}