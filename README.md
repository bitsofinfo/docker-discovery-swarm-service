# docker-discovery-swarm-service

Peer discovery library for JVM based applications running in Docker containers deployed as Swarm Services

[![Build Status](https://travis-ci.org/bitsofinfo/docker-discovery-swarm-service.svg?branch=master)](https://travis-ci.org/bitsofinfo/docker-discovery-swarm-service)

The purpose of this library is for "self-discovery" from within your JVM based Docker swarm service application where you need to discover what your overlay IP is, as well as your peers within the same service. This is critical if your container has to do further peer discovery for other services it provides or clustering groups it must form.

* [Status](#status)
* [Releases](#releases)
* [Requirements](#requirements)
* [Maven/Gradle install](#mavengradle)
* [Features](#features)
* [Usage Overview](#usageoverview)
* [Running example](#runningexample)
* [Build from source](#building)
* [Unit tests](#tests)
* [Logging](#logging)
* [Related Info](#related)


![Diagram of docker discovery swarm service](/docs/diag1.png "Diagram1")

## <a id="status"></a>Status

Beta code. 

## <a id="releases"></a>Releases

* MASTER - in progress, this README refers to what is in the master branch. Switch to relevant RELEASE tag above to see that versions README

* [1.0-RC2](https://github.com/bitsofinfo/docker-discovery-swarm-service/releases/tag/1.0-RC2) - excludes stopped tasks

* [1.0-RC1](https://github.com/bitsofinfo/docker-discovery-swarm-service/releases/tag/1.0-RC1)

## <a id="requirements"></a>Requirements

* Java 7+
* [Docker 1.12+ Swarm Mode](https://docs.docker.com/engine/swarm/) with one or more swarm manager nodes listening on a `tcp://` socket
* Your application is running in a Docker container and deployed as a swarm service, using this library for discovery

## <a id="mavengradle"></a>Maven/Gradle

To use this discovery strategy in your Maven or Gradle project use the dependency samples below. (coming soon)

### Gradle:

```
repositories {
    jcenter()
}

dependencies {
    compile 'org.bitsofinfo:docker-discovery-swarm-service:1.0-RC2'
}
```

### Maven:

```
<dependencies>
    <dependency>
        <groupId>org.bitsofinfo</groupId>
        <artifactId>docker-discovery-swarm-service</artifactId>
        <version>1.0-RC2</version>
    </dependency>
</dependencies>

<repositories>
    <repository>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
        <id>central</id>
        <name>bintray</name>
        <url>http://jcenter.bintray.com</url>
    </repository>
</repositories>
```

## <a id="features"></a>Features

* Permits a JVM based container application to self-discover its ip address on a docker overlay network, as well as those of all of its peer containers as identified by matching Docker network names, service names and/or service labels.


## <a id="usageoverview"></a>Usage overview

Its **highly recommended** that you walking the example below in the section below. Overall the concept and API is quite simple.

1. You launch your container that utilizes this library as a Docker Swarm Service on a Docker Overlay Network, specifying a `DOCKER_HOST`  (via `-e` args) that points to a Swarm Manager listening on `tcp://` and your container the required arguments (or via other configuration means) about the relevant `dockerNetworkNames`, `dockerServiceNames`, and any `dockerServiceLabels` it will use to locate itself and peer containers in the swarm service via the `/networks`, `/services` and `/tasks` APIs provided by the configured `DOCKER_HOST` (which should point to one or more swarm managers, i.e. via dns etc. `http://swarmmgrs:2375`)

2. In your app's code, you create a new [SwarmServiceDiscovery](src/main/java/org/bitsofinfo/docker/discovery/swarm/service/SwarmServiceDiscovery.java) instance, giving it the required constructor args or properties via the builder syntax for the docker network, service names and labels to utilize.

3. Once constructed you can call the various methods on [SwarmServiceDiscovery](src/main/java/org/bitsofinfo/docker/discovery/swarm/service/SwarmServiceDiscovery.java) such as `getMyIpAddress()`, `getMyContainer()`, or `discoverContainers()`, which are backed by [DiscoveredContainer](src/main/java/org/bitsofinfo/docker/discovery/swarm/service/DiscoveredContainer.java) instances, each containing various methods about each container i.e. `getIp()`.

  
#### Sample code:

```

/**

  PRIOR TO RUNNING THIS CODE:
  
  You must have the following environment variable set:
 
  DOCKER_HOST=http://[swarmmgr]:[port]
  
**/

SwarmServiceDiscovery ssd = new SwarmServiceDiscovery()
                            .addDockerNetworkName("my-service-net")
                            .addDockerServiceName("my-service-01")
                            
                            // optional label filter
                            .addDockerServiceLabel("production");
                        
// get my ip on the docker overlay network
InetAddress myIp = ssd.getMyIpAddress();
                        
// gets all nodes inclusive of this node
for (DiscoveredContainer dc : ssd.discoverContainers()) {
    System.out.println(dc.getIp());
}

```

## <a id="logging"></a> Logging

This library uses slf4j. If you enable TRACE logging for `org.bitsofinfo.docker.discovery.swarm.service` additional verbose debugging information will appear


## <a id="runningexample"></a>Running example

The best example is to check out the [.travis.yml](.travis.yml) file or browse the latest test output at
https://travis-ci.org/bitsofinfo/docker-discovery-swarm-service

## <a id="building"></a>Building from source

* From the root of this project, build a Jar : `./gradlew build`

* Include the built jar artifact located at `build/libs/docker-discovery-swarm-service-[VERSION].jar` in your JVM based project

* If not already present in your hazelcast application's Maven (pom.xml) or Gradle (build.gradle) dependencies section; ensure that these dependencies are present (versions may vary as appropriate):
    
    ```
    compile group: 'com.spotify', name: 'docker-client', version: '8.7.3'
    compile 'org.slf4j:slf4j-api:1.7.19'
    
    // optional if you don't already have a SLF4j impl logger
    //compile group: 'log4j', name: 'log4j', version: '1.2.17'
    ```


## <a id="tests"></a>Unit-tests

[![Build Status](https://travis-ci.org/bitsofinfo/docker-discovery-swarm-service.svg?branch=master)](https://travis-ci.org/bitsofinfo/docker-discovery-swarm-service)

There are really no traditional Java "unit tests" for this SPI due to its reliance on Docker. 

There is however a [Travis CI test](https://travis-ci.org/bitsofinfo/docker-discovery-swarm-service) that properly
validates the functionality in a real Docker swarm environment that brings up a single instance, scales it to 10 hazelcastnodes and then back down to 2 nodes. Demonstrating the proper discovery of self and peer containers in the service.

See the [.travis.yml](.travis.yml) file for the full details.


## <a id="related"></a>Related info

* https://docs.docker.com/engine/swarm/




