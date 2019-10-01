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

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;


public class FailureAwareScheduler extends BaseCloudletScheduler{

	protected   Map<Cloudlet, Double> rank;
	
	protected Map<Cloudlet, Double> earliestFinishTimes;
	protected Map<Cloudlet, Double> shiftamount;

	int countfinal =0;//to specify the position of failure slot to check for the chosenvm
	int countcurrent =0;//to specify the position of failure slot to check for the curent vm
	//int maxfr =160;
	
	
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

	public FailureAwareScheduler(List<Cloudlet> cloudletlist, List<Vm> vmlist) {
		// TODO Auto-generated constructor stub
		super(cloudletlist, vmlist);
		rank = new HashMap<>();
		earliestFinishTimes = new HashMap<>();
		schedules = new HashMap<>();
		shiftamount = new HashMap<>();
	}
	
	public void run(){
		try{
			File farsfile = new File("FARSResults.txt");
			BufferedWriter farsWriter = new BufferedWriter(new FileWriter(farsfile, true));
			
			Log.printLine("FARS scheduler with MCS running with " + Cloudletlist.size()
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
	        allocatevmavailability();
			allocateVmPowerParameters();
	        Log.printLine("Ranks calculated");
	        
	        // Selection phase
	        Log.printLine("Allocation for FARS");
	        allocateCloudletsfars();
	        
	        findExecutionEnergyConsumption();
	        totalEnergyConsumption = findTotalEnergyConsumption();
	        System.out.println("\nThe Total Energy Consumption is: " + totalEnergyConsumption);
	        //write the makespan for fars in the specified file
	        
	        farsWriter.write(makespan + "\t" + totalEnergyConsumption + "\n" );
	        Log.printLine("makespan for FARS = " + makespan);
	        farsWriter.close();
	        
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	


	protected void calculateRanks() {
		for (Object cloudletObject : Cloudletlist) {
			Cloudlet cloudlet = (Cloudlet) cloudletObject;
			calculateRank(cloudlet);
		}
	}

	protected double calculateRank(Cloudlet cloudlet) {
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

	protected void allocateCloudletsfars() {
		List<CloudletRank> cloudletRank = new ArrayList<>();
		for (Cloudlet cloudlet : rank.keySet()) {
			cloudletRank.add(new CloudletRank(cloudlet, rank.get(cloudlet)));
		}

		// Sorting in non-ascending order of rank and displaying it
		Collections.sort(cloudletRank);
		for (CloudletRank cr : cloudletRank) {
			allocateCloudletfars(cr.cloudlet);
		}

	}


	protected void allocateCloudletfars(Cloudlet cloudlet) {
		Vm chosenvm = null;
		double earliestFinishTime = Double.MAX_VALUE;
		double bestReadyTime = 0.0;
		double finishTime;

		for (Object vmObject : vmlist) {
			Vm vm = (Vm) vmObject;
			double minReadyTime = 0.0;
			//mcs
			List<Slot> nacurrent =  BaseCloudletScheduler.getSlots(vm);

			for (Cloudlet parent : Runner.getParentList(cloudlet)) {
				double readyTime = earliestFinishTimes.get(parent);
				//mcs

				countcurrent =0;
				//List<Slot> naparent = mc.getSlots(parent);

				while(nacurrent.get(countcurrent).repair <= readyTime){
					countcurrent++;
				}
				if(nacurrent.get(countcurrent).failure <=readyTime){
                	readyTime = nacurrent.get(countcurrent).repair;
                	countcurrent++;
                }
				if (parent.getVmId() != vm.getId()) {
					if(readyTime + transferCosts.get(parent).get(cloudlet) < nacurrent.get(countcurrent).failure ){
						readyTime += transferCosts.get(parent).get(cloudlet);
					}
					else{
						//if(readyTime + transferCosts.get(parent).get(cloudlet) < nacurrent.get(countcurrent+1).failure ){
						readyTime = nacurrent.get(countcurrent).repair + transferCosts.get(parent).get(cloudlet);
						countcurrent++;

						//}
					}/*
                while(nacurrent.get(counrcurrent).repair < readyTime){
                	countcurrent++;
                }
					 */

					minReadyTime = Math.max(minReadyTime, readyTime);
				}
			}

			finishTime = findFinishTimefars(cloudlet, vm, minReadyTime, false, countcurrent);

			if (finishTime < earliestFinishTime) {
				bestReadyTime = minReadyTime;
				earliestFinishTime = finishTime;
				chosenvm = vm;
				countfinal = countcurrent;
			}
		}


		findFinishTimefars(cloudlet, chosenvm, bestReadyTime, true, countfinal);
		earliestFinishTimes.put(cloudlet, earliestFinishTime);
        
        //durationTimes.put(cloudlet, earliestFinishTime - bestReadyTime);
        reservationIds.put(cloudlet, chosenvm.getId());
		cloudlet.setVmId(chosenvm.getId());
		if(cloudlet.getCloudletId() == Cloudletlist.size()-1 ){
			makespan = earliestFinishTime;
		}

	}


	protected double findFinishTimefars(Cloudlet cloudlet, Vm vm, double readyTime,
			boolean occupySlot, int countna) {

		List<Event> sched = schedules.get(vm);
		List<Slot> na = BaseCloudletScheduler.getSlots(vm);
		double computationCost = computationCosts.get(cloudlet).get(vm);
		double transferCost = 0.0;//to find out max transfer cost to the child
		double temp = 0.0;
		double start, finish;
		int pos;
		double finishTime = 0.0;
		for (Cloudlet child : Runner.getChildList(cloudlet)) {

			temp = transferCosts.get(cloudlet).get(child);
			if(temp > transferCost){
				transferCost = temp;
			}
		}

		if (sched.size() == 0) {

			//mcs
			if(readyTime < na.get(countna).failure && (readyTime + computationCost + transferCost) <= na.get(countna).failure){
				//sched.add(new Event(readyTime, readyTime + computationCost));
				finishTime = readyTime + computationCost;
			}
			else{
				do{
					readyTime = na.get(countna).repair;
					countna++;
				}while(!(readyTime >= na.get(countna -1).repair && (readyTime + computationCost + transferCost) <= na.get(countna).failure ));

				//sched.add(new Event(readyTime, readyTime + computationCost));
				finishTime = readyTime + computationCost;
			}
			if (occupySlot) {
				sched.add(new Event(readyTime, readyTime + computationCost, cloudlet, vm.getMips()));
				//cloudlet.setExecStartTime(readyTime);
				startTimes.put(cloudlet, readyTime);
				durationTimes.put(cloudlet, finishTime - readyTime);
				
				System.out.println("Cloudlet: " + cloudlet.getCloudletId() + "\tVM Allocated: " + vm.getId() + "\tstart: " + readyTime + "\tfinish: " + finishTime);
				begin[cloudlet.getCloudletId()] = readyTime;
				end[cloudlet.getCloudletId()] = finishTime;
				if(cloudlet.getCloudletId() == Cloudletlist.size());//you can print makespan
			}
			countcurrent = countna;
			return finishTime;
		}

		// Trivial case: Start after the latest task scheduled
		temp = Math.max(readyTime, sched.get(sched.size() - 1).finish);
		pos= sched.size();
		// start = Math.max(readyTime, sched.get(sched.size() - 1).finish);
		if(temp != readyTime){
			//adjust countna as for last schedule values
			countna =0;
			while(na.get(countna).repair <= temp){
				countna++;
			}
			if(na.get(countna).failure <=temp){
				temp = na.get(countna).repair;
				countna++;
			}
		}
		if(temp < na.get(countna).failure && (temp + computationCost + transferCost) <= na.get(countna).failure ){
			start = temp;

		}else {
			do{
				temp = na.get(countna).repair;
				countna++;

			}while(!(temp <= na.get(countna -1).repair && (temp + computationCost + transferCost) <= na.get(countna).failure ));

		}
		start = temp;

		finish = start + computationCost;
		countcurrent = countna;

		if (occupySlot) {
			sched.add(pos, new Event(start, finish, cloudlet, vm.getMips()));
			startTimes.put(cloudlet, start);
			durationTimes.put(cloudlet, finish - start);
			begin[cloudlet.getCloudletId()] = start;
			end[cloudlet.getCloudletId()] = finish;
			//cloudlet.setExecStartTime(start);
			System.out.println("Cloudlet: " + cloudlet.getCloudletId() + "\tVM Allocated: " + vm.getId() + "\tstart: " + start + "\tfinish: " + finish);

		}

		return finish;
	}



}
