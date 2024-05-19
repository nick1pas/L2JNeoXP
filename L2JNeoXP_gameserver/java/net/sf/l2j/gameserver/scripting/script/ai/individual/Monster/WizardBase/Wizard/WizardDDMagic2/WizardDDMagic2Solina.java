package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WizardBase.Wizard.WizardDDMagic2;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.NpcStringId;
import net.sf.l2j.gameserver.skills.L2Skill;

public class WizardDDMagic2Solina extends WizardDDMagic2Heal
{
	public WizardDDMagic2Solina()
	{
		super("ai/individual/Monster/WizardBase/Wizard/WizardDDMagic2");
	}
	
	public WizardDDMagic2Solina(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		22134
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		npc._i_ai3 = 0;
		npc._i_ai4 = 0;
		
		super.onCreated(npc);
	}
	
	@Override
	public void onNoDesire(Npc npc)
	{
		npc._i_ai3 = 0;
		npc._i_ai4 = 0;
		
		super.onNoDesire(npc);
	}
	
	@Override
	public void onSeeCreature(Npc npc, Creature creature)
	{
		if (creature instanceof Player)
		{
			if (checkOccupation(creature))
			{
				if (npc._i_ai3 == 0)
				{
					if (Rnd.get(100) < 50)
						npc.broadcastNpcSay(NpcStringId.ID_10075, creature.getName());
					else
						npc.broadcastNpcSay(NpcStringId.ID_10076, creature.getName());
					
					npc._i_ai3 = 1;
				}
			}
			else if (npc.distance2D(creature) > 100 && Rnd.get(100) < 80)
			{
				if (npc.getCast().meetsHpMpConditions(creature, getNpcSkillByType(npc, NpcSkillType.W_LONG_RANGE_DD_MAGIC)))
				{
					npc.getAI().addCastDesire(creature, getNpcSkillByType(npc, NpcSkillType.W_LONG_RANGE_DD_MAGIC), 1000000);
				}
				else
				{
					npc._i_ai0 = 1;
					npc.getAI().addAttackDesire(creature, 1000);
				}
			}
			else if (npc.getCast().meetsHpMpConditions(creature, getNpcSkillByType(npc, NpcSkillType.W_SHORT_RANGE_DD_MAGIC)))
			{
				npc.getAI().addCastDesire(creature, getNpcSkillByType(npc, NpcSkillType.W_SHORT_RANGE_DD_MAGIC), 1000000);
			}
			else
			{
				npc._i_ai0 = 1;
				npc.getAI().addAttackDesire(creature, 1000);
			}
			
			npc.getAI().getHateList().addDefaultHateInfo(creature);
		}
		
		super.onSeeCreature(npc, creature);
	}
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (attacker instanceof Player)
		{
			if (checkOccupation(attacker))
			{
				if (npc._i_ai4 == 0)
				{
					if (Rnd.get(100) < 50)
						npc.broadcastNpcSay(NpcStringId.ID_10077);
					else
						npc.broadcastNpcSay(NpcStringId.ID_10078);
					
					npc._i_ai4 = 1;
				}
			}
		}
		
		super.onAttacked(npc, attacker, damage, skill);
	}
	
	private static boolean checkOccupation(Creature creature)
	{
		if (creature instanceof Player)
		{
			final int classId = ((Player) creature).getClassId().getId();
			if (classId == 30 || classId == 16 || classId == 5 || classId == 90 || classId == 105 || classId == 97)
				return true;
		}
		
		return false;
	}
}