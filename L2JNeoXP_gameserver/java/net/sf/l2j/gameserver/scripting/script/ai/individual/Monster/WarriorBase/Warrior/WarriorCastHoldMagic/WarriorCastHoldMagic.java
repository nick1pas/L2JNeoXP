package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WarriorBase.Warrior.WarriorCastHoldMagic;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.actors.NpcSkillType;
import net.sf.l2j.gameserver.enums.items.WeaponType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WarriorBase.Warrior.Warrior;
import net.sf.l2j.gameserver.skills.L2Skill;

public class WarriorCastHoldMagic extends Warrior
{
	public WarriorCastHoldMagic()
	{
		super("ai/individual/Monster/WarriorBase/Warrior/WarriorCastHoldMagic");
	}
	
	public WarriorCastHoldMagic(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		21628,
		27118,
		20663,
		20844,
		21626,
		21627
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		npc._i_ai0 = 0;
		
		super.onCreated(npc);
	}
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (attacker instanceof Playable)
		{
			final int i6 = Rnd.get(100);
			
			startQuestTimer("2001", npc, null, 12000);
			
			if (npc._i_ai0 == 0)
			{
				final double distance = npc.distance2D(attacker);
				if (distance > 300)
				{
					if (i6 < 50)
					{
						final L2Skill holdMagic = getNpcSkillByType(npc, NpcSkillType.HOLD_MAGIC);
						npc.getAI().addCastDesire(attacker, holdMagic, 1000000);
						
						npc._i_ai0 = 1;
					}
				}
				else if (distance > 100)
				{
					final Creature topDesireTarget = npc.getAI().getTopDesireTarget();
					if (topDesireTarget != null)
					{
						if ((topDesireTarget == attacker && i6 < 50) || i6 < 10)
						{
							final L2Skill holdMagic = getNpcSkillByType(npc, NpcSkillType.HOLD_MAGIC);
							npc.getAI().addCastDesire(attacker, holdMagic, 1000000);
							
							npc._i_ai0 = 1;
						}
					}
				}
				
				if (attacker.getAttackType() == WeaponType.BOW)
				{
					if (Rnd.get(100) < 50)
					{
						final L2Skill holdMagic = getNpcSkillByType(npc, NpcSkillType.HOLD_MAGIC);
						npc.getAI().addCastDesire(attacker, holdMagic, 1000000);
						
						npc._i_ai0 = 1;
					}
				}
			}
		}
		super.onAttacked(npc, attacker, damage, skill);
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("2001"))
		{
			final Creature c0 = npc.getLastAttacker();
			if (c0 != null && Rnd.get(100) < 80 && npc._i_ai0 == 0)
			{
				final L2Skill holdMagic = getNpcSkillByType(npc, NpcSkillType.HOLD_MAGIC);
				npc.getAI().addCastDesire(c0, holdMagic, 1000000);
				
				npc._i_ai0 = 1;
			}
		}
		
		return super.onTimer(name, npc, player);
	}
}