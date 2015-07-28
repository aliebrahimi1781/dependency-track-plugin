# dependency-track-plugin
A Dependency-Track Plugin (OWASP)

## A. PRELIMINARIES

**All actions of this part will be made only once !**

### A.1. Installing a Java servlet (Tomcat) and Maven
```
sudo apt-get update
sudo apt-get upgrade
```
#### Installing OpenJDK :
```
sudo apt-get install openjdk-7-jdk openjdk-7-jre
export JAVA_HOME=/usr/lib/jvm/java-1.7.0-openjdk-amd64
```
#### Installing Tomcat7 :
```
sudo apt-get install tomcat7 tomcat7-admin tomcat7-docs tomcat7-examples

sudo chown -R tomcat7:tomcat7 /var/lib/tomcat7/
sudo chown -R tomcat7:tomcat7 /etc/tomcat7/
sudo chown -R tomcat7:tomcat7 /usr/share/tomcat7
```
#### Installing Maven :
```
sudo apt-get install maven
```
### A.2. Downloading Dependency-Track and Plugin sources
Dependency-Track sources : [Dependency-Track](https://github.com/stevespringett/dependency-track)

Plugin sources : [This github](https://github.com/florent-fauvin/dependency-track-plugin)

The following commands will be valid if the sources are put in a same folder, named "~/dtrack".
You can obviously do otherwise, but the commands must be changed accordingly.
```
mkdir ~/dtrack
cp Plugin ~/dtrack
cp dependency-track-master ~/dtrack
cd ~/dtrack
```
### A.3. Including the Plugin sources in the Dependency-Track sources
```
cp Plugin/dependency-track/sources/page.jsp \
	dependency-track-master/src/main/webapp/WEB-INF/views/templates/page.jsp

cp Plugin/dependency-track/sources/ApplicationController.java \
	dependency-track-master/src/main/java/org/owasp/dependencytrack/controller/ApplicationController.java
```
### A.4. Compiling (pluged) Dependency-Track
```
cd ~/dtrack/dependency-track-master
mvn clean -Dmaven.compiler.failOnError=false package

sudo /etc/init.d/tomcat7 stop
sudo cp target/dtrack.war /var/lib/tomcat7/webapps/
sudo /etc/init.d/tomcat7 start

```
### A.5. Adding/Replacing licenses
```
cd ~/dtrack/Plugin/dependency-track/licenses/

sudo /etc/init.d/tomcat7 stop
sudo java -jar addNewLicenses.jar /usr/share/tomcat7/dependency-track/ removeAllTables=Yes
removeAllTables=[Yes|No]
sudo /etc/init.d/tomcat7 start
```
## B. GETTING DEPENDENCIES OF SOME SOFTWARES

##### B.1. Write in the *Plugin/executables.txt* file the path of dynamic executables you want to analyze.
For example :
```
/usr/bin/dwp
/usr/bin/dig
```
##### B.2. Run the analyzing.
```
cd ~/dtrack/Plugin
python2.7 Main.py
```
The output file *program_info.json* will be created in the *Plugin/dependency-track* folder. It is a JSON file which contains all dependencies information that will be inserted in the Dependency-Track database.
##### B.3. Copy the output file where the plugin will read
```
cd ~/dtrack

sudo cp Plugin/dependency-track/program_info.json \
	/var/opt/dependency-track-pluggin/program_info.json
```
## C. INSERTING DEPENDENCIES INFORMATION IN DEPENDENCY-TRACK