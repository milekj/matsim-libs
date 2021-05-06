
/* *********************************************************************** *
 * project: org.matsim.*
 * EventsManagerModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

 package org.mjanowski.master;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.events.handler.EventHandler;

import javax.inject.Inject;
import java.util.Set;

public final class MasterEventsManagerModule extends AbstractModule {

	@Override
	public void install() {
		bindEventsManager().to(MasterEventsManager.class).asEagerSingleton();
		bind(EventHandlerRegistrator.class).asEagerSingleton();
	}

	public static class EventHandlerRegistrator {
		@Inject
		EventHandlerRegistrator(EventsManager eventsManager, Set<EventHandler> eventHandlersDeclaredByModules) {
			for (EventHandler eventHandler : eventHandlersDeclaredByModules) {
				eventsManager.addHandler(eventHandler);
			}
		}
	}
}
