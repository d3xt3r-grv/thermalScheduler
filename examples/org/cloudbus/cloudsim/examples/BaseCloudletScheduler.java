//@author Nidhi Rehani, nidhirehani@gmail.com, NIT Kurukshetra

package org.cloudbus.cloudsim.examples;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Consts;
import org.cloudbus.cloudsim.Vm;


public class BaseCloudletScheduler {

	public class Slot {

		public double failure;
		public double repair;

		public Slot(double failure, double repair) {
			this.failure = failure;
			this.repair = repair;
		}
	}

	public class Event {

		public double start;
		public double finish;
		Cloudlet cloudlet;
		double mips;

		public Event(double start, double finish, Cloudlet cloudlet, double mips) {
			this.start = start;
			this.finish = finish;
			this.cloudlet = cloudlet;
			this.mips = mips;
		}
	}

	public class VmParameters {
		public double coefficient;
		public double maxMIPS;
		public double minMIPS;
		
		public VmParameters(double coefficient, double maxMIPS, double minMIPS){
			this.coefficient = coefficient;
			this.maxMIPS = maxMIPS;
			this.minMIPS = minMIPS;
		}
	}

	public static List<Slot> getSlots(Vm vm){
		return vmnotavailable.get(vm);
	}


	protected List<Cloudlet> Cloudletlist;
	protected List <Vm> vmlist;
	protected Map<Cloudlet, Map<Vm, Double>> computationCosts;
	protected Map<Cloudlet, Map<Cloudlet, Double>> transferCosts;
	protected double averageBandwidth;
	double averageComputationCost = 0.0;
	double makespan =0;
	int numberoffailures =0;
	double begin[], end[];
	//double ccr = 0.2;

	protected static Map<Cloudlet, Double> startTimes;
	protected static Map<Cloudlet, Double> durationTimes;
	protected static Map<Cloudlet, Integer > reservationIds;

	protected Map<Vm, List<Event>> schedules;
	public static Map <Vm, List<Slot>> vmnotavailable;

	public static Map<Vm, VmParameters> vmparams;
	
	double totalEnergyConsumption = 0.0;
	protected Map<Vm, Double> executionTimes;
	protected Map<Vm, Double> executionEnergies;
	
	
	public BaseCloudletScheduler(List<Cloudlet> cloudletlist, List<Vm> vmlist) {
		this.Cloudletlist = cloudletlist;
		this.vmlist = vmlist;
		computationCosts = new HashMap<>();
		transferCosts = new HashMap<>();
		begin = new double[cloudletlist.size()];
		end = new double[cloudletlist.size()];
		startTimes = new HashMap<>();
		durationTimes = new HashMap<>();
		reservationIds = new HashMap<>();
		vmnotavailable = new HashMap<>();
		vmparams = new HashMap<>();
		executionTimes = new HashMap<>();
		executionEnergies = new HashMap<>();

	}
	
	public static double getStartTime(Cloudlet cloudlet){
		return  startTimes.get(cloudlet);

	}

	public static double getDurationTime(Cloudlet cloudlet){
		return durationTimes.get(cloudlet);
	}

	public static int getReservationId(Cloudlet cloudlet){
		return reservationIds.get(cloudlet);
	}
	/*
	 * file vmparams.txt saves the informations as:
	 * for each vm:
	 * coefficient \t maxmips \t minmips
	 */

	protected void allocateVmPowerParameters(){
		try{
			File myFile =new File("vmparams.txt");
			BufferedReader reader = new BufferedReader(new FileReader(myFile));
			String line = null;
			for(Object vmObject: vmlist){
				Vm vm = (Vm) vmObject;
				line = reader.readLine();
				String[] result = line.split("\t");
				vmparams.put(vm, new VmParameters(Double.parseDouble(result[0]), Double.parseDouble(result[1]), Double.parseDouble(result[2])));
				
			}
			reader.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	protected void allocatevmavailability(){
		try{
			File myFile =new File("MCSimulation.txt");
			BufferedReader reader = new BufferedReader(new FileReader(myFile));
			String line = null;
			for(Object vmObject: vmlist){
				Vm vm = (Vm) vmObject;
				vmnotavailable.put(vm, new ArrayList<Slot>());
				List<Slot> slots = vmnotavailable.get(vm);
				line = reader.readLine();
				String[] result = line.split("\t");
				int position =0;
				for(int i =0; i<(result.length); i=i+2){
					//convert hours into seconds and add slots
					slots.add(position, new Slot(Double.parseDouble(result[i])*60*60, Double.parseDouble(result[i+1])*60*60));
					position++;
					//schedules.put(vm, new Event(Integer.parseInt(result[i], Integer.parseInt(result[i+1]));
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	protected double findFailureTime(Vm vm){
		double failureTime =0.0;
		int position =0;
		List<Slot> slots = vmnotavailable.get(vm);
		List<Event> ev = schedules.get(vm);
		if(ev.isEmpty()){
			return 0.0;
		}
		while(slots.get(position).failure < ev.get(ev.size()-1).finish){
			failureTime +=  (slots.get(position).repair - slots.get(position).failure);
			position++;
		}
		return failureTime;

	}

	protected double findEnergyConsumption(Vm vm, double time, double slackedmips){
		double executionEnergy = 0.0;
		executionEnergy = vmparams.get(vm).coefficient * Math.pow(slackedmips/1000, 3)* (time/3600);
		return executionEnergy;
	}
	
	protected void findExecutionEnergyConsumption(){
		
		for(Vm vmObject: vmlist){
			Vm vm = (Vm) vmObject;
			double executionEnergy = 0.0;
			double executionTime = 0.0;
			List<Event> ev = schedules.get(vm);
			for(Event eventObject: ev){
				Event event = (Event) eventObject;
				double mips = event.mips;
				executionTime += event.finish - event.start;
				executionEnergy += findEnergyConsumption(vm, event.finish - event.start, mips);
			}
			executionTimes.put(vm, executionTime);
			executionEnergies.put(vm, executionEnergy);
		}
	}
	protected double findTotalEnergyConsumption(){
		double totalEnergy = 0.0;
		for(Vm vmObject: vmlist){
			Vm vm = (Vm) vmObject;
			List<Event> ev = schedules.get(vm);
			//idleTime stores the idle time and the communication time for the vm since 
			//both operate at the lowest voltage level
			double idleTime = 0.0;
			double executionTime = executionTimes.get(vm);
			//double failureTime = findFailureTime(vm);
			//idle time is equal to last task finish time on that vm - (execution and failure time)
			if(ev.isEmpty()){
				idleTime = 0.0;
			}else{
				idleTime =  ev.get(ev.size()-1).finish - (executionTime );
			}
			double idleEnergy = findEnergyConsumption(vm, idleTime,(int)vmparams.get(vm).minMIPS);
			totalEnergy = totalEnergy + (executionEnergies.get(vm)+ idleEnergy);

		}

		return totalEnergy;
	}

	public double calc_avg_bw(){
		double avg = 0.0;
		for (Object vmObject : vmlist) {
			Vm vm = (Vm) vmObject;
			avg += vm.getBw();
		}
		return avg / vmlist.size();
	}

	public void calc_ComputationCosts() {
		for (Object cloudletObject : Cloudletlist) {
			Cloudlet cloudlet = (Cloudlet) cloudletObject;

			Map<Vm, Double> costsVm = new HashMap<Vm, Double>();
			// System.out.println("\n\nCloudlet: " + cloudlet.getCloudletId() + "  computationCost on vms:");
			for (Object vmObject : vmlist) {
				Vm vm = (Vm) vmObject;
				if (vm.getNumberOfPes() < cloudlet.getNumberOfPes()) {
					costsVm.put(vm, Double.MAX_VALUE);
				} 
				else {
					costsVm.put(vm,
							cloudlet.getCloudletTotalLength() / vm.getMips());
				}
				//System.out.print("\tvm: " + vm.getId() + "  " + costsVm.get(vm));
			}

			computationCosts.put(cloudlet, costsVm);
		}
	}

	public void calc_TransferCosts() {
		// Initializing the matrix
		for (Object cloudletObject1 : Cloudletlist) {
			Cloudlet cloudlet1 = (Cloudlet) cloudletObject1;
			Map<Cloudlet, Double> cloudletTransferCosts = new HashMap<Cloudlet, Double>();

			for (Object cloudletObject2 : Cloudletlist) {
				Cloudlet cloudlet2 = (Cloudlet) cloudletObject2;
				cloudletTransferCosts.put(cloudlet2, 0.0);
			}

			transferCosts.put(cloudlet1, cloudletTransferCosts);
		}

		// Calculating the actual values
		for (Cloudlet parentObject : Cloudletlist) {
			Cloudlet parent = parentObject;

			for (Cloudlet child : Runner.getChildList(parent)) {

				transferCosts.get(parent).put(child, calculateTransferCost(parent, child));
			}

		}
		//displaying values
		/*
	        for (Object cloudletObject1 : Cloudletlist) {
	        	System.out.println("transfer costs: ");
	        	Cloudlet cloudlet1 = (Cloudlet) cloudletObject1;
	            System.out.println();
	            for (Object cloudletObject2 : Cloudletlist) {
	                Cloudlet cloudlet2 = (Cloudlet) cloudletObject2;
	               System.out.print("\t" + transferCosts.get(cloudlet1).get(cloudlet2));
	            }
	            System.out.println();


	        }
		 */
	}

	private double calculateTransferCost(Cloudlet parent, Cloudlet child) {

		double filesize = 0.0;
		//data to be transferred is equal to the output file size of the parent cloudlet in bytes
		filesize = parent.getCloudletOutputSize();
		//file Size is in Bytes, acc in MB
		filesize = filesize / Consts.MILLION;
		// acc in MB, averageBandwidth in Mb/s
		return (filesize * 8) / averageBandwidth;
	}

}
