package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WizardBase.Wizard.WizardDDMagic2.WizardCorpseVampireBasic;

import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.skills.L2Skill;

public class WizardCorpseVampireCurseAggressive extends WizardCorpseVampireBasicAggressive
{
	public WizardCorpseVampireCurseAggressive()
	{
		super("ai/individual/Monster/WizardBase/Wizard/WizardDDMagic2/WizardCorpseVampireBasic");
	}
	
	public WizardCorpseVampireCurseAggressive(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		21585
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		npc._i_ai4 = 0;
		
		super.onCreated(npc);
	}
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (attacker instanceof Playable)
		{
			if (npc._i_ai4 == 0)
			{
				final L2Skill debuff1 = getNpcSkillByType(npc, NpcSkillType.DEBUFF1);
				npc.getAI().addCastDesire(attacker, debuff1, 1000000);
				
				npc._i_ai4 = 1;
				npc._c_ai0 = attacker;
			}
			
			if (npc._i_ai4 == 1 && npc._c_ai0 != null && npc.getStatus().getHpRatio() < 0.1 && npc.distance2D(npc._c_ai0) < 100)
			{
				final L2Skill cancel = getNpcSkillByType(npc, NpcSkillType.CANCEL);
				npc.getAI().addCastDesire(npc._c_ai0, cancel, 1000000);
				
				npc._i_ai4 = 2;
			}
		}
		
		super.onAttacked(npc, attacker, damage, skill);
	}
}