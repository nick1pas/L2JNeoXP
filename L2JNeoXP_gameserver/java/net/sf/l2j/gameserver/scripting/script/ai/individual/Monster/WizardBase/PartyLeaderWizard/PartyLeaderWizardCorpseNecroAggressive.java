package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WizardBase.PartyLeaderWizard;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.container.attackable.HateList;
import net.sf.l2j.gameserver.skills.L2Skill;

public class PartyLeaderWizardCorpseNecroAggressive extends PartyLeaderWizardDD2
{
	public PartyLeaderWizardCorpseNecroAggressive()
	{
		super("ai/individual/Monster/WizardBase/PartyLeaderWizard");
	}
	
	public PartyLeaderWizardCorpseNecroAggressive(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		21596,
		21599
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		startQuestTimerAtFixedRate("3456", npc, null, 5000, 5000);
		
		super.onCreated(npc);
	}
	
	@Override
	public void onSeeCreature(Npc npc, Creature creature)
	{
		if (!(creature instanceof Playable))
		{
			super.onSeeCreature(npc, creature);
			return;
		}
		
		tryToAttack(npc, creature);
		
		if (creature.isDead())
		{
			if (npc.getAI().getCurrentIntention().getType() == IntentionType.ATTACK && Rnd.get(100) < 50 && npc.distance2D(creature) < 100)
			{
				final Creature mostHated = npc.getAI().getAggroList().getMostHatedCreature();
				if (mostHated != null)
				{
					createOnePrivateEx(npc, getNpcIntAIParam(npc, "SummonPrivate"), creature.getX(), creature.getY(), creature.getZ(), 0, 0, false, 1000, mostHated.getObjectId(), 0);
					
					final L2Skill clearCorpse = getNpcSkillByType(npc, NpcSkillType.CLEAR_CORPSE);
					npc.getAI().addCastDesire(creature, clearCorpse, 1000000);
				}
			}
		}
		super.onSeeCreature(npc, creature);
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("3456"))
		{
			npc.lookNeighbor();
		}
		return super.onTimer(name, npc, player);
	}
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		super.onAttacked(npc, attacker, damage, skill);
		
		if (attacker instanceof Playable)
		{
			final HateList hateList = npc.getAI().getHateList();
			
			final Creature mostHated = hateList.getMostHatedCreature();
			if (mostHated != null && npc._i_ai0 == 0)
			{
				final Creature topDesireTarget = npc.getAI().getTopDesireTarget();
				if (topDesireTarget == attacker)
				{
					if (Rnd.get(100) < 33 && npc.getStatus().getHpRatio() < 0.4)
					{
						final L2Skill DDMagic1 = getNpcSkillByType(npc, NpcSkillType.DD_MAGIC1);
						npc.getAI().addCastDesire(attacker, DDMagic1, 1000000);
					}
				}
			}
		}
		super.onAttacked(npc, attacker, damage, skill);
	}
}