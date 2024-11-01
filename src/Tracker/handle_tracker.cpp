#include "handle_tracker.hpp"


// Implementation of Tracker methods
void Tracker::addFile(const std::shared_ptr<File>& file) {
    if (fileList.size() >= MAX_FILES) {
        std::cerr << "Error: Max file limit reached.\n";
        return;
    }
    fileList.push_back(file);
}

void Tracker::addPeer(const std::shared_ptr<Peer>& peer) {
    if (peerList.size() >= MAX_PEERS) {
        std::cerr << "Error: Max peer limit reached.\n";
        return;
    }
    peerList.push_back(peer);
}

std::shared_ptr<File> Tracker::findFileByKey(const std::string& key) {
    for (auto& file : fileList) {
        if (file->key == key) return file;
    }
    return nullptr;
}

std::shared_ptr<Peer> Tracker::findPeerByAddr(const std::string& addr) {
    for (auto& peer : peerList) {
        if (peer->peer_addr == addr) return peer;
    }
    return nullptr;
}

void Tracker::printFile(const std::shared_ptr<File>& file) {
    std::cout << "Name: " << file->nom << "\nPath: " << file->path << "\nKey: " << file->key 
              << "\nSize: " << file->size << "\nPiece Size: " << file->piecesize << "\n";
}

void Tracker::printFiles() {
    for (const auto& file : fileList) {
        printFile(file);
    }
}

void Tracker::handleLook(int socket, const std::string& message) {
    std::string query;
    if (message.find("filename=") != std::string::npos) {
        query = message.substr(message.find("filename=") + 9);
    } else {
        std::cerr << "Error: Criteria not specified in message.\n";
        if (socket) send(socket, "Error: Criteria not specified.", 29, 0);
        return;
    }

    std::string response = "list [";
    for (const auto& file : fileList) {
        if (file->nom == query) {
            response += file->nom + " " + std::to_string(file->size) + " " + 
                        std::to_string(file->piecesize) + " " + file->key + " ";
        }
    }
    response += "]\n";
    if (socket) send(socket, response.c_str(), response.size(), 0);
}

void Tracker::handleGetFileInfoFromTracker(int clientSocket, const std::string &buffer) {
    std::cout << "About to read from client in function getinfo\n";
    std::cout << "Buffer in get info = " << buffer << "\n";
    
    std::istringstream iss(buffer);
    std::string request, key;
    iss >> request;
    
    if (request == "info" && iss >> key) {
        auto it = findFileByKey(key);
        if (it != nullptr) {
            auto file = it;
            std::ostringstream response;
            response << "fileinfo " << file->key << " " << file->nom << " " << file->size << " " << file->piecesize << "\n";
            std::string responseStr = response.str();
            
            std::cout << "Sending response: " << responseStr << "\n";
            write(clientSocket, responseStr.c_str(), responseStr.length());
        } else {
            const std::string response = "filenotfound";
            write(clientSocket, response.c_str(), response.length());
        }
    }
}

void Tracker::handleGetFile(int socket, const std::string& message) {
    auto key_pos = message.find(" ");
    if (key_pos == std::string::npos) return;

    std::string key = message.substr(key_pos + 1);
    auto file = findFileByKey(key);

    std::string response = "peers " + key + " [";
    if (file) {
        for (const auto& peer : file->peers) {
            response += peer->peer_addr + ":" + std::to_string(peer->port) + " ";
        }
    } else {
        response += "file not found";
    }
    response += "]\n";
    send(socket, response.c_str(), response.size(), 0);
}

void Tracker::handleAnnounce(int socket, const std::string& message) {
    auto peer_addr = inet_ntoa(((sockaddr_in) { .sin_family = AF_INET, .sin_port = htons(0) }).sin_addr);

    auto peer = std::make_shared<Peer>(peer_addr, std::stoi(message.substr(8)));
    addPeer(peer);

    std::string response = "ok\n";
    send(socket, response.c_str(), response.size(), 0);
}

void Tracker::handleUpdate(int socket, const std::string& message) {
    std::string addr = inet_ntoa(((sockaddr_in) { .sin_family = AF_INET, .sin_port = htons(0) }).sin_addr);
    auto peer = findPeerByAddr(addr);

    if (!peer) {
        std::cerr << "Error: Peer not found.\n";
        return;
    }
    std::string response = "ok\n";
    send(socket, response.c_str(), response.size(), 0);
}



