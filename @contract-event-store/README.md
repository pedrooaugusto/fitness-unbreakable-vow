## Simple Event Store

Public RPC Nodes have a limit where you can only query 50,000 blocks when searching for contract events. Since this project does not have a backend and relies entirely on queries made to public RPC endpoints this becomes an issue because there's no way to query all events emmited by the contract to show them in the frontend, specially since the contract may span more than 3 months.

This module is a lambda function that runs once a week to save all the events emmited by the contract in the S3 bucket where the frontend is deployed. This way, the frontend can directly query the events.

**This module is not essentiall at all, and it could be replaced with a link to Etherscan. I choose to add it because it makes the UI better.**