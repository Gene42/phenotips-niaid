# PhenoTips for NIAID
This project is a set of custom modules built to run on top of PhenoTips.

## Project structure
The project is structured as extensions on top of the core PhenoTips platform. Inside the ```distribution``` folder are distributions which provide pre-packaged installations of PhenoTips with the NIAID extensions already installed.

**The project is currently based on PhenoTips 1.3-milestone-3**. Because of its modular nature, it can be made to work with newer versions of PhenoTips without substantial effort.

## Building
1. Clone this repository
2. Run a maven build in the root of the repository.
```
mvn clean install
```

## Installation and usage
The project is split into several modules, among which distribution/standalone will result in a fully-working self-contained package ready to run. Running the application is as simple as:
1. Go to the directory where the distribution package is located.
```
cd distribution/standalone/target
```
3. Extract the contents of the `phenotips-standalone-1.0-SNAPSHOT.zip` archive to a location of your choice (outside the target directory, to ensure it is not overwritten by subsequent builds).
```
unzip phenotips-niaid-standalone-1.0-SNAPSHOT.zip
mv phenotips-niaid-standalone-1.0-SNAPSHOT [path/to/location of choice]
```
4. Launch the start script (`start.sh` on unix-like systems, `start.bat` on Windows).
5. Open http://localhost:8080/ in a browser.
