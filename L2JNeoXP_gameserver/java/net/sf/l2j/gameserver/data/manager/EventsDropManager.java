package net.sf.l2j.gameserver.data.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.zone.type.subtype.ZoneType;

public class EventsDropManager
{
	private boolean _haveActiveChristmasEvent = false;
	private boolean _haveActiveMedalsEvent = false;
	private boolean _haveActiveL2DayEvent = false;
	private boolean _haveActiveSquashEvent = false;
	
	private final Map<Integer, RewardRule> _rewardChristmasRules = new HashMap<>();
	private final Map<Integer, RewardRule> _rewardMedalsRules = new HashMap<>();
	private final Map<Integer, RewardRule> _rewardL2DayRules = new HashMap<>();
	private final Map<Integer, RewardRule> _rewardSquashRules = new HashMap<>();
	
	public static enum RuleType
	{
		ALL_NPC,
		BY_NPCID,
		BY_ZONE
	}
	
	public boolean haveActiveChristmasEvent()
	{
		return _haveActiveChristmasEvent;
	}
	
	public boolean haveActiveMedalsEvent()
	{
		return _haveActiveMedalsEvent;
	}
	
	public boolean haveActiveL2DayEvent()
	{
		return _haveActiveL2DayEvent;
	}
	
	public boolean haveActiveSquashEvent()
	{
		return _haveActiveSquashEvent;
	}
	
	public void addChristmasRule(String event, RuleType type, int[] itemId, int[] itemCnt, int[] itemChance)
	{
		addRule(_rewardChristmasRules, event, type, itemId, itemCnt, itemChance, true);
		_haveActiveChristmasEvent = true;
	}
	
	public void addMedalsRule(String event, RuleType type, int[] itemId, int[] itemCnt, int[] itemChance)
	{
		addRule(_rewardMedalsRules, event, type, itemId, itemCnt, itemChance, true);
		_haveActiveMedalsEvent = true;
	}
	
	public void addL2DayRule(String event, RuleType type, int[] itemId, int[] itemCnt, int[] itemChance)
	{
		addRule(_rewardL2DayRules, event, type, itemId, itemCnt, itemChance, true);
		_haveActiveL2DayEvent = true;
	}
	
	public void addSquashRule(String event, RuleType type, int[] itemId, int[] itemCnt, int[] itemChance)
	{
		addRule(_rewardSquashRules, event, type, itemId, itemCnt, itemChance, true);
		_haveActiveSquashEvent = true;
	}
	
	private void addRule(Map<Integer, RewardRule> rules, String event, RuleType type, int[] itemId, int[] itemCnt, int[] itemChance, boolean lvlControl)
	{
		RewardRule rule = new RewardRule(event, type, itemId, itemCnt, itemChance, lvlControl);
		rules.put(rules.size() + 1, rule);
	}
	
	public void removeChristmasRules(String event)
	{
		removeRules(_rewardChristmasRules, event);
		_haveActiveChristmasEvent = !_rewardChristmasRules.isEmpty();
	}
	
	public void removeMedalsRules(String event)
	{
		removeRules(_rewardMedalsRules, event);
		_haveActiveMedalsEvent = !_rewardMedalsRules.isEmpty();
	}
	
	public void removeL2DayRules(String event)
	{
		removeRules(_rewardL2DayRules, event);
		_haveActiveL2DayEvent = !_rewardL2DayRules.isEmpty();
	}
	
	public void removeSquashRules(String event)
	{
		removeRules(_rewardSquashRules, event);
		_haveActiveSquashEvent = !_rewardSquashRules.isEmpty();
	}
	
	private void removeRules(Map<Integer, RewardRule> rules, String event)
	{
		rules.values().removeIf(tmp -> tmp.eventName().equals(event));
	}
	
	private int[] calculateRewardItem(Map<Integer, RewardRule> rules, NpcTemplate npcTemplate, Creature lastAttacker)
	{
		int res[] =
		{
			0,
			0
		};
		int lvlDif = lastAttacker.getStatus().getLevel() - npcTemplate.getLevel();
		List<Rewards> rewardsList = new ArrayList<>();
		
		if (!rules.isEmpty())
		{
			for (RewardRule rule : rules.values())
			{
				if (rule.levDifferenceControl() && (lvlDif <= 7 && lvlDif >= -7))
					calculateRewards(rule, rewardsList);
			}
		}
		
		if (!rewardsList.isEmpty())
		{
			int rndRew = Rnd.get(rewardsList.size());
			res[0] = rewardsList.get(rndRew).rewardId();
			res[1] = rewardsList.get(rndRew).rewardCnt();
		}
		
		return res;
	}
	
	private void calculateRewards(RewardRule rule, List<Rewards> rewardsList)
	{
		if (rule.ruleType() == RuleType.ALL_NPC)
		{
			for (int cnt = 0; cnt < rule.rewardCnt(); cnt++)
			{
				if (rule.itemChance().get(cnt) >= Rnd.get(0, 100))
					rewardsList.add(new Rewards(rule.itemId().get(cnt), rule.itemCnt().get(cnt)));
			}
		}
	}
	
	public record RewardRule(int rewardCnt, String eventName, RuleType ruleType, boolean levDifferenceControl, List<Integer> mobId, ZoneType zone, List<Integer> itemId, List<Integer> itemCnt, List<Integer> itemChance)
	{
		public RewardRule(String eventName, RuleType ruleType, int[] itemId, int[] itemCnt, int[] itemChance, boolean levDifferenceControl)
		{
			this(itemId.length, eventName, ruleType, levDifferenceControl, new ArrayList<>(), null, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
			
			for (int id : itemId)
				this.itemId.add(id);
			for (int cnt : itemCnt)
				this.itemCnt.add(cnt);
			for (int chance : itemChance)
				this.itemChance.add(chance);
		}
	}
	
	public record Rewards(int rewardId, int rewardCnt)
	{
	}
	
	public int[] calculateChristmasRewardItem(NpcTemplate npcTemplate, Creature lastAttacker)
	{
		return calculateRewardItem(_rewardChristmasRules, npcTemplate, lastAttacker);
	}
	
	public int[] calculateMedalsRewardItem(NpcTemplate npcTemplate, Creature lastAttacker)
	{
		return calculateRewardItem(_rewardMedalsRules, npcTemplate, lastAttacker);
	}
	
	public int[] calculateL2DayRewardItem(NpcTemplate npcTemplate, Creature lastAttacker)
	{
		return calculateRewardItem(_rewardL2DayRules, npcTemplate, lastAttacker);
	}
	
	public int[] calculateSquashRewardItem(NpcTemplate npcTemplate, Creature lastAttacker)
	{
		return calculateRewardItem(_rewardSquashRules, npcTemplate, lastAttacker);
	}
	
	public static final EventsDropManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final EventsDropManager INSTANCE = new EventsDropManager();
	}
}