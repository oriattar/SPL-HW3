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
        std::map<string,std::vector<Event>> updateByUser;
        bool afterHalftime;

    public:
        Game(string name):name(name),afterHalftime(false){};

        string getName(){return name;}
        std::map<string,std::vector<Event>> getUpdates(){return updateByUser;}
        bool isAfterHalftime(){return afterHalftime;}

        void setAfterHalftime(bool status){afterHalftime = status;}
        void addUpdate(string& user,Event& e)
        {
            //to impl
        }
};