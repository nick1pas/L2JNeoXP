package net.sf.l2j.gameserver.scripting.script.ai.individual.AgitWarrior.AgitWarriorFlee;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.L2Skill;

public class AgitThief extends AgitWarriorFlee
{
	private static final L2Skill NPC_HOLD = SkillTable.getInstance().getInfo(4047, 6);
	
	public AgitThief()
	{
		super("ai/individual/AgitWarrior/AgitWarriorFlee");
	}
	
	public AgitThief(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		35432,
		35622
	};
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (npc._i_ai1 == 1)
		{
			npc.removeAllAttackDesire();
			npc.getAI().addFleeDesire(attacker, Config.MAX_DRIFT_RANGE, 10000);
			
			if (Rnd.get(100) < 10)
				npc.getAI().addCastDesire(attacker, NPC_HOLD, 1000000);
		}
		else
		{
			final Player player = attacker.getActingPlayer();
			if (player != null && (player.getClanId() != npc.getClanId() || player.getClanId() == 0))
			{
				final double hpRatio = npc.getStatus().getHp() / npc.getStatus().getMaxHp();
				if (hpRatio > 0.5)
					npc.getAI().addAttackDesire(attacker, (int) (((((double) damage) / npc.getStatus().getMaxHp()) / 0.05) * (attacker instanceof Player ? 100 : 10)));
				else if (hpRatio > 0.3)
				{
					if (Rnd.get(100) < 90)
						npc.getAI().addAttackDesire(attacker, (int) (((((double) damage) / npc.getStatus().getMaxHp()) / 0.05) * (attacker instanceof Player ? 100 : 10)));
					else
					{
						npc._i_ai1 = 1;
						npc.removeAllAttackDesire();
						
						startQuestTimer("3001", npc, player, 5000);
					}
				}
				else
				{
					npc._i_ai1 = 1;
					npc.removeAllAttackDesire();
				}
			}
		}
	}
}