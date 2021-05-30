export JAVA_HOME=/home/mjanowski/.jabba/jdk/openjdk@1.11.0-2
export MAVEN_OPTS=-Xmx4G
cd contribs/mjanowski
mvn exec:java -Dexec.mainClass="org.mjanowski.worker.WorkerController" -Dexec.args="/home/mjanowski/IdeaProjects/matsim-libs/examples/scenarios/los-angeles/config.xml localhost 1313 2"
