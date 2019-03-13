# xoStitch is tool used to create stitchport-to-stitchport circuits across ExoGENI including special 
# handling for facilities like Chameleon
#  
#

* GENI Cert info
1. You will need a [GENI cert](http://www.exogeni.net/2015/09/exogeni-getting-started-tutorial/) to create slices on ExoGENI.
1. `mvn clean package`
1. `java -cp ./target/xoStitch-1.0-SNAPSHOT-jar-with-dependencies.jar org.renci.xoStitch.App certLocation keyLocation controllerURL sliceName`
    * `certLocation` and `keyLocation` will come from your GENI cert creation
    * `controllerURL` could be ExoSM: `https://geni.renci.org:11443/orca/xmlrpc`
    * `sliceName` can be of your choosing
1. You can verify slice creation using [Flukes](https://github.com/RENCI-NRIG/flukes)

* Build
1. mvn clean package appassembler:assemble

* Chameleon Example
1. ./target/appassembler/bin/xoStitch create  -chameleon -uc 3297 -tacc 3506 -c ~/.ssl/geni-pruth.pem
1. ./target/appassembler/bin/xoStitch delete  -chameleon -uc 3297 -tacc 3506 -c ~/.ssl/geni-pruth.pem
1. ./target/appassembler/bin/xoStitch status  -chameleon -uc 3297 -tacc 3506 -c ~/.ssl/geni-pruth.pem

* Standard Stitchport Example
1. TBD


* References
  * This project uses [Ahab](https://github.com/RENCI-NRIG/ahab) to create virtual infrastructure on [ExoGENI](www.exogeni.net).
