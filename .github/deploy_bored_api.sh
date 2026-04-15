#!/usr/bin/env bash
set -euo pipefail

#########################
# CONFIGURABLE SETTINGS #
#########################

# Where the repo should live on the server
REPO_DIR="/home/bored/project/api/bored-api"

# Your remote Git URL
GIT_URL="https://github.com/amxtremelybored/bored-api.git"
BRANCH="main"

# Docker image / container names
IMAGE_NAME="bored-api:latest"
CONTAINER_NAME="bored-api"

# Use existing network
NETWORK_NAME="bored-net"

# Host port -> container port
HOST_PORT=7082
CONTAINER_PORT=7082

# Env file with secrets (GEMINI_API_KEY, DB_*)
ENV_FILE="/home/bored/secrets/bored-api.env"

# Firebase service account JSON on host
FIREBASE_JSON="/home/bored/secrets/firebase-service-account.json"

#########################
# 1) CLONE / UPDATE GIT #
#########################

echo "📁 Using repo directory: $REPO_DIR"

if [ ! -d "$REPO_DIR/.git" ]; then
  echo "📦 No git repo found at $REPO_DIR"

  if [ -d "$REPO_DIR" ] && [ "$(ls -A "$REPO_DIR")" ]; then
    echo "❌ Directory $REPO_DIR exists and is not empty, but has no .git"
    echo "   Either:"
    echo "   - remove or move it (mv $REPO_DIR ${REPO_DIR}.bak)"
    echo "   - OR manually make it a git repo and set remote."
    exit 1
  fi

  echo "➡️ Cloning $GIT_URL into $REPO_DIR ..."
  mkdir -p "$REPO_DIR"
  git clone "$GIT_URL" "$REPO_DIR"
fi

cd "$REPO_DIR"

echo "🔄 Checking out branch $BRANCH and pulling latest..."
git fetch origin
git checkout "$BRANCH"
git pull origin "$BRANCH"

#########################
# 2) BUILD SPRING BOOT  #
#########################

echo "⚙️ Building Spring Boot JAR..."

if [ -x "./gradlew" ]; then
  echo "➡️ Using ./gradlew"
  ./gradlew clean bootJar
elif command -v gradle >/dev/null 2>&1; then
  echo "➡️ Using system gradle"
  gradle clean bootJar
else
  echo "❌ No Gradle wrapper (./gradlew) or system 'gradle' found."
  exit 1
fi

#########################
# 3) BUILD DOCKER IMAGE #
#########################

echo "🐳 Building Docker image: $IMAGE_NAME ..."
docker build -t "$IMAGE_NAME" .

###########################################
# 4) STOP & REMOVE OLD CONTAINER (IF ANY) #
###########################################

echo "🧹 Stopping old container (if running)..."
docker stop "$CONTAINER_NAME" || true

echo "🧹 Removing old container (if exists)..."
docker rm "$CONTAINER_NAME" || true

###########################################
# 5) ENSURE NETWORK EXISTS                #
###########################################

if ! docker network ls --format '{{.Name}}' | grep -q "^${NETWORK_NAME}\$"; then
  echo "🌐 Creating docker network: $NETWORK_NAME ..."
  docker network create "$NETWORK_NAME"
else
  echo "🌐 Docker network $NETWORK_NAME already exists."
fi

#########################
# 6) CHECK SECRETS      #
#########################

if [ ! -f "$ENV_FILE" ]; then
  echo "❌ Env file not found: $ENV_FILE"
  echo "   Create it with GEMINI_API_KEY and DB_* variables."
  exit 1
fi

if [ ! -f "$FIREBASE_JSON" ]; then
  echo "❌ Firebase JSON not found: $FIREBASE_JSON"
  echo "   Place your firebase-service-account.json there."
  exit 1
fi

#########################
# 7) RUN NEW CONTAINER  #
#########################

echo "🚀 Starting new container: $CONTAINER_NAME ..."

docker run -d \
  --name "$CONTAINER_NAME" \
  --restart unless-stopped \
  --network "$NETWORK_NAME" \
  --env-file "$ENV_FILE" \
  -e "GOOGLE_APPLICATION_CREDENTIALS=/app/bored/firebase-service-account.json" \
  -v "$FIREBASE_JSON:/app/bored/firebase-service-account.json:ro" \
  -v "/home/bored/ad:/home/bored/ad" \
  -p "127.0.0.1:${HOST_PORT}:${CONTAINER_PORT}" \
  "$IMAGE_NAME"

echo "✅ Deploy complete. bored-api is running on 127.0.0.1:${HOST_PORT}."