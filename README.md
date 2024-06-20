## Functionality
✅ Refreshing client token\
✅ Automatically farming\
✅ Collecting Daily rewards\
✅ Random points in the game\
✅ Automatically completing available tasks

## Quick Start
1. Install [docker](https://docs.docker.com/engine/install/)
2. Create clients.json file that will store your clients
3. Use following command to register clients and add them to clients.json file
```bash
sudo docker run -it --rm -v $(pwd)/clients.json:/BlumBot/clients.json --pull=always --entrypoint bin/BlumBot ghcr.io/anisovaleksey/blumbot:latest -a 2
```
4. Run the bot
```bash
docker run --name blum -d -v $(pwd)/clients.json:/BlumBot/clients.json --pull=always ghcr.io/anisovaleksey/blumbot:latest
```
