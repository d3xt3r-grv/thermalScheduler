//@Nidhi Rehani, nidhirehani@gmail.com, NIT Kurukshetra

package org.cloudbus.cloudsim.examples;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
//import org.cloudbus.cloudsim.scheduler.BaseCloudletScheduler.Event;


public class HEFTScheduler extends BaseCloudletScheduler{
	
	private   Map<Cloudlet, Double> rank;
    protected Map<Vm, List<Event>> originalschedules;
    protected Map<Cloudlet, Double> earliestFinishTimes;
    protected Map<Cloudlet, Double> shiftamount;
    
    int countfinal =0;//to specify the position of failure slot to check for the chosenvm
    int countcurrent =0;//to specify the position of failure slot to check for the curent vm
    //int maxfr =160;
   // int numberoffailures =0;
    //double begin[], end[];
    int seq=0;
    
    protected class CloudletRank implements Comparable<CloudletRank> {

        public Cloudlet cloudlet;
        public Double rank;

        public CloudletRank(Cloudlet cloudlet, Double rank) {
            this.cloudlet = cloudlet;
            this.rank = rank;
        }

        @Override
        public int compareTo(CloudletRank o) {
            return o.rank.compareTo(rank);
        }
    }

	public HEFTScheduler(List<Cloudlet> cloudletlist, List<Vm> vmlist) {
		// TODO Auto-generated constructor stub
		super(cloudletlist, vmlist);
		rank = new HashMap<>();
        earliestFinishTimes = new HashMap<>();
        originalschedules = new HashMap<>();
        schedules = new HashMap<>();
        shiftamount = new HashMap<>();
        
		
	}
	
	public void run(){
		try{
			File heftfile = new File("HEFTResults");
			BufferedWriter HEFTWriter = new BufferedWriter(new FileWriter(heftfile, true));
			
			Log.printLine("HEFT scheduler with MCS running with " + Cloudletlist.size()
	                + " cloudlets.");

	        averageBandwidth = calc_avg_bw();

	        for (Object vmObject : vmlist) {
	            Vm vm = (Vm) vmObject;
	            originalschedules.put(vm, new ArrayList<Event>());
	            schedules.put(vm, new ArrayList<Event>());
	        }
	        
	        
	        // Prioritization phase
	        calc_ComputationCosts();
	        calc_TransferCosts();
	        calculateRanks();
	        Log.printLine("Ranks calculated");
	        allocatevmavailability();
			allocateVmPowerParameters();
	        // Selection phase
	        Log.printLine("Allocation for HEFT");
	        allocateCloudletsheft();
	        
	        for (Object cloudletObject : Cloudletlist) {
	            Cloudlet cloudlet = (Cloudlet) cloudletObject;
	            shiftamount.put(cloudlet, 0.0);
	        }
	        /*
	        System.out.println("Original HEFT schedule");
	        for(int i =0; i<Cloudletlist.size(); i++){
	        	System.out.print("Cloudlet " + i + " : start : " + begin[i] + " finish : " + end[i] );
	        	System.out.println();
	        }
	        */
	        adjustforfailures();
	        System.out.println("\nafter adjusting heft schedule for failures");
	        for(Cloudlet cloudletObject: Cloudletlist){
	        	Cloudlet cloudlet = cloudletObject;
	        	int i = cloudlet.getCloudletId();
	        	System.out.print("Cloudlet: " + i + " vm allocated: " + cloudlet.getVmId() + "  : start : " + begin[i] + " finish : " + end[i] );
	            startTimes.put(cloudlet, begin[i]);
	            durationTimes.put(cloudlet, end[i]);
	            reservationIds.put(cloudlet, cloudlet.getVmId());
	        	System.out.println();
	        }
	        System.out.println();
	        //set schedules for each vm after failure adjustment
	        for (Object vmObject : vmlist) {
	            Vm vm = (Vm) vmObject;
	            List<Event> sched = originalschedules.get(vm);
	            List<Event> newsched = schedules.get(vm);
	            for(Object event: sched){
	            	Event ev = (Event) event;
	            	int id = ev.cloudlet.getCloudletId();
	            	newsched.add(new Event(begin[id], end[id], ev.cloudlet, vm.getMips()));
	            }
	        }
	        findExecutionEnergyConsumption();
	        totalEnergyConsumption = findTotalEnergyConsumption();
	        System.out.println("\nThe Total Energy Consumption is: " + totalEnergyConsumption);
	        System.out.println("Number of failures occured: " + numberoffailures);
	        //write the makespan for heft in the specified file
	        makespan = end[Cloudletlist.size()-1] ;
	        HEFTWriter.write(makespan + "\t" + totalEnergyConsumption + "\n" );
	        HEFTWriter.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	private void calculateRanks() {
		for (Object cloudletObject : Cloudletlist) {
			Cloudlet cloudlet = (Cloudlet) cloudletObject;
			calculateRank(cloudlet);
		}
	}

	private double calculateRank(Cloudlet cloudlet) {
		if (rank.containsKey(cloudlet)) {
			return rank.get(cloudlet);
		}

		for (Double cost : computationCosts.get(cloudlet).values()) {
			averageComputationCost += cost;
		}

		averageComputationCost /= computationCosts.get(cloudlet).size();

		double max = 0.0;
		for (Cloudlet child : Runner.getChildList(cloudlet)) {
			double rankval = calculateRank(child);
			double childCost = transferCosts.get(cloudlet).get(child)+ rankval;
			max = Math.max(max, childCost);
		}

		rank.put(cloudlet, averageComputationCost + max);
		//print the rank for the cloudlet
		//System.out.println("Cloudlet: " + cloudlet.getCloudletId() + "rank: " + rank.get(cloudlet));
		return rank.get(cloudlet);
	}
	
    private void allocateCloudletsheft() {
        List<CloudletRank> cloudletRank = new ArrayList<>();
        for (Cloudlet cloudlet : rank.keySet()) {
            cloudletRank.add(new CloudletRank(cloudlet, rank.get(cloudlet)));
        }

     // Sorting in non-ascending order of rank
        Collections.sort(cloudletRank);
        for (CloudletRank cr : cloudletRank) {
            allocateCloudletheft(cr.cloudlet);
        }
    }

    private void allocateCloudletheft(Cloudlet cloudlet) {
    	//sequence[seq] = cloudlet.getCloudletId(); 
    	seq++;
        Vm chosenvm = null;
        double earliestFinishTime = Double.MAX_VALUE;
        double bestReadyTime = 0.0;
        double finishTime;

        for (Object vmObject : vmlist) {
            Vm vm = (Vm) vmObject;
            double minReadyTime = 0.0;

            for (Cloudlet parent : Runner.getParentList(cloudlet)) {
                double readyTime = earliestFinishTimes.get(parent);
                if (parent.getVmId() != vm.getId()) {
                    readyTime += transferCosts.get(parent).get(cloudlet);
                }

                minReadyTime = Math.max(minReadyTime, readyTime);
            }

            finishTime = findFinishTime(cloudlet, vm, minReadyTime, false);

            if (finishTime < earliestFinishTime) {
                bestReadyTime = minReadyTime;
                earliestFinishTime = finishTime;
                chosenvm = vm;
            }
        }

        findFinishTime(cloudlet, chosenvm, bestReadyTime, true);

        earliestFinishTimes.put(cloudlet, earliestFinishTime);

        cloudlet.setVmId(chosenvm.getId());
    }
    
    private double findFinishTime(Cloudlet cloudlet, Vm vm, double readyTime,
            boolean occupySlot) {
    	int id = cloudlet.getCloudletId();
    	int vid = vm.getId();
        List<Event> sched = originalschedules.get(vm);
        double computationCost = computationCosts.get(cloudlet).get(vm);
        double start, finish;
        int pos;

        if (sched.size() == 0) {
            if (occupySlot) {
                sched.add(new Event(readyTime, readyTime + computationCost, cloudlet, vm.getMips()));
                begin[id] = readyTime;
                end[id] = readyTime + computationCost;
                //vmallocated[id] = vid;
                System.out.println("Cloudlet: " + id + "\tVm allocated: " + vid + "  begin : " + begin[id]  + "  end:  " + end[id]);
            }
            return readyTime + computationCost;
        }

        if (sched.size() == 1) {
            if (readyTime >= sched.get(0).finish) {
                pos = 1;
                start = readyTime;
            } else if (readyTime + computationCost <= sched.get(0).start) {
                pos = 0;
                start = readyTime;
            } else {
                pos = 1;
                start = sched.get(0).finish;
            }

            if (occupySlot) {
                sched.add(pos, new Event(start, start + computationCost, cloudlet, vm.getMips()));
                begin[id] = start;
                end[id] = start + computationCost;
                //vmallocated[id] = vid;
                System.out.println("Cloudlet: " + id + "\tVm allocated: " + vid + "  begin : " + begin[id]  + "  end:  " + end[id]);
            }
            return start + computationCost;
        }

        // Trivial case: Start after the latest task scheduled
        start = Math.max(readyTime, sched.get(sched.size() - 1).finish);
        finish = start + computationCost;
        int i = sched.size() - 1;
        int j = sched.size() - 2;
        pos = i + 1;
        while (j >= 0) {
            Event current = sched.get(i);
            Event previous = sched.get(j);

            if (readyTime > previous.finish) {
                if (readyTime + computationCost <= current.start) {
                    start = readyTime;
                    finish = readyTime + computationCost;
                }

                break;
            }

            if (previous.finish + computationCost <= current.start) {
                start = previous.finish;
                finish = previous.finish + computationCost;
                pos = i;
            }

            i--;
            j--;
        }

        if (readyTime + computationCost <= sched.get(0).start) {
            pos = 0;
            start = readyTime;

            if (occupySlot) {
                sched.add(pos, new Event(start, start + computationCost, cloudlet, vm.getMips()));
                begin[id] = start;
                end[id] = start + computationCost;
               // vmallocated[id] = vid;
                System.out.println("Cloudlet: " + id + "\tVm allocated: " + vid + "  begin : " + begin[id]  + "  end:  " + end[id]);
            }
            return start + computationCost;
        }
        if (occupySlot) {
            sched.add(pos, new Event(start, finish, cloudlet, vm.getMips()));
            begin[id] = start;
            end[id] = start + computationCost;
            //vmallocated[id] = vid;
            System.out.println("Cloudlet: " + id + "\tVm allocated: " + vid + "  begin : " + begin[id]  + "  end:  " + end[id]);
        }
        return finish;
    }
    
    public void adjustforfailures(){
    	for (Object cloudletObject : Cloudletlist) {
            Cloudlet cloudlet = (Cloudlet) cloudletObject;
            int flag =0;
            double shift =0.0;
            double prevshift = shiftamount.get(cloudlet);
            double total;
            double transferCost = 0.0;
            double temp =0.0;
            double tempstart;
            double computationCost = end[cloudlet.getCloudletId()] - begin[cloudlet.getCloudletId()];
            for (Cloudlet child : Runner.getChildList(cloudlet)) {
            	
                temp = transferCosts.get(cloudlet).get(child);
                if(temp > transferCost){
                	transferCost = temp;
                }
            }
            //find out the allocated vm and shift for failure 
            //Vm vm;
            for (Object vmObject : vmlist) {
                Vm vm = (Vm) vmObject;
                int vmi = vm.getId();
                if(vmi == cloudlet.getVmId()){
                	 List<Slot> na = BaseCloudletScheduler.getSlots(vm);
                     double start = begin[cloudlet.getCloudletId()];
                     double finish = end[cloudlet.getCloudletId()];
                     int count =0;
                     int finalcount =0;
                     while(count< MCSimulation.MAXFR/2){
                     	if(start >= na.get(count).failure && start < na.get(count).repair ){
                     		flag =1;
                     		finalcount=count;
                     		break;
                     	}
                     	if((finish+ transferCost)>= na.get(count).failure && (finish+ transferCost)<= na.get(count).repair){
                     		flag =1;
                     		finalcount=count;
                     		break;
                     	}
                     	if(start <= na.get(count).failure && (finish+ transferCost) > na.get(count).failure){
                     		flag = 1;
                     		finalcount=count;
                     		break;
                     	}
                     	count++;
                     	if(start < na.get(count).failure && (finish+ transferCost) < na.get(count).failure){
                     		
                     		break;
                     		
                     	}
                     }
                     //if failure occures during the current cloudlet execution
                     if(flag ==1){
                    	numberoffailures += 1;
                    	tempstart = na.get(finalcount).repair;
                    	finalcount++;
                    	if(tempstart < na.get(finalcount).failure && (tempstart + computationCost + transferCost) <= na.get(finalcount).failure ){
                        	start = tempstart;
                        	
                        }else {
                        	do{
                        		tempstart = na.get(finalcount).repair;
                        		finalcount++;
                        		
                        	}while(!(temp <= na.get(finalcount -1).repair && (temp + computationCost + transferCost) <= na.get(finalcount).failure ));
                        	
                        }
                    	start = tempstart;
                    	shift = start - begin[cloudlet.getCloudletId()];
                    	//System.out.println("Cloudlet: " + cloudlet.getCloudletId() + "  Current shift for failure: " + shift);
 		    			begin[cloudlet.getCloudletId()] = start;
 		    			end[cloudlet.getCloudletId()] = end[cloudlet.getCloudletId()] + shift;
 		    			total = prevshift + shift;
 		    			shiftamount.put(cloudlet, total);
 		    			
 		    			//System.out.println("Total shift amount: " + shiftamount.get(cloudlet));
 		    			adjustcloudletfailure(cloudlet, total);
 		    			
                     }
                
                }
                else
                	continue;
            }
           
            
            
    	}
    }
    
    private void adjustcloudletfailure(Cloudlet cloudlet, double totalshift){
    	
    	for (Cloudlet child : Runner.getChildList(cloudlet)) {
				double prevshift = shiftamount.get(child);
				double difference =0;
				//shift time if total shift is more than already shifted amount
				if(totalshift > prevshift){
					difference = totalshift - prevshift;
					begin[child.getCloudletId()] = begin[child.getCloudletId()] + difference;
		    		end[child.getCloudletId()] = end[child.getCloudletId()] + difference;
		    		shiftamount.put(child, totalshift);
		    		//System.out.println("\nCloudlet id: " + child.getCloudletId() + "  previous shift: " + prevshift + " total shift now: " + shiftamount.get(child) );
				}
				adjustcloudletfailure(child, totalshift);
			}
    }


}
