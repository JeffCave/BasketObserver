class Soundplayer extends Thread {

	privat Applet applet;
	private String sound_url;

	public Soundplayer(Applet app, String url) {
		applet = app;
		sound_url = url;
		this.start();
	}

	public void run() { applet.play(getDocumentBase(), sound_url);  }

}
