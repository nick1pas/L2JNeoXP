package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;

public class Online implements IVoicedCommandHandler
{
	private static final String[] _voicedCommands =
	{
		"online"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String target)
	{
		if (command.equals("online") && Config.ENABLE_ONLINE_COMMAND)
			player.sendMessage(player.getSysString(10_088, World.getInstance().getPlayers().size() * Config.MULTIPLIER_ONLINE_COMMAND));
		
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
}