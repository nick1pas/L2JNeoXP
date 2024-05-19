package net.sf.l2j.gameserver.scripting.script.ai.individual;

import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.skills.L2Skill;

public class ImperialGravekeeper extends DefaultNpc
{
	public ImperialGravekeeper()
	{
		super("ai/individual");
	}
	
	public ImperialGravekeeper(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		27181
	};
	
	@Override
	public void onNoDesire(Npc npc)
	{
		npc.getAI().addWanderDesire(5, 5);
	}
	
	@Override
	public void onCreated(Npc npc)
	{
		npc._i_ai0 = 1;
		npc._i_ai1 = 50;
		npc._i_ai2 = 80;
		npc._weightPoint = 10;
	}
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (!npc.isInsideZone(ZoneId.PEACE))
		{
			if (npc.getStatus().getHpRatio() * 100 <= npc._i_ai1)
			{
				if (npc._i_ai0 == 1 || npc._i_ai0 == 3)
					npc.getSpawn().instantTeleportInMyTerritory(179520, 6464, -2706, 200);
				else
					npc.getSpawn().instantTeleportInMyTerritory(179520, 6464, -2706, 200);
			}
			npc.getAI().addCastDesire(npc, 4080, 1, 1000000);
			if (npc._i_ai1 == 50)
				npc._i_ai1 = 30;
			else if (npc._i_ai1 == 30)
				npc._i_ai1 = -1;
		}
		if (npc.getStatus().getHpRatio() * 100 <= npc._i_ai2)
		{
			if (npc._i_ai2 == 80)
				npc._i_ai2 = 40;
			else if (npc._i_ai2 == 40)
				npc._i_ai2 = 20;
			else
				npc._i_ai2 = -1;
			
			createOnePrivate(npc, 27180, 0, false);
			createOnePrivate(npc, 27180, 0, false);
			createOnePrivate(npc, 27180, 0, false);
			createOnePrivate(npc, 27180, 0, false);
		}
		if (npc.getStatus().getHpRatio() > 0.5)
			npc._i_ai1 = 50;
		else if (npc.getStatus().getHpRatio() > 0.3)
			npc._i_ai1 = 30;
		if (npc.getStatus().getHpRatio() > 0.8)
			npc._i_ai2 = 80;
		else if (npc.getStatus().getHpRatio() > 0.4)
			npc._i_ai2 = 40;
		else if (npc.getStatus().getHpRatio() > 0.2)
			npc._i_ai2 = 20;
		
		if (attacker instanceof Playable)
			npc.getAI().addAttackDesire(attacker, (((damage * 1.0) / npc.getStatus().getMaxHp()) / 0.05) * 100);
	}
	
	@Override
	public void onPartyAttacked(Npc caller, Npc called, Creature target, int damage)
	{
		if (target instanceof Playable)
			called.getAI().addAttackDesire(target, (((damage * 1.0) / called.getStatus().getMaxHp()) / 0.05) * 50);
	}
	
	@Override
	public void onClanAttacked(Npc caller, Npc called, Creature attacker, int damage, L2Skill skill)
	{
		if (attacker instanceof Playable)
			called.getAI().addAttackDesire(attacker, (((damage * 1.0) / called.getStatus().getMaxHp()) / 0.05) * 50);
	}
	
	@Override
	public void onOutOfTerritory(Npc npc)
	{
		npc.removeAllAttackDesire();
	}
}