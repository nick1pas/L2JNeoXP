package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WarriorBase.Warrior.WarriorCastSummonPC;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;

public class WarriorCastSummonPCAggressive extends WarriorCastSummonPC
{
	public WarriorCastSummonPCAggressive()
	{
		super("ai/individual/Monster/WarriorBase/Warrior/WarriorCastSummonPC");
	}
	
	public WarriorCastSummonPCAggressive(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		20221
	};
	
	@Override
	public void onSeeCreature(Npc npc, Creature creature)
	{
		if (!(creature instanceof Playable))
			return;
		
		tryToAttack(npc, creature);
		
		super.onSeeCreature(npc, creature);
	}
}