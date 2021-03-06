/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.aa.light;

import de.citec.csra.allocation.cli.ExecutableResource;
import static de.citec.csra.allocation.cli.ExecutableResource.Completion.MONITOR;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openbase.bco.dal.remote.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import static rst.communicationpatterns.ResourceAllocationType.ResourceAllocation.Initiator.SYSTEM;
import rst.communicationpatterns.ResourceAllocationType.ResourceAllocation.Policy;
import rst.communicationpatterns.ResourceAllocationType.ResourceAllocation.Priority;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.state.PowerStateType.PowerState.State;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author Patrick Holthaus
 * (<a href=mailto:patrick.holthaus@uni-bielefeld.de>patrick.holthaus@uni-bielefeld.de</a>)
 */
public class AllocatedLightSwitch extends ExecutableResource<Void> {

	private final long HA_TIMEOUT = 500;

	private final static Logger LOG = Logger.getLogger(AllocatedLightSwitch.class.getName());
	private final UnitConfig unitCfg;
	private final State state;
	private final long interval;

public AllocatedLightSwitch(UnitConfig unitCfg, State state, Policy pol, Priority prio, long duration, long interval, TimeUnit timeUnit) throws CouldNotPerformException {
		super("switch:" + unitCfg.getLabel() + " -> " + state.name(),
				pol, prio, SYSTEM, 0, duration, timeUnit, MONITOR,
				ScopeGenerator.generateStringRep(unitCfg.getScope()));
		this.unitCfg = unitCfg;
		this.interval = timeUnit.toMicros(interval);
		this.state = state;
	}

	@Override
	public Void execute() throws ExecutionException, InterruptedException {
		if (interval > 0) {
			while (interval < getRemote().getRemainingTime()) {
				exec();
				Thread.sleep(interval / 1000, (int) ((interval % 1000) * 1000));
			}
		} else {
			exec();
		}
		return null;
	}

	private void exec() throws ExecutionException, InterruptedException {
		try {
			ColorableLightRemote light = Units.getFutureUnit(unitCfg, true, ColorableLightRemote.class).get(HA_TIMEOUT, MILLISECONDS);
			LOG.log(Level.FINE, "execute setPower async with parameters ''{0}'' at ''{1}''", new Object[]{state, unitCfg.getLabel() + " [" + ScopeGenerator.generateStringRep(unitCfg.getScope()) + "]"});
			light.setPowerState(PowerState.newBuilder().setValue(state).build());
		} catch (CouldNotPerformException ex) {
			LOG.log(Level.SEVERE, "could not perform ^^", ex);
			throw new ExecutionException(ex);
		} catch (TimeoutException ex) {
			LOG.log(Level.SEVERE, "timeout", ex);
		}
	}
}
