package net.sf.l2j.gameserver.scripting.script.ai.boss.queenant;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.enums.skills.ElementType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.scripting.script.ai.individual.DefaultNpc;
import net.sf.l2j.gameserver.skills.L2Skill;

public class QueenAnt extends DefaultNpc
{
	public QueenAnt()
	{
		super("ai/boss/queenant");
	}
	
	public QueenAnt(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		29001
	};
	
	@Override
	public void onNoDesire(Npc npc)
	{
		npc.getAI().addDoNothingDesire(40, 5);
	}
	
	@Override
	public void onCreated(Npc npc)
	{
		if (Rnd.get(100) < 33)
			npc.getSpawn().instantTeleportInMyTerritory(-19480, 187344, -5600, 200);
		else if (Rnd.get(100) < 50)
			npc.getSpawn().instantTeleportInMyTerritory(-17928, 180912, -5520, 200);
		else
			npc.getSpawn().instantTeleportInMyTerritory(-23808, 182368, -5600, 200);
		
		npc.broadcastPacket(new PlaySound(1, "BS01_A", npc));
		npc._weightPoint = 10;
		
		createPrivates(npc);
		
		createOnePrivateEx(npc, 29002, -21600, 179482, -5832, Rnd.get(360), 0, false);
		
		startQuestTimerAtFixedRate("1001", npc, null, 10000, 10000);
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("1001"))
		{
			if (npc.getAI().getCurrentIntention().getType() == IntentionType.WANDER && Rnd.get(100) < 30)
				npc.getAI().addSocialDesire((Rnd.nextBoolean()) ? 3 : 4, (50 * 1000) / 30, 30);
		}
		return super.onTimer(name, npc, player);
	}
	
	@Override
	public void onPartyDied(Npc caller, Npc called)
	{
		if (caller != called)
		{
			if (caller.getNpcId() == 29003)
				caller.scheduleRespawn(10000);
			else
				caller.scheduleRespawn((280 + Rnd.get(40)) * 1000);
		}
	}
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (attacker.getStatus().getLevel() > (npc.getStatus().getLevel() + 8))
		{
			final L2Skill raidCurse = SkillTable.getInstance().getInfo(4515, 1);
			npc.getAI().addCastDesire(attacker, raidCurse, 1000000);
			
			npc.getAI().getAggroList().stopHate(attacker);
		}
		
		if (!npc.isInMyTerritory() || !npc.getSpawn().isInMyTerritory(attacker))
		{
			npc.teleportTo(-21610, 181594, -5734, 0);
			npc.removeAllAttackDesire();
		}
		
		if (attacker instanceof Playable)
		{
			final L2Skill decSpeed = SkillTable.getInstance().getInfo(4018, 1);
			final L2Skill striderSlow = SkillTable.getInstance().getInfo(4258, 1);
			final L2Skill decSpeedDOT = SkillTable.getInstance().getInfo(4019, 1);
			final L2Skill queenAntBrandish = SkillTable.getInstance().getInfo(4017, 1);
			
			if (attacker.isRiding() && getAbnormalLevel(attacker, striderSlow) <= 0)
				npc.getAI().addCastDesire(attacker, striderSlow, 1000000);
			
			if (skill != null && skill.getElement() == ElementType.FIRE && Rnd.get(100) < 70)
				npc.getAI().addCastDesire(attacker, decSpeed, 1000000);
			else
			{
				final double dist = npc.distance2D(attacker);
				if (dist > 500 && Rnd.get(100) < 10)
					npc.getAI().addCastDesireHold(attacker, decSpeedDOT, 1000000);
				else if (dist > 150 && Rnd.get(100) < 10)
				{
					if (Rnd.get(100) < 80)
						npc.getAI().addCastDesireHold(attacker, decSpeed, 1000000);
					else
						npc.getAI().addCastDesireHold(attacker, decSpeedDOT, 1000000);
				}
				else if (dist < 250 && Rnd.get(100) < 5)
					npc.getAI().addCastDesireHold(npc, queenAntBrandish, 1000000);
				else if (Rnd.get(100) < 1)
					npc.getAI().addSocialDesire(1, (60 * 1000) / 30, 3000000);
			}
			
			npc.getAI().addAttackDesire(attacker, (int) (((((((double) damage) / npc.getStatus().getMaxHp()) / 0.05) * damage) * npc._weightPoint) * 1000));
			
			final Creature topDesireTarget = npc.getAI().getTopDesireTarget();
			if (topDesireTarget != null && !npc.canAutoAttack(topDesireTarget))
				npc.getAI().getAggroList().stopHate(topDesireTarget);
		}
	}
	
	@Override
	public void onPartyAttacked(Npc caller, Npc called, Creature target, int damage)
	{
		final L2Skill decSpeed = SkillTable.getInstance().getInfo(4018, 1);
		final L2Skill decSpeedDOT = SkillTable.getInstance().getInfo(4019, 1);
		final L2Skill queenAntBrandish = SkillTable.getInstance().getInfo(4017, 1);
		
		if (target instanceof Playable && caller != called)
		{
			final double dist = called.distance2D(target);
			if (dist > 500 && Rnd.get(100) < 5)
				called.getAI().addCastDesireHold(target, decSpeedDOT, 1000000);
			else if (dist > 150 && Rnd.get(100) < 5)
			{
				if (Rnd.get(100) < 80)
					called.getAI().addCastDesireHold(target, decSpeed, 1000000);
				else
					called.getAI().addCastDesireHold(target, decSpeedDOT, 1000000);
			}
			else if (dist < 250 && Rnd.get(100) < 2)
				called.getAI().addCastDesireHold(called, queenAntBrandish, 1000000);
			
			called.getAI().addAttackDesire(target, (int) (((((((double) damage) / called.getStatus().getMaxHp()) / 0.05) * damage) * caller._weightPoint) * 1000));
		}
	}
	
	@Override
	public void onClanAttacked(Npc caller, Npc called, Creature attacker, int damage, L2Skill skill)
	{
		final L2Skill decSpeed = SkillTable.getInstance().getInfo(4018, 1);
		final L2Skill decSpeedDOT = SkillTable.getInstance().getInfo(4019, 1);
		final L2Skill queenAntBrandish = SkillTable.getInstance().getInfo(4017, 1);
		
		if (attacker instanceof Playable)
		{
			final double dist = called.distance2D(attacker);
			if (dist > 500 && Rnd.get(100) < 3)
				called.getAI().addCastDesireHold(attacker, decSpeedDOT, 1000000);
			else if (dist > 150 && Rnd.get(100) < 3)
			{
				if (Rnd.get(100) < 80)
					called.getAI().addCastDesireHold(attacker, decSpeed, 1000000);
				else
					called.getAI().addCastDesireHold(attacker, decSpeedDOT, 1000000);
			}
			else if (dist < 250 && Rnd.get(100) < 2)
				called.getAI().addCastDesireHold(called, queenAntBrandish, 1000000);
			
			called.getAI().addAttackDesireHold(attacker, (int) (((((double) damage) / called.getStatus().getMaxHp()) / 0.05) * 500));
		}
	}
	
	@Override
	public void onSeeSpell(Npc npc, Player caster, L2Skill skill, Creature[] targets, boolean isPet)
	{
		if (caster.getStatus().getLevel() > (npc.getStatus().getLevel() + 8))
		{
			final L2Skill raidMute = SkillTable.getInstance().getInfo(4215, 1);
			npc.getAI().addCastDesire(caster, raidMute, 1000000);
			
			return;
		}
		
		if (skill.getAggroPoints() > 0 && Rnd.get(100) < 15)
		{
			final L2Skill decSpeed = SkillTable.getInstance().getInfo(4018, 1);
			npc.getAI().addCastDesire(caster, decSpeed, 1000000);
		}
	}
	
	@Override
	public void onOutOfTerritory(Npc npc)
	{
		npc.teleportTo(-21610, 181594, -5734, 0);
		npc.removeAllAttackDesire();
	}
	
	@Override
	public void onMyDying(Npc npc, Creature killer)
	{
		npc.broadcastPacket(new PlaySound(1, "BS02_D", npc));
	}
}