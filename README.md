# Simple KDC Server

##### Useful for testing Kerberos enabled systems
---

##### How to run this project?

Running the jar:
---
Run using the following command
 
	java -jar simple-kdc-server-jar-with-dependencies.jar -Dconfig.file=./config/application.conf
	
Test with the following command:
    
    kinit -V -k -t work/client.keytab client@github.com
