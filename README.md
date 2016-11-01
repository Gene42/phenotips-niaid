# PhenoTips for NIAID
This project is a set of custom modules built to run on top of PhenoTips.

## Project structure
The project is structured as extensions on top of the core PhenoTips platform. Inside the ```distribution``` folder are distributions which provide pre-packaged installations of PhenoTips with the NIAID extensions already installed. *Because of an issue with the PhenoTips extension manager, extension XAR files still need to be installed manually when using these distributions.*
**The project is currently based on PhenoTips 1.3-milestone-3**. Because of its modular nature, it can be 

## Building
1. Clone this repository
2. Run a maven build in the root of the repository.
```
mvn clean install -Pquick
```

### Notes
Currently the ```-Pquick``` flag is **required** in order to skip certain checkstyle and code quality checks. This will not be necessary in the final release.

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

### Installing XAR extensions
Inside of the `ui/target` folder of each of the following modules, upload the `*.xar` file using the PhenoTips web interface (i.e. path to the xar file: `phenotips-niaid/encrypted-pii/ui/target/patient-data-encrypted-ui.xar`)

Modules with XAR files to install:
1. encrypted-pii
2. family-dashboard
3. family-data-table
4. family-groups
4. genetic-evaluation

**Importing the XAR files**:
Upon launching the PhenoTips software, click on `Administration` in the top left corner. From there, click on `Import` in the left menu panel. Proceed to upload the XAR files inside of the above listed modules. Once the XAR files are added under "Available packages", you must click on the XAR package and click on `import` that shows up under "Package Content" in the right panel. 
