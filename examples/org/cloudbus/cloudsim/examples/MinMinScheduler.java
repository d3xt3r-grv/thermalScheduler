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
import org.cloudbus.cloudsim.Consts;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
//import org.cloudbus.cloudsim.examples.Schedulerforfarsandheft.Event;
import org.cloudbus.cloudsim.lists.CloudletList;

public class MinMinScheduler extends BaseCloudletScheduler{
	
	private Map<Vm, Double> priority;
	private Map<Vm, List<Event>> schedules;
	private Map<Cloudlet, Double> earliestFinishTimes;
	private Map<Cloudlet, Double> shiftamount;
	
	//double averageComputationCost = 0.0;
	

	//cloudlet available for consideration
	private List available = new ArrayList<Boolean>();
	//cloudlet assigned to a machine
	private List assigned = new ArrayList<Boolean>();

	public MinMinScheduler(List<Cloudlet> cloudletlist, List<Vm> vmlist) {
		// TODO Auto-generated constructor stub
		super(cloudletlist, vmlist);
		priority = new HashMap<>();
	}
	
	public void run(){
		
		try{
			File minminfile = new File("MinMinResults");
			BufferedWriter minminWriter = new BufferedWriter(new FileWriter(minminfile));
		Log.printLine("MIN MIN scheduler running with " + Cloudletlist.size()
		        + " cloudlets.");

		averageBandwidth = calc_avg_bw();

		for (Object vmObject : vmlist) {
			Vm vm = (Vm) vmObject;
			schedules.put(vm, new ArrayList<Event>());
		}
		//mcs simulation
        //mc = new MCSimulation(vmlist, vmlist.size()); 
        //mc.run();
		//rank calculation
		calc_ComputationCosts();
		calc_TransferCosts();
		int size = Cloudletlist.size();
		available.clear();
		assigned.clear();
		for(int t=0; t<size; t++){
			available.add(false);
			assigned.add(false);
		}
		available.set(0, true);
		allocatecloudlets();
		//allocateavailablecloudlets();
		for(Object cloudletObject:Cloudletlist){
			Cloudlet cloudlet = (Cloudlet) cloudletObject;
			shiftamount.put(cloudlet, 0.0);
		}
		adjustforfailures();
        System.out.println("\nafter adjusting MIN MIN schedule for failures");
        for(Cloudlet cloudletObject: Cloudletlist){
        	Cloudlet cloudlet = cloudletObject;
        	int i = cloudlet.getCloudletId();
        	System.out.print("Cloudlet " + i + " : start : " + begin[i] + " finish : " + end[i] );
            startTimes.put(cloudlet, begin[i]);
            durationTimes.put(cloudlet, end[i]);
            reservationIds.put(cloudlet, cloudlet.getVmId());
        	System.out.println();
        }
        System.out.println();
        System.out.println("Number of failures occured: " + numberoffailures);
      //write the makespan for minmin schedule in the specified file
        makespan = end[Cloudletlist.size()-1] ;
        minminWriter.write(makespan + "\n" );
        minminWriter.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}   



	public double calc_priority(Cloudlet cloudlet, Vm vm){
		double computationCost = computationCosts.get(cloudlet).get(vm);
		//to find out max transfer cost to the child
		double transferCost = 0.0;
		double temp = 0.0;
		for (Cloudlet parent : Runner.getParentList(cloudlet)) {

			temp = transferCosts.get(parent).get(cloudlet);
			if(temp > transferCost){
				transferCost = temp;
			}
		}
		return computationCost + transferCost;

	}

	public void allocatecloudlets(){
		while(true){
			boolean loop = false;
			for(Object cloudletObject:Cloudletlist){
				Cloudlet cloudlet = (Cloudlet) cloudletObject;
				int j = cloudlet.getCloudletId();
				boolean chk = (Boolean) (assigned.get(j));
				if (!chk) {
					//minCloudlet = cloudlet;
					// minIndex = j;
					loop = true;
					break;
				}
			}
			if (loop == false) {
				break;
			}
			allocateavailablecloudlets();
			for(Object cloudletObject:Cloudletlist){
				Cloudlet child = (Cloudlet) cloudletObject;
				boolean check = false;
				if((boolean)assigned.get(child.getCloudletId())){
					continue;
				}
				for (Cloudlet parent : Runner.getParentList(child)) {
					int parentid = parent.getCloudletId();
					boolean chkassigned = (boolean)assigned.get(parentid);
					if(!chkassigned){
						check = false;
						break;
					}
					else
						check = true;
				}
				if(check == true){
					available.set(child.getCloudletId(), true);
				}
			}
		}
	}
	public void allocateavailablecloudlets(){
		
		
		while(true){
			
			boolean availableflag = false;
			for(Object cloudletObject:Cloudletlist){
				Cloudlet cloudlet = (Cloudlet) cloudletObject;
				int j = cloudlet.getCloudletId();
				boolean availcheck = (Boolean) (available.get(j));
				boolean assigncheck = (Boolean) (assigned.get(j));
				if (availcheck && !assigncheck) {
					availableflag = true;
				}
			}
			if (availableflag == false) {
				break;
			}

			Cloudlet minCloudlet = null;
			double mincompletionTime = Double.MAX_VALUE;
			double minreadyTime = Double.MAX_VALUE;
			Vm minVmfinal = null; 
			
			//for each cloudlet
			for(Object cloudletObject:Cloudletlist){
				Cloudlet cloudlet = (Cloudlet) cloudletObject;


				int i = cloudlet.getCloudletId();
				//check if its available and has not yet been assigned
				boolean chkavail = (Boolean) (available.get(i));
				boolean chkassign = (Boolean) (assigned.get(i));
				double minVmCompletionTime = Double.MAX_VALUE;
				double minVmreadytime = Double.MAX_VALUE;
				Vm minvm = null;
				if(chkavail && !chkassign){
					//the cloudlet is available for execution
					//find out the completion time for the task on each virtual machine 
					for(Object vmObject: vmlist){
						Vm vm = (Vm) vmObject;
						List<Event> sched = schedules.get(vm);
						double readyTime = 0.0;
						//rank for the cloudlet is referred to as priority
						double priority = Double.MAX_VALUE;
						double completionTime = Double.MAX_VALUE;
						//find the earliest available time for the vm 
						if(sched.size() == 0){
							readyTime = 0.0;
						}
						else{
							readyTime = sched.get(sched.size() -1).finish;

						}
						priority = calc_priority(cloudlet, vm);
						completionTime = priority + readyTime;
						if(completionTime < minVmCompletionTime){
							minVmCompletionTime = completionTime;
							minvm = vm;
							minVmreadytime = readyTime;
						}

					}
					if(minVmCompletionTime < mincompletionTime){
						minCloudlet = cloudlet;
						minreadyTime = minVmreadytime; 
						minVmfinal = minvm;
						mincompletionTime = minVmCompletionTime;

					}
				}

			}
			if(minCloudlet != null){
				allocateminCloudlet(minCloudlet,minVmfinal, minreadyTime, mincompletionTime);
			}
			
		}
		
	}
	public void allocateminCloudlet(Cloudlet cloudlet,Vm vm, double readyTime, double mincompletionTime){
		List<Event> sched = schedules.get(vm);
		int id = cloudlet.getCloudletId();
		int vid = vm.getId();
		if (sched.size() == 0) {
			sched.add(new Event(readyTime, mincompletionTime, cloudlet, vm.getMips()));
			begin[id] = readyTime;
			end[id] = mincompletionTime;
			//vmallocated[id] = vid;
			System.out.println("Cloudlet: " + id + "\tVm allocated: " + vid + "  begin: " + begin[id] + "  end: " + end[id]);
		}
		else {
			int pos = sched.size();
			sched.add(pos, new Event(readyTime,mincompletionTime, cloudlet, vm.getMips() ));
			begin[id] = readyTime;
			end[id] = mincompletionTime;
			// vmallocated[id] = vid;
			System.out.println("Cloudlet: " + id + "\tVm allocated: " + vid + "  begin: " + begin[id] + "  end: " + end[id]);
		}
		assigned.set(id, true);
		cloudlet.setVmId(vid);

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
                     	if(start > na.get(count).failure && start < na.get(count).failure ){
                     		flag =1;
                     		finalcount=count;
                     		break;
                     	}
                     	if((finish+ transferCost)> na.get(count).failure && (finish+ transferCost)< na.get(count).repair){
                     		flag =1;
                     		finalcount=count;
                     		break;
                     	}
                     	if(start < na.get(count).failure && (finish+ transferCost) > na.get(count).repair){
                     		flag = 1;
                     		finalcount=count;
                     		break;
                     	}
                     	count++;
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
