package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WarriorBase.Warrior;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;

public class PartyLeaderSplitAggressive extends PartyLeaderSplit
{
	public PartyLeaderSplitAggressive()
	{
		super("ai/individual/Monster/WarriorBase/Warrior");
	}
	
	public PartyLeaderSplitAggressive(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		22094
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