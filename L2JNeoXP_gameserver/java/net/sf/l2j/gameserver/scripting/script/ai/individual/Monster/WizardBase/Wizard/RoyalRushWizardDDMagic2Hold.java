package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WizardBase.Wizard;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.skills.L2Skill;

public class RoyalRushWizardDDMagic2Hold extends Wizard
{
	public RoyalRushWizardDDMagic2Hold()
	{
		super("ai/individual/Monster/WizardBase/Wizard");
	}
	
	public RoyalRushWizardDDMagic2Hold(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		18137,
		18170,
		18191,
		18226
	};
	
	@Override
	public void onNoDesire(Npc npc)
	{
		npc.getAI().addMoveToDesire(npc.getSpawnLocation(), 30);
	}
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		super.onAttacked(npc, attacker, damage, skill);
		
		if (attacker instanceof Playable)
		{
			if (npc._i_ai0 == 0)
			{
				final boolean isNullHate = npc.getAI().getHateList().isEmpty();
				if (npc.distance2D(attacker) > 100 && Rnd.get(100) < 80)
				{
					if (!isNullHate || Rnd.get(100) < 2)
						npc.getAI().addCastDesire(attacker, getNpcSkillByType(npc, NpcSkillType.W_LONG_RANGE_DD_MAGIC), 1000000);
				}
				else if (!isNullHate || Rnd.get(100) < 2)
				{
					npc.getAI().addCastDesire(attacker, getNpcSkillByType(npc, NpcSkillType.W_SHORT_RANGE_DD_MAGIC), 1000000);
				}
			}
			else
			{
				double f0 = getHateRatio(npc, attacker);
				f0 = (((1.0 * damage) / (npc.getStatus().getLevel() + 7)) + ((f0 / 100) * ((1.0 * damage) / (npc.getStatus().getLevel() + 7))));
				npc.getAI().addAttackDesire(attacker, f0 * 100);
			}
		}
	}
	
	@Override
	public void onClanAttacked(Npc caller, Npc called, Creature attacker, int damage, L2Skill skill)
	{
		called.getAI().getHateList().refresh();
		if ((called.getAI().getLifeTime() > 7 && attacker instanceof Playable) && called.getAI().getHateList().isEmpty())
		{
			if (caller.distance2D(attacker) > 100)
				called.getAI().addCastDesire(attacker, getNpcSkillByType(called, NpcSkillType.W_LONG_RANGE_DD_MAGIC), 1000000);
			else
				called.getAI().addCastDesire(attacker, getNpcSkillByType(called, NpcSkillType.W_SHORT_RANGE_DD_MAGIC), 1000000);
		}
		
		super.onClanAttacked(caller, called, attacker, damage, skill);
	}
	
	@Override
	public void onUseSkillFinished(Npc npc, Creature creature, L2Skill skill, boolean success)
	{
		final Creature mostHated = npc.getAI().getHateList().getMostHatedCreature();
		if (mostHated != null && npc._i_ai0 != 1)
		{
			if (npc.distance2D(npc.getAI().getHateList().getMostHatedCreature()) > 100)
				npc.getAI().addCastDesire(mostHated, getNpcSkillByType(npc, NpcSkillType.W_LONG_RANGE_DD_MAGIC), 1000000);
			else
				npc.getAI().addCastDesire(mostHated, getNpcSkillByType(npc, NpcSkillType.W_SHORT_RANGE_DD_MAGIC), 1000000);
		}
	}
}
