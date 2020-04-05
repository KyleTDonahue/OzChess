package ozplayschess;

import processing.core.PImage;

public class namedImage {
	private String name;
	private PImage image;

	public namedImage(String name, PImage image) {
		this.name = name;
		this.image = image;
	}
	
	public String getName() {
		return name;
	}
	
	public PImage getImage() {
		return image;
	}
	
	public String toString() {
		return name;
	}
}
