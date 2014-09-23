package scheduler;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import backtype.storm.scheduler.Cluster;
import backtype.storm.scheduler.EvenScheduler;
import backtype.storm.scheduler.ExecutorDetails;
import backtype.storm.scheduler.IScheduler;
import backtype.storm.scheduler.SchedulerAssignment;
import backtype.storm.scheduler.SupervisorDetails;
import backtype.storm.scheduler.Topologies;
import backtype.storm.scheduler.TopologyDetails;
import backtype.storm.scheduler.WorkerSlot;

public class myScheduler implements IScheduler{

	@Override
	public void prepare(Map conf) {
		// TODO Auto-generated method stub
	}

	@Override
	public void schedule(Topologies topologies, Cluster cluster) {
		// TODO Auto-generated method stub
		System.out.println("\n MyScheduler: begin Scheduling \n");
		//get the topology object that we want to schedule
		TopologyDetails topology = topologies.getByName("mytopology");
		
		//make sure that the special topology exists
		if(topology != null){
			boolean needScheduling = cluster.needsScheduling(topology);
			
			if(!needScheduling){
				System.out.println("Our special topology DOES NOT NEED scheduling.");
			}else{
				System.out.println("Our special topology needs scheduling.");
				// find out all the needs-scheduling components of this topology
				Map<String, List<ExecutorDetails>> componentToExecutors = 
						cluster.getNeedsSchedulingComponentToExecutors(topology);
				System.out.println("needs scheduling(component->executor): " + componentToExecutors);
				System.out.println("needs scheduling(executor->compoenents): " + 
				cluster.getNeedsSchedulingExecutorToComponents(topology));
				
				SchedulerAssignment currentAssignment = cluster.getAssignmentById(topologies.getByName("mytopology").getId());
				
				if (currentAssignment != null) {
                	System.out.println("current assignments: " + currentAssignment.getExecutorToSlot());
                } else {
                	System.out.println("current assignments: {}");
                }
				
				
				if (!componentToExecutors.containsKey("wordSpout")) {
                	System.out.println("Our special-spout DOES NOT NEED scheduling.");
                } else{
                	System.out.println("Our special-spout needs scheduling.");
                	List<ExecutorDetails> executors = componentToExecutors.get("wordSpout");
                	// find out the our "special-supervisor" from the supervisor metadata
                	Collection<SupervisorDetails> supervisors = cluster.getSupervisors().values();
                	SupervisorDetails specialSupervisor = null;
                	
                	for(SupervisorDetails supervisor : supervisors){
                		Map meta = (Map) supervisor.getSchedulerMeta();
                		if(meta.get("name").equals("mytopology")){
                			specialSupervisor = supervisor;
                			break;
                		}
                	}
                	
                	//found the special supervisor
                	if(specialSupervisor !=null){
                		System.out.println("Found the special supervisor");
                		List<WorkerSlot> availableslots = cluster.getAvailableSlots(specialSupervisor);
                		// if there is no available slots on this supervisor, free some.
                        // TODO for simplicity, we free all the used slots on the supervisor
                		if(availableslots.isEmpty() && !executors.isEmpty()){
                			for(Integer port: cluster.getUsedPorts(specialSupervisor)){
                				cluster.freeSlot(new WorkerSlot(specialSupervisor.getId(),port));
                			}
                		}
                		
                		//re-get the availableSlots
                		availableslots = cluster.getAvailableSlots(specialSupervisor);
                		
                		// since it is just a demo, to keep things simple, we assign all the
                        // executors into one slot.
                		cluster.assign(availableslots.get(0), topology.getId(), executors);

                        System.out.println("We assigned executors:" + executors + " to slot: [" + availableslots.get(0).getNodeId() + ", " + availableslots.get(0).getPort() + "]");
                	
                	
                	}else {
                    	System.out.println("There is no supervisor named mycomputer!!!");
                    }
                	
                }
				
				
				
			}
			
		}else{
			System.out.println("Topology Not exists");
		}
		
		// let system's even scheduler handle the rest scheduling work
        // you can also use your own other scheduler here, this is what
        // makes storm's scheduler composable.
		new EvenScheduler().schedule(topologies, cluster);
	}

}
