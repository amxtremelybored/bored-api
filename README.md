# See running containers
docker ps

# See all containers (including stopped)
docker ps -a

# Logs for bored-api
docker logs bored-api
docker logs -f bored-api      # follow live

# Exec into the container shell
docker exec -it bored-api sh
# or bash if present:
docker exec -it bored-api bash

# Inspect env vars inside container
docker exec -it bored-api env | grep -E 'GEMINI|DB_|GOOGLE_'

# Check Docker networks
docker network ls
docker network inspect bored-net

# Check images & space
docker images
docker system df
