#include <iostream>
#include <stdio.h>
#include "../include/ConnectionHandler.h"
#include <atomic>
#include <mutex>
#include <thread>
#include <map>
#include <vector>
#include <stdexcept>

#define COMMAND 0
#define ACTION 0
#define HOST_ADDRESS 1
#define TOPIC 1
#define USERNAME 2
#define PASSWORD 3

using std::string;
using std::cout;
using std::endl;

std::mutex receiptMutex;
ConnectionHandler * connect(string& host,string& port);
void addReceipt(int rec,string action,std::map<int,string>& receipts);
string removeReceipt(int rec,std::map<int,string>& receipts);
void threadLoop(ConnectionHandler& ch,std::atomic<bool>& shouldTerminate,std::map<int,string>& receipts);
void handleLogin(string& hostInfo,string& username,string &password,ConnectionHandler &ch);
void handleJoin(string game,int subId,int receiptId, ConnectionHandler &ch);
void handleExit(int subId,int receiptId,ConnectionHandler &ch);
void handleLogout(int receiptId,ConnectionHandler &ch);
std::vector<string> parseCommand(string& input,int len);
std::vector<string> split(string input);

int main(int argc, char *argv[]) {
	
	bool isConnected = false;
	std::atomic<bool> shouldTerminate(false);
	ConnectionHandler * con = nullptr;
	const short bufsize = 1024;
    char buf[bufsize];
	int receiptIdGen = 0;
	int subIdGen = 0;

	std::map<int, std::string> reciepts;
	std::map<string,int> subscriptions;
	
	while(!isConnected)
	{
		try
		{
        	std::cin.getline(buf, bufsize);
			std::string line(buf);
			int len=line.length();

			std::vector<string> command = parseCommand(line,len);
			string comm= command[COMMAND];
			if(comm != "login")
				std::cout << "You first must use login before using any other commands" << std::endl;
			else
			{
				std::vector<string> hostInfo = split(command[HOST_ADDRESS]);
				con = connect(hostInfo[0],hostInfo[1]);
				handleLogin(command[HOST_ADDRESS],command[USERNAME],command[PASSWORD],*con);
				isConnected = true;
			}
			
		}
		catch(const std::exception& e)
		{
			std::cerr << e.what() << '\n';
			shouldTerminate = true;
		}
	}
	
	
	std::thread t(threadLoop,std::ref(*con),std::ref(shouldTerminate),std::ref(reciepts));
	
	while(!shouldTerminate)
	{
		try
		{
			std::cin.getline(buf, bufsize);
			std::string line(buf);
			int len=line.length();

			std::vector<string> command = parseCommand(line,len);
			string comm= command[COMMAND];
			if(comm=="login")
				handleLogin(command[HOST_ADDRESS],command[USERNAME],command[PASSWORD],*con);
			else if(comm =="join")
			{
				subscriptions[command[TOPIC]] = subIdGen;
				addReceipt(receiptIdGen,"SUB:" + command[TOPIC],reciepts);
				handleJoin(command[TOPIC],subIdGen,receiptIdGen,*con);
				subIdGen++;
				receiptIdGen++;
			}
			else if(comm == "exit")
			{
				addReceipt(receiptIdGen,"UNSUB:" + command[TOPIC],reciepts);
				handleExit(subscriptions[command[TOPIC]],receiptIdGen,*con);
				receiptIdGen++;
			}
			else if(comm == "report")
			{
				//send logic - will get to events later.
			}
			else if(comm == "logout")
			{
				addReceipt(receiptIdGen,"LOGOUT:LOGOUT",reciepts);
				handleLogout(receiptIdGen,*con);
				receiptIdGen++;
				break;

			}
			
		}
		catch(const std::exception& e)
		{
			std::cerr << e.what() << '\n';
			shouldTerminate = true;
		}
	}

	t.join();
	con->close(); //terminating
	delete con;
	
	return 0;
}

ConnectionHandler * connect(string& host,string& port) {

	cout << "Connecting to " + host + " on port: " + port << endl;
	ConnectionHandler *ch = new ConnectionHandler(host, std::stoi(port));

    if (!ch->connect()) {
        std::cerr << "Cannot connect to " << host << ":" << port << std::endl;
		delete ch;
        throw std::runtime_error("Cannot connect to " + host + ":" + port);
    }
	
	return ch;
}
/*
Adds a receipt to the common map, locking the mutex and frees it.
*/
void addReceipt(int rec,string action,std::map<int,string>& receipts)
{
	std::lock_guard<std::mutex> lock(receiptMutex);
	receipts[rec] = action;
}
/*
Removes a receipt from common map and returns it, locking the mutex and frees it.
*/
string removeReceipt(int rec,std::map<int,string>& receipts)
{
	std::lock_guard<std::mutex> lock(receiptMutex);
	string res = receipts[rec];
	receipts.erase(rec);

	return res;
}

void threadLoop(ConnectionHandler& ch,std::atomic<bool>& shouldTerminate,std::map<int,string>& receipts)
{
	while(!shouldTerminate)
	{
		try
		{
			string mes;
			if (!ch.getFrameAscii(mes,'\0')) {
            	cout << "Disconnected. Exiting...\n" << endl;
            	shouldTerminate = true;
        	}
			string frameType;
			
			size_t pos = mes.find('\n');
			frameType = mes.substr(0, pos);
			cout << "------------------" << endl;
			cout << mes << endl;
			if(frameType == "ERROR")
			{
				cout << "Conncetion is terminated" << endl;
				shouldTerminate = true;
			}
			else if(frameType == "CONNECTED")
			{
				cout <<"Login successful" << endl;
			}
			else if(frameType == "RECEIPT")
			{
				int i = mes.find("receipt-id:") + 11;
				int j = mes.find('\n', i);
				int receiptNum = std::stoi(mes.substr(i, j - i));
				std::vector<string> action = split(removeReceipt(receiptNum,receipts));

				if(action[ACTION] == "SUB")
					cout << "Joined channel " + action[TOPIC] << endl;
				else if(action[ACTION] == "UNSUB")
					cout << "Exited channel " + action[TOPIC] << endl;
				else
				{
					cout << "Disconnected from the server" << endl;
					shouldTerminate = true;
				}
			}
			cout << "------------------" << endl;
		}
		catch(const std::exception& e)
		{
				std::cerr << e.what() << '\n';
				shouldTerminate = true;
		}

	}
}


void handleLogin(string& hostInfo,string& username,string &password,ConnectionHandler &ch)
{
	string host = "";
    int i = 0;

    while (hostInfo[i] != ':') {
        host += hostInfo[i];
        i++;
    }
	string frame = "CONNECT\naccept-version:1.2\nhost:"+host +"\nlogin:" + username +"\npasscode:" + password + "\n\n\0";
	if (!ch.sendFrameAscii(frame, '\0')) 
        throw std::runtime_error("A frame could not be sent to the server - shuting down");
    			
        
}

void handleJoin(string game,int subId,int receiptId, ConnectionHandler &ch)
{
	string frame = "SUBSCRIBE\nid:"+ std::to_string(subId)+ "\ndestination:/" + game + "\nreceipt:"+std::to_string(receiptId)+"\n\n\0";
	if (!ch.sendFrameAscii(frame, '\0'))  
        throw std::runtime_error("A frame could not be sent to the server - shuting down");
	
}

void handleExit(int subId,int receiptId,ConnectionHandler &ch)
{
	string frame = "UNSUBSCRIBE\nid:" + std::to_string(subId) + "\nreceipt:" + std::to_string(receiptId) +"\n\n\0";
	if (!ch.sendFrameAscii(frame, '\0'))  
        throw std::runtime_error("A frame could not be sent to the server - shuting down");

}

void handleLogout(int receiptId,ConnectionHandler &ch)
{
	string frame = "DISCONNECT\nreceipt:" + std::to_string(receiptId) +"\n\n\0";
	if (!ch.sendFrameAscii(frame, '\0'))  
        throw std::runtime_error("A frame could not be sent to the server - shuting down");
}

std::vector<string> parseCommand(string& input,int len)
{
	std::vector<string> res;
	string word ="";
	for(int i=0;i<len;i++)
	{
		if(input[i] == ' ')
		{
			res.push_back(word);
			word.clear();
		}
		else
		{
			word+=input[i];
		}
	}

	res.push_back(word);
	return res;
}

/*
Method that splits a string by ':' , assumes it exist in the string.
*/
std::vector<string> split(string input)
{
	std::vector<std::string> res;
    std::string cur = "";

    for (int i = 0; i < input.size(); i++) {
        if (input[i] == ':') {
            res.push_back(cur);
            cur = "";
        } else {
            cur += input[i];
        }
    }
    res.push_back(cur);
    return res;
}

