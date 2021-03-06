package nl.armeagle.minecraft.SheepFeed;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;
import org.bukkit.plugin.Plugin;

/**
 * 
 * @author ArmEagle
 * The goal is to have sheep grow back their wool when fed a certain item.
 * For now this will have to be done by having a player hit a sheep with some food.
 * 
 * But since you cannot yet determine whether a sheep has its wool still (right?) or
 * give it back, we'll have to keep track of the sheep that were hit by a player
 * (which makes them lose their wool) since server start (plugin enabled). And if
 * a player hits one of these sheep (again), to remove that sheep and spawn a new one  
 */

// TODO choose whether to make the listener or this main class heavy, not both

public class SheepFeed extends JavaPlugin {
	public static final boolean debug = false;
	private static final Logger log = Logger.getLogger("Minecraft");
	
	//	private final SheepFeedPlayerListener playerListener = new SheepFeedPlayerListener(this);
	private SheepFeedEntityListener entityListener;
	protected SheepFeedConfig config;
	private boolean hasRegisteredEventListeners = false;
	public BukkitScheduler scheduler;
	
	// TODO, cancel wool grow task if sheep is gone:
	private ConcurrentHashMap<Sheep, Integer> woolGrowingSheep = new ConcurrentHashMap<Sheep, Integer>();
	
	private PermissionHandler permissionHandler;
	
	@Override
	public void onDisable() {
		PluginDescriptionFile pdfFile = this.getDescription();
        log.info( pdfFile.getName() + " version " + pdfFile.getVersion() + " is disabled." );

        // cancel all tasks (and clear list)
        Collection<Integer> woolGrowingTaskIDs = this.woolGrowingSheep.values();
        Iterator<Integer> wGTIDitr = woolGrowingTaskIDs.iterator();
        while ( wGTIDitr.hasNext() ) {
        	// remove task
        	this.scheduler.cancelTask(wGTIDitr.next());
        	wGTIDitr.remove();
        }
        SheepFeed.debug("growing sheep left: "+ this.woolGrowingSheep.size() +", should not be > 0");
        
        // clear configuration, saving memory
        this.config = null;
	}

	@Override
	public void onEnable() {		
		PluginDescriptionFile pdfFile = this.getDescription();

        // load configuration, this way dis&enabling the plugin will read changes in the config file
		this.config = new SheepFeedConfig(this);
		
        // only register event listeners once
        //TODO will need to unregister the listeners when that feature is added 
        if ( !this.hasRegisteredEventListeners ) {
	        PluginManager pm = this.getServer().getPluginManager();
	        // register entities receiving damage and dieing
	        pm.registerEvents(this.entityListener, this);
	        this.hasRegisteredEventListeners = true;
	        
	        this.setupPermissions();
        }
        log.info( pdfFile.getName() + " version " + pdfFile.getVersion() + " by "+ pdfFile.getAuthors().get(0) +" is enabled!" );
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command,	String label, String[] args) {
		if ( this.isEnabled() ) {
			if ( command.getName().equals("sheepfeed") ) {
				// check for permission
				if ( !this.canInfo(sender) ) {
					sender.sendMessage("You are not allowed to use this command.");
					return true;
				}
				// show naked/regrowing sheep counts
				sender.sendMessage("There are "+ this.woolGrowingSheep.size() +" sheep regrowing their coat.");
				// show food info
				sender.sendMessage("Valid food is: ");
				List<Integer> foodIDs = this.config.getFoodIDs();
				Iterator<Integer> foodIDsIterator = foodIDs.iterator();
				while ( foodIDsIterator.hasNext() ) {
					Integer foodID = foodIDsIterator.next();
					SheepFoodData foodData = this.config.getFoodData(foodID);
					sender.sendMessage(foodData.name +" ("+ foodID +") minticks: "+ foodData.minticks + " maxticks: "+ foodData.maxticks +" healing: "+ foodData.healamount);
				}
				return true;
			}
		}
		return super.onCommand(sender, command, label, args);
	}
	
	@Override
	public void onLoad() {
		this.scheduler = this.getServer().getScheduler();
		this.entityListener = new SheepFeedEntityListener(this);
	}
	/**
	 * To check whether a sheep is currently growing wool
	 * @param sheep
	 * @return true if the sheep is growing wool
	 */
	public boolean isWoolGrowingSheep(Sheep sheep) {
		return this.woolGrowingSheep.containsKey(sheep);
	}
	/**
	 * Remove the sheep from the growing list
	 * @param sheep
	 */
	public void removeWoolGrowingSheep(Sheep sheep) {
		this.woolGrowingSheep.remove(sheep);
	}
	/**
	 * Used to handle a sheep dying. To remove from naked/wool growing list and
	 *  cancel task if the latter.
	 * @param sheep
	 */
	public void sheepDied(Sheep sheep) {
		SheepFeed.debug("Sheep ("+ sheep.getEntityId() +") died, removing from list (and schedule)");
		if ( this.isWoolGrowingSheep(sheep) ) {
			// cancel task
			this.scheduler.cancelTask(this.woolGrowingSheep.get(sheep));
			// then remove the sheep
			this.removeWoolGrowingSheep(sheep);
		}
	}
		
	public void scheduleWoolGrowth(Sheep sheep, SheepFoodData foodData) {
		// create new wool growing task
		SheepGrowingWool woolGrowTask = new SheepGrowingWool(this, sheep, foodData.healamount);
		// use the min/maxticks from the fooData object to randomise the regrowth time
		int regrowthTime = foodData.minticks + (int)Math.round(((double)(foodData.maxticks-foodData.minticks))*Math.random());
		// schedule the wool growth
		int taskID = this.scheduler.scheduleSyncDelayedTask(this, woolGrowTask, regrowthTime);
		SheepFeed.debug("Scheduling wool growth in "+ regrowthTime +" ticks for sheep ("+ sheep.getEntityId() +")");
		//  add to woolGrowing
		this.woolGrowingSheep.put(sheep, taskID);
	}
	
	private void setupPermissions() {
		Plugin permissionsPlugin = this.getServer().getPluginManager().getPlugin("Permissions");

		if (this.permissionHandler == null) {
			if (permissionsPlugin != null) {
				this.permissionHandler = ((Permissions) permissionsPlugin).getHandler();
				SheepFeed.log("Permissions plugin detected.");
			} else {
				SheepFeed.log("Permissions plugin not detected, defaulting to Bukkit's built-in system.");
			}
		}
	}
	public boolean canFeed(CommandSender sender) {
		if ( sender instanceof Player ) {
			return this.hasPermission((Player)sender, "SheepFeed.feed");
		}
		return false; // can not feed from console
	}
	public boolean canInfo(CommandSender sender) {
		if ( sender instanceof Player ) {
			return this.hasPermission((Player)sender, "SheepFeed.info");
		}
		return true; // can info from console 
	}
	private boolean hasPermission(Player player, String node) {
		if ( this.permissionHandler != null ) {
			return this.permissionHandler.has(player, node);
		} else {
			return player.hasPermission(node);
		}
	}
	
	public static void debug(String string) {
		if ( SheepFeed.debug ) {
			log.info("SheepFeed DEBUG: "+ string);
		}
	}
	
	public static void log(String string) {
		log.info("Sheepfeed: "+ string);
	}
}
