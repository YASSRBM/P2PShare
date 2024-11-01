package peer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class INIReader {
    private String filePath;

    public INIReader(String path) {
        filePath = path;
    }

    public Map<String, String> readIniFile(String filePath) throws IOException {
        Map<String, String> iniValues = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Ignore comments and empty lines
                if (line.isEmpty() || line.startsWith(";") || line.startsWith("#")) {
                    continue;
                }

                int equalsIndex = line.indexOf('=');
                if (equalsIndex != -1) {
                    String key = line.substring(0, equalsIndex).trim();
                    String value = line.substring(equalsIndex + 1).trim();
                    iniValues.put(key, value);
                }
            }
        }

        return iniValues;
    }

    public String[] getIPs() {
        try {
            Map<String, String> iniValues = readIniFile(filePath);

            // Get the values for tracker-address and tracker-port
            String Tracker_IP = iniValues.get("tracker-address");
            String Peer_IP = iniValues.get("peer-address");
            String[] IPs = { Tracker_IP, Peer_IP };

            return IPs;

        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] err = { "error" };
        return err;
    }

    public int[] getPorts() {
        try {
            Map<String, String> iniValues = readIniFile(filePath);

            // Get the values for tracker-address and tracker-port
            int Tracker_Port = Integer.parseInt(iniValues.get("tracker-port"));
            int Peer_Port = Integer.parseInt(iniValues.get("peer-port"));
            int[] Ports = { Tracker_Port, Peer_Port };

            return Ports;

        } catch (IOException e) {
            e.printStackTrace();
        }
        int[] err = { 0 };
        return err;
    }

    public int getPieceSize() {
        try {
            Map<String, String> iniValues = readIniFile(filePath);

            // Get the values for tracker-address and tracker-port
            int len = Integer.parseInt(iniValues.get("piece-size"));

            return len;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public String getpathtoFiles() {
        try {
            Map<String, String> iniValues = readIniFile(filePath);

            // Get the values for tracker-address and tracker-port
            String path = iniValues.get("path-to-files");
            return path;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "error";
    }

    public int gettimeslice() {
        try {
            Map<String, String> iniValues = readIniFile(filePath);

            // Get the values for tracker-address and tracker-port
            int time = Integer.parseInt(iniValues.get("time-slice"));
            return time;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
