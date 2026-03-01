This project was my A level NEA project, it includes a chess AI with 3 difficulty levels, a puzzles gamemode, where the user can develop there skill over time and test themselves with increasingly difficult puzzles. In addition, this project also includes a two player chess gamemode,
Users have there progress saved by logging into the account.

This project will not work when run directly as two files could not be correctly upload due to there size - (database and csv file containing puzzles)

CSV file can be found here - https://database.lichess.org/#puzzles

The database is designed as followed:

Two tables -  userdetails and puzzleattempts table (spelling exact)

userdetails contains - UserName, UserPassword, UserGames, UserWins, userID (userID as a primary key)

puzzle attempts contains columns - userID, puzzles, successfulPuzzles and userElo (userID being a foreign key)

Links included below show the application working with various tests:

AI test - https://youtu.be/p7VXOTYky7s
Accessibility options - https://youtu.be/yFvc2gfsJSE
Puzzles gamemode - https://youtu.be/alzbadPW3Uk
stack to undo and redo moves - https://youtu.be/Q-NvW8QVgXs
