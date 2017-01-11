package controller;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import model.Elevator;
import model.Floor;
import sqelevator.IElevator;
import ui.UserInterface;

public class ElevatorManager implements ElevatorManagerInterface {
	private static final int MAX_REMOTE_EXCEPTIONS = 5;
	private List<Elevator> elevators;
	private IElevator controller;
	private List<Floor> floors;
	private boolean uiInitialized;

	private UserInterface ui;
	private int exceptionsCatched;

	public ElevatorManager(IElevator controller) {
		this.uiInitialized = false;
		this.controller = controller;
		ui = null;
	}

	public void addElevators() throws RemoteException {
		elevators = new ArrayList<Elevator>();
		for (int i = 0; i < controller.getElevatorNum(); i++) {
			Elevator e = new Elevator(i);
			elevators.add(e);
			ui.addElevator(e);
		}

	}

	public void addElevator(Elevator elevator) throws RemoteException {
		elevators.add(elevator);
		elevator.setFloors(floors);
	}

	public void updateElevators() {
		if (ui == null || exceptionsCatched > MAX_REMOTE_EXCEPTIONS)
			return;

		try {
			if (!uiInitialized) {
				createFloorsList();
				addElevators();
				uiInitialized = true;
			}

			for (Elevator e : elevators) {
				updateElevator(e);
			}

			ui.update(elevators);

			exceptionsCatched = 0;
		} catch (RemoteException e) {
			exceptionsCatched++;
			if (exceptionsCatched > MAX_REMOTE_EXCEPTIONS)
				ui.showError("Connection lost to the elevator. Please restart the application.");
		}

	}

	private void updateElevator(Elevator e) throws RemoteException {
		int n = e.getElevatorNumber();

		e.setPosition(controller.getElevatorPosition(n));
		e.setSpeed(controller.getElevatorSpeed(n));
		e.setWeight(controller.getElevatorCapacity(n));
		e.setDoorStatus(controller.getElevatorDoorStatus(n));
		e.setNearestFloor(controller.getElevatorFloor(n));
	}

	public void setTargetFloor(Elevator elevator, int targetFloor) {
		try {
			controller.setTarget(elevator.getElevatorNumber(), targetFloor);
			int direction = elevator.getNearestFloor() < targetFloor ? IElevator.ELEVATOR_DIRECTION_UP
					: IElevator.ELEVATOR_DIRECTION_DOWN;
			controller.setCommittedDirection(elevator.getElevatorNumber(), direction);
		} catch (RemoteException e) {
			exceptionsCatched++;
			if (exceptionsCatched > MAX_REMOTE_EXCEPTIONS)
				ui.showError("Connection lost to the elevator. Please restart the application.");
		}
	}

	private void createFloorsList() throws RemoteException {
		floors = new ArrayList<>();
		for (int i = 0; i < controller.getFloorNum(); i++) {
			floors.add(new Floor(i));
		}
	}

	// Getters/Setters
	public static int getNumberOfFloors() throws RemoteException {
		return controller.getFloorNum();
	}

	public List<Elevator> getElevators() {
		return elevators;
	}

	public UserInterface getUi() {
		return ui;
	}

	public void setUi(UserInterface ui) {
		uiInitialized = false;
		this.ui = ui;
	}

}
