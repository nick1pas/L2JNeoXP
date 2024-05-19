package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WarriorBase.Warrior;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.L2Skill;

// Used for Frintezza raid only

public class WarriorTimerBomb extends Warrior
{
	public WarriorTimerBomb()
	{
		super("ai/individual/Monster/WarriorBase/Warrior");
	}
	
	public WarriorTimerBomb(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		18340,
		18341
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		npc._i_ai0 = 0;
		
		int i0 = Rnd.get(59);
		
		if (i0 < 10)
			i0 = (i0 + 10);
		
		startQuestTimer("1009", npc, null, i0 * 1000);
		
		int i1 = Rnd.get(9) + 1;
		
		startQuestTimer("1010", npc, null, i1 * 1000);
	}
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (npc.distance2D(attacker) < 200)
			npc.getAI().addCastDesire(npc, getNpcSkillByType(npc, NpcSkillType.SELF_RANGE_DD_MAGIC), 1000000);
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("1009"))
		{
			npc.getAI().addCastDesire(npc, getNpcSkillByType(npc, NpcSkillType.SELF_RANGE_DD_MAGIC), 1000000);
		}
		if (name.equalsIgnoreCase("1010"))
		{
			if (npc._i_ai0 == 0)
			{
				npc.setWalkOrRun(false);
				npc._i_ai0 = 1;
			}
			else
			{
				npc.setWalkOrRun(true);
				npc._i_ai0 = 0;
			}
			
			int i1 = Rnd.get(9);
			
			if (i1 <= 2)
			{
				i1 = (i1 + 3);
			}
			startQuestTimer("1010", npc, null, i1 * 1000);
		}
		
		return super.onTimer(name, npc, player);
	}
	
	@Override
	public void onUseSkillFinished(Npc npc, Creature creature, L2Skill skill, boolean success)
	{
		if (skill == getNpcSkillByType(npc, NpcSkillType.SELF_RANGE_DD_MAGIC) && success)
			npc.doDie(npc);
		
		super.onUseSkillFinished(npc, creature, skill, success);
	}
}