package net.KabOOm356.Reporter.Metrics;

import java.io.IOException;

import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.Metrics;
import org.mcstats.Metrics.Graph;

import net.KabOOm356.Database.DatabaseType;
import net.KabOOm356.Locale.Locale;
import net.KabOOm356.Locale.Entry.LocaleInfo;
import net.KabOOm356.Metrics.FeaturePlotter;
import net.KabOOm356.Reporter.Reporter;
import net.KabOOm356.Util.FormattingUtil;

/**
 * A class to initialize and start plugin metrics reporting.
 */
public class MetricsInitializer implements Runnable
{
	JavaPlugin plugin;
	private Locale locale;
	private DatabaseType databaseType;
	
	private Metrics metrics;
	
	public MetricsInitializer(Reporter plugin)
	{
		this.plugin = plugin;
		this.locale = plugin.getLocale();
		this.databaseType = plugin.getDatabaseHandler().getDatabaseType();
	}
	
	@Override
	public void run()
	{
		// If the locale has not been initialized, wait for a notification it has been initialized.
		synchronized(locale)
		{
			try
			{
				if(!locale.isInitialized())
				{
					locale.wait();
				}
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		
		try
		{
			// Initialize Metrics.
			metrics = new Metrics(plugin);
			
			// Create a graph to track the locale language being used.
			Graph localeGraph = metrics.createGraph("Locale");
			
			String language = locale.getString(LocaleInfo.language);
			language = FormattingUtil.capitalizeFirstCharacter(language);
			
			// Increment the language.
			localeGraph.addPlotter(new FeaturePlotter(language));
			
			// Create a graph to track the locale version being used.
			Graph localeVersionGraph = metrics.createGraph("Locale Version");
			
			String localeVersion = "Version " + locale.getString(LocaleInfo.version);
			
			// Increment the locale version.
			localeVersionGraph.addPlotter(new FeaturePlotter(localeVersion));
			
			// Create a graph to track the database engine being used.
			Graph databaseEngineGraph = metrics.createGraph("Database Engine");
			
			String databaseEngine = databaseType.toString();
			
			// Increment the database engine.
			databaseEngineGraph.addPlotter(new FeaturePlotter(databaseEngine));
			
			metrics.start();
		}
		catch (IOException e)
		{
			Reporter.getLog().warning(
					Reporter.getDefaultConsolePrefix() +
					"Could not enable statistics tracking with MCStats!");
			e.printStackTrace();
		}
	}
}
