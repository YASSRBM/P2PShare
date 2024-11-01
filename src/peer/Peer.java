package peer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;

public class Peer implements PeerInterface {
    private ServerSocket serverSocket;
    private List<Thread> connectionThreads;
    private String Peer_IP;
    private int Peer_Port;
    private String Tracker_IP;
    private int Tracker_Port;
    private ArrayList<FileHandle> Files;
    private StringBuilder leech;
    private String PathtoFiles;
    private INIReader reader;

    // private ArrayList peers_connected;
    class PeerInfo {
        String ip;
        int port;
        boolean[] bufferMap;

        public PeerInfo(String ip, int port) {
            this.ip = ip;
            this.port = port;
            this.bufferMap = null;
        }

        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            PeerInfo peerInfo = (PeerInfo) obj;
            return port == peerInfo.port && Objects.equals(ip, peerInfo.ip);
        }

    }

    // map les clés des fichiers aux pairs qui ont ces fichiers
    private Map<String, List<PeerInfo>> peerMap = new HashMap<>();

    public List<PeerInfo> getUniquePeers() {
        List<PeerInfo> uniquePeers = new ArrayList<>();

        // Utilisation d'un ensemble pour garder une trace des PeerInfo déjà ajoutés
        Set<PeerInfo> seenPeers = new HashSet<>();

        // Parcours de chaque entrée dans peerMap
        for (List<PeerInfo> peerInfos : peerMap.values()) {
            // Parcours de chaque PeerInfo dans la liste
            for (PeerInfo peerInfo : peerInfos) {
                // Vérifie si le PeerInfo est déjà vu
                if (!seenPeers.contains(peerInfo)) {
                    // Ajoute le PeerInfo à la liste des PeerInfo uniques
                    uniquePeers.add(peerInfo);
                    // Ajoute le PeerInfo à l'ensemble des PeerInfo déjà vus
                    seenPeers.add(peerInfo);
                }
            }
        }

        return uniquePeers;
    }

    public Peer(String configFile, String[] filesPaths) {
        reader = new INIReader(configFile);
        String[] IPs = reader.getIPs();
        int[] Ports = reader.getPorts();
        int piece_size = reader.getPieceSize();
        PathtoFiles = reader.getpathtoFiles();
        Tracker_IP = IPs[0];
        Peer_IP = IPs[1];
        Tracker_Port = Ports[0];
        Peer_Port = Ports[1];
        leech = new StringBuilder();
        leech.append("[ ");
        Files = new ArrayList<>(); // Initialiser le tableau Files
        for (int i = 0; i < filesPaths.length; i++) {
            Files.add(new FileHandle(filesPaths[i], piece_size));
            // System.out.println("========File "+i+" : "+Files[i]);
        }
    }

    public void runUpdatePeriodically(long timeInterval) {
        while (true) {
            try {
                // Exécute la fonction update
                Update();
                // Pause pour l'intervalle de temps spécifié
                Thread.sleep(timeInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void runHavePeriodically(long interval) {
        try {
            while (true) {
                for (String key : peerMap.keySet()) {
                    FileHandle fileHandle = getFileByKey(key);
                    if (fileHandle != null) {
                        have(key);
                    }
                }
                Thread.sleep(interval);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        // Start the server thread
        Thread serverThread = new Thread(() -> startServer());
        serverThread.start();

        // Start the client thread
        Thread clientThread = new Thread(() -> startClient());
        clientThread.start();

        Thread thread = new Thread(() -> runUpdatePeriodically(3000));

        thread.start();
        Thread havethread = new Thread(() -> runHavePeriodically(reader.gettimeslice()));

    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(Peer_Port);
            connectionThreads = new ArrayList<>();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                Thread connectionThread = new Thread(() -> handleConnection(clientSocket));
                connectionThreads.add(connectionThread);
                connectionThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void printLogo() {
        String logo =
                "                _____ _      _                    \n" +
                "               | ____(_)_ __| |__   ___  _ __   ___\n" +
                "               |  _| | | '__| '_ \\ / _ \\| '_ \\ / _ \\\n" +
                "               | |___| | |  | |_) | (_) | | | |  __/\n" +
                "               |_____|_|_|  |_.__/ \\___/|_| |_|\\___|\n";
    
        // ANSI escape codes for colors
        String red = "\u001B[31m";
        String reset = "\u001B[0m";
    
        System.out.println(red + logo + reset);
    }
    
    private void startClient() {
        printLogo();
        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;
            while (running) {
                System.out.print("> ");
                String command = scanner.nextLine().trim();
                String[] parts = command.split(" ");

                switch (parts[0].toLowerCase()) {
                    case "announce":
                        Announce();
                        break;
                    case "look":
                        if (parts.length > 1) {
                            String[] criteria = Arrays.copyOfRange(parts, 1, parts.length);
                            Look(criteria);
                        } else {
                            System.out.println("Usage: look <criteria1> <criteria2> ...");
                        }
                        break;
                    case "getfile":
                        if (parts.length == 2) {
                            String fileKey = parts[1];
                            String peerInfo = getFile(fileKey);
                            if (peerInfo != null) {
                                // Parse the peer information and connect to the peer
                                // ...
                            }
                        } else {
                            System.out.println("Usage: getfile <file_key>");
                        }
                        break;
                    case "interested":
                        if (parts.length == 2) {
                            String key = parts[1];
                            interested(key);
                        } else {
                            System.out.println("Usage: interested <key>");
                        }
                        break;
                    case "getpieces":
                        if (parts.length == 2) {
                            String key = parts[1];
                            getPieces(key);
                        } else {
                            System.out.println("Usage: getpieces <key>");
                        }
                        break;
                    case "update":
                        Update();
                    case "quit":
                        running = false;
                        break;
                    default:
                        System.out.println("Unknown command: " + command);
                        break;
                }
            }
        }
    }

    private boolean[] convertBufferMapFromString(String bufferMapStr, int originalLength) {
        // Remove the '%' characters from the beginning and end of the string
        bufferMapStr = bufferMapStr.substring(1, bufferMapStr.length() - 1);

        // Convert the string to a byte array
        byte[] bufferMapBytes = Base64.getDecoder().decode(bufferMapStr);

        // Create a boolean array to store the buffer map
        boolean[] bufferMap = new boolean[originalLength];

        // Convert each byte to 8 boolean values
        for (int i = 0; i < bufferMapBytes.length; i++) {
            for (int j = 0; j < 8 && i * 8 + j < originalLength; j++) {
                bufferMap[i * 8 + j] = ((bufferMapBytes[i] >> (7 - j)) & 1) == 1;
            }
        }

        return bufferMap;
    }

    private String convertBufferMapToString(boolean[] bufferMap) {
        // Convertir le tableau de booléens en un tableau d'octets
        byte[] bufferMapBytes = new byte[(bufferMap.length + 7) / 8];
        for (int i = 0; i < bufferMap.length; i++) {
            if (bufferMap[i]) {
                bufferMapBytes[i / 8] |= 1 << (7 - (i % 8));
            }
        }

        // Encoder le tableau d'octets en une chaîne de caractères Base64
        String bufferMapStr = Base64.getEncoder().encodeToString(bufferMapBytes);

        // Ajouter les caractères '%' au début et à la fin de la chaîne
        bufferMapStr = "%" + bufferMapStr + "%";

        return bufferMapStr;
    }

    private void handleConnection(Socket clientSocket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

            String message;
            while ((message = reader.readLine()) != null) {
                String[] parts = message.split(" ");
                String command = parts[0].toLowerCase();

                switch (command) {
                    case "interested":
                        if (parts.length == 2) {
                            String key = parts[1];
                            handleInterested(key, clientSocket);
                        } else {
                            writer.write("Invalid interested message format");
                            writer.newLine();
                            writer.flush();
                        }
                        break;
                    case "getpieces":
                        if (parts.length >= 3) {
                            handleGetPieces(message, clientSocket);
                        } else {
                            writer.write("Invalid getpieces message format");
                            writer.newLine();
                            writer.flush();
                        }
                        break;
                    case "have":
                        handleHave(message, clientSocket);
                    default:
                        writer.write("Unknown command: " + command);
                        writer.newLine();
                        writer.flush();
                        break;
                }
            }

            reader.close();
            writer.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String Announce() {
        try {
            // Créer un socket pour se connecter au tracker
            Socket socket = new Socket(Tracker_IP, Tracker_Port);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Construire le message d'annonce
            StringBuilder message = new StringBuilder("announce listen ");
            message.append(Peer_Port).append(" seed [");
            for (FileHandle file : Files) {
                message.append(file.getName()).append(" ").append(file.getLength()).append(" ")
                        .append(file.getPieceSize()).append(" ").append(file.getKey()).append(" ");
            }
            message.deleteCharAt(message.length() - 1); // Supprimer le dernier espace
            message.append("]");
            System.out.println("Message d'annonce : " + message);
            // Envoyer le message au tracker
            writer.write(message.toString());
            writer.newLine();
            writer.flush();

            // Lire la réponse du tracker
            String response = reader.readLine();
            System.out.println("Réponse du tracker : " + response);

            // Fermer les flux et le socket
            writer.close();
            reader.close();
            socket.close();
            return response;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void Look(String[] criteria) {
        try {
            // Créer un socket pour se connecter au tracker
            Socket socket = new Socket(Tracker_IP, Tracker_Port);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Construire le message de recherche
            StringBuilder message = new StringBuilder("look [");
            for (String criterion : criteria) {
                message.append(criterion).append(" ");
            }
            message.deleteCharAt(message.length() - 1); // Supprimer le dernier espace
            message.append("]");

            System.out.println("Message de recherche : " + message);
            // Envoyer le message au tracker
            writer.write(message.toString());
            writer.newLine();
            writer.flush();

            // Lire la réponse du tracker
            String response = reader.readLine();
            System.out.println("Réponse du tracker apres look : " + response);

            // Fermer les flux et le socket
            writer.close();
            reader.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getFile(String key) {
        try {
            // Créer un socket pour se connecter au tracker
            Socket socket = new Socket(Tracker_IP, Tracker_Port);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Construire le message d'annonce
            StringBuilder message = new StringBuilder("getfile ");
            message.append(key);
            System.out.println("Message getfile : " + message);
            // Envoyer le message au tracker
            writer.write(message.toString());
            writer.newLine();
            writer.flush();

            // Lire la réponse du tracker
            String response = reader.readLine();
            System.out.println("Réponse du tracker : " + response);

            // Analyser la réponse pour obtenir la liste des pairs
            // Analyser la réponse pour obtenir la liste des pairs
            String[] parts = response.split(" ");
            if (parts.length >= 3 && parts[0].equals("peers")) {
                String[] peerList = parts[2].split("\\[|\\]");

                // Stocker les pairs dans la variable peerMap
                List<PeerInfo> peers = new ArrayList<>();
                for (String peerInfo : peerList) {
                    if (!peerInfo.isEmpty()) {
                        String[] peerData = peerInfo.split(":");
                        String peerIP = peerData[0];
                        int peerPort = Integer.parseInt(peerData[1]);
                        peers.add(new PeerInfo(peerIP, peerPort));
                    }
                }
                peerMap.put(key, peers);
            }
            // Fermer les flux et le socket
            writer.close();
            reader.close();
            socket.close();
            return response;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void debugPeerInfo(String key) {
        List<PeerInfo> peers = peerMap.get(key);
        if (peers != null) {
            System.out.println("Peer information for key: " + key);
            for (PeerInfo peer : peers) {
                System.out.println("IP: " + peer.ip);
                System.out.println("Port: " + peer.port);
                System.out.println("Buffer Map: " + Arrays.toString(peer.bufferMap));
                System.out.println("------------------------");
            }
        } else {
            System.out.println("No peer information found for key: " + key);
        }
    }

    public void interested(String key) {
        List<PeerInfo> peers = peerMap.get(key);
        if (peers != null) {
            for (PeerInfo peer : peers) {
                try {
                    // Créer un socket pour se connecter au pair
                    Socket peerSocket = new Socket(peer.ip, peer.port);
                    BufferedWriter peerWriter = new BufferedWriter(
                            new OutputStreamWriter(peerSocket.getOutputStream()));

                    // Construire le message "interested"
                    String interestedMessage = "interested " + key;

                    // Envoyer le message au pair
                    peerWriter.write(interestedMessage);
                    peerWriter.newLine();
                    peerWriter.flush();

                    // Lire la réponse du pair
                    BufferedReader peerReader = new BufferedReader(new InputStreamReader(peerSocket.getInputStream()));
                    String response = peerReader.readLine();
                    System.out.println("Réponse du pair : " + response);

                    // Analyser la réponse pour obtenir le buffermap et la longueur originale
                    String[] parts = response.split(" ");
                    if (parts.length == 4 && parts[0].equals("have")) {
                        String bufferMapStr = parts[2];
                        int originalLength = Integer.parseInt(parts[3]);

                        boolean[] bufferMap = convertBufferMapFromString(bufferMapStr, originalLength);
                        peer.bufferMap = bufferMap;
                    }

                    // Fermer le socket, le reader et le writer
                    peerReader.close();
                    peerWriter.close();
                    peerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void have(String key) {
        FileHandle file = getFileByKey(key);
        List<PeerInfo> peers = peerMap.get(key);
        boolean[] bufferMap1 = file.getBufferMap();
        for (PeerInfo peer : peers) {
            try {
                // Créer un socket pour se connecter au pair
                Socket peerSocket = new Socket(peer.ip, peer.port);
                BufferedWriter peerWriter = new BufferedWriter(
                        new OutputStreamWriter(peerSocket.getOutputStream()));

                // Construire le message "interested"
                String haveMessage = "have " + key + " " + convertBufferMapToString(bufferMap1) + " "
                        + bufferMap1.length;

                // Envoyer le message au pair
                peerWriter.write(haveMessage);
                peerWriter.newLine();
                peerWriter.flush();

                // Lire la réponse du pair
                BufferedReader peerReader = new BufferedReader(new InputStreamReader(peerSocket.getInputStream()));
                String response = peerReader.readLine();

                // Analyser la réponse pour obtenir le buffermap et la longueur originale
                String[] parts = response.split(" ");
                if (parts.length == 4 && parts[0].equals("have")) {
                    String bufferMapStr = parts[2];
                    int originalLength = Integer.parseInt(parts[3]);

                    boolean[] bufferMap = convertBufferMapFromString(bufferMapStr, originalLength);
                    peer.bufferMap = bufferMap;
                    System.out.println(
                            "BufferMap du pair affecté dans la fonction Interested: " + Arrays.toString(bufferMap));
                }

                // Fermer le socket, le reader et le writer
                peerReader.close();
                peerWriter.close();
                peerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void handleInterested(String key, Socket socket) {
        // Rechercher le fichier correspondant à la clé
        System.out.println("this function is being called ");
        FileHandle file = null;
        for (FileHandle f : Files) {
            if (f.getKey().equals(key)) {
                file = f;
                break;
            }
        }

        if (file != null) {
            // Créer le buffermap pour le fichier
            boolean[] bufferMap = new boolean[file.getPieceCount()];
            for (int i = 0; i < file.getPieceCount(); i++) {
                bufferMap[i] = file.hasPiece(i);
            }

            // Convertir le buffermap en une chaîne de caractères
            String bufferMapStr = convertBufferMapToString(bufferMap);

            try {
                // Créer un writer pour envoyer la réponse au pair intéressé
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                // Construire le message "have" avec la longueur originale du bufferMap
                String message = "have " + key + " " + bufferMapStr + " " + bufferMap.length;
                System.out.println("Message have is ~~~ : " + message);

                // Envoyer le message au pair intéressé
                writer.write(message);
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static byte[] convertBinaryStringToByteArray(String binaryString) {
        byte[] byteArray = new byte[binaryString.length()];
        for (int i = 0; i < binaryString.length(); i++) {
            // Convert character '0' to byte 0 and character '1' to byte 1
            byteArray[i] = (byte) (binaryString.charAt(i) - '0');
        }
        return byteArray;
    }

    public static String convertByteArrayToBinaryString(byte[] byteArray) {
        StringBuilder binaryStringBuilder = new StringBuilder();
        for (byte b : byteArray) {
            // Convert the byte to an unsigned integer and then to a binary string
            String binary = Integer.toBinaryString(Byte.toUnsignedInt(b));
            // Pad the binary string with leading zeros to ensure it has 8 bits
            binary = String.format("%s", binary).replace(' ', '0');
            // Append the binary string to the StringBuilder
            binaryStringBuilder.append(binary);
        }
        return binaryStringBuilder.toString();
    }

    private FileHandle getFileByKey(String key) {
        // printin the key to debug
        for (FileHandle file : Files) {
            // printing the key of the file to debug
            if (file.getKey().equals(key)) {
                return file;
            }
        }
        return null;
    }

    public void getPieces(String key) {
        try {
            // Rechercher le fichier correspondant à la clé
            FileHandle file = getFileByKey(key);

            // Ajouter la clé à la liste des fichiers en cours de télechargement
            leech.append(key).append(" ");

            // Si le fichier n'existe pas, le créer et l'ajouter à la liste des fichiers
            if (file == null) {
                // Obtenir les informations du fichier depuis le Tracker
                String[] fileInfo = getFileInfoFromTracker(key);
                if (fileInfo != null) {
                    String fileName = fileInfo[0];
                    long fileSize = Long.parseLong(fileInfo[1]);
                    int pieceSize = Integer.parseInt(fileInfo[2]);

                    // Créer un nouveau FileHandle avec les informations du fichier
                    file = new FileHandle(PathtoFiles + fileName, pieceSize, fileSize);
                    file.Length = fileSize;
                    file.Key = key;
                    // Ajouter le fichier à la liste des fichiers
                    Files.add(file);
                } else {
                    System.out.println("File information not found on the Tracker.");
                    return;
                }
            }

            // Obtenir la liste des pairs possédant le fichier
            List<PeerInfo> peers = peerMap.get(key);

            // Parcourir tous les pairs possédant le fichier
            for (PeerInfo peer : peers) {
                // Comparer les buffermaps du pair et du fichier local
                boolean[] peerBufferMap = peer.bufferMap;
                boolean[] localBufferMap = file.getBufferMap();
                // System.out.println("peerBufferMap : "+Arrays.toString(peerBufferMap));
                // System.out.println("localBufferMap : "+Arrays.toString(localBufferMap));
                // // printing lengths of these buffermaps
                // System.out.println("peerBufferMap length : "+peerBufferMap.length);
                // System.out.println("localBufferMap length : "+localBufferMap.length);
                // Vérifier que les buffermaps ont la même taille
                if (peerBufferMap.length == localBufferMap.length) {
                    List<Integer> indicesToRequest = new ArrayList<>();

                    // Parcourir les index des buffermaps
                    for (int i = 0; i < peerBufferMap.length; i++) {
                        // Si le pair possède la pièce mais pas le fichier local, ajouter l'index à la
                        // liste
                        if (peerBufferMap[i] && !localBufferMap[i]) {
                            indicesToRequest.add(i);
                        }
                    }
                    System.out.println("indicesToRequest : " + indicesToRequest);
                    if (indicesToRequest.size() > 0) {
                        // Envoyer le message "getpieces" au pair avec les index manquants
                        sendGetPiecesMessage(peer, key, indicesToRequest);
                    }
                }

            }
            int index = leech.indexOf(key);
            while (index != -1) {
                leech.delete(index, index + key.length());
                index = leech.indexOf(key);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendGetPiecesMessage(PeerInfo peer, String key, List<Integer> indices) {
        // Créer un socket pour se connecter au pair
        try (Socket peerSocket = new Socket(peer.ip, peer.port);
                BufferedWriter peerWriter = new BufferedWriter(new OutputStreamWriter(peerSocket.getOutputStream()));
                BufferedReader peerReader = new BufferedReader(new InputStreamReader(peerSocket.getInputStream()))) {

            // Construire le message "getpieces"
            StringBuilder message = new StringBuilder();
            message.append("getpieces ").append(key).append(" [");
            for (int i = 0; i < indices.size(); i++) {
                message.append(indices.get(i));
                if (i < indices.size() - 1) {
                    message.append(" ");
                }
            }
            message.append("]");
            // Envoyer le message au pair
            peerWriter.write(message.toString());
            peerWriter.newLine();
            peerWriter.flush();

            // Lire la réponse du pair
            String response = peerReader.readLine();
            // adding the original size of indices to the message
            response = response + " " + (indices.size() + "\n");
            handleGetPiecesResponse(response, getFileByKey(key));
            // close the reader and writer
            peerReader.close();
            peerWriter.close();
            peerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleGetPiecesResponse(String response, FileHandle file) {
        // Vérifier que la réponse commence par "data"
        if (response.startsWith("data")) {

            String[] parts = response.split(" ", 3);
            if (parts.length == 3) {
                String key = parts[1];
                String piecesData = parts[2];

                String[] parts2 = piecesData.split("]");
                piecesData = parts2[0];
                String originalSize = parts2[1].substring(1, parts2[1].length() - 1);
                int size = Integer.parseInt(originalSize);

                piecesData = piecesData.substring(1, piecesData.length());
                int counter = 1;
                // Diviser les données des pièces en morceaux individuels
                String[] pieceParts = piecesData.split(" ");
                for (String piecePart : pieceParts) {
                    String[] pieceInfo = piecePart.split(":");
                    if (pieceInfo.length == 2) {
                        int index = Integer.parseInt(pieceInfo[0]);
                        String pieceData = pieceInfo[1];

                        // Décoder les données de la pièce de Base64 en tableau d'octets
                        byte[] pieceBytes = Base64.getDecoder().decode(pieceData);
                        file.setData(index, pieceBytes, counter, size);
                        counter++;
                        file.setBufferMap(index, true);
                    }
                }
            }
        }
    }

    public void handleGetPieces(String message, Socket socket) {
        String[] parts = message.split(" ", 3);
        if (parts.length == 3) {
            String key = parts[1];
            String indicesStr = parts[2];

            // Supprimer les crochets [ ] autour des indices
            indicesStr = indicesStr.substring(1, indicesStr.length() - 1);

            // Diviser les indices en un tableau
            String[] indicesArray = indicesStr.split(" ");

            // Rechercher le fichier correspondant à la clé
            FileHandle file = getFileByKey(key);

            if (file != null) {
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
                    // Construire le message "data"
                    StringBuilder dataMessage = new StringBuilder();
                    dataMessage.append("data ").append(key).append(" [");

                    // Parcourir les indices demandés
                    for (int i = 0; i < indicesArray.length; i++) {
                        int index = Integer.parseInt(indicesArray[i]);

                        // Vérifier si le fichier possède la pièce à l'index spécifié
                        if (file.hasPiece(index)) {
                            // Récupérer les données de la pièce
                            byte[] pieceData = file.getData(index);

                            // Encoder les données de la pièce en Base64
                            String encodedPiece = Base64.getEncoder().encodeToString(pieceData);

                            // Ajouter les informations de la pièce au message "data"
                            dataMessage.append(index).append(":").append(encodedPiece);

                            // Ajouter un espace si ce n'est pas la dernière pièce
                            if (i < indicesArray.length - 1) {
                                dataMessage.append(" ");
                            }
                        }
                    }

                    dataMessage.append("]");

                    // Envoyer le message "data" au pair
                    writer.write(dataMessage.toString());
                    writer.newLine();
                    writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void handleHave(String message, Socket socket) {
        String[] parts = message.split(" ");
        if (parts.length == 4 && parts[0].equals("have")) {
            String key = parts[1];
            String bufferMapStr = parts[2];
            int originalLength = Integer.parseInt(parts[3]);

            // Rechercher le fichier correspondant à la clé
            FileHandle file = getFileByKey(key);
            boolean[] bufferMap1 = file.getBufferMap();
            // Convertir le buffermap en une chaîne de caractères
            String buffer = convertBufferMapToString(bufferMap1);

            try {
                // Créer un writer pour envoyer la réponse au pair intéressé
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                // Construire le message "have" avec la longueur originale du bufferMap
                String send = "have " + key + " " + buffer + " " + bufferMap1.length;
                // Envoyer le message au pair intéressé
                writer.write(send);
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void Update() {
        try {
            // Créer un socket pour se connecter au tracker
            Socket socket = new Socket(Tracker_IP, Tracker_Port);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // Construire le message d'annonce
            StringBuilder message = new StringBuilder("update seed [");

            for (FileHandle file : Files) {
                message.append(file.getName()).append(" ").append(file.getLength()).append(" ")
                        .append(file.getPieceSize()).append(" ").append(file.getKey()).append(" ");
            }

            message.append("] leech ").append(leech.toString()).append("]");

            // Envoyer le message au tracker
            writer.write(message.toString());
            writer.newLine();
            writer.flush();
            // Lire la réponse du tracker
            String response = reader.readLine();

            // Fermer les flux et le socket
            writer.close();
            reader.close();
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        // return null;

    }

    private String[] getFileInfoFromTracker(String key) {
        try {
            // Établir une connexion avec le Tracker
            Socket socket = new Socket(Tracker_IP, Tracker_Port);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Envoyer la requête au Tracker pour obtenir les informations du fichier
            String request = "info " + key;
            writer.write(request);
            writer.newLine();
            writer.flush();

            // Lire la réponse du Tracker
            String response = reader.readLine();
            if (response != null) {
                System.out.println("Réponse du Tracker : " + response);
                // Vérifier si les informations du fichier sont trouvées
                if (response.startsWith("fileinfo")) {
                    // Extraire les informations du fichier de la réponse
                    String[] parts = response.split(" ");
                    if (parts.length == 5) {
                        String fileName = parts[2];
                        String fileSize = parts[3];
                        String pieceSize = parts[4];
                        return new String[] { fileName, fileSize, pieceSize };
                    }
                }
            }

            // Fermer la connexion avec le Tracker
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java project.Peer <configFile> <filePath1> <filePath2> ...");
            return;
        }

        String configFile = args[0];
        String[] filesPaths = Arrays.copyOfRange(args, 1, args.length);

        Peer peer = new Peer(configFile, filesPaths);
        peer.start();
    }
}