# ECM2414 Software Development – How to run the test suite

## Overview
This file intent to explains **how to compile and run the JUnit 5 tests for the multi-threaded card game.** 

Basically, the test verify that the program behaves in a correct way and following the specifications:
- Card and CarDeck functionality (thread-safety container (FIFO order))
- Player behaviour (Winning hand, atomic draw/discard logic)
- Overall CardGame execution (valid/ivalid pack handling, file outputs, console messages)

---

## Requirements

| Component     | Detail |
| Java Development Kit | 17 or newer |
| JUnit 5 | Stand-alone launcher: junit-platform-console-standalone-1.10.x.jar|
| Operationg system | Any system with command line |

For the standalone JAR it can be use the one from Maven Central (get it through the next URL): https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/
 1. First when opening the URL, it will redirected to a screen like this (the project used the ../1.10.5 but it can be used any ../1.10.x): 
 ![alt text](image.png)
 2. Then to finally install the Stand-alone launcher, click the following link, the install will stat inmediatelly:
 ![alt text](image-1.png)
 3. Finally place it inside a folder named lib/ eg:
 ECM2414-CardGame/
│
├── src/..
│
├── test/cardgame/..
**├── lib/junit-platform-console-standalone-1.10.5.jar**
│
├── docs/..
│
├── output/..
├── build/..
├── scripts/..
│
├── README.md
└── .gitignore

## Running Test (Command line)



## Authors
- Candidate ID: 730061231
- Partner Candidate ID: 750082802
