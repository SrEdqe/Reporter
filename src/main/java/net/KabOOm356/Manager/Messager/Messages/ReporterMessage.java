package net.KabOOm356.Manager.Messager.Messages;

import java.util.ArrayList;

import org.bukkit.ChatColor;

import net.KabOOm356.Util.FormattingUtil;
import net.KabOOm356.Util.Util;

/**
 * A {@link Message} that has indexes associated with it.
 */
public class ReporterMessage extends Message
{
	/**
	 * The indexes associated with this message.
	 */
	private ArrayList<Integer> indexes;
	
	/**
	 * Constructor.
	 * 
	 * @param message The initial message.
	 */
	public ReporterMessage(String message)
	{
		super(message);
		
		indexes = new ArrayList<Integer>();
	}
	
	/**
	 * Constructor.
	 * 
	 * @param message An initial message.
	 */
	public ReporterMessage(Message message)
	{
		super(message.getMessage());
		
		indexes = new ArrayList<Integer>();
	}
	
	/**
	 * Constructor.
	 * 
	 * @param message The message.
	 * @param index An index associated with this message
	 */
	public ReporterMessage(String message, int index)
	{
		super(message);
		
		indexes = new ArrayList<Integer>();
		
		indexes.add(index);
	}
	
	/**
	 * Returns the raw message.
	 * <br />
	 * The raw message does not have the indexes appended to it.
	 * 
	 * @return The raw message.
	 */
	public String getRawMessage()
	{
		return super.getMessage();
	}
	
	@Override
	public String getMessage()
	{
		String message = super.getMessage();
		
		String indexString = Util.indexesToString(indexes, ChatColor.GOLD, ChatColor.WHITE);
		
		message = message.replaceAll("%i", indexString);
		
		return message;
	}
	
	/**
	 * Returns the indexes currently associated with this message.
	 * 
	 * @return The indexes currently associated with this message.
	 */
	public ArrayList<Integer> getIndexes()
	{
		return indexes;
	}
	
	/**
	 * Adds the given messages indexes to this message.
	 * 
	 * @param message The message to add the indexes from.
	 */
	public void addIndexes(ReporterMessage message)
	{
		if(messagesEqual(message))
			addIndexes(message.getIndexes());
	}
	
	/**
	 * Adds the given index to this message.
	 * 
	 * @param index The index to add.
	 */
	public void addIndex(int index)
	{
		if(!indexes.contains(index))
			indexes.add(index);
	}
	
	/**
	 * Adds the given indexes to this message.
	 * 
	 * @param indexes The indexes to add.
	 */
	public void addIndexes(ArrayList<Integer> indexes)
	{
		for(int index : indexes)
			addIndex(index);
	}
	
	/**
	 * Removes the given index and re-indexes the remaining indexes.
	 * 
	 * @param index The index to remove.
	 */
	public void removeIndex(int index)
	{
		int LCV = 0;
		
		while(LCV < indexes.size())
		{
			if (indexes.get(LCV) == index)
				indexes.remove(LCV);
			else if (indexes.get(LCV) > index)
			{
				indexes.set(LCV, indexes.get(LCV)-1);
				LCV++;
			}
			else
				LCV++;
		}
	}
	
	/**
	 * Re-indexes all the messages.
	 * 
	 * @param remainingIndexes The remaining indexes after a batch deletion.
	 */
	public void reindex(ArrayList<Integer> remainingIndexes)
	{
		int LCV = 0;
		
		while(LCV < indexes.size())
		{
			if (remainingIndexes.contains(indexes.get(LCV)))
			{
				indexes.set(LCV, remainingIndexes.indexOf(indexes.get(LCV))+1);
				LCV++;
			}
			else
				indexes.remove(LCV);
		}
	}
	
	/**
	 * Compares this message to the given message.  Messages are equal if their raw message string is equal.
	 * 
	 * @param message The message to compare this to.
	 * 
	 * @return True if this and the given message are equal, otherwise false.
	 */
	public boolean messagesEqual(ReporterMessage message)
	{
		return getRawMessage().equalsIgnoreCase(message.getRawMessage());
	}
	
	@Override
	public boolean isEmpty()
	{
		return indexes.isEmpty();
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append(super.toString());
		
		sb.append("\nIndexes: ");
		
		sb.append(Util.indexesToString(indexes) + "\n");
		
		sb.append("Full Message: " + this.getMessage());
		
		return FormattingUtil.addTabsToNewLines(sb.toString(), 1);
	}
}
