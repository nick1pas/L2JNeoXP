package net.sf.l2j.commons.mmocore;

import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.records.EffectHolder;

public abstract class SendablePacket<T extends MMOClient<?>> extends AbstractPacket<T>
{
	protected abstract void write();
	
	protected final void writeC(final int data)
	{
		_buf.put((byte) data);
	}
	
	protected final void writeF(final double value)
	{
		_buf.putDouble(value);
	}
	
	protected final void writeH(final int value)
	{
		_buf.putShort((short) value);
	}
	
	protected final void writeD(final int value)
	{
		_buf.putInt(value);
	}
	
	protected final void writeQ(final long value)
	{
		_buf.putLong(value);
	}
	
	protected final void writeB(final byte[] data)
	{
		_buf.put(data);
	}
	
	protected final void writeS(final String text)
	{
		if (text != null)
		{
			final int len = text.length();
			for (int i = 0; i < len; i++)
			{
				_buf.putChar(text.charAt(i));
			}
		}
		
		_buf.putChar('\000');
	}
	
	protected final void writeLoc(final Location loc)
	{
		writeD(loc.getX());
		writeD(loc.getY());
		writeD(loc.getZ());
	}
	
	protected void writeEffect(EffectHolder effect, boolean toggle)
	{
		writeD(effect.id());
		writeH(effect.level());
		writeD((toggle) ? -1 : effect.duration() / 1000);
	}
}