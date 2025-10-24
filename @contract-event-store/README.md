## Simple Event Exlorer

Block explorers have a limit where you can only query 50,000 contract events per request. Since this project does not have a backend and relies entirely on queries made to public RPC endpoints this becomes an issue because there's no way to to cache those events.

This project is lambda functions that runs once a week to save all the events emmited for one contract in S3. Later the frontend can query those events direcly from there.

Don't pay much attention to it.