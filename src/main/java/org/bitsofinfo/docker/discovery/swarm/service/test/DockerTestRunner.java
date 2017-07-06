package org.bitsofinfo.docker.discovery.swarm.service.test;

import org.bitsofinfo.docker.discovery.swarm.service.DiscoveredContainer;
import org.bitsofinfo.docker.discovery.swarm.service.SwarmServiceDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple class for manually spawning instances and reporting SwarmServiceDiscovery state
 * 
 * @author bitsofinfo
 *
 */
public class DockerTestRunner {
	
    private static final Logger logger = LoggerFactory.getLogger(SwarmServiceDiscovery.class);

	public static void main(String[] args) throws Exception {
		
		String rawDockerNetworkNames = System.getProperty("dockerNetworkNames");
		String rawDockerServiceLabels = System.getProperty("dockerServiceLabels");
		String rawDockerServiceNames = System.getProperty("dockerServiceNames");
		
		SwarmServiceDiscovery ssd = new SwarmServiceDiscovery(rawDockerNetworkNames,
															  rawDockerServiceLabels,
															  rawDockerServiceNames);
		
		
		for (int i=0; i<300; i++) {
		
			logger.info("MyAddress: " + ssd.getMyIpAddress().getHostAddress() + " total nodes: ["+ssd.discoverContainers().size()+"]");
			
			StringBuffer sb = new StringBuffer("Discovered Nodes including self:\n");
			for (DiscoveredContainer dc : ssd.discoverContainers()) {
				sb.append(dc.getIp()+"\n");
			}
			logger.info(sb.toString());
			
			Thread.currentThread().sleep(1000);
		}
		
		System.exit(0);
	}
}
