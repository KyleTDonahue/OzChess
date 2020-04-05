package ozplayschess;

import processing.core.PImage;

public class Emote {
	private String name;
	private PImage image;

	public Emote(String name, PImage image) {
		this.name = name;
		this.image = image;
	}
	
	public String getName() {
		return name;
	}
	
	public PImage getImage() {
		return image;
	}
	
}
