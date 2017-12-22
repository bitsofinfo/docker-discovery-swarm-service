package org.bitsofinfo.docker.discovery.swarm.service;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerClient.ListNetworksParam;
import com.spotify.docker.client.messages.Network;
import com.spotify.docker.client.messages.swarm.EndpointVirtualIp;
import com.spotify.docker.client.messages.swarm.NetworkAttachment;
import com.spotify.docker.client.messages.swarm.Service;
import com.spotify.docker.client.messages.swarm.Service.Criteria;
import com.spotify.docker.client.messages.swarm.Task;
import com.spotify.docker.client.messages.swarm.TaskStatus;

/**
 * Use SwarmServiceDiscovery for finding about your docker swarm service
 * assigned ip as well of those of peer containers in the same service
 * 
 * It assumes that each container you launch that uses this library
 * has an ENVIRONMENT variable "DOCKER_HOST" that is set equal to 
 * a swarm manager node.
 * 
 * @author bitsofinfo
 *
 */
public class SwarmServiceDiscovery {
	
    private static final Logger logger = LoggerFactory.getLogger(SwarmServiceDiscovery.class);
	
	private Set<String> dockerNetworkNames = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
	private Map<String,String> dockerServiceLabels = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);
	private Set<String> dockerServiceNames = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
	
	private String rawDockerNetworkNames = null;
	private String rawDockerServiceLabels = null;
	private String rawDockerServiceNames = null;

	private DiscoveredContainer myContainer = null;
	private InetAddress myAddress = null;

	/**
	 * Constructor
	 * 
	 * @param rawDockerNetworkNames comma delimited list of names
	 * @param rawDockerServiceLabels comma delimited list of names
	 * @param rawDockerServiceNames (optional) comma delimited list of names
	 * @throws Exception
	 */
	public SwarmServiceDiscovery(String rawDockerNetworkNames, 
						         String rawDockerServiceLabels,
							     String rawDockerServiceNames) throws Exception {

		this.rawDockerNetworkNames = rawDockerNetworkNames;
		this.rawDockerServiceLabels = rawDockerServiceLabels;
		this.rawDockerServiceNames = rawDockerServiceNames;

		if (rawDockerNetworkNames != null && !rawDockerNetworkNames.trim().isEmpty()) {
			for (String rawElement : rawDockerNetworkNames.split(",")) {
				if (!rawElement.trim().isEmpty()) {
					dockerNetworkNames.add(rawElement.trim());
				}
			}
			
		} else {
			String msg = "SwarmDiscoveryUtil() You must specify at least one value for 'rawDockerNetworkNames'";
			logger.error("SwarmDiscoveryUtil: " + msg);
			throw new Exception(msg);
		}

		if (rawDockerServiceLabels != null && !rawDockerServiceLabels.trim().isEmpty()) {
			for (String rawElement : rawDockerServiceLabels.split(",")) {
				if (!rawElement.trim().isEmpty() && rawElement.indexOf('=') != -1) {
					String[] labelVal = rawElement.split("=");
					dockerServiceLabels.put(labelVal[0].trim(),labelVal[1].trim());
				}
			}	
		}

		if (rawDockerServiceNames != null && !rawDockerServiceNames.trim().isEmpty()) {
			for (String rawElement : rawDockerServiceNames.split(",")) {
				if (!rawElement.trim().isEmpty()) {
					dockerServiceNames.add(rawElement.trim());
				}
			}
		}

		// invalid setup
		if (dockerServiceLabels.size() == 0 && dockerServiceNames.size() == 0) {
			String msg = "SwarmDiscoveryUtil() You must specify at least one value for "
					+ "either 'rawDockerServiceNames' or 'rawDockerServiceLabels'";
			logger.error(msg);
			throw new Exception(msg);
		}
		
		// discover self
		discoverSelf();
	}
	
	
	private void discoverSelf() throws Exception {
		
		Set<DiscoveredContainer> discoveredContainers = this.discoverContainers();
		
		Map<String,DiscoveredContainer> ip2ContainerMap = new HashMap<String,DiscoveredContainer>();
		for (DiscoveredContainer dc : discoveredContainers) {
			ip2ContainerMap.put(dc.getIp(), dc);
		}
		
		Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface ni = networkInterfaces.nextElement();
            Enumeration<InetAddress> e = ni.getInetAddresses();
            while (e.hasMoreElements()) {
                InetAddress inetAddress = e.nextElement();
                
                DiscoveredContainer dc = ip2ContainerMap.get(inetAddress.getHostAddress());
                
                // found myself..
                if (dc != null) {
                	
                	// set local references
                	this.myContainer = dc;
                	this.myAddress = InetAddress.getByName(myContainer.getIp());
                	
                	break;
                }
            }
        }
		
	}


	public Set<String> getDockerNetworkNames() {
		return dockerNetworkNames;
	}


	public void setDockerNetworkNames(Set<String> dockerNetworkNames) {
		this.dockerNetworkNames = dockerNetworkNames;
	}


	public Map<String, String> getDockerServiceLabels() {
		return dockerServiceLabels;
	}


	public void setDockerServiceLabels(Map<String, String> dockerServiceLabels) {
		this.dockerServiceLabels = dockerServiceLabels;
	}


	public Set<String> getDockerServiceNames() {
		return dockerServiceNames;
	}


	public void setDockerServiceNames(Set<String> dockerServiceNames) {
		this.dockerServiceNames = dockerServiceNames;
	}


	public String getRawDockerNetworkNames() {
		return rawDockerNetworkNames;
	}


	public void setRawDockerNetworkNames(String rawDockerNetworkNames) {
		this.rawDockerNetworkNames = rawDockerNetworkNames;
	}


	public String getRawDockerServiceLabels() {
		return rawDockerServiceLabels;
	}


	public void setRawDockerServiceLabels(String rawDockerServiceLabels) {
		this.rawDockerServiceLabels = rawDockerServiceLabels;
	}


	public String getRawDockerServiceNames() {
		return rawDockerServiceNames;
	}


	public void setRawDockerServiceNames(String rawDockerServiceNames) {
		this.rawDockerServiceNames = rawDockerServiceNames;
	}

	public Set<DiscoveredContainer> discoverContainers() throws Exception {
		
		try {

			// our client
			final DockerClient docker = DefaultDockerClient.fromEnv().build();
			
			StringBuffer sb = new StringBuffer("discoverNodes(): via DOCKER_HOST: " + docker.getHost() + "\n");
			sb.append("dockerNetworkNames = " + this.getRawDockerNetworkNames() + "\n");
			sb.append("dockerServiceNames = " + this.getRawDockerServiceNames() + "\n");
			sb.append("dockerServiceLabels = " + this.getRawDockerServiceLabels() + "\n");
			logger.info(sb.toString());

			// our discovered containers
			Set<DiscoveredContainer> discoveredContainers = new HashSet<DiscoveredContainer>();

			// the relevant networks we are looking for
			Map<String,Network> relevantNetIds2Networks = new TreeMap<String,Network>(String.CASE_INSENSITIVE_ORDER);

			// build list of relevant networkIds -> Network objects we care about
			for (String dockerNetworkName : this.getDockerNetworkNames()) {
				List<Network> networks = docker.listNetworks(ListNetworksParam.byNetworkName(dockerNetworkName));
				for (Network network : networks) {
					relevantNetIds2Networks.put(network.id(),network);
					logger.info("Found relevant docker network: " + network.name() +"["+ network.id() + "]");
				}
			}

			// Collect all relevant containers for services with services-names on the relevant networks
			for (String dockerServiceName : this.getDockerServiceNames()) {
				discoveredContainers.addAll(
						discoverContainersViaCriteria(docker,
										   relevantNetIds2Networks,
										   Criteria.builder().serviceName(dockerServiceName).build()));
			}
			
			
			// Collect all relevant containers for services matching the labels on the relevant networks
			for (String dockerServiceLabel : this.getDockerServiceLabels().keySet()) {
				String labelValue = this.getDockerServiceLabels().get(dockerServiceLabel);
				discoveredContainers.addAll(
						discoverContainersViaCriteria(docker,
										   relevantNetIds2Networks,
										   Criteria.builder().addLabel(dockerServiceLabel, labelValue).build()));
			}


			return discoveredContainers;

		} catch(Exception e) {
			throw new Exception("discoverContainers() unexpected error: " + e.getMessage(),e);
		}
	}
	
	/**
	 * Discover containers on the relevant networks that match the given
	 * service critiera. 
	 * 
	 * @param docker
	 * @param relevantNetIds2Networks
	 * @param criteria
	 * @return set of DiscoveredContainer instances
	 * @throws Exception
	 */
	private Set<DiscoveredContainer> discoverContainersViaCriteria(DockerClient docker, 
			Map<String,Network> relevantNetIds2Networks, 
			Service.Criteria criteria) throws Exception {

		Set<DiscoveredContainer> discoveredContainers = new HashSet<DiscoveredContainer>();

		// find all relevant services given the criteria....
		List<Service> services = docker.listServices(criteria);
		
		for (Service service : services) {

			// crawl through all VIPs the service is on
			for (EndpointVirtualIp vip : service.endpoint().virtualIps()) {

				// does the service have a VIP on one of the networks we care about?
				if (relevantNetIds2Networks.containsKey(vip.networkId())) {
					
					// get the network object that the vip is on
					Network network = relevantNetIds2Networks.get(vip.networkId());
					
					logger.info("Found qualifying docker service["+service.spec().name()+"] "
							+ "on network: " + network.name() +"["+ network.id() + ":" +vip.addr() +"]");

					// if so, then lets find all its tasks (actual container instances of the service)
					List<Task> tasks = docker.listTasks(Task.Criteria.builder().serviceName(service.spec().name()).build());

					// for every task, lets get its network attachments
					for (Task task : tasks) {
						for (NetworkAttachment networkAttachment : task.networkAttachments()) {

							// if the network ID the task is = the current network we care about for 
							// the service.. then lets treat it as a "dicovered container"
							// that we actually care aboute
							if (networkAttachment.network().id().equals(vip.networkId())) {
								
								// if container is in status 'running', then add it!									
								if (TaskStatus.TASK_STATE_RUNNING.equals(task.status().state())) {
									 
									logger.info("Found qualifying docker service task[taskId: " +task.id() + ", container: "+task.status().containerStatus().containerId()+ ", state: " + task.status().state()+ "] "
											+ "on network: " + network.name() +"["+ network.id() + ":" + networkAttachment.addresses().iterator().next() +"]");																
									
									discoveredContainers.add(new DiscoveredContainer(network, service, task, networkAttachment));
								}
							}

						}
					}

				}
			}
		}

		return discoveredContainers;
	}

	public InetAddress getMyIpAddress() {
		if (myAddress == null) {
			try {
				this.discoverSelf();
			} catch(Exception e) {
				logger.error("Unexpected error in getMyIpAddress(): " + e.getMessage(),e);
			}
		}
		return myAddress;
	}

	public DiscoveredContainer getMyContainer() {
		if (myContainer == null) {
			try {
				this.discoverSelf();
			} catch(Exception e) {
				logger.error("Unexpected error in getMyContainer(): " + e.getMessage(),e);
			}
		}
		return myContainer;
	}
	
	public SwarmServiceDiscovery addDockerNetworkName(String name) {
		if (this.dockerNetworkNames == null) {
			this.dockerNetworkNames = new HashSet<String>();
		}
		this.dockerNetworkNames.add(name);
		return this;
	}
	
	public SwarmServiceDiscovery addDockerServiceName(String name) {
		if (this.dockerServiceNames == null) {
			this.dockerServiceNames = new HashSet<String>();
		}
		this.dockerServiceNames.add(name);
		return this;
	}
	
	public SwarmServiceDiscovery addDockerServiceLabel(String label, String value) {
		if (this.dockerServiceLabels == null) {
			this.dockerServiceLabels = new HashMap<String,String>();
		}
		this.dockerServiceLabels.put(label,value);
		return this;
	}
}



