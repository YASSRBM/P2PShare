package peer;

public class ParsedMessage {
    private String key;
    private String[] infoArray;
    
    public ParsedMessage(String key, String[] infoArray){
        this.key = key;
        this.infoArray = infoArray;
    }

    public String getParsedKey(){
        return key;
    }

    public String[] getParsedInfo(){
        return infoArray;
    }
}
