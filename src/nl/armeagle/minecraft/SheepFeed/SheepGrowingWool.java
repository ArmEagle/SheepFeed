package nl.armeagle.minecraft.SheepFeed;

import org.bukkit.DyeColor;
import org.bukkit.entity.Sheep;

public class SheepGrowingWool implements Runnable {
	SheepFeed sheepFeedPlugin;
	Sheep sheep;
	int healamount;
	
	SheepGrowingWool(SheepFeed sheepFeedPlugin, Sheep sheep, int healamount) {
		this.sheepFeedPlugin = sheepFeedPlugin;
		this.sheep = sheep;
		this.healamount = healamount;
	}
	
	@Override
	public void run() {
		// if sheep is somehow null (it died/is unloaded)?
		if ( sheep == null ) {
			SheepFeed.debug(" Sheep is null ");
			return;
		}
		// reset color if not natural
		if ( !this.sheepFeedPlugin.config.isRegrowColor(this.sheep.getColor()) ) {
			this.sheep.setColor(DyeColor.WHITE);
		}
		//DyeColor color = SheepGrowingWool.regrowthColor(this.sheep.getColor());
		
		
		// give back wool
		sheep.setSheared(false);
		// heal sheep based on type of food
		sheep.setHealth( Math.min(sheep.getHealth() + this.healamount, 20) );
		
		// remove sheep from growing list
		this.sheepFeedPlugin.removeWoolGrowingSheep(sheep);
		
		if ( SheepFeed.debug ) {
			SheepFeed.debug(" Sheep ("+ sheep.getEntityId() +") grew back its wool and healed (up to) "+ this.healamount);
		}
		this.sheep = null;
	}

	/**
	 * Convert color of a sheep to a color it could have grown, so no special dyes allowed.
	 * TODO? Store original color of a sheep?
	 * @param color
	 * @return A natural sheep color
	 */
	public static DyeColor regrowthColor(DyeColor color) {
		switch ( color ) {
		case WHITE:
		case BLACK:
		case GRAY:
		case SILVER:
			return color;
		default:
			return DyeColor.WHITE;
		}
	}
}
