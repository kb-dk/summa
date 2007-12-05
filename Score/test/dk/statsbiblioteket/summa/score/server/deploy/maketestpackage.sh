javac FakeThinClient.java
cd ../../../../../../
jar cfm dk/statsbiblioteket/summa/score/server/deploy/Main.jar dk/statsbiblioteket/summa/score/server/deploy/Manifest.txt dk/statsbiblioteket/summa/score/server/deploy/*.class
cd dk/statsbiblioteket/summa/score/server/deploy/
zip FakeZIP Main.jar FakeClient.java
 