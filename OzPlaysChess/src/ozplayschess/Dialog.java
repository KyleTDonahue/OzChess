package ozplayschess;

public class Dialog {
	private String message;
	private String emote;
	
	public Dialog(String emote, String message) {
		this.message=message;
		this.emote = emote;
	}

	public String getMessage() {
		return message;
	}
	
	public String getEmote() {
		return emote;
	}
	
}
