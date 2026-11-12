package org.example.api;

import org.example.Util.PerformanceReporter;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MonitorRecursosTaller {

    @EventListener(ApplicationReadyEvent.class)
    public void registrarInicio() {
        PerformanceReporter.reportarMemoriaSistema();
    }
}
