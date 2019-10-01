//@author Nidhi Rehani, nidhirehani@gmail.com, NIT Kurukshetra

package org.cloudbus.cloudsim.examples;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.lists.CloudletList;
//import org.cloudbus.cloudsim.scheduler.BaseCloudletScheduler.Event;
//import org.cloudbus.cloudsim.scheduler.FailureAwareScheduler.CloudletRank;
//import org.cloudbus.cloudsim.scheduler.MCSimulation.Slot;

public class MultiObjectiveScheduler extends FailureAwareScheduler {

	private Map<Cloudlet, Boolean> critical;



	public MultiObjectiveScheduler(List<Cloudlet> cloudletlist, List<Vm> vmlist) {
		// TODO Auto-generated constructor stub
		super(cloudletlist, vmlist);

		critical = new HashMap<>();
	}

	protected void findCriticalPath(Cloudlet cloudlet){
		critical.put(cloudlet, true);
		Cloudlet criticalParent = null;
		double value = 0.0, maxvalue = 0.0;  
		if(Runner.getParentList(cloudlet)==null)
			return;
		for(Object cloudletObject: Runner.getParentList(cloudlet)){
			Cloudlet parent = (Cloudlet) cloudletObject;

			//find the critical parent for the cloudlet
			value = end[parent.getCloudletId()] + transferCosts.get(parent).get(cloudlet);
			//if cloudlet falls on the critical path, mark it as critical
			if(value > maxvalue){
				criticalParent = parent;
				maxvalue = value;
			}

		}
		//for noncritical parents, assign critical as false
		/*
		for(Cloudlet cloudletObject: Runner.getParentList(cloudlet)){
			Cloudlet parent = cloudletObject;
			if(parent.equals(criticalParent)){
				critical.put(parent, true);
			}else{
				critical.put(parent, false);
			}
		}
		 */
		if(criticalParent != null){
			findCriticalPath(criticalParent);
		}
	}
	/*
	protected void adjustcloudletSlack(){
		for(Vm vmObject: vmlist){
			Vm vm = (Vm) vmObject;
			int position =0;
			double executionTime = 0.0;
			double executionEnergy = 0.0;
			List<Event> sched = schedules.get(vm);
			int size = schedules.get(vm).size();
			for(int i =0; i<size; i++){
				Event ev = sched.get(i);
				if(critical.get(ev.cloudlet) == true){
					executionEnergy += findEnergyConsumption(vm, ev.finish-ev.start, Runner.getVmMips(vm));
					executionTime += ev.finish - ev.start;


				}else{
					//the task is non critical 
					//find slack time for the task as latest finish time - earliest start time
					double est = ev.start;
					System.out.println("EST is: "+ est);
					double lft = findLFT(ev.cloudlet);
					System.out.println("LFT is " + lft);

					double maxmips = vmparams.get(vm).maxMIPS;
					//assuming that there are four vm levels
					double step = (vmparams.get(vm).maxMIPS-vmparams.get(vm).minMIPS)/4;
					double finishTime, mips;
					mips = vmparams.get(vm).minMIPS;
					while(true){

						finishTime = ev.start + ev.cloudlet.getCloudletTotalLength() / mips;
						if(finishTime <= lft)
							break;
						mips = mips + step;
					}
					//find a vm for the cloudlet
					boolean check = checkVmAvailability(vm, est, finishTime, ev.cloudlet);
					//int mips = (int) (maxmips *((lft - est)/(ev.finish - est)));
					if(check){
						System.out.println("mips new = " + mips);

						sched.remove(position);
						sched.add(position, new Event(est, finishTime, ev.cloudlet));
						//find energyConsumtion
						executionEnergy += findEnergyConsumption(vm, ev.finish-ev.start, mips);
						end[ev.cloudlet.getCloudletId()] = finishTime;
						executionTime += finishTime - est;
					}
					else{
						//the task is non critical but vm fails if execution extended
						executionEnergy += findEnergyConsumption(vm, ev.finish-ev.start, Runner.getVmMips(vm));
						executionTime += ev.finish - ev.start;
					}
				}
				position++;
			}
			//schedules.remove(vm);
			//schedules.put(vm, sched);
			executionTimes.put(vm, executionTime);
			executionEnergies.put(vm, executionEnergy);
		}
	}

	 */
	protected void assignVm(Cloudlet cloudlet, double est, double lft){
		int vmid = cloudlet.getVmId();
		int cloudletid = cloudlet.getCloudletId();
		Vm vm = vmlist.get(vmid);//the vm initially assigned to the cloudlet
		List<Event> schedule = schedules.get(vm);
		Event previousassignment = null;
		int position = -1;
		System.out.println("Assigning Cloudlet: " + cloudletid);
		//remove the cloudlet from the schedule of vm
		for(Event event: schedule){
			Event ev = event;
			if(ev.cloudlet.equals(cloudlet)){
				position = schedule.indexOf(ev);
				previousassignment = ev;
				System.out.println("previous assignment for cloudlet  " + cloudletid +"  for vmid:  "+ vmid + " and mips  " + vm.getMips() + "  is: " + ev.start +"\t" + ev.finish);
			}
		}
		if(position!=-1)
			schedule.remove(position);
		Boolean check = false;
		Vm chosenvm = null;
		double chosenmips =0;
		double cloudletstart =0, cloudletfinish =0; 
		double executionEnergy, minExecutionEnergy = Double.MAX_VALUE;
		for(Object vmObject : vmlist){
			Vm currentvm = (Vm) vmObject;
			List<Event> sched = schedules.get(currentvm);
			List<Slot> na = vmnotavailable.get(currentvm);
			Boolean occupied = true;
			Boolean available = true;
			double duration;
			double start = Double.MAX_VALUE, finish = Double.MAX_VALUE;
			double mips = vmparams.get(currentvm).minMIPS;
			//assuming that there are four vm levels
			double step = (vmparams.get(currentvm).maxMIPS-vmparams.get(currentvm).minMIPS)/4;
			while(true){
				duration = cloudlet.getCloudletTotalLength() / mips;
				if(duration <= lft-est){//the vm mips can execute the cloudlet within time
					//find if vm is occupied 
					/*
					for(Event eventObject: sched){
						Event ev = eventObject;
						if(ev.start < est && ev.finish > est+duration){
							occupied = true;
							break;
						}
						if(ev.start > est && ev.start < est+duration){
							occupied = true;
							break;
						}
						if(ev.start > est && ev.finish < est+duration){
							occupied = true;
							break;
						}
					}*/
					if(sched.size() == 0){
						//the vm is not occupied for the duration
						occupied = false;
						start = est;
						
					}else if(sched.size() == 1){
						if(est >= sched.get(0).finish){
							start = est;
							occupied = false;
						}else if((est+ duration) <= sched.get(0).start){
							start = est;
							occupied = false;
						}else{
							start = sched.get(0).finish;
							if(start+ duration <= lft){
								occupied = false;
								
							}else{
								occupied = true;
								
							}
						}
					}else{
						int i = sched.size() - 1;
				        int j = sched.size() - 2;
				        while(j>=0){
				        	Event current = sched.get(i);
				            Event previous = sched.get(j);
				            if(est > previous.finish){
				            	if(est+duration <= current.start){
				            		start = est;
				            		occupied = false;
				            	}
				            	break;
				            }
				            if(previous.finish + duration <=current.start){
				            	if(previous.finish + duration <=lft){
				            		start = previous.finish;
				            		occupied = false;
				            	}else{
				            		occupied = true;
				            		break;
				            	}
				            }
				            i--;
				            j--;
				        }
				        if((est+duration) < sched.get(0).start){
							start = est;
							occupied = false;
						}
					}
					
					
					if(!occupied){
						//if the vm is not occupied, find if it fails during the cloudlet execution
						int pos = 0;
						while(na.get(pos).failure <= makespan){
							//if failure occurs during vm execution
							if(na.get(pos).failure <= start && na.get(pos).repair > start){
								available = false;
								break;
							}
							if(na.get(pos).failure >= start && na.get(pos).failure < start+duration){
								available = false;
								break;
							}
							if(na.get(pos).failure <= start && na.get(pos).repair > start+duration){
								available = false;
								break;
							}
							pos++;
						}
						if(available ==true){//the vm is not occupied and does not fail for the duration
							//find execution energy for the cloudlet
							executionEnergy = findEnergyConsumption(currentvm, duration, mips);
							if(executionEnergy < minExecutionEnergy){
								minExecutionEnergy = executionEnergy;
								chosenvm = currentvm;
								chosenmips = mips;
								cloudletstart = start;
								cloudletfinish = start+duration;
							}
						}
					}
					if(chosenvm == currentvm){
						break;
					}
				}

				mips = mips + step;
				if(mips > vmparams.get(currentvm).maxMIPS)
					break;
			}

		}
		if(chosenvm == null){
			schedule.add(position, previousassignment);
		}else{
			//find position for the chosen vm and add the cloudlet to its schedule
			List<Event> sched = schedules.get(chosenvm);
			int pos =0;
			if(sched.size()==0)
				sched.add(0, new Event(cloudletstart, cloudletfinish, cloudlet, chosenmips));
			else if(sched.size() == 1){
				Event ev = sched.get(0);
				if(cloudletstart >= ev.finish){
					sched.add(1, new Event(cloudletstart, cloudletfinish, cloudlet, chosenmips));
				}else if(cloudletfinish <= ev.start){
					sched.add(0, new Event(cloudletstart, cloudletfinish, cloudlet, chosenmips));
				}
					
			}else{
				int n = sched.size();
				int i=0;
				for(i=0; i<n-1;i++){
					if(cloudletfinish <= sched.get(0).start){
						pos =0;
						break;
					}
					if(sched.get(i).finish <= cloudletstart && sched.get(i+1).start >= cloudletfinish){
						pos = i+1;
						break;
					}
				}
				sched.add(pos, new Event(cloudletstart, cloudletfinish, cloudlet, chosenmips));
			}
			/*
			for(Event ev: sched){
				if(ev.finish >= cloudletstart ){
					position++;
					continue;
				}
				break;
			}
			*/
			
			begin[cloudlet.getCloudletId()] = cloudletstart;
			end[cloudlet.getCloudletId()] = cloudletfinish;
			cloudlet.setVmId(chosenvm.getId());
			System.out.println("New assignment for cloudlet  " + cloudletid +"  for vmid:  "+ chosenvm.getId() +  "  with mips  " + chosenmips  + "  is: " + cloudletstart +"\t" + cloudletfinish);
		}

	}

	protected void adjustcloudletSlack(){
		List<CloudletRank> cloudletRank = new ArrayList<>();
		for (Cloudlet cloudlet : rank.keySet()) {
			cloudletRank.add(new CloudletRank(cloudlet, rank.get(cloudlet)));
		}

		// Sorting in non-ascending order of rank and displaying it
		Collections.sort(cloudletRank);
		for (CloudletRank cr : cloudletRank) {
			Cloudlet cloudlet = cr.cloudlet;
			double est, lft, slack;
			int vmid = cloudlet.getVmId();
			Vm assignedvm = vmlist.get(vmid); 
			/*Boolean avail;
			//if the cloudlet is critical, it does not need to be adjusted
			if(critical.get(cloudlet) == true){
				break;
			}
			*/
			//adjust of noncritical cloudlet
			est = findEST(cloudlet);
			lft = findLFT(cloudlet);
			slack = lft-est;
			double mips = assignedvm.getMips();
			double fastesttime = cloudlet.getCloudletLength()/mips;
			if((slack -fastesttime)< 1.0 || cloudlet.getCloudletId() == Cloudletlist.get(Cloudletlist.size()-1).getCloudletId()){
				critical.put(cloudlet, true);
				
			}else{
				//the task is non critical, find a virtual machine for this cloudlet, within the time frame 
				critical.put(cloudlet, false);
				assignVm(cloudlet, est, lft);
				
			}
		}
	}

	protected double findEST(Cloudlet cloudlet){
		double maxvalue = 0.0;
		double lftParent, transferCost, est;
		if(Runner.getParentList(cloudlet) == null)
			return 0.0;
		for(Cloudlet parent: Runner.getParentList(cloudlet)){
			int id = parent.getCloudletId();
			lftParent = end[id];
			//System.out.println("LFT for parent: "  + lftParent);
			transferCost = transferCosts.get(parent).get(cloudlet);
			//System.out.println("Transfer Cost: " + transferCost);
			est = lftParent+transferCost;
			//System.out.println("LFT: " + lft);
			if(est > maxvalue){
				maxvalue = est;
			}
		}
		System.out.println("EST fr cloudlet   " + cloudlet.getCloudletId() + "  is:  " + maxvalue );
		return maxvalue;
	}


	protected double findLFT(Cloudlet cloudlet){
		double minvalue = Double.MAX_VALUE;
		double estChild, transferCost, lft;
		if(Runner.getChildList(cloudlet) == null)
			return makespan;
		for(Cloudlet child: Runner.getChildList(cloudlet)){
			int id = child.getCloudletId();
			estChild = begin[id];
			//System.out.println("Est: "  + estChild);
			transferCost = transferCosts.get(cloudlet).get(child);
			//System.out.println("Transfer Cost: " + transferCost);
			lft = estChild-transferCost;
			//System.out.println("LFT: " + lft);
			if(lft< minvalue){
				minvalue = lft;
			}
		}
		System.out.println("LFT for cloudlet   " + cloudlet.getCloudletId() + "  is:  " + minvalue );
		return minvalue;
	}

	protected Boolean checkVmAvailability(Vm vm, double start, double finish, Cloudlet cloudlet){
		Boolean flag = true;//to check if the same vm can be assigned 
		//check for availability
		List<Slot> slots = vmnotavailable.get(vm);
		int position =0;
		while(slots.get(position).failure < makespan){
			//if failure occurs during vm execution
			if(slots.get(position).failure < finish && slots.get(position).repair > finish){
				flag = false;
				break;
			}
			position++;
		}
		return flag;
	}

	/*
	protected void deadlineDistribution(Cloudlet cloudlet){
		//while cloudlet has unassigned parent
		while(assigned.containsValue(false)){
			ArrayList<Cloudlet> path = new ArrayList<Cloudlet>();
			Cloudlet c = cloudlet;
			//check if the cloudlet has unassigned parent
			for(Cloudlet cloudletObject: Runner.getParentList(c)){
				Cloudlet parent = cloudletObject;
				Boolean checkunassigned = false;
				double criticalvalue, maxvalue = 0.0;
				Cloudlet critical;
				//if the parent is unassigned, check if it falls on the critical path
				if(assigned.get(parent)== false){
					checkunassigned = true;
					criticalvalue = end[parent.getCloudletId()];
					//if cloudlet falls on the critical path, mark it as critical
					if(criticalvalue > maxvalue){
						critical = parent;
						maxvalue = criticalvalue;
					}

				}
				//if there is an unassigned cloudlet still there, mark parent to be checked in the loop 
				if(checkunassigned){
					path.add(parent);
					c = parent;
				}
			}
			assignSubDeadline(path);
			//mark all cloudlets on the path as assigned
			for(Cloudlet cloudletObject: path){
				Cloudlet task = cloudletObject;
				assigned.put(task, true);
				//update EST and EFT of all unassigned successors and predecessors of task c

				deadlineDistribution(task);
			}

		}
	}

	private void assignSubDeadline(ArrayList<Cloudlet> path){
		Cloudlet lastCloudlet = path.get(path.size()-1);
		Cloudlet firstCloudlet = path.get(0);

	}
	 */
	public void run(){
		try{
			File mosfile = new File("MOSResults.txt");
			BufferedWriter mosWriter = new BufferedWriter(new FileWriter(mosfile, true));

			Log.printLine("MO Scheduler running with " + Cloudletlist.size()
					+ " cloudlets.");

			averageBandwidth = calc_avg_bw();

			for (Object vmObject : vmlist) {
				Vm vm = (Vm) vmObject;
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
			Log.printLine("Allocation for MOScheduler");
			allocateCloudletsfars();

			//find the critical cloudlets by using findCriticalPath() method and passing the last cloudlet to it
			//findCriticalPath(Cloudletlist.get(Cloudletlist.size()-1));
			//for non critical cloudlets, specify the same
			/*
			for(Cloudlet cloudletObject: Cloudletlist){
				Cloudlet cloudlet = cloudletObject;
				if(critical.get(cloudlet)==null)
					critical.put(cloudlet, false);
			}
			*/
			//adjust slack 
			System.out.println();
			for(Object cloudletObject: Cloudletlist){
				Cloudlet cloudlet = (Cloudlet) cloudletObject;
				System.out.println("Cloudlet: " + cloudlet.getCloudletId() + "\tVM Allocated: " + cloudlet.getVmId() + "\tstart: " + begin[cloudlet.getCloudletId()] + "\tfinish: " + end[cloudlet.getCloudletId()]);
			}
			System.out.println();
			adjustcloudletSlack();
			Log.printLine("Assignment after adjusting slack for cloudlets: ");
			for(Object cloudletObject: Cloudletlist){
				Cloudlet cloudlet = (Cloudlet) cloudletObject;
				System.out.println("Cloudlet: " + cloudlet.getCloudletId() + "\tVM Allocated: " + cloudlet.getVmId() + "\tstart: " + begin[cloudlet.getCloudletId()] + "\tfinish: " + end[cloudlet.getCloudletId()]);
			}
			System.out.println();
			System.out.println("The vm failure parameters are: ");
			for(Vm vm: vmlist){
				List<Slot> na = vmnotavailable.get(vm);
				System.out.println("Vm : " + vm.getId());
				for(Slot slot: na){
					System.out.print(slot.failure + "\t" + slot.repair);
				}
				System.out.println();
			}
			findExecutionEnergyConsumption();
			totalEnergyConsumption = findTotalEnergyConsumption();
			System.out.println("\nThe Total Energy Consumption is: " + totalEnergyConsumption);
			//write the makespan for MOS in the specified file

			mosWriter.write(makespan + "\t" + totalEnergyConsumption + "\n" );
			Log.printLine("makespan for MOScheduler = " + makespan);
			mosWriter.close();

		}catch(Exception e){
			e.printStackTrace();
		}
	}

}
