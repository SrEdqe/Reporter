package net.KabOOm356.Command.Commands;

import java.sql.SQLException;
import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import net.KabOOm356.Command.ReporterCommand;
import net.KabOOm356.Command.ReporterCommandManager;
import net.KabOOm356.Database.SQLResultSet;
import net.KabOOm356.Locale.Entry.LocalePhrases.DowngradePhrases;
import net.KabOOm356.Permission.ModLevel;
import net.KabOOm356.Util.Util;

/**
 * A {@link ReporterCommand} that will handle downgrading a report's priority.
 */
public class DowngradeCommand extends ReporterCommand
{
	private static final String name = "Downgrade";
	private static int minimumNumberOfArguments = 1;
	private final static String permissionNode = "reporter.move";
	
	/**
	 * Constructor.
	 * 
	 * @param manager The {@link ReporterCommandManager} managing this Command.
	 */
	public DowngradeCommand(ReporterCommandManager manager)
	{
		super(manager, name, permissionNode, minimumNumberOfArguments);
		
		updateDocumentation();
	}
	
	@Override
	public void execute(CommandSender sender, ArrayList<String> args)
	{
		if(!hasRequiredPermission(sender))
			return;
		
		int index = Util.parseInt(args.get(0));
		
		if(args.get(0).equalsIgnoreCase("last"))
		{
			if(!hasRequiredLastViewed(sender))
				return;
			
			index = getLastViewed(sender);
		}
		
		if(!getManager().isReportIndexValid(sender, index))
			return;
		
		if(!getManager().canAlterReport(sender, index))
			return;
		
		ModLevel newPriority = getNextLowestPriorityLevel(index);
		
		if(newPriority == ModLevel.UNKNOWN)
		{
			String output = getManager().getLocale().getString(DowngradePhrases.reportIsAtLowestPriority);
			
			output = output.replaceAll("%i", ChatColor.GOLD + Integer.toString(index) + ChatColor.RED);
			
			sender.sendMessage(ChatColor.RED + output);
			
			return;
		}
		
		// Get MoveCommand and let it take care of moving the report to the new priority.
		MoveCommand move = (MoveCommand) getManager().getCommand("Move");
		
		move.moveReport(sender, index, newPriority);
	}
	
	private ModLevel getNextLowestPriorityLevel(int index)
	{
		String query = "SELECT Priority FROM Reports WHERE ID=" + index;
		
		try
		{
			SQLResultSet result = getManager().getDatabaseHandler().sqlQuery(query);
			
			int currentPriorityLevel = result.getInt("Priority");
			
			return ModLevel.getByLevel(currentPriorityLevel-1);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				getManager().getDatabaseHandler().closeConnection();
			}
			catch (SQLException e)
			{
			}
		}
		
		return ModLevel.UNKNOWN;
	}
	
	@Override
	public void updateDocumentation()
	{
		super.updateDocumentation(
				getManager().getLocale().getString(DowngradePhrases.downgradeHelp),
				getManager().getLocale().getString(DowngradePhrases.downgradeHelpDetails));
	}
	
	/**
	 * Returns the name of this command.
	 * 
	 * @return The name of this command.
	 */
	public static String getCommandName()
	{
		return name;
	}
	
	/**
	 * Returns the permission node of this command.
	 * 
	 * @return The permission node of this command.
	 */
	public static String getCommandPermissionNode()
	{
		return permissionNode;
	}
}
