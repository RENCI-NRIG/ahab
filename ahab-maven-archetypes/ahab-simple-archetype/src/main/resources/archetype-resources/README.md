# Note on maven-archetype generation:
# Lines beginning with `#` are treated as comments by the Maven filtering, 
#  and will be removed from this file when presented to the user.
# For this reason, bullets are used below for what should properly be headings.

* Running the Sample Application
1. You will need a [GENI cert](http://www.exogeni.net/2015/09/exogeni-getting-started-tutorial/) to create slices on ExoGENI.
1. `mvn clean package`
1. `java -cp ./target/${artifactId}-${version}-jar-with-dependencies.jar ${package}.App certLocation keyLocation controllerURL sliceName`
    * `certLocation` and `keyLocation` will come from your GENI cert creation
    * `controllerURL` could be ExoSM: `https://geni.renci.org:11443/orca/xmlrpc`
    * `sliceName` can be of your choosing
1. You can verify slice creation using [Flukes](https://github.com/RENCI-NRIG/flukes)

* References
  * This project uses [Ahab](https://github.com/RENCI-NRIG/ahab) to create virtual infrastructure on [ExoGENI](www.exogeni.net).
