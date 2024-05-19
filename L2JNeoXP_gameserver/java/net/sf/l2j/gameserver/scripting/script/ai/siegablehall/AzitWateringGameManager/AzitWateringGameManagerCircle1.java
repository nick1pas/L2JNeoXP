package net.sf.l2j.gameserver.scripting.script.ai.siegablehall.AzitWateringGameManager;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;

public class AzitWateringGameManagerCircle1 extends AzitWateringGameManager
{
	public AzitWateringGameManagerCircle1()
	{
		super("ai/siegeablehall/AzitWateringGameManager");
		
		GAME_MASTER = 1;
		NUMBER_OF_CIRCLE = 1;
		AREA_DATA_DEBUFF = "rainbow_slow_1";
		
		addFirstTalkId(_npcIds);
		addTalkId(_npcIds);
	}
	
	public AzitWateringGameManagerCircle1(String descr)
	{
		super(descr);
		
		GAME_MASTER = 1;
		NUMBER_OF_CIRCLE = 1;
		AREA_DATA_DEBUFF = "rainbow_slow_1";
		
		addFirstTalkId(_npcIds);
		addTalkId(_npcIds);
	}
	
	protected final int[] _npcIds =
	{
		35596
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		npc._i_ai3 = 0;
		
		super.onCreated(npc);
	}
	
	@Override
	public void onSeeCreature(Npc npc, Creature creature)
	{
		if ((creature instanceof Npc) && npc._i_ai3 == 0)
		{
			final int npcId = ((Npc) creature).getNpcId();
			if (npcId >= 35588 && npcId <= 35591)
			{
				npc._i_ai3 = creature.getObjectId();
				broadcastScriptEvent(npc, 10002, npc._i_ai3, 8000);
			}
		}
		
		super.onSeeCreature(npc, creature);
	}
}