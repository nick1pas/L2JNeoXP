package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.RaidBoss.RaidBossAlone.RaidBossType1;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.spawn.MultiSpawn;
import net.sf.l2j.gameserver.model.spawn.NpcMaker;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.taskmanager.GameTimeTaskManager;

public class RaidBossForTeleportDungeon extends RaidBossType1
{
	public RaidBossForTeleportDungeon()
	{
		super("ai/individual/Monster/RaidBoss/RaidBossAlone/RaidBossType1/RaidBossForTeleportDungeon");
	}
	
	public RaidBossForTeleportDungeon(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		25333,
		25334,
		25335,
		25336,
		25337,
		25338
	};
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (!npc.isInMyTerritory())
		{
			// myself::InstantRandomTeleportInMyTerritory()
			return;
		}
		
		npc._i_ai0 = GameTimeTaskManager.getInstance().getCurrentTick();
		
		super.onAttacked(npc, attacker, damage, skill);
	}
	
	@Override
	public void onCreated(Npc npc)
	{
		startQuestTimerAtFixedRate("2003", npc, null, 180000, 60000);
		
		final NpcMaker maker0 = ((MultiSpawn) npc.getSpawn()).getNpcMaker();
		maker0.getMaker().onMakerScriptEvent("2", maker0, 0, 0);
		
		npc._i_ai0 = GameTimeTaskManager.getInstance().getCurrentTick();
		
		super.onCreated(npc);
	}
	
	@Override
	public void onMyDying(Npc npc, Creature killer)
	{
		final NpcMaker maker0 = ((MultiSpawn) npc.getSpawn()).getNpcMaker();
		maker0.getMaker().onMakerScriptEvent("3", maker0, 0, 0);
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("2003"))
		{
			final NpcMaker maker0 = ((MultiSpawn) npc.getSpawn()).getNpcMaker();
			
			if (getElapsedTicks(npc._i_ai0) > (60 * 3))
				maker0.getMaker().onMakerScriptEvent("3", maker0, 0, 0);
			else
				maker0.getMaker().onMakerScriptEvent("2", maker0, 0, 0);
		}
		return super.onTimer(name, npc, player);
	}
}