package ozplayschess;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import processing.core.PApplet;
import processing.core.PImage;

public class OzPlaysChess extends PApplet {

	private String gamePath;
	private int width;
	private int height;
	private ArrayList<namedImage> ims;
	private Random r;
	private int rRes;
		
	//Tracking player move
	private int xStart;//Starting x of held piece
	private int yStart;//Starting y of held piece
	private int xEnd;
	private int yEnd;
	private Piece held;
	//private Piece ghost;

	
	//Game State
	private ArrayList<Piece>pieces;
	private int ozScore;
	private int playerScore;
	private String specMove;//Special move currently being performed
	private int specProg;//Progress through special move
	private int intro;
	private Piece taken;//Last Piece Taken
	
	//Oz Moving Piece
	//Tracking player move
	private int xStartOz;//Starting x of held piece
	private int yStartOz;//Starting y of held piece
	private int xEndOz;
	private int yEndOz;
	private Piece moving;
	private int moveProgress;
	private int moveSpeed;
	private float xInc;//Movement increment
	private float yInc;
	
	//Current Display
	private String currDialog;
	private namedImage ozEmote;

	
	//Last Move track
	private String lastTeam; //The team of the last piece that was moved
	private String lastDest; //The end result of the move
		/*	Possible Values:
		 * 		Empty
		 * 		Oz			//OzPiece
		 * 		Player		//Player Piece
		 * 		Off
		 * 		OzFrame
		 */
	
	private String state;
		/*	Possible States:
		 * 		intro			:Starting Dialog
		 * 		ozResponse		:respond to last player move
		 * 		waitResponse	:wait for click to take move
		 * 		ozTurn			:Oz makes move
		 * 		playerTurn		:player makes move
		 * 		done			:Oz done playing
		 * 		outOfSpecial	:no more special moves
		 * 		pieceMove		:Piece in transit
		 */
	
	//Board Effects
	private Boolean showLasers;
	private Boolean lightsOut;
	private int tide;
	private String tideState;
	private Piece combo;//Piece to be moved multiple times
	
		// Off, In, Out
	private int[]explosion;
	
	//Oz Responses
	public ArrayList<Dialog>ppto;//Player takes Oz piece
	public ArrayList<Dialog>potp;//Oz takes player piece
	public ArrayList<Dialog>pptp;//Player takes player piece
	public ArrayList<Dialog>poto;//Oz takes Oz piece
	public ArrayList<Dialog>poot;//Oz Piece off table
	public ArrayList<Dialog>ppot;//Player Piece off table
	public ArrayList<Dialog>po;  //Piece hits Oz
	public ArrayList<Dialog>gen; //Generic Responses
	public ArrayList<String>spec;//Special Responses
	public ArrayList<Dialog>thinking;
	public ArrayList<Dialog>victory;
	public ArrayList<Dialog>defeat;
	
////////////////////////////////Processing Functions////////////////////////////////////////////
	public void setup() {
		findGamePath();
		generateImages();
		initializeDialogs();
		initializePieces();
		state="intro";
		ozEmote = findNamedImage("smile");
		r = new Random();
		specProg = 0;
		intro =0;
		moveSpeed=100;//Speed at which pieces move during Oz's turn
		moveProgress =-1;
		
		ozScore=0;
		playerScore =0;
		
		//Board Effects
		showLasers=false;
		lightsOut=false;
		explosion =new int[]{-3,-3};
		tide =0;
		tideState = "off";
	}
	
	public void settings() {
		width =  (int) (800*1.5);
		height = (int) (500*1.5);
		size(1200,750);
	}

	public void draw() {
		//System.out.println("state: "+state);
		//System.out.println("specMove: "+specMove);
		
		if(state.equalsIgnoreCase("ozResponse")) {
			ozRespond();
		}
		else if(state.equalsIgnoreCase("waitResponse")) {
			try {
				TimeUnit.SECONDS.sleep(3);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			state="ozTurn";
		}
		else if(state.equalsIgnoreCase("done")) {
			
		}
		else if(state.equalsIgnoreCase("intro")) {
			intro();
		}
		else if(state.equalsIgnoreCase("pieceMove")) {
			genericMove();
		}
		else if(state.equalsIgnoreCase("outOfSpecial")) {
			currDialog="Oz: I'm getting kinda tired... it was fun playing with you though!";
			ozEmote = findNamedImage("smile");
		}
		else if(state.equalsIgnoreCase("ozTurn")) {
			ozTurn();
		}
		else if(state.equalsIgnoreCase("playerTurn")) {
			if(pieces.isEmpty()&&held==null) {//No More Pieces
				state="ozTurn";//Handled in OzTurn
			}
			else if(!mousePressed && held!=null) {//Check if mouse was released offscreen
				if(!(lastDest.equalsIgnoreCase("ozFrame"))) {
					lastTeam=held.getTeam();
					lastDest="Off";
					held=null;
					state="ozTurn";
				}
			}
		}
		else {
			System.out.println("Unregonized state \""+state+"\"");
		}
		background(200,200,200);
		drawBoard();
		drawPieces();
	}
	
	public void mouseClicked() {
		//if(state.equalsIgnoreCase("waitResponse")) {
		//	state="ozTurn";
		//}
		if(state.equalsIgnoreCase("intro")) {
			intro++;
		}
		else if(state.equalsIgnoreCase("done")||state.equalsIgnoreCase("outOfSpecial")) {
			exit();
		}
		else if(state.equalsIgnoreCase("playerTurn")) {
			if(mouseX>=width*0.62f&&mouseX<=width*0.72f&&mouseY>=height*0.65f&&mouseY<=height*0.73f) {//Within Victory
				playerScore++;
				state="ozTurn";
				specMove="victory";
				specProg=0;
				specialMove();
			}
			else if(mouseX>=width*0.78f&&mouseX<=width*0.88f&&mouseY>=height*0.65f&&mouseY<=height*0.73f) {//Within Defeat
				ozScore++;
				state="ozTurn";
				specMove="defeat";
				specProg=0;
				specialMove();
			}			
		}
		
	}
	
	public void mousePressed() {
		if(state.equalsIgnoreCase("playerTurn")) {
			if(mouseX>=width*0.1f&&mouseX<=width*0.6f&&mouseY<height*0.8f) {//Within board
				xStart = (int)((mouseX-width*0.1f)/(width*0.0625f));
				yStart = (int)(mouseY/(height*0.1f));
				held = findPiece(xStart,yStart);
				if(held!=null)pieces.remove(held);
			}
		}
	}
	
	public void mouseReleased() {
		if(held!=null) {//Piece being held
			lastTeam = held.getTeam();
			if(mouseX>=width*0.1f&&mouseX<=width*0.6f&&mouseY<height*0.8f) {//Within board
				xEnd = (int)((mouseX-width*0.1f)/(width*0.0625f));
				yEnd = (int)(mouseY/(height*0.1f));
				if(xStart==xEnd&&yStart==yEnd) {
					pieces.add(held);
					held=null;
					return;
				}
				else if(findPiece(xEnd,yEnd)==null) {//Spot was empty
					lastDest="Empty";	
				}
				else{//Piece in space
					lastDest = findPiece(xEnd,yEnd).getTeam();
					taken = findPiece(xEnd,yEnd);
					pieces.remove(findPiece(xEnd,yEnd));
				}
				held.setCoor(xEnd, yEnd);
				pieces.add(held);
				held=null;
			}
			else if(mouseX>=width*0.6f && mouseX<=width*0.9f&&mouseY<=height*0.48f) {//Within Oz Frame
				lastTeam=held.getTeam();
				lastDest="ozFrame";
				held=null;
			}
			else {//Off Table
				lastTeam=held.getTeam();
				lastDest="Off";
				held=null;
			}
			state="ozResponse";
		}
	}

////////////////////////////////////Utility Functions///////////////////////////////////////////	
	public Piece findPiece(int x,int y) {
		for(int p=0;p<pieces.size();p++) {
			if(pieces.get(p).getX()==x && pieces.get(p).getY()==y) {
				return pieces.get(p);
			}
		}
		return null;
	}
	
	public void findGamePath() {
		File file = new File("."); 
        try {
			gamePath=file.getCanonicalPath();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public namedImage findNamedImage(String name) {
		for(namedImage i:ims) {
			if(i.getName().equalsIgnoreCase(name))return i;
		}
		System.out.println("Cannot find image for:"+name);
		return null;
	}
	
	public void addPieceIm(String name) {
		PImage temp;
		temp = loadImage(gamePath+"\\images\\"+name+".png");
		temp.resize((int)(width*0.0625), (int)(height*0.1));
		ims.add(new namedImage(name,temp));
	}
	
	public void addOzIm(String name) {
		PImage temp;
		temp = loadImage(gamePath+"\\images\\"+name+".png");
		temp.resize((int)(width*0.3), (int)(height*0.48));
		ims.add(new namedImage(name,temp));
	}
	
	public void processDialog(ArrayList<Dialog>a){
		rRes=(int)(r.nextDouble()*a.size());
		ozEmote = findNamedImage(a.get(rRes).getEmote());
		currDialog = a.get(rRes).getMessage();
		a.remove(rRes);
	}
	
	public void addPiece(String team, String name,int x,int y) {
		if(x<0||x>7||y<0||y>7) {
			System.out.println("["+x+","+y+"] is not a valid coordinate to add a piece");
			return;
		}
		if(findPiece(x,y)!=null) {
			System.out.println("Cannot add piece at occupied space ["+x+","+y+"]");
		}
		namedImage temp = findNamedImage(name);
		if(temp==null) {
			System.out.println("image for "+name+" could not be found");
			return;
		}
		else {
			pieces.add(new Piece(team,name,x,y,temp.getImage()));
		}
		
	}
	
	public ArrayList<Coor> findEmptySpaces(ArrayList<Piece>setup) {//Returns random unoccupied space
		int[][]spaces = new int[8][8];
		for(Piece p:setup) {
			spaces[p.getX()][p.getY()]=1;//occupied
		}
		ArrayList<Coor>available = new ArrayList<Coor>();
		for(int x=0;x<8;x++) {
			for(int y=0;y<8;y++) {
				if(spaces[x][y]==0) {
					available.add(new Coor(x,y));
				}
			}
		}
		return available;
		
	}

	public static void main(String _args[]) {
		PApplet.main(new String[] { ozplayschess.OzPlaysChess.class.getName() });
	}
	
	public void genericMove() {

		if(moveProgress==-1) {//Piece not in motion
			state= "pieceMove";
			currDialog="";
			ozEmote = findNamedImage("smile");
			moveProgress=0;
			
			//Choose Piece
			rRes = (int)(r.nextDouble()*pieces.size());
			moving= pieces.remove(rRes);
			xStartOz =moving.getX();
			yStartOz =moving.getY();
			xEndOz =-1;
			yEndOz =-1;
			
			//Choose new location
			while(xEndOz<0 || xEndOz>7 || yEndOz<0 || yEndOz>7|| (xEndOz==xStartOz&&yEndOz==yStartOz) ) {//Not real move
				xEndOz = xStartOz + (int)(r.nextDouble()*6) -3; //Add or subtract 3
				yEndOz = yStartOz + (int)(r.nextDouble()*6) -3; //Add or subtract 3
			}
			
			xInc = ((xEndOz*width*0.0625f+width*0.1f)-(xStartOz*width*0.0625f+width*0.1f))/moveSpeed;
			yInc = ((yEndOz*height*0.1f)-((yStartOz*height*0.1f)))/moveSpeed;
			/*System.out.println("xStart: "+xStartOz);
			System.out.println("yStart: "+yStartOz);
			System.out.println("xEnd: "+xEndOz);
			System.out.println("yEnd: "+yEndOz);
			System.out.println("xInc: "+xInc);
			System.out.println("yInc: "+yInc);*/
		}
		else if(moveProgress==moveSpeed) {//Finished moving	
			if(findPiece(xEndOz,yEndOz)!=null) {//Piece already present
				taken = findPiece(xEndOz,yEndOz);
				pieces.remove(findPiece(xEndOz,yEndOz));
			}
			pieces.add(new Piece(moving.getTeam(),moving.getType(),xEndOz,yEndOz,moving.getImage()));
			//Cleanup
			moveProgress=-1;
			//Verbalize
			//String role = moving.getType().replace("player","");
			//role = role.replace("oz", "");
			//currDialog = ("Oz: "+role+" to "+letters[xEndOz]+""+(yEndOz+1));
			moving = null;
			state="playerTurn";
		}
		else {//Piece in motion
			moveProgress ++;
		}
	}

	public boolean hasPiece(String type) {
		for(Piece iter:pieces) {
			if(iter.getType().equalsIgnoreCase(type)) {
				return true;
			}
		}
		return false;
	}
	
	
	///////////////////////////////Initial Setup/////////////////////////////////////////////////
	public void initializePieces() {
		pieces = new ArrayList<Piece>();
		addPiece("oz","ozPawn",0,1);
		addPiece("oz","ozPawn",1,1);
		addPiece("oz","ozPawn",2,1);
		addPiece("oz","ozPawn",3,1);
		addPiece("oz","ozPawn",4,1);
		addPiece("oz","ozPawn",5,1);
		addPiece("oz","ozPawn",6,1);
		addPiece("oz","ozPawn",7,1);
		addPiece("player","playerPawn",0,6);
		addPiece("player","playerPawn",1,6);
		addPiece("player","playerPawn",2,6);
		addPiece("player","playerPawn",3,6);
		addPiece("player","playerPawn",4,6);
		addPiece("player","playerPawn",5,6);
		addPiece("player","playerPawn",6,6);
		addPiece("player","playerPawn",7,6);
		
		addPiece("oz","ozPointy",2,0);
		addPiece("oz","ozPointy",5,0);
		addPiece("player","playerPointy",2,7);
		addPiece("player","playerPointy",5,7);
		
		addPiece("oz","ozHorse",1,0);
		addPiece("oz","ozHorse",6,0);
		addPiece("player","playerHorse",1,7);
		addPiece("player","playerHorse",6,7);
		
		addPiece("oz","ozTower",0,0);
		addPiece("oz","ozTower",7,0);
		addPiece("player","playerTower",0,7);
		addPiece("player","playerTower",7,7);
		
		addPiece("oz","ozKing",3,0);
		addPiece("player","playerKing",3,7);
		
		addPiece("oz","ozQueen",4,0);
		addPiece("player","playerQueen",4,7);
	}
	
	
	public void initializeDialogs() {
		//.add(new Dialog("",""));
		//Player Moves
		ppto= new ArrayList<Dialog>();//Player takes Oz piece
		ppto.add(new Dialog("beam","Oz: Wow! You're really good at this!"));
		ppto.add(new Dialog("glee","Oz: Excellent strategy!"));
		ppto.add(new Dialog("surprise","Oz: Wait you can do that???"));
		ppto.add(new Dialog("shock","Oz: Oh... well that certainly changes my plans"));
		ppto.add(new Dialog("glee","Oz: Good move"));
		ppto.add(new Dialog("glee","Oz: A bold move indeed"));
		ppto.add(new Dialog("concern","Oz: Now I KNOW that one's against the rules"));
		
		potp= new ArrayList<Dialog>();//Oz takes player piece
		potp.add(new Dialog("glee","Oz: Hey, that was gonna be my move!"));
		potp.add(new Dialog("ponder","Oz: Was that a good move? I genuinely don't know..."));
		potp.add(new Dialog("ponder","Oz: The white pieces are yours right?"));
		
		pptp= new ArrayList<Dialog>();//Player takes player piece
		pptp.add(new Dialog("concern","Oz: I see you found my spy..."));
		pptp.add(new Dialog("ponder","Oz: That's part of some bigger plan I'm sure..."));
		pptp.add(new Dialog("confused","Oz: You know that was your piece right?"));
		pptp.add(new Dialog("confused","Oz: Are you sure you know the rules to this?"));
		
		poto= new ArrayList<Dialog>();//Oz takes Oz piece
		poto.add(new Dialog("glee","Oz: I knew that one was a traitor!"));
		poto.add(new Dialog("concern","Oz: Hey now, at least pretend you're playing fair"));
		poto.add(new Dialog("concern","Oz: You know, I'm really not a fan of cheaters"));
		poto.add(new Dialog("beam","Oz: With their powers combined, this piece is even stronger!"));
		
		poot= new ArrayList<Dialog>();//Oz Piece off table
		poot.add(new Dialog("concern","Oz: Well that's one way to capture a piece"));
		poot.add(new Dialog("glee","Oz: Don't worry, they'll be back soon"));
		
		ppot= new ArrayList<Dialog>();//Player Piece off table
		ppot.add(new Dialog("surprise","Oh? We're playing 3D chess now?"));
		ppot.add(new Dialog("Thinking","Oz: That's certainly an interesting strategy"));
		
		po = new ArrayList<Dialog>();//Piece hits Oz
		po.add(new Dialog("cry","Oz: Ouch!"));
		po.add(new Dialog("cry","Oz: C'mon dude! That hurt"));
		po.add(new Dialog("cry","Oz: Ow!"));
		po.add(new Dialog("cry","Oz: That was pretty mean..."));
		po.add(new Dialog("cry","Oz: Ouch..."));
		
		gen = new ArrayList<Dialog>();//Generic Responses
		gen.add(new Dialog("thinking","Oz: ..."));
		gen.add(new Dialog("thinking","Oz: ..."));
		gen.add(new Dialog("thinking","Oz: ..."));
		gen.add(new Dialog("thinking","Oz: ..."));
		gen.add(new Dialog("thinking","Oz: ..."));
		gen.add(new Dialog("thinking","Oz: Now let me think..."));
		gen.add(new Dialog("thinking","Oz: hmmmmmmmmmmmmmmmmmmmmmmmmm..."));
		gen.add(new Dialog("thinking","Oz: hmmmmmmmmmmmmmmmmmm..."));
		gen.add(new Dialog("thinking","Oz: hmmmmmmmmmmmmm..."));
		gen.add(new Dialog("thinking","Oz: hmmmmmmmm..."));
		gen.add(new Dialog("thinking","Oz: hmmm..."));
		gen.add(new Dialog("thinking","Oz: hmmm..."));
		gen.add(new Dialog("thinking","Oz: hmmm..."));
		gen.add(new Dialog("thinking","Oz: hmmm..."));
		gen.add(new Dialog("thinking","Oz: hmmm..."));
		gen.add(new Dialog("thinking","Oz: Now there's an interesting move"));
		gen.add(new Dialog("thinking","Oz: I see..."));
		gen.add(new Dialog("confused","Oz: Really? There?"));
		
		
		
		victory = new ArrayList<Dialog>();
		victory.add(new Dialog("beam","Oz: That was a good game!"));
		victory.add(new Dialog("smile","Oz: What a close game!"));
		victory.add(new Dialog("concern","Oz: Oh no! I was about to unleash my ultimate move"));
		victory.add(new Dialog("confused","Oz: Wait which of us is the purple team?"));
		victory.add(new Dialog("smile","Oz: I'll get you next time"));
		victory.add(new Dialog("concern","Oz: Dang, you got me"));
		victory.add(new Dialog("ponder","Oz: The strategy guide didn't mention this part..."));
		victory.add(new Dialog("thinking","Oz: Man, I really didn't see that one coming"));
		
		defeat = new ArrayList<Dialog>();
		defeat.add(new Dialog("beam","Oz: I did it!"));
		defeat.add(new Dialog("beam","Oz: Wow, I won even with you cheating"));
		defeat.add(new Dialog("beam","Oz: That's a point for me!"));
		defeat.add(new Dialog("smile","Oz: It's ok, maybe you'll win next time"));
		defeat.add(new Dialog("smile","Oz: All according to keikaku... (keikaku means plan)"));
		defeat.add(new Dialog("shock","Oz: Oh... I thought I was about to lose"));
		defeat.add(new Dialog("thinking","Oz: Are you sure you're not letting me win?"));
		
		
		spec = new ArrayList<String>();//Special responses
		spec.add("laser");
		spec.add("terra");
		spec.add("toTheLeft");
		spec.add("reinforcements");
		spec.add("lightsOut");
		spec.add("firecracker");
		spec.add("moveBack");
		spec.add("kingMe");
		spec.add("tide");
		
		//spec.add("leftFootBlue");
		//spec.add("allIn");
		//spec.add("combo");//Take a bunch of turns in a row
			//add version of genericMove that takes in a piece parameter?
	}
	
	
	public void generateImages() {
		ims = new ArrayList<namedImage>();
		
		try {
			PImage temp;
			temp = loadImage(gamePath+"\\images\\board.png");
			temp.resize((int)(width*0.5f), (int)(height*0.8f));
			ims.add(new namedImage("board",temp));	
			temp = loadImage(gamePath+"\\images\\tide.png");
			temp.resize((int)(width*0.5f), (int)(height*0.8f));
			ims.add(new namedImage("tideIm",temp));	
			
			addOzIm("beam");
			addOzIm("charge");
			addOzIm("concern");
			addOzIm("confused");
			addOzIm("cry");
			addOzIm("done");
			addOzIm("glee");
			addOzIm("laser");
			addOzIm("ponder");
			addOzIm("shock");
			addOzIm("smile");
			addOzIm("surprise");
			addOzIm("thinking");
			
			addPieceIm("ozPawn");
			addPieceIm("playerPawn");
			addPieceIm("ozPointy");
			addPieceIm("playerPointy");
			addPieceIm("ozHorse");
			addPieceIm("playerHorse");
			addPieceIm("ozTower");
			addPieceIm("playerTower");
			addPieceIm("ozKing");
			addPieceIm("playerKing");
			addPieceIm("ozQueen");
			addPieceIm("playerQueen");
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
		
	
///////////////////////////////Oz Moves/////////////////////////////////////////////////////////
	public void ozRespond() {
		// 1) Respond to player move
		//	a) Check for special cases
		//	b) Determine move category
		//	c) Check for response from category
		//	d) If none found, give generic response
		if(specMove!=null){
			if(specMove.equalsIgnoreCase("laser")) {
				state="waitResponse";
				return;
			}
		}
		
		// 1) Respond to player move
		if(lastTeam.equalsIgnoreCase("player")&&lastDest.equalsIgnoreCase("oz")) {//Player piece takes Oz piece
			boolean hasQueen=false;
			boolean hasKing = false;
			if(hasPiece("ozKing"))hasKing=true;
			if(hasPiece("ozQueen"))hasQueen=true;
			
			if(taken.getType().equalsIgnoreCase("ozKing")&& hasQueen && !hasKing) {
				ozEmote = findNamedImage("glee");
				currDialog = "Oz: Good thing this is a Matriarchy!";
			}
			else if(ppto.isEmpty()) {
				 genericResponse();
			}
			else {
				processDialog(ppto);
			}
		}
		else if(lastTeam.equalsIgnoreCase("oz")&&lastDest.equalsIgnoreCase("player")) {//Oz piece takes player piece
			if(potp.isEmpty()) {
				 genericResponse();
			}
			else {
				processDialog(potp);
			}
		}
		else if(lastTeam.equalsIgnoreCase("player")&&lastDest.equalsIgnoreCase("player")) {//player piece takes player piece
			if(pptp.isEmpty()) {
				 genericResponse();
			}
			else {
				processDialog(pptp);
			}
		}
		else if(lastTeam.equalsIgnoreCase("oz")&&lastDest.equalsIgnoreCase("oz")) {//Oz piece takes Oz piece
			if(poto.isEmpty()) {
				 genericResponse();
			}
			else {
				processDialog(poto);
			}
		}
		else if(lastTeam.equalsIgnoreCase("oz")&&lastDest.equalsIgnoreCase("off")) {//Oz piece off table
			if(poot.isEmpty()) {
				 genericResponse();
			}
			else {
				processDialog(poot);
			}
		}
		else if(lastTeam.equalsIgnoreCase("player")&&lastDest.equalsIgnoreCase("off")) {//player piece off table
			if(ppot.isEmpty()) {
				 genericResponse();
			}
			else {
				processDialog(ppot);
			}
		}
		else if(lastDest.equalsIgnoreCase("ozFrame")) {//Piece thrown at Oz
			if(po.isEmpty()) {
				ozEmote = findNamedImage("done");
				currDialog = "Well if you're gonna keep doing that I'm done playing";
				state="done";
				return;
			}
			else {
				processDialog(po);
			}
		}
		else {//Empty Space
			genericResponse();
		}
		state="waitResponse";
	}
	
	
	public void genericResponse() {
		rRes=(int)(r.nextDouble()*gen.size());
		ozEmote = findNamedImage(gen.get(rRes).getEmote());
		currDialog = gen.get(rRes).getMessage();
	}
	
	
	public void ozTurn() {
		//currDialog = null;/////////////////////////////////////////////////////////////////////////////////////////////////
		// 1) Check if board empty or only 1 piece
		//	[true] Offer to play again
		// 2) Check for carryover move
		//	[true] continue carryover move
		// 3) Check if special moves empty
		// 	[true] Stop Play 
		// 4) Generate Random Number
		// 5) Check if within range for special
		//	[true] randomly pick special move
		//	[false] select basic move
		// 6) Increment numTurns
			
		if(pieces.isEmpty()) {//No More Pieces
			if(!specMove.equalsIgnoreCase("outofPieces")) {//If not already in progress
				specMove="outofPieces";
				specProg=0;
			}
			outofPieces();
			return;
		}
		else if(specProg>0) {// 2) Check for carryover move
			specialMove();
		}
		else if(specProg==0) {//No special move in progress
			if(specMove !=null) {
				specMove=null;
				state="playerTurn";
				return;
			}
			if(spec.isEmpty()) {// 3) Check if special moves empty
				state = "outOfSpecial";
				return;
			}	
			rRes = (int)(r.nextDouble()*10);// 4) Generate Random Number
			if(rRes>5) {// 5) Check if within range for special//////////////////////////////////////////
				rRes = (int)(r.nextDouble()*spec.size());//[true] randomly pick special move
				specMove= spec.remove(rRes);
				specialMove();
			}
			else {//[false] select basic move
				genericMove();
			}			
		}
		else {//SpecProg <0
			System.out.println("Cannot Use Specprog value below 0: "+specProg);
		}
	}
	
	public void intro() {
		switch(intro) {
		case 0:
			currDialog= "Oz: Hi! Thanks for coming to visit me!";
			ozEmote = findNamedImage("glee");
			break;
		case 1:
			currDialog= "Oz: Before we get started you should know that I DEFINITELY\n           know the rules to this ok?";
			//ozEmote = findNamedImage("serious");
			break;
		case 2:
			currDialog= "Oz: I didn't just find this set lying around in the lobby.";
			break;
		case 3:
			currDialog= "Oz: I'll be purple and you can be white.";
			break;
		case 4:
			currDialog= "Oz: I'll even let you have the first move!";
			ozEmote = findNamedImage("beam");
			state="playerTurn";
			break;
		default:
			System.out.println("No case in intro for:"+intro);
			break;
		}
	}
	
//////////////////////////////////////Visual/////////////////////////////////////////////////
	public void drawPieces() {
		if(!lightsOut) {
			for(Piece p:pieces) {
				image(p.getImage(),(p.getX()*width*0.0625f)+(width*0.1f),p.getY()*height*0.1f);
			}
			if(held!=null) {//draw held
				image(held.getImage(),mouseX-(width*0.03125f),mouseY-(height*0.05f));
			}
			if(moving!=null) {//draw moving
				try {
					TimeUnit.MILLISECONDS.sleep(5);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				image(moving.getImage(),
						 (xStartOz*width*0.0625f)+(width*0.1f)+(xInc*moveProgress),
						 (yStartOz*height*0.1f)+(yInc*moveProgress));
			}
		}
	}
	
	public void drawBoard() {
		//OzFrame
		try {
			image(ozEmote.getImage(),width*0.6f,0);
		}
		catch(Exception e) {
			System.out.println("Cannot find image for "+ozEmote.getName());
		}
		
		image(findNamedImage("board").getImage(),width*0.1f,0f);
		
		//Explosion
				stroke(0,0,0,100);
				fill(0,0,0,100);
				circle(width*(explosion[0]+0.5f)*0.0625f+width*0.1f,height*(explosion[1]+0.5f)*0.1f,3*width*0.0625f);
		
		//Scoreboard
		stroke(180,180,180);
		fill(180,180,180);
		rect(width*0.6f,height*0.481f,width*0.4f,height);
		textSize(height/20);
		textAlign(CENTER,CENTER);
		if(state.equalsIgnoreCase("ozTurn")||state.equalsIgnoreCase("ozResponse")||state.equalsIgnoreCase("waitResponse")) {
			fill(120,70,120);
			stroke(120,70,120);
			rect(width*0.65f, height*0.50f,width*0.20f,height*0.08f);
			fill(0,0,0);
			text("Oz's Turn",width*0.75f,height*0.535f);
		}
		else if(state.equalsIgnoreCase("playerTurn")) {
			fill(80,80,80);
			stroke(80,80,80);
			rect(width*0.65f, height*0.50f,width*0.20f,height*0.08f);
			fill(0,0,0);
			text("Your Turn",width*0.75f,height*0.535f);
		}
		
		//Victory Defeat Buttons
		fill(50,200,50);
		rect(width*0.62f, height*0.65f,width*0.10f,height*0.08f);
		fill(200,50,50);
		rect(width*0.78f, height*0.65f,width*0.10f,height*0.08f);
		fill(0,0,0);
		textSize(height/35);
		textAlign(CENTER,CENTER);
		text("Declare\nVictory",width*0.67f,height*0.69f);
		text("Admit\nDefeat",width*0.83f,height*0.69f);
		text("You: "+playerScore+"    Oz: "+ozScore,width*0.75f,height*0.61f);
		
		
		
		//Draw OzMove
		//stroke(255,245,171,100);
		//fill(255,245,171,100);
		//rect(width*xEndOz*0.0625f+width*0.1f,height*yEndOz*0.1f,width*0.0625f, height*0.1f);
		
		
		//BoardEffects////////////////////////////////////////////////////////////////////////////////
		
		//Tide
		if(!tideState.equalsIgnoreCase("off")) {
			image(findNamedImage("tideIm").getImage(),width*(-0.38f)+(tide*width*0.059f),0f);
			//Remove Caught Pieces
			for(int p=0;p<pieces.size();p++) {
				if(pieces.get(p).getX()<=tide-1) {//change to < later////////////////////////////////////////
					pieces.remove(p);
					p--;
				}
			}
		}
		
		//Lasers
		if(showLasers) {
				fill(102,255,178);
				rect(width*0.225f,0,width*0.0625f,height);
				rect(width*0.4125f,0,width*0.0625f,height);
		}
		
		//LightsOut
		if(lightsOut) {
			background(0,0,0);
			//image(findNamedImage("dark").getImage(),width*0.6f,0);
		}
		
		
		//EndBoardEffects//////////////////////////////////////////////////////////////////////////////
		
		//Dialog Panel
		stroke(150,150,150);
		fill(150,150,150);
		rect(width*0.1f,height*0.8f,width*0.8f,height*0.2f);
		if(currDialog!=null) {
			fill(0,0,0);
			textSize(width*0.025f);
			textAlign(LEFT,TOP);
			text(currDialog,width*0.11f,height*0.81f);
		}		
		
		//Draw Borders
		stroke(0,0,0);
		fill(0,0,0);
		rect(0,0,width*0.1f,height);//left bar
		rect(width*0.9f,0,width*0.1f,height);//right bar
		
		
		
	}
	
////////////////////////////////////////////SpecialMoves//////////////////////////////////////////////
	public void specialMove() {	
		//System.out.println("specProg: "+specProg);
		//System.out.println("tideState: "+tideState);
		switch(specMove) {
			case "laser":
				laser();
				break;
			case "terra":
				terra();
				break;
			case "firecracker":
				fireCracker();
				break;
			case "chocolate":
				
				break;
			case "lightsOut":
				lightsOut();
				break;
			case "moveBack":
				moveBack();
				break;
			case "leftFootBlue":
				
				break;
			case "allIn":
	
				break;
			case "toTheLeft":
				toTheLeft();
				break;
			case "sinkhole":
	
				break;
			case "reinforcements":
				reinforcements();
				break;
			case "tide":
				tide();
				break;
			case "victory":
				victory();
				break;
			case "defeat":
				defeat();
				break;
			case "kingMe":
				kingMe();
				break;
			default:
				System.out.println("Cannot perform unknow special move:"+specMove);
				break;
		}
	}
	
	public void laser(){
		switch(specProg) {
		case 5://Charge
			ozEmote = findNamedImage("charge");
			currDialog = "Oz is charging a powerful attack!!!";
			specProg--;
			break;
		case 4://Charge
			try {
				TimeUnit.SECONDS.sleep(3);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			specProg--;
			break;
		case 3://Charge
			state="playerTurn";
			specProg--;
			break;
		case 2://Fire Laser
			showLasers=true;
			ozEmote = findNamedImage("laser");
			currDialog="Oz fires a powerful attack!";
			for(int p=0;p<pieces.size();p++) {//Remove pieces hit
				if(pieces.get(p).getX()==2||pieces.get(p).getX()==5) {
					pieces.remove(p);
					p--;
				}
			}
			specProg--;
			break;
		case 1://End Special Move
			try {
				TimeUnit.SECONDS.sleep(4);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			showLasers=false;
			ozEmote = findNamedImage("smile");
			currDialog=null;
			state="ozTurn";
			specProg--;
			break;
		case 0:
			specProg=5;//set to start value////////////////////////
			break;
		default:
			System.out.println("No case in special move \"Laser\" for specProg: "+specProg);
			break;
		}
	}
	
	public void toTheLeft(){
		switch(specProg) {
		case 7://To the left!
			ozEmote = findNamedImage("glee");
			currDialog = "Oz: To the left!";
			specProg--;
			break;
		case 6://Sound Effect & Move
			try {
				TimeUnit.SECONDS.sleep(3);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			//Sound Effect/////////////////////////
			for(int p = 0;p<pieces.size();p++) {
				pieces.get(p).setX(pieces.get(p).getX()-1);
				if(pieces.get(p).getX()<0) {
					pieces.remove(p);
					p--;
				}
			}
			specProg--;
			break;
		case 5://Take it back now y'all
			ozEmote = findNamedImage("beam");
			currDialog = "Oz: Take it back now y'all";
			specProg--;
			break;
		case 4://Sound Effect & Move
			try {
				TimeUnit.SECONDS.sleep(3);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			//Sound Effect/////////////////////////
			for(int p = 0;p<pieces.size();p++) {
				pieces.get(p).setY(pieces.get(p).getY()+1);
				if(pieces.get(p).getY()>7) {
					pieces.remove(p);
					p--;
				}
			}
			specProg--;
			break;
		case 3://Reverse!
			ozEmote = findNamedImage("beam");
			currDialog = "Oz: Reverse! Reverse!";
			specProg--;
			break;
		case 2://Reverse!
			try {
				TimeUnit.SECONDS.sleep(3);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			for(Piece p:pieces) {
				p.setY(7-p.getY());
			}
			specProg--;
			break;
		case 1://End Special Move
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			ozEmote = findNamedImage("smile");
			currDialog=null;
			state="ozTurn";
			specProg--;
			break;
		case 0:
			specProg=7;//set to start value
			break;
		default:
			System.out.println("No case in special move \"toTheLeft\" for specProg:"+specProg);
			break;
		}
	}
	
	public void terra(){
		switch(specProg) {
		case 4:
			currDialog = "Oz has completed the terracotta army!";
			ozEmote = findNamedImage("beam");
			
			ArrayList<Coor>available = findEmptySpaces(pieces);
			ArrayList<Piece>toAdd = new ArrayList<Piece>();
			for(Piece p:pieces) {
				if(available.isEmpty()) {
					break;
				}
				else {
					if(p.getTeam().equalsIgnoreCase("oz")) {//if Oz's piece
						rRes = (int)(r.nextDouble()*available.size());//Choose random empty coordinate
						toAdd.add(new Piece(p.getTeam(),
											p.getType(),
											available.get(rRes).getX(),
											available.get(rRes).getY(),
											p.getImage()));
						available.remove(rRes);
					}
				}
			}
			for(Piece p:toAdd) {
				pieces.add(p);
			}
			specProg--;
			break;
		case 3:
			try {
				TimeUnit.SECONDS.sleep(3);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			currDialog = "Oz: Whoa! I totally forgot I was building that!";
			specProg--;
			break;
		case 2:
			try {
				TimeUnit.SECONDS.sleep(3);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			ozEmote = findNamedImage("beam");
			currDialog = "Oz is now producing +1 culture per turn";
			specProg--;
			break;
		case 1://End Special Move
			try {
				TimeUnit.SECONDS.sleep(3);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			ozEmote = findNamedImage("smile");
			currDialog=null;
			state="ozTurn";
			specProg--;
			break;
		case 0:
			specProg=4;//set to start value
			break;
		default:
			System.out.println("No case in special move \"specialTemplate\" for specProg"+specProg);
			break;
		}
	}
	
	public void reinforcements(){
		switch(specProg) {
		case 4:
			//ozEmote = findNamedImage("serious");
			currDialog = "Warning: Incoming Reinforcements";
			specProg--;
			break;
		case 3:
			try {
				TimeUnit.SECONDS.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			ArrayList<Coor>available = findEmptySpaces(pieces);
			for(int i =0;i<10;i++) {
				if(available.isEmpty()) {
					break;
				}
				else {
					rRes = (int)(r.nextDouble()*available.size());//Choose random empty coordinate
					pieces.add(new Piece("oz",
										 "ozQueen",
										 available.get(rRes).getX(),
										 available.get(rRes).getY(),
										 findNamedImage("ozQueen").getImage()));
					available.remove(rRes);
				}
			}
			ozEmote = findNamedImage("glee");
			currDialog = "Oz: You're all just in time!";
			specProg--;
			break;
		case 2:
			try {
				TimeUnit.SECONDS.sleep(3);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			ozEmote = findNamedImage("beam");
			currDialog = "Oz: Now, let the annihilation begin";
			specProg--;
			break;
		case 1://End Special Move
			try {
				TimeUnit.SECONDS.sleep(3);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			ozEmote = findNamedImage("smile");
			currDialog=null;
			state="ozTurn";
			specProg--;
			break;
		case 0:
			specProg=4;//set to start value////////////////////////
			break;
		default:
			System.out.println("No case in special move \"reinforcements\" for specProg"+specProg);
			break;
		}
	}
	
	public void lightsOut(){
		switch(specProg) {
		case 5:
			lightsOut=true;
			specProg--;
			break;
		case 4:
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			currDialog="Oz: What happened? Is something wrong?";
			ArrayList<Coor>available = findEmptySpaces(new ArrayList<Piece>());
			for(Piece p:pieces) {
					rRes = (int)(r.nextDouble()*available.size());//Choose random empty coordinate
					p.setX(available.get(rRes).getX());
					p.setY(available.get(rRes).getY());
					available.remove(rRes);
			}
			specProg--;
			break;
		case 3:
			try {
				TimeUnit.SECONDS.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			currDialog=" *CLANG*    *CRASH*    *THUD*";
			specProg--;
			break;
		case 2:
			try {
				TimeUnit.SECONDS.sleep(4);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			lightsOut=false;
			currDialog="Oz: Well at least nothing changed right?";
			ozEmote = findNamedImage("beam");
			specProg--;
			break;
		case 1://End Special Move
			try {
				TimeUnit.SECONDS.sleep(3);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			ozEmote = findNamedImage("smile");
			currDialog=null;
			state="ozTurn";
			specProg--;
			break;
		case 0:
			specProg=5;//set to start value
			break;
		default:
			System.out.println("No case in special move \"specialTemplate\" for specProg"+specProg);
			break;
		}
	}
	
	public void fireCracker(){
		switch(specProg) {
		case 3:
			rRes = (int)(r.nextDouble()*pieces.size());
			Piece p = pieces.get(rRes);
			explosion[0]=p.getX();
			explosion[1]=p.getY();
			for(int i =0;i<pieces.size();i++) {//Remove Surrounding pieces
				if(abs(p.getX()-pieces.get(i).getX())<=1 && abs(p.getY()-pieces.get(i).getY())<=1) {
					pieces.remove(i);
					i--;
				}
			}
			//Sound effect////////////////////////////
			specProg--;
			break;
		case 2:
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			ozEmote = findNamedImage("surprise");
			currDialog = "Oz: Oh! I forgot I put a firecracker in that one";
			specProg--;
			break;
		case 1://End Special Move
			try {
				TimeUnit.SECONDS.sleep(3);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			ozEmote = findNamedImage("smile");
			currDialog=null;
			state="ozTurn";
			specProg--;
			break;
		case 0:
			specProg=3;//set to start value
			break;
		default:
			System.out.println("No case in special move \"firecracker\" for specProg"+specProg);
			break;
		}
	}
	
	public void moveBack(){
		switch(specProg) {
		case 2:
			ozEmote = findNamedImage("thinking");
			currDialog = "Oz: Hmmm, I think I liked it better there.";
			for(Piece p:pieces) {
				if(p.getX()==xEnd && p.getY()==yEnd) {
					p.setX(xStart);
					p.setY(yStart);
					break;
				}
			}
			specProg--;
			break;
		case 1://End Special Move
			try {
				TimeUnit.SECONDS.sleep(3);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			ozEmote = findNamedImage("smile");
			currDialog=null;
			state="ozTurn";
			specProg--;
			break;
		case 0:
			specProg=2;//set to start value////////////////////////
			break;
		default:
			System.out.println("No case in special move \"moveBack\" for specProg"+specProg);
			break;
		}
	}
	
	public void tide(){
		switch(specProg) {
		case 4:
			currDialog = "Oz: Looks like the tide's coming in";
			ozEmote = findNamedImage("surprise");
			tide=1;
			tideState="in";
			specProg--;
			break;
		case 3:
			try {
				TimeUnit.SECONDS.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			specProg--;
			genericMove();
			break;
		case 2:			
			if(tideState.equalsIgnoreCase("in")) {
				if(tide>=4) {
					tideState="out";
				}
				if(tide>=1) {
					tide++;
				}
				else if(r.nextDouble()>0.3){
					tide++;
				}
				else {
					tide--;
				}
				genericMove();
			}
			if(tideState.equalsIgnoreCase("out")){
				if(tide<=0) {
					tideState="off";
					specProg--;
				}
				if(r.nextDouble()>0.3){
					tide--;
				}
				else {
					tide++;
				}
				genericMove();
			}
			
			break;
		case 1://End Special Move
			ozEmote = findNamedImage("smile");
			currDialog=null;
			state="ozTurn";
			specProg--;
			break;
		case 0:
			specProg=4;
			break;
		default:
			System.out.println("No case in special move \"tide\" for specProg"+specProg);
			break;
		}
	}
	
	public void victory(){
		switch(specProg) {
		case 4:
			rRes=(int)(r.nextDouble()*victory.size());
			ozEmote = findNamedImage(victory.get(rRes).getEmote());
			currDialog = victory.get(rRes).getMessage();
			specProg--;
			break;
		case 3:
			try {
				TimeUnit.SECONDS.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			ozEmote = findNamedImage("beam");
			currDialog="Let's play again!";
			specProg--;
			break;
		case 2:
			try {
				TimeUnit.SECONDS.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			tideState="off";
			initializePieces();
			specProg--;
			break;
		case 1://End Special Move
			ozEmote = findNamedImage("smile");
			currDialog=null;
			state="ozTurn";
			specProg--;
			break;
		case 0:
			specProg=4;//set to start value////////////////////////
			break;
		default:
			System.out.println("No case in special move \"specialTemplate\" for specProg"+specProg);
			break;
		}
	}
	
	public void defeat(){
		switch(specProg) {
		case 4:
			rRes=(int)(r.nextDouble()*defeat.size());
			ozEmote = findNamedImage(defeat.get(rRes).getEmote());
			currDialog = defeat.get(rRes).getMessage();
			specProg--;
			break;
		case 3:
			try {
				TimeUnit.SECONDS.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			ozEmote = findNamedImage("beam");
			currDialog="Let's play again!";
			specProg--;
			break;
		case 2:
			try {
				TimeUnit.SECONDS.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			initializePieces();
			specProg--;
			break;
		case 1://End Special Move
			ozEmote = findNamedImage("smile");
			currDialog=null;
			state="ozTurn";
			specProg--;
			break;
		case 0:
			specProg=4;//set to start value////////////////////////
			break;
		default:
			System.out.println("No case in special move \"specialTemplate\" for specProg"+specProg);
			break;
		}
	}
	
	public void outofPieces(){
		switch(specProg) {
		case 3:
			try {
				TimeUnit.SECONDS.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			ozEmote = findNamedImage("concern");
			currDialog = "Oz: Looks like we're out of pieces.";
			showLasers=false;
			specProg--;
			break;
		case 2:
			try {
				TimeUnit.SECONDS.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			ozEmote = findNamedImage("beam");
			currDialog="Let's play again!";
			specProg--;
			break;
		case 1:
			try {
				TimeUnit.SECONDS.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			tideState="off";
			initializePieces();
			ozEmote = findNamedImage("smile");
			currDialog=null;
			state="ozTurn";
			specProg--;
			break;
		case 0:
			specProg=3;//set to start value////////////////////////
			break;
		default:
			System.out.println("No case in special move \"outofPieces\" for specProg "+specProg);
			break;
		}
	}
	
	
	public boolean ozOnlyHasKing() {
		for(Piece iter:pieces) {
			if(iter.getTeam().equalsIgnoreCase("oz")) {//Piece Belongs to Oz
				if(!(iter.getType().equalsIgnoreCase("ozKing"))) {//Piece is not a king
					return false;
				}
			}
		}
		return true;
	}
		
	public void kingMe(){
		switch(specProg) {
		case 4:
			ozEmote = findNamedImage("thinking");
			currDialog = "Oz: I think I have a good idea for a move...";
			specProg--;
			break;
		case 3:
			try {
				TimeUnit.SECONDS.sleep(3);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			ozEmote = findNamedImage("beam");
			currDialog = "Oz: King Me";
			specProg--;
			break;
		case 2:
			try {
				TimeUnit.MILLISECONDS.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(ozOnlyHasKing()) {
				specProg--;
			}
			else {
				int tempx=0;
				int tempy=0;
				Piece toReplace=null;
				for(Piece iter:pieces) {
					if(iter.getTeam().equalsIgnoreCase("oz")) {//Piece Belongs to Oz
						if(!(iter.getType().equalsIgnoreCase("ozKing"))) {//Piece is not a king
							tempx=iter.getX();
							tempy=iter.getY();
							toReplace=iter;
							break;
						}
					}
				}
				if(toReplace!=null) {
					pieces.remove(toReplace);
					addPiece("oz","ozKing",tempx,tempy);
				}
			}
			break;
		case 1://End Special Move
			ozEmote = findNamedImage("smile");
			currDialog=null;
			state="ozTurn";
			specProg--;
			break;
		case 0:
			specProg=4;//set to start value////////////////////////
			break;
		default:
			System.out.println("No case in special move \"specialTemplate\" for specProg"+specProg);
			break;
		}
	}
	
	public void specialTemplate(){
		switch(specProg) {
		case 10:
			
			specProg--;
			break;
		case 9:
			
			specProg--;
			break;
		case 8:
			
			specProg--;
			break;
		case 7:
	
			specProg--;
			break;
		case 6:
	
			specProg--;
			break;
		case 5:
			
			specProg--;
			break;
		case 4:

			specProg--;
			break;
		case 3:
			
			specProg--;
			break;
		case 2:

			specProg--;
			break;
		case 1://End Special Move
			ozEmote = findNamedImage("smile");
			currDialog=null;
			state="ozTurn";
			specProg--;
			break;
		case 0:
			specProg=10;//set to start value////////////////////////
			break;
		default:
			System.out.println("No case in special move \"specialTemplate\" for specProg"+specProg);
			break;
		}
	}
	
	/*public void combo(){
		switch(specProg) {
		case 10://Comment & choose piece
			rRes = (int)(r.nextDouble()*pieces.size());
			combo = pieces.remove(rRes);
			specProg--;
			genericMove(combo);
			break;
		case 9:
			currDialog="What??? Oz is chaining attacks!";
			specProg--;
			break;
		case 8:
			genericMove(combo);
			genericMove(combo);
			genericMove(combo);
			specProg--;
			break;
		case 7:
	
			specProg--;
			break;
		case 6:
	
			specProg--;
			break;
		case 5:
			
			specProg--;
			break;
		case 4:

			specProg--;
			break;
		case 3:
			
			specProg--;
			break;
		case 2:

			specProg--;
			break;
		case 1://End Special Move
			ozEmote = findNamedImage("smile");
			currDialog=null;
			state="ozTurn";
			specProg--;
			break;
		case 0:
			specProg=10;//set to start value////////////////////////
			break;
		default:
			System.out.println("No case in special move \"specialTemplate\" for specProg"+specProg);
			break;
		}
	}
	*/
}
