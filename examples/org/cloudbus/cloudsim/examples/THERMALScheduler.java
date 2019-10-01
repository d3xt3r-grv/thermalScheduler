
package org.cloudbus.cloudsim.examples;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.examples.BaseCloudletScheduler.Event;

public class THERMALScheduler extends BaseCloudletScheduler {

	private   Map<Cloudlet, Double> rank;
	protected Map<Vm, List<Event>> originalschedules;
	protected Map<Cloudlet, Double> earliestFinishTimes;
	protected Map<Cloudlet, Double> shiftamount;
	protected Map<Vm, Double> temps;
	PriorityQueue<Vm> sortedTemp;
	final int TAMBIENT = 70;
	final int THOLD = 80;
	private double Ptotal = 0;
	private double minCloudlet = 99999999,minMips = 99999999;
	private double maxCloudlet = 0,maxMips=0;
  
  int countfinal = 0;//to specify the position of failure slot to check for the chosenvm
  int countcurrent = 0;//to specify the position of failure slot to check for the curent vm
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

	public THERMALScheduler(List<Cloudlet> cloudletlist, List<Vm> vmlist) {
		// TODO Auto-generated constructor stub
		super(cloudletlist, vmlist);
		rank = new HashMap<>();
      earliestFinishTimes = new HashMap<>();
      originalschedules = new HashMap<>();
      schedules = new HashMap<>();
      shiftamount = new HashMap<>();
      sortedTemp = new PriorityQueue<>(vmlist.size(), new TempComparator());
      temps = new HashMap<>();
      for (Object vm : vmlist) {
    	   // idle temp 50F
    	  temps.put((Vm) vm, 70.0);
      }
      for (Object vm : vmlist) {
    	  Vm v = (Vm) vm;
    	  
    	  double mipss = v.getMips();
    	  minMips = mipss < minMips ? mipss : minMips;
    	  maxMips = mipss > maxMips ? mipss : maxMips;
    	  sortedTemp.add(v);
      }
      
      for (Cloudlet c : cloudletlist) {
    	  double len = c.getCloudletLength();
    	  minCloudlet = len < minCloudlet ? len : minCloudlet;
    	  maxCloudlet = len > maxCloudlet ? len : maxCloudlet;
    	  
      }
      
      
	}
	
	class TempComparator implements Comparator<Vm> {
		public int compare(Vm vm1, Vm vm2) {
			if (temps.get(vm1) < temps.get(vm2)) 
                return 1; 
            else if (temps.get(vm1) > temps.get(vm2)) 
                return -1; 
            return 0; 
		}
	}
	
	public void run(){
		try{
			File heftfile = new File("THEFTResults");
			BufferedWriter HEFTWriter = new BufferedWriter(new FileWriter(heftfile, true));
			
			///Log.printLine("THEFT scheduler with MCS running with " + Cloudletlist.size() + " cloudlets.");

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
	        ///Log.printLine("Ranks calculated");
	        allocatevmavailability();
			allocateVmPowerParameters();
	        // Selection phase
	        Log.printLine("Allocation for THEFT");
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
	        ///System.out.println("\nafter adjusting heft schedule for failures");
	        for(Cloudlet cloudletObject: Cloudletlist){
	        	Cloudlet cloudlet = cloudletObject;
	        	int i = cloudlet.getCloudletId();
	        	///System.out.print("Cloudlet: " + i + " vm allocated: " + cloudlet.getVmId() + "  : start : " + begin[i] + " finish : " + end[i] );
	            startTimes.put(cloudlet, begin[i]);
	            durationTimes.put(cloudlet, end[i]);
	            reservationIds.put(cloudlet, cloudlet.getVmId());
	        	///System.out.println();
	        }
	        ///System.out.println();
	        //set schedules for each vm after failure adjustment
	        for (Object vmObject : vmlist) {
	            Vm vm = (Vm) vmObject;
	            List<Event> sched = originalschedules.get(vm);
	            List<Event> newsched = schedules.get(vm);
	            for(Object event: sched){
	            	Event ev = (Event) event;
	            	int id = ev.cloudlet.getCloudletId();
//	            	Log.printLine(begin[id]   + " " + end[id]);
	            	newsched.add(new Event(begin[id], end[id], ev.cloudlet, vm.getMips()));
	            }
	        }
	        findExecutionEnergyConsumption();
	        totalEnergyConsumption = findTotalEnergyConsumption();
	        System.out.println("\nThe Total Energy Consumption is: " + totalEnergyConsumption);
	        System.out.println("Number of failures occured: " + numberoffailures);
	        //write the makespan for heft in the specified file
//	        makespan = end[Cloudletlist.size()-1] ;
	        System.out.println("\nThe makespan is: " + makespan);
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
	
	public double generateTemp(double vmTemp) {
		double minn = 0;
		double maxx = maxCloudlet/minMips;
		double temp = ((vmTemp - minn)/(maxx - minn)) * (20 - 0.1);
		return temp;
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
//      List<CloudletRank> cloudletRank = new ArrayList<>();
//      for (Cloudlet cloudlet : rank.keySet()) {
//          cloudletRank.add(new CloudletRank(cloudlet, rank.get(cloudlet)));
//      }
//
//   // Sorting in non-ascending order of rank
//      Collections.sort(cloudletRank);
//      for (CloudletRank cr : cloudletRank) {
//          allocateCloudletheft(cr.cloudlet);
//      }
	  allocateCloudletheft();
  }
  
  class JobComparator implements Comparator<Cloudlet> {
		public int compare(Cloudlet c1, Cloudlet c2) {
			if (c1.getCloudletLength() < c2.getCloudletLength()) 
              return -1; 
			else if (c1.getCloudletLength() >= c2.getCloudletLength()) 
              return 1;
			return 0;
		}
  }

  class NodeTempComparator implements Comparator<Vm> {
		public int compare(Vm vm1, Vm vm2) {
			if (temps.get(vm1) <= temps.get(vm2)) 
              return 1; 
			else if (temps.get(vm1) > temps.get(vm2)) 
              return -1;
			return 0;
		}
  }
  
  public void coolVms(List<Vm> vmlist)
  {
	  double p = 1.225; // kg/metre cube
	  double f = 2.3; //GHZ
	  double C = 1003.5; // joules/kgram/C
	  double COP = 0.0068*TAMBIENT*TAMBIENT + 0.0008*TAMBIENT + 0.458;
	  // power consumed to cool from Tout to Tambient
	 
	  for (Vm vm : vmlist) 
	  {
		  double Tout = temps.get(vm);
		  if(Tout <= THOLD) // needs no cooling
			  continue;
		  double Pc = p*f*C*(Tout - TAMBIENT);
		  double Pcrac = Pc/COP;
		  Ptotal += Pcrac;
		  temps.replace(vm, (double)TAMBIENT);  
	  }
	  //if(Ptotal > 0)
	 // Log.print("cooling energy is" + Ptotal+"\n");
  }
  private void allocateCloudletheft() {
      
      Map<Cloudlet, Integer> jobarrival = new HashMap<>();
      Map<Vm, Double> nodeavail = new HashMap<>();
      
      for (Cloudlet c : Cloudletlist) {
    	  jobarrival.put(c, 0);
      }
      
      for (Vm vm : vmlist) {
    	  nodeavail.put(vm, 0.0);
      }
      
      double ti = 1; // tInterval
      double tcur = 0;
      int hua = 0;
      
      while (hua != 1) {
    	  
    	  List<Cloudlet> jobv = new ArrayList<>();
          List<Vm> nodev = new ArrayList<>();
    	  
          coolVms(vmlist);
          
//          for (Vm vm : vmlist) 
//    	  {
//    		   temps.replace(vm, THOLD-1);  
//    	  }
          
    	  for (Cloudlet c : Cloudletlist) {
    		  if (c.getStatus() != Cloudlet.SUCCESS && jobarrival.get(c) <= tcur) {
    			  jobv.add(c);
    		  }
    	  }
    	  
    	  Collections.sort(jobv, new JobComparator());
    	  for (Vm vm : vmlist) {
    		  if (nodeavail.get(vm) <= tcur) {
    			  nodev.add(vm);
    		  }
    	  }
    	  
    	  Collections.sort(nodev, new NodeTempComparator());
    	  
    	  int chosenvm = 0;
    	  
    	  
    	  for (Cloudlet c : jobv) {
    		  
    		  if (chosenvm == nodev.size())
    			  break;
    		  
    		  Vm curVm = nodev.get(chosenvm);
    	      List<Event> sched = originalschedules.get(curVm);

    		  c.setVmId(chosenvm);
    		  Log.printLine("Cloudlet id: " + c.getCloudletId() + " " + "allocated to Vm no.: " + curVm.getId());
    		  double availTime = nodeavail.get(curVm);
    		  begin[c.getCloudletId()] = availTime;
    		  nodeavail.put(curVm, availTime + c.getCloudletLength() / curVm.getMips());
    		  end[c.getCloudletId()] = nodeavail.get(curVm);
    		  
    		  sched.add(new Event(availTime, nodeavail.get(curVm), c, curVm.getMips()));
    		  
    		  double curVmTemp = temps.get(curVm);
    		  curVmTemp += generateTemp(c.getCloudletLength() / curVm.getMips());
    		  Log.printLine(curVmTemp);
    		  //Log.printLine(c.getCloudletLength() / curVm.getMips() + " " + generateTemp(c.getCloudletLength() / curVm.getMips()));
    		  temps.replace(curVm, curVmTemp);
    		  //Log.printLine(temps.get(curVm) + " ----------------------");
    		  chosenvm++;
    		  
    		  makespan = Math.max(makespan, end[c.getCloudletId()]);
    		  
    		  try {
					c.setCloudletStatus(Cloudlet.SUCCESS);
				} catch (Exception e) {
					Log.print("Failed to set the status of cloudlet to success");
					e.printStackTrace();
				}
    	  }
    	  
    	  hua = 1;
    	  for (Cloudlet c : Cloudletlist) {
    		  int done = c.getCloudletStatus() == Cloudlet.SUCCESS ? 1 : 0;
    		  hua = hua & done;
    	  }
    	  tcur += ti;
      }
      coolVms(vmlist);
      Log.printLine("total cooling energy is "+Ptotal/1000);
//      makespan = tcur;
//      cloudlet.setVmId(chosenvm.getId());
  }
  
  private double findFinishTime(Cloudlet cloudlet, Vm vm, double readyTime, boolean occupySlot) {
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
