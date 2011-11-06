package nl.armeagle.minecraft.SheepFeed;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bukkit.DyeColor;

public class SheepFeedConfig {
	public static final String configFileName = "config.yml";
	public static final int defMinTicks = 10;
	public static final int defMaxTicks = 1000;
	public static final int defHealAmount = 1;
	
	SheepFeed sheepFeedPlugin;
	
	SheepFeedConfig(SheepFeed sheepFeedPlugin) {
		this.sheepFeedPlugin = sheepFeedPlugin;

		this.sheepFeedPlugin.getConfig().options().copyDefaults(true);
		this.sheepFeedPlugin.saveConfig();
	}
	
	/**
	 * Based on a materialID, return the data for a specific type of food from the configuration file
	 * @param materialID
	 * @return a SheepFoodData object containing the info we need, name, min/maxticks
	 */
	public SheepFoodData getFoodData(int materialID) {
		// this sucks, but at least it doesn't give any warnings
		String name = sheepFeedPlugin.getConfig().getString("sheepfood.id"+ materialID +".name");
		int minticks = sheepFeedPlugin.getConfig().getInt("sheepfood.id"+ materialID +".minticks", SheepFeedConfig.defMinTicks);
		int maxticks = sheepFeedPlugin.getConfig().getInt("sheepfood.id"+ materialID +".maxticks", SheepFeedConfig.defMaxTicks);
		int healamount = sheepFeedPlugin.getConfig().getInt("sheepfood.id"+ materialID +".healamount", SheepFeedConfig.defHealAmount);
		return new SheepFoodData(name, minticks, maxticks, healamount);
	}
	
	/**
	 * Based on a materialID, return true if this is configured to be food for sheep
	 * @param materialID
	 * @return
	 */
	public boolean isSheepFood(int materialID) {
		Map<String, Object> sheepFood = sheepFeedPlugin.getConfig().getConfigurationSection("sheepfood").getValues(true);
		return sheepFood.containsKey("id"+ materialID);
	}
	/**
	 * Return a list of food IDs
	 * @return
	 */
	public List<Integer> getFoodIDs() {
		Map<String, Object> foodIdStrings = sheepFeedPlugin.getConfig().getConfigurationSection("sheepfood").getValues(false);
		List<Integer> foodIDs = new ArrayList<Integer>();
		Iterator<String> foodIdStrIter = foodIdStrings.keySet().iterator();
		while ( foodIdStrIter.hasNext() ) {
			String foodElement = foodIdStrIter.next();
			foodIDs.add(Integer.parseInt(foodElement.replace("id", "")));
		}
		return foodIDs;
	}
	
	/**
	 * Return whether the given color is configured to be a color that is a valid (natural) color for wool to regrow. 
	 * @param dyeColor
	 * @return True if it is a valid color to be regrown.
	 */
	public synchronized boolean isRegrowColor(DyeColor dyeColor) {
		@SuppressWarnings("unchecked")
		List<String> regrowColors = sheepFeedPlugin.getConfig().getList("regrowcolors", null);
		if ( regrowColors == null || regrowColors.size() == 0 ) {
			SheepFeed.log("Config does not contain any regrowColor entries, while the plugin should have populated the list.");
			return false;
		}
		return regrowColors.contains(dyeColor.toString());
	}
}
