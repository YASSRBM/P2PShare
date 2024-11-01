#include <netinet/in.h>
#include <arpa/inet.h>
#include <iostream>
#include <vector>
#include <string>
#include <cstring>
#include <memory>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <unistd.h>
#include <sstream>



#define MAX_FILES 100
#define MAX_PEERS 100

// File and Peer classes
class Peer {
public:
    std::string peer_addr;
    int port;
    std::vector<int> leechfiles;

    Peer(std::string addr, int port_num) : peer_addr(addr), port(port_num) {}
};

class File {
public:
    std::string nom;
    std::string path;
    std::string key;
    int size;
    int piecesize;
    std::vector<std::shared_ptr<Peer>> peers;

    File(const std::string& name, const std::string& filepath, const std::string& filekey, int filesize, int psize)
        : nom(name), path(filepath), key(filekey), size(filesize), piecesize(psize) {}
};

class Files {
public:
    std::vector<std::shared_ptr<File>> files;

    void addFile(const std::shared_ptr<File> &file) {
        if (files.size() < MAX_FILES) {
            files.push_back(file);
        }
    }

    void printFiles() const {
        for (const auto &file : files) {
            std::cout << "File: " << file->nom << ", Path: " << file->path << "\n";
        }
    }

    void freeFiles() {
        files.clear();
    }
};


// Manager for file and peer lists
class Tracker {
private:
    std::vector<std::shared_ptr<File>> fileList;
    std::vector<std::shared_ptr<Peer>> peerList;

public:
    void init();
    void addFile(const std::shared_ptr<File>& file);
    void addPeer(const std::shared_ptr<Peer>& peer);
    std::shared_ptr<File> findFileByKey(const std::string& key);
    std::shared_ptr<Peer> findPeerByAddr(const std::string& addr);
    void printFile(const std::shared_ptr<File>& file);
    void printFiles();

    void handleLook(int socket, const std::string& message);
    void handleGetFile(int socket, const std::string& message);
    void handleGetFileInfoFromTracker(int clientSocket, const std::string &buffer);
    void handleAnnounce(int socket, const std::string& message);
    void handleUpdate(int socket, const std::string& message);
};
