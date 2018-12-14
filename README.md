# ahab
Ahab is a collection of libraries for managing the state of slices created in ORCA

## Using ahab in your project
Ahab includes a [Maven Archetype](https://maven.apache.org/guides/introduction/introduction-to-archetypes.html), providing a template for creating projects that use Ahab.  You can run the following maven command to generate a new maven project / sample application, that is ready to compile and run.  When run, it will create a simple slice using Ahab.
* `mvn archetype:generate -DarchetypeGroupId=org.renci.ahab -DarchetypeArtifactId=ahab-simple-archetype -DarchetypeVersion=0.1.7 -DgroupId=com.mycompany.app -DartifactId=my-app`

The generated template will include a README.md file with instructions on how to build and run the sample application.

## Releases
Release artifacts of Ahab are available from the [The Central Repository](http://central.sonatype.org/), Open Source Software Repository Hosting (OSSRH).  Information on the latest version available can be found using this search: [g:"org.renci.ahab"](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.renci.ahab%22) 

Use the following pom snippet to include Ahab in your project:
```
<dependency>
    <groupId>org.renci.ahab</groupId>
    <artifactId>ahab</artifactId>
    <version>0.1.6</version>
    <type>pom</type>
</dependency>
```

Further information for Ahab developers on the Release process can be found in [RELEASE.md](https://github.com/RENCI-NRIG/ahab/blob/master/RELEASE.md)
