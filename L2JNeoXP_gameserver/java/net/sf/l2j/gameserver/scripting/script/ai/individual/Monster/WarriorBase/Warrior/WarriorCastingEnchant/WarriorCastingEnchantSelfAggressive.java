package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WarriorBase.Warrior.WarriorCastingEnchant;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.skills.L2Skill;

public class WarriorCastingEnchantSelfAggressive extends WarriorCastingEnchant
{
	public WarriorCastingEnchantSelfAggressive()
	{
		super("ai/individual/Monster/WarriorBase/Warrior/WarriorCastingEnchant");
	}
	
	public WarriorCastingEnchantSelfAggressive(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		20260,
		20424,
		20088,
		20620,
		20624,
		20642,
		20234,
		20098
	};
	
	@Override
	public void onSeeCreature(Npc npc, Creature creature)
	{
		if (npc.getAI().getLifeTime() > 7 && npc.isInMyTerritory())
		{
			if (npc._i_ai1 == 0 && Rnd.get(100) < 33 && npc.getStatus().getHpRatio() > 0.5)
			{
				final L2Skill buff = getNpcSkillByType(npc, NpcSkillType.BUFF);
				npc.getAI().addCastDesire(npc, buff, 1000000);
			}
			npc._i_ai1 = 1;
		}
		
		if (!(creature instanceof Playable))
			return;
		
		tryToAttack(npc, creature);
		
		super.onSeeCreature(npc, creature);
	}
}