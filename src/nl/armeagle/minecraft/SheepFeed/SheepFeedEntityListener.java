package nl.armeagle.minecraft.SheepFeed;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.inventory.ItemStack;

public class SheepFeedEntityListener extends EntityListener{

	private SheepFeed sheepFeedPlugin;
	
	public SheepFeedEntityListener(SheepFeed sheepFeedPlugin) {
		this.sheepFeedPlugin = sheepFeedPlugin;
	}

	/**
	 * Handler for entity receiving damage
	 * Filter out specific case of a player hitting a sheep. Then check the item the
	 * player is using for it being suitable food.
	 * 
	 * @param event EntityDamageEvent
	 */
	public void onEntityDamage(EntityDamageEvent event) {
		// do not act when disabled  TODO, use unregister when available
		if ( !this.sheepFeedPlugin.isEnabled() ) {
			return;
		}

		// see whether this is an attack event		
		if ( event.getCause() != DamageCause.ENTITY_ATTACK ) {
			return;
		}
		
		EntityDamageByEntityEvent entDmByEntEv = (EntityDamageByEntityEvent)event; 
		Entity attacker = entDmByEntEv.getDamager();
		Entity target = entDmByEntEv.getEntity();
		
		// attacker needs to be a player and target a sheep
		if ( attacker instanceof Player && target instanceof Sheep ) {
			Player player = (Player) attacker;
			Sheep sheep = (Sheep) target;
			SheepFeed.debug("Hitting sheep ("+ sheep.getEntityId() +") that had health "+ sheep.getHealth() +" original damage: "+ event.getDamage());

			/* Support for sheared state (setting), using that now.
			 * If the sheep is sheared, then add to growing (if holding a food item and the sheep isn't growing already. Remove from naked list.
			 * If not sheared, then add to naked list.
			 */ 
			if ( sheep.isSheared() ) {
				// Sheep is already sheered, adding to growing list if holding a food item and not growing already
				SheepFeed.debug("Sheep is already sheered");
				if ( sheepFeedPlugin.isWoolGrowingSheep(sheep) ) {
					SheepFeed.debug("Sheep is already registered to grow wool");
					return;
				}
				// check whether player has the right item in hand
				ItemStack heldItemStack = player.getItemInHand();
				if ( !sheepFeedPlugin.config.isSheepFood(heldItemStack.getTypeId()) ) {
					// That's not sheep food! 
					SheepFeed.debug("Sheep would not like that food");
					return;
				}
				// not already scheduled to grow and correct food, remove one item and set growing schedule
				SheepFoodData foodData = sheepFeedPlugin.config.getFoodData(heldItemStack.getTypeId());
				SheepFeed.debug(foodData.toString());
				
				// remove the food item
				SheepFeed.debug(" amount of "+ heldItemStack.getType().toString() +" held: "+ heldItemStack.getAmount());
				player.sendMessage("The sheep munches on the "+ foodData.name +" you fed it.");
				// reduce stack by one, but if it's the last item, remove the whole stack
				if ( heldItemStack.getAmount() == 1 ) {
					player.getInventory().clear(player.getInventory().getHeldItemSlot());
				} else {
					heldItemStack.setAmount(heldItemStack.getAmount()-1);
				}
				
				// schedule growing of wool
				this.sheepFeedPlugin.scheduleWoolGrowth(sheep, foodData);
				
				// cancel the damage given
				event.setDamage(0);
			}
		}
	}
	
	/**
	 * If the dieing entity is a sheep that we kept track of, remove that from the list.
	 * 
	 * @param event EntityDeathEvent	
	 */
	public void onEntityDeath(EntityDeathEvent event) {
		// do not act when disabled  TODO, use unregister when available
		if ( !this.sheepFeedPlugin.isEnabled() ) {
			return;
		}

		Entity entity = event.getEntity();
		if ( entity instanceof Sheep ) {
			this.sheepFeedPlugin.sheepDied((Sheep) entity);
		}
	}

}
