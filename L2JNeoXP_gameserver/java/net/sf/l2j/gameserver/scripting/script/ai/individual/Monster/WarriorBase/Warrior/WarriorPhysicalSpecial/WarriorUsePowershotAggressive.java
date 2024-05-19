package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WarriorBase.Warrior.WarriorPhysicalSpecial;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.L2Skill;

public class WarriorUsePowershotAggressive extends WarriorPhysicalSpecialAggressive
{
	public WarriorUsePowershotAggressive()
	{
		super("ai/individual/Monster/WarriorBase/Warrior/WarriorPhysicalSpecial");
	}
	
	public WarriorUsePowershotAggressive(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		20812,
		20660,
		21008,
		20826,
		21064,
		20623,
		21642
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		npc._i_ai2 = 0;
		
		super.onCreated(npc);
	}
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (npc._i_ai2 == 0 && npc.distance2D(attacker) < 100)
		{
			startQuestTimer("100002", npc, null, 2000);
			
			npc._i_ai2 = 1;
			npc._c_ai1 = attacker;
		}
		super.onAttacked(npc, attacker, damage, skill);
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("100002"))
		{
			npc.getAI().addFleeDesire(npc._c_ai1, Config.MAX_DRIFT_RANGE, 10000);
			
			npc._i_ai2 = 0;
		}
		return super.onTimer(name, npc, player);
	}
}