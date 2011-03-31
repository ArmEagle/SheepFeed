package nl.armeagle.minecraft.SheepFeed;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.bukkit.util.config.Configuration;

public class SheepFeedConfig {
	public static final String configFileName = "config.yml";
	public static final int defMinTicks = 10;
	public static final int defMaxTicks = 1000;
	public static final int defHealAmount = 1;
	
	SheepFeed sheepFeedPlugin;
	Configuration config;
	
	SheepFeedConfig(SheepFeed sheepFeedPlugin) {
		this.sheepFeedPlugin = sheepFeedPlugin;
		String configDir = "";
		File configFile = null;
		
		// make folder in the plugins dir
		try {
			configDir = "plugins" + File.separator + this.sheepFeedPlugin.getDescription().getName() + File.separator;
		} catch (Exception e) {
            e.printStackTrace();
        }
		new File(configDir).mkdirs();
		
		// create file handle for config file
		try {
			configFile = new File(configDir + SheepFeedConfig.configFileName);
		} catch (Exception e) {
            e.printStackTrace();
        }
		// if does not exist, copy from the jar
		if ( configFile != null && !configFile.exists() ) {
			SheepFeed.debug(this.sheepFeedPlugin.getDescription().getName() +": configfile "+ configFile.getPath() +" does not exist yet");
			InputStream input = this.getClass().getResourceAsStream("/" + SheepFeedConfig.configFileName);
			if ( input != null ) {
				FileOutputStream output = null;

	            try {
	                output = new FileOutputStream(configFile);
	                byte[] buf = new byte[8192];
	                int length = 0;
	                while ((length = input.read(buf)) > 0) {
	                    output.write(buf, 0, length);
	                }
	                SheepFeed.log.info(this.sheepFeedPlugin.getDescription().getName() + ": Default configuration file written: " + configFile.getPath());
	            } catch (IOException e) {
	                e.printStackTrace();
	            } finally {
	                try {
	                    if (input != null) {
	                        input.close();
	                    }
	                } catch (IOException e) {}

	                try {
	                    if (output != null) {
	                        output.close();
	                    }
	                } catch (IOException e) {}
	            }
			}
		}
		this.config = new Configuration(configFile);
		this.config.load();
	}
	
	/**
	 * Based on a materialID, return the data for a specific type of food from the configuration file
	 * @param materialID
	 * @return a SheepFoodData object containing the info we need, name, min/maxticks
	 */
	public SheepFoodData getFoodData(int materialID) {
		// this sucks, but at least it doesn't give any warnings
		String name = this.config.getString("sheepfood.id"+ materialID +".name");
		int minticks = this.config.getInt("sheepfood.id"+ materialID +".minticks", SheepFeedConfig.defMinTicks);
		int maxticks = this.config.getInt("sheepfood.id"+ materialID +".maxticks", SheepFeedConfig.defMaxTicks);
		int healamount = this.config.getInt("sheepfood.id"+ materialID +".healamount", SheepFeedConfig.defHealAmount);
		return new SheepFoodData(name, minticks, maxticks, healamount);
	}
	
	/**
	 * Based on a materialID, return true if this is configured to be food for sheep
	 * @param materialID
	 * @return
	 */
	public boolean isSheepFood(int materialID) {
		return this.config.getKeys("sheepfood").contains("id"+ materialID);
	}
	/**
	 * Return a list of food IDs
	 * @return
	 */
	public List<Integer> getFoodIDs() {
		List<String> foodIdStrings = this.config.getKeys("sheepfood");
		List<Integer> foodIDs = new ArrayList<Integer>();
		ListIterator<String> foodIdStrIter = foodIdStrings.listIterator();
		while ( foodIdStrIter.hasNext() ) {
			String foodElement = foodIdStrIter.next();
			foodIDs.add(Integer.parseInt(foodElement.replace("id", "")));
		}
		return foodIDs;
	}
}
