package controller;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import model.Elevator;
import model.Floor;
import sqelevator.IElevator;
import ui.UIInterface;

public class ElevatorManager implements ElevatorManagerMainInterface, ElevatorManagerUIInterface, ElevatorManagerAlgorithmInterface {
	protected static final int MAX_REMOTE_EXCEPTIONS = 5;
	private List<Elevator> elevators;
	private final IElevator controller;
	private List<Floor> floors;
	private boolean automaticMode;
	// introduced it to handle remote exceptions only in the update method, and
	// not in the setUI
	private boolean listsInitialized;

	private ElevatorManagerAlgorithmInterface algorithm;
	private UIInterface ui;
	private int exceptionsCatched, floorHeight;

	public ElevatorManager(IElevator controller) {
		this.controller = controller;
		ui = null;
		listsInitialized = false;
		elevators = null;
		algorithm = this;
	}

	private void addElevators() throws RemoteException {
		elevators = new ArrayList<Elevator>();
		for (int i = 0; i < controller.getElevatorNum(); i++) {
			Elevator e = new Elevator(i);
			e.setName("Elevator " + i);
			elevators.add(e);
		}
	}

	public void updateElevators() {
		if (exceptionsCatched > MAX_REMOTE_EXCEPTIONS) {
			return;
		}

		try {
			if (!listsInitialized) {
				createFloorsList();
				addElevators();
				listsInitialized = true;
			}

			for (Elevator e : elevators) {
				updateElevator(e);
				if (automaticMode) {
					algorithm.controlElevator(e, this);
				}
			}
			for (Floor f : floors) {
				updateFloor(f);
			}
			exceptionsCatched = 0;
		} catch (RemoteException e) {
			exceptionsCatched++;
			if (exceptionsCatched > MAX_REMOTE_EXCEPTIONS) {
				ui.showError("Connection lost to the elevator. Please restart the application.");
			}
		}
	}

	private void updateElevator(Elevator e) throws RemoteException {
		int n = e.getElevatorNumber();

		e.setPosition(controller.getElevatorPosition(n));
		e.setSpeed(controller.getElevatorSpeed(n));
		e.setWeight(controller.getElevatorWeight(n));
		e.setDoorStatus(controller.getElevatorDoorStatus(n));
		e.setCommitedDirection(controller.getCommittedDirection(n));
		e.setNearestFloor(controller.getElevatorFloor(n));
		e.setTargetFloor(controller.getTarget(n));
	}

	/* (non-Javadoc)
	 * @see controller.ElevatorManagerAlgorithm#controlElevator(model.Elevator)
	 */
	@Override
	public void controlElevator(Elevator e, ElevatorManagerInterface manager) throws RemoteException {
		if (e.getNearestFloor() == e.getTargetFloor() && e.getSpeed() == 0
				&& e.getDoorStatus() == IElevator.ELEVATOR_DOORS_OPEN) {
			if (e.getNearestFloor() == 0) {
				e.setCommitedDirection(IElevator.ELEVATOR_DIRECTION_UP);
			}
			if (e.getNearestFloor() == manager.getFloorsCount() - 1) {
				e.setCommitedDirection(IElevator.ELEVATOR_DIRECTION_DOWN);
			}
			switch (e.getCommitedDirection()) {
			case IElevator.ELEVATOR_DIRECTION_UNCOMMITTED:
			case IElevator.ELEVATOR_DIRECTION_DOWN:
				manager.setTargetFloor(e, e.getNearestFloor() - 1);
				break;
			case IElevator.ELEVATOR_DIRECTION_UP:
				manager.setTargetFloor(e, e.getNearestFloor() + 1);
				break;
			default:
				break;
			}
		}
	}

	private void updateFloor(Floor f) throws RemoteException {
		f.setDownCall(controller.getFloorButtonDown(f.getFloor()));
		f.setUpCall(controller.getFloorButtonUp(f.getFloor()));
	}

	public void setTargetFloor(Elevator elevator, int targetFloor) {
		try {
			controller.setTarget(elevator.getElevatorNumber(), targetFloor);
			int direction = elevator.getNearestFloor() < targetFloor ? IElevator.ELEVATOR_DIRECTION_UP
					: IElevator.ELEVATOR_DIRECTION_DOWN;
			controller.setCommittedDirection(elevator.getElevatorNumber(), direction);
		} catch (RemoteException e) {
				ui.showError("Could not send the command to the elevator. Please try again.");
		}
	}

	private void createFloorsList() throws RemoteException {
		floors = new ArrayList<>();
		for (int i = 0; i < controller.getFloorNum(); i++) {
			floors.add(new Floor(i));
		}
		floorHeight = controller.getFloorHeight();
	}

	// Getters/Setters

	public List<Elevator> getElevators() {
		return elevators;
	}

	public List<Floor> getFloors() {
		return floors;
	}

	public int getFloorsCount() {
		return floors.size();
	}

	public int getFloorHeight() {
		return floorHeight;
	}

	public void setAutomaticMode(boolean automaticMode) {
		this.automaticMode = automaticMode;
	}

	public boolean isAutomaticMode() {
		return this.automaticMode;
	}

	public void setUI(UIInterface ui)
	{
		this.ui = ui;
	}

	@Override
	public void setAlgorithm(ElevatorManagerAlgorithmInterface algorithm) {
		this.algorithm = algorithm;
	}
}
