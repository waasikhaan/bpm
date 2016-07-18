package io.takari.bpm.handlers;

import io.takari.bpm.AbstractEngine;
import io.takari.bpm.DefaultExecution;
import io.takari.bpm.FlowUtils;
import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.commands.ProcessEventMappingCommand;
import io.takari.bpm.model.SequenceFlow;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParallelGatewayHandler extends AbstractElementHandler {

    private static final Logger log = LoggerFactory.getLogger(ParallelGatewayHandler.class);

    public ParallelGatewayHandler(AbstractEngine engine) {
        super(engine);
    }

    @Override
    public void handle(DefaultExecution s, ProcessElementCommand c) throws ExecutionException {
        s.pop();

        String eId = c.getElementId();
        String defId = c.getProcessDefinitionId();

        // try to join
        IndexedProcessDefinition pd = getProcessDefinition(defId);
        List<SequenceFlow> in = ProcessDefinitionUtils.findIncomingFlows(pd, eId);

        int activated = s.getActivationCount(defId, eId) + 1; // add current activation
        int total = in.size();

        if (activated > total) {
            throw new ExecutionException("Incorrect number of activations for the element '%s' in the process '%s': %d", eId, defId, activated);
        } else if (activated == total) {
            log.debug("handle ['{}', '{}'] -> forking", s.getId(), c.getProcessDefinitionId());
            // fork
            List<SequenceFlow> out = ProcessDefinitionUtils.findOutgoingFlows(pd, eId);
            List<SequenceFlow> filtered = filter(s, c, out);

            List<SequenceFlow> inactive = new ArrayList<>(out);
            inactive.removeAll(filtered);
            processInactive(s, c, inactive);

            s.push(new ProcessEventMappingCommand(c.getProcessDefinitionId()));

            UUID groupId = getEngine().getUuidGenerator().generate();

            FlowUtils.followFlows(s, c.getProcessDefinitionId(), c.getElementId(), groupId, false, filtered);
        } else {
            log.debug("handle ['{}', '{}'] -> keep joining on '{}' (activated: {}, total: {})", s.getId(), c.getProcessDefinitionId(), eId, activated, total);
        }
    }

    protected List<SequenceFlow> filter(DefaultExecution s, ProcessElementCommand c, List<SequenceFlow> flows) {
        return flows;
    }

    protected void processInactive(DefaultExecution s, ProcessElementCommand c, List<SequenceFlow> inactive) throws ExecutionException {
        // nothing to do
    }
}
