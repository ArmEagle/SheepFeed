package nl.armeagle.minecraft.SheepFeed;

public class SheepFoodData {
	public String name;
	public int minticks;
	public int maxticks;
	public int healamount;
	
	SheepFoodData(String name, int minticks, int maxticks, int healamount) {
		this.name = name;
		this.minticks = minticks;
		this.maxticks = maxticks;
		this.healamount = healamount;
	}
	
	public String toString() {
		return "SheepFoodData name: "+ this.name +" minticks: "+ this.minticks +" maxticks: "+ this.maxticks +" healamount: "+ this.healamount;
	}
}
