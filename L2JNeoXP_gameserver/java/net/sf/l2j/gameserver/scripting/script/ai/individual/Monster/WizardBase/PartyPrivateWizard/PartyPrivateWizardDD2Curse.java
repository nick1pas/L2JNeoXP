package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WizardBase.PartyPrivateWizard;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.container.attackable.HateList;
import net.sf.l2j.gameserver.skills.L2Skill;

public class PartyPrivateWizardDD2Curse extends PartyPrivateWizardDD2
{
	public PartyPrivateWizardDD2Curse()
	{
		super("ai/individual/Monster/WizardBase/PartyPrivateWizard");
	}
	
	public PartyPrivateWizardDD2Curse(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		20972
	};
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		super.onAttacked(npc, attacker, damage, skill);
		
		Creature mostHateHI = npc.getAI().getHateList().getMostHatedCreature();
		
		if (attacker instanceof Playable && npc._i_ai0 == 0)
		{
			int i0 = 0;
			if (mostHateHI != null)
			{
				i0 = 1;
			}
			
			L2Skill debuff = getNpcSkillByType(npc, NpcSkillType.DEBUFF);
			
			if (Rnd.get(100) < 33 && getAbnormalLevel(attacker, debuff) <= 0 && i0 == 1)
			{
				if (npc.getCast().meetsHpMpConditions(attacker, debuff))
				{
					npc.getAI().addCastDesire(attacker, debuff, 1000000);
				}
				else
				{
					npc._i_ai0 = 1;
					npc.getAI().addAttackDesire(attacker, 1000);
				}
			}
		}
		super.onAttacked(npc, attacker, damage, skill);
	}
	
	@Override
	public void onClanAttacked(Npc caller, Npc called, Creature attacker, int damage, L2Skill skill)
	{
		HateList hateList = called.getAI().getHateList();
		
		if ((called.getAI().getLifeTime() > 7 && attacker instanceof Playable) && hateList.isEmpty())
		{
			L2Skill debuff = getNpcSkillByType(called, NpcSkillType.DEBUFF);
			
			if (Rnd.get(100) < 33 && getAbnormalLevel(attacker, debuff) <= 0)
			{
				if (called.getCast().meetsHpMpConditions(attacker, debuff))
				{
					called.getAI().addCastDesire(attacker, debuff, 1000000);
				}
				else
				{
					called._i_ai0 = 1;
					called.getAI().addAttackDesire(attacker, 1000);
				}
			}
		}
		super.onClanAttacked(caller, called, attacker, damage, skill);
	}
}
