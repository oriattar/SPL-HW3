#pragma once
#include <iostream>
#include <stdio.h>
#include <atomic>
#include <mutex>
#include <thread>
#include <map>
#include <vector>
#include <stdexcept>
#include "../include/ConnectionHandler.h"
#include "../include/event.h"

using std::string; 
class Game
{
    private:
        string name;
        std::map<string,std::vector<Event>> updateByUserBefore;
        std::map<string,std::vector<Event>> updateByUserAfter;
        std::map<string,bool> halfTimeFalgs;

    public:
        Game() : name("") {};
        Game(string name):name(name){};

        string getName(){return name;}
        std::vector<Event> getUpdatesBefore(string& user){return updateByUserBefore[user];}
        std::vector<Event> getUpdatesAfter(string& user){return updateByUserAfter[user];}

        void markPastHalftime(string & user){halfTimeFalgs[user] = true;}
       
        /*
        A method that enables adding event to a certain user eventlist in the game
        sort after each insertion to maintain time order.
        */
        void addUpdate(string& user,Event& e)
        {
            if(!halfTimeFalgs[user])
            {
                auto& v = updateByUserBefore[user];
                v.push_back(e);
                std::sort(v.begin(), v.end(),[](const Event& a, const Event& b){ return a.get_time() < b.get_time(); }); //sorts vector by time
            }
            else
            {
                auto& v = updateByUserAfter[user];
                v.push_back(e);
                std::sort(v.begin(), v.end(),[](const Event& a, const Event& b){ return a.get_time() < b.get_time(); }); //sorts vector by time
            }
        }
};