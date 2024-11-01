package peer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;

public class FileHandle extends File {
    private String Name;
    private String Path;
    public  String Key;
    public long Length;
    private int pieceSize;
    private boolean BufferMap[];

    public FileHandle(String filePath, int pieceSize){
        super(filePath);
        Name = super.getName();
        Path = filePath;
        Length = super.length();
        this.pieceSize = pieceSize; 
        BufferMap = new boolean[(int) Math.ceil((double) Length / pieceSize)];
        if(!super.isFile()){
            try{
                if(super.createNewFile()){
                    System.out.println("File created");
                }else{
                    System.out.println("File not created");
                }
            } catch (Exception e){
                e.printStackTrace();
            }

        }
        try {
            Process p = Runtime.getRuntime().exec("md5sum "+filePath);
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            Key = br.readLine().split(" ")[0];
            p.waitFor();
            p.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
       
        // printing buffermap 
        initializeBufferMap();
    }
    public FileHandle(String filePath, int pieceSize,long length ){
        super(filePath);
        Name = super.getName();
        Path = filePath;
        Length = length;
        this.pieceSize = pieceSize; 
        BufferMap = new boolean[(int) Math.ceil((double) Length / pieceSize)];
        if(!super.isFile()){
            try{
                if(super.createNewFile()){
                    System.out.println("File created");
                }else{
                    System.out.println("File not created");
                }
            } catch (Exception e){
                e.printStackTrace();
            }

        }
        try {
            Process p = Runtime.getRuntime().exec("md5sum "+filePath);
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            Key = br.readLine().split(" ")[0];
            p.waitFor();
            p.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
       
        // printing buffermap 
        initializeEmptyBufferMap();
    }
                    
    private void initializeBufferMap() {
        int pieceCount = (int) Math.ceil((double) Length / pieceSize);
        for (int i = 0; i < pieceCount; i++) {
            long startPos = i * pieceSize;
            long endPos = Math.min(startPos + pieceSize, Length);
            if (endPos - startPos == pieceSize) {
                BufferMap[i] = true;
            }
        }
    }
    private void initializeEmptyBufferMap() {
        int pieceCount = (int) Math.ceil((double) Length / pieceSize);
        for (int i = 0; i < pieceCount; i++) {
            long startPos = i * pieceSize;
            long endPos = Math.min(startPos + pieceSize, Length);
            if (endPos - startPos == pieceSize) {
                BufferMap[i] = false;
            }
        }
    }

    public String getName(){
        return Name;
    }

    public long getLength(){
        return Length;
    }

    public int getPieceSize(){
        return pieceSize;
    }

    public String getKey() {
        if (Key != null) {
            return Key.split("\\s+")[0]; // Split the string by whitespace and return the first part
        }
        return null;
    }

    public boolean[] getBufferMap(){
        return BufferMap;
    }

    public void setBufferMap(int index, boolean digit){
        if(index > BufferMap.length){
            System.out.println("index > length");
        }else{
            BufferMap[index] = digit;
        }
    }

    public void setData(int index, byte[] data, int counter , int size ){

        try {
            RandomAccessFile raf = new RandomAccessFile(Path, "rw");
            raf.seek(index*pieceSize);
            raf.write(data);
            raf.close();

            System.out.println("Bytes written successfully at position " + index*pieceSize + " percantage downloaded "+(counter*100)/size+"%");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] getData(int index) {
        byte[] data = new byte[pieceSize]; // Initialize byte array to store data

        try (RandomAccessFile raf = new RandomAccessFile(Path, "r")) {
            raf.seek(index * pieceSize); // Move to the desired position
            raf.readFully(data); // Read pieceSize bytes of data into the array
            return data;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public int getPieceCount() {
        return (int) Math.ceil((double) Length / pieceSize);
    }

    public boolean hasPiece(int index) {
        if (index >= 0 && index < BufferMap.length) {
            return BufferMap[index];
        }
        return false;
    }

    public static void main(String[] args) {
        FileHandle f = new FileHandle("file1.txt", 3);
        //System.out.println(f.Key);
        byte[] p = {8};
        // f.setData(5, p);
    }

}
