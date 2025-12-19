# Occtet NATS Download Service

A microservice responsible for downloading files from a given code location such as a GitHub repository or a maven URL.

## Architecture 
1. **Input**: Listens for `work.download` messages containing `DownloadServiceWorkData`:
    - URL of download location
    - Location file path to where the files should be downloaded 
    - Version number of Project/Package
    - projectId, InventoryItemId and whether it belongs to the main project or not
2. **Processing**:
   - Downloads the artifact to a local directory structure (e.g.: /dependencies/{lib}/{version}). 
   - Extracts contents (Zip/Tar.gz). 
   - Flattens unnecessary wrapper folders (common in GitHub archives).
3. Persistence: Scans the file system and persists `File` entities to the database, linking them to their 
   `InventoryItem` -> important for the file grid in frontend.

## Building & Deployment
Build JAR with Maven
The JAR will be created in the target/ folder.
```shell
mvn clean package
```
Docker Operations

Create Docker Image:
```shell
docker build -t occtet-nats-download-service .
```
Export Docker image as a file for remote deployment:
```shell
docker save occtet-nats-download-service | gzip > occtet-nats-download-service.tar.gz
```
Import Docker image on a remote machine:
```shell
gunzip -c occtet-nats-download-service.tar.gz | docker load
```
