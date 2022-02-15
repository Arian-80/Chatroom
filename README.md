# Chatroom
A program for users to socialise with each other by connecting to any of the available servers.  
Mainly worked on to practise programming skills and to learn using a wide range of features.  
  
An administrator can launch the server, and by default, it will be on localhost with port 14001.  
The administrator can use change the port by typing -csp <port number> as a command line argument upon launching the server.  
Up to a maximum of 10 servers can be run concurrently, with the default ports increasing from 14001 to 14010.  
Users can connect to any running server. By default, the program will try to connect them to the server on port 14001.  
If unsuccessful, the program then tries to connect them to the server on port 14002, and so on.  
Users can also change the server and port they are trying to connect to by typing -ccs <server address> and -ccp <port number>
as command line arguments upon trying to connect.  
Users will then be asked to choose a name, with a minimum and maximum length of 2 and 20, respectively.  
The program ensures that no two names are the same, and if the user wishes to remain anonymous, they can choose nothing as their name
and the server creates an anonymous name for them.  
Users may then use the program as intended.