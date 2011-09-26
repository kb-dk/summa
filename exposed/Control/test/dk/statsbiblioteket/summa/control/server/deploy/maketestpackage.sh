javac FakeThinClient.java
cd ../../../../../../
jar cfm dk/statsbiblioteket/summa/control/server/deploy/Main.jar dk/statsbiblioteket/summa/control/server/deploy/Manifest.txt dk/statsbiblioteket/summa/control/server/deploy/*.class
cd dk/statsbiblioteket/summa/control/server/deploy/
zip FakeZIP Main.jar FakeClient.java
 