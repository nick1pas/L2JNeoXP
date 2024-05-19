package net.sf.l2j.gameserver.scripting.script.ai.siegablehall.AzitWateringGameManager;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;

public class AzitWateringGameManagerCircle3 extends AzitWateringGameManagerCircle1
{
	public AzitWateringGameManagerCircle3()
	{
		super("ai/siegeablehall/AzitWateringGameManager");
		
		NUMBER_OF_CIRCLE = 3;
		AREA_DATA_DEBUFF = "rainbow_slow_3";
		
		addFirstTalkId(_npcIds);
		addTalkId(_npcIds);
	}
	
	public AzitWateringGameManagerCircle3(String descr)
	{
		super(descr);
		
		NUMBER_OF_CIRCLE = 3;
		AREA_DATA_DEBUFF = "rainbow_slow_3";
		
		addFirstTalkId(_npcIds);
		addTalkId(_npcIds);
	}
	
	protected final int[] _npcIds =
	{
		35598
	};
	
	@Override
	public void onSeeCreature(Npc npc, Creature creature)
	{
		if ((creature instanceof Npc) && npc._i_ai3 == 0)
		{
			final int npcId = ((Npc) creature).getNpcId();
			if (npcId >= 35588 && npcId <= 35591)
			{
				npc._i_ai3 = creature.getObjectId();
				broadcastScriptEvent(npc, 10004, npc._i_ai3, 8000);
			}
		}
		
		super.onSeeCreature(npc, creature);
	}
}