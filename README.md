# Battleships
## Table of Contents
* [Introduction](#introduction)
* [Database model](#database-model)
* [Class diagram](#class-diagram)
* [User documentation](#user-documentation)
* [Inspiration](#inspiration)
## Introduction
A client-server application that allows users to log in and play battleships. Additionally, users can write chat messages to each other.
## Database model
![image](https://user-images.githubusercontent.com/85128542/215051293-7bad4dfc-f90c-4a03-b5b7-c3a241e44942.png)
## Class diagram
![battleships-diagram](https://user-images.githubusercontent.com/85128542/215054761-1344b399-278c-47b1-9ce9-589493ea9703.png)
## User documentation
From the start screen, it is possible to view a list of the top ten users. From this screen, it is also possible to log into the application and later start the game.

![image](https://user-images.githubusercontent.com/85128542/215093504-247770e7-8c39-4227-af45-89a4c849f7ce.png)

After entering the login and password, the password is hashed and the database is queried. If:
- Valid data has been entered - login occurs and the game screen is turned on
- A new username has been entered - a new account is created and after displaying prompt note, the game is turned on
- An invalid password has been entered - an invalid password prompt is displayed

![image](https://user-images.githubusercontent.com/85128542/215094320-92783b0f-87ad-4123-b732-e75776495b15.png)

Entering the wrong password.

![image](https://user-images.githubusercontent.com/85128542/215094206-3e1ac818-c07c-4e96-8a30-87f36a3470fe.png)

Creating a new account.

![image](https://user-images.githubusercontent.com/85128542/215094461-7ccadaaa-8f33-4440-afc0-cecefa8a69ff.png)

Screen view when only one user has joined the game and is waiting for another to join the table.

![image](https://user-images.githubusercontent.com/85128542/215094939-5830cf20-d734-44f5-aa75-053d1e8d1a58.png)

One player's game screen after taking several turns and sending several messages between them.

![image](https://user-images.githubusercontent.com/85128542/215085376-c7fc0237-c459-49c0-a0e0-1552fc0700bc.png)

After a game is completed (either won or lost), a game status prompt is displayed, allowing you to start a new game and be assigned to a new table, or to end the game and shut down the application.

![image](https://user-images.githubusercontent.com/85128542/215085886-243a2fa1-1688-4872-b394-b97e62e9ec77.png)

## Inspiration
https://github.com/AlmasB/Battleship.git
