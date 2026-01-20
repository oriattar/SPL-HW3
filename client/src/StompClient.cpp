#pragma once
#include <iostream>
#include <stdio.h>
#include "../include/ConnectionHandler.h"
#include "../include/StompProtocol.h"
#include "../include/event.h"
#include "../include/Game.h"
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
#define FILE_PATH 1
#define USERNAME 2
#define PASSWORD 3

#define BODY_START 6
#define USER_LINE 5
#define DEST_LINE 3

using std::string;
using std::cout;
using std::endl;

std::mutex receiptMutex;
std::mutex updateMutex;
ConnectionHandler * connect(string& host,string& port);
void addReceipt(int rec,string action,std::map<int,string>& receipts);
string removeReceipt(int rec,std::map<int,string>& receipts);
void threadLoop(StompProtocol& protocol,std::map<int,string>& receipts);
std::vector<string> parseCommand(string& input,int len);
std::vector<string> split(string input);
std::vector<string> getLines(string& str);

int main(int argc, char *argv[]) {
	
	bool isConnected = false;
	ConnectionHandler * con = nullptr;
	const short bufsize = 1024;
    char buf[bufsize];
	int receiptIdGen = 0;
	int subIdGen = 0;

	std::map<int, std::string> reciepts;
	std::map<string,int> subscriptions;

	StompProtocol protocol;
	
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
				protocol.start(con,command[USERNAME]);

				protocol.handleLogin(command[HOST_ADDRESS],command[USERNAME],command[PASSWORD]);
				isConnected = true;
			}
			
		}
		catch(const std::exception& e)
		{
			std::cerr << e.what() << '\n';
			protocol.setShouldTerminate(true);
		}
	}
	
	
	std::thread t(threadLoop,std::ref(protocol),std::ref(reciepts));
	
	while(!protocol.shouldTerminate())
	{
		try
		{
			std::cin.getline(buf, bufsize);
			std::string line(buf);
			int len=line.length();
			if(protocol.shouldTerminate())
				throw std::runtime_error("Connection was closed by the server.");

			std::vector<string> command = parseCommand(line,len);
			string comm= command[COMMAND];
			if(comm=="login")
				protocol.handleLogin(command[HOST_ADDRESS],command[USERNAME],command[PASSWORD]);

			else if(comm =="join")
			{
				if(subscriptions.find(command[TOPIC])!= subscriptions.end())
					cout<< "User already subscribed to the topic." << endl;
				else
				{
					subscriptions[command[TOPIC]] = subIdGen;
					addReceipt(receiptIdGen,"SUB:" + command[TOPIC],reciepts);
					protocol.handleJoin(command[TOPIC],subIdGen,receiptIdGen);
					subIdGen++;
					receiptIdGen++;
				}
			}
			else if(comm == "exit")
			{
				if(subscriptions.find(command[TOPIC])== subscriptions.end())
					cout<< "You are subscribed to that topic." << endl;
				
				else
				{
					addReceipt(receiptIdGen,"UNSUB:" + command[TOPIC],reciepts);
					protocol.handleExit(command[TOPIC],subscriptions[command[TOPIC]],receiptIdGen);
					subscriptions.erase(command[TOPIC]);
					receiptIdGen++;
				}
			}
			else if(comm == "report")
			{
				protocol.handleReport(command[FILE_PATH]);
			}
			else if(comm == "logout")
			{
				addReceipt(receiptIdGen,"LOGOUT:LOGOUT",reciepts);
				protocol.handleLogout(receiptIdGen);
				receiptIdGen++;
				break;

			}
			
		}
		catch(const std::exception& e)
		{
			std::cerr << e.what() << '\n';
			protocol.setShouldTerminate(true);
		}
	}

	t.join();
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

void threadLoop(StompProtocol& protocol,std::map<int,string>& receipts)
{
	while(!protocol.shouldTerminate())
	{
		try
		{
			protocol.parseResponse(receipts);
		}
		catch(const std::exception& e)
		{
			protocol.setShouldTerminate(true);
		}

	}
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

std::vector<string> getLines(string& str)
{
	string tmp ="";
	std::vector<string> lines;
	for(int i=0;i<str.size();i++)
	{
		if(str[i] = '\n')
		{
			lines.push_back(tmp);
			tmp.clear();
		}
		else
			tmp+=str[i];
	}
	return lines;
}

/*
=================================

Stomp protocol implemintations

=================================
*/
StompProtocol::StompProtocol():ch(nullptr),username(""),shouldTerminateField(false)
{}
StompProtocol::~StompProtocol()
{
	this->ch->close(); //terminating
	delete this->ch;
}

void StompProtocol::start(ConnectionHandler * ch, string username)
{
	this->ch = ch;
	this->username = username;
}

void StompProtocol::handleLogin(string& hostInfo,string& username,string &password)
{
	string host = "";
    int i = 0;

    while (hostInfo[i] != ':') {
        host += hostInfo[i];
        i++;
    }
	string frame = "CONNECT\naccept-version:1.2\nhost:"+host +"\nlogin:" + username +"\npasscode:" + password + "\n\n";
	if (this->shouldTerminate() || !this->ch->sendFrameAscii(frame, '\0')) 
        throw std::runtime_error("A frame could not be sent to the server - shuting down");
    			
        
}

void StompProtocol::handleJoin(string& game,int subId,int receiptId)
{
	string frame = "SUBSCRIBE\nid:"+ std::to_string(subId)+ "\ndestination:/" + game + "\nreceipt:"+std::to_string(receiptId)+"\n\n";
	if (this->shouldTerminate() ||!this->ch->sendFrameAscii(frame, '\0'))  
        throw std::runtime_error("A frame could not be sent to the server - shuting down");
	this->Games[game] = Game(game);
}

void StompProtocol::handleExit(string& game,int subId,int receiptId)
{
	string frame = "UNSUBSCRIBE\nid:" + std::to_string(subId) + "\nreceipt:" + std::to_string(receiptId) +"\n\n";
	if (this->shouldTerminate() ||!this->ch->sendFrameAscii(frame, '\0'))  
        throw std::runtime_error("A frame could not be sent to the server - shuting down");

	this->Games.erase(game);
}

void StompProtocol::handleLogout(int receiptId)
{
	string frame = "DISCONNECT\nreceipt:" + std::to_string(receiptId) +"\n\n";
	if (this->shouldTerminate() ||!this->ch->sendFrameAscii(frame, '\0'))  
        throw std::runtime_error("A frame could not be sent to the server - shuting down");
}

void StompProtocol::handleReport(string& filePath)
{
	names_and_events data = parseEventsFile(filePath);
	std::vector<Event> events = data.events;
	string game =  data.team_a_name + "_" +data.team_b_name;
	for(int i=0;i<events.size();i++)
	{
		Event curr = events[i];
		string body = ConstructEventFrame(curr);
		string frame = "SEND\ndestination:/" + game +"\n\n" + body;
		if (this->shouldTerminate() ||!this->ch->sendFrameAscii(frame, '\0'))  
        	throw std::runtime_error("A frame could not be sent to the server - shuting down");
	}
}

/*
A method that construct the body for the sent frames according to the manual.
*/
string StompProtocol::ConstructEventFrame(Event& e)
{
	std::map<string,string> gameUpdates = e.get_game_updates();
	std::map<string,string> team_aUpdates = e.get_team_a_updates();
	std::map<string,string> team_bUpdates = e.get_team_a_updates();

	string res = "user: " + this->username +'\n'
	+ "team a: " + e.get_team_a_name() +'\n'
	+ "team b: " + e.get_team_b_name() +'\n'
	+ "event name: " + e.get_name() +'\n'
	+ "time: " + std::to_string(e.get_time()) +'\n'
	+ "general game updates:\n";
	for (auto [key, value] : gameUpdates) 
		res+= "    " +key +": " + value +'\n';

	res +="team a updates:\n";
	for (auto [key, value] : team_aUpdates) 
		res+= "    " +key +": " + value +'\n';

	res +="team b updates:\n";
	for (auto [key, value] : team_bUpdates) 
		res+= "    " +key +": " + value +'\n';
	
	res+="description:\n" + e.get_discription() +'\n';

	return res;
}

bool StompProtocol::shouldTerminate()
{
	return this->shouldTerminateField;
}

void StompProtocol::setShouldTerminate(bool status)
{
	this->shouldTerminateField = status;
}
void StompProtocol::parseResponse(std::map<int,string>& receipts)
{
	string mes;
	if (!this->ch->getFrameAscii(mes,'\0')) {
        cout << "Disconnected. Exiting...\n" << endl;
    	throw std::runtime_error("Connection was terimnated");
    }
	string frameType;
			
	size_t pos = mes.find('\n');
	frameType = mes.substr(0, pos);
	cout << "------------------" << endl;
	cout << mes << endl;
	if(frameType == "ERROR")
	{
		cout << "Conncetion is terminated\npress anything to close." << endl;
		throw std::runtime_error("Connection was terimnated");
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
			cout << "Disconnected from the server\npress anything to close." << endl;
			throw std::runtime_error("Connection was terimnated");
		}
	}
	else if(frameType == "MESSAGE")
	{
		std::vector<string> lines = getLines(mes);
		string username=split(lines[USER_LINE])[1].substr(1);
		string game = split(lines[DEST_LINE])[1].substr(1);
		Event e = parseToEvent(lines);
		addUpdate(game,username,e);
	}
	cout << "------------------" << endl;
}

/*
A method to add new update for a certain user for the game map.
*/
void StompProtocol::addUpdate(string& game,string& user,Event& e)
{
	std::lock_guard<std::mutex> lock(updateMutex);
	this->Games[game].addUpdate(user,e);	
}

/*
Method that parses message frame to event according to the frame setup mentioned in the instruction manual
*/
Event StompProtocol::parseToEvent(std::vector<string> lines)
{
	std::string teamA, teamB, eventName, desc;
    int time = 0;
    std::map<std::string,std::string> gameUpdates, teamAUpdates, teamBUpdates;

	int i = BODY_START;
	
	i++;
	teamA = split(lines[i])[1].substr(1);
	i++;
	teamB = split(lines[i])[1].substr(1);
	i++;
	eventName = split(lines[i])[1].substr(1);
	i++;
	time = std::stoi(split(lines[i])[1].substr(1));
	i++;

	i++;//skipping the string general game updates
	std::vector<string> section;
	if(lines[i][0]==' ')
	{
		while(lines[i][0] == ' ')
		{
			section.push_back(lines[i].substr(4)); // removing the tab at the start
			i++;
		}
		for(int j =0;j<section.size();j++)
		{
			std::vector<string> line = split(section[i]);
			gameUpdates[line[0]] = line[1].substr(1);
		}
	}
	else
		i++;

	i++; // skipping team a updates
	section.clear();
	if(lines[i][0]==' ')
	{
		while(lines[i][0] == ' ')
		{
			section.push_back(lines[i].substr(4)); // removing the tab at the start
			i++;
		}
		for(int j =0;j<section.size();j++)
		{
			std::vector<string> line = split(section[j]);
			teamAUpdates[line[0]] = line[1].substr(1);
		}
	}
	else
		i++;
	
	i++; // skipping team b updates
	section.clear();
	if(lines[i][0]==' ')
	{
		while(lines[i][0] == ' ')
		{
			section.push_back(lines[i].substr(4)); // removing the tab at the start
			i++;
		}
		for(int j =0;j<section.size();j++)
		{
			std::vector<string> line = split(section[j]);
			teamBUpdates[line[0]] = line[1].substr(1);
		}
	}
	else
		i++;
	i++;
	while(i<lines.size())
	{
		desc += lines[i];
		i++;
	}

	return Event(teamA,teamB,eventName,time,gameUpdates,teamAUpdates,teamBUpdates,desc);
}
