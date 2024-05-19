package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WarriorBase.Warrior.WarriorCast3SkillsApproach;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.skills.L2Skill;

public class WarriorCast3SkillsApproachRevivalAggressive extends WarriorCast3SkillsApproachAggressive
{
	public WarriorCast3SkillsApproachRevivalAggressive()
	{
		super("ai/individual/Monster/WarriorBase/Warrior/WarriorCast3SkillsApproach");
	}
	
	public WarriorCast3SkillsApproachRevivalAggressive(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		20830,
		21067,
		21062,
		20831,
		21070,
		20858
	};
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		super.onAttacked(npc, attacker, damage, skill);
		
		final Creature topDesireTarget = npc.getAI().getTopDesireTarget();
		if (topDesireTarget instanceof Playable)
			npc._i_ai4 = topDesireTarget.getObjectId();
	}
	
	@Override
	public void onCreated(Npc npc)
	{
		npc._i_ai4 = 0;
		
		super.onCreated(npc);
	}
	
	@Override
	public void onMyDying(Npc npc, Creature killer)
	{
		if (npc._i_ai4 != -1)
			createOnePrivateEx(npc, getNpcIntAIParam(npc, "silhouette"), npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), 0, false, 1000, killer.getObjectId(), 0);
	}
}