package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WarriorBase.Warrior.WarriorCastHoldMagic;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.L2Skill;

public class OrfenPrivateRiba extends WarriorCastHoldMagic
{
	public OrfenPrivateRiba()
	{
		super("ai/individual/Monster/WarriorBase/Warrior/WarriorCastHoldMagicAggressive");
	}
	
	public OrfenPrivateRiba(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		29017 // riba
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		startQuestTimer("5001", npc, null, (90000 + Rnd.get(240000)));
		
		super.onCreated(npc);
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("5001"))
		{
			if (Rnd.get(100) < 66)
			{
				npc.getAI().getAggroList().randomizeAttack();
			}
			
			startQuestTimer("5001", npc, null, (90000 + Rnd.get(240000)));
		}
		
		return super.onTimer(name, npc, null);
	}
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (attacker.getStatus().getLevel() > (npc.getStatus().getLevel() + 8))
		{
			final L2Skill raidCurse = SkillTable.getInstance().getInfo(4515, 1);
			npc.getAI().addCastDesire(attacker, raidCurse, 1000000);
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
		super.onSeeSpell(npc, caster, skill, targets, isPet);
	}
}
