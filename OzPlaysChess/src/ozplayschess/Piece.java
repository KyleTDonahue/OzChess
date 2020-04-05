package ozplayschess;

import processing.core.PImage;

public class Piece {
	
	private String team;
	private String type;
	private int x;
	private int y;
	private PImage image;
	
	public Piece(String team, String type,int x, int y,PImage image) {
		this.team=team;
		this.type=type;
		this.x=x;
		this.y=y;
		this.image=image;
	}
	
	public String getTeam() {
		return team;
	}
	
	public String getType() {
		return type;
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	public PImage getImage() {
		return image;
	}
	
	public void setCoor(int x,int y) {
		this.x=x;
		this.y=y;
	}
	
	public void setX(int x) {
		this.x=x;
	}
	
	public void setY(int y) {
		this.y=y;
	}
	public String toString() {
		return ("Team: "+team+ " Type: "+type+" x: "+x+" y: "+y);
	}
	
}
