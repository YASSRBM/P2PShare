#include "handle_tracker.hpp"
#include <thread>
#include <mutex>
#include <condition_variable>
#include <queue>

#define MAX_JOBS 1000
#define NUM_THREADS 10

class Job {
public:
    int socket;
    Job(int sock) : socket(sock) {}
};

class JobQueue {
private:
    std::queue<Job> jobQueue;
    std::mutex queueMutex;
    std::condition_variable queueCondition;
    
public:
    void enqueueJob(const Job &job) {
        std::unique_lock<std::mutex> lock(queueMutex);
        queueCondition.wait(lock, [this] { return jobQueue.size() < MAX_JOBS; });
        jobQueue.push(job);
        queueCondition.notify_one();
    }
    
    Job dequeueJob() {
        std::unique_lock<std::mutex> lock(queueMutex);
        queueCondition.wait(lock, [this] { return !jobQueue.empty(); });
        Job job = jobQueue.front();
        jobQueue.pop();
        queueCondition.notify_one();
        return job;
    }
};

void workerThread(JobQueue &jobQueue, Tracker &tracker) {
    std::thread::id threadId = std::this_thread::get_id();
    while (true) {
        Job job = jobQueue.dequeueJob();
        int client_socket = job.socket;

        std::cout << "Thread " << threadId << " is handling a new job\n";

        while (true) {
            char buffer[1024] = {0};
            int valread = read(client_socket, buffer, 1024);

            if (valread <= 0) {
                break;
            }

            buffer[strcspn(buffer, "\n")] = '\0';
            std::cout << "Thread " << threadId << " - Received: " << buffer << "\n";

            // Use the shared Tracker instance to handle different commands
            if (strncmp(buffer, "announce", 8) == 0) {
                tracker.handleAnnounce(client_socket, buffer);
            } else if (strncmp(buffer, "look", 4) == 0) {
                tracker.handleLook(client_socket, buffer);
            } else if (strncmp(buffer, "getfile", 7) == 0) {
                tracker.handleGetFile(client_socket, buffer);
            } else if (strncmp(buffer, "info", 4) == 0) {
                tracker.handleGetFileInfoFromTracker(client_socket, buffer);
            } else if (strncmp(buffer, "update", 6) == 0) {
                tracker.handleUpdate(client_socket, buffer);
            }
        }

        std::cout << "Thread " << threadId << " - Connection closed\n";
        close(client_socket);
    }
}


int main() {
    int server_fd, new_socket;
    struct sockaddr_in address;
    int opt = 1;
    int addrlen = sizeof(address);
    std::vector<std::thread> threads;
    JobQueue jobQueue;
    Tracker tracker;

    if ((server_fd = socket(AF_INET, SOCK_STREAM, 0)) == 0) {
        perror("socket failed");
        exit(EXIT_FAILURE);
    }

    if (setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR | SO_REUSEPORT, &opt, sizeof(opt))) {
        perror("setsockopt");
        exit(EXIT_FAILURE);
    }

    address.sin_family = AF_INET;
    address.sin_addr.s_addr = INADDR_ANY;
    address.sin_port = htons(8080);

    if (bind(server_fd, (struct sockaddr *)&address, sizeof(address)) < 0) {
        perror("bind failed");
        exit(EXIT_FAILURE);
    }

    if (listen(server_fd, 3) < 0) {
        perror("listen");
        exit(EXIT_FAILURE);
    }

    //init();

    for (int i = 0; i < NUM_THREADS; i++) {
        threads.emplace_back(workerThread, std::ref(jobQueue), std::ref(tracker));
    }

    std::cout << "Tracker is waiting for connections...\n";

    while (true) {
        if ((new_socket = accept(server_fd, (struct sockaddr *)&address, (socklen_t *)&addrlen)) < 0) {
            perror("accept");
            exit(EXIT_FAILURE);
        }

        Job newJob(new_socket);
        jobQueue.enqueueJob(newJob);
    }

    return 0;
}
