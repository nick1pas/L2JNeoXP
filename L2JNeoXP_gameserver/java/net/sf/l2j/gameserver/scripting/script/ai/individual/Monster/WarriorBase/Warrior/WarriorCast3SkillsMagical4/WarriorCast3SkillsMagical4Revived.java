package net.sf.l2j.gameserver.scripting.script.ai.individual.Monster.WarriorBase.Warrior.WarriorCast3SkillsMagical4;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.skills.L2Skill;

public class WarriorCast3SkillsMagical4Revived extends WarriorCast3SkillsMagical4
{
	public WarriorCast3SkillsMagical4Revived()
	{
		super("ai/individual/Monster/WarriorBase/Warrior/WarriorCast3SkillsMagical4");
	}
	
	public WarriorCast3SkillsMagical4Revived(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		21206
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		if (npc._param1 == 1000)
		{
			final Creature c0 = (Creature) World.getInstance().getObject(npc._param2);
			if (c0 != null)
			{
				final L2Skill npcHate = SkillTable.getInstance().getInfo(4663, 1);
				
				npc.getAI().addCastDesire(c0, npcHate, 10000);
				npc.getAI().addAttackDesire(c0, 500);
			}
		}
		
		super.onCreated(npc);
	}
}