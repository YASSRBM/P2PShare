package peer;

import java.net.Socket;

public interface PeerInterface {

    public String Announce();

    public void Look(String[] criteria);

    public String getFile(String key);

    public void interested(String key);

    public void handleInterested(String key, Socket socket);

    public void getPieces(String key);

    public void handleGetPieces(String message, Socket socket);

    public void have(String key);

    public void handleHave(String message, Socket socket);

    public void Update();

}