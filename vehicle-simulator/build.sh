
docker rm -f fleet-service || true

./gradlew clean shadowJar

docker build . -t io.simplematter/vehicle-simulator

docker run -d --name fleet-service io.simplematter/vehicle-simulator
